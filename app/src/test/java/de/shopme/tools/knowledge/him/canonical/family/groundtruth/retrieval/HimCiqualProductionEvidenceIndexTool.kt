package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonObject
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

object HimCiqualProductionEvidenceIndexPaths {
    const val SOURCE = "data/sources/ciqual/optimized/ciqual-him-final-source.json.gz"
    const val FINAL_INDEX = "data/sources/ciqual/index/ciqual-him-evidence-index.v1.sqlite"
    const val BUILDING_INDEX = "data/sources/ciqual/index/.ciqual-him-evidence-index.v1.sqlite.building"
    const val REPORT = "build/knowledge/reports/him/retrieval/him-f3d2c3-ciqual-production-evidence-index-build.txt"
}

data class HimCiqualEvidenceIndexValidation(
    val metadata: HimEvidenceRetrievalIndexMetadata,
    val recomputedLogicalDigest: HimSha256,
    val integrityCheck: String,
    val kindCounts: Map<HimEvidenceRecordKind, Long>,
    val lexicalSamples: Map<String, String>,
    val missingnessPreserved: Boolean,
    val duplicatedInfoodsCode: String,
    val duplicatedInfoodsConstCodes: List<String>,
    val duplicatedCitation: String,
    val duplicatedCitationSourceCodes: List<String>,
)

data class HimCiqualEvidenceIndexBuildResult(
    val builtNow: Boolean,
    val fullSourceReads: Int,
    val validation: HimCiqualEvidenceIndexValidation,
)

object HimCiqualProductionEvidenceIndexTool {
    const val EXPECTED_TOTAL = 5_674L
    const val BATCH_SIZE = 500
    val SOURCE_SHA = HimSha256("807d16c222f0e812831c10bdd89f1a5ebbc74c95e8a4944723db486add96dcff")
    val LOGICAL_COUNTS = linkedMapOf("foods" to 3_484L, "taxonomy" to 138L, "constituents" to 74L, "sources" to 1_978L)

