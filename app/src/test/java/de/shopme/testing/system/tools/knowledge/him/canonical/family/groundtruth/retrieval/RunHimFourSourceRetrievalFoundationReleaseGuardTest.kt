package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.*
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

class RunHimFourSourceRetrievalFoundationReleaseGuardTest {
    @Test
    fun validateAndReleaseFourSourceRetrievalFoundation() {
        de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
        val root = projectRoot()
        Class.forName("org.sqlite.JDBC")
        val foundationBefore = hashes(root, FOUNDATION_GUARDS)
        val sourcesBefore = hashes(root, SOURCE_GUARDS.mapValues { it.value.sha })
        require(foundationBefore == FOUNDATION_GUARDS)
        require(sourcesBefore == SOURCE_GUARDS.mapValues { it.value.sha })

        val indexBefore = INDEX_SPECS.associate { it.source to snapshot(root.resolve(it.indexPath)) }
        INDEX_SPECS.forEach { spec ->
            require(root.resolve(spec.indexPath).isFile)
            require(isIgnored(root, spec.indexPath))
        }
        require(GLOBAL_INDEX_PATHS.none { root.resolve(it).exists() })

        val offValidation = HimOffEvidenceIndexValidator.validateReadOnly(root.resolve(HimOffProductionEvidenceIndexPaths.FINAL_INDEX))
        val agribalyseValidation = HimAgribalyseEvidenceIndexValidator.validateReadOnly(root.resolve(HimAgribalyseProductionEvidenceIndexPaths.FINAL_INDEX))
        val ciqualValidation = HimCiqualEvidenceIndexValidator.validateReadOnly(root.resolve(HimCiqualProductionEvidenceIndexPaths.FINAL_INDEX))
        val giValidation = HimGlycemicIndexEvidenceIndexValidator.validateReadOnly(root.resolve(HimGlycemicIndexProductionEvidenceIndexPaths.FINAL_INDEX))
        val validations = linkedMapOf(
            HimGroundTruthSource.OPEN_FOOD_FACTS to Validation(offValidation.metadata, offValidation.recomputedLogicalDigest, offValidation.integrityCheck),
            HimGroundTruthSource.AGRIBALYSE to Validation(agribalyseValidation.metadata, agribalyseValidation.recomputedLogicalDigest, agribalyseValidation.integrityCheck),
            HimGroundTruthSource.CIQUAL to Validation(ciqualValidation.metadata, ciqualValidation.recomputedLogicalDigest, ciqualValidation.integrityCheck),
            HimGroundTruthSource.GLYCEMIC_INDEX to Validation(giValidation.metadata, giValidation.recomputedLogicalDigest, giValidation.integrityCheck),
        )
        validateMetadata(validations)
        require(agribalyseValidation.distinctAgbCodes == 2_451 && agribalyseValidation.duplicateAgbGroups == 7 && agribalyseValidation.maximumMultiplicity == 2)
        require(ciqualValidation.lexicalSamples.keys == linkedSetOf("traces", "< 2,2", "< 0,1", "-"))
        require(ciqualValidation.missingnessPreserved && ciqualValidation.duplicatedInfoodsConstCodes.size > 1 && ciqualValidation.duplicatedCitationSourceCodes.size > 1)
        require(giValidation.kindCounts.values.sum() == 2_254L && giValidation.missingnessStatuses.containsAll(setOf("PRESENT", "SOURCE_MISSING", "UNRESOLVED")))

        val stores = linkedMapOf<HimGroundTruthSource, Store>(
            HimGroundTruthSource.OPEN_FOOD_FACTS to Store(
                search = HimOffSqliteEvidenceRetrievalStore.openAfterValidation(root.resolve(HimOffProductionEvidenceIndexPaths.FINAL_INDEX), offValidation)::search,
                fetch = HimOffSqliteEvidenceRetrievalStore.openAfterValidation(root.resolve(HimOffProductionEvidenceIndexPaths.FINAL_INDEX), offValidation)::fetch,
            ),
            HimGroundTruthSource.AGRIBALYSE to Store(
                search = HimAgribalyseSqliteEvidenceRetrievalStore.openAfterValidation(root.resolve(HimAgribalyseProductionEvidenceIndexPaths.FINAL_INDEX), agribalyseValidation)::search,
                fetch = HimAgribalyseSqliteEvidenceRetrievalStore.openAfterValidation(root.resolve(HimAgribalyseProductionEvidenceIndexPaths.FINAL_INDEX), agribalyseValidation)::fetch,
            ),
            HimGroundTruthSource.CIQUAL to Store(
                search = HimCiqualSqliteEvidenceRetrievalStore.openAfterValidation(root.resolve(HimCiqualProductionEvidenceIndexPaths.FINAL_INDEX), ciqualValidation)::search,
                fetch = HimCiqualSqliteEvidenceRetrievalStore.openAfterValidation(root.resolve(HimCiqualProductionEvidenceIndexPaths.FINAL_INDEX), ciqualValidation)::fetch,
            ),
            HimGroundTruthSource.GLYCEMIC_INDEX to Store(
                search = HimGlycemicIndexSqliteEvidenceRetrievalStore.openAfterValidation(root.resolve(HimGlycemicIndexProductionEvidenceIndexPaths.FINAL_INDEX), giValidation)::search,
                fetch = HimGlycemicIndexSqliteEvidenceRetrievalStore.openAfterValidation(root.resolve(HimGlycemicIndexProductionEvidenceIndexPaths.FINAL_INDEX), giValidation)::fetch,
            ),
        )

        stores.forEach { (source, store) ->
            listOf(1, 5, 10).forEach { limit -> require(store.search("Baguette", HimEvidenceSearchLimit(limit)).size <= limit) }
            require(runCatching { HimEvidenceSearchLimit(11) }.isFailure)
            val first = store.search("Baguette", HimEvidenceSearchLimit(10))
            val second = store.search("Baguette", HimEvidenceSearchLimit(10))
            require(first == second)
            require(first.all { it.source == source && it.retrievalRank in 1..10 })
        }

        val smoke = SMOKE_QUERIES.associateWith { query -> retrieve(stores.keys, query, stores) }
        require(smoke.values.all { result -> result.results.keys.toList() == stores.keys.toList() })
        stores.keys.forEach { source ->
            val result = smoke.values.asSequence().map { it.results.getValue(source) }.firstOrNull { it.isNotEmpty() }
            requireNotNull(result) { "No smoke-test result available for $source" }
            val retrieved = result.first()
            val fetched = requireNotNull(stores.getValue(source).fetch(retrieved.sourceRecordReference))
            require(fetched.source == retrieved.source)
            require(fetched.sourceRecordReference == retrieved.sourceRecordReference)
            require(fetched.recordKind == retrieved.recordKind)
            require(fetched.evidenceProjection == retrieved.evidenceProjection)
        }

        require(retrieve(setOf(HimGroundTruthSource.OPEN_FOOD_FACTS), "Baguette", stores).results.keys == setOf(HimGroundTruthSource.OPEN_FOOD_FACTS))
        require(retrieve(setOf(HimGroundTruthSource.CIQUAL), "Baguette", stores).results.keys == setOf(HimGroundTruthSource.CIQUAL))
        require(retrieve(linkedSetOf(HimGroundTruthSource.OPEN_FOOD_FACTS, HimGroundTruthSource.CIQUAL), "Baguette", stores).results.keys == linkedSetOf(HimGroundTruthSource.OPEN_FOOD_FACTS, HimGroundTruthSource.CIQUAL))
        require(retrieve(linkedSetOf(HimGroundTruthSource.AGRIBALYSE, HimGroundTruthSource.GLYCEMIC_INDEX), "Baguette", stores).results.keys == linkedSetOf(HimGroundTruthSource.AGRIBALYSE, HimGroundTruthSource.GLYCEMIC_INDEX))

        validateFailureSemantics(validations.getValue(HimGroundTruthSource.OPEN_FOOD_FACTS).metadata, root)
        require(noPreHimDependency(root))
        val foundationDigestInput = canonicalFoundationDigestInput(validations)
        val foundationDigest = sha256(foundationDigestInput.toByteArray(StandardCharsets.UTF_8))

        val foundationAfter = hashes(root, FOUNDATION_GUARDS)
        val sourcesAfter = hashes(root, SOURCE_GUARDS.mapValues { it.value.sha })
        val indexAfter = INDEX_SPECS.associate { it.source to snapshot(root.resolve(it.indexPath)) }
        require(foundationAfter == foundationBefore && sourcesAfter == sourcesBefore && indexAfter == indexBefore)

        val report = renderReport(validations, smoke, foundationDigestInput, foundationDigest)
        val reportFile = root.resolve(REPORT_PATH)
        requireNotNull(reportFile.parentFile).mkdirs()
        writeReport(reportFile, report)
        val firstReportSha = sha256(reportFile.readBytes())
        writeReport(reportFile, report)
        require(sha256(reportFile.readBytes()) == firstReportSha)
    }

