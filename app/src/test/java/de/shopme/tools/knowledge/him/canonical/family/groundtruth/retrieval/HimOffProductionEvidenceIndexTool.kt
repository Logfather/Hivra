package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.sql.Connection
import java.sql.DriverManager
import java.util.zip.GZIPInputStream

object HimOffProductionEvidenceIndexPaths {
    const val SOURCE = "data/sources/openfoodfacts/optimized/off-him-final-source.jsonl.gz"
    const val FINAL_INDEX = "data/sources/openfoodfacts/index/off-him-evidence-index.v1.sqlite"
    const val BUILDING_INDEX = "data/sources/openfoodfacts/index/.off-him-evidence-index.v1.sqlite.building"
    const val REPORT = "build/knowledge/reports/him/retrieval/him-f3d2c1-off-production-evidence-index-build.txt"
}

data class HimOffEvidenceIndexValidation(
    val metadata: HimEvidenceRetrievalIndexMetadata,
    val recomputedLogicalDigest: HimSha256,
    val integrityCheck: String,
    val firstReference: String,
    val lastReference: String,
)

data class HimOffEvidenceIndexBuildResult(
    val builtNow: Boolean,
    val fullSourceScans: Int,
    val validation: HimOffEvidenceIndexValidation,
)

object HimOffProductionEvidenceIndexTool {
    const val EXPECTED_RECORDS = 4_591_865L
    const val BATCH_SIZE = 5_000
    const val MINIMUM_REMAINING_BYTES = 12L * 1024 * 1024 * 1024
    val SOURCE_SHA = HimSha256("63b20183229a6f3f648c3d189c265d5d5a4b332d530d084af5640081d060a236")

    fun buildOrReuse(projectRoot: File): HimOffEvidenceIndexBuildResult {
        Class.forName("org.sqlite.JDBC")
        val source = projectRoot.resolve(HimOffProductionEvidenceIndexPaths.SOURCE)
        val finalIndex = projectRoot.resolve(HimOffProductionEvidenceIndexPaths.FINAL_INDEX)
        require(source.isFile)
        if (finalIndex.isFile) {
            return HimOffEvidenceIndexBuildResult(
                builtNow = false,
                fullSourceScans = 0,
                validation = HimOffEvidenceIndexValidator.validateReadOnly(finalIndex),
            )
        }

        val indexDirectory = requireNotNull(finalIndex.parentFile)
        require(indexDirectory.exists() || indexDirectory.mkdirs())
        require(indexDirectory.usableSpace >= MINIMUM_REMAINING_BYTES)
        val building = projectRoot.resolve(HimOffProductionEvidenceIndexPaths.BUILDING_INDEX)
        deleteBuildingFiles(building)

        try {
            build(source, building)
            val preValidated = HimOffEvidenceIndexValidator.validateWritable(building, requireValidated = false)
            require(preValidated.metadata.buildState == HimEvidenceRetrievalIndexBuildState.BUILDING)
            require(preValidated.recomputedLogicalDigest == preValidated.metadata.logicalContentSha256)
            markValidated(building)
            val validated = HimOffEvidenceIndexValidator.validateReadOnly(building)
            Files.move(
                building.toPath(),
                finalIndex.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
            )
            require(!sidecar(building, "-wal").exists() && !sidecar(building, "-shm").exists())
            return HimOffEvidenceIndexBuildResult(true, 1, validated)
        } catch (failure: Throwable) {
            System.err.println("OFF Evidence index build stopped; final index remains untouched: ${failure.message}")
            throw failure
        }
    }

