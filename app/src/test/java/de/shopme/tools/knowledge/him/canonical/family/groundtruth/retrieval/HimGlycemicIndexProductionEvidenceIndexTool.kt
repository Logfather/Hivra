package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonParser
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.sql.Connection
import java.sql.DriverManager
import java.util.zip.GZIPInputStream

object HimGlycemicIndexProductionEvidenceIndexPaths {
    const val SOURCE = "data/sources/glycemic-index/optimized/glycemic-index-him-final-source.json.gz"
    const val FINAL_INDEX = "data/sources/glycemic-index/index/glycemic-index-him-evidence-index.v1.sqlite"
    const val BUILDING_INDEX = "data/sources/glycemic-index/index/.glycemic-index-him-evidence-index.v1.sqlite.building"
    const val REPORT = "build/knowledge/reports/him/retrieval/him-f3d2c4-glycemic-index-production-evidence-index-build.txt"
}

data class HimGlycemicIndexEvidenceIndexValidation(
    val metadata: HimEvidenceRetrievalIndexMetadata,
    val recomputedLogicalDigest: HimSha256,
    val integrityCheck: String,
    val kindCounts: Map<HimEvidenceRecordKind, Long>,
    val missingnessStatuses: Set<String>,
    val representativeMeasurementReference: String,
)

data class HimGlycemicIndexEvidenceIndexBuildResult(
    val builtNow: Boolean,
    val fullSourceReads: Int,
    val validation: HimGlycemicIndexEvidenceIndexValidation,
)

object HimGlycemicIndexProductionEvidenceIndexTool {
    const val EXPECTED_TOTAL = 2_254L
    const val BATCH_SIZE = 500
    val SOURCE_SHA = HimSha256("6891c2ff2ab3734a1d2768339c1f660f7093a31b97a856c82bf9b8c3c88422b5")
    val LOGICAL_COUNTS = linkedMapOf("measurements" to 2_091L, "meanSummaries" to 122L, "categoryNotes" to 15L, "footnotes" to 26L)

    fun buildOrReuse(root: File): HimGlycemicIndexEvidenceIndexBuildResult {
        Class.forName("org.sqlite.JDBC")
        val source = root.resolve(HimGlycemicIndexProductionEvidenceIndexPaths.SOURCE)
        val finalIndex = root.resolve(HimGlycemicIndexProductionEvidenceIndexPaths.FINAL_INDEX)
        require(source.isFile)
        if (finalIndex.isFile) return HimGlycemicIndexEvidenceIndexBuildResult(false, 0, HimGlycemicIndexEvidenceIndexValidator.validateReadOnly(finalIndex))
        val directory = requireNotNull(finalIndex.parentFile)
        require(directory.exists() || directory.mkdirs())
        val building = root.resolve(HimGlycemicIndexProductionEvidenceIndexPaths.BUILDING_INDEX)
        deleteBuildingFiles(building)
        try {
            build(source, building)
            HimGlycemicIndexEvidenceIndexValidator.validateWritable(building, false)
            markValidated(building)
            val validated = HimGlycemicIndexEvidenceIndexValidator.validateReadOnly(building)
            Files.move(building.toPath(), finalIndex.toPath(), StandardCopyOption.ATOMIC_MOVE)
            require(!sidecar(building, "-wal").exists() && !sidecar(building, "-shm").exists())
            return HimGlycemicIndexEvidenceIndexBuildResult(true, 1, validated)
        } catch (failure: Throwable) {
            System.err.println("GLYCEMIC_INDEX Evidence index build stopped; final index remains untouched: ${failure.message}")
            throw failure
        }
    }

