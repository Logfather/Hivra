package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonParser
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.sql.Connection
import java.sql.DriverManager
import java.util.zip.GZIPInputStream

object HimAgribalyseProductionEvidenceIndexPaths {
    const val SOURCE = "data/sources/agribalyse/optimized/agribalyse-him-final-source.jsonl.gz"
    const val FINAL_INDEX = "data/sources/agribalyse/index/agribalyse-him-evidence-index.v1.sqlite"
    const val BUILDING_INDEX = "data/sources/agribalyse/index/.agribalyse-him-evidence-index.v1.sqlite.building"
    const val REPORT = "build/knowledge/reports/him/retrieval/him-f3d2c2-agribalyse-production-evidence-index-build.txt"
}

data class HimAgribalyseEvidenceIndexValidation(
    val metadata: HimEvidenceRetrievalIndexMetadata,
    val recomputedLogicalDigest: HimSha256,
    val integrityCheck: String,
    val firstReference: String,
    val lastReference: String,
    val distinctAgbCodes: Int,
    val duplicateAgbGroups: Int,
    val maximumMultiplicity: Int,
)

data class HimAgribalyseEvidenceIndexBuildResult(
    val builtNow: Boolean,
    val fullSourceScans: Int,
    val validation: HimAgribalyseEvidenceIndexValidation,
)

object HimAgribalyseProductionEvidenceIndexTool {
    const val EXPECTED_RECORDS = 2_458L
    const val EXPECTED_DISTINCT_AGB_CODES = 2_451
    const val EXPECTED_DUPLICATE_GROUPS = 7
    const val BATCH_SIZE = 500
    val SOURCE_SHA = HimSha256("9068c89fa887ef087e87dcc51f758dd29e9623b93d9bd0a2277a4faae57c2297")

    fun buildOrReuse(projectRoot: File): HimAgribalyseEvidenceIndexBuildResult {
        Class.forName("org.sqlite.JDBC")
        val source = projectRoot.resolve(HimAgribalyseProductionEvidenceIndexPaths.SOURCE)
        val finalIndex = projectRoot.resolve(HimAgribalyseProductionEvidenceIndexPaths.FINAL_INDEX)
        require(source.isFile)
        if (finalIndex.isFile) {
            return HimAgribalyseEvidenceIndexBuildResult(false, 0, HimAgribalyseEvidenceIndexValidator.validateReadOnly(finalIndex))
        }
        val directory = requireNotNull(finalIndex.parentFile)
        require(directory.exists() || directory.mkdirs())
        val building = projectRoot.resolve(HimAgribalyseProductionEvidenceIndexPaths.BUILDING_INDEX)
        deleteBuildingFiles(building)
        try {
            build(source, building)
            val buildingValidation = HimAgribalyseEvidenceIndexValidator.validateWritable(building, false)
            require(buildingValidation.metadata.buildState == HimEvidenceRetrievalIndexBuildState.BUILDING)
            require(buildingValidation.recomputedLogicalDigest == buildingValidation.metadata.logicalContentSha256)
            markValidated(building)
            val validated = HimAgribalyseEvidenceIndexValidator.validateReadOnly(building)
            Files.move(building.toPath(), finalIndex.toPath(), StandardCopyOption.ATOMIC_MOVE)
            require(!sidecar(building, "-wal").exists() && !sidecar(building, "-shm").exists())
            return HimAgribalyseEvidenceIndexBuildResult(true, 1, validated)
        } catch (failure: Throwable) {
            System.err.println("AGRIBALYSE Evidence index build stopped; final index remains untouched: ${failure.message}")
            throw failure
        }
    }

