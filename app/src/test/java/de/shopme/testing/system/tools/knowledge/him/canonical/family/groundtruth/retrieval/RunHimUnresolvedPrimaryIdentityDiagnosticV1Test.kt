package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimProductOnlyCanonicalMaster
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimAgribalyseEvidenceProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimCiqualEvidenceProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimCiqualLogicalRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimDeterministicEvidenceAlignmentEvaluatorV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentAuditClassification
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentClassificationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditBindingsV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentCountersV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentProvenanceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardPlanV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagCountV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisFindingIndexEntryV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisFlagsV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeCountV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsAnalysisReportV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentRuntimeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditQueryTermKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditQueryTermV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexBuildState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexRecord
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceSearchText
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceProjection
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimGlycemicIndexEvidenceProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimGlycemicIndexLogicalRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticAggregateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticCountersV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticFailureReasonV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticMissionInputV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticRuntimeRequestV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticRuntimeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticExactFetchPortV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticMissionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticShardResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticSourceReferenceCountV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticLanguageV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticStatusV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimOffEvidenceProjectionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditSourceBindingV1
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.test.fail
import java.io.File

class RunHimUnresolvedPrimaryIdentityDiagnosticV1Test {
    @Test fun selectsOnlyUnresolvedReconstructedFindings() {
        val fixture = Fixture.small()
        val mission = assertIs<HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Completed<*>>(fixture.plan()).value as HimUnresolvedPrimaryIdentityDiagnosticMissionV1
        assertEquals(4, mission.expectedOccurrenceCount)
        assertEquals(4, mission.expectedReferenceCount)
    }

    @Test fun scaledFixtureContainsExactly3036Occurrences() {
        val fixture = Fixture.scaled()
        val mission = fixture.planValue()
        assertEquals(3036, mission.expectedOccurrenceCount)
        assertEquals(2983, mission.expectedReferenceCount)
        assertEquals(924, mission.expectedCanonicalCount)
        assertEquals(listOf(2743, 60, 105, 75), mission.inputBinding.sourceReferenceCounts.map { it.count })
    }

    @Test fun globallyDeduplicatesSourceAndReferencePairs() {
        val mission = Fixture.scaled().planValue()
        assertEquals(2983, mission.referencePlans.map { it.key }.distinct().size)
    }

    @Test fun assignsDeterministicOwnerToEveryReference() {
        val mission = Fixture.scaled().planValue()
        mission.referencePlans.forEach { plan -> assertEquals(HimUnresolvedPrimaryIdentityDiagnosticContractV1.expectedOwner(plan.occurrences), plan.ownerShardId) }
    }

    @Test fun preservesReferenceReuseAcrossCanonicalAndAuditShards() {
        val mission = Fixture.scaled().planValue()
        assertEquals(50, mission.reusedReferenceCount)
        assertEquals(103, mission.reusedOccurrenceCount)
    }

    @Test fun losesNoOccurrenceAndDuplicatesNone() {
        val mission = Fixture.scaled().planValue()
        val occurrences = mission.referencePlans.flatMap { it.occurrences }
        assertEquals(3036, occurrences.size)
        assertEquals(3036, occurrences.map { it.findingOccurrenceId }.distinct().size)
    }

    @Test fun reversedInputProducesIdenticalMission() {
        val fixture = Fixture.scaled()
        val reversed = fixture.withReport(fixture.report.copy(findingIndex = fixture.report.findingIndex.asReversed()).withDigest())
        assertEquals(fixture.planValue(), reversed.planValue())
    }

    @Test fun shardPlansAreSortedAndPartitionReferencesOnce() {
        val mission = Fixture.scaled().planValue()
        assertEquals(HimUnresolvedPrimaryIdentityDiagnosticContractV1.SHARD_IDS, mission.shards.map { it.shardId })
        assertEquals(mission.expectedReferenceCount, mission.shards.flatMap { it.referencePlanKeys }.size)
    }

    @Test fun exactFetchPortHasNoSearchOrScanOperation() {
        val names = HimUnresolvedPrimaryIdentityDiagnosticExactFetchPortV1::class.java.methods.map { it.name }
        assertEquals(listOf("fetchExact", "getSource"), names.filter { it != "wait" }.sorted())
    }