    fun buildOrReuse(root: File): HimCiqualEvidenceIndexBuildResult {
        Class.forName("org.sqlite.JDBC")
        val source = root.resolve(HimCiqualProductionEvidenceIndexPaths.SOURCE)
        val finalIndex = root.resolve(HimCiqualProductionEvidenceIndexPaths.FINAL_INDEX)
        require(source.isFile)
        if (finalIndex.isFile) return HimCiqualEvidenceIndexBuildResult(false, 0, HimCiqualEvidenceIndexValidator.validateReadOnly(finalIndex))
        val directory = requireNotNull(finalIndex.parentFile)
        require(directory.exists() || directory.mkdirs())
        val building = root.resolve(HimCiqualProductionEvidenceIndexPaths.BUILDING_INDEX)
        deleteBuildingFiles(building)
        try {
            build(source, building)
            val beforeState = HimCiqualEvidenceIndexValidator.validateWritable(building, false)
            require(beforeState.metadata.buildState == HimEvidenceRetrievalIndexBuildState.BUILDING)
            markValidated(building)
            val validated = HimCiqualEvidenceIndexValidator.validateReadOnly(building)
            Files.move(building.toPath(), finalIndex.toPath(), StandardCopyOption.ATOMIC_MOVE)
            require(!sidecar(building, "-wal").exists() && !sidecar(building, "-shm").exists())
            return HimCiqualEvidenceIndexBuildResult(true, 1, validated)
        } catch (failure: Throwable) {
            System.err.println("CIQUAL Evidence index build stopped; final index remains untouched: ${failure.message}")
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
                connection.prepareStatement(
                    "INSERT INTO evidence_search(rowid,primary_name,secondary_names,taxonomy_text,ingredient_text,context_text) VALUES (?,?,?,?,?,?)"
                ).use { search ->
                    connection.autoCommit = false
                    val aggregate = InputStreamReader(GZIPInputStream(source.inputStream().buffered(1024 * 1024)), Charsets.UTF_8).use {
                        JsonParser.parseReader(it).asJsonObject
                    }
                    var key = 0L
                    val groups = listOf(
                        Triple("foods", HimCiqualLogicalRecordKind.FOOD, 3_484),
                        Triple("taxonomy", HimCiqualLogicalRecordKind.TAXONOMY, 138),
                        Triple("constituents", HimCiqualLogicalRecordKind.CONSTITUENT, 74),
                        Triple("sources", HimCiqualLogicalRecordKind.SOURCE, 1_978),
                    )
                    groups.forEach { (name, kind, expected) ->
                        val values = aggregate.getAsJsonArray(name)
                        require(values.size() == expected)
                        values.forEach { element ->
                            key++
                            val record = HimCiqualEvidenceProjectionV1.fromSourceObject(element.asJsonObject, kind, key)
                            bindRecord(records, record); records.addBatch()
                            bindSearch(search, record); search.addBatch()
                            digest.add(record)
                            if (key % BATCH_SIZE == 0L) { records.executeBatch(); search.executeBatch(); connection.commit() }
                        }
                    }
                    if (key % BATCH_SIZE != 0L) { records.executeBatch(); search.executeBatch(); connection.commit() }
                    require(key == EXPECTED_TOTAL)
                    val logicalDigest = digest.finish()
                    connection.prepareStatement(
                        "UPDATE index_metadata SET indexed_record_count=?,evidence_record_count=?,fts_row_count=?,logical_content_sha256=? WHERE singleton_id=1 AND build_state='BUILDING'"
                    ).use { update ->
                        update.setLong(1, key); update.setLong(2, key); update.setLong(3, key); update.setString(4, logicalDigest.value)
                        require(update.executeUpdate() == 1)
                    }
                    connection.commit(); connection.autoCommit = true
                }
            }
        }
    }

    private fun configure(connection: Connection) = connection.createStatement().use { statement ->
        statement.execute("PRAGMA page_size=4096")
        statement.execute("PRAGMA journal_mode=OFF")
        statement.execute("PRAGMA synchronous=OFF")
        statement.execute("PRAGMA temp_store=MEMORY")
        statement.execute("PRAGMA locking_mode=EXCLUSIVE")
    }

    private fun insertMetadata(connection: Connection) {
        connection.prepareStatement("INSERT INTO index_metadata VALUES (1,?,?,?,?,?,?,?, ?,0,0,0,?,'BUILDING',?,NULL)").use { statement ->
            statement.setString(1, HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION)
            statement.setString(2, HimGroundTruthSource.CIQUAL.name)
            statement.setString(3, HimGroundTruthSource.CIQUAL.artifactPath)
            statement.setString(4, SOURCE_SHA.value)
            statement.setLong(5, EXPECTED_TOTAL)
            statement.setString(6, "{\"constituents\":74,\"foods\":3484,\"sources\":1978,\"taxonomy\":138}")
            statement.setString(7, HimEvidenceRetrievalIndexSchemaV1.BUILD_POLICY_VERSION)
            statement.setString(8, HimEvidenceRetrievalIndexSchemaV1.PROJECTION_POLICY_VERSION)
            statement.setString(9, "0".repeat(64))
            statement.setString(10, sqliteVersion(connection))
            require(statement.executeUpdate() == 1)
        }
    }

    private fun bindRecord(statement: java.sql.PreparedStatement, record: HimEvidenceRetrievalIndexRecord) {
        statement.setLong(1, record.internalRecordKey); statement.setString(2, record.sourceRecordReference.value)
        statement.setString(3, record.recordKind.name); statement.setString(4, record.sourceNativeIdentifiersJson)
        statement.setString(5, record.evidenceProjection.deterministicJson)
    }

    private fun bindSearch(statement: java.sql.PreparedStatement, record: HimEvidenceRetrievalIndexRecord) {
        statement.setLong(1, record.internalRecordKey); statement.setString(2, record.searchText.primaryName)
        statement.setString(3, record.searchText.secondaryNames); statement.setString(4, record.searchText.taxonomyText)
        statement.setString(5, record.searchText.ingredientText); statement.setString(6, record.searchText.contextText)
    }

    private fun markValidated(database: File) = connection(database, false).use { connection ->
        connection.createStatement().use { require(it.executeUpdate("UPDATE index_metadata SET build_state='VALIDATED' WHERE singleton_id=1 AND build_state='BUILDING'") == 1) }
    }

    internal fun newDigest() = HimEvidenceRetrievalIndexDigest.newAccumulator(
        HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION, HimGroundTruthSource.CIQUAL, SOURCE_SHA, LOGICAL_COUNTS,
        HimEvidenceRetrievalIndexSchemaV1.BUILD_POLICY_VERSION, HimEvidenceRetrievalIndexSchemaV1.PROJECTION_POLICY_VERSION,
    )

    internal fun connection(database: File, readOnly: Boolean): Connection {
        val path = database.toPath().toAbsolutePath()
        return DriverManager.getConnection(if (readOnly) "jdbc:sqlite:file:$path?mode=ro" else "jdbc:sqlite:$path")
    }

    internal fun sqliteVersion(connection: Connection): String = connection.createStatement().use { statement ->
        statement.executeQuery("select sqlite_version()").use { result -> require(result.next()); result.getString(1) }
    }

    private fun deleteBuildingFiles(database: File) {
        listOf(database, sidecar(database, "-journal"), sidecar(database, "-wal"), sidecar(database, "-shm"))
            .forEach { if (it.exists()) require(it.delete()) }
    }
    private fun sidecar(database: File, suffix: String) = File(database.absolutePath + suffix)
}

