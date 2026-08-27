package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPaths
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyValidator
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMasterReader
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisReportV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewAssociationStateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusReportV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusRuntimeRequestV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusRuntimeResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewCorpusRuntimeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryReviewStateV1
import java.io.File
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Assume.assumeTrue

class RunHimZeroCandidateRecoveryReviewCorpusRealV1Test {

    @Test
    fun `recovery review gate is disabled without explicit opt in`() {
        val gate = gate(null, null)
        assertFalse(gate.enabled)
        assertFalse(gate.confirmation == RECOVERY_REVIEW_CONFIRMATION)
    }

    @Test
    fun `wrong recovery review confirmation is rejected before input access`() {
        val gate = gate("true", "WRONG_CONFIRMATION")
        assertTrue(gate.enabled)
        assertFalse(gate.confirmation == RECOVERY_REVIEW_CONFIRMATION)
    }

    @Test
    fun `real bound recovery review path has no store search fetch scan sqlite or inference dependency`() {
        val forbidden = listOf("Store", "Search", "Fetch", "Scan", "SQLite", "OpenAI", "Provider", "Inference")
        val types = listOf(
            HimZeroCandidateRecoveryReviewCorpusRuntimeRequestV1::class.java,
            HimZeroCandidateRecoveryReviewCorpusRuntimeV1::class.java,
        )
        val names = types.flatMap { type ->
            type.declaredFields.map { it.type.simpleName } +
                type.declaredMethods.flatMap { method ->
                    listOf(method.name, method.returnType.simpleName) + method.parameterTypes.map { it.simpleName }
                } +
                type.declaredConstructors.flatMap { constructor -> constructor.parameterTypes.map { it.simpleName } }
        }
        assertTrue(names.none { name -> forbidden.any { token -> name.contains(token, ignoreCase = true) } })
    }

    @Test
    fun `typed recovery review diagnostics do not expose paths or throwable text`() {
        val result = HimZeroCandidateRecoveryReviewCorpusRuntimeResult.Failed(
            HimZeroCandidateRecoveryReviewFailureReasonV1.CAUSE_ANALYSIS_BINDING_MISMATCH,
            "causeAnalysisDigest",
        )
        val message = diagnostic(result)
        assertEquals("CAUSE_ANALYSIS_BINDING_MISMATCH causeAnalysisDigest", message)
        assertFalse(message.contains("/"))
        assertFalse(message.contains("Exception"))
        assertFalse(message.contains("Throwable"))
    }

    @Test
    fun `runtime request fields remain exactly the committed input model`() {
        val fields = HimZeroCandidateRecoveryReviewCorpusRuntimeRequestV1::class.java
            .declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
        assertEquals(
            setOf(
                "enabled",
                "causeAnalysis",
                "catalog",
                "registry",
                "authority",
                "inputBinding",
                "jsonOutputFile",
                "textOutputFile",
            ),
            fields.map { it.name }.toSet(),
        )
        assertTrue(fields.none { it.name.contains("port", ignoreCase = true) })
    }

    @Test
    fun `operational real bound entrypoint is skipped without both opt ins`() {
        val gate = gate(
            System.getProperty(RECOVERY_REVIEW_ENABLED_PROPERTY),
            System.getProperty(RECOVERY_REVIEW_CONFIRMATION_PROPERTY),
        )
        assumeTrue(!gate.enabled || gate.confirmation != RECOVERY_REVIEW_CONFIRMATION)
    }

