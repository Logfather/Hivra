package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

/**
 * Pure bridge from a validated P1 validation submission to the existing typed
 * independent-validation records. It deliberately stops before batch creation,
 * persistence, runtime execution, and downstream route authorization.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordMaterializationV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_RECORD_MATERIALIZATION_V1"
    const val VERSION = "1"
    const val STATE = "RECORD_MATERIALIZATION_ONLY"

    private const val EXPECTED_RECORD_COUNT = 4

    fun materialize(
        submission: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordMaterializationResultV1 {
        when (
            val validation = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1.validate(
                submission,
            )
        ) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionValidationResultV1.Valid -> Unit
            is HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionValidationResultV1.Invalid ->
                return failed(validation.reason)
        }

        val binding = submission.packetBinding
        if (
            binding.packetId != HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.PACKET_ID ||
            binding.packetInputBindingDigest != HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.REVIEW_PACKET_INPUT_BINDING_DIGEST ||
            binding.packetBindingDigest != HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.REVIEW_PACKET_BINDING_DIGEST ||
            binding.packetLogicalDigest != HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.REVIEW_PACKET_LOGICAL_DIGEST ||
            binding.originalReviewerRef != HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.ORIGINAL_REVIEWER_REF ||
            binding.originalReviewRound != HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.VALIDATION_ROUND ||
            binding.originalRevision != HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.VALIDATION_REVISION
        ) return failed(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.INVALID_PACKET_BINDING)

        if (
            submission.assessments.size != EXPECTED_RECORD_COUNT ||
            binding.unitBindings.size != EXPECTED_RECORD_COUNT
        ) return failed(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.INVALID_ASSESSMENT_COUNT)

        val expectedSelections = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.FROZEN_INPUT_BINDING.originalSelections
        if (binding.unitBindings.mapIndexed { index, unit ->
                val expected = expectedSelections[index]
                unit.reviewUnitId == expected.reviewUnitId &&
                    unit.stableEntryId == expected.stableEntryId &&
                    unit.canonicalEntityId == expected.canonicalEntityId &&
                    unit.originalDecision == expected.decision
            }.any { !it }
        ) {
            return failed(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.UNIT_BINDING_MISMATCH)
        }
        if (submission.assessments.map { it.reviewUnitId } != binding.unitBindings.map { it.reviewUnitId }) {
            return failed(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.INVALID_ASSESSMENT_ORDER)
        }

        val records = submission.assessments.mapIndexed { index, input ->
            val unit = binding.unitBindings[index]
            val originalDecision = expectedSelections[index]
            if (
                input.stableEntryId != unit.stableEntryId ||
                input.canonicalEntityId != unit.canonicalEntityId ||
                input.originalDecision != originalDecision.decision
            ) return failed(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1.UNIT_BINDING_MISMATCH)

            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.createValidationRecord(
                inputBinding = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.FROZEN_INPUT_BINDING,
                originalDecision = originalDecision,
                validatorReviewerRef = submission.validatorReviewerRef,
                validationRound = submission.validationRound,
                validationRevision = submission.validationRevision,
                assessment = input.assessment,
                reasonCodes = input.reasonCodes,
                evidenceReferenceIds = input.evidenceReferenceIds,
                rationale = input.rationale,
            )
        }
        return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordMaterializationResultV1.Completed(records)
    }

    private fun failed(
        reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordMaterializationResultV1.Failed(reason)
}

sealed interface HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordMaterializationResultV1 {
    data class Completed(
        val records: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1>,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordMaterializationResultV1

    data class Failed(
        val reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordMaterializationResultV1
}
