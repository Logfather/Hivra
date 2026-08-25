package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentAuditClassification
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditBindingsV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditSourceBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeRequestV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentCountersV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentProvenanceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardPlanV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentClassificationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditQueryTermKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditQueryTermV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexBuildState
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail
import java.util.Locale

class RunHimEvidenceAlignmentCatalogAuditFindingsAnalysisV1Test {
    @Test
    fun analysisBuildsCompleteDeterministicIndexAndTypedBuckets() {
        val report = completed(request())
        val repeated = completed(request())

        assertEquals(9, report.totalFindingOccurrences)
        assertEquals(report.logicalDigest, repeated.logicalDigest)
        assertEquals(report.findingIndex, repeated.findingIndex)
        assertEquals(9, report.findingIndex.size)
        assertEquals(8, report.primaryOutcomeBuckets.size)
        assertEquals(
            setOf(
                HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeV1.DIRECT_SUPPORTED,
                HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeV1.DIRECT_REJECTED,
                HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeV1.MODIFIER_ONLY,
                HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeV1.UNRESOLVED_PRIMARY_IDENTITY,
                HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeV1.NO_RETRIEVAL_HIT,
                HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeV1.UNSUPPORTED_RECORD_KIND,
                HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeV1.INVALID_PROJECTION,
                HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeV1.SOURCE_OR_BINDING_FAILURE,
            ),
            report.primaryOutcomeBuckets.map { it.outcome }.toSet(),
        )
        assertEquals(8, report.counters.uniqueEvidenceReferences)
        assertEquals(
            7,
            report.findingIndex.mapNotNull { it.originalEvidenceReference }.distinct().size,
        )
        assertEquals(
            4,
            report.findingIndex.filter { it.auditShardId == "shard-000001" }
                .mapNotNull { it.originalEvidenceReference }
                .distinct()
                .size,
        )
        assertEquals(
            4,
            report.findingIndex.filter { it.auditShardId == "shard-000002" }
                .mapNotNull { it.originalEvidenceReference }
                .distinct()
                .size,
        )
        assertEquals(1, report.counters.noRetrievalHits)
        assertEquals(1, report.counters.unsupportedRecordKinds)
        assertEquals(0, report.counters.classificationMismatches)
        assertEquals(2, report.findingIndex.count { it.originalEvidenceReference == OFF_REFERENCE })
        assertEquals(1, report.findingIndex.count { it.source == HimGroundTruthSource.OPEN_FOOD_FACTS && it.originalAuditClassification == HimEvidenceAlignmentAuditClassification.DIRECT_SUPPORTED })
        assertTrue(report.findingIndex.zipWithNext().all { (left, right) ->
            left.auditShardId <= right.auditShardId || left.entityId <= right.entityId || left.source.ordinal <= right.source.ordinal
        })
        assertTrue(report.diagnosticFlags.all { it.count >= 0 })
        assertTrue(report.breakdowns.any { it.key == "NOT_AVAILABLE" })
    }

    @Test
    fun reorderedShardInputProducesSameCanonicalFindingAnalysis() {
        val first = completed(request())
        val original = request()
        val reorderedAggregate = original.aggregate.copy(
            shardResults = original.aggregate.shardResults.asReversed(),
            logicalDigest = "",
        ).let { it.copy(logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(it)) }
        val second = completed(request(reorderedAggregate))

        assertEquals(first.findingIndex, second.findingIndex)
        assertEquals(first.primaryOutcomeBuckets, second.primaryOutcomeBuckets)
        assertEquals(first.breakdowns, second.breakdowns)
        assertEquals(first.diagnosticFlags, second.diagnosticFlags)
        assertFalse(first.inputBinding.enrichmentAggregateLogicalDigest == second.inputBinding.enrichmentAggregateLogicalDigest)
    }