    private fun build(source: File, database: File) {
        connection(database, false).use { connection ->
            configure(connection)
            HimEvidenceRetrievalIndexSchemaV1.creationStatements.forEach { sql -> connection.createStatement().use { it.execute(sql) } }
            insertMetadata(connection)
            val digest = newDigest()
            connection.prepareStatement("INSERT INTO evidence_records VALUES (?,?,?,?,?)").use { records ->
                connection.prepareStatement("INSERT INTO evidence_search(rowid,primary_name,secondary_names,taxonomy_text,ingredient_text,context_text) VALUES (?,?,?,?,?,?)").use { search ->
                    connection.autoCommit = false
                    val aggregate = InputStreamReader(GZIPInputStream(source.inputStream().buffered(1024 * 1024)), Charsets.UTF_8).use { JsonParser.parseReader(it).asJsonObject }
                    var key = 0L
                    val groups = listOf(
                        Triple("measurements", HimGlycemicIndexLogicalRecordKind.MEASUREMENT, 2_091),
                        Triple("meanSummaries", HimGlycemicIndexLogicalRecordKind.MEAN_SUMMARY, 122),
                        Triple("categoryNotes", HimGlycemicIndexLogicalRecordKind.CATEGORY_NOTE, 15),
                        Triple("footnotes", HimGlycemicIndexLogicalRecordKind.FOOTNOTE, 26),
                    )
                    groups.forEach { (name, kind, expected) ->
                        val values = aggregate.getAsJsonArray(name)
                        require(values.size() == expected)
                        values.forEachIndexed { zeroBased, value ->
                            key++
                            val record = HimGlycemicIndexEvidenceProjectionV1.fromSourceObject(value.asJsonObject, kind, zeroBased + 1L, key)
                            bindRecord(records, record); records.addBatch(); bindSearch(search, record); search.addBatch(); digest.add(record)
                            if (key % BATCH_SIZE == 0L) { records.executeBatch(); search.executeBatch(); connection.commit() }
                        }
                    }
                    if (key % BATCH_SIZE != 0L) { records.executeBatch(); search.executeBatch(); connection.commit() }
                    require(key == EXPECTED_TOTAL)
                    val logicalDigest = digest.finish()
                    connection.prepareStatement("UPDATE index_metadata SET indexed_record_count=?,evidence_record_count=?,fts_row_count=?,logical_content_sha256=? WHERE singleton_id=1 AND build_state='BUILDING'").use { update ->
                        update.setLong(1, key); update.setLong(2, key); update.setLong(3, key); update.setString(4, logicalDigest.value)
                        require(update.executeUpdate() == 1)
                    }
                    connection.commit(); connection.autoCommit = true
                }
            }
        }
    }

    private fun configure(connection: Connection) = connection.createStatement().use { statement ->
        statement.execute("PRAGMA page_size=4096"); statement.execute("PRAGMA journal_mode=OFF")
        statement.execute("PRAGMA synchronous=OFF"); statement.execute("PRAGMA temp_store=MEMORY")
        statement.execute("PRAGMA locking_mode=EXCLUSIVE")
    }

    private fun insertMetadata(connection: Connection) {
        connection.prepareStatement("INSERT INTO index_metadata VALUES (1,?,?,?,?,?,?,?, ?,0,0,0,?,'BUILDING',?,NULL)").use { statement ->
            statement.setString(1, HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION); statement.setString(2, HimGroundTruthSource.GLYCEMIC_INDEX.name)
            statement.setString(3, HimGroundTruthSource.GLYCEMIC_INDEX.artifactPath); statement.setString(4, SOURCE_SHA.value)
            statement.setLong(5, EXPECTED_TOTAL); statement.setString(6, "{\"categoryNotes\":15,\"footnotes\":26,\"meanSummaries\":122,\"measurements\":2091}")
            statement.setString(7, HimEvidenceRetrievalIndexSchemaV1.BUILD_POLICY_VERSION); statement.setString(8, HimEvidenceRetrievalIndexSchemaV1.PROJECTION_POLICY_VERSION)
            statement.setString(9, "0".repeat(64)); statement.setString(10, sqliteVersion(connection)); require(statement.executeUpdate() == 1)
        }
    }

    private fun bindRecord(statement: java.sql.PreparedStatement, record: HimEvidenceRetrievalIndexRecord) {
        statement.setLong(1, record.internalRecordKey); statement.setString(2, record.sourceRecordReference.value); statement.setString(3, record.recordKind.name)
        statement.setString(4, record.sourceNativeIdentifiersJson); statement.setString(5, record.evidenceProjection.deterministicJson)
    }
    private fun bindSearch(statement: java.sql.PreparedStatement, record: HimEvidenceRetrievalIndexRecord) {
        statement.setLong(1, record.internalRecordKey); statement.setString(2, record.searchText.primaryName); statement.setString(3, record.searchText.secondaryNames)
        statement.setString(4, record.searchText.taxonomyText); statement.setString(5, record.searchText.ingredientText); statement.setString(6, record.searchText.contextText)
    }
    private fun markValidated(database: File) = connection(database, false).use { connection -> connection.createStatement().use { require(it.executeUpdate("UPDATE index_metadata SET build_state='VALIDATED' WHERE singleton_id=1 AND build_state='BUILDING'") == 1) } }
    internal fun newDigest() = HimEvidenceRetrievalIndexDigest.newAccumulator(
        HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION, HimGroundTruthSource.GLYCEMIC_INDEX, SOURCE_SHA, LOGICAL_COUNTS,
        HimEvidenceRetrievalIndexSchemaV1.BUILD_POLICY_VERSION, HimEvidenceRetrievalIndexSchemaV1.PROJECTION_POLICY_VERSION,
    )
    internal fun connection(database: File, readOnly: Boolean): Connection {
        val path = database.toPath().toAbsolutePath(); return DriverManager.getConnection(if (readOnly) "jdbc:sqlite:file:$path?mode=ro" else "jdbc:sqlite:$path")
    }
    internal fun sqliteVersion(connection: Connection): String = connection.createStatement().use { statement -> statement.executeQuery("select sqlite_version()").use { result -> require(result.next()); result.getString(1) } }
    private fun deleteBuildingFiles(database: File) { listOf(database, sidecar(database, "-journal"), sidecar(database, "-wal"), sidecar(database, "-shm")).forEach { if (it.exists()) require(it.delete()) } }
    private fun sidecar(database: File, suffix: String) = File(database.absolutePath + suffix)
}