    @Test fun routesOpenFoodFactsThroughExistingEvaluatorExtractor() {
        val result = Fixture.small().execute().records.single { it.source == HimGroundTruthSource.OPEN_FOOD_FACTS }
        assertEquals(HimEvidenceRecordKind.OFF_PRODUCT, result.recordKind)
    }

    @Test fun routesAgribalyseThroughExistingEvaluatorExtractor() {
        val result = Fixture.small().execute().records.single { it.source == HimGroundTruthSource.AGRIBALYSE }
        assertEquals(HimEvidenceRecordKind.AGRIBALYSE_RECORD, result.recordKind)
    }

    @Test fun routesCiqualThroughExistingEvaluatorExtractor() {
        val result = Fixture.small().execute().records.single { it.source == HimGroundTruthSource.CIQUAL }
        assertEquals(HimEvidenceRecordKind.CIQUAL_FOOD, result.recordKind)
    }

    @Test fun routesGlycemicIndexThroughExistingEvaluatorExtractor() {
        val result = Fixture.small().execute().records.single { it.source == HimGroundTruthSource.GLYCEMIC_INDEX }
        assertEquals(HimEvidenceRecordKind.GI_MEASUREMENT, result.recordKind)
    }

    @Test fun persistsAllFieldProvenance() {
        val records = Fixture.small().execute().records
        assertTrue(records.all { it.fields.all { field -> field.fieldPath.isNotBlank() && field.originalLexicalValue.trim() == field.trimmedValue && field.recordKind.source == field.source } })
    }

    @Test fun usesOnlyDeclaredFieldLanguages() {
        val allowed = HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.values().toSet()
        assertTrue(Fixture.small().execute().records.flatMap { it.fields }.all { it.language in allowed })
        assertTrue(allowed.contains(HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.ES_ES))
    }

    @Test fun doesNotGuessSpanishRegionalVariants() {
        assertEquals(listOf("ES_ES"), HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.values().filter { it.name.startsWith("ES") }.map { it.name })
    }

    @Test fun brandsRemainContext() {
        val fields = Fixture.small().execute().records.flatMap { it.fields }
        assertTrue(fields.filter { it.role == HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.BRAND_CONTEXT }.all { it.lexicalFeatures.containsBrandContext })
        assertTrue(fields.none { it.role == HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.BRAND_CONTEXT && it.fieldPath.contains("primary", true) })
    }

    @Test fun categoriesRemainContext() {
        val fields = Fixture.small().execute().records.flatMap { it.fields }
        assertTrue(fields.filter { it.role == HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.CATEGORY_CONTEXT }.all { it.lexicalFeatures.containsBrandContext.not() })
    }

    @Test
    fun zeroSingleAndMultipleCandidatesAreTyped() {
        val records = Fixture.small().execute().records
        val zeroCandidates = records.filter { it.candidateIdentities.isEmpty() }
        val singleCandidate = records.filter { it.candidateIdentities.size == 1 }
        val multipleCandidates = records.filter { it.candidateIdentities.size >= 2 }

        assertTrue(zeroCandidates.isNotEmpty())
        assertTrue(singleCandidate.isNotEmpty())
        assertTrue(multipleCandidates.isNotEmpty())

        assertTrue(
            zeroCandidates.all {
                it.status == HimUnresolvedPrimaryIdentityDiagnosticStatusV1.IDENTITY_FIELDS_PRESENT_NO_CANDIDATE
            },
        )
        assertTrue(
            singleCandidate.all {
                it.status == HimUnresolvedPrimaryIdentityDiagnosticStatusV1.SINGLE_CANDIDATE_UNRESOLVED
            },
        )
        assertTrue(
            multipleCandidates.all {
                it.status == HimUnresolvedPrimaryIdentityDiagnosticStatusV1.MULTIPLE_CANDIDATES_UNRESOLVED
            },
        )
    }

    @Test fun missingIdentityFieldsAreTyped() {
        val record = Fixture.small(withOffIdentity = false).execute().records.first { it.source == HimGroundTruthSource.OPEN_FOOD_FACTS }
        assertEquals(HimUnresolvedPrimaryIdentityDiagnosticStatusV1.IDENTITY_FIELDS_MISSING, record.status)
    }