    @Test
    fun persistenceReloadAndSecondWriteAreByteIdentical() {
        val report = completed(request())
        val directory = createTempDirectory("him-findings-analysis").toFile()
        val json = directory.resolve("analysis.v1.json")
        val text = directory.resolve("analysis.v1.txt")

        HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1.writeReport(json, text, report)
        val jsonBytes = json.readBytes()
        val textBytes = text.readBytes()
        assertEquals(report, HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1.readReport(json))
        HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1.writeReport(json, text, report)
        assertContentEquals(jsonBytes, json.readBytes())
        assertContentEquals(textBytes, text.readBytes())

        val differentBinding = report.inputBinding.copy(analysisImplementationHead = "0".repeat(40), bindingDigest = "")
            .let { it.copy(bindingDigest = HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1.bindingDigest(it)) }
        val different = report.copy(inputBinding = differentBinding, logicalDigest = "")
            .let { it.copy(logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1.logicalDigest(it)) }
        assertFailsWith<IllegalArgumentException> {
            HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1.writeReport(json, text, different)
        }
    }

    @Test
    fun manipulatedCountersFailClosedWithTypedReason() {
        val original = request()
        val manipulated = original.aggregate.copy(
            counters = original.aggregate.counters.copy(canonicalsProcessed = 0),
            logicalDigest = "",
        ).let { it.copy(logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(it)) }
        val result = HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeV1.analyze(request(manipulated))

        val failure = when (result) {
            is HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult.Failed -> result
            is HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult.Completed -> fail("EXPECTED_FAILURE")
        }
        assertEquals(HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.COUNTER_INVARIANT_FAILED, failure.reason)
        assertEquals("aggregateCounters", failure.safeContext)
    }

    @Test
    fun missingShardAndDuplicateOccurrenceFailClosed() {
        val original = request()
        val partial = original.aggregate.copy(
            state = HimEvidenceAlignmentCatalogAuditState.PARTIAL,
            shardResults = original.aggregate.shardResults.dropLast(1),
            logicalDigest = "",
        ).let { it.copy(logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(it)) }
        val missing = failed(request(partial))
        assertEquals(HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.SHARD_SET_INCOMPLETE, missing.reason)

        val firstFinding = original.aggregate.shardResults.first().findings.first()
        val duplicateShard = original.aggregate.shardResults[1].copy(
            findings = original.aggregate.shardResults[1].findings.drop(1) + firstFinding.copy(
                auditShardId = "shard-000002",
                entityId = "c00003",
            ),
            logicalDigest = "",
        ).let { it.copy(logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(it)) }
        val duplicateAggregate = original.aggregate.copy(
            shardResults = listOf(original.aggregate.shardResults.first(), duplicateShard) + original.aggregate.shardResults.drop(2),
            logicalDigest = "",
        ).let { it.copy(logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(it)) }
        val duplicate = failed(request(duplicateAggregate))
        assertEquals(HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.DUPLICATE_FINDING_OCCURRENCE, duplicate.reason)
    }

    @Test
    fun classificationMismatchIsNotConvertedToAReviewBucket() {
        val original = request()
        val changedFirst = original.aggregate.shardResults.first().copy(
            findings = original.aggregate.shardResults.first().findings.mapIndexed { index, finding ->
                if (index == 0) finding.copy(
                    classificationComparison = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.MISMATCH,
                ) else finding
            },
            logicalDigest = "",
        ).let { it.copy(logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(it)) }
        val mismatchAggregate = original.aggregate.copy(
            shardResults = listOf(changedFirst) + original.aggregate.shardResults.drop(1),
            logicalDigest = "",
        ).let { it.copy(logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(it)) }

        assertEquals(
            HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.CLASSIFICATION_MISMATCH_PRESENT,
            failed(request(mismatchAggregate)).reason,
        )
    }

