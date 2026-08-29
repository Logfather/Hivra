package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidencePositionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationResultV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1Test {
    @Test
    fun contractIdentityAndStateAreFrozen() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POST_VALIDATION_ELIGIBILITY_CONTRACT_V1",
            HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.CONTRACT_ID,
        )
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.VERSION)
        assertEquals(
            "POST_VALIDATION_ELIGIBILITY_CONTEXT_ONLY",
            HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.STATE,
        )
    }

    @Test
    fun validFourRecordBatchProducesTwoPositiveAndTwoNegativeEligibilityDecisions() {
        val result = evaluate(validBatch())
        val output = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.Result.Completed>(result).value

        assertEquals(4, output.decisions.size)
        assertEquals(2, output.counters.eligiblePositive)
        assertEquals(2, output.counters.eligibleNegative)
        assertEquals(0, output.counters.requiresAdjudication)
        assertEquals(0, output.counters.notEligible)
        assertEquals(
            listOf(
                HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_POSITIVE_GOLD_CANDIDATE,
                HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_NEGATIVE_SUPERVISION_CANDIDATE,
                HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_POSITIVE_GOLD_CANDIDATE,
                HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_NEGATIVE_SUPERVISION_CANDIDATE,
            ),
            output.decisions.map { it.state },
        )
        assertTrue(output.decisions.all { it.originalReviewerRef == "reviewer:logfather:v1" })
        assertTrue(output.decisions.all { it.validationBatchLogicalDigest == validBatch().logicalDigest })
    }

    @Test
    fun deterministicSecondCreationProducesIdenticalDecisionsAndDigest() {
        val first = output(validBatch())
        val second = output(validBatch())
        assertEquals(first, second)
        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.decisions.map { it.decisionId }, second.decisions.map { it.decisionId })
    }

    @Test
    fun challengeAndEscalateRequireAdjudication() {
        val challenge = output(batchReplacing(0, record(0, assessment = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.CHALLENGE_ORIGINAL_DECISION, reasonCodes = adjudicationReasons())))
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.REQUIRES_ADJUDICATION,
            challenge.decisions[0].state,
        )

        val escalate = output(batchReplacing(0, record(0, assessment = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ESCALATE_VALIDATION_CONFLICT, reasonCodes = adjudicationReasons())))
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.REQUIRES_ADJUDICATION,
            escalate.decisions[0].state,
        )
    }

    @Test
    fun abstainProducesNotEligibleWithoutPublicationState() {
        val abstain = output(
            batchReplacing(
                0,
                record(
                    0,
                    assessment = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ABSTAIN_VALIDATION_INSUFFICIENT_EVIDENCE,
                    reasonCodes = listOf(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.INSUFFICIENT_DIRECT_EVIDENCE),
                    evidenceReferenceIds = emptyList(),
                ),
            ),
        )
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.NOT_ELIGIBLE,
            abstain.decisions[0].state,
        )
        assertEquals(1, abstain.counters.notEligible)
    }

    @Test
    fun foreignEvidenceFailsClosed() {
        val foreign = validBatch().records[0].copy(
            evidenceReferenceIds = validBatch().records[1].evidenceReferenceIds.take(1),
        )
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.Result.Failed>(
            evaluate(batchReplacing(0, foreign)),
        )
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.FailureReason.FOREIGN_EVIDENCE, failure.reason)
    }

    @Test
    fun duplicateRecordFailsClosed() {
        val batch = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.createBatch(
            inputBinding = inputBinding(),
            validatorReviewerRef = VALIDATOR,
            validationRound = 1,
            validationRevision = 1,
            records = listOf(validBatch().records[0], validBatch().records[0], validBatch().records[2], validBatch().records[3]),
        )
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.Result.Failed>(evaluate(batch))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.FailureReason.DUPLICATE_RECORD, failure.reason)
    }

    @Test
    fun recordOrderMismatchFailsClosed() {
        val baseline = validBatch()
        val batch = baseline.copy(records = baseline.records.reversed())
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.Result.Failed>(evaluate(batch))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.FailureReason.INVALID_RECORD_COUNT_OR_ORDER, failure.reason)
    }

    @Test
    fun invalidValidatorFailsClosed() {
        val broken = validBatch().copy(validatorReviewerRef = "reviewer:logfather:v1")
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.Result.Failed>(evaluate(broken))
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.FailureReason.VALIDATOR_MISMATCH, failure.reason)
    }

    @Test
    fun invalidRecordIdFailsClosed() {
        val baseline = validBatch()
        val broken = baseline.records[0].copy(validationRecordId = "0".repeat(64))
        val malformed = baseline.copy(
            records = baseline.records.toMutableList().also { it[0] = broken },
        )
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.Result.Failed>(
            evaluate(malformed),
        )
        assertEquals(HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.FailureReason.INVALID_RECORD_ID, failure.reason)
    }

    private fun evaluate(batch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1) =
        HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.evaluate(batch)

    private fun output(batch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1) =
        assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.Result.Completed>(evaluate(batch)).value

    private fun validBatch(): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1 =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.createBatch(
            inputBinding = inputBinding(),
            validatorReviewerRef = VALIDATOR,
            validationRound = 1,
            validationRevision = 1,
            records = (0 until 4).map { record(it) },
        )

    private fun batchReplacing(
        index: Int,
        replacement: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.createBatch(
        inputBinding = inputBinding(),
        validatorReviewerRef = VALIDATOR,
        validationRound = 1,
        validationRevision = 1,
        records = validBatch().records.toMutableList().also { it[index] = replacement },
    )

    private fun record(
        index: Int,
        assessment: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1 = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION,
        reasonCodes: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1>? = null,
        evidenceReferenceIds: List<String>? = null,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1 {
        val selection = inputBinding().originalSelections[index]
        val isConfirm = selection.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION
        val evidence = inputBinding().evidenceBindings.filter { it.reviewUnitId == selection.reviewUnitId }
        val selectedEvidence = evidence.filter {
            it.position == if (isConfirm) {
                HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION
            } else {
                HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION
            }
        }.map { it.evidenceReferenceId }.sorted()
        return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.createValidationRecord(
            inputBinding = inputBinding(),
            originalDecision = selection,
            validatorReviewerRef = VALIDATOR,
            validationRound = 1,
            validationRevision = 1,
            assessment = assessment,
            reasonCodes = reasonCodes ?: if (isConfirm) {
                listOf(
                    HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_SUPPORTS_ORIGINAL_DECISION,
                    HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.ORIGINAL_REASON_CODES_SUPPORTED,
                )
            } else {
                listOf(
                    HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_CONTRADICTS_ORIGINAL_DECISION,
                    HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.ORIGINAL_REASON_CODES_SUPPORTED,
                )
            },
            evidenceReferenceIds = evidenceReferenceIds ?: selectedEvidence,
            rationale = "Hermetic validation rationale for unit ${index + 1}",
        )
    }

    private fun adjudicationReasons() = listOf(
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.CONFLICTING_DIRECT_EVIDENCE,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.REQUIRES_ADJUDICATION,
    )

    private fun inputBinding() = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.FROZEN_INPUT_BINDING

    private companion object {
        const val VALIDATOR = "validator:karina-glatschke:v1"
    }
}
