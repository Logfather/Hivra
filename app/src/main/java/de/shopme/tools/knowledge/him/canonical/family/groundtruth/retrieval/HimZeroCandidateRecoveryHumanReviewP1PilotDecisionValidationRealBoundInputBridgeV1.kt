package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import java.io.File

/**
 * Prepares the already existing independent-validation runtime request.
 * This bridge stops before runtime execution, batch creation, persistence, and
 * any downstream authorization.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_REAL_BOUND_INPUT_BRIDGE_V1"
    const val VERSION = "1"
    const val STATE = "REAL_BOUND_INPUT_PREPARATION_ONLY"

    private const val EXPECTED_ASSESSMENT_COUNT = 4
    private const val ORIGINAL_REVIEWER_REF = "reviewer:logfather:v1"
    private val REVIEWER_REF = Regex("[A-Za-z0-9._:-]{1,128}")
    private val BATCH_ID = Regex("[a-z0-9][a-z0-9-]{0,63}")

    fun prepare(
        request: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeRequestV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeResultV1 {
        if (!request.enabled) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeResultV1.Disabled
        if (request.assessments.size != EXPECTED_ASSESSMENT_COUNT) {
            return failed(BridgeFailureReason.INVALID_ASSESSMENT_COUNT, "assessments")
        }
        val validatorReviewerRef = request.validatorReviewerRef
            ?: return failed(BridgeFailureReason.INVALID_VALIDATOR_REVIEWER, "validator")
        if (validatorReviewerRef.isBlank() || !REVIEWER_REF.matches(validatorReviewerRef)) {
            return failed(BridgeFailureReason.INVALID_VALIDATOR_REVIEWER, "validator")
        }
        if (validatorReviewerRef == ORIGINAL_REVIEWER_REF) {
            return failed(BridgeFailureReason.VALIDATOR_NOT_INDEPENDENT, "validator")
        }
        if (!validBatchId(request.validationBatchId)) {
            return failed(BridgeFailureReason.INVALID_VALIDATION_BATCH_ID, "batchId")
        }
        if (request.durableRoot.path.isBlank() || request.reportRoot.path.isBlank()) {
            return failed(BridgeFailureReason.INVALID_ROOT_CONFIGURATION, "roots")
        }

        when (
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.validate(
                request.packet,
            )
        ) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketValidationResultV1.Valid -> Unit
            is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketValidationResultV1.Invalid ->
                return failed(BridgeFailureReason.INVALID_PACKET, "packet")
        }

        when (
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.validate(
                request.originalDecisionBatch,
            )
        ) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionValidationResultV1.Valid -> Unit
            is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionValidationResultV1.Invalid ->
                return failed(BridgeFailureReason.INVALID_ORIGINAL_DECISION_BATCH, "originalBatch")
        }

        val inputBinding =
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.FROZEN_INPUT_BINDING
        if (
            request.originalDecisionBatch.selections != inputBinding.originalSelections ||
            request.originalDecisionBatch.reviewerRef != inputBinding.originalReviewerRef ||
            request.originalDecisionBatch.reviewRound != inputBinding.originalReviewRound ||
            request.originalDecisionBatch.selections.any { it.revision != inputBinding.originalRevision }
        ) {
            return failed(BridgeFailureReason.ORIGINAL_BATCH_BINDING_MISMATCH, "originalBatch")
        }

        val submission = when (
            val result = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1.create(
                packet = request.packet,
                validatorReviewerRef = validatorReviewerRef,
                validationRound = request.validationRound,
                validationRevision = request.validationRevision,
                assessments = request.assessments,
            )
        ) {
            is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionResultV1.Completed -> result.submission
            is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionResultV1.Invalid ->
                return failed(BridgeFailureReason.SUBMISSION_CREATION_FAILED, "submission")
        }

        val records = when (
            val result = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordMaterializationV1.materialize(
                submission,
            )
        ) {
            is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordMaterializationResultV1.Completed -> result.records
            is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordMaterializationResultV1.Failed ->
                return failed(BridgeFailureReason.RECORD_MATERIALIZATION_FAILED, "records")
        }
        if (records.size != EXPECTED_ASSESSMENT_COUNT) {
            return failed(BridgeFailureReason.INVALID_MATERIALIZED_RECORDS, "records")
        }
        if (records.map { it.originalDecision.reviewUnitId } != request.packet.items.map { it.reviewUnitId }) {
            return failed(BridgeFailureReason.INVALID_MATERIALIZED_RECORDS, "records")
        }
        if (records.any {
                it.validatorReviewerRef != submission.validatorReviewerRef ||
                    it.validationRound != submission.validationRound ||
                    it.validationRevision != submission.validationRevision
            }
        ) {
            return failed(BridgeFailureReason.INVALID_MATERIALIZED_RECORDS, "records")
        }

        val runtimeRequest = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeRequestV1(
            enabled = true,
            validationBatchId = request.validationBatchId,
            inputBinding = inputBinding,
            originalDecisionBatch = request.originalDecisionBatch,
            validatorReviewerRef = submission.validatorReviewerRef,
            validationRound = submission.validationRound,
            validationRevision = submission.validationRevision,
            submittedValidationRecords = records,
            durableRoot = request.durableRoot,
            reportRoot = request.reportRoot,
        )
        return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeResultV1.Prepared(
            submission = submission,
            records = records,
            runtimeRequest = runtimeRequest,
        )
    }

    private fun validBatchId(value: String): Boolean =
        BATCH_ID.matches(value) && !value.endsWith('-') && !value.contains("..") && value.none { it.isWhitespace() }

    private fun failed(
        reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeFailureReasonV1,
        safeContext: String,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeResultV1.Failed(
        reason = reason,
        safeContext = safeContext,
    )
}

enum class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeFailureReasonV1 {
    INVALID_ASSESSMENT_COUNT,
    INVALID_VALIDATOR_REVIEWER,
    VALIDATOR_NOT_INDEPENDENT,
    INVALID_VALIDATION_BATCH_ID,
    INVALID_ROOT_CONFIGURATION,
    INVALID_PACKET,
    INVALID_ORIGINAL_DECISION_BATCH,
    ORIGINAL_BATCH_BINDING_MISMATCH,
    SUBMISSION_CREATION_FAILED,
    RECORD_MATERIALIZATION_FAILED,
    INVALID_MATERIALIZED_RECORDS,
}

private typealias BridgeFailureReason =
    HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeFailureReasonV1

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeRequestV1(
    val enabled: Boolean,
    val validatorReviewerRef: String?,
    val assessments: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionAssessmentInputV1>,
    val packet: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1,
    val originalDecisionBatch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1,
    val validationBatchId: String,
    val validationRound: Int,
    val validationRevision: Int,
    val durableRoot: File,
    val reportRoot: File,
)

sealed interface HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeResultV1 {
    data object Disabled : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeResultV1

    data class Prepared(
        val submission: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionV1,
        val records: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1>,
        val runtimeRequest: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeRequestV1,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeResultV1

    data class Failed(
        val reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeFailureReasonV1,
        val safeContext: String,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRealBoundInputBridgeResultV1
}