object HimGlycemicIndexEvidenceIndexValidator {
    fun validateReadOnly(database: File) = HimGlycemicIndexProductionEvidenceIndexTool.connection(database, true).use { validate(it, true) }
    fun validateWritable(database: File, requireValidated: Boolean) = HimGlycemicIndexProductionEvidenceIndexTool.connection(database, false).use { validate(it, requireValidated) }

    private fun validate(connection: Connection, requireValidated: Boolean): HimGlycemicIndexEvidenceIndexValidation {
        val metadata = readMetadata(connection)
        require(metadata.schemaVersion == HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION && metadata.source == HimGroundTruthSource.GLYCEMIC_INDEX)
        require(metadata.sourceArtifactSha256 == HimGlycemicIndexProductionEvidenceIndexTool.SOURCE_SHA)
        require(metadata.sourceRecordCount == HimGlycemicIndexProductionEvidenceIndexTool.EXPECTED_TOTAL)
        require(metadata.logicalRecordCounts == HimGlycemicIndexProductionEvidenceIndexTool.LOGICAL_COUNTS.toSortedMap())
        require(metadata.indexedRecordCount == HimGlycemicIndexProductionEvidenceIndexTool.EXPECTED_TOTAL && metadata.evidenceRecordCount == HimGlycemicIndexProductionEvidenceIndexTool.EXPECTED_TOTAL && metadata.ftsRowCount == HimGlycemicIndexProductionEvidenceIndexTool.EXPECTED_TOTAL)
        require(metadata.buildState == if (requireValidated) HimEvidenceRetrievalIndexBuildState.VALIDATED else HimEvidenceRetrievalIndexBuildState.BUILDING)
        require(scalar(connection, "SELECT count(*) FROM evidence_records") == HimGlycemicIndexProductionEvidenceIndexTool.EXPECTED_TOTAL)
        require(scalar(connection, "SELECT count(*) FROM evidence_search") == HimGlycemicIndexProductionEvidenceIndexTool.EXPECTED_TOTAL)
        require(scalar(connection, "SELECT count(*) FROM evidence_search s JOIN evidence_records r ON r.internal_record_key=s.rowid") == HimGlycemicIndexProductionEvidenceIndexTool.EXPECTED_TOTAL)

        val digest = HimGlycemicIndexProductionEvidenceIndexTool.newDigest()
        val kinds = linkedMapOf<HimEvidenceRecordKind, Long>()
        val statuses = linkedSetOf<String>()
        var representative = ""
        var rows = 0L
        connection.createStatement().use { statement -> statement.executeQuery("SELECT * FROM evidence_records ORDER BY internal_record_key").use { result ->
            while (result.next()) {
                rows++; require(result.getLong("internal_record_key") == rows)
                val record = HimGlycemicIndexEvidenceProjectionV1.fromProjectionJson(result.getString("evidence_projection_json"), rows)
                require(record.sourceRecordReference.value == result.getString("source_record_reference") && record.recordKind.name == result.getString("record_kind"))
                require(record.sourceNativeIdentifiersJson == result.getString("source_native_identifiers_json"))
                kinds[record.recordKind] = kinds.getOrDefault(record.recordKind, 0) + 1
                val projection = JsonParser.parseString(record.evidenceProjection.deterministicJson).asJsonObject
                projection.entrySet().forEach { (_, value) -> if (value.isJsonObject && value.asJsonObject.has("status")) statuses += value.asJsonObject.get("status").asString }
                if (record.recordKind == HimEvidenceRecordKind.GI_MEASUREMENT && projection.getAsJsonObject("foodItem").get("lexicalValue").asString.contains("Baguette")) representative = record.sourceRecordReference.value
                digest.add(record)
            }
        } }
        require(rows == HimGlycemicIndexProductionEvidenceIndexTool.EXPECTED_TOTAL)
        require(kinds == mapOf(
            HimEvidenceRecordKind.GI_MEASUREMENT to 2_091L, HimEvidenceRecordKind.GI_MEAN_SUMMARY to 122L,
            HimEvidenceRecordKind.GI_CATEGORY_NOTE to 15L, HimEvidenceRecordKind.GI_FOOTNOTE to 26L,
        ))
        require(statuses.containsAll(setOf("PRESENT", "SOURCE_MISSING", "UNRESOLVED")) && representative.isNotEmpty())
        val recomputed = digest.finish(); require(recomputed == metadata.logicalContentSha256)
        if (!requireValidated) connection.createStatement().use { it.execute("INSERT INTO evidence_search(evidence_search) VALUES('integrity-check')") }
        val integrity = connection.createStatement().use { statement -> statement.executeQuery("PRAGMA integrity_check").use { result -> require(result.next()); result.getString(1) } }
        require(integrity == "ok")
        if (requireValidated) require(HimEvidenceRetrievalIndexValidator.runtimeEligibility(metadata, HimExpectedEvidenceRetrievalIndex(HimGroundTruthSource.GLYCEMIC_INDEX, HimGlycemicIndexProductionEvidenceIndexTool.SOURCE_SHA)) == HimEvidenceRetrievalIndexEligibility.Ready)
        return HimGlycemicIndexEvidenceIndexValidation(metadata, recomputed, integrity, kinds, statuses, representative)
    }

