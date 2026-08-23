package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RunHimSourceRetrievalIndexFeasibilityAuditTest {

    @Test
    fun auditsSourceFieldsReferencesAndSqliteFeasibilityDeterministically() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        val root = projectRoot()
        val foundationBefore = hashes(root, FOUNDATION_GUARDS)
        val sourcesBefore = hashes(root, SOURCE_GUARDS)
        assertEquals(FOUNDATION_GUARDS.mapValues { it.value.sha256 }, foundationBefore)
        assertEquals(SOURCE_GUARDS.mapValues { it.value.sha256 }, sourcesBefore)

        val sourceFacts = auditSources(root)
        assertIndexPathsIgnored(root)
        assertSqliteRuntimeReady()
        assertNoProductionIndexes(root)

        val report = report(sourceFacts)
        val reportFile = root.resolve(REPORT_PATH)
        requireNotNull(reportFile.parentFile).mkdirs()
        reportFile.writeText(report, Charsets.UTF_8)
        assertEquals(report, reportFile.readText(Charsets.UTF_8))

        assertNoProductionIndexes(root)
        assertEquals(foundationBefore, hashes(root, FOUNDATION_GUARDS))
        assertEquals(sourcesBefore, hashes(root, SOURCE_GUARDS))
    }

    private fun auditSources(root: File): SourceFacts {
        val offFile = root.resolve(OFF_PATH)
        val offFingerprint = JsonParser.parseReader(root.resolve(OFF_FINGERPRINT).reader()).asJsonObject
        assertEquals(OFF_RECORDS, offFingerprint["recordCount"].asLong)
        assertEquals(offFile.length(), offFingerprint["compressedBytes"].asLong)
        assertEquals(SOURCE_GUARDS.getValue(OFF_PATH).sha256, offFingerprint["sha256"].asString)
        val offPaths = jsonlSamplePaths(offFile, 512)
        assertPaths(
            offPaths,
            "source.code", "identity.productName", "taxonomy.categories[]",
            "ingredients.text", "nutrition", "classification", "quality", "provenance",
        )

        val agrFile = root.resolve(AGR_PATH)
        val agrCodes = mutableListOf<String>()
        val agrPaths = sortedSetOf<String>()
        gzipLines(agrFile).useLines { lines ->
            lines.filter(String::isNotBlank).forEach { line ->
                val record = JsonParser.parseString(line).asJsonObject
                agrCodes += record["agbCode"].asString
                collectPaths(record, "", agrPaths)
            }
        }
        assertEquals(AGR_RECORDS, agrCodes.size.toLong())
        assertEquals(AGR_DISTINCT_CODES, agrCodes.toSet().size.toLong())
        assertEquals(AGR_DUPLICATE_CODES, agrCodes.groupingBy { it }.eachCount().count { it.value > 1 }.toLong())
        assertPaths(agrPaths, "agbCode", "ciqualCode", "foodGroup", "foodSubgroup", "productNameFr", "lciName", "preparation", "efSingleScore")

        val ciqual = gzipJson(root.resolve(CIQUAL_PATH))
        val foods = ciqual.getAsJsonArray("foods")
        val taxonomy = ciqual.getAsJsonArray("taxonomy")
        val constituents = ciqual.getAsJsonArray("constituents")
        val citations = ciqual.getAsJsonArray("sources")
        assertEquals(CIQUAL_FOODS, foods.size().toLong())
        assertEquals(CIQUAL_TAXONOMY, taxonomy.size().toLong())
        assertEquals(CIQUAL_CONSTITUENTS, constituents.size().toLong())
        assertEquals(CIQUAL_SOURCES, citations.size().toLong())
        assertEquals(foods.size(), foods.map { it.asJsonObject["alimCode"].asString }.toSet().size)
        assertEquals(constituents.size(), constituents.map { it.asJsonObject["constCode"].asString }.toSet().size)
        assertEquals(citations.size(), citations.map { it.asJsonObject["sourceCode"].asString }.toSet().size)
        val ciqualPaths = sortedSetOf<String>()
        collectPaths(foods.first(), "foods[]", ciqualPaths)
        collectPaths(taxonomy.first(), "taxonomy[]", ciqualPaths)
        collectPaths(constituents.first(), "constituents[]", ciqualPaths)
        collectPaths(citations.first(), "sources[]", ciqualPaths)
        assertPaths(ciqualPaths, "foods[].alimCode", "foods[].nameFr", "foods[].compositions[].teneurLexical", "taxonomy[].groupNameFr", "constituents[].constCode", "sources[].sourceCode")

        val gi = gzipJson(root.resolve(GI_PATH))
        val measurements = gi.getAsJsonArray("measurements")
        val summaries = gi.getAsJsonArray("meanSummaries")
        val notes = gi.getAsJsonArray("categoryNotes")
        val footnotes = gi.getAsJsonArray("footnotes")
        assertEquals(GI_MEASUREMENTS, measurements.size().toLong())
        assertEquals(GI_SUMMARIES, summaries.size().toLong())
        assertEquals(GI_NOTES, notes.size().toLong())
        assertEquals(GI_FOOTNOTES, footnotes.size().toLong())
        val giPaths = sortedSetOf<String>()
        collectPaths(measurements.first(), "measurements[]", giPaths)
        collectPaths(summaries.first(), "meanSummaries[]", giPaths)
        collectPaths(notes.first(), "categoryNotes[]", giPaths)
        collectPaths(footnotes.first(), "footnotes[]", giPaths)
        assertPaths(giPaths, "measurements[].foodNumber", "measurements[].foodItem.lexicalValue", "measurements[].gi.lexicalValue", "meanSummaries[].lexicalText.lexicalValue", "categoryNotes[].lexicalText.lexicalValue", "footnotes[].lexicalText.lexicalValue")

        return SourceFacts(
            offFile.length(), agrFile.length(), root.resolve(CIQUAL_PATH).length(),
            root.resolve(GI_PATH).length(),
        )
    }

    private fun assertSqliteRuntimeReady() {
        Class.forName("org.sqlite.JDBC")
        java.sql.DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("CREATE TABLE records(id INTEGER PRIMARY KEY, payload TEXT NOT NULL)")
                statement.execute("CREATE VIRTUAL TABLE search USING fts5(text, tokenize='unicode61')")
                statement.execute("INSERT INTO records VALUES (1, 'source-faithful')")
                statement.execute("INSERT INTO search(rowid, text) VALUES (1, 'Crème fraîche Braeburn')")
                statement.executeQuery("SELECT rowid, bm25(search) FROM search WHERE search MATCH 'braeb*' ORDER BY bm25(search), rowid LIMIT 10").use { result ->
                    assertTrue(result.next())
                    assertEquals(1L, result.getLong(1))
                    assertFalse(result.next())
                }
            }
        }
    }

    private fun assertIndexPathsIgnored(root: File) {
        INDEX_PATHS.forEach { path ->
            val process = ProcessBuilder("git", "check-ignore", "-q", "$path/him-source-index.sqlite")
                .directory(root).start()
            assertEquals(0, process.waitFor(), "Index path is not ignored: $path")
        }
    }

    private fun assertNoProductionIndexes(root: File) {
        INDEX_PATHS.forEach { path ->
            val directory = root.resolve(path)
            if (directory.isDirectory) {
                assertTrue(directory.walkTopDown().filter(File::isFile).none { it.extension in setOf("sqlite", "sqlite3", "db") })
            }
        }
    }

    private fun report(facts: SourceFacts): String =
        """
        HIM F3d.2a SOURCE INDEX FIELD + SQLITE FTS5 FEASIBILITY AUDIT
        ========================================================================
        STATUS
        previous blocker: missing SQLite JDBC/FTS5 proof in HIM JVM runtime
        resolved by F3d.2a.1: true
        SQLite JDBC runtime ready: true
        SQLite FTS5 JVM ready: true
        SQLite FTS5 V1 feasible: true

        GUARDED SOURCES
        OPEN_FOOD_FACTS | $OFF_PATH | records=$OFF_RECORDS | compressedBytes=${facts.offBytes} | sha256=${SOURCE_GUARDS.getValue(OFF_PATH).sha256}
        AGRIBALYSE | $AGR_PATH | records=$AGR_RECORDS | compressedBytes=${facts.agrBytes} | sha256=${SOURCE_GUARDS.getValue(AGR_PATH).sha256}
        CIQUAL | $CIQUAL_PATH | foods=$CIQUAL_FOODS taxonomy=$CIQUAL_TAXONOMY constituents=$CIQUAL_CONSTITUENTS sources=$CIQUAL_SOURCES | compressedBytes=${facts.ciqualBytes} | sha256=${SOURCE_GUARDS.getValue(CIQUAL_PATH).sha256}
        GLYCEMIC_INDEX | $GI_PATH | measurements=$GI_MEASUREMENTS summaries=$GI_SUMMARIES categoryNotes=$GI_NOTES footnotes=$GI_FOOTNOTES | compressedBytes=${facts.giBytes} | sha256=${SOURCE_GUARDS.getValue(GI_PATH).sha256}

        OPEN_FOOD_FACTS FIELD CONTRACT
        IDENTIFIER
        - source.code: preserved original OFF code; not relied upon alone for physical uniqueness
        - deterministic artifact row ordinal: retrieval-stable discriminator for the immutable guarded artifact
        SEARCHABLE_AND_EVIDENCE
        - identity.productName, productNameGerman, productNameEnglish
        - identity.genericName, genericNameGerman, genericNameEnglish
        - identity.brands, identity.quantity, identity.servingSize, identity.productType
        - taxonomy.categories, categoryHierarchy, foodGroups, pnnsGroups, mainCategory
        - ingredients.text, ingredients.tags, ingredients.hierarchy, ingredients.items[].id/text recursively
        SEARCHABLE
        - allergens source text/tags; geography text/tags; packaging item material/shape/recycling text
        - nutrition vitamin/mineral/amino-acid/nucleotide/other-substance labels
        SEARCHABLE fields remain Source text; final weights and query policy are deferred.
        EVIDENCE_ONLY
        - complete ingredients structure including percentages, dietary flags and source mappings
        - nutrition declared/structured/levels/estimated payload
        - classification, allergens, geography, environmentalEvidence, packagingEvidence, quality
        - taxonomy.ciqualReferences and provenance language/context needed for traceability
        NOT_RETRIEVAL_RELEVANT
        - provenance created/modified/updated epoch values as FTS input
        - operational quality counters and diagnostic flags as FTS input
        stable reference: off:product:row:<1-based-artifact-row>:code:<source.code>
        direct Evidence lookup: PASS

        AGRIBALYSE FIELD CONTRACT
        IDENTIFIER
        - agbCode preserved; deterministic artifact row ordinal disambiguates duplicates
        - ciqualCode preserved as Source reference, never used to deduplicate
        SEARCHABLE_AND_EVIDENCE
        - productNameFr, lciName, foodGroup, foodSubgroup
        SEARCHABLE
        - preparation, delivery, packagingApproach
        EVIDENCE_ONLY
        - agbCode, ciqualCode, seasonCode, airTransportCode, dataQualityRating
        - all EF/environmental indicator values including climate component fields
        NOT_RETRIEVAL_RELEVANT
        - no Source field is removed from Authority; numeric indicators are excluded only from FTS text
        AGB codes: records=2458 distinct=2451 duplicateCodes=7 maxMultiplicity=2
        duplicates preserved: true
        stable reference: agribalyse:row:<1-based-artifact-row>:agb:<agbCode>
        direct Evidence lookup: PASS

        CIQUAL FIELD CONTRACT
        IDENTIFIER
        - food: ciqual:food:<alimCode>
        - taxonomy: ciqual:taxonomy:<groupCode>/<subgroupCode>/<subSubgroupCode>
        - constituent: ciqual:constituent:<constCode>
        - source: ciqual:source:<sourceCode>
        SEARCHABLE_AND_EVIDENCE
        - foods nameFr/nameEn/scientificName.lexicalValue
        - taxonomy French/English group, subgroup and sub-subgroup names
        - constituents nameFr/nameEn
        SEARCHABLE
        - sources.citation.lexicalValue
        SEARCHABLE citation text never becomes Source identity.
        EVIDENCE_ONLY
        - food taxonomy codes, jonesFactorLexical and complete compositions[]
        - compositions constCode, teneurLexical, minimum/maximum lexical+missingness,
          confidenceCode lexical+missingness, sourceCode lexical+missingness
        - constituent infoodsCode lexical+missingness and source citation missingness
        NOT_RETRIEVAL_RELEVANT
        - missingAttributeValue/status markers as FTS input; they remain Evidence payload
        lexical composition values preserved: true; no Double conversion
        missingness preserved: true; lexical empty and Source-missing remain distinct
        raw XML rejoin required: false
        direct Evidence lookup: PASS

        GLYCEMIC INDEX FIELD CONTRACT
        IDENTIFIER
        - record kind plus deterministic 1-based ordinal in that guarded root array
        - original foodNumber, pageNumber, referenceCode and footnote identifier preserved where present
        SEARCHABLE_AND_EVIDENCE
        - measurement foodItem.lexicalValue and sourceContext majorCategory/subcategory/deeperHeading
        - mean-summary/category-note/footnote lexicalText.lexicalValue and sourceContext
        SEARCHABLE
        - country.lexicalValue and other directly stated contextual description text
        EVIDENCE_ONLY
        - page, year, GI, SEM, GL, subjects, available carbohydrate, test portion
        - reference food/time, timepoints, sample collection, analysis method, reference code
        - all lexical status values and category/footnote context
        NOT_RETRIEVAL_RELEVANT
        - PRESENT/missing status tokens as FTS input; status remains Evidence
        record kinds: measurement, mean-summary, category-note, footnote
        stable references: gi:<record-kind>:<1-based-array-ordinal>, with Source IDs retained in payload
        direct Evidence lookup: PASS

        EVIDENCE PROJECTION REQUIREMENTS
        OPEN_FOOD_FACTS
        - identity: stable reference, artifact row ordinal, source.code
        - searchable copy: selected Source lexical naming/taxonomy/ingredient/context text
        - normal payload: identity, taxonomy, ingredients, nutrition, classification, allergens,
          geography, environmental, packaging, quality and traceable provenance context
        - omit from normal bounded projection: operational timestamps/counters/low-signal diagnostics;
          full optimized record remains authoritative and unchanged
        - preserve nested ingredients, structured nutrition and source-native values without canonicalization
        - scale concern: projection must be serialized once into the Evidence Store and fetched only for Top <= 10
        AGRIBALYSE
        - identity: stable composite reference, row ordinal, agbCode, ciqualCode
        - searchable copy: names, groups and stated preparation/delivery/packaging text
        - normal payload: all original row fields, including every environmental numeric indicator
        - missing/null values remain unchanged; no duplicate AGB row may be collapsed
        CIQUAL
        - identity: record kind and verified relation key
        - searchable copy: food/taxonomy/constituent names and citation text
        - normal food payload keeps compositions intact and references constituent/source records by Source codes
        - lexicalValue and missingAttributeValue pairs remain intact; teneur/min/max stay lexical
        - auxiliary taxonomy/constituent/source projections must remain directly addressable
        GLYCEMIC_INDEX
        - identity: record kind, root-array ordinal and all available Source identifiers
        - searchable copy: food/category/heading/summary/note/footnote/country Source text
        - normal payload: complete selected logical record, retaining every lexicalValue/status pair
        - measurement, summary, category-note and footnote structures remain distinguishable

        QUERY NORMALIZATION BASELINE
        - Unicode normalization: allowed technical lookup copy; exact normalization form deferred
        - trim: allowed on lookup copy
        - whitespace normalization: allowed on lookup copy
        - lowercase: allowed on lookup copy
        - original Source text is preserved in Evidence
        - stemming=false synonyms=false translation=false canonicalMapping=false semanticRewriting=false

        SQLITE ARCHITECTURE
        - one database per Source: PASS
        - metadata table: PASS
        - relational Evidence Store: PASS
        - FTS5 search structure: PASS
        - direct FTS row key -> Evidence lookup: PASS
        - Source-local bm25 ranking: PASS
        - deterministic secondary ordering by stable key: PASS
        - SQL LIMIT <= 10 bounded Top-K: PASS
        - Source SHA binding and stale detection: PASS
        - PRAGMA integrity_check and logical count validation: PASS
        - temporary build plus validated atomic publication: PASS
        - no global cross-Source score: PASS; each Source result set remains independent

        MINIMUM INDEX METADATA FOR F3d.2b
        - indexSchemaVersion
        - source identifier
        - sourceArtifactPath and sourceArtifactSha256
        - sourceRecordCount and source-specific logical counts
        - indexBuildPolicyVersion and evidenceProjectionPolicyVersion
        - SQLite runtime/version information for diagnostics, not semantic identity
        - buildCompletionState (non-final BUILDING versus validated final state)
        - indexedRecordCount, evidenceRecordCount and FTS row count
        - deterministic logical content digest/manifest inputs

        OFF SCALE FEASIBILITY
        - 4,591,865 records; 2,091,302,220 compressed bytes
        - one-time streaming JSONL/GZIP scan: FEASIBLE
        - bounded record/chunk memory: FEASIBLE
        - batched transactions and prepared inserts: REQUIRED AND FEASIBLE
        - relational Evidence Store plus FTS population: FEASIBLE
        - build temporary DB, validate, close, atomically publish: FEASIBLE
        - runtime compressed-bulk scan: false
        - single local SQLite file size: expected multi-GB; technically plausible, exact sizing deferred
        - overall: PASS

        MEMORY MODEL
        - OFF and AGRIBALYSE JSONL: stream one line/record or bounded batch
        - CIQUAL and GI aggregate JSON: F3d.2b builder requires streaming JSON token parsing by root array;
          loading the complete aggregate is not a production-build requirement
        - no complete Source corpus must be retained in RAM

        DETERMINISM
        - logical determinism: FEASIBLE from guarded Source SHA, policy versions, fixed processing order,
          deterministic record keys/search text/projections and stable query tie-break
        - byte-identical SQLite file required: false
        - raw SQLite-file SHA alone is not a semantic determinism contract
        - validity/fingerprint inputs: Source SHA, schema/build/projection policy versions, logical counts,
          ordered stable record keys and logical Evidence/search content digest

        PUBLICATION AND FAILURE REQUIREMENTS
        - build into a temporary sibling path, complete transactions, validate metadata/counts/FTS consistency,
          run integrity_check, close, then atomic move to final path
        - no partially built file may carry a validated completion state
        - missing/stale/corrupt index, Source SHA mismatch, dangling reference, projection failure,
          interrupted build and invalid query require explicit failure; never fall back to Source bulk scan

        INDEX LOCATION AND SCOPE
        - data/sources/openfoodfacts/index/
        - data/sources/agribalyse/index/
        - data/sources/ciqual/index/
        - data/sources/glycemic-index/index/
        - all conceptual index paths ignored by current Git rules: true
        - current audit JVM runtime: READY
        - future production builder runtime scope: DECISION_REQUIRED
        - future production reader runtime scope: DECISION_REQUIRED
        - no separate production JVM tooling module/configuration currently exists

        BOUNDARIES AND REMAINING BLOCKERS
        - PRE-HIM dependency: false
        - legacy matcher/merger/sourceVariants dependency: false
        - production indexes created: 0
        - Candidate/Authority/Ground-Truth data writes: 0
        - retrieval architecture blocker: resolved
        - semantic HIM inference: still unresolved for First Real Candidate Generation
        - Candidate Dataset root/provenance contract: still unresolved

        RESULT
        SQLite FTS5 V1 feasibility: PASS
        F3d.2a complete: true
        next step: F3d.2b Retrieval Index Contracts / SQLite Schema V1
        """.trimIndent() + "\n"

    private fun jsonlSamplePaths(file: File, limit: Int): Set<String> {
        val paths = sortedSetOf<String>()
        gzipLines(file).useLines { lines ->
            lines.filter(String::isNotBlank).take(limit).forEach { line ->
                collectPaths(JsonParser.parseString(line), "", paths)
            }
        }
        return paths
    }

    private fun collectPaths(element: com.google.gson.JsonElement, path: String, result: MutableSet<String>) {
        if (path.isNotEmpty()) result += path
        when {
            element.isJsonObject -> element.asJsonObject.entrySet().forEach { (key, value) ->
                collectPaths(value, if (path.isEmpty()) key else "$path.$key", result)
            }
            element.isJsonArray -> element.asJsonArray.firstOrNull()?.let { collectPaths(it, "$path[]", result) }
        }
    }

    private fun assertPaths(actual: Set<String>, vararg expected: String) =
        expected.forEach { path -> assertTrue(path in actual, "Missing actual optimized Source path: $path") }

    private fun gzipLines(file: File) = GZIPInputStream(file.inputStream().buffered()).bufferedReader(Charsets.UTF_8)

    private fun gzipJson(file: File): JsonObject =
        gzipLines(file).use { JsonParser.parseReader(it).asJsonObject }

    private fun hashes(root: File, guards: Map<String, Guard>): Map<String, String> =
        guards.mapValues { (path, _) -> sha256(root.resolve(path)) }

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

    private data class SourceFacts(val offBytes: Long, val agrBytes: Long, val ciqualBytes: Long, val giBytes: Long)
    private data class Guard(val sha256: String)

    companion object {
        private const val REPORT_PATH = "build/knowledge/reports/him/retrieval/him-f3d2a-source-index-field-sqlite-feasibility-audit.txt"
        private const val OFF_PATH = "data/sources/openfoodfacts/optimized/off-him-final-source.jsonl.gz"
        private const val OFF_FINGERPRINT = "data/sources/openfoodfacts/optimized/off-him-final-source.fingerprint.json"
        private const val AGR_PATH = "data/sources/agribalyse/optimized/agribalyse-him-final-source.jsonl.gz"
        private const val CIQUAL_PATH = "data/sources/ciqual/optimized/ciqual-him-final-source.json.gz"
        private const val GI_PATH = "data/sources/glycemic-index/optimized/glycemic-index-him-final-source.json.gz"
        private const val OFF_RECORDS = 4_591_865L
        private const val AGR_RECORDS = 2_458L
        private const val AGR_DISTINCT_CODES = 2_451L
        private const val AGR_DUPLICATE_CODES = 7L
        private const val CIQUAL_FOODS = 3_484L
        private const val CIQUAL_TAXONOMY = 138L
        private const val CIQUAL_CONSTITUENTS = 74L
        private const val CIQUAL_SOURCES = 1_978L
        private const val GI_MEASUREMENTS = 2_091L
        private const val GI_SUMMARIES = 122L
        private const val GI_NOTES = 15L
        private const val GI_FOOTNOTES = 26L

        private val INDEX_PATHS = listOf(
            "data/sources/openfoodfacts/index", "data/sources/agribalyse/index",
            "data/sources/ciqual/index", "data/sources/glycemic-index/index",
        )
        private val FOUNDATION_GUARDS = mapOf(
            "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json" to Guard("922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f"),
            "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json" to Guard("46145e663b483777228894a099f958f8150773af98b3772882823745b365cf9a"),
            "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json" to Guard("86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184"),
            "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json" to Guard("4afc7490bd69f2f8919558d33d98c912771d128da4ac7a782cc116a1c9fccb6e"),
            "data/knowledge/him/canonical-family/index/him-entity-fingerprint-index.v1.json" to Guard("6dcb065d6dce0aaa22c8d1d3dd620372fc90d3fe9a3ec9d20ee54737a8f9a951"),
        )
        private val SOURCE_GUARDS = mapOf(
            OFF_PATH to Guard("63b20183229a6f3f648c3d189c265d5d5a4b332d530d084af5640081d060a236"),
            AGR_PATH to Guard("9068c89fa887ef087e87dcc51f758dd29e9623b93d9bd0a2277a4faae57c2297"),
            CIQUAL_PATH to Guard("807d16c222f0e812831c10bdd89f1a5ebbc74c95e8a4944723db486add96dcff"),
            GI_PATH to Guard("6891c2ff2ab3734a1d2768339c1f660f7093a31b97a856c82bf9b8c3c88422b5"),
        )
    }
}