    @Test
    fun `writes current real bound recovery review corpus only with explicit authorization`() {
        requireRecoveryReviewGate()

        val root = projectRoot()
        val causeFile = root.resolve(CAUSE_ANALYSIS_ROOT).resolve("analysis.v1.json")
        require(causeFile.isFile) { "CAUSE_ANALYSIS_INPUT_MISSING" }
        require(HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256(causeFile) == CAUSE_ANALYSIS_SHA256) {
            "CAUSE_ANALYSIS_SHA256_MISMATCH"
        }
        val causeAnalysis = HimZeroCandidateCauseAnalysisPersistenceV1.readReport(causeFile)
        causeAnalysis.validate()
        require(causeAnalysis.logicalDigest == CAUSE_ANALYSIS_LOGICAL_DIGEST) {
            "CAUSE_ANALYSIS_LOGICAL_DIGEST_MISMATCH"
        }
        require(causeAnalysis.counters.records == EXPECTED_CAUSE_ANALYSIS_RECORDS) {
            "CAUSE_ANALYSIS_RECORD_COUNT_MISMATCH"
        }
        require(causeAnalysis.recurringPrimaryValueGroups.size == EXPECTED_RECURRING_GROUPS) {
            "RECURRING_GROUP_COUNT_MISMATCH"
        }

        val paths = HimCanonicalFamilyPaths(root)
        val catalog = HimProductOnlyCanonicalMasterReader().read(paths)
        val persistence = HimCanonicalFamilyPersistence()
        val registry = persistence.readRegistry(paths.entityIdRegistry)
        val masterAuthority = persistence.readAuthority(paths.familyAuthority)
        HimCanonicalFamilyValidator().validate(catalog, registry, masterAuthority)

        val active = HimActiveGroundTruthResolutionV1().resolve(root)
        val authority = persistence.readAuthority(active.authorityFile)
        require(authority.sourceCatalog.path == catalog.path) { "AUTHORITY_SOURCE_CATALOG_MISMATCH" }
        require(authority.sourceCatalog.contentSha256 == catalog.contentSha256) { "AUTHORITY_SOURCE_CATALOG_MISMATCH" }
        require(authority.sourceCatalog.recordCount == catalog.records.size) { "AUTHORITY_SOURCE_CATALOG_MISMATCH" }

        val currentHead = git(root, "rev-parse", "HEAD")
        require(currentHead == EXPECTED_IMPLEMENTATION_HEAD) { "RECOVERY_REVIEW_HEAD_MISMATCH" }
        require(causeAnalysis.inputBinding.analysisImplementationHead != currentHead) {
            "CAUSE_ANALYSIS_AND_RECOVERY_REVIEW_HEADS_MUST_REMAIN_SEPARATE"
        }
        val inputBinding = inputBinding(root, causeFile, causeAnalysis, catalog, authority, active.authorityFile, currentHead)
        inputBinding.validate()

        val outputRoot = root.resolve(HimZeroCandidateRecoveryReviewCorpusContractV1.REVIEW_ROOT)
        val jsonFile = outputRoot.resolve("review-corpus.v1.json")
        val textFile = outputRoot.resolve("review-corpus.v1.txt")
        require(jsonFile.exists() == textFile.exists()) { "RECOVERY_REVIEW_PARTIAL_OUTPUT" }
        val previousJson = if (jsonFile.isFile) jsonFile.readBytes() else null
        val previousText = if (textFile.isFile) textFile.readBytes() else null
        val request = HimZeroCandidateRecoveryReviewCorpusRuntimeRequestV1(
            enabled = true,
            causeAnalysis = causeAnalysis,
            catalog = catalog,
            registry = registry,
            authority = authority,
            inputBinding = inputBinding,
            jsonOutputFile = jsonFile,
            textOutputFile = textFile,
        )

        val first = completed(HimZeroCandidateRecoveryReviewCorpusRuntimeV1.execute(request))
        validateRealReport(first, inputBinding, causeAnalysis)
        require(jsonFile.isFile && textFile.isFile) { "RECOVERY_REVIEW_OUTPUT_MISSING" }
        val firstJson = jsonFile.readBytes()
        val firstText = textFile.readBytes()
        val firstReload = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.readReport(jsonFile)
        assertEquals(first, firstReload)

        val second = completed(HimZeroCandidateRecoveryReviewCorpusRuntimeV1.execute(request))
        validateRealReport(second, inputBinding, causeAnalysis)
        val secondReload = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.readReport(jsonFile)
        assertEquals(first, second)
        assertEquals(first, secondReload)
        assertTrue(firstJson.contentEquals(jsonFile.readBytes()))
        assertTrue(firstText.contentEquals(textFile.readBytes()))
        if (previousJson != null) assertTrue(previousJson.contentEquals(jsonFile.readBytes()))
        if (previousText != null) assertTrue(previousText.contentEquals(textFile.readBytes()))
        assertEquals(firstJson.size.toLong(), jsonFile.length())
        assertEquals(HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256(jsonFile), sha256(firstJson))
        assertEquals(HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256(textFile), sha256(firstText))
    }