    private fun readMetadata(connection: Connection): HimEvidenceRetrievalIndexMetadata = connection.createStatement().use { statement -> statement.executeQuery("SELECT * FROM index_metadata WHERE singleton_id=1").use { result ->
        require(result.next())
        val counts = JsonParser.parseString(result.getString("logical_record_counts_json")).asJsonObject.entrySet().associate { it.key to it.value.asLong }
        HimEvidenceRetrievalIndexMetadata(
            result.getString("schema_version"), HimGroundTruthSource.valueOf(result.getString("source")), result.getString("source_artifact_path"), HimSha256(result.getString("source_artifact_sha256")),
            result.getLong("source_record_count"), HimEvidenceRetrievalIndexMetadata.sortedLogicalCounts(counts), result.getString("index_build_policy_version"), result.getString("evidence_projection_policy_version"),
            result.getLong("indexed_record_count"), result.getLong("evidence_record_count"), result.getLong("fts_row_count"), HimSha256(result.getString("logical_content_sha256")),
            HimEvidenceRetrievalIndexBuildState.valueOf(result.getString("build_state")), result.getString("sqlite_runtime_version"), result.getString("sqlite_file_sha256")?.let(::HimSha256),
        )
    } }
    private fun scalar(connection: Connection, sql: String): Long = connection.createStatement().use { statement -> statement.executeQuery(sql).use { result -> require(result.next()); result.getLong(1) } }
}

class HimGlycemicIndexSqliteEvidenceRetrievalStore private constructor(private val database: File) {
    fun search(query: String, limit: HimEvidenceSearchLimit): List<HimEvidenceSearchResult> {
        val ftsQuery = Regex("[\\p{L}\\p{N}]+").findAll(query.lowercase()).joinToString(" ") { "\"${it.value}\"" }; require(ftsQuery.isNotBlank())
        return HimGlycemicIndexProductionEvidenceIndexTool.connection(database, true).use { connection -> connection.prepareStatement(HimEvidenceRetrievalIndexSchemaV1.SEARCH_SQL).use { statement ->
            statement.setString(1, ftsQuery); statement.setInt(2, limit.value); statement.executeQuery().use { result -> buildList { while (result.next()) add(HimEvidenceSearchResult(
                HimGroundTruthSource.GLYCEMIC_INDEX, HimEvidenceRecordReference.parse(HimGroundTruthSource.GLYCEMIC_INDEX, result.getString("source_record_reference")),
                HimEvidenceRecordKind.valueOf(result.getString("record_kind")), size + 1, HimEvidenceProjection(result.getString("evidence_projection_json")),
            )) } }
        } }
    }
    fun fetch(reference: HimEvidenceRecordReference): HimEvidenceSearchResult? {
        require(reference.source == HimGroundTruthSource.GLYCEMIC_INDEX)
        return HimGlycemicIndexProductionEvidenceIndexTool.connection(database, true).use { connection -> connection.prepareStatement(HimEvidenceRetrievalIndexSchemaV1.EXACT_FETCH_SQL).use { statement ->
            statement.setString(1, reference.value); statement.executeQuery().use { result -> if (!result.next()) null else HimEvidenceSearchResult(
                HimGroundTruthSource.GLYCEMIC_INDEX, reference, HimEvidenceRecordKind.valueOf(result.getString("record_kind")), 1, HimEvidenceProjection(result.getString("evidence_projection_json")),
            ) }
        } }
    }
    companion object { fun openAfterValidation(database: File, validation: HimGlycemicIndexEvidenceIndexValidation): HimGlycemicIndexSqliteEvidenceRetrievalStore { require(validation.metadata.buildState == HimEvidenceRetrievalIndexBuildState.VALIDATED); return HimGlycemicIndexSqliteEvidenceRetrievalStore(database) } }
}
