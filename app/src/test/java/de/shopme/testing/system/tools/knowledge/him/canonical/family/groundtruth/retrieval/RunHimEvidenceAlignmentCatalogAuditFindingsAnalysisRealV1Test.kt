package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisReportV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeRequestV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentCountersV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail
import org.junit.Assume.assumeTrue
import java.io.File

class RunHimEvidenceAlignmentCatalogAuditFindingsAnalysisRealV1Test {
    @Test
    fun `analyzes current complete real-bound enriched findings aggregate without stores`() {
        requireAnalysisGate()

        val root = projectRoot()
        val missionFile = enrichmentMission(root)
        val aggregateFile = enrichmentAggregate(root)
        require(missionFile.isFile) { "ENRICHMENT_MISSION_MISSING" }
        require(aggregateFile.isFile) { "ENRICHMENT_AGGREGATE_MISSING" }
        require(HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.sha256(missionFile) == FROZEN_MISSION_SHA256) {
            "MISSION_SHA256_MISMATCH"
        }
        require(HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.sha256(aggregateFile) == FROZEN_AGGREGATE_SHA256) {
            "AGGREGATE_SHA256_MISMATCH"
        }

        val mission = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.readMission(missionFile)
        require(mission.logicalDigest == FROZEN_MISSION_LOGICAL_DIGEST) { "MISSION_LOGICAL_DIGEST_MISMATCH" }
        require(mission.shards.size == EXPECTED_SHARD_COUNT) { "MISSION_SHARD_COUNT_MISMATCH" }
        require(mission.expectedCanonicalCount == EXPECTED_CANONICAL_COUNT) { "MISSION_CANONICAL_COUNT_MISMATCH" }

        val aggregate = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.readAggregate(aggregateFile, mission)
        require(aggregate.logicalDigest == FROZEN_AGGREGATE_LOGICAL_DIGEST) { "AGGREGATE_LOGICAL_DIGEST_MISMATCH" }
        require(aggregate.state == HimEvidenceAlignmentCatalogAuditState.COMPLETE) { "AGGREGATE_INCOMPLETE" }
        require(aggregate.shardResults.size == EXPECTED_SHARD_COUNT) { "AGGREGATE_SHARD_COUNT_MISMATCH" }
        require(aggregate.missionDigest == mission.logicalDigest) { "AGGREGATE_MISSION_BINDING_MISMATCH" }
        require(aggregate.shardResults.map { it.shardId } == mission.shards.map { it.shardId }) {
            "AGGREGATE_SHARD_SET_MISMATCH"
        }
        require(aggregate.counters == EXPECTED_COUNTERS) { "AGGREGATE_COUNTER_MISMATCH" }
        require(mission.provenance.auditMissionLogicalDigest == mission.auditMissionDigest) {
            "MISSION_AUDIT_BINDING_MISMATCH"
        }

        val analysisHead = git(root, "rev-parse", "HEAD")
        require(analysisHead.matches(HEAD)) { "ANALYSIS_HEAD_UNAVAILABLE" }
        val inputBinding = inputBinding(root, missionFile, aggregateFile, mission, aggregate, analysisHead)
        val request = HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeRequestV1(
            mission = mission,
            aggregate = aggregate,
            inputBinding = inputBinding,
        )
        val report = analysisValue(HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeV1.analyze(request))
        require(report.inputBinding.analysisImplementationHead == analysisHead) {
            "ANALYSIS_HEAD_MISMATCH"
        }

        val jsonFile = analysisJson(root)
        val textFile = analysisText(root)
        require(jsonFile.exists() == textFile.exists()) { "PARTIAL_ANALYSIS_ARTIFACT" }
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1.writeReport(jsonFile, textFile, report)
        val reloaded = HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1.readReport(jsonFile)
        require(reloaded == report) { "ANALYSIS_RELOAD_VALIDATION_FAILED" }
        val firstJson = jsonFile.readBytes()
        val firstText = textFile.readBytes()

        HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1.writeReport(jsonFile, textFile, report)
        assertTrue(firstJson.contentEquals(jsonFile.readBytes()))
        assertTrue(firstText.contentEquals(textFile.readBytes()))
    }