    @Test
    fun foreignBindingAndUnknownResolutionFailClosed() {
        val original = request()
        val foreign = original.aggregate.copy(
            missionDigest = "0".repeat(64),
            logicalDigest = "",
        ).let { it.copy(logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(it)) }
        assertEquals(
            HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.AGGREGATE_VALIDATION_FAILED,
            failed(request(foreign)).reason,
        )

        val changedFirst = original.aggregate.shardResults.first().copy(
            findings = original.aggregate.shardResults.first().findings.mapIndexed { index, finding ->
                if (index == 0) finding.copy(extractorResolution = "UNKNOWN") else finding
            },
            logicalDigest = "",
        ).let { it.copy(logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(it)) }
        val unknownAggregate = original.aggregate.copy(
            shardResults = listOf(changedFirst) + original.aggregate.shardResults.drop(1),
            logicalDigest = "",
        ).let { it.copy(logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(it)) }
        assertEquals(
            HimEvidenceAlignmentCatalogAuditFindingsAnalysisFailureReasonV1.UNSUPPORTED_ANALYSIS_VALUE,
            failed(request(unknownAggregate)).reason,
        )
    }

    private fun completed(
        value: HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeRequestV1,
    ) = when (val result = HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeV1.analyze(value)) {
        is HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult.Completed -> result.value
        is HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult.Failed -> fail("${result.reason} ${result.safeContext}")
    }

    private fun failed(
        value: HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeRequestV1,
    ): HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult.Failed = when (val result = HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeV1.analyze(value)) {
        is HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult.Failed -> result
        is HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeResult.Completed -> fail("EXPECTED_FAILURE")
    }

    private fun request(
        aggregateOverride: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1? = null,
    ): HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeRequestV1 {
        val mission = mission()
        val aggregate = aggregateOverride ?: aggregate(mission)
        return HimEvidenceAlignmentCatalogAuditFindingsAnalysisRuntimeRequestV1(
            mission = mission,
            aggregate = aggregate,
            inputBinding = inputBinding(mission, aggregate),
        )
    }

    private fun inputBinding(
        mission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1,
        aggregate: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1,
    ): HimEvidenceAlignmentCatalogAuditFindingsAnalysisInputBindingV1 {
        val unsigned = HimEvidenceAlignmentCatalogAuditFindingsAnalysisInputBindingV1(
            contractId = HimEvidenceAlignmentCatalogAuditFindingsAnalysisContractV1.VERSION,
            auditHead = mission.provenance.auditHead,
            enrichmentImplementationHead = mission.provenance.enrichmentImplementationHead,
            analysisImplementationHead = "f".repeat(40),
            enrichmentMission = fileBinding("build/knowledge/reports/him/analysis/mission.json", mission.logicalDigest),
            enrichmentAggregate = fileBinding("build/knowledge/reports/him/analysis/aggregate.json", aggregate.logicalDigest),
            enrichmentMissionLogicalDigest = mission.logicalDigest,
            enrichmentAggregateLogicalDigest = aggregate.logicalDigest,
            canonicalOrderDigest = mission.auditCanonicalOrderDigest,
            expectedCanonicalCount = mission.expectedCanonicalCount,
            expectedShardIds = mission.shards.map { it.shardId },
            bindingDigest = "",
        )
        return unsigned.copy(bindingDigest = HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1.bindingDigest(unsigned))
    }

    private fun fileBinding(path: String, logicalDigest: String) = HimEvidenceAlignmentCatalogAuditFindingsAnalysisFileBindingV1(
        relativePath = path,
        byteSize = 1,
        sha256 = "1".repeat(64),
        logicalDigest = logicalDigest,
    )