    private fun validateRealReport(
        report: HimZeroCandidateRecoveryReviewCorpusReportV1,
        inputBinding: HimZeroCandidateRecoveryReviewCorpusInputBindingV1,
        causeAnalysis: HimZeroCandidateCauseAnalysisReportV1,
    ) {
        report.validate()
        assertEquals(HimZeroCandidateRecoveryReviewCorpusContractV1.VERSION, report.contractId)
        assertEquals(inputBinding, report.inputBinding)
        assertEquals(EXPECTED_CAUSE_ANALYSIS_RECORDS, report.counters.causeAnalysisRecords)
        assertEquals(EXPECTED_RECURRING_GROUPS, report.counters.recurringPrimaryValueGroups)
        assertEquals(EXPECTED_CIQUAL_SUPPLEMENT, report.counters.ciqualLocalizedSupplementRecords)
        assertEquals(listOf(80, 0, 34, 9), report.priorityCounters.map { it.groups })
        assertEquals(0, report.counters.confirmedRecords)
        assertEquals(report.counters.selectedUniqueRecords, report.counters.unreviewedRecords)
        assertTrue(report.entries.all {
            it.associationState == HimZeroCandidateRecoveryReviewAssociationStateV1.UNVERIFIED_AUDIT_ASSOCIATION &&
                it.reviewState == HimZeroCandidateRecoveryReviewStateV1.UNREVIEWED
        })
        assertEquals(causeAnalysis.logicalDigest, report.inputBinding.causeAnalysisLogicalDigest)
    }

    private fun completed(
        result: HimZeroCandidateRecoveryReviewCorpusRuntimeResult<HimZeroCandidateRecoveryReviewCorpusReportV1>,
    ): HimZeroCandidateRecoveryReviewCorpusReportV1 = when (result) {
        is HimZeroCandidateRecoveryReviewCorpusRuntimeResult.Completed -> result.value
        is HimZeroCandidateRecoveryReviewCorpusRuntimeResult.Failed -> fail("${result.reason} ${result.safeContext}")
        is HimZeroCandidateRecoveryReviewCorpusRuntimeResult.Skipped -> fail(result.reason)
    }

    private fun diagnostic(
        result: HimZeroCandidateRecoveryReviewCorpusRuntimeResult<*>,
    ): String = when (result) {
        is HimZeroCandidateRecoveryReviewCorpusRuntimeResult.Completed -> "COMPLETED"
        is HimZeroCandidateRecoveryReviewCorpusRuntimeResult.Failed -> "${result.reason} ${result.safeContext}"
        is HimZeroCandidateRecoveryReviewCorpusRuntimeResult.Skipped -> result.reason
    }

    private fun requireRecoveryReviewGate() {
        assumeTrue(System.getProperty(RECOVERY_REVIEW_ENABLED_PROPERTY) == "true")
        assumeTrue(System.getProperty(RECOVERY_REVIEW_CONFIRMATION_PROPERTY) == RECOVERY_REVIEW_CONFIRMATION)
        HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()
    }

