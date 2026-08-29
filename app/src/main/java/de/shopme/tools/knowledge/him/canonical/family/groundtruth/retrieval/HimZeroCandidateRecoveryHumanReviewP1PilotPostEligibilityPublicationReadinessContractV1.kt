package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Pure, in-memory boundary after P1 post-validation eligibility.
 *
 * This contract identifies the next permissible publication direction only.
 * It does not approve, publish, persist, release, train, or mutate anything.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POST_ELIGIBILITY_PUBLICATION_READINESS_CONTRACT_V1"
    const val VERSION = "1"
    const val STATE = "POST_ELIGIBILITY_PUBLICATION_READINESS_CONTEXT_ONLY"

    private const val DECISION_ID_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POST_ELIGIBILITY_PUBLICATION_READINESS_DECISION_V1"
    private const val BATCH_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POST_ELIGIBILITY_PUBLICATION_READINESS_BATCH_V1"
    private val SHA256 = Regex("[0-9a-f]{64}")

    fun evaluate(
        eligibilityBatch: HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.Batch,
    ): Result {
        validate(eligibilityBatch)?.let { return Result.Failed(it) }

        val decisions = eligibilityBatch.decisions.map { decision ->
            val state = readinessState(decision.state)
            Decision(
                readinessDecisionId = readinessDecisionId(eligibilityBatch, decision, state),
                eligibilityBatchId = eligibilityBatch.validationBatchId,
                eligibilityInputBindingDigest = eligibilityBatch.inputBindingDigest,
                eligibilityDecisionId = decision.decisionId,
                eligibilityState = decision.state,
                validationBatchId = decision.validationBatchId,
                validationBatchBindingDigest = decision.validationBatchBindingDigest,
                validationBatchLogicalDigest = decision.validationBatchLogicalDigest,
                validationRecordId = decision.validationRecordId,
                reviewUnitId = decision.reviewUnitId,
                stableEntryId = decision.stableEntryId,
                canonicalEntityId = decision.canonicalEntityId,
                originalReviewerRef = decision.originalReviewerRef,
                validatorReviewerRef = decision.validatorReviewerRef,
                validationRound = decision.validationRound,
                validationRevision = decision.validationRevision,
                originalDecision = decision.originalDecision,
                assessment = decision.assessment,
                validationReasonCodes = decision.validationReasonCodes,
                evidenceReferenceIds = decision.evidenceReferenceIds,
                downstreamRoute = decision.downstreamRoute,
                state = state,
            )
        }
        val output = Batch(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = STATE,
            eligibilityBatchId = eligibilityBatch.validationBatchId,
            eligibilityInputBindingDigest = eligibilityBatch.inputBindingDigest,
            eligibilityBatchLogicalDigest = eligibilityBatch.logicalDigest,
            decisions = decisions,
            counters = Counters.from(decisions),
            logicalDigest = "",
        )
        return Result.Completed(output.copy(logicalDigest = batchLogicalDigest(output)))
    }

    fun readinessDecisionId(
        eligibilityBatch: HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.Batch,
        eligibilityDecision: HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.Decision,
        state: ReadinessState,
    ): String = sha256(
        buildString {
            appendLine(DECISION_ID_DOMAIN)
            appendLine("contractId=$CONTRACT_ID")
            appendLine("version=$VERSION")
            appendLine("eligibilityBatchLogicalDigest=${eligibilityBatch.logicalDigest}")
            appendLine("eligibilityDecisionId=${eligibilityDecision.decisionId}")
            appendLine("eligibilityState=${eligibilityDecision.state.name}")
            appendLine("readinessState=${state.name}")
            appendLine("inputBindingDigest=${eligibilityBatch.inputBindingDigest}")
        },
    )

    fun batchLogicalDigest(batch: Batch): String = sha256(
        buildString {
            appendLine(BATCH_DIGEST_DOMAIN)
            appendLine("contractId=${batch.contractId}")
            appendLine("version=${batch.version}")
            appendLine("state=${batch.state}")
            appendLine("eligibilityBatchId=${batch.eligibilityBatchId}")
            appendLine("eligibilityInputBindingDigest=${batch.eligibilityInputBindingDigest}")
            appendLine("eligibilityBatchLogicalDigest=${batch.eligibilityBatchLogicalDigest}")
            batch.decisions.forEach { decision ->
                appendLine("readinessDecisionId=${decision.readinessDecisionId}")
                appendLine("eligibilityBatchId=${decision.eligibilityBatchId}")
                appendLine("eligibilityInputBindingDigest=${decision.eligibilityInputBindingDigest}")
                appendLine("eligibilityDecisionId=${decision.eligibilityDecisionId}")
                appendLine("eligibilityState=${decision.eligibilityState.name}")
                appendLine("validationBatchId=${decision.validationBatchId}")
                appendLine("validationBatchBindingDigest=${decision.validationBatchBindingDigest}")
                appendLine("validationBatchLogicalDigest=${decision.validationBatchLogicalDigest}")
                appendLine("validationRecordId=${decision.validationRecordId}")
                appendLine("reviewUnitId=${decision.reviewUnitId}")
                appendLine("stableEntryId=${decision.stableEntryId}")
                appendLine("canonicalEntityId=${decision.canonicalEntityId}")
                appendLine("originalReviewerRef=${decision.originalReviewerRef}")
                appendLine("validatorReviewerRef=${decision.validatorReviewerRef}")
                appendLine("validationRound=${decision.validationRound}")
                appendLine("validationRevision=${decision.validationRevision}")
                appendLine("originalDecision=${decision.originalDecision.name}")
                appendLine("assessment=${decision.assessment.name}")
                decision.validationReasonCodes.forEach { appendLine("reasonCode=${it.name}") }
                decision.evidenceReferenceIds.forEach { appendLine("evidenceReferenceId=$it") }
                appendLine("downstreamRoute=${decision.downstreamRoute.name}")
                appendLine("readinessState=${decision.state.name}")
            }
            appendLine("counters=${batch.counters}")
        },
    )

    private fun validate(
        batch: HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.Batch,
    ): FailureReason? {
        if (batch.contractId !=
            HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.CONTRACT_ID ||
            batch.version != HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.VERSION ||
            batch.state != HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.STATE
        ) return FailureReason.UPSTREAM_CONTRACT_INVALID
        if (batch.decisions.size != batch.counters.totalDecisions ||
            batch.decisions.size !=
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.FROZEN_INPUT_BINDING.originalSelections.size
        ) return FailureReason.INVALID_ELIGIBILITY_DECISION_COUNT_OR_ORDER

        val expectedReviewUnitOrder =
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.FROZEN_INPUT_BINDING
                .originalSelections
                .map { it.reviewUnitId }
        if (batch.decisions.map { it.reviewUnitId } != expectedReviewUnitOrder) {
            return FailureReason.INVALID_ELIGIBILITY_DECISION_COUNT_OR_ORDER
        }
        if (batch.decisions.map { it.decisionId }.distinct().size != batch.decisions.size) {
            return FailureReason.DUPLICATE_ELIGIBILITY_DECISION
        }
        if (batch.decisions.any {
                !SHA256.matches(it.decisionId) || !SHA256.matches(it.validationRecordId)
            }) return FailureReason.INVALID_ELIGIBILITY_DECISION_ID
        if (!SHA256.matches(batch.inputBindingDigest) ||
            !SHA256.matches(batch.validationBatchBindingDigest) ||
            !SHA256.matches(batch.validationBatchLogicalDigest) ||
            !SHA256.matches(batch.logicalDigest) ||
            batch.logicalDigest !=
            HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.batchLogicalDigest(
                batch.copy(logicalDigest = ""),
            )
        ) return FailureReason.DIGEST_MISMATCH
        if (batch.counters !=
            HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.Counters.from(
                batch.decisions,
            )
        ) return FailureReason.BROKEN_PROVENANCE

        batch.decisions.forEach { decision ->
            if (decision.validationBatchId != batch.validationBatchId ||
                decision.inputBindingDigest != batch.inputBindingDigest ||
                decision.validationBatchBindingDigest != batch.validationBatchBindingDigest ||
                decision.validationBatchLogicalDigest != batch.validationBatchLogicalDigest
            ) return FailureReason.BROKEN_VALIDATION_RECORD_BINDING
            if (decision.reviewUnitId.isBlank() || decision.stableEntryId.isBlank() ||
                decision.canonicalEntityId.isBlank() || decision.originalReviewerRef.isBlank() ||
                decision.validatorReviewerRef.isBlank() || decision.validationRound < 1 ||
                decision.validationRevision < 1
            ) return FailureReason.BROKEN_PROVENANCE
            if (decision.downstreamRoute != expectedRoute(decision.state)) {
                return FailureReason.ELIGIBILITY_STATE_MISMATCH
            }
        }
        return null
    }

    private fun expectedRoute(
        state: HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State,
    ) = when (state) {
        HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_POSITIVE_GOLD_CANDIDATE ->
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_POSITIVE_GOLD_CANDIDATE
        HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_NEGATIVE_SUPERVISION_CANDIDATE ->
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE
        HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.REQUIRES_ADJUDICATION ->
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.REQUIRES_ADJUDICATION
        HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.NOT_ELIGIBLE ->
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.NOT_ELIGIBLE
    }

    private fun readinessState(
        state: HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State,
    ) = when (state) {
        HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_POSITIVE_GOLD_CANDIDATE ->
            ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE
        HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_NEGATIVE_SUPERVISION_CANDIDATE ->
            ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE
        HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.REQUIRES_ADJUDICATION ->
            ReadinessState.REQUIRES_ADJUDICATION
        HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.NOT_ELIGIBLE ->
            ReadinessState.NOT_PUBLICATION_READY
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    enum class ReadinessState {
        POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE,
        NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE,
        REQUIRES_ADJUDICATION,
        NOT_PUBLICATION_READY,
    }

    enum class FailureReason {
        INVALID_ELIGIBILITY_BATCH,
        INVALID_ELIGIBILITY_DECISION_COUNT_OR_ORDER,
        DUPLICATE_ELIGIBILITY_DECISION,
        INVALID_ELIGIBILITY_DECISION_ID,
        BROKEN_VALIDATION_RECORD_BINDING,
        BROKEN_REVIEW_UNIT_BINDING,
        BROKEN_PROVENANCE,
        ELIGIBILITY_STATE_MISMATCH,
        UNSUPPORTED_ELIGIBILITY_STATE,
        DIGEST_MISMATCH,
        UPSTREAM_CONTRACT_INVALID,
    }

    data class Decision(
        val readinessDecisionId: String,
        val eligibilityBatchId: String,
        val eligibilityInputBindingDigest: String,
        val eligibilityDecisionId: String,
        val eligibilityState: HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State,
        val validationBatchId: String,
        val validationBatchBindingDigest: String,
        val validationBatchLogicalDigest: String,
        val validationRecordId: String,
        val reviewUnitId: String,
        val stableEntryId: String,
        val canonicalEntityId: String,
        val originalReviewerRef: String,
        val validatorReviewerRef: String,
        val validationRound: Int,
        val validationRevision: Int,
        val originalDecision: HimZeroCandidateRecoveryHumanReviewDecisionV1,
        val assessment: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1,
        val validationReasonCodes: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1>,
        val evidenceReferenceIds: List<String>,
        val downstreamRoute: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1,
        val state: ReadinessState,
    )

    data class Counters(
        val totalDecisions: Int,
        val positiveReadinessCount: Int,
        val negativeReadinessCount: Int,
        val requiresAdjudicationCount: Int,
        val notReadyCount: Int,
    ) {
        companion object {
            fun from(decisions: List<Decision>) = Counters(
                totalDecisions = decisions.size,
                positiveReadinessCount = decisions.count { it.state == ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE },
                negativeReadinessCount = decisions.count { it.state == ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE },
                requiresAdjudicationCount = decisions.count { it.state == ReadinessState.REQUIRES_ADJUDICATION },
                notReadyCount = decisions.count { it.state == ReadinessState.NOT_PUBLICATION_READY },
            )
        }
    }

    data class Batch(
        val contractId: String,
        val version: String,
        val state: String,
        val eligibilityBatchId: String,
        val eligibilityInputBindingDigest: String,
        val eligibilityBatchLogicalDigest: String,
        val decisions: List<Decision>,
        val counters: Counters,
        val logicalDigest: String,
    )

    sealed interface Result {
        data class Completed(val value: Batch) : Result

        data class Failed(val reason: FailureReason) : Result
    }
}
