package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.testing.system.tools.knowledge.him.support.HimTestExecutionBoundaryV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticAggregateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticCountersV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticMissionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticSourceCounterV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticStatusV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisRecurringPrimaryValueGroupV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisReportV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisRuntimeRequestV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisRuntimeResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateCauseAnalysisRuntimeV1
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class RunHimZeroCandidateCauseAnalysisRealV1Test {
    @Test
    fun `cause analysis gate is disabled without explicit opt in`() {
        val gate = causeAnalysisGate(null, null)
        assertFalse(gate.enabled)
        assertFalse(gate.confirmation == CAUSE_ANALYSIS_CONFIRMATION)
    }

    @Test
    fun `wrong cause analysis confirmation is rejected before input access`() {
        val gate = causeAnalysisGate("true", "WRONG_CONFIRMATION")
        assertTrue(gate.enabled)
        assertFalse(gate.confirmation == CAUSE_ANALYSIS_CONFIRMATION)
    }

    @Test
    fun `real-bound cause analysis path has no store search fetch or scan dependency`() {
        val forbidden = listOf("Store", "SQLite", "Fetch", "Search", "Scan", "Retrieval", "Catalog", "Authority")
        val classes = listOf(
            HimZeroCandidateCauseAnalysisRuntimeRequestV1::class.java,
            HimZeroCandidateCauseAnalysisRuntimeV1::class.java,
        )
        val typeNames = classes.flatMap { type ->
            type.declaredFields.map { it.type.simpleName } +
                    type.declaredMethods.flatMap { method ->
                        listOf(method.returnType.simpleName) +
                                method.parameterTypes.map { it.simpleName }
                    } +
                    type.declaredConstructors.flatMap { constructor ->
                        constructor.parameterTypes.map { it.simpleName }
                    }
        }
        assertTrue(typeNames.none { typeName -> forbidden.any { token -> typeName.contains(token, ignoreCase = true) } })
    }

    @Test
    fun `historical diagnostic and current cause analysis heads remain separate bindings`() {
        requireSourceIntegration()
        val root = projectRoot()
        val inputs = loadInputs(root)
        val currentHead = git(root, "rev-parse", "HEAD")
        require(currentHead != inputs.mission.diagnosticImplementationHead) {
            "DIAGNOSTIC_AND_ANALYSIS_HEAD_MUST_REMAIN_SEPARATE"
        }
        val binding = inputBinding(root, inputs, currentHead)
        assertEquals(inputs.mission.diagnosticImplementationHead, HISTORICAL_DIAGNOSTIC_HEAD)
        assertEquals(currentHead, binding.analysisImplementationHead)
        assertEquals(inputs.mission.logicalDigest, binding.diagnosticMissionLogicalDigest)
        assertEquals(inputs.aggregate.logicalDigest, binding.diagnosticAggregateLogicalDigest)
        assertTrue(currentHead.matches(HEAD))
    }

    @Test
    fun `typed cause analysis diagnostics do not expose paths or throwable text`() {
        val result = HimZeroCandidateCauseAnalysisRuntimeResult.Failed(
            HimZeroCandidateCauseAnalysisFailureReasonV1.AGGREGATE_BINDING_MISMATCH,
            "aggregateDigest",
        )
        val message = diagnostic(result)
        assertEquals("AGGREGATE_BINDING_MISMATCH aggregateDigest", message)
        assertFalse(message.contains("/Users/"))
        assertFalse(message.contains("Exception"))
        assertFalse(message.contains("Throwable"))
        assertFalse(message.contains("\n"))
    }

    @Test
    fun `analyzes current complete real-bound zero candidate cause analysis without stores`() {
        requireSourceIntegration()
        requireCauseAnalysisGate()

        val root = projectRoot()
        val inputs = loadInputs(root)
        val currentHead = git(root, "rev-parse", "HEAD")
        val binding = inputBinding(root, inputs, currentHead)
        val jsonFile = analysisJson(root)
        val textFile = analysisText(root)
        require(jsonFile.exists() == textFile.exists()) { "PARTIAL_CAUSE_ANALYSIS_ARTIFACT" }
        val previousJson = if (jsonFile.isFile) jsonFile.readBytes() else null
        val previousText = if (textFile.isFile) textFile.readBytes() else null

        val result = HimZeroCandidateCauseAnalysisRuntimeV1.analyze(
            HimZeroCandidateCauseAnalysisRuntimeRequestV1(
                enabled = true,
                mission = inputs.mission,
                aggregate = inputs.aggregate,
                inputBinding = binding,
                jsonOutputFile = jsonFile,
                textOutputFile = textFile,
            ),
        )
        val report = when (result) {
            is HimZeroCandidateCauseAnalysisRuntimeResult.Completed -> result.value
            is HimZeroCandidateCauseAnalysisRuntimeResult.Failed -> fail("${result.reason} ${result.safeContext}")
            is HimZeroCandidateCauseAnalysisRuntimeResult.Skipped -> fail(result.reason)
        }
        validateReport(report, binding)
        require(jsonFile.isFile && textFile.isFile) { "CAUSE_ANALYSIS_ARTIFACT_MISSING" }
        val reloaded = HimZeroCandidateCauseAnalysisPersistenceV1.readReport(jsonFile)
        assertEquals(report, reloaded)
        if (previousJson != null) assertTrue(previousJson.contentEquals(jsonFile.readBytes()))
        if (previousText != null) assertTrue(previousText.contentEquals(textFile.readBytes()))
    }

    private fun requireCauseAnalysisGate() {
        val gate = causeAnalysisGate(
            System.getProperty(CAUSE_ANALYSIS_ENABLED_PROPERTY),
            System.getProperty(CAUSE_ANALYSIS_CONFIRMATION_PROPERTY),
        )
        assumeTrue(gate.enabled)
        assumeTrue(gate.confirmation == CAUSE_ANALYSIS_CONFIRMATION)
    }

    private fun requireSourceIntegration() = HimTestExecutionBoundaryV1.requireSourceIntegrationEnabled()

    private fun validateReport(
        report: HimZeroCandidateCauseAnalysisReportV1,
        binding: HimZeroCandidateCauseAnalysisInputBindingV1,
    ) {
        report.validate()
        assertEquals(HimZeroCandidateCauseAnalysisContractV1.VERSION, report.contractId)
        assertEquals(binding, report.inputBinding)
        assertEquals(2_810, report.records.size)
        assertTrue(report.records.all {
            it.originalStatus == HimUnresolvedPrimaryIdentityDiagnosticStatusV1.IDENTITY_FIELDS_PRESENT_NO_CANDIDATE
        })
        assertTrue(report.records.all { it.candidateIdentities.isEmpty() })
        assertEquals(2_810, report.bucketCounters.sumOf { it.count })
        assertEquals(2_810, report.sourceBucketBreakdown.sumOf { it.count })
        assertEquals(2_810, report.recordKindBucketBreakdown.sumOf { it.count })
        assertEquals(HimZeroCandidateCauseAnalysisContractV1.BUCKET_ORDER, report.bucketCounters.map { it.bucket })
        assertEquals(HimZeroCandidateCauseAnalysisContractV1.FLAG_ORDER, report.flagCounters.map { it.flag })
        assertTrue(
            report.recurringPrimaryValueGroups ==
                    report.recurringPrimaryValueGroups.sortedWith(
                        compareByDescending<HimZeroCandidateCauseAnalysisRecurringPrimaryValueGroupV1> {
                            it.references.size
                        }.thenBy {
                            it.key
                        },
                    ),
        )
        assertTrue(report.recurringTokenGroups.isEmpty())
        assertTrue(report.logicalDigest.matches(SHA256))
    }

    private fun diagnostic(
        result: HimZeroCandidateCauseAnalysisRuntimeResult<*>,
    ): String = when (result) {
        is HimZeroCandidateCauseAnalysisRuntimeResult.Completed -> "COMPLETED"
        is HimZeroCandidateCauseAnalysisRuntimeResult.Failed -> "${result.reason} ${result.safeContext}"
        is HimZeroCandidateCauseAnalysisRuntimeResult.Skipped -> result.reason
    }

    private fun inputBinding(
        root: File,
        inputs: RealInputs,
        currentHead: String,
    ): HimZeroCandidateCauseAnalysisInputBindingV1 {
        val unsigned = HimZeroCandidateCauseAnalysisInputBindingV1(
            diagnosticMission = fileBinding(root, inputs.missionFile, inputs.mission.logicalDigest),
            diagnosticAggregate = fileBinding(root, inputs.aggregateFile, inputs.aggregate.logicalDigest),
            diagnosticMissionLogicalDigest = inputs.mission.logicalDigest,
            diagnosticAggregateLogicalDigest = inputs.aggregate.logicalDigest,
            analysisImplementationHead = currentHead,
            bindingDigest = "",
        )
        return unsigned.copy(
            bindingDigest = HimZeroCandidateCauseAnalysisPersistenceV1.bindingDigest(unsigned),
        )
    }

    private fun loadInputs(root: File): RealInputs {
        val missionFile = root.resolve(DIAGNOSTIC_REPORT_ROOT).resolve("mission.v1.json")
        val aggregateFile = root.resolve(DIAGNOSTIC_REPORT_ROOT).resolve("aggregate.v1.json")
        require(missionFile.isFile) { "DIAGNOSTIC_MISSION_INPUT_MISSING" }
        require(aggregateFile.isFile) { "DIAGNOSTIC_AGGREGATE_INPUT_MISSING" }
        require(HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.sha256(missionFile) == DIAGNOSTIC_MISSION_SHA256) {
            "DIAGNOSTIC_MISSION_SHA256_MISMATCH"
        }
        require(HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.sha256(aggregateFile) == DIAGNOSTIC_AGGREGATE_SHA256) {
            "DIAGNOSTIC_AGGREGATE_SHA256_MISMATCH"
        }
        val mission = HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.readMission(missionFile)
        require(mission.logicalDigest == DIAGNOSTIC_MISSION_LOGICAL_DIGEST) { "DIAGNOSTIC_MISSION_LOGICAL_DIGEST_MISMATCH" }
        require(mission.diagnosticImplementationHead == HISTORICAL_DIAGNOSTIC_HEAD) { "DIAGNOSTIC_HEAD_MISMATCH" }
        val aggregate = HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.readAggregate(aggregateFile, mission)
        require(aggregate.logicalDigest == DIAGNOSTIC_AGGREGATE_LOGICAL_DIGEST) { "DIAGNOSTIC_AGGREGATE_LOGICAL_DIGEST_MISMATCH" }
        require(aggregate.counters == expectedCounters()) { "DIAGNOSTIC_COUNTER_MISMATCH" }
        require(aggregate.shardResults.size == 11) { "DIAGNOSTIC_SHARD_COUNT_MISMATCH" }
        return RealInputs(missionFile, aggregateFile, mission, aggregate)
    }

    private fun expectedCounters() = HimUnresolvedPrimaryIdentityDiagnosticCountersV1(
        unresolvedOccurrences = 3_036,
        canonicalTargets = 924,
        uniqueEvidenceReferences = 2_983,
        exactFetches = 2_983,
        recordsLoaded = 2_983,
        identityFieldsPresent = 2_983,
        recordsWithNoIdentityFields = 0,
        recordsWithOnlyEmptyIdentityFields = 0,
        zeroCandidateRecords = 2_810,
        singleCandidateRecords = 55,
        multipleCandidateRecords = 118,
        perSourceReferences = listOf(
            HimUnresolvedPrimaryIdentityDiagnosticSourceCounterV1(HimGroundTruthSource.OPEN_FOOD_FACTS, 2_743),
            HimUnresolvedPrimaryIdentityDiagnosticSourceCounterV1(HimGroundTruthSource.AGRIBALYSE, 60),
            HimUnresolvedPrimaryIdentityDiagnosticSourceCounterV1(HimGroundTruthSource.CIQUAL, 105),
            HimUnresolvedPrimaryIdentityDiagnosticSourceCounterV1(HimGroundTruthSource.GLYCEMIC_INDEX, 75),
        ),
        technicalErrors = 0,
    )

    private fun fileBinding(
        root: File,
        file: File,
        logicalDigest: String,
    ) = HimZeroCandidateCauseAnalysisFileBindingV1(
        relativePath = relativePath(root, file),
        byteSize = file.length(),
        sha256 = HimZeroCandidateCauseAnalysisPersistenceV1.sha256(file),
        logicalDigest = logicalDigest,
    )

    private fun projectRoot(): File {
        var current = File(System.getProperty("user.dir") ?: error("USER_DIR_UNAVAILABLE")).canonicalFile
        while (true) {
            if (current.resolve("settings.gradle.kts").isFile) return current
            current = current.parentFile ?: error("REPOSITORY_ROOT_NOT_FOUND")
        }
    }

    private fun git(root: File, vararg arguments: String): String {
        val process = ProcessBuilder(listOf("git") + arguments.toList())
            .directory(root)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        require(process.waitFor() == 0 && output.matches(HEAD)) { "GIT_BINDING_FAILED" }
        return output
    }

    private fun relativePath(root: File, file: File): String = root.canonicalFile.toPath()
        .relativize(file.canonicalFile.toPath())
        .toString()
        .replace(File.separatorChar, '/')

    private fun analysisJson(root: File) = root.resolve(ANALYSIS_REPORT_ROOT).resolve("analysis.v1.json")

    private fun analysisText(root: File) = root.resolve(ANALYSIS_REPORT_ROOT).resolve("analysis.v1.txt")

    private fun causeAnalysisGate(enabled: String?, confirmation: String?) = CauseAnalysisGate(
        enabled = enabled == "true",
        confirmation = confirmation,
    )

    private data class CauseAnalysisGate(val enabled: Boolean, val confirmation: String?)

    private data class RealInputs(
        val missionFile: File,
        val aggregateFile: File,
        val mission: HimUnresolvedPrimaryIdentityDiagnosticMissionV1,
        val aggregate: HimUnresolvedPrimaryIdentityDiagnosticAggregateV1,
    )

    companion object {
        const val CAUSE_ANALYSIS_ENABLED_PROPERTY = "him.zeroCandidateCauseAnalysis.enabled"
        const val CAUSE_ANALYSIS_CONFIRMATION_PROPERTY = "him.zeroCandidateCauseAnalysis.confirmation"
        const val CAUSE_ANALYSIS_CONFIRMATION = "AUTHORIZED_DETERMINISTIC_ZERO_CANDIDATE_CAUSE_ANALYSIS_OFFLINE"
        const val DIAGNOSTIC_REPORT_ROOT = "build/knowledge/reports/him/evidence-alignment/catalog-audit/unresolved-primary-identity-diagnostic/v1"
        const val ANALYSIS_REPORT_ROOT = "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-cause-analysis/v1"
        const val DIAGNOSTIC_MISSION_SHA256 = "bc0be736a1d3be737e28e4616b54b6cacacecdd3003ee01b40f9ad5c94479ea2"
        const val DIAGNOSTIC_MISSION_LOGICAL_DIGEST = "b4d09ae748f79e5930210a38c1034135fa705eb34e5aeefe9913ff7681f24af6"
        const val DIAGNOSTIC_AGGREGATE_SHA256 = "cebed26fb6ffe1be9ff1aa3ee0bcbbb5df814789e8c36beb1625b76dbf33a57b"
        const val DIAGNOSTIC_AGGREGATE_LOGICAL_DIGEST = "9490b5087392b9c3d18e4c1344c7c61b654366570b5e5e82b32adbcb15c20e91"
        const val HISTORICAL_DIAGNOSTIC_HEAD = "ec295c83f1481185c98f4d32b529a64918790b7c"
        private const val SHA256_PATTERN = "[0-9a-f]{64}"
        private val SHA256 = Regex(SHA256_PATTERN)
        private val HEAD = Regex("[0-9a-f]{40}")
    }
}
