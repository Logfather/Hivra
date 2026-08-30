package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CandidateState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.FailureReason
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.Result
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1Test {
    @Test
    fun contractIdentityAndContextOnlyStateAreFrozen() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_NEGATIVE_SUPERVISION_CANDIDATE_CONTRACT_V1",
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.CONTRACT_ID,
        )
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.VERSION)
        assertEquals(
            "NEGATIVE_SUPERVISION_CANDIDATE_CONTEXT_ONLY",
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.STATE,
        )
    }

    @Test
    fun currentP1ReadinessProducesOnlyTwoNegativeCandidates() {
        val output = completed(evaluate(Fixture.readiness()))

        assertEquals(
            listOf(
                CandidateState.NOT_APPLICABLE_NON_NEGATIVE,
                CandidateState.NEGATIVE_SUPERVISION_CANDIDATE,
                CandidateState.NOT_APPLICABLE_NON_NEGATIVE,
                CandidateState.NEGATIVE_SUPERVISION_CANDIDATE,
            ),
            output.decisions.map { it.state },
        )
        assertEquals(2, output.counters.negativeCandidateCount)
    }

    @Test
    fun currentP1CountersAreComplete() {
        val counters = completed(evaluate(Fixture.readiness())).counters

        assertEquals(
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.Counters(
                totalDecisions = 4,
                negativeCandidateCount = 2,
                nonNegativeNotApplicableCount = 2,
                requiresAdjudicationCount = 0,
                notEligibleCount = 0,
            ),
            counters,
        )
    }

    @Test
    fun negativeCandidateIdsAreDeterministicAndBoundToReadiness() {
        val readiness = Fixture.readiness()
        val first = completed(evaluate(readiness))
        val second = completed(evaluate(readiness))

        assertEquals(first.decisions.map { it.negativeCandidateId }, second.decisions.map { it.negativeCandidateId })
        assertEquals(first.logicalDigest, second.logicalDigest)
        assertTrue(first.decisions.filter { it.state == CandidateState.NEGATIVE_SUPERVISION_CANDIDATE }
            .all { it.negativeCandidateId!!.matches(Regex("[0-9a-f]{64}")) })
    }

    @Test
    fun logicalDigestIsRecomputedFromTheDeterministicOutput() {
        val output = completed(evaluate(Fixture.readiness()))

        assertEquals(
            output.logicalDigest,
            HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.batchLogicalDigest(
                output.copy(logicalDigest = ""),
            ),
        )
    }

    @Test
    fun changingNegativeEvidenceChangesCandidateIdentity() {
        val baseline = Fixture.readiness()
        val changedDecision = baseline.decisions[1].copy(evidenceReferenceIds = listOf("different-evidence"))
        val changed = Fixture.rebind(baseline, baseline.decisions.mapIndexed { index, decision ->
            if (index == 1) changedDecision else decision
        })

        val originalId = completed(evaluate(baseline)).decisions[1].negativeCandidateId
        val changedId = completed(evaluate(changed)).decisions[1].negativeCandidateId
        assertNotEquals(originalId, changedId)
    }

    @Test
    fun outputOrderMatchesReadinessOrder() {
        val readiness = Fixture.readiness()
        val output = completed(evaluate(readiness))

        assertEquals(
            readiness.decisions.map { it.readinessDecisionId },
            output.decisions.map { it.readinessDecisionId },
        )
    }

    @Test
    fun fullReadinessProvenanceIsRetained() {
        val readiness = Fixture.readiness()
        val output = completed(evaluate(readiness))

        readiness.decisions.zip(output.decisions).forEach { (source, candidate) ->
            assertEquals(source.readinessDecisionId, candidate.readinessDecisionId)
            assertEquals(source.state, candidate.readinessState)
            assertEquals(source.eligibilityBatchId, candidate.eligibilityBatchId)
            assertEquals(source.eligibilityInputBindingDigest, candidate.eligibilityInputBindingDigest)
            assertEquals(source.eligibilityDecisionId, candidate.eligibilityDecisionId)
            assertEquals(source.eligibilityState, candidate.eligibilityState)
            assertEquals(source.validationBatchId, candidate.validationBatchId)
            assertEquals(source.validationBatchBindingDigest, candidate.validationBatchBindingDigest)
            assertEquals(source.validationBatchLogicalDigest, candidate.validationBatchLogicalDigest)
            assertEquals(source.validationRecordId, candidate.validationRecordId)
            assertEquals(source.reviewUnitId, candidate.reviewUnitId)
            assertEquals(source.stableEntryId, candidate.stableEntryId)
            assertEquals(source.canonicalEntityId, candidate.canonicalEntityId)
            assertEquals(source.originalReviewerRef, candidate.originalReviewerRef)
            assertEquals(source.validatorReviewerRef, candidate.validatorReviewerRef)
            assertEquals(source.validationRound, candidate.validationRound)
            assertEquals(source.validationRevision, candidate.validationRevision)
            assertEquals(source.originalDecision, candidate.originalDecision)
            assertEquals(source.assessment, candidate.assessment)
            assertEquals(source.validationReasonCodes, candidate.validationReasonCodes)
            assertEquals(source.evidenceReferenceIds, candidate.evidenceReferenceIds)
            assertEquals(source.downstreamRoute, candidate.downstreamRoute)
        }
        assertEquals(readiness.logicalDigest, output.readinessBatchLogicalDigest)
    }

    @Test
    fun negativeCandidatesPreserveRejectedAssociationAndOwningUnit() {
        val output = completed(evaluate(Fixture.readiness()))
        val negative = output.decisions.filter { it.state == CandidateState.NEGATIVE_SUPERVISION_CANDIDATE }

        assertTrue(negative.all { it.originalDecision == HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION })
        assertEquals(
            Fixture.readiness().decisions
                .filter { it.state == ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE }
                .map { it.reviewUnitId },
            negative.map { it.reviewUnitId },
        )
        assertTrue(negative.all { it.evidenceReferenceIds.isNotEmpty() })
    }

    @Test
    fun positiveReadinessIsExplicitlyNonNegative() {
        val output = completed(evaluate(Fixture.readiness()))
        val positive = output.decisions.filter { it.readinessState == ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE }

        assertTrue(positive.all { it.state == CandidateState.NOT_APPLICABLE_NON_NEGATIVE })
        assertTrue(positive.all { it.negativeCandidateId == null })
    }

    @Test
    fun adjudicationIsPropagatedWithoutCandidate() {
        val output = completed(evaluate(Fixture.readiness(statesWithAdjudication())))

        assertEquals(2, output.counters.requiresAdjudicationCount)
        assertTrue(output.decisions.filter { it.readinessState == ReadinessState.REQUIRES_ADJUDICATION }
            .all { it.state == CandidateState.REQUIRES_ADJUDICATION && it.negativeCandidateId == null })
    }

    @Test
    fun notReadyIsExplicitlyNotEligible() {
        val output = completed(evaluate(Fixture.readiness(statesNotReady())))

        assertEquals(2, output.counters.notEligibleCount)
        assertTrue(output.decisions.filter { it.readinessState == ReadinessState.NOT_PUBLICATION_READY }
            .all { it.state == CandidateState.NOT_ELIGIBLE_FOR_NEGATIVE_SUPERVISION })
    }

    @Test
    fun missingNegativeEvidenceFailsClosed() {
        val readiness = Fixture.readiness()
        val broken = Fixture.rebind(readiness, readiness.decisions.mapIndexed { index, decision ->
            if (index == 1) decision.copy(evidenceReferenceIds = emptyList()) else decision
        })

        assertFailure(broken, FailureReason.MISSING_NEGATIVE_EVIDENCE)
    }

    @Test
    fun duplicateReadinessDecisionFailsClosed() {
        val readiness = Fixture.readiness()
        val duplicate = readiness.decisions.toMutableList().also {
            it[1] = it[1].copy(readinessDecisionId = it[0].readinessDecisionId)
        }

        assertFailure(Fixture.rebind(readiness, duplicate), FailureReason.DUPLICATE_READINESS_DECISION)
    }

    @Test
    fun duplicateReviewUnitFailsClosed() {
        val readiness = Fixture.readiness()
        val duplicate = readiness.decisions.toMutableList().also {
            it[1] = it[1].copy(reviewUnitId = it[0].reviewUnitId)
        }

        assertFailure(Fixture.rebind(readiness, duplicate), FailureReason.INVALID_READINESS_COUNT_OR_ORDER)
    }

    @Test
    fun brokenProvenanceFailsClosed() {
        val readiness = Fixture.readiness()
        val broken = readiness.decisions.toMutableList().also {
            it[1] = it[1].copy(canonicalEntityId = "")
        }

        assertFailure(Fixture.rebind(readiness, broken), FailureReason.BROKEN_READINESS_PROVENANCE)
    }

    @Test
    fun staleReadinessDecisionIdFailsClosed() {
        val readiness = Fixture.readiness()
        val broken = readiness.decisions.toMutableList().also {
            it[1] = it[1].copy(readinessDecisionId = "stale")
        }

        assertFailure(Fixture.rebind(readiness, broken), FailureReason.INVALID_READINESS_ID)
    }

    @Test
    fun invalidReadinessContractFailsClosed() {
        assertFailure(Fixture.readiness().copy(contractId = "wrong"), FailureReason.INVALID_READINESS_CONTRACT)
    }

    @Test
    fun invalidReadinessDigestFailsClosed() {
        assertFailure(Fixture.readiness().copy(logicalDigest = "a".repeat(64)), FailureReason.BROKEN_READINESS_BINDING)
    }

    @Test
    fun routeMismatchFailsClosed() {
        val readiness = Fixture.readiness()
        val broken = readiness.decisions.toMutableList().also {
            it[1] = it[1].copy(
                downstreamRoute = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_POSITIVE_GOLD_CANDIDATE,
            )
        }

        assertFailure(Fixture.rebind(readiness, broken), FailureReason.READINESS_STATE_OR_ROUTE_MISMATCH)
    }

    @Test
    fun readinessBindingMismatchFailsClosed() {
        val readiness = Fixture.readiness()
        val broken = readiness.decisions.toMutableList().also {
            it[1] = it[1].copy(eligibilityBatchId = "other-batch")
        }

        assertFailure(Fixture.rebind(readiness, broken), FailureReason.BROKEN_READINESS_BINDING)
    }

    @Test
    fun duplicateEvidenceReferenceFailsClosed() {
        val readiness = Fixture.readiness()
        val broken = readiness.decisions.toMutableList().also {
            it[1] = it[1].copy(evidenceReferenceIds = listOf("same", "same"))
        }

        assertFailure(Fixture.rebind(readiness, broken), FailureReason.INVALID_EVIDENCE_OWNERSHIP)
    }

    private fun evaluate(
        readiness: HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Batch,
    ): Result = HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.evaluate(readiness)

    private fun completed(result: Result): HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1.Batch =
        assertIs<Result.Completed>(result).value

    private fun assertFailure(
        readiness: HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Batch,
        reason: FailureReason,
    ) {
        assertEquals(reason, assertIs<Result.Failed>(evaluate(readiness)).reason)
    }

    private fun statesWithAdjudication() = listOf(
        ReadinessState.REQUIRES_ADJUDICATION,
        ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE,
        ReadinessState.REQUIRES_ADJUDICATION,
        ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE,
    )

    private fun statesNotReady() = listOf(
        ReadinessState.NOT_PUBLICATION_READY,
        ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE,
        ReadinessState.NOT_PUBLICATION_READY,
        ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE,
    )

    private object Fixture {
        private const val ELIGIBILITY_BATCH_ID = "eligibility-batch-fixture-v1"
        private const val VALIDATION_BATCH_ID = "validation-batch-fixture-v1"
        private const val REVIEWER = "reviewer:fixture:v1"
        private const val VALIDATOR = "validator:fixture:v1"
        private val INPUT_DIGEST = "a".repeat(64)
        private val ELIGIBILITY_BATCH_DIGEST = "b".repeat(64)
        private val VALIDATION_BINDING_DIGEST = "c".repeat(64)
        private val VALIDATION_LOGICAL_DIGEST = "d".repeat(64)

        fun readiness(
            states: List<ReadinessState> = listOf(
                ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE,
                ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE,
                ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE,
                ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE,
            ),
        ): HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Batch {
            val selections = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1
                .FROZEN_INPUT_BINDING.originalSelections
            val decisions = selections.mapIndexed { index, selection ->
                val state = states[index]
                val positive = state == ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE
                val originalDecision = if (positive) {
                    HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION
                } else if (state == ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE) {
                    HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION
                } else {
                    HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_AMBIGUOUS
                }
                val assessment = when (state) {
                    ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE,
                    ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE,
                        -> HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION
                    ReadinessState.REQUIRES_ADJUDICATION ->
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.CHALLENGE_ORIGINAL_DECISION
                    ReadinessState.NOT_PUBLICATION_READY ->
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ABSTAIN_VALIDATION_INSUFFICIENT_EVIDENCE
                }
                val route = when (state) {
                    ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE ->
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_POSITIVE_GOLD_CANDIDATE
                    ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE ->
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE
                    ReadinessState.REQUIRES_ADJUDICATION ->
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.REQUIRES_ADJUDICATION
                    ReadinessState.NOT_PUBLICATION_READY ->
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.NOT_ELIGIBLE
                }
                HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Decision(
                    readinessDecisionId = id('d', index),
                    eligibilityBatchId = ELIGIBILITY_BATCH_ID,
                    eligibilityInputBindingDigest = INPUT_DIGEST,
                    eligibilityDecisionId = id('e', index),
                    eligibilityState = eligibilityState(state),
                    validationBatchId = VALIDATION_BATCH_ID,
                    validationBatchBindingDigest = VALIDATION_BINDING_DIGEST,
                    validationBatchLogicalDigest = VALIDATION_LOGICAL_DIGEST,
                    validationRecordId = id('f', index),
                    reviewUnitId = selection.reviewUnitId,
                    stableEntryId = selection.stableEntryId,
                    canonicalEntityId = selection.canonicalEntityId,
                    originalReviewerRef = REVIEWER,
                    validatorReviewerRef = VALIDATOR,
                    validationRound = 1,
                    validationRevision = 1,
                    originalDecision = originalDecision,
                    assessment = assessment,
                    validationReasonCodes = listOf(
                        if (positive) {
                            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_SUPPORTS_ORIGINAL_DECISION
                        } else if (state == ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE) {
                            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_CONTRADICTS_ORIGINAL_DECISION
                        } else {
                            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.REQUIRES_ADJUDICATION
                        },
                    ),
                    evidenceReferenceIds = listOf("evidence-fixture-$index"),
                    downstreamRoute = route,
                    state = state,
                )
            }
            val unsigned = HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Batch(
                contractId = HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.CONTRACT_ID,
                version = HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.VERSION,
                state = HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.STATE,
                eligibilityBatchId = ELIGIBILITY_BATCH_ID,
                eligibilityInputBindingDigest = INPUT_DIGEST,
                eligibilityBatchLogicalDigest = ELIGIBILITY_BATCH_DIGEST,
                decisions = decisions,
                counters = HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Counters.from(decisions),
                logicalDigest = "",
            )
            return unsigned.copy(
                logicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.batchLogicalDigest(unsigned),
            )
        }

        fun rebind(
            batch: HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Batch,
            decisions: List<HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Decision>,
        ) = batch.copy(
            decisions = decisions,
            counters = HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Counters.from(decisions),
            logicalDigest = "",
        ).let { it.copy(logicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.batchLogicalDigest(it)) }

        private fun id(prefix: Char, index: Int): String = prefix + index.toString().padStart(63, '0')

        private fun eligibilityState(
            state: ReadinessState,
        ) = when (state) {
            ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE ->
                HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_POSITIVE_GOLD_CANDIDATE
            ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE ->
                HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_NEGATIVE_SUPERVISION_CANDIDATE
            ReadinessState.REQUIRES_ADJUDICATION ->
                HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.REQUIRES_ADJUDICATION
            ReadinessState.NOT_PUBLICATION_READY ->
                HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.NOT_ELIGIBLE
        }
    }
}