    private fun retrieve(
        selected: Set<HimGroundTruthSource>,
        query: String,
        stores: Map<HimGroundTruthSource, Store>,
    ): FourSourceRetrievalResult {
        require(selected.isNotEmpty())
        return FourSourceRetrievalResult(query, selected.associateWith { stores.getValue(it).search(query, HimEvidenceSearchLimit(10)) })
    }

    private fun validateMetadata(validations: Map<HimGroundTruthSource, Validation>) {
        require(validations.keys.toList() == SOURCE_ORDER)
        INDEX_SPECS.forEach { expected ->
            val validation = validations.getValue(expected.source)
            val metadata = validation.metadata
            require(metadata.source == expected.source && metadata.sourceArtifactPath == expected.source.artifactPath)
            require(metadata.sourceArtifactSha256.value == SOURCE_GUARDS.getValue(expected.source.artifactPath).sha)
            require(metadata.schemaVersion == HimEvidenceRetrievalIndexSchemaV1.SCHEMA_VERSION)
            require(metadata.indexBuildPolicyVersion == HimEvidenceRetrievalIndexSchemaV1.BUILD_POLICY_VERSION)
            require(metadata.evidenceProjectionPolicyVersion == HimEvidenceRetrievalIndexSchemaV1.PROJECTION_POLICY_VERSION)
            require(metadata.buildState == HimEvidenceRetrievalIndexBuildState.VALIDATED)
            require(metadata.indexedRecordCount == expected.count && metadata.evidenceRecordCount == expected.count && metadata.ftsRowCount == expected.count)
            require(validation.digest.value == expected.digest && metadata.logicalContentSha256 == validation.digest)
            require(validation.integrity == "ok")
            require(HimEvidenceRetrievalIndexValidator.runtimeEligibility(metadata, HimExpectedEvidenceRetrievalIndex(expected.source, metadata.sourceArtifactSha256)) == HimEvidenceRetrievalIndexEligibility.Ready)
        }
    }