object HimCiqualEvidenceIndexValidator {
    fun validateReadOnly(database: File) = HimCiqualProductionEvidenceIndexTool.connection(database, true).use { validate(it, true) }
    fun validateWritable(database: File, requireValidated: Boolean) = HimCiqualProductionEvidenceIndexTool.connection(database, false).use { validate(it, requireValidated) }

    private fun validate(connection: Connection, requireValidated: Boolean): HimCiqualEvidenceIndexValidation {
        val metadata = readMetadata(connection)
        require(metadata.schemaVersion == HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION)
        require(metadata.source == HimGroundTruthSource.CIQUAL)
        require(metadata.sourceArtifactSha256 == HimCiqualProductionEvidenceIndexTool.SOURCE_SHA)
        require(metadata.sourceRecordCount == HimCiqualProductionEvidenceIndexTool.EXPECTED_TOTAL)
        require(metadata.logicalRecordCounts == HimCiqualProductionEvidenceIndexTool.LOGICAL_COUNTS.toSortedMap())
        require(metadata.indexedRecordCount == HimCiqualProductionEvidenceIndexTool.EXPECTED_TOTAL)
        require(metadata.evidenceRecordCount == HimCiqualProductionEvidenceIndexTool.EXPECTED_TOTAL)
        require(metadata.ftsRowCount == HimCiqualProductionEvidenceIndexTool.EXPECTED_TOTAL)
        require(metadata.buildState == if (requireValidated) HimEvidenceRetrievalIndexBuildState.VALIDATED else HimEvidenceRetrievalIndexBuildState.BUILDING)
        require(scalar(connection, "SELECT count(*) FROM evidence_records") == HimCiqualProductionEvidenceIndexTool.EXPECTED_TOTAL)
        require(scalar(connection, "SELECT count(*) FROM evidence_search") == HimCiqualProductionEvidenceIndexTool.EXPECTED_TOTAL)
        require(scalar(connection, "SELECT count(*) FROM evidence_search s JOIN evidence_records r ON r.internal_record_key=s.rowid") == HimCiqualProductionEvidenceIndexTool.EXPECTED_TOTAL)

        val digest = HimCiqualProductionEvidenceIndexTool.newDigest()
        val kinds = linkedMapOf<HimEvidenceRecordKind, Long>()
        val infoods = linkedMapOf<String, MutableList<String>>()
        val citations = linkedMapOf<String, MutableList<String>>()
        val lexical = linkedMapOf<String, String>()
        var missingness = false
        var rows = 0L
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT * FROM evidence_records ORDER BY internal_record_key").use { result ->
                while (result.next()) {
                    rows++
                    require(result.getLong("internal_record_key") == rows)
                    val record = HimCiqualEvidenceProjectionV1.fromProjectionJson(result.getString("evidence_projection_json"), rows)
                    require(record.sourceRecordReference.value == result.getString("source_record_reference"))
                    require(record.recordKind.name == result.getString("record_kind"))
                    require(record.sourceNativeIdentifiersJson == result.getString("source_native_identifiers_json"))
                    kinds[record.recordKind] = kinds.getOrDefault(record.recordKind, 0) + 1
                    val projection = JsonParser.parseString(record.evidenceProjection.deterministicJson).asJsonObject
                    if (record.recordKind == HimEvidenceRecordKind.CIQUAL_FOOD) {
                        projection.getAsJsonArray("compositions").forEach { composition ->
                            val objectValue = composition.asJsonObject
                            val value = objectValue.get("teneurLexical").asString
                            if (value in REQUIRED_LEXICALS && value !in lexical) lexical[value] = record.sourceRecordReference.value
                            if (objectValue.toString().contains("missingAttributeValue")) missingness = true
                        }
                    }
                    if (record.recordKind == HimEvidenceRecordKind.CIQUAL_CONSTITUENT) {
                        val infoodsCode = projection.getAsJsonObject("infoodsCode").get("lexicalValue").asString
                        if (infoodsCode.isNotEmpty()) infoods.getOrPut(infoodsCode, ::mutableListOf).add(projection.get("constCode").asString)
                    }
                    if (record.recordKind == HimEvidenceRecordKind.CIQUAL_SOURCE) {
                        val citation = projection.getAsJsonObject("citation").get("lexicalValue").asString
                        if (citation.isNotEmpty()) citations.getOrPut(citation, ::mutableListOf).add(projection.get("sourceCode").asString)
                    }
                    digest.add(record)
                }
            }
        }
        require(rows == HimCiqualProductionEvidenceIndexTool.EXPECTED_TOTAL)
        val expectedKinds = mapOf(
            HimEvidenceRecordKind.CIQUAL_FOOD to 3_484L, HimEvidenceRecordKind.CIQUAL_TAXONOMY to 138L,
            HimEvidenceRecordKind.CIQUAL_CONSTITUENT to 74L, HimEvidenceRecordKind.CIQUAL_SOURCE to 1_978L,
        )
        require(kinds == expectedKinds)
        require(lexical.keys == REQUIRED_LEXICALS)
        require(missingness)
        val infoodsDuplicate = requireNotNull(infoods.entries.firstOrNull { it.value.size > 1 })
        val citationDuplicate = requireNotNull(citations.entries.firstOrNull { it.value.size > 1 })
        val recomputed = digest.finish()
        require(recomputed == metadata.logicalContentSha256)
        if (!requireValidated) connection.createStatement().use { it.execute("INSERT INTO evidence_search(evidence_search) VALUES('integrity-check')") }
        val integrity = connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA integrity_check").use { result -> require(result.next()); result.getString(1) }
        }
        require(integrity == "ok")
        if (requireValidated) require(
            HimEvidenceRetrievalIndexValidator.runtimeEligibility(
                metadata, HimExpectedEvidenceRetrievalIndex(HimGroundTruthSource.CIQUAL, HimCiqualProductionEvidenceIndexTool.SOURCE_SHA)
            ) == HimEvidenceRetrievalIndexEligibility.Ready
        )
        return HimCiqualEvidenceIndexValidation(
            metadata, recomputed, integrity, kinds, lexical, missingness,
            infoodsDuplicate.key, infoodsDuplicate.value, citationDuplicate.key, citationDuplicate.value,
        )
    }

    private fun readMetadata(connection: Connection): HimEvidenceRetrievalIndexMetadata = connection.createStatement().use { statement ->
        statement.executeQuery("SELECT * FROM index_metadata WHERE singleton_id=1").use { result ->
            require(result.next())
            val counts = JsonParser.parseString(result.getString("logical_record_counts_json")).asJsonObject.entrySet()
                .associate { it.key to it.value.asLong }
            HimEvidenceRetrievalIndexMetadata(
                result.getString("schema_version"), HimGroundTruthSource.valueOf(result.getString("source")), result.getString("source_artifact_path"),
                HimSha256(result.getString("source_artifact_sha256")), result.getLong("source_record_count"), HimEvidenceRetrievalIndexMetadata.sortedLogicalCounts(counts),
                result.getString("index_build_policy_version"), result.getString("evidence_projection_policy_version"), result.getLong("indexed_record_count"),
                result.getLong("evidence_record_count"), result.getLong("fts_row_count"), HimSha256(result.getString("logical_content_sha256")),
                HimEvidenceRetrievalIndexBuildState.valueOf(result.getString("build_state")), result.getString("sqlite_runtime_version"),
                result.getString("sqlite_file_sha256")?.let(::HimSha256),
            )
        }
    }

    private fun scalar(connection: Connection, sql: String): Long = connection.createStatement().use { statement ->
        statement.executeQuery(sql).use { result -> require(result.next()); result.getLong(1) }
    }

    private val REQUIRED_LEXICALS = linkedSetOf("traces", "< 2,2", "< 0,1", "-")
}