    private fun inputBinding(
        root: File,
        causeFile: File,
        causeAnalysis: HimZeroCandidateCauseAnalysisReportV1,
        catalog: HimProductOnlyCanonicalMaster,
        authority: HimCanonicalFamilyAuthority,
        authorityFile: File,
        currentHead: String,
    ): HimZeroCandidateRecoveryReviewCorpusInputBindingV1 {
        val persistence = HimCanonicalFamilyPersistence()
        val unsigned = HimZeroCandidateRecoveryReviewCorpusInputBindingV1(
            causeAnalysis = HimZeroCandidateCauseAnalysisFileBindingV1(
                relativePath = relativePath(root, causeFile),
                byteSize = causeFile.length(),
                sha256 = HimZeroCandidateCauseAnalysisPersistenceV1.sha256(causeFile),
                logicalDigest = causeAnalysis.logicalDigest,
            ),
            canonicalCatalog = fileBinding(
                root,
                root.resolve(HimCanonicalFamilyPaths.PRODUCT_ONLY_MASTER_PATH),
                logicalDigest(persistence.serialize(catalog.records)),
            ),
            canonicalAuthority = fileBinding(
                root,
                authorityFile,
                logicalDigest(persistence.serialize(authority)),
            ),
            causeAnalysisLogicalDigest = causeAnalysis.logicalDigest,
            recoveryReviewImplementationHead = currentHead,
            bindingDigest = "",
        )
        return unsigned.copy(
            bindingDigest = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.bindingDigest(unsigned),
        )
    }

    private fun fileBinding(root: File, file: File, logicalDigest: String) =
        HimZeroCandidateRecoveryReviewCorpusFileBindingV1(
            relativePath = relativePath(root, file),
            byteSize = file.length(),
            sha256 = HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256(file),
            logicalDigest = logicalDigest,
        )

    private fun logicalDigest(serialized: ByteArray): String {
        require(serialized.isNotEmpty() && serialized.last() == '\n'.code.toByte())
        return HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256(
            serialized.copyOf(serialized.size - 1).toString(Charsets.UTF_8),
        )
    }

    private fun sha256(bytes: ByteArray): String =
        HimZeroCandidateRecoveryReviewCorpusPersistenceV1.sha256(bytes.toString(Charsets.UTF_8))

    private fun projectRoot(): File {
        var current = File(System.getProperty("user.dir") ?: error("USER_DIR_UNAVAILABLE")).canonicalFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile) return current
            current = current.parentFile ?: error("REPOSITORY_ROOT_NOT_FOUND")
        }
    }

    private fun relativePath(root: File, file: File): String = root.canonicalFile.toPath()
        .relativize(file.canonicalFile.toPath())
        .toString()
        .replace(File.separatorChar, '/')

    private fun git(root: File, vararg arguments: String): String {
        val process = ProcessBuilder(listOf("git") + arguments.toList())
            .directory(root)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        require(process.waitFor() == 0 && output.matches(HEAD)) { "GIT_BINDING_FAILED" }
        return output
    }

    private fun gate(enabled: String?, confirmation: String?) = RecoveryReviewGate(
        enabled = enabled == "true",
        confirmation = confirmation,
    )

    private data class RecoveryReviewGate(val enabled: Boolean, val confirmation: String?)

    companion object {
        const val RECOVERY_REVIEW_ENABLED_PROPERTY = "him.zeroCandidateRecoveryReviewCorpus.enabled"
        const val RECOVERY_REVIEW_CONFIRMATION_PROPERTY = "him.zeroCandidateRecoveryReviewCorpus.confirmation"
        const val RECOVERY_REVIEW_CONFIRMATION =
            "AUTHORIZED_BOUNDED_ZERO_CANDIDATE_RECOVERY_REVIEW_CORPUS_OFFLINE"
        const val CAUSE_ANALYSIS_ROOT =
            "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-cause-analysis/v1"
        const val CAUSE_ANALYSIS_SHA256 =
            "bd725f67889eb39cfbc8016ec176834bc90774020a9eff4cc5498141df171314"
        const val CAUSE_ANALYSIS_LOGICAL_DIGEST =
            "692c1e6f09e05598066cd1cf841dfdbed59c3fbd1250476bea2922eb17e0aedc"
        const val EXPECTED_IMPLEMENTATION_HEAD = "8ccb4fb3ac2fd612bbbdb0d39ee36848b0e95dd4"
        const val EXPECTED_CAUSE_ANALYSIS_RECORDS = 2810
        const val EXPECTED_RECURRING_GROUPS = 123
        const val EXPECTED_CIQUAL_SUPPLEMENT = 104
        private val HEAD = Regex("[0-9a-f]{40}")
    }
}