    private fun mission(): HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1 {
        val ids = (1..22).map { "c%05d".format(Locale.ROOT, it) }
        val shards = ids.chunked(2).mapIndexed { index, chunk ->
            HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardPlanV1(
                shardId = "shard-${(index + 1).toString().padStart(6, '0')}",
                startInclusive = index * 2,
                endExclusive = index * 2 + chunk.size,
                canonicalCount = chunk.size,
                canonicalOrderDigest = "a".repeat(64),
                canonicalEntityIds = chunk,
            )
        }
        val auditDigest = "b".repeat(64)
        val bindings = auditBindings()
        val provenance = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentProvenanceV1(
            auditHead = bindings.gitHead,
            enrichmentImplementationHead = "e".repeat(40),
            auditMission = enrichmentFile("build/audit/mission.json", auditDigest),
            auditAggregate = enrichmentFile("build/audit/aggregate.json", "c".repeat(64)),
            auditMissionLogicalDigest = auditDigest,
            auditAggregateLogicalDigest = "c".repeat(64),
            auditShardBindings = shards.map { shard ->
                HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardFileBindingV1(
                    shard.shardId,
                    enrichmentFile("build/audit/${shard.shardId}.json", "d".repeat(64)),
                )
            },
            auditBindings = bindings,
            extractorEvaluatorImplementationSha256 = "f".repeat(64),
        )
        val unsigned = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1(
            contractId = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentContractV1.VERSION,
            auditMissionDigest = auditDigest,
            auditCanonicalOrderDigest = "1".repeat(64),
            expectedCanonicalCount = ids.size,
            maxItemsPerShard = 2,
            shards = shards,
            provenance = provenance,
            logicalDigest = "",
        )
        return unsigned.copy(logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(unsigned)).also {
            it.validate()
        }
    }

    private fun aggregate(
        mission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1,
    ): HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1 {
        val results = mission.shards.map { shard ->
            val findings = when (shard.shardId) {
                "shard-000001" -> listOf(
                    finding(shard.shardId, "c00001", HimGroundTruthSource.OPEN_FOOD_FACTS, HimEvidenceAlignmentAuditClassification.DIRECT_SUPPORTED, OFF_REFERENCE, HimEvidenceRecordKind.OFF_PRODUCT, HimEvidenceAlignmentClassificationV1.PRIMARY_CANONICAL_MATCH),
                    finding(shard.shardId, "c00001", HimGroundTruthSource.AGRIBALYSE, HimEvidenceAlignmentAuditClassification.DIRECT_REJECTED, AGRIBALYSE_REFERENCE, HimEvidenceRecordKind.AGRIBALYSE_RECORD, HimEvidenceAlignmentClassificationV1.OTHER_PRIMARY_IDENTITY),
                    finding(shard.shardId, "c00001", HimGroundTruthSource.CIQUAL, HimEvidenceAlignmentAuditClassification.MODIFIER_ONLY, CIQUAL_REFERENCE, HimEvidenceRecordKind.CIQUAL_FOOD, HimEvidenceAlignmentClassificationV1.CANONICAL_ONLY_AS_MODIFIER),
                    finding(shard.shardId, "c00001", HimGroundTruthSource.GLYCEMIC_INDEX, HimEvidenceAlignmentAuditClassification.UNRESOLVED_PRIMARY_IDENTITY, GI_REFERENCE, HimEvidenceRecordKind.GI_MEASUREMENT, HimEvidenceAlignmentClassificationV1.UNRESOLVED_PRIMARY_IDENTITY),
                )
                "shard-000002" -> listOf(
                    finding(shard.shardId, "c00004", HimGroundTruthSource.OPEN_FOOD_FACTS, HimEvidenceAlignmentAuditClassification.DIRECT_REJECTED, OFF_REFERENCE, HimEvidenceRecordKind.OFF_PRODUCT, HimEvidenceAlignmentClassificationV1.OTHER_PRIMARY_IDENTITY),
                    finding(shard.shardId, "c00004", HimGroundTruthSource.AGRIBALYSE, HimEvidenceAlignmentAuditClassification.INVALID_PROJECTION, AGRIBALYSE_OTHER_REFERENCE, HimEvidenceRecordKind.AGRIBALYSE_RECORD, null),
                    finding(shard.shardId, "c00004", HimGroundTruthSource.CIQUAL, HimEvidenceAlignmentAuditClassification.UNSUPPORTED_RECORD_KIND, CIQUAL_OTHER_REFERENCE, HimEvidenceRecordKind.CIQUAL_TAXONOMY, null),
                    finding(shard.shardId, "c00004", HimGroundTruthSource.GLYCEMIC_INDEX, HimEvidenceAlignmentAuditClassification.NO_RETRIEVAL_HIT, null, null, null),
                    finding(shard.shardId, "c00003", HimGroundTruthSource.OPEN_FOOD_FACTS, HimEvidenceAlignmentAuditClassification.SOURCE_OR_BINDING_FAILURE, OFF_OTHER_REFERENCE, HimEvidenceRecordKind.OFF_PRODUCT, null),
                )
                else -> emptyList()
            }
            val counters = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentCountersV1(
                canonicalsProcessed = shard.canonicalCount,
                sourcesProcessed = HimEvidenceAlignmentCatalogAuditFindingsAnalysisContractV1.SOURCE_ORDER.size,
                findingOccurrences = findings.size,
                uniqueEvidenceReferences = findings.mapNotNull { it.originalEvidenceReference }.distinct().size,
                exactFetches = findings.mapNotNull { it.originalEvidenceReference }.distinct().size,
                noRetrievalHits = findings.count { it.reconstructionStatus == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.NO_RETRIEVAL_HIT },
                unsupportedRecordKinds = findings.count { it.reconstructionStatus == HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.UNSUPPORTED_RECORD_KIND },
                reconstructedFindings = findings.count { it.reconstructionStatus != HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.NO_RETRIEVAL_HIT },
                classificationMismatches = 0,
            )
            val unsigned = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardResultV1(
                missionDigest = mission.logicalDigest,
                shardId = shard.shardId,
                shardBindingDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.shardBindingDigest(mission, shard),
                state = HimEvidenceAlignmentCatalogAuditState.COMPLETE,
                findings = findings,
                counters = counters,
                logicalDigest = "",
            )
            unsigned.copy(logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(unsigned))
        }
        val counters = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentCountersV1(22, 44, 9, 8, 8, 1, 1, 8, 0)
        val unsigned = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1(
            missionDigest = mission.logicalDigest,
            state = HimEvidenceAlignmentCatalogAuditState.COMPLETE,
            shardResults = results,
            counters = counters,
            logicalDigest = "",
        )
        return unsigned.copy(logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(unsigned)).also {
            it.validateAgainst(mission)
        }
    }