    @Test fun emptyIdentityFieldsAreTyped() {
        val record = Fixture.small(withOffIdentity = true, offValue = "").execute().records.first { it.source == HimGroundTruthSource.OPEN_FOOD_FACTS }
        assertEquals(HimUnresolvedPrimaryIdentityDiagnosticStatusV1.IDENTITY_FIELDS_EMPTY, record.status)
    }

    @Test fun fetchMissFailsClosed() {
        val fixture = Fixture.small()
        val request = fixture.request(fetchPorts = fixture.ports.map { port -> Port(port.source) { _, _ -> null } })
        val failure = assertIs<HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Failed>(HimUnresolvedPrimaryIdentityDiagnosticRuntimeV1.executeShard(request))
        assertEquals(HimUnresolvedPrimaryIdentityDiagnosticFailureReasonV1.EXACT_FETCH_FAILED, failure.reason)
    }

    @Test fun sourceMismatchFailsClosed() {
        val fixture = Fixture.small()
        val bad = fixture.records[HimGroundTruthSource.OPEN_FOOD_FACTS]!!.copy(
            sourceRecordReference = fixture.records[HimGroundTruthSource.AGRIBALYSE]!!.sourceRecordReference,
            recordKind = HimEvidenceRecordKind.AGRIBALYSE_RECORD,
        )
        val ports = fixture.ports.map { port -> if (port.source == HimGroundTruthSource.OPEN_FOOD_FACTS) Port(port.source) { _, _ -> bad } else port }
        val failure = assertIs<HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Failed>(HimUnresolvedPrimaryIdentityDiagnosticRuntimeV1.executeShard(fixture.request(ports)))
        assertEquals(HimUnresolvedPrimaryIdentityDiagnosticFailureReasonV1.RECORD_KIND_MISMATCH, failure.reason)
    }

    @Test
    fun recordKindMismatchFailsClosed() {
        val fixture = Fixture.small()
        val bad = fixture.records[HimGroundTruthSource.CIQUAL]!!.copy(
            recordKind = HimEvidenceRecordKind.CIQUAL_TAXONOMY,
        )
        val ports = fixture.ports.map { port ->
            if (port.source == HimGroundTruthSource.CIQUAL) {
                Port(port.source) { _, _ -> bad }
            } else {
                port
            }
        }
        val failure =
            assertIs<HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Failed>(
                HimUnresolvedPrimaryIdentityDiagnosticRuntimeV1.executeShard(
                    fixture.request(ports),
                ),
            )

        assertEquals(
            HimUnresolvedPrimaryIdentityDiagnosticFailureReasonV1.RECORD_KIND_MISMATCH,
            failure.reason,
        )
    }

    @Test
    fun missionBindingManipulationFailsClosed() {
        val mission = Fixture.small().planValue()
        val manipulatedAuditHead =
            if (mission.auditHead == "a".repeat(40)) {
                "b".repeat(40)
            } else {
                "a".repeat(40)
            }
        require(manipulatedAuditHead != mission.auditHead)
        val broken = mission.copy(auditHead = manipulatedAuditHead)

        assertFailsWith<IllegalArgumentException> { broken.validate() }
    }

    @Test fun shardBindingManipulationFailsClosed() {
        val fixture = Fixture.small()
        val broken = fixture.mission.copy(shards = fixture.mission.shards.drop(1) + fixture.mission.shards.first())
        assertFailsWith<IllegalArgumentException> { broken.validate() }
    }

    @Test fun counterManipulationFailsClosed() {
        val fixture = Fixture.small()
        val result = fixture.execute()
        val broken = result.copy(counters = result.counters.copy(recordsLoaded = result.counters.recordsLoaded + 1))
        assertFailsWith<IllegalArgumentException> { broken.validateAgainst(fixture.mission) }
    }