    private fun build(source: File, database: File) {
        connection(database, false).use { connection ->
            configure(connection)
            HimEvidenceRetrievalIndexSchemaV1.creationStatements.forEach { sql ->
                connection.createStatement().use { it.execute(sql) }
            }
            insertMetadata(connection)
            val digest = newDigest()
            connection.prepareStatement("INSERT INTO evidence_records VALUES (?, ?, ?, ?, ?)").use { records ->
                connection.prepareStatement(
                    "INSERT INTO evidence_search(rowid,primary_name,secondary_names,taxonomy_text,ingredient_text,context_text) VALUES (?, ?, ?, ?, ?, ?)"
                ).use { search ->
                    connection.autoCommit = false
                    var ordinal = 0L
                    BufferedReader(
                        InputStreamReader(GZIPInputStream(source.inputStream().buffered(1024 * 1024)), Charsets.UTF_8),
                        1024 * 1024,
                    ).useLines { lines ->
                        lines.forEach { line ->
                            ordinal++
                            val record = HimAgribalyseEvidenceProjectionV1.fromOptimizedSourceLine(line, ordinal)
                            bindRecord(records, record)
                            records.addBatch()
                            bindSearch(search, record)
                            search.addBatch()
                            digest.add(record)
                            if (ordinal % BATCH_SIZE == 0L) {
                                records.executeBatch(); search.executeBatch(); connection.commit()
                            }
                        }
                    }
                    if (ordinal % BATCH_SIZE != 0L) {
                        records.executeBatch(); search.executeBatch(); connection.commit()
                    }
                    require(ordinal == EXPECTED_RECORDS)
                    val logicalDigest = digest.finish()
                    connection.prepareStatement(
                        "UPDATE index_metadata SET indexed_record_count=?,evidence_record_count=?,fts_row_count=?,logical_content_sha256=? WHERE singleton_id=1 AND build_state='BUILDING'"
                    ).use { update ->
                        update.setLong(1, ordinal); update.setLong(2, ordinal); update.setLong(3, ordinal)
                        update.setString(4, logicalDigest.value)
                        require(update.executeUpdate() == 1)
                    }
                    connection.commit()
                    connection.autoCommit = true
                }
            }
        }
    }