    private fun finding(
        shardId: String,
        entityId: String,
        source: HimGroundTruthSource,
        classification: HimEvidenceAlignmentAuditClassification,
        reference: String?,
        recordKind: HimEvidenceRecordKind?,
        alignment: HimEvidenceAlignmentClassificationV1?,
    ): HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFindingV1 {
        val noHit = classification == HimEvidenceAlignmentAuditClassification.NO_RETRIEVAL_HIT
        val occurrence = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentContractV1.findingOccurrenceId(shardId, entityId, source, reference)
        return HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFindingV1(
            findingOccurrenceId = occurrence,
            auditShardId = shardId,
            entityId = entityId,
            canonicalName = "Fixture $entityId",
            normalizedName = "fixture-$entityId",
            source = source,
            originalAuditClassification = classification,
            originalEvidenceReference = reference,
            originalPrimaryIdentity = if (classification == HimEvidenceAlignmentAuditClassification.DIRECT_SUPPORTED) "Fixture" else null,
            queryPlanTerms = listOf(HimEvidenceAlignmentCatalogAuditQueryTermV1("Fixture", "fixture", HimEvidenceAlignmentCatalogAuditQueryTermKind.CANONICAL_NAME)),
            recordKind = recordKind,
            projectionSha256 = if (noHit) null else "1".repeat(64),
            candidateIdentities = when (classification) {
                HimEvidenceAlignmentAuditClassification.DIRECT_REJECTED -> listOf("one", "two")
                HimEvidenceAlignmentAuditClassification.DIRECT_SUPPORTED,
                HimEvidenceAlignmentAuditClassification.UNRESOLVED_PRIMARY_IDENTITY,
                -> listOf("one")
                else -> emptyList()
            },
            reconstructedPrimaryIdentity = if (alignment == HimEvidenceAlignmentClassificationV1.PRIMARY_CANONICAL_MATCH) "Fixture" else null,
            modifiers = when (classification) {
                HimEvidenceAlignmentAuditClassification.MODIFIER_ONLY -> listOf("form", "style")
                HimEvidenceAlignmentAuditClassification.UNRESOLVED_PRIMARY_IDENTITY -> listOf("form")
                else -> emptyList()
            },
            extractorResolution = when (classification) {
                HimEvidenceAlignmentAuditClassification.NO_RETRIEVAL_HIT,
                HimEvidenceAlignmentAuditClassification.UNSUPPORTED_RECORD_KIND,
                HimEvidenceAlignmentAuditClassification.INVALID_PROJECTION,
                HimEvidenceAlignmentAuditClassification.SOURCE_OR_BINDING_FAILURE,
                -> null
                HimEvidenceAlignmentAuditClassification.UNRESOLVED_PRIMARY_IDENTITY -> "UNRESOLVED"
                else -> "RESOLVED"
            },
            identityFieldsUsed = if (noHit) emptyList() else listOf("primaryName"),
            extractionPath = if (noHit) null else "FIXTURE_PATH",
            reconstructedAlignmentClassification = alignment,
            reconstructedReasonCode = if (alignment == null) null else "FIXTURE_REASON",
            classificationComparison = if (noHit || alignment == null) HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.NOT_RECONSTRUCTABLE else HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.MATCH,
            reconstructionStatus = when {
                noHit -> HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.NO_RETRIEVAL_HIT
                classification == HimEvidenceAlignmentAuditClassification.UNSUPPORTED_RECORD_KIND -> HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.UNSUPPORTED_RECORD_KIND
                else -> HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.RECONSTRUCTED
            },
        )
    }