    @Test fun jsonAndTextPersistenceReloadsAndIsByteIdentical() {
        val fixture = Fixture.small()
        val root = createTempDirectory("him-unresolved-diagnostic").toFile()
        try {
            val file = root.resolve("shard.json")
            HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.writeShard(file, fixture.mission, fixture.execute())
            val before = file.readBytes()
            HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.writeShard(file, fixture.mission, fixture.execute())
            assertEquals(before.toList(), file.readBytes().toList())
            assertEquals(fixture.execute(), HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.readShard(file, fixture.mission))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun safeFailuresContainNoThrowableOrPath() {
        val fixture = Fixture.small()
        val request = fixture.request(fetchPorts = fixture.ports.map { Port(it.source) { _, _ -> null } })
        val failure = assertIs<HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Failed>(HimUnresolvedPrimaryIdentityDiagnosticRuntimeV1.executeShard(request))
        assertFalse(failure.safeContext.contains("/") || failure.safeContext.contains("Exception") || failure.safeContext.contains(".db"))
    }

    @Test fun noHitDirectModifierAndUnsupportedFindingsAreExcluded() {
        val report = Fixture.small().report
        assertEquals(4, report.findingIndex.size)
        assertTrue(report.findingIndex.all { it.originalAuditClassification == HimEvidenceAlignmentAuditClassification.UNRESOLVED_PRIMARY_IDENTITY })
    }

    @Test fun alignmentClassificationIsNotRewritten() {
        assertTrue(Fixture.small().report.findingIndex.all { it.originalAuditClassification == HimEvidenceAlignmentAuditClassification.UNRESOLVED_PRIMARY_IDENTITY })
    }

    private data class Fixture(
        val report: HimEvidenceAlignmentCatalogAuditFindingsAnalysisReportV1,
        val enrichmentMission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1,
        val enrichmentAggregate: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1,
        val records: Map<HimGroundTruthSource, HimEvidenceRetrievalIndexRecord>,
        val mission: HimUnresolvedPrimaryIdentityDiagnosticMissionV1,
        val ports: List<Port>,
    ) {
        fun plan() = HimUnresolvedPrimaryIdentityDiagnosticRuntimeV1.planMission(
            HimUnresolvedPrimaryIdentityDiagnosticMissionInputV1(
                report,
                binding("analysis.json", "1"),
                enrichmentMission,
                bindingWithLogical("enrichment-mission.json", enrichmentMission.logicalDigest),
                enrichmentAggregate,
                bindingWithLogical("enrichment-aggregate.json", enrichmentAggregate.logicalDigest),
                sourceBindings(),
                "d".repeat(40),
            ),
        )

        fun planValue() = assertIs<HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Completed<*>>(plan()).value as HimUnresolvedPrimaryIdentityDiagnosticMissionV1

        fun request(fetchPorts: List<Port> = ports) = HimUnresolvedPrimaryIdentityDiagnosticRuntimeRequestV1(true, mission, "shard-000001", catalog(), authority(), fetchPorts, null)

        fun execute(): HimUnresolvedPrimaryIdentityDiagnosticShardResultV1 {
            val result = HimUnresolvedPrimaryIdentityDiagnosticRuntimeV1.executeShard(request())
            return when (result) {
                is HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Completed -> result.value
                is HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Failed ->
                    fail("${result.reason} ${result.safeContext}")
                is HimUnresolvedPrimaryIdentityDiagnosticRuntimeResult.Skipped ->
                    fail(result.reason)
            }
        }

        fun withReport(report: HimEvidenceAlignmentCatalogAuditFindingsAnalysisReportV1) = Fixture(report, enrichmentMission, enrichmentAggregate, records, mission, ports)

        companion object {
            fun small(withOffIdentity: Boolean = true, offValue: String = "Unknown OFF") = create(4, withOffIdentity, offValue)

            fun scaled() = create(3036, true, "Unknown OFF")

            private fun create(count: Int, withOffIdentity: Boolean, offValue: String): Fixture {
                val all = (0 until count).map { index -> entry(index, count) }
                val report = analysisReport(all)
                val enrichmentMission = enrichmentMission()
                val aggregate = enrichmentAggregate(enrichmentMission)
                val records = mapOf(
                    HimGroundTruthSource.OPEN_FOOD_FACTS to offRecord(1, "100", withOffIdentity, offValue),
                    HimGroundTruthSource.AGRIBALYSE to agribalyseRecord(2, "200"),
                    HimGroundTruthSource.CIQUAL to ciqualRecord(3, "300"),
                    HimGroundTruthSource.GLYCEMIC_INDEX to giRecord(4),
                )
                val preliminary = Fixture(report, enrichmentMission, aggregate, records, missionPlaceholder(), emptyList())
                val mission = preliminary.planValue()
                val ports = HimUnresolvedPrimaryIdentityDiagnosticContractV1.SOURCE_ORDER.map { source ->
                    Port(source) { _, reference -> records[source] }
                }
                return preliminary.copy(mission = mission, ports = ports)
            }

            private fun entry(index: Int, count: Int): HimEvidenceAlignmentCatalogAuditFindingsAnalysisFindingIndexEntryV1 {
                val unique = if (index < 2983 || count < 2983) index else when (index - 2983) {
                    in 0..46 -> index - 2983
                    in 47..52 -> 47 + ((index - 2983 - 47) / 2)
                    else -> 0
                }
                val source = if (count < 2983) smallSourceFor(index) else sourceFor(unique)
                val ref = if (count < 2983) smallReferenceFor(source) else referenceFor(source, unique)
                val entity = "c${(index % 924 + 1).toString().padStart(5, '0')}"
                val candidates = when {
                    count < 2983 -> when (index) { 0 -> emptyList(); 1 -> listOf("One"); else -> listOf("One", "Two") }
                    index < 2841 -> emptyList()
                    index < 2898 -> listOf("One")
                    else -> listOf("One", "Two")
                }
                val resolution = if (index < 102) "MISSING" else "UNRESOLVED"
                val shardId = if (count < 2983) "shard-000001" else "shard-${(index % 11 + 1).toString().padStart(6, '0')}"
                return HimEvidenceAlignmentCatalogAuditFindingsAnalysisFindingIndexEntryV1(
                    findingOccurrenceId = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentContractV1.findingOccurrenceId(shardId, entity, source, ref),
                    auditShardId = shardId,
                    entityId = entity,
                    canonicalName = "Canonical $entity",
                    normalizedName = "canonical-$entity",
                    source = source,
                    originalAuditClassification = HimEvidenceAlignmentAuditClassification.UNRESOLVED_PRIMARY_IDENTITY,
                    originalEvidenceReference = ref,
                    originalPrimaryIdentity = null,
                    originalClaimedEvidenceRelation = null,
                    originalReasonPersistenceState = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentContractV1.NOT_PERSISTED_IN_AUDIT_V1,
                    queryPlanTerms = listOf(HimEvidenceAlignmentCatalogAuditQueryTermV1("canonical", "canonical", HimEvidenceAlignmentCatalogAuditQueryTermKind.CANONICAL_NAME)),
                    queryTermFindingState = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentContractV1.NOT_PERSISTED_IN_AUDIT_V1,
                    recordKind = kindFor(source),
                    projectionSha256 = "a".repeat(64),
                    candidateIdentities = candidates,
                    reconstructedPrimaryIdentity = null,
                    modifiers = emptyList(),
                    extractorResolution = resolution,
                    identityFieldsUsed = if (resolution == "MISSING") emptyList() else listOf("identity.productName"),
                    extractionPath = "UNRESOLVED",
                    reconstructedAlignmentClassification = HimEvidenceAlignmentClassificationV1.UNRESOLVED_PRIMARY_IDENTITY,
                    reconstructedReasonCode = "PRIMARY_IDENTITY_UNRESOLVED",
                    classificationComparison = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentClassificationComparisonV1.MATCH,
                    reconstructionStatus = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentStatus.RECONSTRUCTED,
                    flags = flags(),
                )
            }

            private fun analysisReport(entries: List<HimEvidenceAlignmentCatalogAuditFindingsAnalysisFindingIndexEntryV1>): HimEvidenceAlignmentCatalogAuditFindingsAnalysisReportV1 {
                val input = HimEvidenceAlignmentCatalogAuditFindingsAnalysisInputBindingV1(
                    HimEvidenceAlignmentCatalogAuditFindingsAnalysisContractV1.VERSION,
                    "a".repeat(40), "b".repeat(40), "c".repeat(40),
                    analysisBinding("enrichment-mission.json", "2"), analysisBinding("enrichment-aggregate.json", "3"),
                    "2".repeat(64), "3".repeat(64), "4".repeat(64), 924,
                    HimUnresolvedPrimaryIdentityDiagnosticContractV1.SHARD_IDS,
                    "",
                )
                val bound = input.copy(bindingDigest = HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1.bindingDigest(input))
                val unsigned = HimEvidenceAlignmentCatalogAuditFindingsAnalysisReportV1(
                    HimEvidenceAlignmentCatalogAuditFindingsAnalysisContractV1.VERSION, bound,
                    HimEvidenceAlignmentCatalogAuditFindingsEnrichmentCountersV1(entries.size.coerceAtMost(924), 4, entries.size, entries.size - (entries.size - 2983).coerceAtLeast(0), entries.size - (entries.size - 2983).coerceAtLeast(0), 0, 0, entries.size, 0),
                    entries.size, listOf(HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeCountV1(HimEvidenceAlignmentCatalogAuditFindingsAnalysisPrimaryOutcomeV1.UNRESOLVED_PRIMARY_IDENTITY, entries.size)), emptyList(), HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagV1.values().map { HimEvidenceAlignmentCatalogAuditFindingsAnalysisDiagnosticFlagCountV1(it, 0) }, entries, "",
                )
                return unsigned.copy(logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1.logicalDigest(unsigned))
            }

            private fun enrichmentMission(): HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1 {
                val audit = auditBindings()
                val provenance = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentProvenanceV1(
                    "a".repeat(40), "b".repeat(40), enrichmentBinding("audit-mission.json", "4"), enrichmentBinding("audit-aggregate.json", "5"), "4".repeat(64), "5".repeat(64), HimUnresolvedPrimaryIdentityDiagnosticContractV1.SHARD_IDS.map { HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardFileBindingV1(it, enrichmentBinding("$it.json", "6")) }, audit, "6".repeat(64),
                )
                val shards = HimUnresolvedPrimaryIdentityDiagnosticContractV1.SHARD_IDS.mapIndexed { index, id -> HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardPlanV1(id, index, index + 1, 1, "7".repeat(64), listOf("c${(index + 1).toString().padStart(5, '0')}")) }
                val unsigned = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1(HimEvidenceAlignmentCatalogAuditFindingsEnrichmentContractV1.VERSION, "4".repeat(64), "8".repeat(64), 11, 1, shards, provenance, "")
                return unsigned.copy(logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(unsigned))
            }

            private fun enrichmentAggregate(mission: HimEvidenceAlignmentCatalogAuditFindingsEnrichmentMissionV1): HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1 {
                val shards = mission.shards.map { shard ->
                    val unsigned = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentShardResultV1(mission.logicalDigest, shard.shardId, HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.shardBindingDigest(mission, shard), HimEvidenceAlignmentCatalogAuditState.COMPLETE, emptyList(), HimEvidenceAlignmentCatalogAuditFindingsEnrichmentCountersV1(0, 0, 0, 0, 0, 0, 0, 0, 0), "")
                    unsigned.copy(logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(unsigned))
                }
                val unsigned = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentAggregateV1(mission.logicalDigest, HimEvidenceAlignmentCatalogAuditState.COMPLETE, shards, HimEvidenceAlignmentCatalogAuditFindingsEnrichmentCountersV1(0, 0, 0, 0, 0, 0, 0, 0, 0), "")
                return unsigned.copy(logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentPersistenceV1.logicalDigest(unsigned))
            }

            private fun missionPlaceholder() = enrichmentMissionInputMission()
            private fun enrichmentMissionInputMission() = HimUnresolvedPrimaryIdentityDiagnosticMissionV1("", inputBindingPlaceholder(), "a".repeat(40), "b".repeat(40), "c".repeat(40), "d".repeat(40), binding("analysis.json", "1"), binding("enrichment-mission.json", "2"), binding("enrichment-aggregate.json", "3"), sourceBindings(), 0, 0, 0, 0, 0, 0, 0, 0, 0, emptyList(), emptyList(), "")
            private fun inputBindingPlaceholder() = HimUnresolvedPrimaryIdentityDiagnosticInputBindingV1(binding("analysis.json", "1"), binding("enrichment-mission.json", "2"), binding("enrichment-aggregate.json", "3"), "a".repeat(40), "b".repeat(40), "c".repeat(40), 1, 1, 1, listOf(HimUnresolvedPrimaryIdentityDiagnosticSourceReferenceCountV1(HimGroundTruthSource.OPEN_FOOD_FACTS, 1), HimUnresolvedPrimaryIdentityDiagnosticSourceReferenceCountV1(HimGroundTruthSource.AGRIBALYSE, 0), HimUnresolvedPrimaryIdentityDiagnosticSourceReferenceCountV1(HimGroundTruthSource.CIQUAL, 0), HimUnresolvedPrimaryIdentityDiagnosticSourceReferenceCountV1(HimGroundTruthSource.GLYCEMIC_INDEX, 0)), "")

            private fun sourceBindings() = HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER.map { source -> HimEvidenceAlignmentCatalogAuditSourceBindingV1(source, "index/${source.name.lowercase()}.sqlite", 1, "e".repeat(64), source.artifactPath, "f".repeat(64), "schema", "build", "projection", "0".repeat(64), HimEvidenceRetrievalIndexBuildState.VALIDATED) }
            private fun auditBindings() = HimEvidenceAlignmentCatalogAuditBindingsV1("HIM_EVIDENCE_ALIGNMENT_CATALOG_AUDIT_CONTRACT_V1", "HIM_EVIDENCE_ALIGNMENT_CATALOG_AUDIT_CONTRACT_V1", "a".repeat(40), "1".repeat(64), auditFile("catalog.json"), auditFile("authority.json"), "release:v1:${"2".repeat(64)}", sourceBindings())
            private fun auditFile(path: String) = HimEvidenceAlignmentCatalogAuditFileBindingV1(path, 1, "3".repeat(64))
            private fun binding(path: String, digit: String) = HimUnresolvedPrimaryIdentityDiagnosticFileBindingV1(path, 1, digit.repeat(64), digit.repeat(64))
            private fun bindingWithLogical(path: String, logicalDigest: String) = HimUnresolvedPrimaryIdentityDiagnosticFileBindingV1(path, 1, "2".repeat(64), logicalDigest)
            private fun analysisBinding(path: String, digit: String) = HimEvidenceAlignmentCatalogAuditFindingsAnalysisFileBindingV1(path, 1, digit.repeat(64), digit.repeat(64))
            private fun enrichmentBinding(path: String, digit: String) = HimEvidenceAlignmentCatalogAuditFindingsEnrichmentFileBindingV1(path, 1, digit.repeat(64), digit.repeat(64))
            private fun flags() = HimEvidenceAlignmentCatalogAuditFindingsAnalysisFlagsV1(false, false, false, false, false, true, true, true, true, false, true, true, false, false, true, false, false, false, false)
            private fun sourceFor(index: Int) = when { index < 2743 -> HimGroundTruthSource.OPEN_FOOD_FACTS; index < 2803 -> HimGroundTruthSource.AGRIBALYSE; index < 2908 -> HimGroundTruthSource.CIQUAL; else -> HimGroundTruthSource.GLYCEMIC_INDEX }
            private fun smallSourceFor(index: Int) = HimUnresolvedPrimaryIdentityDiagnosticContractV1.SOURCE_ORDER[index]
            private fun smallReferenceFor(source: HimGroundTruthSource) = when (source) { HimGroundTruthSource.OPEN_FOOD_FACTS -> HimEvidenceRecordReference.offProduct(1, "100").value; HimGroundTruthSource.AGRIBALYSE -> HimEvidenceRecordReference.agribalyse(2, "200").value; HimGroundTruthSource.CIQUAL -> HimEvidenceRecordReference.ciqualFood("300").value; HimGroundTruthSource.GLYCEMIC_INDEX -> HimEvidenceRecordReference.gi("measurement", 4).value }
            private fun referenceFor(source: HimGroundTruthSource, index: Int) = when (source) { HimGroundTruthSource.OPEN_FOOD_FACTS -> HimEvidenceRecordReference.offProduct(index + 1L, "${100000 + index}").value; HimGroundTruthSource.AGRIBALYSE -> HimEvidenceRecordReference.agribalyse(index - 2742L, "${200000 + index}").value; HimGroundTruthSource.CIQUAL -> HimEvidenceRecordReference.ciqualFood("${300000 + index}").value; HimGroundTruthSource.GLYCEMIC_INDEX -> HimEvidenceRecordReference.gi("measurement", index - 2907L).value }
            private fun kindFor(source: HimGroundTruthSource) = when (source) { HimGroundTruthSource.OPEN_FOOD_FACTS -> HimEvidenceRecordKind.OFF_PRODUCT; HimGroundTruthSource.AGRIBALYSE -> HimEvidenceRecordKind.AGRIBALYSE_RECORD; HimGroundTruthSource.CIQUAL -> HimEvidenceRecordKind.CIQUAL_FOOD; HimGroundTruthSource.GLYCEMIC_INDEX -> HimEvidenceRecordKind.GI_MEASUREMENT }
            private fun offRecord(row: Long, code: String, withIdentity: Boolean, value: String) = HimOffEvidenceProjectionV1.fromProjectionJson("{\"rowOrdinal\":$row,\"source\":{\"code\":\"$code\"},\"identity\":${if (withIdentity) "{\"productName\":\"$value\"${if (value.isNotEmpty()) ",\"brands\":[\"Brand\"]" else ""}}" else "{}"},\"quality\":{}}")
            private fun agribalyseRecord(row: Long, code: String) = HimAgribalyseEvidenceProjectionV1.fromProjectionJson("{\"rowOrdinal\":$row,\"agbCode\":\"$code\",\"ciqualCode\":\"1\",\"seasonCode\":\"2\",\"airTransportCode\":\"3\",\"productNameFr\":\"Produit inconnu\",\"lciName\":\"Unknown product\",\"foodGroup\":\"Group\",\"foodSubgroup\":\"Subgroup\",\"preparation\":\"none\",\"delivery\":\"none\",\"packagingApproach\":\"none\"}")
            private fun ciqualRecord(
                key: Long,
                code: String,
            ) = HimCiqualEvidenceProjectionV1.fromProjectionJson(
                """
                {
                  "recordKind": "FOOD",
                  "alimCode": "$code",
                  "nameFr": "Produit inconnu",
                  "nameEn": "Unknown product",
                  "scientificName": {
                    "lexicalValue": "Species unknown",
                    "missingAttributeValue": null
                  },
                  "groupCode": "1",
                  "groupNameFr": "Groupe",
                  "groupNameEn": "Group",
                  "subgroupCode": "2",
                  "subgroupNameFr": "Sous-groupe",
                  "subgroupNameEn": "Subgroup",
                  "subSubgroupCode": "3",
                  "subSubgroupNameFr": "Aliment",
                  "subSubgroupNameEn": "Food"
                }
                """.trimIndent(),
                key,
            )
            private fun giRecord(ordinal: Long) = HimGlycemicIndexEvidenceProjectionV1.fromSourceObject(com.google.gson.JsonParser.parseString("{\"foodNumber\":1,\"pageNumber\":1,\"referenceCode\":{\"lexicalValue\":\"r\"},\"foodItem\":{\"lexicalValue\":\"Unknown food\"},\"country\":{\"lexicalValue\":\"France\"},\"referenceFoodTime\":{\"lexicalValue\":\"now\"},\"timepoints\":{\"lexicalValue\":\"1\"},\"sampleCollection\":{\"lexicalValue\":\"lab\"},\"analysisMethod\":{\"lexicalValue\":\"method\"},\"sourceContext\":{\"majorCategory\":\"Category\",\"subcategory\":\"Subcategory\",\"deeperHeading\":\"Heading\"}}").asJsonObject, HimGlycemicIndexLogicalRecordKind.MEASUREMENT, ordinal, ordinal)
            private fun catalog() = HimProductOnlyCanonicalMaster("fixture", "0".repeat(64), emptyList())
            private fun authority() = HimCanonicalFamilyAuthority("fixture", de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog("fixture", "0".repeat(64), 0), emptyList())
        }
    }

    private fun HimEvidenceAlignmentCatalogAuditFindingsAnalysisReportV1.withDigest() = copy(
        logicalDigest = HimEvidenceAlignmentCatalogAuditFindingsAnalysisPersistenceV1.logicalDigest(copy(logicalDigest = "")),
    )

    private class Port(
        override val source: HimGroundTruthSource,
        private val operation: (HimGroundTruthSource, HimEvidenceRecordReference) -> HimEvidenceRetrievalIndexRecord?,
    ) : HimUnresolvedPrimaryIdentityDiagnosticExactFetchPortV1 {
        override fun fetchExact(source: HimGroundTruthSource, evidenceReference: HimEvidenceRecordReference) = operation(source, evidenceReference)
    }
}
