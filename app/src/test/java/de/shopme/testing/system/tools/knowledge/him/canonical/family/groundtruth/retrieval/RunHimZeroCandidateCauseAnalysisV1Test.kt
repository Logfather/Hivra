package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.HimGroundTruthSource
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentAuditClassification
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentCatalogAuditSourceBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordKind
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRecordReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceRetrievalIndexBuildState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticAggregateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticFieldV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticFileBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticInputBindingV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticLanguageV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticLexicalFeaturesV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticMissionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticOccurrenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticRecordV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticReferencePlanV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticResultStateV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticRuntimeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticShardPlanV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticShardResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimUnresolvedPrimaryIdentityDiagnosticSourceReferenceCountV1
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
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RunHimZeroCandidateCauseAnalysisV1Test {
    @Test
    fun selectsOnlyZeroCandidateRecords() {
        val report = completed()
        assertEquals(2_810, report.counters.zeroCandidateRecords)
        assertTrue(report.records.all { it.originalStatus == HimUnresolvedPrimaryIdentityDiagnosticStatusV1.IDENTITY_FIELDS_PRESENT_NO_CANDIDATE })
        assertTrue(report.records.all { it.candidateIdentities.isEmpty() })
    }

    @Test
    fun excludesSingleAndMultipleCandidateRecords() {
        val fixture = Fixture.value()
        val aggregateRecords = fixture.aggregate.shardResults.flatMap { it.records }
        assertTrue(aggregateRecords.any { it.candidateIdentities.size == 1 })
        assertTrue(aggregateRecords.any { it.candidateIdentities.size >= 2 })
        assertEquals(2_810, completed().records.size)
    }

    @Test
    fun everyPrimaryBucketIsReached() {
        val report = completed()
        assertTrue(report.bucketCounters.all { it.count > 0 })
        assertEquals(
            HimZeroCandidateCauseAnalysisContractV1.BUCKET_ORDER,
            report.bucketCounters.map { it.bucket },
        )
    }

    @Test
    fun bucketPrecedenceIsStable() {
        val first = completed()
        val second = completed()
        assertEquals(first.records, second.records)
        assertEquals(first.bucketCounters, second.bucketCounters)
    }

    @Test
    fun flagsAreNonExclusiveAndCanonical() {
        val report = completed()
        assertTrue(report.records.any { it.flags.size > 1 })
        assertTrue(report.records.all { it.flags == it.flags.distinct().sortedBy { flag -> flag.ordinal } })
        assertEquals(HimZeroCandidateCauseAnalysisContractV1.FLAG_ORDER, report.flagCounters.map { it.flag })
    }

    @Test
    fun reorderedShardInputProducesSameReportAndDigest() {
        val fixture = Fixture.value()
        val reordered = fixture.aggregate.copy(shardResults = fixture.aggregate.shardResults.reversed())
        val result = HimZeroCandidateCauseAnalysisRuntimeV1.analyze(
            fixture.request(aggregate = reordered),
        )
        val report = assertIs<HimZeroCandidateCauseAnalysisRuntimeResult.Completed<HimZeroCandidateCauseAnalysisReportV1>>(result).value
        assertEquals(completed(), report)
    }

    @Test
    fun invalidDuplicateReferenceFailsTyped() {
        val fixture = Fixture.value()
        val shard = fixture.aggregate.shardResults.first()
        val duplicate = shard.copy(records = shard.records + shard.records.first())
        val aggregate = fixture.aggregate.copy(shardResults = listOf(duplicate) + fixture.aggregate.shardResults.drop(1))
        val failure = assertIs<HimZeroCandidateCauseAnalysisRuntimeResult.Failed>(
            HimZeroCandidateCauseAnalysisRuntimeV1.analyze(fixture.request(aggregate = aggregate)),
        )
        assertEquals(HimZeroCandidateCauseAnalysisFailureReasonV1.AGGREGATE_VALIDATION_FAILED, failure.reason)
    }

    @Test
    fun manipulatedMissionBindingFailsTyped() {
        val fixture = Fixture.value()
        val unsigned = fixture.inputBinding.copy(
            diagnosticMission = fixture.inputBinding.diagnosticMission.copy(logicalDigest = "f".repeat(64)),
            diagnosticMissionLogicalDigest = "f".repeat(64),
            bindingDigest = "",
        )
        val binding = unsigned.copy(bindingDigest = HimZeroCandidateCauseAnalysisPersistenceV1.bindingDigest(unsigned))
        val failure = assertIs<HimZeroCandidateCauseAnalysisRuntimeResult.Failed>(
            HimZeroCandidateCauseAnalysisRuntimeV1.analyze(fixture.request(inputBinding = binding)),
        )
        assertEquals(HimZeroCandidateCauseAnalysisFailureReasonV1.MISSION_BINDING_MISMATCH, failure.reason)
    }

    @Test
    fun manipulatedAggregateBindingFailsTyped() {
        val fixture = Fixture.value()
        val unsigned = fixture.inputBinding.copy(
            diagnosticAggregate = fixture.inputBinding.diagnosticAggregate.copy(logicalDigest = "f".repeat(64)),
            diagnosticAggregateLogicalDigest = "f".repeat(64),
            bindingDigest = "",
        )
        val binding = unsigned.copy(bindingDigest = HimZeroCandidateCauseAnalysisPersistenceV1.bindingDigest(unsigned))
        val failure = assertIs<HimZeroCandidateCauseAnalysisRuntimeResult.Failed>(
            HimZeroCandidateCauseAnalysisRuntimeV1.analyze(fixture.request(inputBinding = binding)),
        )
        assertEquals(HimZeroCandidateCauseAnalysisFailureReasonV1.AGGREGATE_BINDING_MISMATCH, failure.reason)
    }

    @Test
    fun manipulatedZeroCounterFailsTyped() {
        val fixture = Fixture.value()
        val brokenAggregate = fixture.aggregate.copy(
            counters = fixture.aggregate.counters.copy(zeroCandidateRecords = 0),
            logicalDigest = "",
        ).let { it.copy(logicalDigest = HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.logicalDigest(it)) }
        val failure = assertIs<HimZeroCandidateCauseAnalysisRuntimeResult.Failed>(
            HimZeroCandidateCauseAnalysisRuntimeV1.analyze(fixture.request(aggregate = brokenAggregate)),
        )
        assertEquals(HimZeroCandidateCauseAnalysisFailureReasonV1.AGGREGATE_VALIDATION_FAILED, failure.reason)
    }

    @Test
    fun invalidFieldShapeFailsClosed() {
        val fixture = Fixture.value()
        val first = fixture.aggregate.shardResults.first()
        val original = first.records.first { it.fields.isNotEmpty() }

        val internallyValidForeignField = original.fields.first().copy(
            source = HimGroundTruthSource.CIQUAL,
            recordKind = HimEvidenceRecordKind.CIQUAL_FOOD,
        )

        assertFailsWith<IllegalArgumentException> {
            original.copy(fields = listOf(internallyValidForeignField))
        }
    }

    @Test
    fun recurringPrimaryValuesAreCompleteAndSorted() {
        val groups = completed().recurringPrimaryValueGroups
        assertTrue(groups.isNotEmpty())
        assertTrue(groups == groups.sortedWith(compareByDescending<HimZeroCandidateCauseAnalysisRecurringPrimaryValueGroupV1> { it.references.size }.thenBy { it.key }))
        assertTrue(groups.all { it.references.size >= 2 })
    }

    @Test
    fun jsonReloadEqualsOriginal() {
        val directory = Files.createTempDirectory("him-zero-analysis").toFile()
        try {
            val json = directory.resolve("analysis.v1.json")
            val text = directory.resolve("analysis.v1.txt")
            val report = completed()
            HimZeroCandidateCauseAnalysisPersistenceV1.writeReport(json, text, report)
            assertEquals(report, HimZeroCandidateCauseAnalysisPersistenceV1.readReport(json))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun secondWriteIsByteIdentical() {
        val directory = Files.createTempDirectory("him-zero-analysis").toFile()
        try {
            val json = directory.resolve("analysis.v1.json")
            val text = directory.resolve("analysis.v1.txt")
            val report = completed()
            HimZeroCandidateCauseAnalysisPersistenceV1.writeReport(json, text, report)
            val before = json.readBytes() to text.readBytes()
            HimZeroCandidateCauseAnalysisPersistenceV1.writeReport(json, text, report)
            assertTrue(before.first.contentEquals(json.readBytes()))
            assertTrue(before.second.contentEquals(text.readBytes()))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun divergentExistingBytesAreRejected() {
        val directory = Files.createTempDirectory("him-zero-analysis").toFile()
        try {
            val json = directory.resolve("analysis.v1.json")
            val text = directory.resolve("analysis.v1.txt")
            json.writeText("different")
            assertFailsWith<IllegalArgumentException> {
                HimZeroCandidateCauseAnalysisPersistenceV1.writeReport(json, text, completed())
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun safeDiagnosticsContainNoPathsOrThrowableText() {
        val fixture = Fixture.value()
        val failure = assertIs<HimZeroCandidateCauseAnalysisRuntimeResult.Failed>(
            HimZeroCandidateCauseAnalysisRuntimeV1.analyze(
                fixture.request(inputBinding = fixture.inputBinding.copy(bindingDigest = "f".repeat(64))),
            ),
        )
        assertTrue(!failure.safeContext.contains("/"))
        assertTrue(!failure.safeContext.contains("Exception"))
        assertTrue(!failure.safeContext.contains("Throwable"))
    }

    @Test
    fun runtimeAndRequestHaveNoStoreSearchFetchOrScanMethods() {
        val forbidden = setOf("store", "search", "fetch", "scan")
        val names = (HimZeroCandidateCauseAnalysisRuntimeV1::class.java.declaredMethods +
            HimZeroCandidateCauseAnalysisRuntimeRequestV1::class.java.declaredMethods)
            .map { it.name.lowercase() }
        assertTrue(names.none { name -> forbidden.any { token -> token in name } })
    }

    @Test
    fun scaledFixtureHasAllExpectedZeroCandidates() {
        val fixture = Fixture.value()
        assertEquals(2_983, fixture.aggregate.counters.recordsLoaded)
        assertEquals(2_810, fixture.aggregate.counters.zeroCandidateRecords)
        assertEquals(924, fixture.aggregate.counters.canonicalTargets)
        assertEquals(2_810, completed().records.size)
    }

    @Test
    fun disabledAnalysisIsSkippedWithoutTouchingInputs() {
        assertEquals(
            HimZeroCandidateCauseAnalysisRuntimeResult.Skipped("ANALYSIS_OPT_IN_REQUIRED"),
            HimZeroCandidateCauseAnalysisRuntimeV1.analyze(Fixture.value().request(enabled = false)),
        )
    }

    private fun completed(): HimZeroCandidateCauseAnalysisReportV1 =
        assertIs<HimZeroCandidateCauseAnalysisRuntimeResult.Completed<HimZeroCandidateCauseAnalysisReportV1>>(
            HimZeroCandidateCauseAnalysisRuntimeV1.analyze(Fixture.value().request()),
        ).value

    private class Fixture private constructor(
        val mission: HimUnresolvedPrimaryIdentityDiagnosticMissionV1,
        val aggregate: HimUnresolvedPrimaryIdentityDiagnosticAggregateV1,
        val inputBinding: HimZeroCandidateCauseAnalysisInputBindingV1,
    ) {
        fun request(
            enabled: Boolean = true,
            aggregate: HimUnresolvedPrimaryIdentityDiagnosticAggregateV1 = this.aggregate,
            inputBinding: HimZeroCandidateCauseAnalysisInputBindingV1 = this.inputBinding,
        ) = HimZeroCandidateCauseAnalysisRuntimeRequestV1(enabled, mission, aggregate, inputBinding)

        companion object {
            private const val TOTAL = 2_983
            private const val ZERO = 2_810
            private const val CANONICALS = 924

            fun value(): Fixture {
                val source = HimGroundTruthSource.OPEN_FOOD_FACTS
                val records = (0 until TOTAL).map { index ->
                    val entityId = "c${(index % CANONICALS).toString().padStart(5, '0')}"
                    val reference = HimEvidenceRecordReference.offProduct(index + 1L, "p$index").value
                    val status = when {
                        index < ZERO -> HimUnresolvedPrimaryIdentityDiagnosticStatusV1.IDENTITY_FIELDS_PRESENT_NO_CANDIDATE
                        index < 2_900 -> HimUnresolvedPrimaryIdentityDiagnosticStatusV1.SINGLE_CANDIDATE_UNRESOLVED
                        else -> HimUnresolvedPrimaryIdentityDiagnosticStatusV1.MULTIPLE_CANDIDATES_UNRESOLVED
                    }
                    val candidates = when (status) {
                        HimUnresolvedPrimaryIdentityDiagnosticStatusV1.IDENTITY_FIELDS_PRESENT_NO_CANDIDATE -> emptyList()
                        HimUnresolvedPrimaryIdentityDiagnosticStatusV1.SINGLE_CANDIDATE_UNRESOLVED -> listOf("candidate-$index")
                        HimUnresolvedPrimaryIdentityDiagnosticStatusV1.MULTIPLE_CANDIDATES_UNRESOLVED -> listOf("candidate-$index", "other-$index")
                        else -> emptyList()
                    }
                    val occurrence = HimUnresolvedPrimaryIdentityDiagnosticOccurrenceV1(
                        findingOccurrenceId = HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.sha256("occurrence-$index"),
                        auditShardId = "shard-000001",
                        entityId = entityId,
                        canonicalName = "Canonical $entityId",
                        normalizedName = "canonical-$entityId",
                        source = source,
                        evidenceReference = reference,
                        originalAuditClassification = HimEvidenceAlignmentAuditClassification.UNRESOLVED_PRIMARY_IDENTITY,
                        originalRecordKind = HimEvidenceRecordKind.OFF_PRODUCT,
                        candidateIdentities = candidates,
                        originalExtractorResolution = "MISSING",
                        originalIdentityFieldUsed = emptyList(),
                        originalExtractionPath = null,
                        originalReasonCode = null,
                    )
                    HimUnresolvedPrimaryIdentityDiagnosticRecordV1(
                        source = source,
                        evidenceReference = reference,
                        recordKind = HimEvidenceRecordKind.OFF_PRODUCT,
                        ownerShardId = "shard-000001",
                        occurrences = listOf(occurrence),
                        canonicalEntityIds = listOf(entityId),
                        fields = if (index >= ZERO) listOf(field(index)) else fieldsForBucket(index),
                        candidateIdentities = candidates,
                        originalExtractorResolutions = listOf("MISSING"),
                        originalIdentityFieldsUsed = emptyList(),
                        originalExtractionPaths = emptyList(),
                        originalReasonCodes = emptyList(),
                        status = status,
                    )
                }.sortedWith(recordComparator)
                val plans = records.map { record ->
                    HimUnresolvedPrimaryIdentityDiagnosticReferencePlanV1(
                        source = source,
                        evidenceReference = record.evidenceReference,
                        ownerShardId = "shard-000001",
                        occurrences = record.occurrences,
                    )
                }
                val sourceCounts = HimUnresolvedPrimaryIdentityDiagnosticContractV1.SOURCE_ORDER.map {
                    HimUnresolvedPrimaryIdentityDiagnosticSourceReferenceCountV1(it, if (it == source) TOTAL else 0)
                }
                val missionBindingUnsigned = HimUnresolvedPrimaryIdentityDiagnosticInputBindingV1(
                    analysis = fileBinding("analysis", "1".repeat(64)),
                    enrichmentMission = fileBinding("enrichment-mission", "2".repeat(64)),
                    enrichmentAggregate = fileBinding("enrichment-aggregate", "3".repeat(64)),
                    auditHead = "a".repeat(40),
                    enrichmentImplementationHead = "b".repeat(40),
                    analysisImplementationHead = "c".repeat(40),
                    expectedOccurrenceCount = TOTAL,
                    expectedReferenceCount = TOTAL,
                    expectedCanonicalCount = CANONICALS,
                    sourceReferenceCounts = sourceCounts,
                    bindingDigest = "",
                )
                val missionBinding = missionBindingUnsigned.copy(
                    bindingDigest = HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.bindingDigest(missionBindingUnsigned),
                )
                val unsignedMission = HimUnresolvedPrimaryIdentityDiagnosticMissionV1(
                    contractId = HimUnresolvedPrimaryIdentityDiagnosticContractV1.VERSION,
                    inputBinding = missionBinding,
                    auditHead = missionBinding.auditHead,
                    enrichmentImplementationHead = missionBinding.enrichmentImplementationHead,
                    analysisImplementationHead = missionBinding.analysisImplementationHead,
                    diagnosticImplementationHead = "d".repeat(40),
                    analysisBinding = missionBinding.analysis,
                    enrichmentMissionBinding = missionBinding.enrichmentMission,
                    enrichmentAggregateBinding = missionBinding.enrichmentAggregate,
                    sourceBindings = HimEvidenceAlignmentCatalogAuditContractV1.SOURCE_ORDER.map { sourceBinding(it) },
                    expectedOccurrenceCount = TOTAL,
                    expectedReferenceCount = TOTAL,
                    expectedCanonicalCount = CANONICALS,
                    reusedReferenceCount = 0,
                    reusedOccurrenceCount = 0,
                    missingIdentityCount = TOTAL,
                    zeroCandidateCount = ZERO,
                    singleCandidateCount = 90,
                    multipleCandidateCount = TOTAL - ZERO - 90,
                    referencePlans = plans,
                    shards = HimUnresolvedPrimaryIdentityDiagnosticContractV1.SHARD_IDS.map { shardId ->
                        HimUnresolvedPrimaryIdentityDiagnosticShardPlanV1(
                            shardId,
                            if (shardId == "shard-000001") plans.map { it.key }.sorted() else emptyList(),
                        )
                    },
                    logicalDigest = "",
                )
                val mission = unsignedMission.copy(logicalDigest = HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.logicalDigest(unsignedMission))
                val shardResults = mission.shards.map { plan ->
                    val shardRecords = records.filter { it.ownerShardId == plan.shardId }
                    val unsigned = HimUnresolvedPrimaryIdentityDiagnosticShardResultV1(
                        missionDigest = mission.logicalDigest,
                        shardId = plan.shardId,
                        state = HimUnresolvedPrimaryIdentityDiagnosticResultStateV1.COMPLETE,
                        records = shardRecords,
                        counters = HimUnresolvedPrimaryIdentityDiagnosticRuntimeV1.deriveCounters(shardRecords),
                        logicalDigest = "",
                    )
                    unsigned.copy(logicalDigest = HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.logicalDigest(unsigned))
                }
                val unsignedAggregate = HimUnresolvedPrimaryIdentityDiagnosticAggregateV1(
                    missionDigest = mission.logicalDigest,
                    state = HimUnresolvedPrimaryIdentityDiagnosticResultStateV1.COMPLETE,
                    shardResults = shardResults,
                    counters = HimUnresolvedPrimaryIdentityDiagnosticRuntimeV1.deriveCounters(records),
                    logicalDigest = "",
                )
                val aggregate = unsignedAggregate.copy(logicalDigest = HimUnresolvedPrimaryIdentityDiagnosticPersistenceV1.logicalDigest(unsignedAggregate))
                val inputUnsigned = HimZeroCandidateCauseAnalysisInputBindingV1(
                    diagnosticMission = analysisFileBinding("diagnostic-mission", mission.logicalDigest),
                    diagnosticAggregate = analysisFileBinding("diagnostic-aggregate", aggregate.logicalDigest),
                    diagnosticMissionLogicalDigest = mission.logicalDigest,
                    diagnosticAggregateLogicalDigest = aggregate.logicalDigest,
                    analysisImplementationHead = "e".repeat(40),
                    bindingDigest = "",
                )
                val input = inputUnsigned.copy(bindingDigest = HimZeroCandidateCauseAnalysisPersistenceV1.bindingDigest(inputUnsigned))
                mission.validate()
                aggregate.validateAgainst(mission)
                return Fixture(mission, aggregate, input)
            }

            private fun fieldsForBucket(index: Int): List<HimUnresolvedPrimaryIdentityDiagnosticFieldV1> = when (index % 7) {
                0 -> emptyList()
                1 -> listOf(field(index, "taxonomy.category", "category", HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.CATEGORY_CONTEXT))
                2 -> listOf(field(index, "identity.productName", "brand product", containsBrand = true))
                3 -> listOf(field(index, "identity.productNameEnglish", "localized product"))
                4 -> listOf(field(index, "identity.productName", "code$index", containsDigits = true))
                5 -> listOf(field(index, "identity.productName", "shared-primary"))
                else -> listOf(field(index, "identity.productType", "other", HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.OTHER_IDENTITY_CONTEXT))
            }

            private fun field(
                index: Int,
                path: String = "identity.productName",
                value: String = "product-$index",
                role: HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1 = HimUnresolvedPrimaryIdentityDiagnosticFieldRoleV1.PRIMARY_IDENTITY_CANDIDATE,
                containsBrand: Boolean = false,
                containsDigits: Boolean = false,
            ) = HimUnresolvedPrimaryIdentityDiagnosticFieldV1(
                fieldPath = path,
                originalLexicalValue = value,
                trimmedValue = value.trim(),
                language = if (path.endsWith("English")) HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.EN else HimUnresolvedPrimaryIdentityDiagnosticLanguageV1.UNSPECIFIED,
                role = role,
                source = HimGroundTruthSource.OPEN_FOOD_FACTS,
                recordKind = HimEvidenceRecordKind.OFF_PRODUCT,
                lexicalFeatures = HimUnresolvedPrimaryIdentityDiagnosticLexicalFeaturesV1(
                    empty = value.isEmpty(),
                    characterLength = value.length,
                    tokenCount = value.split(' ').size,
                    distinctTokenCount = value.split(' ').distinct().size,
                    containsDigits = containsDigits,
                    containsParentheses = false,
                    containsHyphen = value.contains('-'),
                    containsComma = false,
                    containsSlash = false,
                    containsBrandContext = containsBrand,
                ),
            )

            private fun analysisFileBinding(
                name: String,
                logical: String,
            ) = HimZeroCandidateCauseAnalysisFileBindingV1(
                relativePath = "fixture/$name.json",
                byteSize = 1,
                sha256 = "0".repeat(64),
                logicalDigest = logical,
            )

            private fun fileBinding(name: String, logical: String) = HimUnresolvedPrimaryIdentityDiagnosticFileBindingV1(
                relativePath = "fixture/$name.json",
                byteSize = 1,
                sha256 = "0".repeat(64),
                logicalDigest = logical,
            )

            private fun sourceBinding(source: HimGroundTruthSource) = HimEvidenceAlignmentCatalogAuditSourceBindingV1(
                source = source,
                indexRelativePath = "fixture/${source.name.lowercase()}.sqlite",
                indexByteSize = 1,
                sqliteFileSha256 = "1".repeat(64),
                sourceArtifactPath = source.artifactPath,
                sourceArtifactSha256 = "2".repeat(64),
                schemaVersion = "fixture-schema-v1",
                indexBuildPolicyVersion = "fixture-index-policy-v1",
                evidenceProjectionPolicyVersion = "fixture-projection-policy-v1",
                logicalContentSha256 = "3".repeat(64),
                buildState = HimEvidenceRetrievalIndexBuildState.VALIDATED,
            )

            private val recordComparator = compareBy<HimUnresolvedPrimaryIdentityDiagnosticRecordV1>(
                { HimUnresolvedPrimaryIdentityDiagnosticContractV1.SOURCE_ORDER.indexOf(it.source) },
                { it.evidenceReference },
            )
        }
    }
}
