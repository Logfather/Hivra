package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.FailureReason
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.PersistenceStatus
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Result
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.SkipReason
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1Test {
    @Test
    fun validNegativeCandidateBatchMaterializesDurableBatch() = withTempDirectory { root ->
        val result = execute(root)
        val completed = completed(result)

        assertEquals(PersistenceStatus.CREATED, completed.status)
        assertEquals(2, completed.batch.records.size)
        assertEquals(2, completed.batch.counters.totalNegativeSupervisionRecords)
        assertTrue(File(root, "batch-fixture-v1/${HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.DURABLE_FILE_NAME}").isFile)
    }

    @Test
    fun onlyNegativeCandidateStatesArePersisted() = withTempDirectory { root ->
        val records = completed(execute(root)).batch.records
        assertTrue(records.all { it.candidateState == CandidateState.NEGATIVE_SUPERVISION_CANDIDATE })
        assertEquals(listOf("unit-2", "unit-4"), records.map { it.reviewUnitId })
    }

    @Test
    fun nonNegativeCandidateDecisionsAreExcluded() = withTempDirectory { root ->
        val source = Fixture.candidate()
        val persisted = completed(execute(root, source)).batch

        assertEquals(
            source.decisions.filter { it.state != CandidateState.NEGATIVE_SUPERVISION_CANDIDATE }
                .map { it.reviewUnitId },
            listOf("unit-1", "unit-3"),
        )
        assertTrue(persisted.records.none { it.reviewUnitId in setOf("unit-1", "unit-3") })
    }

    @Test
    fun candidateOrderIsPreserved() = withTempDirectory { root ->
        val sourceOrder = Fixture.candidate().decisions
            .filter { it.state == CandidateState.NEGATIVE_SUPERVISION_CANDIDATE }
            .map { it.reviewUnitId }
        assertEquals(sourceOrder, completed(execute(root)).batch.records.map { it.reviewUnitId })
    }

    @Test
    fun selectedEvidenceIsPreservedExactly() = withTempDirectory { root ->
        val source = Fixture.candidate()
        val expected = source.decisions.filter { it.state == CandidateState.NEGATIVE_SUPERVISION_CANDIDATE }
            .map { it.evidenceReferenceIds }
        assertEquals(expected, completed(execute(root, source)).batch.records.map { it.evidenceReferenceIds })
    }

    @Test
    fun evidenceOwnershipRemainsBoundToItsRecord() = withTempDirectory { root ->
        val records = completed(execute(root)).batch.records
        assertEquals(2, records.flatMap { it.evidenceReferenceIds }.distinct().size)
        assertTrue(records.all { it.evidenceReferenceIds.size == 1 })
    }

    @Test
    fun provenanceIsPreserved() = withTempDirectory { root ->
        val records = completed(execute(root)).batch.records
        assertTrue(records.all { it.originalReviewerRef == "reviewer-fixture-v1" })
        assertTrue(records.all { it.validatorReviewerRef == "validator-fixture-v1" })
        assertTrue(records.all { it.validationRound == 1 && it.validationRevision == 1 })
        assertTrue(records.all { it.validationBatchId == "validation-batch-fixture-v1" })
        assertTrue(records.all { it.eligibilityBatchId == "eligibility-batch-fixture-v1" })
    }

    @Test
    fun recordIdsAreDeterministic() = withTempDirectory { firstRoot ->
        withTempDirectory { secondRoot ->
            assertEquals(
                completed(execute(firstRoot)).batch.records.map { it.recordId },
                completed(execute(secondRoot)).batch.records.map { it.recordId },
            )
        }
    }

    @Test
    fun batchIdentityUsesExplicitStableBatchId() = withTempDirectory { root ->
        val batch = completed(execute(root, batchId = "stable-batch-v1")).batch
        assertEquals("stable-batch-v1", batch.durableBatchId)
    }

    @Test
    fun logicalDigestIsRecomputedFromSemanticBatch() = withTempDirectory { root ->
        val batch = completed(execute(root)).batch
        assertEquals(
            batch.logicalDigest,
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.batchLogicalDigest(
                batch.copy(logicalDigest = ""),
            ),
        )
    }

    @Test
    fun serializedBytesAreDeterministic() = withTempDirectory { firstRoot ->
        withTempDirectory { secondRoot ->
            val first = completed(execute(firstRoot)).batch
            val second = completed(execute(secondRoot)).batch
            assertContentEquals(
                HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.serializeBatch(first),
                HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.serializeBatch(second),
            )
        }
    }

    @Test
    fun reloadEqualsMaterializedBatch() = withTempDirectory { root ->
        val result = completed(execute(root))
        val file = File(root, result.relativePath)
        assertEquals(result.batch, HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.readBatch(file))
    }

    @Test
    fun firstPersistenceReturnsCreated() = withTempDirectory { root ->
        assertEquals(PersistenceStatus.CREATED, completed(execute(root)).status)
    }

    @Test
    fun identicalSecondPersistenceReturnsAlreadyPresentIdentical() = withTempDirectory { root ->
        execute(root)
        assertEquals(PersistenceStatus.ALREADY_PRESENT_IDENTICAL, completed(execute(root)).status)
    }

    @Test
    fun identicalSecondPersistenceDoesNotChangeBytes() = withTempDirectory { root ->
        val first = completed(execute(root))
        val file = File(root, first.relativePath)
        val before = file.readBytes()
        completed(execute(root))
        assertContentEquals(before, file.readBytes())
    }

    @Test
    fun conflictingExistingArtifactFailsClosed() = withTempDirectory { root ->
        execute(root)
        val failure = failed(execute(root, Fixture.withEvidence("different-evidence")))
        assertEquals(FailureReason.EXISTING_ARTIFACT_CONFLICT, failure.reason)
    }

    @Test
    fun malformedJsonFailsClosed() = withTempDirectory { root ->
        val file = File(root, "malformed/${HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.DURABLE_FILE_NAME}")
        file.parentFile.mkdirs()
        file.writeText("not-json\n")
        assertFailsWith<IllegalArgumentException> {
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.readBatch(file)
        }
    }

    @Test
    fun wrongContractFailsClosedOnReload() = withTempDirectory { root ->
        val batch = completed(execute(root)).batch
        val file = File(root, "batch-fixture-v1/${HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.DURABLE_FILE_NAME}")
        val tampered = String(HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.serializeBatch(batch))
            .replace(HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.CONTRACT_ID, "wrong-contract")
        file.writeText(tampered)
        assertFailsWith<IllegalArgumentException> {
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.readBatch(file)
        }
    }

    @Test
    fun wrongVersionFailsClosedOnReload() = withTempDirectory { root ->
        val batch = completed(execute(root)).batch
        val file = File(root, "batch-fixture-v1/${HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.DURABLE_FILE_NAME}")
        val tampered = String(HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.serializeBatch(batch))
            .replace("\"version\":\"1\"", "\"version\":\"2\"")
        file.writeText(tampered)
        assertFailsWith<IllegalArgumentException> {
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.readBatch(file)
        }
    }

    @Test
    fun tamperedRecordIdFailsClosedOnReload() = withTempDirectory { root ->
        val batch = completed(execute(root)).batch
        val file = File(root, "batch-fixture-v1/${HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.DURABLE_FILE_NAME}")
        val tampered = String(HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.serializeBatch(batch))
            .replaceFirst(batch.records.first().recordId, "0".repeat(64))
        file.writeText(tampered)
        assertFailsWith<IllegalArgumentException> {
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.readBatch(file)
        }
    }

    @Test
    fun tamperedLogicalDigestFailsClosedOnReload() = withTempDirectory { root ->
        val batch = completed(execute(root)).batch
        val file = File(root, "batch-fixture-v1/${HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.DURABLE_FILE_NAME}")
        val tampered = String(HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.serializeBatch(batch))
            .replaceFirst(batch.logicalDigest, "0".repeat(64))
        file.writeText(tampered)
        assertFailsWith<IllegalArgumentException> {
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.readBatch(file)
        }
    }

    @Test
    fun brokenProvenanceFailsClosedBeforeWrite() = withTempDirectory { root ->
        val broken = Fixture.withDecision(1) { it.copy(originalReviewerRef = "") }
        val failure = failed(execute(root, broken))
        assertEquals(FailureReason.INVALID_CANDIDATE_BATCH, failure.reason)
    }

    @Test
    fun duplicateEvidenceFailsClosedBeforeWrite() = withTempDirectory { root ->
        val broken = Fixture.withDecision(1) { it.copy(evidenceReferenceIds = listOf("same", "same")) }
        val failure = failed(execute(root, broken))
        assertEquals(FailureReason.INVALID_CANDIDATE_BATCH, failure.reason)
    }

    @Test
    fun missingEvidenceFailsClosedBeforeWrite() = withTempDirectory { root ->
        val broken = Fixture.withDecision(1) { it.copy(evidenceReferenceIds = emptyList()) }
        val failure = failed(execute(root, broken))
        assertEquals(FailureReason.INVALID_CANDIDATE_BATCH, failure.reason)
    }

    @Test
    fun duplicateReviewUnitFailsClosedBeforeWrite() = withTempDirectory { root ->
        val source = Fixture.candidate()
        val broken = Fixture.rebind(source, source.decisions.mapIndexed { index, decision ->
            if (index == 3) decision.copy(reviewUnitId = source.decisions[2].reviewUnitId) else decision
        })
        val failure = failed(execute(root, broken))
        assertEquals(FailureReason.INVALID_CANDIDATE_BATCH, failure.reason)
    }

    @Test
    fun invalidCandidateBatchFailsClosedBeforeWrite() = withTempDirectory { root ->
        val broken = Fixture.candidate().copy(
            contractId = "wrong-contract",
            logicalDigest = "0".repeat(64),
        )
        val failure = failed(execute(root, broken))
        assertEquals(FailureReason.INVALID_CANDIDATE_BATCH, failure.reason)
    }

    @Test
    fun routeStateMismatchFailsClosedBeforeWrite() = withTempDirectory { root ->
        val broken = Fixture.withDecision(1) {
            it.copy(
                downstreamRoute = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_POSITIVE_GOLD_CANDIDATE,
            )
        }
        val failure = failed(execute(root, broken))
        assertEquals(FailureReason.INVALID_CANDIDATE_BATCH, failure.reason)
    }

    @Test
    fun zeroNegativeBatchIsNotApplicableAndDoesNotPersist() = withTempDirectory { root ->
        val source = Fixture.candidate(
            states = List(4) { CandidateState.NOT_APPLICABLE_NON_NEGATIVE },
        )
        val result = assertIs<Result.Skipped>(execute(root, source))
        assertEquals(SkipReason.NO_NEGATIVE_SUPERVISION_CANDIDATES, result.reason)
        assertFalse(File(root, "batch-fixture-v1").exists())
    }

    @Test
    fun persistedBatchHasNoTrainingTypesOrEffects() = withTempDirectory { root ->
        val batch = completed(execute(root)).batch
        val json = String(HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.serializeBatch(batch))
        assertFalse(json.contains("HimTraining"))
        assertFalse(json.contains("TrainingExample"))
        assertEquals(2, batch.records.size)
    }

    private fun execute(
        root: File,
        candidate: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.Batch = Fixture.candidate(),
        batchId: String = "batch-fixture-v1",
    ): Result = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.execute(
        HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionPersistenceV1.Request(
            batchId = batchId,
            candidateBatch = candidate,
            durableRoot = root,
        ),
    )

    private fun completed(result: Result): Result.Completed = assertIs<Result.Completed>(result)

    private fun failed(result: Result): Result.Failed = assertIs<Result.Failed>(result)

    private fun withTempDirectory(block: (File) -> Unit) {
        val directory = Files.createTempDirectory("him-negative-supervision-persistence-test-").toFile()
        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }

    private object Fixture {
        private val INPUT_DIGEST = "b".repeat(64)
        private val READINESS_DIGEST = "c".repeat(64)
        private val VALIDATION_BINDING_DIGEST = "d".repeat(64)
        private val VALIDATION_LOGICAL_DIGEST = "e".repeat(64)

        fun candidate(
            states: List<CandidateState> = listOf(
                CandidateState.NOT_APPLICABLE_NON_NEGATIVE,
                CandidateState.NEGATIVE_SUPERVISION_CANDIDATE,
                CandidateState.NOT_APPLICABLE_NON_NEGATIVE,
                CandidateState.NEGATIVE_SUPERVISION_CANDIDATE,
            ),
        ): HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.Batch {
            val reviewUnits = listOf("unit-1", "unit-2", "unit-3", "unit-4")
            val decisions = states.mapIndexed { index, state -> decision(index, state, reviewUnits[index]) }
            val unsigned = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.Batch(
                contractId = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CONTRACT_ID,
                version = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.VERSION,
                state = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.STATE,
                readinessBatchId = "readiness-batch-fixture-v1",
                readinessInputBindingDigest = INPUT_DIGEST,
                readinessBatchLogicalDigest = READINESS_DIGEST,
                decisions = decisions,
                counters = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.Counters.from(decisions),
                logicalDigest = "",
            )
            return unsigned.copy(
                logicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.batchLogicalDigest(unsigned),
            )
        }

        fun withEvidence(value: String) = withDecision(1) {
            it.copy(evidenceReferenceIds = listOf(value))
        }

        fun withDecision(
            index: Int,
            transform: (HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.Decision) -> HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.Decision,
        ) = rebind(candidate(), candidate().decisions.mapIndexed { current, decision ->
            if (current == index) transform(decision) else decision
        })

        fun rebind(
            batch: HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.Batch,
            decisions: List<HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.Decision>,
        ) = batch.copy(
            decisions = decisions,
            counters = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.Counters.from(decisions),
            logicalDigest = "",
        ).let {
            it.copy(
                logicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.batchLogicalDigest(it),
            )
        }

        private fun decision(index: Int, state: CandidateState, reviewUnitId: String) =
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.Decision(
                negativeCandidateId = if (state == CandidateState.NEGATIVE_SUPERVISION_CANDIDATE) "%064x".format(100 + index) else null,
                readinessDecisionId = "%064x".format(1 + index),
                readinessState = if (state == CandidateState.NEGATIVE_SUPERVISION_CANDIDATE) {
                    ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE
                } else {
                    ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE
                },
                eligibilityBatchId = "eligibility-batch-fixture-v1",
                eligibilityInputBindingDigest = INPUT_DIGEST,
                eligibilityDecisionId = "%064x".format(11 + index),
                eligibilityState = if (state == CandidateState.NEGATIVE_SUPERVISION_CANDIDATE) {
                    HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_NEGATIVE_SUPERVISION_CANDIDATE
                } else {
                    HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_POSITIVE_GOLD_CANDIDATE
                },
                validationBatchId = "validation-batch-fixture-v1",
                validationBatchBindingDigest = VALIDATION_BINDING_DIGEST,
                validationBatchLogicalDigest = VALIDATION_LOGICAL_DIGEST,
                validationRecordId = "%064x".format(21 + index),
                reviewUnitId = reviewUnitId,
                stableEntryId = "stable-entry-$index",
                canonicalEntityId = if (index % 2 == 0) "canonical-a" else "canonical-b",
                originalReviewerRef = "reviewer-fixture-v1",
                validatorReviewerRef = "validator-fixture-v1",
                validationRound = 1,
                validationRevision = 1,
                originalDecision = if (state == CandidateState.NEGATIVE_SUPERVISION_CANDIDATE) {
                    HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION
                } else {
                    HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION
                },
                assessment = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION,
                validationReasonCodes = listOf(
                    if (state == CandidateState.NEGATIVE_SUPERVISION_CANDIDATE) {
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_CONTRADICTS_ORIGINAL_DECISION
                    } else {
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_SUPPORTS_ORIGINAL_DECISION
                    },
                ),
                evidenceReferenceIds = listOf("evidence-fixture-$index"),
                downstreamRoute = if (state == CandidateState.NEGATIVE_SUPERVISION_CANDIDATE) {
                    HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE
                } else {
                    HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_POSITIVE_GOLD_CANDIDATE
                },
                state = state,
            )
    }
}