    private fun build(source: File, building: File) {
        connection(building, readOnly = false).use { connection ->
            configureBuildConnection(connection)
            HimEvidenceRetrievalIndexSchemaV1.creationStatements.forEach { sql ->
                connection.createStatement().use { it.execute(sql) }
            }
            insertBuildingMetadata(connection)
            val digest = newDigest()
            val recordsSql = "INSERT INTO evidence_records VALUES (?, ?, ?, ?, ?)"
            val searchSql = "INSERT INTO evidence_search(rowid, primary_name, secondary_names, taxonomy_text, ingredient_text, context_text) VALUES (?, ?, ?, ?, ?, ?)"
            connection.prepareStatement(recordsSql).use { recordsStatement ->
                connection.prepareStatement(searchSql).use { searchStatement ->
                    connection.autoCommit = false
                    var ordinal = 0L
                    BufferedReader(
                        InputStreamReader(
                            GZIPInputStream(source.inputStream().buffered(1024 * 1024)),
                            Charsets.UTF_8,
                        ),
                        1024 * 1024,
                    )
                        .useLines { lines ->
                            lines.forEach { line ->
                                require(line.isNotBlank()) { "Blank OFF Source line at ${ordinal + 1}" }
                                ordinal++
                                val record = HimOffEvidenceProjectionV1.fromOptimizedSourceLine(line, ordinal)
                                bindRecord(recordsStatement, record)
                                recordsStatement.addBatch()
                                bindSearch(searchStatement, record)
                                searchStatement.addBatch()
                                digest.add(record)

                                if (ordinal % BATCH_SIZE == 0L) {
                                    recordsStatement.executeBatch()
                                    searchStatement.executeBatch()
                                    connection.commit()
                                }
                                if (ordinal % 100_000L == 0L) {
                                    println("OFF Evidence index build records=$ordinal")
                                    require(building.usableSpace >= MINIMUM_REMAINING_BYTES) {
                                        "Disk safety floor reached after $ordinal records"
                                    }
                                }
                            }
                        }
                    if (ordinal % BATCH_SIZE != 0L) {
                        recordsStatement.executeBatch()
                        searchStatement.executeBatch()
                        connection.commit()
                    }
                    require(ordinal == EXPECTED_RECORDS) {
                        "OFF record count mismatch: expected=$EXPECTED_RECORDS actual=$ordinal"
                    }
                    val logicalDigest = digest.finish()
                    connection.prepareStatement(
                        """
                        UPDATE index_metadata SET
                            indexed_record_count=?, evidence_record_count=?, fts_row_count=?,
                            logical_content_sha256=?
                        WHERE singleton_id=1 AND build_state='BUILDING'
                        """.trimIndent()
                    ).use { statement ->
                        statement.setLong(1, ordinal)
                        statement.setLong(2, ordinal)
                        statement.setLong(3, ordinal)
                        statement.setString(4, logicalDigest.value)
                        require(statement.executeUpdate() == 1)
                    }
                    connection.commit()
                    connection.autoCommit = true
                }
            }
        }
    }