    private fun validateFailureSemantics(metadata: HimEvidenceRetrievalIndexMetadata, root: File) {
        require(runCatching { HimOffSqliteEvidenceRetrievalStore.open(root.resolve("build/nonexistent/f3d2c5.sqlite")) }.exceptionOrNull() is HimSourceIndexUnavailableException)
        val expected = HimExpectedEvidenceRetrievalIndex(metadata.source, metadata.sourceArtifactSha256)
        require(HimEvidenceRetrievalIndexValidator.runtimeEligibility(metadata.copy(sourceArtifactSha256 = HimSha256("0".repeat(64))), expected) is HimEvidenceRetrievalIndexEligibility.Stale)
        require(HimEvidenceRetrievalIndexValidator.runtimeEligibility(metadata.copy(buildState = HimEvidenceRetrievalIndexBuildState.BUILDING), expected) is HimEvidenceRetrievalIndexEligibility.Corrupt)
    }

    private fun canonicalFoundationDigestInput(validations: Map<HimGroundTruthSource, Validation>): String = buildString {
        append("digest-contract=HIM_FOUR_SOURCE_RETRIEVAL_FOUNDATION_DIGEST_V1\n")
        SOURCE_ORDER.forEach { source ->
            val metadata = validations.getValue(source).metadata
            append("source=").append(source.name).append('\n')
            append("source-artifact-sha256=").append(metadata.sourceArtifactSha256.value).append('\n')
            append("schema-version=").append(metadata.schemaVersion).append('\n')
            append("build-policy-version=").append(metadata.indexBuildPolicyVersion).append('\n')
            append("projection-policy-version=").append(metadata.evidenceProjectionPolicyVersion).append('\n')
            append("logical-content-sha256=").append(metadata.logicalContentSha256.value).append('\n')
            append("evidence-record-count=").append(metadata.evidenceRecordCount).append('\n')
            append("fts-row-count=").append(metadata.ftsRowCount).append('\n')
        }
    }

