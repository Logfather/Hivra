package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceProjection
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexBuildState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexDigest
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexEligibility
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexMetadata
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexRecord
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexSchemaV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexStaleReason
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexValidator
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceSearchLimit
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceSearchText
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimExpectedEvidenceRetrievalIndex
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.sql.Connection
import java.sql.DriverManager
import kotlin.io.path.deleteIfExists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimEvidenceRetrievalIndexSchemaV1Test {

    @Test
    fun verifiesSchemaContractsAndWritesDeterministicReport() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        val root = projectRoot()
        val foundationBefore = guardedHashes(root, FOUNDATION_GUARDS)
        val sourcesBefore = guardedHashes(root, SOURCE_GUARDS)
        assertEquals(FOUNDATION_GUARDS, foundationBefore)
        assertEquals(SOURCE_GUARDS, sourcesBefore)

        verifyRecordReferences()
        verifyLogicalDigest()
        verifyMetadataEligibility()
        verifySqliteSchemaRoundtrip()
        assertNoProductionIndexes(root)

        val reportFile = root.resolve(REPORT_PATH)
        requireNotNull(reportFile.parentFile).mkdirs()
        reportFile.writeText(report(), Charsets.UTF_8)
        assertEquals(report(), reportFile.readText(Charsets.UTF_8))

        assertNoProductionIndexes(root)
        assertEquals(foundationBefore, guardedHashes(root, FOUNDATION_GUARDS))
        assertEquals(sourcesBefore, guardedHashes(root, SOURCE_GUARDS))
    }

    private fun verifyRecordReferences() {
        val fixtures = listOf(
            HimEvidenceRecordReference.offProduct(123, "3017620422003"),
            HimEvidenceRecordReference.agribalyse(12, "26013"),
            HimEvidenceRecordReference.ciqualFood("12345"),
            HimEvidenceRecordReference.ciqualTaxonomy("01", "0101", "010101"),
            HimEvidenceRecordReference.ciqualConstituent("400"),
            HimEvidenceRecordReference.ciqualSource("444"),
            HimEvidenceRecordReference.gi("measurement", 42),
            HimEvidenceRecordReference.gi("footnote", 7),
        )
        fixtures.forEach { fixture ->
            assertEquals(fixture, HimEvidenceRecordReference.parse(fixture.source, fixture.value))
        }
        assertFailsWith<IllegalArgumentException> {
            HimEvidenceRecordReference.parse(HimGroundTruthSource.AGRIBALYSE, "agribalyse:agb:26013")
        }
    }

    private fun verifyLogicalDigest() {
        val records = syntheticRecords(3)
        val digest = logicalDigest(records)
        assertEquals(digest, logicalDigest(records))
        assertEquals(digest, logicalDigest(records.reversed()))
        assertNotEquals(digest, logicalDigest(records.updated(0) {
            copy(evidenceProjection = HimEvidenceProjection("{\"name\":\"changed\"}"))
        }))
        assertNotEquals(digest, logicalDigest(records.updated(0) {
            copy(searchText = searchText.copy(primaryName = "changed"))
        }))
        assertNotEquals(digest, logicalDigest(records.updated(0) {
            copy(sourceRecordReference = HimEvidenceRecordReference.offProduct(99, "changed"))
        }))
        assertNotEquals(digest, logicalDigest(records, projectionPolicy = "CHANGED_POLICY"))
    }

    private fun verifyMetadataEligibility() {
        val expected = expectedIndex()
        val building = metadata(HimEvidenceRetrievalIndexBuildState.BUILDING)
        assertIs<HimEvidenceRetrievalIndexEligibility.Corrupt>(
            HimEvidenceRetrievalIndexValidator.runtimeEligibility(building, expected)
        )
        val validated = metadata(HimEvidenceRetrievalIndexBuildState.VALIDATED)
        assertEquals(
            HimEvidenceRetrievalIndexEligibility.Ready,
            HimEvidenceRetrievalIndexValidator.runtimeEligibility(validated, expected),
        )
        val stale = HimEvidenceRetrievalIndexValidator.runtimeEligibility(
            validated,
            expected.copy(sourceArtifactSha256 = SHA_B),
        )
        assertIs<HimEvidenceRetrievalIndexEligibility.Stale>(stale)
        assertTrue(HimEvidenceRetrievalIndexStaleReason.SOURCE_ARTIFACT_SHA_MISMATCH in stale.reasons)
    }

    private fun verifySqliteSchemaRoundtrip() {
        Class.forName("org.sqlite.JDBC")
        val directory = Files.createTempDirectory("him-index-schema-v1-")
        val database = directory.resolve("schema-v1.sqlite")
        try {
            connect(database).use { connection ->
                HimEvidenceRetrievalIndexSchemaV1.creationStatements.forEach { sql ->
                    connection.createStatement().use { it.execute(sql) }
                }
                insertMetadata(connection, HimEvidenceRetrievalIndexBuildState.BUILDING)
                assertEquals("BUILDING", metadataState(connection))

                val records = syntheticRecords(12)
                records.forEach { insertRecord(connection, it) }
                assertEquals(12L, count(connection, "evidence_records"))
                assertEquals(12L, count(connection, "evidence_search"))

                val topTen = search(connection, "apple", HimEvidenceSearchLimit(10))
                assertEquals(10, topTen.size)
                assertEquals(topTen.sorted(), topTen)
                assertFailsWith<IllegalArgumentException> { HimEvidenceSearchLimit(11) }

                val expectedReference = records.first().sourceRecordReference.value
                assertEquals(expectedReference, exactFetch(connection, expectedReference))
                assertEquals(null, exactFetch(connection, "off:product:row:999:code:missing"))
                assertEquals(0L, danglingFtsRows(connection))

                connection.createStatement().use { statement ->
                    statement.execute("UPDATE index_metadata SET build_state='VALIDATED'")
                }
                assertEquals("VALIDATED", metadataState(connection))
                assertEquals("ok", integrityCheck(connection))
            }
            connect(database).use { reopened ->
                assertEquals("VALIDATED", metadataState(reopened))
                assertEquals(10, search(reopened, "apple", HimEvidenceSearchLimit(10)).size)
                assertEquals("ok", integrityCheck(reopened))
            }
        } finally {
            deleteDatabase(database)
            directory.deleteIfExists()
        }
    }

    private fun insertMetadata(connection: Connection, state: HimEvidenceRetrievalIndexBuildState) {
        connection.prepareStatement(
            """
            INSERT INTO index_metadata VALUES (
                1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL
            )
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION)
            statement.setString(2, HimGroundTruthSource.OPEN_FOOD_FACTS.name)
            statement.setString(3, HimGroundTruthSource.OPEN_FOOD_FACTS.artifactPath)
            statement.setString(4, SHA_A.value)
            statement.setLong(5, 12)
            statement.setString(6, "{\"products\":12}")
            statement.setString(7, HimEvidenceRetrievalIndexSchemaV1.BUILD_POLICY_VERSION)
            statement.setString(8, HimEvidenceRetrievalIndexSchemaV1.PROJECTION_POLICY_VERSION)
            statement.setLong(9, 12)
            statement.setLong(10, 12)
            statement.setLong(11, 12)
            statement.setString(12, logicalDigest(syntheticRecords(12)).value)
            statement.setString(13, state.name)
            statement.setString(14, sqliteVersion(connection))
            statement.executeUpdate()
        }
    }

    private fun insertRecord(connection: Connection, record: HimEvidenceRetrievalIndexRecord) {
        connection.prepareStatement(
            "INSERT INTO evidence_records VALUES (?, ?, ?, ?, ?)"
        ).use { statement ->
            statement.setLong(1, record.internalRecordKey)
            statement.setString(2, record.sourceRecordReference.value)
            statement.setString(3, record.recordKind.name)
            statement.setString(4, record.sourceNativeIdentifiersJson)
            statement.setString(5, record.evidenceProjection.deterministicJson)
            statement.executeUpdate()
        }
        connection.prepareStatement(
            "INSERT INTO evidence_search(rowid, primary_name, secondary_names, taxonomy_text, ingredient_text, context_text) VALUES (?, ?, ?, ?, ?, ?)"
        ).use { statement ->
            statement.setLong(1, record.internalRecordKey)
            statement.setString(2, record.searchText.primaryName)
            statement.setString(3, record.searchText.secondaryNames)
            statement.setString(4, record.searchText.taxonomyText)
            statement.setString(5, record.searchText.ingredientText)
            statement.setString(6, record.searchText.contextText)
            statement.executeUpdate()
        }
    }

    private fun search(connection: Connection, query: String, limit: HimEvidenceSearchLimit): List<String> =
        connection.prepareStatement(HimEvidenceRetrievalIndexSchemaV1.SEARCH_SQL).use { statement ->
            statement.setString(1, query)
            statement.setInt(2, limit.value)
            statement.executeQuery().use { result ->
                buildList { while (result.next()) add(result.getString("source_record_reference")) }
            }
        }

    private fun exactFetch(connection: Connection, reference: String): String? =
        connection.prepareStatement(HimEvidenceRetrievalIndexSchemaV1.EXACT_FETCH_SQL).use { statement ->
            statement.setString(1, reference)
            statement.executeQuery().use { result ->
                if (result.next()) result.getString("source_record_reference") else null
            }
        }

    private fun syntheticRecords(count: Int): List<HimEvidenceRetrievalIndexRecord> =
        (1..count).map { ordinal ->
            HimEvidenceRetrievalIndexRecord(
                internalRecordKey = ordinal.toLong(),
                sourceRecordReference = HimEvidenceRecordReference.offProduct(ordinal.toLong(), "code-$ordinal"),
                recordKind = HimEvidenceRecordKind.OFF_PRODUCT,
                sourceNativeIdentifiersJson = "{\"code\":\"code-$ordinal\",\"rowOrdinal\":$ordinal}",
                evidenceProjection = HimEvidenceProjection("{\"productName\":\"Apple $ordinal\"}"),
                searchText = HimEvidenceSearchText("apple", "apple $ordinal", "fruit", "", "fixture"),
            )
        }

    private fun logicalDigest(
        records: List<HimEvidenceRetrievalIndexRecord>,
        projectionPolicy: String = HimEvidenceRetrievalIndexSchemaV1.PROJECTION_POLICY_VERSION,
    ) = HimEvidenceRetrievalIndexDigest.compute(
        schemaVersion = HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION,
        source = HimGroundTruthSource.OPEN_FOOD_FACTS,
        sourceArtifactSha256 = SHA_A,
        logicalRecordCounts = mapOf("products" to records.size.toLong()),
        indexBuildPolicyVersion = HimEvidenceRetrievalIndexSchemaV1.BUILD_POLICY_VERSION,
        evidenceProjectionPolicyVersion = projectionPolicy,
        records = records,
    )

    private fun expectedIndex() = HimExpectedEvidenceRetrievalIndex(
        HimGroundTruthSource.OPEN_FOOD_FACTS,
        SHA_A,
    )

    private fun metadata(state: HimEvidenceRetrievalIndexBuildState) =
        HimEvidenceRetrievalIndexMetadata(
            schemaVersion = HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION,
            source = HimGroundTruthSource.OPEN_FOOD_FACTS,
            sourceArtifactPath = HimGroundTruthSource.OPEN_FOOD_FACTS.artifactPath,
            sourceArtifactSha256 = SHA_A,
            sourceRecordCount = 3,
            logicalRecordCounts = HimEvidenceRetrievalIndexMetadata.sortedLogicalCounts(mapOf("products" to 3)),
            indexBuildPolicyVersion = HimEvidenceRetrievalIndexSchemaV1.BUILD_POLICY_VERSION,
            evidenceProjectionPolicyVersion = HimEvidenceRetrievalIndexSchemaV1.PROJECTION_POLICY_VERSION,
            indexedRecordCount = 3,
            evidenceRecordCount = 3,
            ftsRowCount = 3,
            logicalContentSha256 = logicalDigest(syntheticRecords(3)),
            buildState = state,
            sqliteRuntimeVersion = "3.53.2",
        )

    private fun report(): String =
        """
        HIM F3d.2b RETRIEVAL INDEX CONTRACTS / SQLITE SCHEMA V1
        ========================================================================
        INDEX ROOT CONTRACT
        - one source-local SQLite database per HimGroundTruthSource
        - schemaVersion=${HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION}
        - buildPolicyVersion=${HimEvidenceRetrievalIndexSchemaV1.BUILD_POLICY_VERSION}
        - projectionPolicyVersion=${HimEvidenceRetrievalIndexSchemaV1.PROJECTION_POLICY_VERSION}
        - indexes are derived, regenerable, Source-SHA-bound and non-authoritative

        TABLE index_metadata
        ${HimEvidenceRetrievalIndexSchemaV1.CREATE_METADATA.trimIndent()}

        TABLE evidence_records
        ${HimEvidenceRetrievalIndexSchemaV1.CREATE_EVIDENCE_RECORDS.trimIndent()}

        VIRTUAL TABLE evidence_search
        ${HimEvidenceRetrievalIndexSchemaV1.CREATE_EVIDENCE_SEARCH.trimIndent()}

        ADDITIONAL TABLES
        - none

        EVIDENCE STORE CONTRACT
        - internal_record_key is deterministic, positive, unique and Source-index-local
        - source_record_reference is deterministic and UNIQUE independently of raw identifier uniqueness
        - record_kind is Source-local and never a Canonical Family entity type
        - source_native_identifiers_json is deterministic compact JSON for audit identifiers
        - evidence_projection_json is deterministic compact source-faithful JSON
        - FTS rowid equals internal_record_key and joins directly to evidence_records
        - exact fetch uses source_record_reference and never scans a compressed Source artifact

        FTS5 CONTRACT
        - mode=contentless
        - tokenizer=unicode61
        - columns=primary_name,secondary_names,taxonomy_text,ingredient_text,context_text
        - searchable text is technically derived only from SEARCHABLE_AND_EVIDENCE/SEARCHABLE fields
        - Evidence-only payload is not automatically indexed
        - MATCH, prefix MATCH and internal bm25 are supported
        - ranking order: bm25(evidence_search), then source_record_reference ascending
        - SQL LIMIT is mandatory through HimEvidenceSearchLimit; valid range=1..10
        - technical score is not part of HimEvidenceSearchResult
        - no weights are frozen in V1; evaluation remains a later query-policy concern

        SOURCE RECORD REFERENCES
        - OPEN_FOOD_FACTS: off:product:row:<ordinal>:code:<source.code>
        - AGRIBALYSE: agribalyse:row:<ordinal>:agb:<agbCode>
        - CIQUAL food: ciqual:food:<alimCode>
        - CIQUAL taxonomy: ciqual:taxonomy:<group>/<subgroup>/<subSubgroup>
        - CIQUAL constituent: ciqual:constituent:<constCode>
        - CIQUAL source: ciqual:source:<sourceCode>
        - GLYCEMIC_INDEX: gi:<measurement|mean-summary|category-note|footnote>:<arrayOrdinal>

        SOURCE-SPECIFIC PROJECTION SUPPORT
        - OFF: complete selected identity/taxonomy/ingredients/nutrition/classification/allergen/geography/environmental/packaging/quality groups fit deterministic JSON payload
        - AGRIBALYSE: row ordinal, identifiers, names/context, scenario/data-quality and all environmental indicators fit deterministic JSON payload; duplicates remain separate
        - CIQUAL: heterogeneous record kinds, names, taxonomy, composition, citations, INFOODS, confidence, lexical values and missingness fit deterministic JSON payload
        - GI: four record kinds, ordinals, food/context and complete lexical/status payload fit deterministic JSON payload
        - no Source requires semantic inference or canonical mapping

        METADATA CONTRACT
        - singleton row stores schema/source/path/Source SHA, flat and deterministic logical counts JSON,
          independent build/projection policy versions, indexed/Evidence/FTS counts, logical digest,
          BUILDING|VALIDATED state, diagnostic SQLite version and optional diagnostic file SHA
        - logical counts JSON properties are lexicographically ordered by the builder contract
        - BUILDING is never runtime eligible
        - VALIDATED is eligible only after every guard succeeds

        STALE AND FAILURE CONTRACT
        - Source SHA mismatch=SOURCE_INDEX_STALE
        - schema/build-policy/projection-policy mismatch=SOURCE_INDEX_STALE and full rebuild required
        - missing index=SOURCE_INDEX_UNAVAILABLE
        - invalid state, count/reference/digest/integrity failure=SOURCE_INDEX_CORRUPT
        - no full-Source fallback, implicit rebuild or partial V1 reconciliation

        LOGICAL CONTENT DIGEST CONTRACT
        - algorithm=SHA-256; contractVersion=${HimEvidenceRetrievalIndexDigest.DIGEST_CONTRACT_VERSION}
        - each UTF-8 field is framed as label + NUL + decimal byte length + ':' + bytes + newline
        - header order: digest contract, schema version, Source, Source SHA, build policy, projection policy
        - logical counts follow in lexicographic key order
        - records follow in internal_record_key order
        - per record: key, Source reference, kind, identifiers JSON, five search columns, projection JSON
        - SQLite pages, row physical location, journal/WAL state, timestamps and file metadata are excluded
        - raw SQLite file SHA is optional transport diagnostics, never semantic identity

        VALIDATION CONTRACT
        - singleton metadata present and schema supported
        - expected Source/path/SHA and policy versions match
        - Source logical counts, indexed count, Evidence count and FTS count match
        - internal keys and composite Source references are unique
        - every Evidence row has a stable valid Source reference and record kind
        - no dangling FTS rowid; logical digest recomputes; PRAGMA integrity_check=ok
        - build_state=VALIDATED

        PUBLICATION CONTRACT
        1 create temporary sibling database
        2 write metadata state BUILDING
        3 stream immutable optimized Source with bounded memory
        4 populate Evidence Store and FTS5 in deterministic order/batches
        5 finalize counts and logical digest
        6 validate references/counts/FTS/digest and integrity_check
        7 set VALIDATED, close cleanly, atomically move to final Source-local path
        - interrupted BUILDING database remains temporary and never replaces a valid final index
        - V1 updates are full rebuilds; no row patches, deltas or runtime index mutation
        - journal/WAL settings are build-policy details, not semantic identity
        - published runtime access is effectively read-only

        QUERY NORMALIZATION
        - a technical lookup copy may apply Unicode normalization, trim, whitespace normalization and lowercase
        - exact normalization form belongs to the versioned build/query policy
        - original Source lexical text remains in Evidence
        - stemming=false synonyms=false translation=false canonicalMapping=false

        RUNTIME SCOPE
        - current JVM schema/test runtime=READY via testImplementation sqlite-jdbc 3.53.2.1
        - future production builder runtime=DECISION_REQUIRED
        - future production reader runtime=DECISION_REQUIRED
        - sqlite-jdbc moved into Android runtime=false

        BOUNDARIES
        - PRE-HIM/legacy matcher/legacy merger/sourceVariants dependency=false
        - Candidate semantics, HimEntityId, cross-Source score and Authority state are absent
        - production Source indexes built=0
        - Candidate/Ground-Truth/Authority writes=0

        TEST CONTRACT RESULTS
        - schema creation, metadata roundtrip, BUILDING/VALIDATED, FTS->Evidence join,
          exact fetch, Top-K rejection, deterministic tie-break, logical digest stability/change,
          stale Source SHA, reopen and integrity_check are asserted by the system test
        """.trimIndent() + "\n"

    private fun sqliteVersion(connection: Connection): String =
        connection.createStatement().use { statement ->
            statement.executeQuery("select sqlite_version()").use { result ->
                assertTrue(result.next())
                result.getString(1)
            }
        }

    private fun metadataState(connection: Connection): String =
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT build_state FROM index_metadata WHERE singleton_id=1").use { result ->
                assertTrue(result.next())
                result.getString(1)
            }
        }

    private fun count(connection: Connection, table: String): Long =
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT count(*) FROM $table").use { result -> result.next(); result.getLong(1) }
        }

    private fun danglingFtsRows(connection: Connection): Long =
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT count(*) FROM evidence_search s LEFT JOIN evidence_records r ON r.internal_record_key=s.rowid WHERE r.internal_record_key IS NULL").use { result -> result.next(); result.getLong(1) }
        }

    private fun integrityCheck(connection: Connection): String =
        connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA integrity_check").use { result -> result.next(); result.getString(1) }
        }

    private fun connect(database: Path): Connection =
        DriverManager.getConnection("jdbc:sqlite:${database.toAbsolutePath()}")

    private fun deleteDatabase(database: Path) {
        listOf("-journal", "-wal", "-shm").forEach { suffix ->
            database.resolveSibling(database.fileName.toString() + suffix).deleteIfExists()
        }
        database.deleteIfExists()
    }

    private fun <T> List<T>.updated(index: Int, transform: T.() -> T): List<T> =
        mapIndexed { current, value -> if (current == index) value.transform() else value }

    private fun assertNoProductionIndexes(root: File) {
        INDEX_PATHS.forEach { relative ->
            val directory = root.resolve(relative)
            if (directory.isDirectory) {
                assertFalse(directory.walkTopDown().filter(File::isFile).any { it.extension in setOf("sqlite", "sqlite3", "db") })
            }
        }
    }

    private fun guardedHashes(root: File, expected: Map<String, String>): Map<String, String> =
        expected.mapValues { (path, _) -> sha256(root.resolve(path)) }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte ->
            (byte.toInt() and 0xff).toString(16).padStart(2, '0')
        }
    }

    private fun projectRoot(): File {
        var current = File(requireNotNull(System.getProperty("user.dir"))).canonicalFile
        while (!current.resolve("settings.gradle.kts").isFile) {
            current = requireNotNull(current.parentFile) { "ShopMe project root not found" }
        }
        return current
    }

    companion object {
        private val SHA_A = HimSha256("a".repeat(64))
        private val SHA_B = HimSha256("b".repeat(64))
        private const val REPORT_PATH = "build/knowledge/reports/him/retrieval/him-f3d2b-retrieval-index-sqlite-schema-v1.txt"
        private val INDEX_PATHS = listOf(
            "data/sources/openfoodfacts/index", "data/sources/agribalyse/index",
            "data/sources/ciqual/index", "data/sources/glycemic-index/index",
        )
        private val FOUNDATION_GUARDS = mapOf(
            "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json" to "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f",
            "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json" to "46145e663b483777228894a099f958f8150773af98b3772882823745b365cf9a",
            "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json" to "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184",
            "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json" to "4afc7490bd69f2f8919558d33d98c912771d128da4ac7a782cc116a1c9fccb6e",
            "data/knowledge/him/canonical-family/index/him-entity-fingerprint-index.v1.json" to "6dcb065d6dce0aaa22c8d1d3dd620372fc90d3fe9a3ec9d20ee54737a8f9a951",
        )
        private val SOURCE_GUARDS = mapOf(
            "data/sources/openfoodfacts/optimized/off-him-final-source.jsonl.gz" to "63b20183229a6f3f648c3d189c265d5d5a4b332d530d084af5640081d060a236",
            "data/sources/agribalyse/optimized/agribalyse-him-final-source.jsonl.gz" to "9068c89fa887ef087e87dcc51f758dd29e9623b93d9bd0a2277a4faae57c2297",
            "data/sources/ciqual/optimized/ciqual-him-final-source.json.gz" to "807d16c222f0e812831c10bdd89f1a5ebbc74c95e8a4944723db486add96dcff",
            "data/sources/glycemic-index/optimized/glycemic-index-him-final-source.json.gz" to "6891c2ff2ab3734a1d2768339c1f660f7093a31b97a856c82bf9b8c3c88422b5",
        )
    }
}