    private fun configureBuildConnection(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute("PRAGMA page_size=4096")
            statement.execute("PRAGMA journal_mode=OFF")
            statement.execute("PRAGMA synchronous=OFF")
            statement.execute("PRAGMA temp_store=FILE")
            statement.execute("PRAGMA cache_size=-262144")
            statement.execute("PRAGMA locking_mode=EXCLUSIVE")
        }
    }

    private fun insertBuildingMetadata(connection: Connection) {
        connection.prepareStatement(
            """
            INSERT INTO index_metadata VALUES (
                1, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, ?, 'BUILDING', ?, NULL
            )
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION)
            statement.setString(2, HimGroundTruthSource.OPEN_FOOD_FACTS.name)
            statement.setString(3, HimGroundTruthSource.OPEN_FOOD_FACTS.artifactPath)
            statement.setString(4, SOURCE_SHA.value)
            statement.setLong(5, EXPECTED_RECORDS)
            statement.setString(6, "{\"products\":$EXPECTED_RECORDS}")
            statement.setString(7, HimEvidenceRetrievalIndexSchemaV1.BUILD_POLICY_VERSION)
            statement.setString(8, HimEvidenceRetrievalIndexSchemaV1.PROJECTION_POLICY_VERSION)
            statement.setString(9, "0".repeat(64))
            statement.setString(10, sqliteVersion(connection))
            require(statement.executeUpdate() == 1)
        }
    }

    private fun markValidated(database: File) {
        connection(database, readOnly = false).use { connection ->
            connection.createStatement().use { statement ->
                require(statement.executeUpdate(
                    "UPDATE index_metadata SET build_state='VALIDATED' WHERE singleton_id=1 AND build_state='BUILDING'"
                ) == 1)
            }
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

    internal fun newDigest() = HimEvidenceRetrievalIndexDigest.newAccumulator(
        HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION,
        HimGroundTruthSource.OPEN_FOOD_FACTS,
        SOURCE_SHA,
        mapOf("products" to EXPECTED_RECORDS),
        HimEvidenceRetrievalIndexSchemaV1.BUILD_POLICY_VERSION,
        HimEvidenceRetrievalIndexSchemaV1.PROJECTION_POLICY_VERSION,
    )

    internal fun connection(database: File, readOnly: Boolean): Connection {
        val absolute = database.toPath().toAbsolutePath()
        val url = if (readOnly) "jdbc:sqlite:file:$absolute?mode=ro" else "jdbc:sqlite:$absolute"
        return DriverManager.getConnection(url)
    }

    internal fun sqliteVersion(connection: Connection): String =
        connection.createStatement().use { statement ->
            statement.executeQuery("select sqlite_version()").use { result ->
                require(result.next())
                result.getString(1)
            }
        }

    private fun deleteBuildingFiles(building: File) {
        listOf(building, sidecar(building, "-journal"), sidecar(building, "-wal"), sidecar(building, "-shm"))
            .forEach { file -> if (file.exists()) require(file.delete()) }
    }

    private fun sidecar(database: File, suffix: String) = File(database.absolutePath + suffix)
}

object HimOffEvidenceIndexValidator {
    fun validateReadOnly(database: File): HimOffEvidenceIndexValidation =
        HimOffProductionEvidenceIndexTool.connection(database, readOnly = true).use { connection ->
            validate(connection, requireValidated = true)
        }

    fun validateWritable(database: File, requireValidated: Boolean): HimOffEvidenceIndexValidation =
        HimOffProductionEvidenceIndexTool.connection(database, readOnly = false).use { connection ->
            validate(connection, requireValidated)
        }

    private fun validate(connection: Connection, requireValidated: Boolean): HimOffEvidenceIndexValidation {
        val metadata = readMetadata(connection)
        require(metadata.schemaVersion == HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION)
        require(metadata.source == HimGroundTruthSource.OPEN_FOOD_FACTS)
        require(metadata.sourceArtifactSha256 == HimOffProductionEvidenceIndexTool.SOURCE_SHA)
        require(metadata.sourceRecordCount == HimOffProductionEvidenceIndexTool.EXPECTED_RECORDS)
        require(metadata.logicalRecordCounts == sortedMapOf("products" to HimOffProductionEvidenceIndexTool.EXPECTED_RECORDS))
        require(metadata.indexedRecordCount == HimOffProductionEvidenceIndexTool.EXPECTED_RECORDS)
        require(metadata.evidenceRecordCount == HimOffProductionEvidenceIndexTool.EXPECTED_RECORDS)
        require(metadata.ftsRowCount == HimOffProductionEvidenceIndexTool.EXPECTED_RECORDS)
        if (requireValidated) require(metadata.buildState == HimEvidenceRetrievalIndexBuildState.VALIDATED)
        else require(metadata.buildState == HimEvidenceRetrievalIndexBuildState.BUILDING)

        val evidenceCount = count(connection, "evidence_records")
        val ftsCount = count(connection, "evidence_search")
        require(evidenceCount == metadata.evidenceRecordCount)
        require(ftsCount == metadata.ftsRowCount)
        require(joinCount(connection) == evidenceCount)
        require(danglingCount(connection) == 0L)

        val digest = HimOffProductionEvidenceIndexTool.newDigest()
        var firstReference = ""
        var lastReference = ""
        var rows = 0L
        connection.createStatement().use { statement ->
            statement.fetchSize = 1_000
            statement.executeQuery(
                """
                SELECT internal_record_key, source_record_reference, record_kind,
                       source_native_identifiers_json, evidence_projection_json
                FROM evidence_records ORDER BY internal_record_key
                """.trimIndent()
            ).use { result ->
                while (result.next()) {
                    rows++
                    val record = HimOffEvidenceProjectionV1.fromProjectionJson(result.getString("evidence_projection_json"))
                    require(record.internalRecordKey == result.getLong("internal_record_key"))
                    require(record.sourceRecordReference.value == result.getString("source_record_reference"))
                    require(record.recordKind.name == result.getString("record_kind"))
                    require(record.sourceNativeIdentifiersJson == result.getString("source_native_identifiers_json"))
                    if (rows == 1L) firstReference = record.sourceRecordReference.value
                    lastReference = record.sourceRecordReference.value
                    digest.add(record)
                }
            }
        }
        require(rows == HimOffProductionEvidenceIndexTool.EXPECTED_RECORDS)
        val recomputed = digest.finish()
        require(recomputed == metadata.logicalContentSha256)
        if (!requireValidated) {
            connection.createStatement().use { statement ->
                statement.execute("INSERT INTO evidence_search(evidence_search) VALUES('integrity-check')")
            }
        }
        val integrity = connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA integrity_check").use { result ->
                require(result.next())
                result.getString(1)
            }
        }
        require(integrity == "ok")
        if (requireValidated) {
            require(
                HimEvidenceRetrievalIndexValidator.runtimeEligibility(
                    metadata,
                    HimExpectedEvidenceRetrievalIndex(
                        HimGroundTruthSource.OPEN_FOOD_FACTS,
                        HimOffProductionEvidenceIndexTool.SOURCE_SHA,
                    ),
                ) == HimEvidenceRetrievalIndexEligibility.Ready
            )
        }
        return HimOffEvidenceIndexValidation(metadata, recomputed, integrity, firstReference, lastReference)
    }

    private fun readMetadata(connection: Connection): HimEvidenceRetrievalIndexMetadata =
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT * FROM index_metadata WHERE singleton_id=1").use { result ->
                require(result.next())
                val logicalCount = Regex("\\{\"products\":(\\d+)}")
                    .matchEntire(result.getString("logical_record_counts_json"))
                    ?.groupValues?.get(1)?.toLong()
                    ?: error("Invalid OFF logical counts JSON")
                HimEvidenceRetrievalIndexMetadata(
                    schemaVersion = result.getString("schema_version"),
                    source = HimGroundTruthSource.valueOf(result.getString("source")),
                    sourceArtifactPath = result.getString("source_artifact_path"),
                    sourceArtifactSha256 = HimSha256(result.getString("source_artifact_sha256")),
                    sourceRecordCount = result.getLong("source_record_count"),
                    logicalRecordCounts = HimEvidenceRetrievalIndexMetadata.sortedLogicalCounts(mapOf("products" to logicalCount)),
                    indexBuildPolicyVersion = result.getString("index_build_policy_version"),
                    evidenceProjectionPolicyVersion = result.getString("evidence_projection_policy_version"),
                    indexedRecordCount = result.getLong("indexed_record_count"),
                    evidenceRecordCount = result.getLong("evidence_record_count"),
                    ftsRowCount = result.getLong("fts_row_count"),
                    logicalContentSha256 = HimSha256(result.getString("logical_content_sha256")),
                    buildState = HimEvidenceRetrievalIndexBuildState.valueOf(result.getString("build_state")),
                    sqliteRuntimeVersion = result.getString("sqlite_runtime_version"),
                    sqliteFileSha256 = result.getString("sqlite_file_sha256")?.let(::HimSha256),
                )
            }
        }

    private fun count(connection: Connection, table: String): Long = scalar(
        connection, "SELECT count(*) FROM $table"
    )

    private fun joinCount(connection: Connection): Long = scalar(
        connection,
        "SELECT count(*) FROM evidence_search s JOIN evidence_records r ON r.internal_record_key=s.rowid",
    )

    private fun danglingCount(connection: Connection): Long = scalar(
        connection,
        "SELECT count(*) FROM evidence_search s LEFT JOIN evidence_records r ON r.internal_record_key=s.rowid WHERE r.internal_record_key IS NULL",
    )

    private fun scalar(connection: Connection, sql: String): Long =
        connection.createStatement().use { statement ->
            statement.executeQuery(sql).use { result -> require(result.next()); result.getLong(1) }
        }
}

class HimOffSqliteEvidenceRetrievalStore private constructor(
    private val database: File,
    val metadata: HimEvidenceRetrievalIndexMetadata,
) {
    fun search(query: String, limit: HimEvidenceSearchLimit): List<HimEvidenceSearchResult> {
        require(query.isNotBlank())
        val ftsQuery = Regex("[\\p{L}\\p{N}]+").findAll(query.lowercase()).joinToString(" ") {
            "\"${it.value.replace("\"", "\"\"")}\""
        }
        require(ftsQuery.isNotBlank())
        return HimOffProductionEvidenceIndexTool.connection(database, readOnly = true).use { connection ->
            connection.prepareStatement(HimEvidenceRetrievalIndexSchemaV1.SEARCH_SQL).use { statement ->
                statement.setString(1, ftsQuery)
                statement.setInt(2, limit.value)
                statement.executeQuery().use { result ->
                    buildList {
                        var rank = 0
                        while (result.next()) {
                            rank++
                            val reference = HimEvidenceRecordReference.parse(
                                HimGroundTruthSource.OPEN_FOOD_FACTS,
                                result.getString("source_record_reference"),
                            )
                            add(
                                HimEvidenceSearchResult(
                                    source = HimGroundTruthSource.OPEN_FOOD_FACTS,
                                    sourceRecordReference = reference,
                                    recordKind = HimEvidenceRecordKind.valueOf(result.getString("record_kind")),
                                    retrievalRank = rank,
                                    evidenceProjection = HimEvidenceProjection(result.getString("evidence_projection_json")),
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun fetch(reference: HimEvidenceRecordReference): HimEvidenceSearchResult? {
        require(reference.source == HimGroundTruthSource.OPEN_FOOD_FACTS)
        return HimOffProductionEvidenceIndexTool.connection(database, readOnly = true).use { connection ->
            connection.prepareStatement(HimEvidenceRetrievalIndexSchemaV1.EXACT_FETCH_SQL).use { statement ->
                statement.setString(1, reference.value)
                statement.executeQuery().use { result ->
                    if (!result.next()) null else HimEvidenceSearchResult(
                        source = HimGroundTruthSource.OPEN_FOOD_FACTS,
                        sourceRecordReference = reference,
                        recordKind = HimEvidenceRecordKind.valueOf(result.getString("record_kind")),
                        retrievalRank = 1,
                        evidenceProjection = HimEvidenceProjection(result.getString("evidence_projection_json")),
                    )
                }
            }
        }
    }

    companion object {
        fun open(database: File): HimOffSqliteEvidenceRetrievalStore {
            if (!database.isFile) throw HimSourceIndexUnavailableException(database.path)
            val validation = HimOffEvidenceIndexValidator.validateReadOnly(database)
            return HimOffSqliteEvidenceRetrievalStore(database, validation.metadata)
        }

        fun openAfterValidation(
            database: File,
            validation: HimOffEvidenceIndexValidation,
        ): HimOffSqliteEvidenceRetrievalStore {
            require(database.isFile)
            require(validation.metadata.buildState == HimEvidenceRetrievalIndexBuildState.VALIDATED)
            return HimOffSqliteEvidenceRetrievalStore(database, validation.metadata)
        }
    }
}

class HimSourceIndexUnavailableException(path: String) :
    IllegalStateException("SOURCE_INDEX_UNAVAILABLE: $path")