class HimCiqualSqliteEvidenceRetrievalStore private constructor(private val database: File) {
    fun search(query: String, limit: HimEvidenceSearchLimit): List<HimEvidenceSearchResult> {
        val ftsQuery = Regex("[\\p{L}\\p{N}]+").findAll(query.lowercase()).joinToString(" ") { "\"${it.value}\"" }
        require(ftsQuery.isNotBlank())
        return HimCiqualProductionEvidenceIndexTool.connection(database, true).use { connection ->
            connection.prepareStatement(HimEvidenceRetrievalIndexSchemaV1.SEARCH_SQL).use { statement ->
                statement.setString(1, ftsQuery); statement.setInt(2, limit.value)
                statement.executeQuery().use { result -> buildList {
                    while (result.next()) add(HimEvidenceSearchResult(
                        HimGroundTruthSource.CIQUAL,
                        HimEvidenceRecordReference.parse(HimGroundTruthSource.CIQUAL, result.getString("source_record_reference")),
                        HimEvidenceRecordKind.valueOf(result.getString("record_kind")), size + 1,
                        HimEvidenceProjection(result.getString("evidence_projection_json")),
                    ))
                } }
            }
        }
    }

    fun fetch(reference: HimEvidenceRecordReference): HimEvidenceSearchResult? {
        require(reference.source == HimGroundTruthSource.CIQUAL)
        return HimCiqualProductionEvidenceIndexTool.connection(database, true).use { connection ->
            connection.prepareStatement(HimEvidenceRetrievalIndexSchemaV1.EXACT_FETCH_SQL).use { statement ->
                statement.setString(1, reference.value)
                statement.executeQuery().use { result -> if (!result.next()) null else HimEvidenceSearchResult(
                    HimGroundTruthSource.CIQUAL, reference, HimEvidenceRecordKind.valueOf(result.getString("record_kind")), 1,
                    HimEvidenceProjection(result.getString("evidence_projection_json")),
                ) }
            }
        }
    }

    companion object {
        fun openAfterValidation(database: File, validation: HimCiqualEvidenceIndexValidation): HimCiqualSqliteEvidenceRetrievalStore {
            require(validation.metadata.buildState == HimEvidenceRetrievalIndexBuildState.VALIDATED)
            return HimCiqualSqliteEvidenceRetrievalStore(database)
        }
    }
}
