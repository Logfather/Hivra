package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Typed human-input boundary for an independent validation submission.
 * It does not materialize validation records, persist a batch, or select a downstream route.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionContractV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_SUBMISSION_V1"
    const val VERSION = "1"
    const val STATE = "HUMAN_INPUT_ONLY"
    private const val SUBMISSION_ID_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_SUBMISSION_ID_V1"
    private const val INPUT_BINDING_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_SUBMISSION_BINDING_V1"
    private const val LOGICAL_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_SUBMISSION_LOGICAL_V1"
    private const val ORIGINAL_REVIEWER_REF = "reviewer:logfather:v1"
    private const val VALIDATION_ROUND = 1
    private const val VALIDATION_REVISION = 1
    private const val MAX_RATIONALE_UTF8_BYTES = 4096
    private val VALIDATOR_REF = Regex("[A-Za-z0-9._:-]{1,128}")

    fun create(
        packet: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1,
        validatorReviewerRef: String?,
        validationRound: Int,
        validationRevision: Int,
        assessments: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionAssessmentInputV1>,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionResultV1 {
        val packetFailure = packetFailure(packet)
        if (packetFailure != null) return invalid(packetFailure)
        val validatorFailure = validatorFailure(validatorReviewerRef, validationRound, validationRevision)
        if (validatorFailure != null) return invalid(validatorFailure)
        val expectedById = packet.items.associateBy { it.reviewUnitId }
        if (assessments.size != packet.items.size) return invalid(SubmissionFailureReason.INVALID_ASSESSMENT_COUNT)
        if (assessments.map { it.reviewUnitId }.distinct().size != assessments.size) {
            return invalid(SubmissionFailureReason.DUPLICATE_REVIEW_UNIT)
        }
        if (assessments.any { it.reviewUnitId !in expectedById }) return invalid(SubmissionFailureReason.UNKNOWN_REVIEW_UNIT)
        assessments.forEach { input ->
            val expected = expectedById[input.reviewUnitId] ?: return invalid(SubmissionFailureReason.UNKNOWN_REVIEW_UNIT)
            unitBindingFailure(expected, input)?.let { return invalid(it) }
            assessmentFailure(expected, input)?.let { return invalid(it) }
        }
        if (assessments.map { it.reviewUnitId } != packet.items.map { it.reviewUnitId }) {
            return invalid(SubmissionFailureReason.INVALID_ASSESSMENT_ORDER)
        }
        val binding = packetBinding(packet)
        val unsigned = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionV1(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = STATE,
            submissionId = submissionId(packet.packetId, validatorReviewerRef!!, validationRound, validationRevision),
            packetBinding = binding,
            validatorReviewerRef = validatorReviewerRef,
            validationRound = validationRound,
            validationRevision = validationRevision,
            assessments = assessments,
            inputBindingDigest = "",
            logicalDigest = "",
        )
        val withBinding = unsigned.copy(inputBindingDigest = inputBindingDigest(unsigned))
        val submission = withBinding.copy(logicalDigest = logicalDigest(withBinding))
        return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionResultV1.Completed(submission)
    }

    fun validate(
        submission: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionValidationResultV1 {
        if (submission.contractId != CONTRACT_ID) return invalidValidation(SubmissionFailureReason.INVALID_CONTRACT_ID)
        if (submission.version != VERSION) return invalidValidation(SubmissionFailureReason.INVALID_CONTRACT_VERSION)
        if (submission.state != STATE) return invalidValidation(SubmissionFailureReason.INVALID_STATE)
        if (!VALIDATOR_REF.matches(submission.validatorReviewerRef)) return invalidValidation(SubmissionFailureReason.INVALID_VALIDATOR_REVIEWER)
        if (submission.validatorReviewerRef == submission.packetBinding.originalReviewerRef) return invalidValidation(SubmissionFailureReason.VALIDATOR_NOT_INDEPENDENT)
        if (submission.validationRound != VALIDATION_ROUND) return invalidValidation(SubmissionFailureReason.INVALID_VALIDATION_ROUND)
        if (submission.validationRevision != VALIDATION_REVISION) return invalidValidation(SubmissionFailureReason.INVALID_VALIDATION_REVISION)
        val units = submission.packetBinding.unitBindings
        if (units.isEmpty() || units.map { it.reviewUnitId }.distinct().size != units.size) return invalidValidation(SubmissionFailureReason.INVALID_PACKET_BINDING)
        if (submission.assessments.size != units.size) return invalidValidation(SubmissionFailureReason.INVALID_ASSESSMENT_COUNT)
        if (submission.assessments.map { it.reviewUnitId } != units.map { it.reviewUnitId }) return invalidValidation(SubmissionFailureReason.INVALID_ASSESSMENT_ORDER)
        submission.assessments.forEachIndexed { index, input ->
            val expected = units[index]
            if (unitBindingFailure(expected, input) != null) return invalidValidation(SubmissionFailureReason.UNIT_BINDING_MISMATCH)
            if (input.reasonCodes.isEmpty() || input.reasonCodes != input.reasonCodes.distinct().sortedBy { it.ordinal }) {
                return invalidValidation(SubmissionFailureReason.INVALID_REASON_CODES)
            }
            if (input.evidenceReferenceIds != input.evidenceReferenceIds.distinct().sorted()) {
                return invalidValidation(SubmissionFailureReason.INVALID_EVIDENCE_REFERENCES)
            }
            if (!validRationale(input.rationale)) return invalidValidation(SubmissionFailureReason.INVALID_RATIONALE)
        }
        if (submission.submissionId != submissionId(submission.packetBinding.packetId, submission.validatorReviewerRef, submission.validationRound, submission.validationRevision)) {
            return invalidValidation(SubmissionFailureReason.SUBMISSION_ID_MISMATCH)
        }
        if (!SHA256.matches(submission.inputBindingDigest) || submission.inputBindingDigest != inputBindingDigest(submission)) {
            return invalidValidation(SubmissionFailureReason.INPUT_BINDING_DIGEST_MISMATCH)
        }
        if (!SHA256.matches(submission.logicalDigest) || submission.logicalDigest != logicalDigest(submission)) {
            return invalidValidation(SubmissionFailureReason.LOGICAL_DIGEST_MISMATCH)
        }
        return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionValidationResultV1.Valid
    }

    fun submissionId(
        packetId: String,
        validatorReviewerRef: String,
        validationRound: Int,
        validationRevision: Int,
    ): String = sha256(buildString {
        appendLine(SUBMISSION_ID_DOMAIN)
        appendLine("packetId=$packetId")
        appendLine("validatorReviewerRef=$validatorReviewerRef")
        appendLine("validationRound=$validationRound")
        appendLine("validationRevision=$validationRevision")
    })

    fun inputBindingDigest(
        submission: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionV1,
    ): String = sha256(buildString {
        appendLine(INPUT_BINDING_DIGEST_DOMAIN)
        appendLine("contractId=${submission.contractId}")
        appendLine("version=${submission.version}")
        appendLine("packetBinding=${submission.packetBinding}")
        appendLine("validatorReviewerRef=${submission.validatorReviewerRef}")
        appendLine("validationRound=${submission.validationRound}")
        appendLine("validationRevision=${submission.validationRevision}")
        submission.assessments.forEach { input ->
            appendLine("reviewUnitId=${input.reviewUnitId}")
            appendLine("stableEntryId=${input.stableEntryId}")
            appendLine("canonicalEntityId=${input.canonicalEntityId}")
            appendLine("originalDecision=${input.originalDecision.name}")
        }
    })

    fun logicalDigest(
        submission: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionV1,
    ): String = sha256(buildString {
        appendLine(LOGICAL_DIGEST_DOMAIN)
        appendLine("inputBindingDigest=${submission.inputBindingDigest}")
        appendLine("submissionId=${submission.submissionId}")
        submission.assessments.forEach { input ->
            appendLine("reviewUnitId=${input.reviewUnitId}")
            appendLine("assessment=${input.assessment.name}")
            input.reasonCodes.forEach { appendLine("reasonCode=${it.name}") }
            input.evidenceReferenceIds.forEach { appendLine("evidenceReferenceId=$it") }
            appendLine("rationale=${input.rationale}")
        }
    })

    private fun packetFailure(
        packet: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1,
    ): SubmissionFailureReason? {
        if (packet.contractId != HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.CONTRACT_ID ||
            packet.version != HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.VERSION ||
            packet.state != HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.STATE ||
            packet.packetId.isBlank() || packet.items.isEmpty()
        ) return SubmissionFailureReason.INVALID_PACKET
        if (packet.items.map { it.reviewUnitId }.distinct().size != packet.items.size) return SubmissionFailureReason.INVALID_PACKET
        if (packet.items.any { it.reviewUnitId.isBlank() || it.stableEntryId.isBlank() || it.canonicalEntityId.isBlank() }) return SubmissionFailureReason.INVALID_PACKET
        if (packet.allowedValidationAssessments != HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.VALIDATION_ASSESSMENTS ||
            packet.allowedValidationReasonCodes != HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketContractV1.VALIDATION_REASON_CODES
        ) return SubmissionFailureReason.INVALID_PACKET
        return null
    }

    private fun validatorFailure(
        validatorReviewerRef: String?,
        validationRound: Int,
        validationRevision: Int,
    ): SubmissionFailureReason? {
        if (validatorReviewerRef == null || !VALIDATOR_REF.matches(validatorReviewerRef)) return SubmissionFailureReason.INVALID_VALIDATOR_REVIEWER
        if (validatorReviewerRef == ORIGINAL_REVIEWER_REF) return SubmissionFailureReason.VALIDATOR_NOT_INDEPENDENT
        if (validationRound != VALIDATION_ROUND) return SubmissionFailureReason.INVALID_VALIDATION_ROUND
        if (validationRevision != VALIDATION_REVISION) return SubmissionFailureReason.INVALID_VALIDATION_REVISION
        return null
    }

    private fun unitBindingFailure(
        expected: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1,
        input: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionAssessmentInputV1,
    ): SubmissionFailureReason? = unitBindingFailure(
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionUnitBindingV1(
            expected.reviewUnitId,
            expected.stableEntryId,
            expected.canonicalEntityId,
            expected.originalDecision,
        ),
        input,
    )

    private fun unitBindingFailure(
        expected: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionUnitBindingV1,
        input: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionAssessmentInputV1,
    ): SubmissionFailureReason? {
        if (input.reviewUnitId != expected.reviewUnitId ||
            input.stableEntryId != expected.stableEntryId ||
            input.canonicalEntityId != expected.canonicalEntityId ||
            input.originalDecision != expected.originalDecision
        ) return SubmissionFailureReason.UNIT_BINDING_MISMATCH
        return null
    }

    private fun assessmentFailure(
        expected: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketItemV1,
        input: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionAssessmentInputV1,
    ): SubmissionFailureReason? {
        if (input.assessment !in expected.allowedValidationAssessments) return SubmissionFailureReason.INVALID_ASSESSMENT
        if (input.reasonCodes.isEmpty() ||
            input.reasonCodes.any { it !in expected.allowedValidationReasonCodes } ||
            input.reasonCodes != input.reasonCodes.distinct().sortedBy { it.ordinal }
        ) return SubmissionFailureReason.INVALID_REASON_CODES
        val expectedEvidence = expected.directEvidence.map { it.evidenceReferenceId }.sorted()
        if (input.evidenceReferenceIds.any { it !in expectedEvidence } || input.evidenceReferenceIds != input.evidenceReferenceIds.distinct().sorted()) {
            return SubmissionFailureReason.INVALID_EVIDENCE_REFERENCES
        }
        if (!validRationale(input.rationale)) return SubmissionFailureReason.INVALID_RATIONALE
        val evidence = expected.directEvidence.filter { it.evidenceReferenceId in input.evidenceReferenceIds }
        val support = evidence.any {
            it.directness == HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT &&
                it.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION
        }
        val contradiction = evidence.any {
            it.directness == HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT &&
                it.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION
        }
        val unitHasContradiction = expected.directEvidence.any {
            it.directness == HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT &&
                it.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION
        }
        return when (input.assessment) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION -> {
                when (input.originalDecision) {
                    HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION -> if (
                        !support || unitHasContradiction || input.reasonCodes.toSet() != setOf(
                            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_SUPPORTS_ORIGINAL_DECISION,
                            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.ORIGINAL_REASON_CODES_SUPPORTED,
                        )
                    ) SubmissionFailureReason.INVALID_REASON_CODES else null
                    HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION -> if (
                        !contradiction || input.reasonCodes.toSet() != setOf(
                            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_CONTRADICTS_ORIGINAL_DECISION,
                            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.ORIGINAL_REASON_CODES_SUPPORTED,
                        )
                    ) SubmissionFailureReason.INVALID_REASON_CODES else null
                    else -> SubmissionFailureReason.INVALID_ASSESSMENT
                }
            }
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.CHALLENGE_ORIGINAL_DECISION,
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ESCALATE_VALIDATION_CONFLICT -> if (
                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.REQUIRES_ADJUDICATION !in input.reasonCodes ||
                input.reasonCodes.none {
                    it == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.CONFLICTING_DIRECT_EVIDENCE ||
                        it == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DECISION_EVIDENCE_BINDING_MISMATCH ||
                        it == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.REVIEW_UNIT_SCOPE_MISMATCH
                }
            ) SubmissionFailureReason.INVALID_REASON_CODES else null
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ABSTAIN_VALIDATION_INSUFFICIENT_EVIDENCE -> if (
                input.reasonCodes != listOf(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.INSUFFICIENT_DIRECT_EVIDENCE)
            ) SubmissionFailureReason.INVALID_REASON_CODES else null
        }
    }

    private fun packetBinding(
        packet: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationPacketV1,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionPacketBindingV1(
        packetId = packet.packetId,
        packetInputBindingDigest = packet.inputBinding.reviewPacketInputBindingDigest,
        packetBindingDigest = packet.packetBindingDigest,
        packetLogicalDigest = packet.packetLogicalDigest,
        originalReviewerRef = packet.inputBinding.originalReviewerRef,
        originalReviewRound = packet.inputBinding.originalReviewRound,
        originalRevision = packet.inputBinding.originalRevision,
        unitBindings = packet.items.map {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionUnitBindingV1(
                it.reviewUnitId,
                it.stableEntryId,
                it.canonicalEntityId,
                it.originalDecision,
            )
        },
    )

    private fun validRationale(rationale: String): Boolean {
        val normalized = rationale.trim().replace(Regex("\\s+"), " ")
        return rationale.isNotBlank() && rationale == normalized &&
            rationale.toByteArray(StandardCharsets.UTF_8).size <= MAX_RATIONALE_UTF8_BYTES &&
            !rationale.contains('\u0000') && !rationale.startsWith('/') && !rationale.contains('\\') &&
            !rationale.contains("exception", ignoreCase = true) &&
            !rationale.contains("throwable", ignoreCase = true) &&
            !rationale.contains("stacktrace", ignoreCase = true)
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun invalid(reason: SubmissionFailureReason) =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionResultV1.Invalid(reason)

    private fun invalidValidation(reason: SubmissionFailureReason) =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionValidationResultV1.Invalid(reason)

    private val SHA256 = Regex("[0-9a-f]{64}")
}

enum class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1 {
    INVALID_CONTRACT_ID,
    INVALID_CONTRACT_VERSION,
    INVALID_STATE,
    INVALID_PACKET,
    INVALID_PACKET_BINDING,
    INVALID_VALIDATOR_REVIEWER,
    VALIDATOR_NOT_INDEPENDENT,
    INVALID_VALIDATION_ROUND,
    INVALID_VALIDATION_REVISION,
    INVALID_ASSESSMENT_COUNT,
    INVALID_ASSESSMENT_ORDER,
    DUPLICATE_REVIEW_UNIT,
    UNKNOWN_REVIEW_UNIT,
    UNIT_BINDING_MISMATCH,
    INVALID_ASSESSMENT,
    INVALID_REASON_CODES,
    INVALID_EVIDENCE_REFERENCES,
    INVALID_RATIONALE,
    SUBMISSION_ID_MISMATCH,
    INPUT_BINDING_DIGEST_MISMATCH,
    LOGICAL_DIGEST_MISMATCH,
}

private typealias SubmissionFailureReason = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1

sealed interface HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionResultV1 {
    data class Completed(
        val submission: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionV1,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionResultV1

    data class Invalid(
        val reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionResultV1
}

sealed interface HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionValidationResultV1 {
    data object Valid : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionValidationResultV1

    data class Invalid(
        val reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionFailureReasonV1,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionValidationResultV1
}

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionPacketBindingV1(
    val packetId: String,
    val packetInputBindingDigest: String,
    val packetBindingDigest: String,
    val packetLogicalDigest: String,
    val originalReviewerRef: String,
    val originalReviewRound: Int,
    val originalRevision: Int,
    val unitBindings: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionUnitBindingV1>,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionUnitBindingV1(
    val reviewUnitId: String,
    val stableEntryId: String,
    val canonicalEntityId: String,
    val originalDecision: HimZeroCandidateRecoveryHumanReviewDecisionV1,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionAssessmentInputV1(
    val reviewUnitId: String,
    val stableEntryId: String,
    val canonicalEntityId: String,
    val originalDecision: HimZeroCandidateRecoveryHumanReviewDecisionV1,
    val assessment: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1,
    val reasonCodes: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1>,
    val evidenceReferenceIds: List<String>,
    val rationale: String,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionV1(
    val contractId: String,
    val version: String,
    val state: String,
    val submissionId: String,
    val packetBinding: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionPacketBindingV1,
    val validatorReviewerRef: String,
    val validationRound: Int,
    val validationRevision: Int,
    val assessments: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationSubmissionAssessmentInputV1>,
    val inputBindingDigest: String,
    val logicalDigest: String,
)
