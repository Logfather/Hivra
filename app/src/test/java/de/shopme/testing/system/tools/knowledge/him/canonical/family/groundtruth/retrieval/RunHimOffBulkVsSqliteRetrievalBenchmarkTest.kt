package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonParser
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceProjection
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexBuildState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexSchemaV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceSearchResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimOffEvidenceIndexValidator
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimOffEvidenceProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimOffProductionEvidenceIndexPaths
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimOffProductionEvidenceIndexTool
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import java.util.zip.GZIPInputStream
import kotlin.math.roundToLong

class RunHimOffBulkVsSqliteRetrievalBenchmarkTest {
    @Test
    fun benchmarkOffBulkAgainstProductionSqliteFts5() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        val root = projectRoot()
        val source = root.resolve(HimOffProductionEvidenceIndexPaths.SOURCE)
        val index = root.resolve(HimOffProductionEvidenceIndexPaths.FINAL_INDEX)
        val authority = root.resolve(AUTHORITY_PATH)
        val report = root.resolve(REPORT_PATH)

        require(source.isFile && source.length() == SOURCE_BYTES)
        require(index.isFile && index.length() == INDEX_BYTES)
        require(root.usableSpace >= MINIMUM_FREE_BYTES)
        val guardedBefore = guards(root)
        require(guardedBefore == EXPECTED_GUARDS)

        Class.forName("org.sqlite.JDBC")
        val validation = HimOffEvidenceIndexValidator.validateReadOnly(index)
        require(validation.metadata.schemaVersion == HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION)
        require(validation.metadata.sourceArtifactSha256 == SOURCE_SHA)
        require(validation.metadata.buildState == HimEvidenceRetrievalIndexBuildState.VALIDATED)
        require(validation.metadata.evidenceRecordCount == EXPECTED_RECORDS)
        require(validation.metadata.ftsRowCount == EXPECTED_RECORDS)
        require(validation.metadata.logicalContentSha256 == LOGICAL_DIGEST)
        require(validation.integrityCheck == "ok")
        val indexIdentityBefore = FileIdentity(index.length(), index.lastModified())

        val canonicals = readCanonicals(authority)
        require(canonicals.size == 10)
        val results = canonicals.map { canonical ->
            val bulk = bulk(source, canonical.query)
            require(bulk.recordsInspected == EXPECTED_RECORDS)
            require(bulk.results.size <= LIMIT)
            val sqliteRuns = buildList {
                add(sqlite(index, canonical.query))
                repeat(3) { add(sqlite(index, canonical.query)) }
            }
            require(sqliteRuns.size == 4 && sqliteRuns.all { it.results.size <= LIMIT })
            QueryBenchmark(canonical, bulk, sqliteRuns.first(), sqliteRuns.drop(1))
        }

        require(results.sumOf { it.bulk.recordsInspected } == EXPECTED_RECORDS * 10)
        require(FileIdentity(index.length(), index.lastModified()) == indexIdentityBefore)
        require(guards(root) == guardedBefore)
        val postMetadata = metadataSnapshot(index)
        require(postMetadata == MetadataSnapshot(EXPECTED_RECORDS, EXPECTED_RECORDS, LOGICAL_DIGEST.value, "VALIDATED"))

        report.parentFile.mkdirs()
        Files.write(
            report.toPath(),
            renderReport(results, source, index, guardedBefore).toByteArray(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
        )
    }

    private fun bulk(source: File, query: String): BulkMeasurement {
        val tokens = tokens(query)
        require(tokens.isNotEmpty())
        val start = System.nanoTime()
        var first = -1L
        var inspected = 0L
        val matches = ArrayList<HimEvidenceSearchResult>(LIMIT)
        BufferedReader(
            InputStreamReader(GZIPInputStream(source.inputStream().buffered(BUFFER_BYTES)), Charsets.UTF_8),
            BUFFER_BYTES,
        ).useLines { lines ->
            lines.forEach { line ->
                inspected++
                val record = HimOffEvidenceProjectionV1.fromOptimizedSourceLine(line, inspected)
                val searchable = listOf(
                    record.searchText.primaryName,
                    record.searchText.secondaryNames,
                    record.searchText.taxonomyText,
                    record.searchText.ingredientText,
                    record.searchText.contextText,
                ).joinToString(" ")
                if (tokens.all(searchable::contains)) {
                    if (first < 0) first = System.nanoTime() - start
                    if (matches.size < LIMIT) {
                        matches += HimEvidenceSearchResult(
                            source = HimGroundTruthSource.OPEN_FOOD_FACTS,
                            sourceRecordReference = record.sourceRecordReference,
                            recordKind = record.recordKind,
                            retrievalRank = matches.size + 1,
                            evidenceProjection = record.evidenceProjection,
                        )
                    }
                }
            }
        }
        val total = System.nanoTime() - start
        return BulkMeasurement(if (first >= 0) first else total, total, inspected, matches)
    }

