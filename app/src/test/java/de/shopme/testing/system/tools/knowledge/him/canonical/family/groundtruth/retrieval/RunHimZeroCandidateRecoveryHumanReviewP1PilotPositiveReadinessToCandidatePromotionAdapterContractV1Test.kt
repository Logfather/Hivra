package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecisionReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewDecisionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveReadinessToCandidatePromotionAdapterContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveReadinessToCandidatePromotionAdapterContractV1.BindingState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveReadinessToCandidatePromotionAdapterContractV1.CandidatePromotionBinding
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveReadinessToCandidatePromotionAdapterContractV1.FailureReason
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveReadinessToCandidatePromotionAdapterContractV1.Request
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimZeroCandidateRecoveryHumanReviewP1PilotPositiveReadinessToCandidatePromotionAdapterContractV1.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RunHimZeroCandidateRecoveryHumanReviewP1PilotPositiveReadinessToCandidatePromotionAdapterContractV1Test {
    @Test
    fun contractIdentityAndStateAreFrozen() {
        assertEquals(
            "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POSITIVE_READINESS_TO_CANDIDATE_PROMOTION_ADAPTER_CONTRACT_V1",
            HimZeroCandidateRecoveryHumanReviewP1PilotPositiveReadinessToCandidatePromotionAdapterContractV1.CONTRACT_ID,
        )
        assertEquals("1", HimZeroCandidateRecoveryHumanReviewP1PilotPositiveReadinessToCandidatePromotionAdapterContractV1.VERSION)
        assertEquals(
            "POSITIVE_CANDIDATE_PROMOTION_BINDING_CONTEXT_ONLY",
            HimZeroCandidateRecoveryHumanReviewP1PilotPositiveReadinessToCandidatePromotionAdapterContractV1.STATE,
        )
    }

    @Test
    fun deterministicPositiveReadyCaseProducesLaterPromotionDomainBinding() {
        val readiness = Fixture.readiness()
        val output = completed(
            evaluate(
                Request(
                    readinessBatch = readiness,
                    candidateBindings = Fixture.positiveBindings(readiness),
                ),
            ),
        )

        assertEquals(
            listOf(
                BindingState.READY_FOR_LATER_PROMOTION_DOMAIN_STEP,
                BindingState.NOT_APPLICABLE_NON_POSITIVE,
                BindingState.READY_FOR_LATER_PROMOTION_DOMAIN_STEP,
                BindingState.NOT_APPLICABLE_NON_POSITIVE,
            ),
            output.decisions.map { it.state },
        )
        assertEquals(2, output.counters.readyForLaterPromotionDomainStep)
        assertEquals(0, output.counters.blockedMissingCandidateBinding)
        assertEquals(2, output.counters.nonApplicableNonPositive)
    }

    @Test
    fun repeatedEvaluationProducesIdenticalBatchIdsAndDigest() {
        val readiness = Fixture.readiness()
        val request = Request(readiness, Fixture.positiveBindings(readiness))
        val first = completed(evaluate(request))
        val second = completed(evaluate(request))

        assertEquals(first, second)
        assertEquals(first.logicalDigest, second.logicalDigest)
        assertEquals(
            first.decisions.map { it.bindingDecisionId },
            second.decisions.map { it.bindingDecisionId },
        )
    }

    @Test
    fun candidateBindingInputOrderDoesNotChangeOutputOrder() {
        val readiness = Fixture.readiness()
        val bindings = Fixture.positiveBindings(readiness)
        val ordered = completed(evaluate(Request(readiness, bindings)))
        val reordered = completed(evaluate(Request(readiness, bindings.reversed())))

        assertEquals(ordered, reordered)
        assertEquals(
            readiness.decisions.map { it.readinessDecisionId },
            ordered.decisions.map { it.readinessDecision.readinessDecisionId },
        )
    }

    @Test
    fun deterministicIdsBindReadinessAndCandidateIdentity() {
        val readiness = Fixture.readiness()
        val baseline = completed(evaluate(Request(readiness, Fixture.positiveBindings(readiness))))
        val changedBinding = Fixture.positiveBindings(readiness).mapIndexed { index, binding ->
            if (index == 0) binding.copy(candidateTerm = "different-term") else binding
        }
        val changed = completed(evaluate(Request(readiness, changedBinding)))

        assertTrue(baseline.decisions[0].bindingDecisionId != changed.decisions[0].bindingDecisionId)
        assertTrue(baseline.logicalDigest != changed.logicalDigest)
    }

    @Test
    fun provenanceIsRetainedWithoutCreatingPromotionOrApprovalState() {
        val readiness = Fixture.readiness()
        val output = completed(evaluate(Request(readiness, Fixture.positiveBindings(readiness))))

        assertEquals(readiness.decisions, output.decisions.map { it.readinessDecision })
        assertTrue(
            output.decisions.none {
                it.state.name in setOf("GOLD", "APPROVED", "PROMOTED", "PROMOTION_ELIGIBLE")
            },
        )
    }

    @Test
    fun negativeReadinessIsNotBoundToPositivePromotion() {
        val readiness = Fixture.readiness()
        val negativeDecision = readiness.decisions[1]
        assertFailure(
            Request(
                readiness,
                listOf(Fixture.bindingFor(negativeDecision)),
            ),
            FailureReason.NON_POSITIVE_CANDIDATE_BINDING,
        )
    }

    @Test
    fun adjudicationAndNotReadyRemainNotApplicable() {
        val readiness = Fixture.readiness(
            states = listOf(
                ReadinessState.REQUIRES_ADJUDICATION,
                ReadinessState.NOT_PUBLICATION_READY,
                ReadinessState.REQUIRES_ADJUDICATION,
                ReadinessState.NOT_PUBLICATION_READY,
            ),
        )
        val output = completed(evaluate(Request(readiness)))

        assertTrue(output.decisions.all { it.state == BindingState.NOT_APPLICABLE_NON_POSITIVE })
        assertEquals(4, output.counters.nonApplicableNonPositive)
    }

    @Test
    fun missingCandidateBindingProducesExplicitBlockedState() {
        val output = completed(evaluate(Request(Fixture.readiness())))

        assertEquals(2, output.counters.blockedMissingCandidateBinding)
        assertTrue(
            output.decisions.filter {
                it.readinessDecision.state == ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE
            }.all { it.state == BindingState.BLOCKED_MISSING_CANDIDATE_BINDING },
        )
    }

    @Test
    fun mismatchedCandidateBindingFailsClosed() {
        val readiness = Fixture.readiness()
        val broken = Fixture.bindingFor(readiness.decisions[0]).copy(
            candidateRelation = HimCandidateRelation.Identity(HimEntityId("000000")),
        )

        assertFailure(Request(readiness, listOf(broken)), FailureReason.CANDIDATE_CANONICAL_MISMATCH)
    }

    @Test
    fun brokenProvenanceFailsClosed() {
        val broken = Fixture.rebind(
            Fixture.readiness(),
            Fixture.readiness().decisions.toMutableList().also {
                it[0] = it[0].copy(canonicalEntityId = "")
            },
        )

        assertFailure(Request(broken), FailureReason.BROKEN_READINESS_PROVENANCE)
    }

    @Test
    fun invalidUpstreamFailsClosed() {
        val broken = Fixture.readiness().copy(contractId = "wrong-contract")

        assertFailure(Request(broken), FailureReason.INVALID_READINESS_CONTRACT)
    }

    @Test
    fun duplicateReadinessDecisionFailsClosed() {
        val baseline = Fixture.readiness()
        val decisions = baseline.decisions.toMutableList()
        decisions[0] = decisions[0].copy(readinessDecisionId = decisions[1].readinessDecisionId)

        assertFailure(Request(Fixture.rebind(baseline, decisions)), FailureReason.DUPLICATE_READINESS_DECISION)
    }

    @Test
    fun routeAndReadinessMismatchFailsClosed() {
        val baseline = Fixture.readiness()
        val decisions = baseline.decisions.toMutableList()
        decisions[0] = decisions[0].copy(
            eligibilityState = HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_NEGATIVE_SUPERVISION_CANDIDATE,
            state = ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE,
        )

        assertFailure(Request(Fixture.rebind(baseline, decisions)), FailureReason.READINESS_ROUTE_MISMATCH)
    }

    private fun evaluate(request: Request): Result =
        HimZeroCandidateRecoveryHumanReviewP1PilotPositiveReadinessToCandidatePromotionAdapterContractV1.evaluate(request)

    private fun completed(result: Result): HimZeroCandidateRecoveryHumanReviewP1PilotPositiveReadinessToCandidatePromotionAdapterContractV1.Batch =
        assertIs<Result.Completed>(result).value

    private fun assertFailure(request: Request, reason: FailureReason) {
        val failure = assertIs<Result.Failed>(evaluate(request))
        assertEquals(reason, failure.reason)
    }

    private object Fixture {
        private val INPUT_DIGEST = "a".repeat(64)
        private val ELIGIBILITY_DIGEST = "b".repeat(64)
        private val VALIDATION_BINDING_DIGEST = "c".repeat(64)
        private val VALIDATION_LOGICAL_DIGEST = "d".repeat(64)
        private const val VALIDATION_BATCH_ID = "validation-batch-fixture-v1"
        private const val REVIEWER = "reviewer:fixture:v1"
        private const val VALIDATOR = "validator:fixture:v1"

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
                val originalDecision = when (state) {
                    ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE ->
                        HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION
                    ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE ->
                        HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION
                    ReadinessState.REQUIRES_ADJUDICATION ->
                        HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_AMBIGUOUS
                    ReadinessState.NOT_PUBLICATION_READY ->
                        HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_INSUFFICIENT_EVIDENCE
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
                    eligibilityBatchId = "eligibility-batch-fixture-v1",
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
                        when (state) {
                            ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE ->
                                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_SUPPORTS_ORIGINAL_DECISION
                            ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE ->
                                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_CONTRADICTS_ORIGINAL_DECISION
                            ReadinessState.REQUIRES_ADJUDICATION ->
                                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.REQUIRES_ADJUDICATION
                            ReadinessState.NOT_PUBLICATION_READY ->
                                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.INSUFFICIENT_DIRECT_EVIDENCE
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
                eligibilityBatchId = "eligibility-batch-fixture-v1",
                eligibilityInputBindingDigest = INPUT_DIGEST,
                eligibilityBatchLogicalDigest = ELIGIBILITY_DIGEST,
                decisions = decisions,
                counters = HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Counters.from(decisions),
                logicalDigest = "",
            )
            return unsigned.copy(
                logicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.batchLogicalDigest(unsigned),
            )
        }

        fun positiveBindings(
            readiness: HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Batch,
        ) = readiness.decisions.filter {
            it.state == ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE
        }.map { bindingFor(it) }

        fun bindingFor(
            decision: HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Decision,
        ) = CandidatePromotionBinding(
            readinessDecisionId = decision.readinessDecisionId,
            candidateReference = HimCandidateReference("candidate:${decision.reviewUnitId}"),
            candidateTerm = "candidate-term-${decision.reviewUnitId}",
            candidateRelation = HimCandidateRelation.Identity(HimEntityId(decision.canonicalEntityId)),
            candidateDatasetDigest = HimSha256("e".repeat(64)),
            validationReference = HimCandidateValidationDecisionReference("validation:v1:${"f".repeat(64)}"),
        )

        fun rebind(
            batch: HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Batch,
            decisions: List<HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Decision>,
        ) = batch.copy(
            decisions = decisions,
            counters = HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Counters.from(decisions),
            logicalDigest = "",
        ).let {
            it.copy(
                logicalDigest = HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.batchLogicalDigest(it),
            )
        }

        private fun id(prefix: Char, index: Int): String =
            prefix + index.toString().padStart(63, '0')

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