    @Test
    fun `analysis gate is disabled without explicit opt in`() {
        val gate = gate(null, null)
        assertFalse(gate.enabled)
        assertFalse(gate.confirmation == ANALYSIS_CONFIRMATION)
    }

    @Test
    fun `wrong analysis confirmation is rejected before input access`() {
        val gate = gate("true", "WRONG_CONFIRMATION")
        assertTrue(gate.enabled)
        assertFalse(gate.confirmation == ANALYSIS_CONFIRMATION)
    }

    @Test
    fun `real bound analysis path has no store search or fetch dependency`() {
        assertTrue(HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeRequestV1::class.java.declaredFields.none {
            it.type.name.contains("Store") || it.type.name.contains("Search") || it.type.name.contains("Fetch")
        })
        assertTrue(HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeV1::class.java.declaredMethods.none {
            it.name == "search" || it.name == "fetch"
        })
    }

    @Test
    fun `typed analysis diagnostics do not expose paths or throwable text`() {
        val result = HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult.Failed(
            HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.INPUT_BINDING_MISMATCH,
            "input",
        )
        val message = diagnostic(result)
        assertEquals("INPUT_BINDING_MISMATCH input", message)
        assertFalse(message.contains("/"))
        assertFalse(message.contains("Exception"))
        assertFalse(message.contains("Throwable"))
    }

    @Test
    fun `historical enrichment and current analysis heads remain separate bindings`() {
        val unsigned = HimEvidenceAlignmentCatalogAuditFindingsAnalysisInputBindingV1(
            contractId = HimEvidenceAlignmentCatalogAuditFindingsAnalysisContractV1.VERSION,
            auditHead = "a".repeat(40),
            enrichmentImplementationHead = "b".repeat(40),
            analysisImplementationHead = "c".repeat(40),
            enrichmentMission = fileBinding("build/enrichment/mission.v1.json", "1".repeat(64)),
            enrichmentAggregate = fileBinding("build/enrichment/aggregate.v1.json", "2".repeat(64)),
            enrichmentMissionLogicalDigest = "1".repeat(64),
            enrichmentAggregateLogicalDigest = "2".repeat(64),
            canonicalOrderDigest = "3".repeat(64),
            expectedCanonicalCount = 1,
            expectedShardIds = listOf("shard-000001"),
            bindingDigest = "",
        )
        val binding = unsigned.copy(
            bindingDigest = HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1.bindingDigest(unsigned),
        )
        binding.validate()
        assertTrue(binding.auditHead != binding.analysisImplementationHead)
        assertTrue(binding.enrichmentImplementationHead != binding.analysisImplementationHead)
    }

    private fun requireAnalysisGate() {
        assumeTrue(System.getProperty(ANALYSIS_ENABLED_PROPERTY) == "true")
        assumeTrue(System.getProperty(ANALYSIS_CONFIRMATION_PROPERTY) == ANALYSIS_CONFIRMATION)
    }

    private fun analysisValue(
        result: HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult<HimEvidenceAlignmentCatalogAuditFindingsAnalysisReportV1>,
    ): HimEvidenceAlignmentCatalogAuditFindingsAnalysisReportV1 = when (result) {
        is HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult.Completed -> result.value
        is HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult.Failed ->
            fail("${result.reason.name} ${result.safeContext}")
    }

    private fun diagnostic(
        result: HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult<*>,
    ): String = when (result) {
        is HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult.Completed -> "COMPLETED"
        is HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult.Failed ->
            "${result.reason.name} ${result.safeContext}"
    }

