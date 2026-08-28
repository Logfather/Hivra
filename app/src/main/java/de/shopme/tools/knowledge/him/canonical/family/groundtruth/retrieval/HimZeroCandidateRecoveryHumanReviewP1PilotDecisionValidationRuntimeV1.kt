package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import java.io.File

/**
 * Deterministic local runtime for independently validating the committed P1 decisions.
 * It creates no decisions, performs no inference, and grants no downstream authority.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_RUNTIME_V1"
    const val RUNTIME_CONTRACT_ID = CONTRACT_ID
    const val VERSION = "1"
    const val RUNTIME_VERSION = VERSION
    const val STATE = "INDEPENDENT_DECISION_VALIDATION_CONTEXT_ONLY"

    private val BATCH_ID = Regex("[a-z0-9][a-z0-9-]{0,63}")

    fun execute(
        request: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeRequestV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1 {
        if (!request.enabled) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1.Disabled
        return try {
            validateRequestShape(request)
            validateBatchId(request.validationBatchId)
            validateOriginalDecisionBatch(request.originalDecisionBatch)
            validateInputBinding(request.inputBinding, request.originalDecisionBatch)
            validateValidator(request)
            val expectedSelections = request.inputBinding.originalSelections
            if (request.submittedValidationRecords.size != expectedSelections.size) {
                fail(RuntimeFailureReason.INVALID_RECORD_COUNT, "records")
            }
            if (request.submittedValidationRecords.map { it.originalDecision.reviewUnitId } != expectedSelections.map { it.reviewUnitId }) {
                fail(RuntimeFailureReason.INVALID_RECORD_ORDER, "records")
            }
            request.submittedValidationRecords.forEachIndexed { index, record ->
                val expected = expectedSelections[index]
                if (record.originalDecision != expected) fail(RuntimeFailureReason.ORIGINAL_DECISION_MISMATCH, "record")
                if (record.validatorReviewerRef != request.validatorReviewerRef) {
                    fail(RuntimeFailureReason.VALIDATION_RECORD_BINDING_MISMATCH, "record")
                }
                if (record.validationRound != request.validationRound) {
                    fail(RuntimeFailureReason.INVALID_VALIDATION_ROUND, "record")
                }
                if (record.validationRevision != request.validationRevision) {
                    fail(RuntimeFailureReason.INVALID_VALIDATION_REVISION, "record")
                }
                val expectedRecordId = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.validationRecordId(
                    request.inputBinding,
                    expected,
                    request.validatorReviewerRef,
                    request.validationRound,
                    request.validationRevision,
                )
                if (record.validationRecordId != expectedRecordId) {
                    fail(RuntimeFailureReason.VALIDATION_RECORD_BINDING_MISMATCH, "record")
                }
            }

            val validationBatch = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.createBatch(
                inputBinding = request.inputBinding,
                validatorReviewerRef = request.validatorReviewerRef,
                validationRound = request.validationRound,
                validationRevision = request.validationRevision,
                records = request.submittedValidationRecords,
            )
            when (
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.validate(validationBatch)
            ) {
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationResultV1.Valid -> Unit
                is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationResultV1.Invalid ->
                    fail(RuntimeFailureReason.INVALID_VALIDATION_RECORD, "validationBatch")
            }

            val persistence = when (
                val result = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.execute(
                    HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceRequestV1(
                        enabled = true,
                        durableRoot = request.durableRoot,
                        reportRoot = request.reportRoot,
                        validationBatchId = request.validationBatchId,
                        validationBatch = validationBatch,
                    ),
                )
            ) {
                is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1.Completed -> result
                is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1.Failed ->
                    fail(RuntimeFailureReason.PERSISTENCE_FAILED, "persistence")
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceResultV1.Disabled ->
                    fail(RuntimeFailureReason.PERSISTENCE_FAILED, "persistence")
            }

            val durableFile = request.durableRoot.toPath().resolve(persistence.durableRelativePath).toFile()
            val reloaded = try {
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceV1.readValidationBatch(durableFile)
            } catch (_: Throwable) {
                fail(RuntimeFailureReason.PERSISTENCE_RELOAD_MISMATCH, "reload")
            }
            if (reloaded != validationBatch || reloaded != persistence.validationBatch) {
                fail(RuntimeFailureReason.PERSISTENCE_RELOAD_MISMATCH, "reload")
            }

            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1.Completed(
                runtimeContractId = CONTRACT_ID,
                runtimeVersion = VERSION,
                validationBatchId = persistence.validationBatchId,
                state = validationBatch.state,
                validatorReviewerRef = validationBatch.validatorReviewerRef,
                validationRound = validationBatch.validationRound,
                validationRevision = validationBatch.validationRevision,
                validationBatch = reloaded,
                counters = validationBatch.counters,
                persistenceStatus = persistence.status,
                durableRelativePath = persistence.durableRelativePath,
                reportJsonRelativePath = persistence.reportJsonRelativePath,
                reportTextRelativePath = persistence.reportTextRelativePath,
                inputBindingDigest = validationBatch.inputBinding.bindingDigest,
                validationBatchBindingDigest = persistence.validationBatchBindingDigest,
                validationBatchLogicalDigest = persistence.validationBatchLogicalDigest,
                durableJsonSha256 = persistence.durableJsonSha256,
                durableJsonByteSize = persistence.durableJsonByteSize,
                reportJsonSha256 = persistence.reportJsonSha256,
                reportJsonByteSize = persistence.reportJsonByteSize,
                reportTextSha256 = persistence.reportTextSha256,
                reportTextByteSize = persistence.reportTextByteSize,
            )
        } catch (failure: RuntimeFailure) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1.Failed(
                failure.reason,
                failure.safeContext,
            )
        } catch (_: Throwable) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1.Failed(
                RuntimeFailureReason.INTERNAL_INVARIANT_VIOLATION,
                "runtime",
            )
        }
    }

    private fun validateRequestShape(
        request: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeRequestV1,
    ) {
        if (request.validationBatchId.isBlank()) fail(RuntimeFailureReason.INVALID_REQUEST, "batchId")
        if (request.durableRoot.path.isBlank() || request.reportRoot.path.isBlank()) {
            fail(RuntimeFailureReason.INVALID_REQUEST, "roots")
        }
    }

    private fun validateBatchId(batchId: String) {
        if (!BATCH_ID.matches(batchId) || batchId.endsWith('-') || batchId.contains("..") || batchId.any { it.isWhitespace() }) {
            fail(RuntimeFailureReason.INVALID_VALIDATION_BATCH_ID, "batchId")
        }
    }

    private fun validateOriginalDecisionBatch(
        batch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1,
    ) {
        when (HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.validate(batch)) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionValidationResultV1.Valid -> Unit
            is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionValidationResultV1.Invalid ->
                fail(RuntimeFailureReason.INVALID_ORIGINAL_DECISION_BATCH, "originalBatch")
        }
    }

    private fun validateInputBinding(
        binding: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationInputBindingV1,
        originalBatch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1,
    ) {
        if (binding != HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.FROZEN_INPUT_BINDING) {
            fail(RuntimeFailureReason.INVALID_INPUT_BINDING, "inputBinding")
        }
        if (originalBatch.selections != binding.originalSelections ||
            originalBatch.reviewerRef != binding.originalReviewerRef ||
            originalBatch.reviewRound != binding.originalReviewRound ||
            originalBatch.selections.any { it.revision != binding.originalRevision }
        ) {
            fail(RuntimeFailureReason.ORIGINAL_BATCH_BINDING_MISMATCH, "originalBatch")
        }
    }

    private fun validateValidator(
        request: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeRequestV1,
    ) {
        if (!VALIDATOR_REF.matches(request.validatorReviewerRef)) {
            fail(RuntimeFailureReason.INVALID_VALIDATOR_REVIEWER, "validator")
        }
        if (request.validatorReviewerRef == request.inputBinding.originalReviewerRef) {
            fail(RuntimeFailureReason.VALIDATOR_NOT_INDEPENDENT, "validator")
        }
        if (request.validationRound != 1) fail(RuntimeFailureReason.INVALID_VALIDATION_ROUND, "round")
        if (request.validationRevision != 1) fail(RuntimeFailureReason.INVALID_VALIDATION_REVISION, "revision")
    }

    private fun fail(reason: RuntimeFailureReason, safeContext: String): Nothing =
        throw RuntimeFailure(reason, safeContext)

    private class RuntimeFailure(
        val reason: RuntimeFailureReason,
        val safeContext: String,
    ) : IllegalArgumentException()

    private val VALIDATOR_REF = Regex("[A-Za-z0-9._:-]{1,128}")
}

enum class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeFailureReasonV1 {
    INVALID_REQUEST,
    INVALID_VALIDATION_BATCH_ID,
    INVALID_ORIGINAL_DECISION_BATCH,
    INVALID_INPUT_BINDING,
    ORIGINAL_BATCH_BINDING_MISMATCH,
    INVALID_VALIDATOR_REVIEWER,
    VALIDATOR_NOT_INDEPENDENT,
    INVALID_VALIDATION_ROUND,
    INVALID_VALIDATION_REVISION,
    INVALID_RECORD_COUNT,
    INVALID_RECORD_ORDER,
    UNKNOWN_REVIEW_UNIT,
    ORIGINAL_DECISION_MISMATCH,
    VALIDATION_RECORD_BINDING_MISMATCH,
    INVALID_VALIDATION_RECORD,
    PERSISTENCE_FAILED,
    PERSISTENCE_RELOAD_MISMATCH,
    INTERNAL_INVARIANT_VIOLATION,
}

private typealias RuntimeFailureReason = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeFailureReasonV1

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeRequestV1(
    val enabled: Boolean,
    val validationBatchId: String,
    val inputBinding: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationInputBindingV1,
    val originalDecisionBatch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionV1,
    val validatorReviewerRef: String,
    val validationRound: Int,
    val validationRevision: Int,
    val submittedValidationRecords: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1>,
    val durableRoot: File,
    val reportRoot: File,
)

sealed interface HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1 {
    data object Disabled : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1

    data class Completed(
        val runtimeContractId: String,
        val runtimeVersion: String,
        val validationBatchId: String,
        val state: String,
        val validatorReviewerRef: String,
        val validationRound: Int,
        val validationRevision: Int,
        val validationBatch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1,
        val counters: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationCountersV1,
        val persistenceStatus: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPersistenceStatusV1,
        val durableRelativePath: String,
        val reportJsonRelativePath: String,
        val reportTextRelativePath: String,
        val inputBindingDigest: String,
        val validationBatchBindingDigest: String,
        val validationBatchLogicalDigest: String,
        val durableJsonSha256: String,
        val durableJsonByteSize: Long,
        val reportJsonSha256: String,
        val reportJsonByteSize: Long,
        val reportTextSha256: String,
        val reportTextByteSize: Long,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1

    data class Failed(
        val reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeFailureReasonV1,
        val safeContext: String,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRuntimeResultV1
}