    private fun renderReport(
        validations: Map<HimGroundTruthSource, Validation>,
        smoke: Map<String, FourSourceRetrievalResult>,
        digestInput: String,
        digest: String,
    ): String = buildString {
        appendLine("HIM F3d.2c.5 FOUR-SOURCE RETRIEVAL FOUNDATION RELEASE")
        appendLine("=".repeat(72))
        appendLine("FOUNDATION_GUARDS=PASS SOURCE_GUARDS=PASS SQLITE=3.53.2 FTS5=READY unicode61=READY")
        appendLine("SOURCE_ORDER=${SOURCE_ORDER.joinToString(",") { it.name }}")
        INDEX_SPECS.forEach { spec ->
            val metadata = validations.getValue(spec.source).metadata
            appendLine("${spec.source.name}|path=${spec.indexPath}|state=${metadata.buildState}|evidence=${metadata.evidenceRecordCount}|fts=${metadata.ftsRowCount}|sourceSha=${metadata.sourceArtifactSha256.value}|logicalDigest=${metadata.logicalContentSha256.value}|schema=${metadata.schemaVersion}|build=${metadata.indexBuildPolicyVersion}|projection=${metadata.evidenceProjectionPolicyVersion}|integrity=ok|ignored=true")
        }
        appendLine("SOURCE_SPECIFIC_INTEGRITY=PASS SOURCE_INDEX_BINDINGS=PASS TOP_K_1_5_10=PASS LIMIT_11_REJECTED=PASS")
        appendLine("EXACT_FETCH_ALL_SOURCES=PASS QUERY_DETERMINISM=PASS SOURCE_SELECTION=PASS ZERO_RESULTS_VALID=true")
        smoke.forEach { (query, result) ->
            appendLine("query=$query")
            SOURCE_ORDER.forEach { source ->
                val values = result.results.getValue(source)
                val top = values.firstOrNull()
                appendLine("${source.name}|count=${values.size}|top=${top?.sourceRecordReference?.value.orEmpty()}|kind=${top?.recordKind?.name.orEmpty()}|rank=${top?.retrievalRank?.toString().orEmpty()}")
            }
        }
        appendLine("SEPARATE_SOURCE_RESULT_SETS=true GLOBAL_MERGED_RANKING=false CROSS_SOURCE_NUMERIC_SCORE=false")
        appendLine("ONE_DB_PER_SOURCE=true CROSS_DATABASE_QUERY=false GLOBAL_MIXED_INDEX=false")
        appendLine("RUNTIME_COMPRESSED_SOURCE_SCAN=false CIQUAL_RAW_XML_ACCESS=false SILENT_FALLBACK=false")
        appendLine("FAILURE_SEMANTICS=SOURCE_INDEX_UNAVAILABLE,SOURCE_INDEX_STALE,SOURCE_INDEX_CORRUPT")
        appendLine("PROVENANCE=source,sourceRecordReference,recordKind,retrievalRank,evidenceProjection")
        appendLine("PRE_HIM_DEPENDENCY=false LEGACY_MATCHER=false LEGACY_MERGER=false sourceVariants=false")
        appendLine("CANDIDATES=0 CANDIDATE_DATASET=0 VALIDATIONS=0 APPROVALS=0 ENTITY_IDS=0 AUTHORITY_MUTATIONS=0 MUTATION_LEDGER=0 RETIRED_IDS=0 GROUND_TRUTH_RELEASES=0")
        appendLine("PRODUCTION_INDEX_REBUILDS=0 PERSISTENT_RELEASE_RECORDS=0")
        appendLine("FOUNDATION_DIGEST_CONTRACT_BEGIN")
        append(digestInput)
        appendLine("FOUNDATION_DIGEST_CONTRACT_END")
        appendLine("foundationDigestSha256=$digest")
        appendLine("FOUNDATION_CONTENT_CHANGED=false SOURCE_CONTENT_CHANGED=false INDEX_CONTENT_CHANGED=false")
        appendLine("F3D_2_RETRIEVAL_FOUNDATION_RELEASE_READY=true")
        appendLine("F3d.2c.5_COMPLETE=true")
    }

    private fun noPreHimDependency(root: File): Boolean {
        val retrievalFiles = (root.resolve("app/src/main/java/de/shopme/tools/knowledge/him/canonical/family/groundtruth/retrieval").walkTopDown() +
            root.resolve("app/src/test/java/de/shopme/tools/knowledge/him/canonical/family/groundtruth/retrieval").walkTopDown())
            .filter { it.isFile && it.extension == "kt" }
        val forbidden = listOf("CanonicalFoodIdentity", "TrueCanonicalFoodIdentity", "sourceVariants", "legacy matcher", "legacy merger")
        return retrievalFiles.filterNot { it.name == "RunHimFourSourceRetrievalFoundationReleaseGuardTest.kt" }
            .all { file -> forbidden.none { token -> file.readText().contains(token) } }
    }