    private fun enrichmentFile(path: String, logicalDigest: String) = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFileBindingV1(path, 1, "1".repeat(64), logicalDigest)

    private fun auditBindings() = HimEvidenceAlignmentCatalogAuditBindingsV1(
        gitHead = "a".repeat(40),
        implementationBindingSha256 = "b".repeat(64),
        canonicalCatalog = HimEvidenceAlignmentCatalogAuditFileBindingV1("data/catalog.json", 1, "c".repeat(64)),
        authority = HimEvidenceAlignmentCatalogAuditFileBindingV1("data/authority.json", 1, "d".repeat(64)),
        groundTruthReleaseReference = "release:v1:${"e".repeat(64)}",
        sourceBindings = HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER.mapIndexed { index, source ->
            HimEvidenceAlignmentCatalogAuditSourceBindingV1(
                source = source,
                indexRelativePath = "data/index/${source.name.lowercase(Locale.ROOT)}.sqlite",
                indexByteSize = index.toLong() + 1,
                sqliteFileSha256 = "f".repeat(64),
                sourceArtifactPath = source.artifactPath,
                sourceArtifactSha256 = "1".repeat(64),
                schemaVersion = "INDEX_SCHEMA_V1",
                indexBuildPolicyVersion = "INDEX_BUILD_V1",
                evidenceProjectionPolicyVersion = "PROJECTION_V1",
                logicalContentSha256 = "2".repeat(64),
                buildState = HimEvidenceRetrievalIndexBuildState.VALIDATED,
            )
        },
    )

    companion object {
        private const val OFF_REFERENCE = "off:product:row:1:code:one"
        private const val OFF_OTHER_REFERENCE = "off:product:row:2:code:two"
        private const val AGRIBALYSE_REFERENCE = "agribalyse:row:1:agb:one"
        private const val AGRIBALYSE_OTHER_REFERENCE = "agribalyse:row:2:agb:two"
        private const val CIQUAL_REFERENCE = "ciqual:food:one"
        private const val CIQUAL_OTHER_REFERENCE = "ciqual:food:two"
        private const val GI_REFERENCE = "gi:measurement:1"
    }
}
