package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewEvidencePositionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1Test {
    @Test
    fun contractIdentityAndStateAreFrozen() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POST_ELIGIBILITY_PUBLICATION_READINESS_CONTRACT_V1",
            HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.CONTRACT_ID,
        )
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.VERSION)
        assertEquals(
            "POST_ELIGIBILITY_PUBLICATION_READINESS_CONTEXT_ONLY",
            HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.STATE,
        )
    }

    @Test
    fun validFourDecisionBatchProducesTwoPositiveAndTwoNegativeReadinessCandidates() {
        val output = output()

        assertEquals(4, output.decisions.size)
        assertEquals(2, output.counters.positiveReadinessCount)
        assertEquals(2, output.counters.negativeReadinessCount)
        assertEquals(0, output.counters.requiresAdjudicationCount)
        assertEquals(0, output.counters.notReadyCount)
        assertEquals(
            listOf(
                HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE,
                HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE,
                HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE,
                HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE,
            ),
            output.decisions.map { it.state },
        )
    }

    @Test
    fun deterministicSecondCreationProducesIdenticalOutput() {
        val first = output()
        val second = output()
        assertEquals(first, second)
        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(first.decisions.map { it.readinessDecisionId }, second.decisions.map { it.readinessDecisionId })
    }

    @Test
    fun positiveEligibilityMapsOnlyToPositiveReadiness() {
        val positive = output().decisions.filter {
            it.eligibilityState == HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_POSITIVE_GOLD_CANDIDATE
        }
        assertTrue(positive.isNotEmpty())
        assertTrue(positive.all {
            it.state == HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE
        })
    }

    @Test
    fun negativeEligibilityMapsOnlyToNegativeReadiness() {
        val negative = output().decisions.filter {
            it.eligibilityState == HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_NEGATIVE_SUPERVISION_CANDIDATE
        }
        assertTrue(negative.isNotEmpty())
        assertTrue(negative.all {
            it.state == HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE
        })
    }

    @Test
    fun requiresAdjudicationRemainsNonPublicationReady() {
        val result = evaluate(eligibility(validationBatchReplacing(0, record(0, HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.CHALLENGE_ORIGINAL_DECISION, adjudicationReasons()))))
        val batch = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Result.Completed>(result).value
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.REQUIRES_ADJUDICATION,
            batch.decisions[0].state,
        )
    }

    @Test
    fun notEligibleRemainsNonPublicationReady() {
        val abstained = record(
            0,
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ABSTAIN_VALIDATION_INSUFFICIENT_EVIDENCE,
            listOf(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.INSUFFICIENT_DIRECT_EVIDENCE),
            emptyList(),
        )
        val result = evaluate(eligibility(validationBatchReplacing(0, abstained)))
        val batch = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Result.Completed>(result).value
        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.NOT_PUBLICATION_READY,
            batch.decisions[0].state,
        )
    }

    @Test
    fun duplicateEligibilityDecisionFailsClosed() {
        val baseline = eligibilityOutput()
        val duplicate = baseline.decisions[0].copy(decisionId = baseline.decisions[1].decisionId)
        val broken = rebindEligibility(baseline, baseline.decisions.toMutableList().also { it[0] = duplicate })
        assertFailure(broken, HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.FailureReason.DUPLICATE_ELIGIBILITY_DECISION)
    }

    @Test
    fun orderMismatchFailsClosed() {
        val baseline = eligibilityOutput()
        val broken = rebindEligibility(baseline, baseline.decisions.reversed())
        assertFailure(broken, HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.FailureReason.INVALID_ELIGIBILITY_DECISION_COUNT_OR_ORDER)
    }

    @Test
    fun staleDecisionIdFailsClosed() {
        val baseline = eligibilityOutput()
        val stale = baseline.decisions[0].copy(decisionId = "stale")
        val broken = rebindEligibility(baseline, baseline.decisions.toMutableList().also { it[0] = stale })
        assertFailure(broken, HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.FailureReason.INVALID_ELIGIBILITY_DECISION_ID)
    }

    @Test
    fun brokenProvenanceFailsClosed() {
        val baseline = eligibilityOutput()
        val brokenDecision = baseline.decisions[0].copy(canonicalEntityId = "")
        val broken = rebindEligibility(baseline, baseline.decisions.toMutableList().also { it[0] = brokenDecision })
        assertFailure(broken, HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.FailureReason.BROKEN_PROVENANCE)
    }

    @Test
    fun wrongUpstreamDigestFailsClosed() {
        val baseline = eligibilityOutput()
        val broken = baseline.copy(
            validationBatchBindingDigest = "0".repeat(64),
            logicalDigest = "",
        ).let {
            it.copy(
                logicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.batchLogicalDigest(it),
            )
        }
        assertFailure(broken, HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.FailureReason.BROKEN_VALIDATION_RECORD_BINDING)
    }

    @Test
    fun noReadinessStateClaimsApprovalPublicationReleaseOrTraining() {
        assertTrue(
            HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.values()
                .none { it.name in setOf("APPROVED", "PUBLISHED", "RELEASED", "TRAINING_READY") },
        )
    }

    private fun evaluate(
        batch: HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.Batch,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.evaluate(batch)

    private fun output() = completed(evaluate(eligibility(validationBatch())))

    private fun eligibilityOutput() = eligibility(validationBatch())

    private fun completed(
        result: HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Result,
    ) = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Result.Completed>(result).value

    private fun assertFailure(
        batch: HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.Batch,
        reason: HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.FailureReason,
    ) {
        val failure = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Result.Failed>(evaluate(batch))
        assertEquals(reason, failure.reason)
    }

    private fun rebindEligibility(
        batch: HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.Batch,
        decisions: List<HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.Decision>,
    ) = batch.copy(
        decisions = decisions,
        counters = HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.Counters.from(decisions),
        logicalDigest = "",
    ).let {
        it.copy(
            logicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.batchLogicalDigest(it),
        )
    }

    private fun eligibility(
        batch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1,
    ) = assertIs<HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.Result.Completed>(
        HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.evaluate(batch),
    ).value

    private fun validationBatch() = validationBatchReplacing()

    private fun validationBatchReplacing(
        index: Int? = null,
        replacement: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1? = null,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.createBatch(
        inputBinding = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.FROZEN_INPUT_BINDING,
        validatorReviewerRef = VALIDATOR,
        validationRound = 1,
        validationRevision = 1,
        records = (0 until 4).map { position ->
            if (position == index && replacement != null) replacement else record(position)
        },
    )

    private fun record(
        index: Int,
        assessment: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1 = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION,
        reasonCodes: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1>? = null,
        evidenceReferenceIds: List<String>? = null,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1 {
        val binding = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.FROZEN_INPUT_BINDING
        val selection = binding.originalSelections[index]
        val isConfirm = selection.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION
        val evidence = binding.evidenceBindings.filter { it.reviewUnitId == selection.reviewUnitId }
        val selectedEvidence = evidence.filter {
            it.position == if (isConfirm) {
                HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION
            } else {
                HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION
            }
        }.map { it.evidenceReferenceId }.sorted()
        return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.createValidationRecord(
            inputBinding = binding,
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
            rationale = "Hermetic readiness rationale for unit ${index + 1}",
        )
    }

    private fun adjudicationReasons() = listOf(
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.CONFLICTING_DIRECT_EVIDENCE,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.REQUIRES_ADJUDICATION,
    )

    private companion object {
        const val VALIDATOR = "validator:karina-glatschke:v1"
    }
}