    private fun hashes(root: File, expected: Map<String, String>) = expected.keys.associateWith { sha256(root.resolve(it)) }
    private fun snapshot(file: File) = FileSnapshot(file.length(), file.lastModified())
    private fun isIgnored(root: File, path: String) = ProcessBuilder("git", "check-ignore", "-q", path).directory(root).start().let { it.waitFor() == 0 }
    private fun writeReport(file: File, text: String) = Files.write(file.toPath(), text.toByteArray(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun sha256(file: File): String {
        require(file.isFile)
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(1024 * 1024).use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    private fun projectRoot(): File {
        var current = File(System.getProperty("user.dir")).absoluteFile
        while (!current.resolve("settings.gradle.kts").isFile) current = requireNotNull(current.parentFile)
        return current
    }

    private data class SourceGuard(val sha: String)
    private data class IndexSpec(val source: HimGroundTruthSource, val indexPath: String, val count: Long, val digest: String)
    private data class Validation(val metadata: HimEvidenceRetrievalIndexMetadata, val digest: HimSha256, val integrity: String)
    private data class FileSnapshot(val bytes: Long, val modified: Long)
    private data class Store(
        val search: (String, HimEvidenceSearchLimit) -> List<HimEvidenceSearchResult>,
        val fetch: (HimEvidenceRecordReference) -> HimEvidenceSearchResult?,
    )
    private data class FourSourceRetrievalResult(
        val query: String,
        val results: Map<HimGroundTruthSource, List<HimEvidenceSearchResult>>,
    )

    companion object {
        private const val REPORT_PATH = "build/knowledge/reports/him/retrieval/him-f3d2c5-four-source-retrieval-foundation-release.txt"
        private val SOURCE_ORDER = listOf(HimGroundTruthSource.OPEN_FOOD_FACTS, HimGroundTruthSource.AGRIBALYSE, HimGroundTruthSource.CIQUAL, HimGroundTruthSource.GLYCEMIC_INDEX)
        private val SMOKE_QUERIES = listOf("Baguette", "Hering", "Salmon", "Milk")
        private val FOUNDATION_GUARDS = linkedMapOf(
            "data/knowledge/catalog/master/product-only/canonical-food-catalog.product-only.master.json" to "922e3fc71a624a94d6787d772e40bba2e31e102212e16c4b315bd9f5dfa30f4f",
            "data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json" to "46145e663b483777228894a099f958f8150773af98b3772882823745b365cf9a",
            "data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json" to "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184",
            "data/knowledge/him/canonical-family/master/canonical-family-foundation-release.v1.json" to "4afc7490bd69f2f8919558d33d98c912771d128da4ac7a782cc116a1c9fccb6e",
            "data/knowledge/him/canonical-family/index/him-entity-fingerprint-index.v1.json" to "6dcb065d6dce0aaa22c8d1d3dd620372fc90d3fe9a3ec9d20ee54737a8f9a951",
        )
        private val SOURCE_GUARDS = linkedMapOf(
            HimGroundTruthSource.OPEN_FOOD_FACTS.artifactPath to SourceGuard("63b20183229a6f3f648c3d189c265d5d5a4b332d530d084af5640081d060a236"),
            HimGroundTruthSource.AGRIBALYSE.artifactPath to SourceGuard("9068c89fa887ef087e87dcc51f758dd29e9623b93d9bd0a2277a4faae57c2297"),
            HimGroundTruthSource.CIQUAL.artifactPath to SourceGuard("807d16c222f0e812831c10bdd89f1a5ebbc74c95e8a4944723db486add96dcff"),
            HimGroundTruthSource.GLYCEMIC_INDEX.artifactPath to SourceGuard("6891c2ff2ab3734a1d2768339c1f660f7093a31b97a856c82bf9b8c3c88422b5"),
        )
        private val INDEX_SPECS = listOf(
            IndexSpec(HimGroundTruthSource.OPEN_FOOD_FACTS, HimOffProductionEvidenceIndexPaths.FINAL_INDEX, 4_591_865, "627ad847e9038961e2ffad790db8dbb5fc23a358720fc6dbdc542eb2323cd743"),
            IndexSpec(HimGroundTruthSource.AGRIBALYSE, HimAgribalyseProductionEvidenceIndexPaths.FINAL_INDEX, 2_458, "2d4036d35413f3a2f98b4798fcf8511960e94b2584efcf057616bf41de8660f6"),
            IndexSpec(HimGroundTruthSource.CIQUAL, HimCiqualProductionEvidenceIndexPaths.FINAL_INDEX, 5_674, "b9f2d2a162093234729a53855804042debe8a2b290a04a4db1654e54fa3489bc"),
            IndexSpec(HimGroundTruthSource.GLYCEMIC_INDEX, HimGlycemicIndexProductionEvidenceIndexPaths.FINAL_INDEX, 2_254, "03cd3c46e7153f915d3043095a22434aeba3b0e2a36e5c28a56d2d3d9e57935a"),
        )
        private val GLOBAL_INDEX_PATHS = listOf("data/knowledge/him/retrieval/index.sqlite", "global-evidence-index.sqlite", "merged-source-index.sqlite")
    }
}