    private fun inputBinding(
        root: File,
        missionFile: File,
        aggregateFile: File,
        mission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1,
        aggregate: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1,
        analysisHead: String,
    ): HimEvidenceAlignmentCatalogAuditFindingsAnalysisInputBindingV1 {
        val unsigned = HimEvidenceAlignmentCatalogAuditFindingsAnalysisInputBindingV1(
            contractId = HimEvidenceAlignmentCatalogAuditFindingsAnalysisContractV1.VERSION,
            auditHead = mission.provenance.auditHead,
            enrichmentImplementationHead = mission.provenance.enrichmentImplementationHead,
            analysisImplementationHead = analysisHead,
            enrichmentMission = fileBinding(root, missionFile, mission.logicalDigest),
            enrichmentAggregate = fileBinding(root, aggregateFile, aggregate.logicalDigest),
            enrichmentMissionLogicalDigest = mission.logicalDigest,
            enrichmentAggregateLogicalDigest = aggregate.logicalDigest,
            canonicalOrderDigest = mission.auditCanonicalOrderDigest,
            expectedCanonicalCount = mission.expectedCanonicalCount,
            expectedShardIds = mission.shards.map { it.shardId },
            bindingDigest = "",
        )
        return unsigned.copy(
            bindingDigest = HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1.bindingDigest(unsigned),
        )
    }

    private fun fileBinding(root: File, file: File, logicalDigest: String) =
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisFileBindingV1(
            relativePath = relativePath(root, file),
            byteSize = file.length(),
            sha256 = HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1.sha256(file),
            logicalDigest = logicalDigest,
        )

    private fun fileBinding(path: String, logicalDigest: String) =
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisFileBindingV1(
            relativePath = path,
            byteSize = 1,
            sha256 = "1".repeat(64),
            logicalDigest = logicalDigest,
        )

    private fun gate(enabled: String?, confirmation: String?) = AnalysisGate(
        enabled = enabled == "true",
        confirmation = confirmation,
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

    private fun enrichmentMission(root: File) = root.resolve(ENRICHMENT_REPORT_ROOT).resolve("mission.v1.json")

    private fun enrichmentAggregate(root: File) = root.resolve(ENRICHMENT_REPORT_ROOT).resolve("aggregate.v1.json")

    private fun analysisJson(root: File) = root.resolve(ANALYSIS_REPORT_ROOT).resolve("analysis.v1.json")

    private fun analysisText(root: File) = root.resolve(ANALYSIS_REPORT_ROOT).resolve("analysis.v1.txt")

    private data class AnalysisGate(val enabled: Boolean, val confirmation: String?)

    private companion object {
        const val ANALYSIS_ENABLED_PROPERTY = "him.evidenceAlignmentCatalogAuditFindingsAnalysis.enabled"
        const val ANALYSIS_CONFIRMATION_PROPERTY = "him.evidenceAlignmentCatalogAuditFindingsAnalysis.confirmation"
        const val ANALYSIS_CONFIRMATION = "AUTHORIZED_FULL_CATALOG_ENRICHED_FINDINGS_ANALYSIS_OFFLINE"
        const val ENRICHMENT_REPORT_ROOT = "build/knowledge/reports/him/evidence-alignment/catalog-audit/findings-enrichment/v1"
        const val ANALYSIS_REPORT_ROOT = "build/knowledge/reports/him/evidence-alignment/catalog-audit/findings-analysis/v1"
        const val FROZEN_MISSION_SHA256 = "6272d445502ff870629f360a589a83a02ecbf91361dc60ae05444873282cb614"
        const val FROZEN_MISSION_LOGICAL_DIGEST = "3bbff3db4140c8724079bafcf94ea1269552c74a076f2d17f52b628f1f0ec69f"
        const val FROZEN_AGGREGATE_SHA256 = "d8982891ad4b6775c6a9f76d6b5ea6dbca447966451236b1da67bd2d6ac40091"
        const val FROZEN_AGGREGATE_LOGICAL_DIGEST = "1bd475310058775d36ccbdbe976ba9f053028255ca1cb20c3004c46840cd2398"
        const val EXPECTED_CANONICAL_COUNT = 1384
        const val EXPECTED_SHARD_COUNT = 11
        val HEAD = Regex("[0-9a-f]{40}")
        val EXPECTED_COUNTERS = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentCountersV1(
            canonicalsProcessed = 1384,
            sourcesProcessed = 44,
            findingOccurrences = 16969,
            uniqueEvidenceReferences = 13022,
            exactFetches = 13022,
            noRetrievalHits = 3935,
            unsupportedRecordKinds = 37,
            reconstructedFindings = 13034,
            classificationMismatches = 0,
        )
    }
}