    private fun sqlite(index: File, query: String): SqliteMeasurement {
        val ftsQuery = tokens(query).joinToString(" ") { "\"${it.replace("\"", "\"\"")}\"" }
        require(ftsQuery.isNotBlank())
        val start = System.nanoTime()
        var first = -1L
        val results = HimOffProductionEvidenceIndexTool.connection(index, readOnly = true).use { connection ->
            connection.prepareStatement(HimEvidenceRetrievalIndexSchemaV1.SEARCH_SQL).use { statement ->
                statement.setString(1, ftsQuery)
                statement.setInt(2, LIMIT)
                statement.executeQuery().use { rows ->
                    buildList {
                        while (rows.next()) {
                            if (first < 0) first = System.nanoTime() - start
                            add(
                                HimEvidenceSearchResult(
                                    source = HimGroundTruthSource.OPEN_FOOD_FACTS,
                                    sourceRecordReference = HimEvidenceRecordReference.parse(
                                        HimGroundTruthSource.OPEN_FOOD_FACTS,
                                        rows.getString("source_record_reference"),
                                    ),
                                    recordKind = HimEvidenceRecordKind.valueOf(rows.getString("record_kind")),
                                    retrievalRank = size + 1,
                                    evidenceProjection = HimEvidenceProjection(rows.getString("evidence_projection_json")),
                                )
                            )
                        }
                    }
                }
            }
        }
        val total = System.nanoTime() - start
        return SqliteMeasurement(if (first >= 0) first else total, total, results)
    }

    private fun readCanonicals(authority: File): List<BenchmarkCanonical> {
        val wanted = QUERY_ORDER.toSet()
        val found = JsonParser.parseReader(authority.reader()).asJsonObject.getAsJsonArray("families")
            .map { it.asJsonObject }
            .filter { it.get("canonicalName").asString in wanted }
            .associate { family ->
                val name = family.get("canonicalName").asString
                name to BenchmarkCanonical(
                    canonicalId = family.getAsJsonObject("canonicalId").get("value").asString,
                    canonicalName = name,
                    query = name,
                )
            }
        require(found.keys == wanted) { "Missing benchmark Canonicals: ${wanted - found.keys}" }
        return QUERY_ORDER.map(found::getValue)
    }

    private fun metadataSnapshot(index: File): MetadataSnapshot =
        HimOffProductionEvidenceIndexTool.connection(index, readOnly = true).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT evidence_record_count,fts_row_count,logical_content_sha256,build_state FROM index_metadata WHERE singleton_id=1"
                ).use { row ->
                    require(row.next())
                    MetadataSnapshot(row.getLong(1), row.getLong(2), row.getString(3), row.getString(4))
                }
            }
        }

    private fun renderReport(
        results: List<QueryBenchmark>,
        source: File,
        index: File,
        guards: Map<String, String>,
    ): String {
        val bulkTimes = results.map { it.bulk.totalNanos }
        val firstTimes = results.map { it.firstPass.totalNanos }
        val warmMedians = results.map { median(it.warm.map(SqliteMeasurement::totalNanos)) }
        val speedups = results.mapIndexed { indexValue, value -> value.bulk.totalNanos.toDouble() / warmMedians[indexValue] }
        fun times(values: List<Long>) = "total=${duration(values.sum())} mean=${duration(values.average().roundToLong())} median=${duration(median(values))} min=${duration(values.min())} max=${duration(values.max())}"
        return buildString {
            appendLine("HIM F3d.2c.1a OFF BULK VS SQLITE FTS5 RETRIEVAL BENCHMARK")
            appendLine("================================================================")
            appendLine("DETERMINISTIC DATA")
            guards.forEach { (path, sha) -> appendLine("guard[$path]=$sha") }
            appendLine("sourceBytes=${source.length()}")
            appendLine("sourceRecords=$EXPECTED_RECORDS")
            appendLine("indexBytes=${index.length()}")
            appendLine("indexLogicalDigest=${LOGICAL_DIGEST.value}")
            appendLine("queryOrder=${QUERY_ORDER.joinToString(" | ")}")
            results.forEachIndexed { ordinal, result ->
                appendLine("canonical[${ordinal + 1}]=${result.canonical.canonicalId} | ${result.canonical.canonicalName} | ${result.canonical.query}")
                appendLine("bulkReferences[${ordinal + 1}]=${result.bulk.results.joinToString(",") { it.sourceRecordReference.value }}")
                appendLine("sqliteReferences[${ordinal + 1}]=${result.firstPass.results.joinToString(",") { it.sourceRecordReference.value }}")
            }
            appendLine()
            appendLine("NONDETERMINISTIC MEASUREMENTS")
            appendLine("Canonical | Bulk Total | SQLite First | SQLite Warm Median | Speedup")
            appendLine("----------|------------|--------------|--------------------|--------")
            results.forEachIndexed { i, result ->
                appendLine("${result.canonical.canonicalName} | ${duration(result.bulk.totalNanos)} | ${duration(result.firstPass.totalNanos)} | ${duration(warmMedians[i])} | ${ratio(speedups[i])}")
            }
            appendLine()
            appendLine("PER QUERY")
            results.forEachIndexed { i, result ->
                val intersection = result.bulk.results.map { it.sourceRecordReference.value }.toSet()
                    .intersect(result.firstPass.results.map { it.sourceRecordReference.value }.toSet()).size
                appendLine("query=${result.canonical.query}")
                appendLine("bulkFirst=${duration(result.bulk.firstNanos)} bulkTotal=${duration(result.bulk.totalNanos)} recordsInspected=${result.bulk.recordsInspected} bulkResults=${result.bulk.results.size}")
                appendLine("sqliteFirstPassFirst=${duration(result.firstPass.firstNanos)} sqliteFirstPassTotal=${duration(result.firstPass.totalNanos)} sqliteResults=${result.firstPass.results.size}")
                appendLine("sqliteWarm1=${duration(result.warm[0].totalNanos)} sqliteWarm2=${duration(result.warm[1].totalNanos)} sqliteWarm3=${duration(result.warm[2].totalNanos)} sqliteWarmMedian=${duration(warmMedians[i])}")
                appendLine("intersection=$intersection intersectionOverSqlite=${if (result.firstPass.results.isEmpty()) "n/a" else "%.4f".format(Locale.ROOT, intersection.toDouble() / result.firstPass.results.size)} speedup=${ratio(speedups[i])}")
            }
            appendLine()
            appendLine("AGGREGATES")
            appendLine("bulk=${times(bulkTimes)}")
            appendLine("bulkRecordsInspected=${results.sumOf { it.bulk.recordsInspected }} expected=${EXPECTED_RECORDS * 10} fullScans=10")
            appendLine("sqliteFirstPass=${times(firstTimes)}")
            appendLine("sqliteWarmMedian=${times(warmMedians)}")
            appendLine("speedup=min=${ratio(speedups.min())} max=${ratio(speedups.max())} mean=${ratio(speedups.average())} median=${ratio(medianDouble(speedups))}")
            appendLine("overallThroughputSpeedup=${ratio(bulkTimes.sum().toDouble() / warmMedians.sum())}")
            appendLine("sqliteTimedQueries=40")
            appendLine("additionalUntimedQueries=precondition/postcondition validation only")
            appendLine("bulkMemory=streaming; complete Source not retained")
            appendLine("sqliteMemory=bounded Top-10 materialization")
            appendLine("cacheLimitation=FIRST_PASS is not guaranteed physical cold-cache I/O; query order and prior reads may affect OS/JVM caches; no cache purge was performed")
            appendLine("semanticEquivalenceClaim=false")
            appendLine("bm25ExposedAsSemanticConfidence=false")
            appendLine("candidates=0 validations=0 entityIds=0 authorityMutations=0 groundTruthReleases=0")
        }
    }

    private fun guards(root: File): Map<String, String> = EXPECTED_GUARDS.keys.associateWith { path -> sha256(root.resolve(path)) }

    private fun sha256(file: File): String {
        require(file.isFile)
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(BUFFER_BYTES)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun tokens(query: String): List<String> = Regex("[\\p{L}\\p{N}]+")
        .findAll(Normalizer.normalize(query, Normalizer.Form.NFC).lowercase(Locale.ROOT))
        .map { it.value }
        .toList()

    private fun median(values: List<Long>): Long = values.sorted()[values.size / 2]
    private fun medianDouble(values: List<Double>): Double = values.sorted().let { (it[4] + it[5]) / 2.0 }
    private fun duration(nanos: Long): String = when {
        nanos >= 1_000_000_000 -> "%.6fs".format(Locale.ROOT, nanos / 1e9)
        nanos >= 1_000_000 -> "%.3fms".format(Locale.ROOT, nanos / 1e6)
        nanos >= 1_000 -> "%.3fµs".format(Locale.ROOT, nanos / 1e3)
        else -> "${nanos}ns"
    }
    private fun ratio(value: Double): String = "%.3fx".format(Locale.ROOT, value)

    private fun projectRoot(): File {
        var directory = File(System.getProperty("user.dir")).absoluteFile
        while (!directory.resolve("settings.gradle.kts").isFile) directory = requireNotNull(directory.parentFile)
        return directory
    }

    private data class BenchmarkCanonical(val canonicalId: String, val canonicalName: String, val query: String)
    private data class BulkMeasurement(val firstNanos: Long, val totalNanos: Long, val recordsInspected: Long, val results: List<HimEvidenceSearchResult>)
    private data class SqliteMeasurement(val firstNanos: Long, val totalNanos: Long, val results: List<HimEvidenceSearchResult>)
    private data class QueryBenchmark(val canonical: BenchmarkCanonical, val bulk: BulkMeasurement, val firstPass: SqliteMeasurement, val warm: List<SqliteMeasurement>)
    private data class FileIdentity(val bytes: Long, val modifiedMillis: Long)
    private data class MetadataSnapshot(val evidence: Long, val fts: Long, val digest: String, val state: String)

    companion object {
        private const val AUTHORITY_PATH = "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json"
        private const val REPORT_PATH = "build/knowledge/reports/him/retrieval/him-f3d2c1a-off-bulk-vs-sqlite-retrieval-benchmark.txt"
        private const val EXPECTED_RECORDS = 4_591_865L
        private const val SOURCE_BYTES = 2_091_302_220L
        private const val INDEX_BYTES = 25_551_749_120L
        private const val LIMIT = 10
        private const val BUFFER_BYTES = 1024 * 1024
        private const val MINIMUM_FREE_BYTES = 12L * 1024 * 1024 * 1024
        private val SOURCE_SHA = HimSha256("63b20183229a6f3f648c3d189c265d5d5a4b332d530d084af5640081d060a236")
        private val LOGICAL_DIGEST = HimSha256("627ad847e9038961e2ffad790db8dbb5fc23a358720fc6dbdc542eb2323cd743")
        private val QUERY_ORDER = listOf("Hering", "Matjes", "Dinkelvollkornbrot", "Kartoffeln", "Milch", "Butter", "Tomaten", "Haferflocken", "Lachs", "Mozzarella")
        private val EXPECTED_GUARDS = linkedMapOf(
            "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json" to "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f",
            "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json" to "46145e663b483777228894a099f958f8150773af98b3772882823745b365cf9a",
            AUTHORITY_PATH to "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184",
            "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json" to "4afc7490bd69f2f8919558d33d98c912771d128da4ac7a782cc116a1c9fccb6e",
            "data/knowledge/him/canonical-family/index/him-entity-fingerprint-index.v1.json" to "6dcb065d6dce0aaa22c8d1d3dd620372fc90d3fe9a3ec9d20ee54737a8f9a951",
            HimOffProductionEvidenceIndexPaths.SOURCE to SOURCE_SHA.value,
        )
    }
}