    private fun configure(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute("PRAGMA page_size=4096")
            statement.execute("PRAGMA journal_mode=OFF")
            statement.execute("PRAGMA synchronous=OFF")
            statement.execute("PRAGMA temp_store=MEMORY")
            statement.execute("PRAGMA locking_mode=EXCLUSIVE")
        }
    }

    private fun insertMetadata(connection: Connection) {
        connection.prepareStatement(
            "INSERT INTO index_metadata VALUES (1,?,?,?,?,?,?,?, ?,0,0,0,?,'BUILDING',?,NULL)"
        ).use { statement ->
            statement.setString(1, HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION)
            statement.setString(2, HimGroundTruthSource.AGRIBALYSE.name)
            statement.setString(3, HimGroundTruthSource.AGRIBALYSE.artifactPath)
            statement.setString(4, SOURCE_SHA.value)
            statement.setLong(5, EXPECTED_RECORDS)
            statement.setString(6, "{\"records\":$EXPECTED_RECORDS}")
            statement.setString(7, HimEvidenceRetrievalIndexSchemaV1.BUILD_POLICY_VERSION)
            statement.setString(8, HimEvidenceRetrievalIndexSchemaV1.PROJECTION_POLICY_VERSION)
            statement.setString(9, "0".repeat(64))
            statement.setString(10, sqliteVersion(connection))
            require(statement.executeUpdate() == 1)
        }
    }

    private fun bindRecord(statement: java.sql.PreparedStatement, record: HimEvidenceRetrievalIndexRecord) {
        statement.setLong(1, record.internalRecordKey)
        statement.setString(2, record.sourceRecordReference.value)
        statement.setString(3, record.recordKind.name)
        statement.setString(4, record.sourceNativeIdentifiersJson)
        statement.setString(5, record.evidenceProjection.deterministicJson)
    }

    private fun bindSearch(statement: java.sql.PreparedStatement, record: HimEvidenceRetrievalIndexRecord) {
        statement.setLong(1, record.internalRecordKey)
        statement.setString(2, record.searchText.primaryName)
        statement.setString(3, record.searchText.secondaryNames)
        statement.setString(4, record.searchText.taxonomyText)
        statement.setString(5, record.searchText.ingredientText)
        statement.setString(6, record.searchText.contextText)
    }

    private fun markValidated(database: File) {
        connection(database, false).use { connection ->
            connection.createStatement().use { statement ->
                require(statement.executeUpdate("UPDATE index_metadata SET build_state='VALIDATED' WHERE singleton_id=1 AND build_state='BUILDING'") == 1)
            }
        }
    }

    internal fun newDigest() = HimEvidenceRetrievalIndexDigest.newAccumulator(
        HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION,
        HimGroundTruthSource.AGRIBALYSE,
        SOURCE_SHA,
        mapOf("records" to EXPECTED_RECORDS),
        HimEvidenceRetrievalIndexSchemaV1.BUILD_POLICY_VERSION,
        HimEvidenceRetrievalIndexSchemaV1.PROJECTION_POLICY_VERSION,
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

object HimAgribalyseEvidenceIndexValidator {
    fun validateReadOnly(database: File): HimAgribalyseEvidenceIndexValidation =
        HimAgribalyseProductionEvidenceIndexTool.connection(database, true).use { validate(it, true) }

    fun validateWritable(database: File, requireValidated: Boolean): HimAgribalyseEvidenceIndexValidation =
        HimAgribalyseProductionEvidenceIndexTool.connection(database, false).use { validate(it, requireValidated) }

    private fun validate(connection: Connection, requireValidated: Boolean): HimAgribalyseEvidenceIndexValidation {
        val metadata = readMetadata(connection)
        require(metadata.schemaVersion == HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION)
        require(metadata.source == HimGroundTruthSource.AGRIBALYSE)
        require(metadata.sourceArtifactSha256 == HimAgribalyseProductionEvidenceIndexTool.SOURCE_SHA)
        require(metadata.sourceRecordCount == HimAgribalyseProductionEvidenceIndexTool.EXPECTED_RECORDS)
        require(metadata.logicalRecordCounts == sortedMapOf("records" to HimAgribalyseProductionEvidenceIndexTool.EXPECTED_RECORDS))
        require(metadata.indexedRecordCount == HimAgribalyseProductionEvidenceIndexTool.EXPECTED_RECORDS)
        require(metadata.evidenceRecordCount == HimAgribalyseProductionEvidenceIndexTool.EXPECTED_RECORDS)
        require(metadata.ftsRowCount == HimAgribalyseProductionEvidenceIndexTool.EXPECTED_RECORDS)
        require(metadata.buildState == if (requireValidated) HimEvidenceRetrievalIndexBuildState.VALIDATED else HimEvidenceRetrievalIndexBuildState.BUILDING)
        require(scalar(connection, "SELECT count(*) FROM evidence_records") == metadata.evidenceRecordCount)
        require(scalar(connection, "SELECT count(*) FROM evidence_search") == metadata.ftsRowCount)
        require(scalar(connection, "SELECT count(*) FROM evidence_search s JOIN evidence_records r ON r.internal_record_key=s.rowid") == metadata.evidenceRecordCount)
        require(scalar(connection, "SELECT count(*) FROM evidence_search s LEFT JOIN evidence_records r ON r.internal_record_key=s.rowid WHERE r.internal_record_key IS NULL") == 0L)

        val digest = HimAgribalyseProductionEvidenceIndexTool.newDigest()
        val agbCounts = linkedMapOf<String, Int>()
        var rows = 0L
        var first = ""
        var last = ""
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT * FROM evidence_records ORDER BY internal_record_key").use { result ->
                while (result.next()) {
                    rows++
                    require(result.getLong("internal_record_key") == rows)
                    val record = HimAgribalyseEvidenceProjectionV1.fromProjectionJson(result.getString("evidence_projection_json"))
                    require(record.sourceRecordReference.value == result.getString("source_record_reference"))
                    require(record.recordKind.name == result.getString("record_kind"))
                    require(record.sourceNativeIdentifiersJson == result.getString("source_native_identifiers_json"))
                    val agb = JsonParser.parseString(record.sourceNativeIdentifiersJson).asJsonObject.get("agbCode").asString
                    agbCounts[agb] = agbCounts.getOrDefault(agb, 0) + 1
                    if (rows == 1L) first = record.sourceRecordReference.value
                    last = record.sourceRecordReference.value
                    digest.add(record)
                }
            }
        }
        require(rows == HimAgribalyseProductionEvidenceIndexTool.EXPECTED_RECORDS)
        val duplicates = agbCounts.values.count { it > 1 }
        val maximum = agbCounts.values.max()
        require(agbCounts.size == HimAgribalyseProductionEvidenceIndexTool.EXPECTED_DISTINCT_AGB_CODES)
        require(duplicates == HimAgribalyseProductionEvidenceIndexTool.EXPECTED_DUPLICATE_GROUPS)
        require(maximum == 2)
        val recomputed = digest.finish()
        require(recomputed == metadata.logicalContentSha256)
        if (!requireValidated) connection.createStatement().use { it.execute("INSERT INTO evidence_search(evidence_search) VALUES('integrity-check')") }
        val integrity = connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA integrity_check").use { result -> require(result.next()); result.getString(1) }
        }
        require(integrity == "ok")
        if (requireValidated) require(
            HimEvidenceRetrievalIndexValidator.runtimeEligibility(
                metadata,
                HimExpectedEvidenceRetrievalIndex(HimGroundTruthSource.AGRIBALYSE, HimAgribalyseProductionEvidenceIndexTool.SOURCE_SHA),
            ) == HimEvidenceRetrievalIndexEligibility.Ready
        )
        return HimAgribalyseEvidenceIndexValidation(metadata, recomputed, integrity, first, last, agbCounts.size, duplicates, maximum)
    }

    private fun readMetadata(connection: Connection): HimEvidenceRetrievalIndexMetadata = connection.createStatement().use { statement ->
        statement.executeQuery("SELECT * FROM index_metadata WHERE singleton_id=1").use { result ->
            require(result.next())
            val count = Regex("\\{\"records\":(\\d+)}").matchEntire(result.getString("logical_record_counts_json"))!!.groupValues[1].toLong()
            HimEvidenceRetrievalIndexMetadata(
                result.getString("schema_version"), HimGroundTruthSource.valueOf(result.getString("source")),
                result.getString("source_artifact_path"), HimSha256(result.getString("source_artifact_sha256")),
                result.getLong("source_record_count"), HimEvidenceRetrievalIndexMetadata.sortedLogicalCounts(mapOf("records" to count)),
                result.getString("index_build_policy_version"), result.getString("evidence_projection_policy_version"),
                result.getLong("indexed_record_count"), result.getLong("evidence_record_count"), result.getLong("fts_row_count"),
                HimSha256(result.getString("logical_content_sha256")), HimEvidenceRetrievalIndexBuildState.valueOf(result.getString("build_state")),
                result.getString("sqlite_runtime_version"), result.getString("sqlite_file_sha256")?.let(::HimSha256),
            )
        }
    }

    private fun scalar(connection: Connection, sql: String): Long = connection.createStatement().use { statement ->
        statement.executeQuery(sql).use { result -> require(result.next()); result.getLong(1) }
    }
}

