package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Pure, deterministic boundary from P1 readiness to a negative-supervision
 * candidate context. This contract does not persist, publish, train, approve,
 * release, or mutate any catalog, authority, registry, or ground-truth data.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotNegativeSupervisionCandidateContractV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_NEGATIVE_SUPERVISION_CANDIDATE_CONTRACT_V1"
    const val VERSION = "1"
    const val STATE = "NEGATIVE_SUPERVISION_CANDIDATE_CONTEXT_ONLY"

    private const val CANDIDATE_ID_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_NEGATIVE_SUPERVISION_CANDIDATE_V1"
    private const val BATCH_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_NEGATIVE_SUPERVISION_CANDIDATE_BATCH_V1"
    private val SHA256 = Regex("[0-9a-f]{64}")

    fun evaluate(
        readinessBatch: HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Batch,
    ): Result {
        validateReadiness(readinessBatch)?.let { return Result.Failed(it) }

        val decisions = readinessBatch.decisions.map { readinessDecision ->
            val state = candidateState(readinessDecision.state)
            Decision(
                negativeCandidateId = if (state == CandidateState.NEGATIVE_SUPERVISION_CANDIDATE) {
                    negativeCandidateId(readinessBatch, readinessDecision)
                } else {
                    null
                },
                readinessDecisionId = readinessDecision.readinessDecisionId,
                readinessState = readinessDecision.state,
                eligibilityBatchId = readinessDecision.eligibilityBatchId,
                eligibilityInputBindingDigest = readinessDecision.eligibilityInputBindingDigest,
                eligibilityDecisionId = readinessDecision.eligibilityDecisionId,
                eligibilityState = readinessDecision.eligibilityState,
                validationBatchId = readinessDecision.validationBatchId,
                validationBatchBindingDigest = readinessDecision.validationBatchBindingDigest,
                validationBatchLogicalDigest = readinessDecision.validationBatchLogicalDigest,
                validationRecordId = readinessDecision.validationRecordId,
                reviewUnitId = readinessDecision.reviewUnitId,
                stableEntryId = readinessDecision.stableEntryId,
                canonicalEntityId = readinessDecision.canonicalEntityId,
                originalReviewerRef = readinessDecision.originalReviewerRef,
                validatorReviewerRef = readinessDecision.validatorReviewerRef,
                validationRound = readinessDecision.validationRound,
                validationRevision = readinessDecision.validationRevision,
                originalDecision = readinessDecision.originalDecision,
                assessment = readinessDecision.assessment,
                validationReasonCodes = readinessDecision.validationReasonCodes,
                evidenceReferenceIds = readinessDecision.evidenceReferenceIds,
                downstreamRoute = readinessDecision.downstreamRoute,
                state = state,
            )
        }
        val unsigned = Batch(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = STATE,
            readinessBatchId = readinessBatch.eligibilityBatchId,
            readinessInputBindingDigest = readinessBatch.eligibilityInputBindingDigest,
            readinessBatchLogicalDigest = readinessBatch.logicalDigest,
            decisions = decisions,
            counters = Counters.from(decisions),
            logicalDigest = "",
        )
        return Result.Completed(unsigned.copy(logicalDigest = batchLogicalDigest(unsigned)))
    }

    fun negativeCandidateId(
        readinessBatch: HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Batch,
        readinessDecision: HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Decision,
    ): String = sha256(
        buildString {
            appendLine(CANDIDATE_ID_DOMAIN)
            appendLine("contractId=$CONTRACT_ID")
            appendLine("version=$VERSION")
            appendLine("readinessBatchLogicalDigest=${readinessBatch.logicalDigest}")
            appendLine("readinessDecisionId=${readinessDecision.readinessDecisionId}")
            appendLine("validationRecordId=${readinessDecision.validationRecordId}")
            appendLine("eligibilityDecisionId=${readinessDecision.eligibilityDecisionId}")
            appendLine("reviewUnitId=${readinessDecision.reviewUnitId}")
            appendLine("canonicalEntityId=${readinessDecision.canonicalEntityId}")
            appendLine("state=${CandidateState.NEGATIVE_SUPERVISION_CANDIDATE.name}")
            readinessDecision.evidenceReferenceIds.forEach {
                appendLine("evidenceReferenceId=$it")
            }
        },
    )

    fun batchLogicalDigest(batch: Batch): String = sha256(
        buildString {
            appendLine(BATCH_DIGEST_DOMAIN)
            appendLine("contractId=${batch.contractId}")
            appendLine("version=${batch.version}")
            appendLine("state=${batch.state}")
            appendLine("readinessBatchId=${batch.readinessBatchId}")
            appendLine("readinessInputBindingDigest=${batch.readinessInputBindingDigest}")
            appendLine("readinessBatchLogicalDigest=${batch.readinessBatchLogicalDigest}")
            batch.decisions.forEach { decision ->
                appendLine("negativeCandidateId=${decision.negativeCandidateId}")
                appendLine("readinessDecisionId=${decision.readinessDecisionId}")
                appendLine("readinessState=${decision.readinessState.name}")
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
                appendLine("candidateState=${decision.state.name}")
            }
            appendLine("counters=${batch.counters}")
        },
    )

    private fun validateReadiness(
        batch: HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Batch,
    ): FailureReason? {
        if (batch.contractId !=
            HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.CONTRACT_ID ||
            batch.version !=
            HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.VERSION ||
            batch.state !=
            HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.STATE
        ) return FailureReason.INVALID_READINESS_CONTRACT

        if (batch.readinessBindingIsInvalid()) return FailureReason.BROKEN_READINESS_BINDING
        if (batch.decisions.size !=
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.FROZEN_INPUT_BINDING.originalSelections.size
        ) return FailureReason.INVALID_READINESS_COUNT_OR_ORDER
        if (batch.decisions.size !=
            HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Counters.from(
                batch.decisions,
            ).totalDecisions ||
            batch.counters !=
            HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Counters.from(
                batch.decisions,
            )
        ) return FailureReason.INVALID_READINESS_COUNTERS

        val expectedOrder = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1
            .FROZEN_INPUT_BINDING.originalSelections.map { it.reviewUnitId }
        if (batch.decisions.map { it.reviewUnitId } != expectedOrder) {
            return FailureReason.INVALID_READINESS_COUNT_OR_ORDER
        }
        if (batch.decisions.map { it.readinessDecisionId }.distinct().size != batch.decisions.size) {
            return FailureReason.DUPLICATE_READINESS_DECISION
        }
        if (batch.decisions.map { it.reviewUnitId }.distinct().size != batch.decisions.size) {
            return FailureReason.DUPLICATE_REVIEW_UNIT
        }
        batch.decisions.forEach { decision ->
            if (!SHA256.matches(decision.readinessDecisionId) ||
                !SHA256.matches(decision.eligibilityDecisionId) ||
                !SHA256.matches(decision.validationRecordId)
            ) return FailureReason.INVALID_READINESS_ID
            if (decision.eligibilityBatchId != batch.eligibilityBatchId ||
                decision.eligibilityInputBindingDigest != batch.eligibilityInputBindingDigest ||
                !SHA256.matches(decision.validationBatchBindingDigest) ||
                !SHA256.matches(decision.validationBatchLogicalDigest)
            ) return FailureReason.BROKEN_READINESS_BINDING
            if (decision.validationBatchId.isBlank() || decision.reviewUnitId.isBlank() ||
                decision.stableEntryId.isBlank() || decision.canonicalEntityId.isBlank() ||
                decision.originalReviewerRef.isBlank() || decision.validatorReviewerRef.isBlank() ||
                decision.validationRound < 1 || decision.validationRevision < 1 ||
                decision.validationReasonCodes.isEmpty()
            ) return FailureReason.BROKEN_READINESS_PROVENANCE
            if (decision.evidenceReferenceIds.any { it.isBlank() } ||
                decision.evidenceReferenceIds.distinct().size != decision.evidenceReferenceIds.size
            ) return FailureReason.INVALID_EVIDENCE_OWNERSHIP
            if (decision.eligibilityState != expectedEligibilityState(decision.state) ||
                decision.downstreamRoute != expectedRoute(decision.state)
            ) return FailureReason.READINESS_STATE_OR_ROUTE_MISMATCH
            if (decision.state ==
                HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE
            ) {
                if (decision.originalDecision != HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION) {
                    return FailureReason.ORIGINAL_DECISION_MISMATCH
                }
                if (decision.evidenceReferenceIds.isEmpty()) return FailureReason.MISSING_NEGATIVE_EVIDENCE
            }
            if (decision.state ==
                HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE &&
                decision.originalDecision != HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION
            ) return FailureReason.ORIGINAL_DECISION_MISMATCH
        }
        return null
    }

    private fun HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.Batch.readinessBindingIsInvalid(): Boolean =
        eligibilityBatchId.isBlank() ||
            !SHA256.matches(eligibilityInputBindingDigest) ||
            !SHA256.matches(eligibilityBatchLogicalDigest) ||
            !SHA256.matches(logicalDigest) ||
            logicalDigest !=
            HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.batchLogicalDigest(
                copy(logicalDigest = ""),
            )

    private fun expectedEligibilityState(
        state: HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState,
    ) = when (state) {
        HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE ->
            HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_POSITIVE_GOLD_CANDIDATE
        HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE ->
            HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.ELIGIBLE_NEGATIVE_SUPERVISION_CANDIDATE
        HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.REQUIRES_ADJUDICATION ->
            HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.REQUIRES_ADJUDICATION
        HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.NOT_PUBLICATION_READY ->
            HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1.State.NOT_ELIGIBLE
    }

    private fun expectedRoute(
        state: HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState,
    ) = when (state) {
        HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE ->
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_POSITIVE_GOLD_CANDIDATE
        HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE ->
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE
        HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.REQUIRES_ADJUDICATION ->
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.REQUIRES_ADJUDICATION
        HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.NOT_PUBLICATION_READY ->
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.NOT_ELIGIBLE
    }

    private fun candidateState(
        state: HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState,
    ) = when (state) {
        HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.POSITIVE_GOLD_PUBLICATION_READINESS_CANDIDATE ->
            CandidateState.NOT_APPLICABLE_NON_NEGATIVE
        HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.NEGATIVE_SUPERVISION_PUBLICATION_READINESS_CANDIDATE ->
            CandidateState.NEGATIVE_SUPERVISION_CANDIDATE
        HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.REQUIRES_ADJUDICATION ->
            CandidateState.REQUIRES_ADJUDICATION
        HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState.NOT_PUBLICATION_READY ->
            CandidateState.NOT_ELIGIBLE_FOR_NEGATIVE_SUPERVISION
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    enum class CandidateState {
        NEGATIVE_SUPERVISION_CANDIDATE,
        NOT_APPLICABLE_NON_NEGATIVE,
        REQUIRES_ADJUDICATION,
        NOT_ELIGIBLE_FOR_NEGATIVE_SUPERVISION,
    }

    enum class FailureReason {
        INVALID_READINESS_CONTRACT,
        BROKEN_READINESS_BINDING,
        INVALID_READINESS_COUNT_OR_ORDER,
        INVALID_READINESS_COUNTERS,
        DUPLICATE_READINESS_DECISION,
        DUPLICATE_REVIEW_UNIT,
        INVALID_READINESS_ID,
        BROKEN_READINESS_PROVENANCE,
        INVALID_EVIDENCE_OWNERSHIP,
        READINESS_STATE_OR_ROUTE_MISMATCH,
        ORIGINAL_DECISION_MISMATCH,
        MISSING_NEGATIVE_EVIDENCE,
    }

    data class Decision(
        val negativeCandidateId: String?,
        val readinessDecisionId: String,
        val readinessState: HimZeroCandidateRecoveryHumanReviewP1PilotPostEligibilityPublicationReadinessContractV1.ReadinessState,
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
        val state: CandidateState,
    )

    data class Counters(
        val totalDecisions: Int,
        val negativeCandidateCount: Int,
        val nonNegativeNotApplicableCount: Int,
        val requiresAdjudicationCount: Int,
        val notEligibleCount: Int,
    ) {
        companion object {
            fun from(decisions: List<Decision>) = Counters(
                totalDecisions = decisions.size,
                negativeCandidateCount = decisions.count { it.state == CandidateState.NEGATIVE_SUPERVISION_CANDIDATE },
                nonNegativeNotApplicableCount = decisions.count { it.state == CandidateState.NOT_APPLICABLE_NON_NEGATIVE },
                requiresAdjudicationCount = decisions.count { it.state == CandidateState.REQUIRES_ADJUDICATION },
                notEligibleCount = decisions.count {
                    it.state == CandidateState.NOT_ELIGIBLE_FOR_NEGATIVE_SUPERVISION
                },
            )
        }
    }

    data class Batch(
        val contractId: String,
        val version: String,
        val state: String,
        val readinessBatchId: String,
        val readinessInputBindingDigest: String,
        val readinessBatchLogicalDigest: String,
        val decisions: List<Decision>,
        val counters: Counters,
        val logicalDigest: String,
    )

    sealed interface Result {
        data class Completed(val value: Batch) : Result
        data class Failed(val reason: FailureReason) : Result
    }
}
