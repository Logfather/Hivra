package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Pure boundary contract after independent P1 validation.
 *
 * This contract classifies an already validated validation batch. It does not
 * publish, persist, promote, adjudicate, or create training material.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotPostValidationEligibilityContractV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POST_VALIDATION_ELIGIBILITY_CONTRACT_V1"
    const val VERSION = "1"
    const val STATE = "POST_VALIDATION_ELIGIBILITY_CONTEXT_ONLY"
    private const val DECISION_ID_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POST_VALIDATION_ELIGIBILITY_DECISION_V1"
    private const val BATCH_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_POST_VALIDATION_ELIGIBILITY_BATCH_V1"
    private val SHA256 = Regex("[0-9a-f]{64}")

    fun evaluate(
        validationBatch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1,
    ): Result {
        preflight(validationBatch)?.let { return Result.Failed(it) }

        when (
            val validation = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.validate(
                validationBatch,
            )
        ) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationResultV1.Valid -> Unit
            is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationResultV1.Invalid ->
                return Result.Failed(mapFailure(validation.reason))
        }

        val decisions = validationBatch.records.map { record ->
            val state = stateFor(record)
            val evidence = validationBatch.inputBinding.evidenceBindings.filter { binding ->
                binding.reviewUnitId == record.originalDecision.reviewUnitId &&
                    binding.evidenceReferenceId in record.evidenceReferenceIds
            }
            if (record.originalDecision.decision != HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION &&
                record.originalDecision.decision != HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION &&
                record.assessment == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION
            ) {
                return Result.Failed(FailureReason.INVALID_ASSESSMENT)
            }
            if (state.isPositive && evidence.none { it.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION }) {
                return Result.Failed(FailureReason.MISSING_EVIDENCE)
            }
            if (state.isNegative && evidence.none { it.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION }) {
                return Result.Failed(FailureReason.MISSING_EVIDENCE)
            }
            decision(validationBatch, record, state)
        }

        val output = Batch(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = STATE,
            validationBatchId = validationBatch.inputBinding.batchId,
            inputBindingDigest = validationBatch.inputBinding.bindingDigest,
            validationBatchBindingDigest = validationBatch.bindingDigest,
            validationBatchLogicalDigest = validationBatch.logicalDigest,
            decisions = decisions,
            counters = Counters.from(decisions),
            logicalDigest = "",
        )
        return Result.Completed(output.copy(logicalDigest = batchLogicalDigest(output)))
    }

    fun decisionId(
        validationBatch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1,
        record: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1,
        state: State,
    ): String = sha256(
        buildString {
            appendLine(DECISION_ID_DOMAIN)
            appendLine("contractId=$CONTRACT_ID")
            appendLine("version=$VERSION")
            appendLine("validationBatchLogicalDigest=${validationBatch.logicalDigest}")
            appendLine("validationRecordId=${record.validationRecordId}")
            appendLine("reviewUnitId=${record.originalDecision.reviewUnitId}")
            appendLine("stableEntryId=${record.originalDecision.stableEntryId}")
            appendLine("canonicalEntityId=${record.originalDecision.canonicalEntityId}")
            appendLine("state=${state.name}")
            appendLine("assessment=${record.assessment.name}")
            record.reasonCodes.forEach { appendLine("reasonCode=${it.name}") }
            record.evidenceReferenceIds.forEach { appendLine("evidenceReferenceId=$it") }
        },
    )

    fun batchLogicalDigest(batch: Batch): String = sha256(
        buildString {
            appendLine(BATCH_DIGEST_DOMAIN)
            appendLine("contractId=${batch.contractId}")
            appendLine("version=${batch.version}")
            appendLine("state=${batch.state}")
            appendLine("validationBatchId=${batch.validationBatchId}")
            appendLine("inputBindingDigest=${batch.inputBindingDigest}")
            appendLine("validationBatchBindingDigest=${batch.validationBatchBindingDigest}")
            appendLine("validationBatchLogicalDigest=${batch.validationBatchLogicalDigest}")
            batch.decisions.forEach { decision ->
                appendLine("decisionId=${decision.decisionId}")
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
                appendLine("eligibilityState=${decision.state.name}")
            }
            appendLine("counters=${batch.counters}")
        },
    )

    private fun preflight(
        batch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1,
    ): FailureReason? {
        val expectedBinding = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.FROZEN_INPUT_BINDING
        if (batch.inputBinding.batchFileBinding != expectedBinding.batchFileBinding) return FailureReason.BROKEN_PACKET_BINDING
        if (batch.inputBinding.submissionId != expectedBinding.submissionId ||
            batch.inputBinding.originalInputBindingDigest != expectedBinding.originalInputBindingDigest ||
            batch.inputBinding.originalBatchLogicalDigest != expectedBinding.originalBatchLogicalDigest
        ) return FailureReason.BROKEN_SUBMISSION_BINDING
        if (batch.records.size != expectedBinding.originalSelections.size) return FailureReason.INVALID_RECORD_COUNT_OR_ORDER
        if (batch.records.map { it.validationRecordId }.distinct().size != batch.records.size) return FailureReason.DUPLICATE_RECORD
        if (batch.records.any { !SHA256.matches(it.validationRecordId) }) return FailureReason.INVALID_RECORD_ID
        if (batch.records.map { it.originalDecision.reviewUnitId } != expectedBinding.originalSelections.map { it.reviewUnitId }) {
            return FailureReason.INVALID_RECORD_COUNT_OR_ORDER
        }
        if (batch.records.any { it.validatorReviewerRef != batch.validatorReviewerRef }) return FailureReason.VALIDATOR_MISMATCH
        if (batch.validationRound != HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.VALIDATION_ROUND ||
            batch.validationRevision != HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.VALIDATION_REVISION
        ) return FailureReason.ROUND_REVISION_MISMATCH

        val allEvidence = expectedBinding.evidenceBindings
        batch.records.forEach { record ->
            val unitEvidence = allEvidence.filter { it.reviewUnitId == record.originalDecision.reviewUnitId }
            if (record.evidenceReferenceIds.any { id -> unitEvidence.none { it.evidenceReferenceId == id } }) {
                return FailureReason.FOREIGN_EVIDENCE
            }
            if ((record.originalDecision.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION ||
                    record.originalDecision.decision == HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION) &&
                record.assessment == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION &&
                record.evidenceReferenceIds.isEmpty()
            ) return FailureReason.MISSING_EVIDENCE
            if (record.downstreamRoute !=
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.deriveRoute(
                    record.originalDecision.decision,
                    record.assessment,
                )
            ) return FailureReason.ROUTE_MISMATCH
        }
        return null
    }

    private fun stateFor(
        record: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1,
    ): State = when (record.assessment) {
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION ->
            when (record.originalDecision.decision) {
                HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION -> State.ELIGIBLE_POSITIVE_GOLD_CANDIDATE
                HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION -> State.ELIGIBLE_NEGATIVE_SUPERVISION_CANDIDATE
                else -> State.NOT_ELIGIBLE
            }
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.CHALLENGE_ORIGINAL_DECISION,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ESCALATE_VALIDATION_CONFLICT ->
            State.REQUIRES_ADJUDICATION
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ABSTAIN_VALIDATION_INSUFFICIENT_EVIDENCE ->
            State.NOT_ELIGIBLE
    }

    private fun decision(
        batch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1,
        record: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1,
        state: State,
    ) = Decision(
        decisionId = decisionId(batch, record, state),
        validationBatchId = batch.inputBinding.batchId,
        inputBindingDigest = batch.inputBinding.bindingDigest,
        validationBatchBindingDigest = batch.bindingDigest,
        validationBatchLogicalDigest = batch.logicalDigest,
        validationRecordId = record.validationRecordId,
        reviewUnitId = record.originalDecision.reviewUnitId,
        stableEntryId = record.originalDecision.stableEntryId,
        canonicalEntityId = record.originalDecision.canonicalEntityId,
        originalReviewerRef = batch.inputBinding.originalReviewerRef,
        validatorReviewerRef = record.validatorReviewerRef,
        validationRound = record.validationRound,
        validationRevision = record.validationRevision,
        originalDecision = record.originalDecision.decision,
        assessment = record.assessment,
        validationReasonCodes = record.reasonCodes,
        evidenceReferenceIds = record.evidenceReferenceIds,
        downstreamRoute = record.downstreamRoute,
        state = state,
    )

    private fun mapFailure(
        reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1,
    ): FailureReason = when (reason) {
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_CONTRACT_ID,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_CONTRACT_VERSION,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_STATE,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_INPUT_BINDING,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INPUT_BINDING_DIGEST_MISMATCH,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_BATCH_BINDING -> FailureReason.BROKEN_SUBMISSION_BINDING
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.UNKNOWN_REVIEW_UNIT,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.ORIGINAL_DECISION_MISMATCH,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.ORIGINAL_DECISION_IDENTITY_MISMATCH,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_VALIDATION_RECORD_ID -> FailureReason.INVALID_RECORD_ID
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_RECORD_COUNT,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_RECORD_ORDER -> FailureReason.INVALID_RECORD_COUNT_OR_ORDER
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_VALIDATOR_REVIEWER_REF,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.VALIDATOR_NOT_INDEPENDENT -> FailureReason.VALIDATOR_MISMATCH
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_VALIDATION_ROUND,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_VALIDATION_REVISION -> FailureReason.ROUND_REVISION_MISMATCH
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.DUPLICATE_VALIDATION_IDENTITY -> FailureReason.DUPLICATE_RECORD
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_ASSESSMENT -> FailureReason.INVALID_ASSESSMENT
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_REASON_CODES -> FailureReason.INVALID_REASON_CODE_COMBINATION
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_RATIONALE -> FailureReason.PROVENANCE_MISMATCH
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_EVIDENCE_REFERENCE,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.CROSS_UNIT_EVIDENCE_REFERENCE -> FailureReason.FOREIGN_EVIDENCE
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INSUFFICIENT_DIRECT_EVIDENCE -> FailureReason.MISSING_EVIDENCE
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.UNADDRESSED_CONTRADICTING_EVIDENCE -> FailureReason.INVALID_REASON_CODE_COMBINATION
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_DOWNSTREAM_ROUTE -> FailureReason.ROUTE_MISMATCH
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_COUNTERS,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.VALIDATION_RECORD_DIGEST_MISMATCH,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.VALIDATION_BINDING_DIGEST_MISMATCH,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.VALIDATION_LOGICAL_DIGEST_MISMATCH -> FailureReason.PROVENANCE_MISMATCH
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.FORBIDDEN_GOLD_SEMANTICS -> FailureReason.UNSUPPORTED_ROUTE
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.FORBIDDEN_NEGATIVE_SUPERVISION_SEMANTICS -> FailureReason.UNSUPPORTED_ROUTE
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.FORBIDDEN_AUTHORITY_MUTATION_SEMANTICS -> FailureReason.PROVENANCE_MISMATCH
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    enum class State(
        val isPositive: Boolean = false,
        val isNegative: Boolean = false,
    ) {
        ELIGIBLE_POSITIVE_GOLD_CANDIDATE(isPositive = true),
        ELIGIBLE_NEGATIVE_SUPERVISION_CANDIDATE(isNegative = true),
        REQUIRES_ADJUDICATION,
        NOT_ELIGIBLE,
    }

    enum class FailureReason {
        INVALID_VALIDATION_BATCH,
        INVALID_RECORD_COUNT_OR_ORDER,
        DUPLICATE_RECORD,
        INVALID_RECORD_ID,
        BROKEN_SUBMISSION_BINDING,
        BROKEN_PACKET_BINDING,
        VALIDATOR_MISMATCH,
        ROUND_REVISION_MISMATCH,
        INVALID_ASSESSMENT,
        INVALID_REASON_CODE_COMBINATION,
        MISSING_EVIDENCE,
        FOREIGN_EVIDENCE,
        ROUTE_MISMATCH,
        UNSUPPORTED_ROUTE,
        PROVENANCE_MISMATCH,
    }

    data class Decision(
        val decisionId: String,
        val validationBatchId: String,
        val inputBindingDigest: String,
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
        val state: State,
    )

    data class Counters(
        val totalDecisions: Int,
        val eligiblePositive: Int,
        val eligibleNegative: Int,
        val requiresAdjudication: Int,
        val notEligible: Int,
    ) {
        companion object {
            fun from(decisions: List<Decision>) = Counters(
                totalDecisions = decisions.size,
                eligiblePositive = decisions.count { it.state == State.ELIGIBLE_POSITIVE_GOLD_CANDIDATE },
                eligibleNegative = decisions.count { it.state == State.ELIGIBLE_NEGATIVE_SUPERVISION_CANDIDATE },
                requiresAdjudication = decisions.count { it.state == State.REQUIRES_ADJUDICATION },
                notEligible = decisions.count { it.state == State.NOT_ELIGIBLE },
            )
        }
    }

    data class Batch(
        val contractId: String,
        val version: String,
        val state: String,
        val validationBatchId: String,
        val inputBindingDigest: String,
        val validationBatchBindingDigest: String,
        val validationBatchLogicalDigest: String,
        val decisions: List<Decision>,
        val counters: Counters,
        val logicalDigest: String,
    )

    sealed interface Result {
        data class Completed(val value: Batch) : Result

        data class Failed(val reason: FailureReason) : Result
    }
}