class HimAgribalyseSqliteEvidenceRetrievalStore private constructor(private val database: File) {
    fun search(query: String, limit: HimEvidenceSearchLimit): List<HimEvidenceSearchResult> {
        val ftsQuery = Regex("[\\p{L}\\p{N}]+").findAll(query.lowercase()).joinToString(" ") { "\"${it.value}\"" }
        require(ftsQuery.isNotBlank())
        return HimAgribalyseProductionEvidenceIndexTool.connection(database, true).use { connection ->
            connection.prepareStatement(HimEvidenceRetrievalIndexSchemaV1.SEARCH_SQL).use { statement ->
                statement.setString(1, ftsQuery); statement.setInt(2, limit.value)
                statement.executeQuery().use { result ->
                    buildList {
                        while (result.next()) add(
                            HimEvidenceSearchResult(
                                HimGroundTruthSource.AGRIBALYSE,
                                HimEvidenceRecordReference.parse(HimGroundTruthSource.AGRIBALYSE, result.getString("source_record_reference")),
                                HimEvidenceRecordKind.valueOf(result.getString("record_kind")), size + 1,
                                HimEvidenceProjection(result.getString("evidence_projection_json")),
                            )
                        )
                    }
                }
            }
        }
    }

    fun fetch(reference: HimEvidenceRecordReference): HimEvidenceSearchResult? {
        require(reference.source == HimGroundTruthSource.AGRIBALYSE)
        return HimAgribalyseProductionEvidenceIndexTool.connection(database, true).use { connection ->
            connection.prepareStatement(HimEvidenceRetrievalIndexSchemaV1.EXACT_FETCH_SQL).use { statement ->
                statement.setString(1, reference.value)
                statement.executeQuery().use { result ->
                    if (!result.next()) null else HimEvidenceSearchResult(
                        HimGroundTruthSource.AGRIBALYSE, reference,
                        HimEvidenceRecordKind.valueOf(result.getString("record_kind")), 1,
                        HimEvidenceProjection(result.getString("evidence_projection_json")),
                    )
                }
            }
        }
    }

    companion object {
        fun openAfterValidation(database: File, validation: HimAgribalyseEvidenceIndexValidation): HimAgribalyseSqliteEvidenceRetrievalStore {
            require(validation.metadata.buildState == HimEvidenceRetrievalIndexBuildState.VALIDATED)
            return HimAgribalyseSqliteEvidenceRetrievalStore(database)
        }
    }
}
