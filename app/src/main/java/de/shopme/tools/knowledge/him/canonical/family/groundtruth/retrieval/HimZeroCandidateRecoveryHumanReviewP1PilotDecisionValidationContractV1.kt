package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Pure, in-memory contract for an independent second validation of the committed P1 decisions.
 * This contract has no persistence, source access, reviewer assignment, or downstream mutation.
 */
object HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1 {
    const val CONTRACT_ID =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_CONTRACT_V1"
    const val VERSION = "1"
    const val STATE = "INDEPENDENT_DECISION_VALIDATION_CONTEXT_ONLY"
    const val INPUT_BINDING_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_BINDING_V1"
    const val RECORD_ID_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_RECORD_V1"
    const val BATCH_LOGICAL_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_LOGICAL_V1"
    const val BATCH_BINDING_DIGEST_DOMAIN =
        "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_P1_PILOT_DECISION_VALIDATION_BATCH_BINDING_V1"
    const val BATCH_ID = "p1-artischocken-brie-logfather-r1-v1"
    const val BATCH_FILE_PATH =
        "data/knowledge/him/canonical-family/human-review/zero-candidate-recovery/v1/decision-batches/p1-artischocken-brie-logfather-r1-v1/review-decisions.v1.json"
    const val BATCH_FILE_SIZE = 29094L
    const val BATCH_FILE_SHA256 =
        "2a892ea6591967fdcbb439f63a3c1d3ef04d041ac10af4863c7c22d4ff140342"
    const val ORIGINAL_INPUT_BINDING_DIGEST =
        "12021b21c18e9394cbb9d27a645ddeaf922675cd1dd3cfb1f5e3a63a83bedc23"
    const val ORIGINAL_BATCH_LOGICAL_DIGEST =
        "53052a8bcceafdade7c7c597750e3497d8ca377369915502257c1427f8106c02"
    const val SUBMISSION_ID = "p1-artischocken-herzen-brie-double-creme-reviewer-logfather-r1-v1"
    const val ORIGINAL_REVIEWER_REF = "reviewer:logfather:v1"
    const val VALIDATION_ROUND = 1
    const val VALIDATION_REVISION = 1
    const val MAX_RATIONALE_UTF8_BYTES = 4096

    val FROZEN_INPUT_BINDING: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationInputBindingV1
        get() {
            val unsigned = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationInputBindingV1(
                batchId = BATCH_ID,
                submissionId = SUBMISSION_ID,
                batchFileBinding = HimZeroCandidateRecoveryHumanReviewFileBindingV1(
                    BATCH_FILE_PATH,
                    BATCH_FILE_SIZE,
                    BATCH_FILE_SHA256,
                    ORIGINAL_BATCH_LOGICAL_DIGEST,
                ),
                originalInputBindingDigest = ORIGINAL_INPUT_BINDING_DIGEST,
                originalBatchLogicalDigest = ORIGINAL_BATCH_LOGICAL_DIGEST,
                originalReviewerRef = ORIGINAL_REVIEWER_REF,
                originalReviewRound = VALIDATION_ROUND,
                originalRevision = VALIDATION_REVISION,
                originalSelections = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.FROZEN_SELECTIONS,
                evidenceBindings = frozenEvidenceBindings(),
                bindingDigest = "",
            )
            return unsigned.copy(bindingDigest = inputBindingDigest(unsigned))
        }

    val FROZEN_SELECTIONS: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1>
        get() = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSubmissionContractV1.FROZEN_SELECTIONS

    fun inputBindingDigest(
        binding: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationInputBindingV1,
    ): String = sha256(
        buildString {
            appendLine(INPUT_BINDING_DIGEST_DOMAIN)
            appendLine("contractId=$CONTRACT_ID")
            appendLine("version=$VERSION")
            appendLine("batchId=${binding.batchId}")
            appendLine("submissionId=${binding.submissionId}")
            appendFileBinding("batch", binding.batchFileBinding)
            appendLine("originalInputBindingDigest=${binding.originalInputBindingDigest}")
            appendLine("originalBatchLogicalDigest=${binding.originalBatchLogicalDigest}")
            appendLine("originalReviewerRef=${binding.originalReviewerRef}")
            appendLine("originalReviewRound=${binding.originalReviewRound}")
            appendLine("originalRevision=${binding.originalRevision}")
            binding.originalSelections.forEachIndexed { index, selection -> appendSelection(index, selection) }
            binding.evidenceBindings
                .sortedWith(compareBy({ it.reviewUnitId }, { it.evidenceReferenceId }))
                .forEach { evidence ->
                    appendLine("evidence.reviewUnitId=${evidence.reviewUnitId}")
                    appendLine("evidence.evidenceReferenceId=${evidence.evidenceReferenceId}")
                    appendLine("evidence.kind=${evidence.kind.name}")
                    appendLine("evidence.directness=${evidence.directness.name}")
                    appendLine("evidence.position=${evidence.position.name}")
                }
        },
    )

    fun validationRecordId(
        inputBinding: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationInputBindingV1,
        originalDecision: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1,
        validatorReviewerRef: String,
        validationRound: Int,
        validationRevision: Int,
    ): String = sha256(
        buildString {
            appendLine(RECORD_ID_DOMAIN)
            appendLine("contractId=$CONTRACT_ID")
            appendLine("version=$VERSION")
            appendLine("batchLogicalDigest=${inputBinding.originalBatchLogicalDigest}")
            appendSelection(0, originalDecision)
            appendLine("validatorReviewerRef=$validatorReviewerRef")
            appendLine("validationRound=$validationRound")
            appendLine("validationRevision=$validationRevision")
        },
    )

    fun createValidationRecord(
        inputBinding: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationInputBindingV1,
        originalDecision: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1,
        validatorReviewerRef: String,
        validationRound: Int,
        validationRevision: Int,
        assessment: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1,
        reasonCodes: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1>,
        evidenceReferenceIds: List<String>,
        rationale: String,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1 =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1(
            validationRecordId = validationRecordId(
                inputBinding,
                originalDecision,
                validatorReviewerRef,
                validationRound,
                validationRevision,
            ),
            originalDecision = originalDecision,
            validatorReviewerRef = validatorReviewerRef,
            validationRound = validationRound,
            validationRevision = validationRevision,
            assessment = assessment,
            reasonCodes = reasonCodes,
            evidenceReferenceIds = evidenceReferenceIds,
            rationale = rationale,
        )

    fun createBatch(
        inputBinding: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationInputBindingV1,
        validatorReviewerRef: String,
        validationRound: Int,
        validationRevision: Int,
        records: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1>,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1 {
        val ordered = records.sortedWith(compareBy({ selectionOrder(it.originalDecision.reviewUnitId) }, { it.validatorReviewerRef }, { it.validationRound }, { it.validationRevision }))
        val unsigned = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1(
            contractId = CONTRACT_ID,
            version = VERSION,
            state = STATE,
            inputBinding = inputBinding,
            validatorReviewerRef = validatorReviewerRef,
            validationRound = validationRound,
            validationRevision = validationRevision,
            records = ordered,
            counters = deriveCounters(ordered),
            bindingDigest = "",
            logicalDigest = "",
        )
        val bound = unsigned.copy(bindingDigest = batchBindingDigest(unsigned))
        return bound.copy(logicalDigest = batchLogicalDigest(bound))
    }

    fun deriveRoute(
        originalDecision: HimZeroCandidateRecoveryHumanReviewDecisionV1,
        assessment: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1 = when (assessment) {
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION ->
            when (originalDecision) {
                HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION -> HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_POSITIVE_GOLD_CANDIDATE
                HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION -> HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE
                else -> HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.NOT_ELIGIBLE
            }
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.CHALLENGE_ORIGINAL_DECISION,
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ESCALATE_VALIDATION_CONFLICT ->
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.REQUIRES_ADJUDICATION
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ABSTAIN_VALIDATION_INSUFFICIENT_EVIDENCE ->
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.NOT_ELIGIBLE
    }

    fun deriveCounters(
        records: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1>,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationCountersV1 =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationCountersV1(
            totalValidationRecords = records.size,
            independentlyValidatedRecords = records.count { it.assessment == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION },
            challengedRecords = records.count { it.assessment == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.CHALLENGE_ORIGINAL_DECISION },
            abstainedRecords = records.count { it.assessment == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ABSTAIN_VALIDATION_INSUFFICIENT_EVIDENCE },
            escalatedRecords = records.count { it.assessment == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ESCALATE_VALIDATION_CONFLICT },
            potentialPositiveGoldCandidates = records.count { it.downstreamRoute == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_POSITIVE_GOLD_CANDIDATE },
            potentialNegativeSupervisionCandidates = records.count { it.downstreamRoute == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE },
            requiresAdjudicationRecords = records.count { it.downstreamRoute == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.REQUIRES_ADJUDICATION },
            notEligibleRecords = records.count { it.downstreamRoute == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1.NOT_ELIGIBLE },
            distinctReviewUnits = records.map { it.originalDecision.reviewUnitId }.distinct().size,
            distinctOriginalDecisionIdentities = records.map { originalDecisionIdentity(it.originalDecision) }.distinct().size,
            referencedEvidenceCount = records.sumOf { it.evidenceReferenceIds.size },
            distinctReferencedEvidenceCount = records.flatMap { it.evidenceReferenceIds }.distinct().size,
        )

    fun validate(
        batch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationResultV1 {
        if (batch.contractId != CONTRACT_ID) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_CONTRACT_ID)
        if (batch.version != VERSION) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_CONTRACT_VERSION)
        if (batch.state != STATE) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_STATE)
        inputBindingFailure(batch.inputBinding)?.let { return invalid(it) }
        if (!REVIEWER_REF.matches(batch.validatorReviewerRef)) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_VALIDATOR_REVIEWER_REF)
        if (batch.validatorReviewerRef == batch.inputBinding.originalReviewerRef) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.VALIDATOR_NOT_INDEPENDENT)
        if (batch.validationRound != VALIDATION_ROUND) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_VALIDATION_ROUND)
        if (batch.validationRevision != VALIDATION_REVISION) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_VALIDATION_REVISION)
        if (batch.records.size != FROZEN_SELECTIONS.size) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_RECORD_COUNT)
        if (batch.records != batch.records.sortedWith(recordComparator())) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_RECORD_ORDER)
        val identities = batch.records.map { validationIdentity(it) }
        if (identities.distinct().size != identities.size) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.DUPLICATE_VALIDATION_IDENTITY)
        batch.records.forEach { record ->
            recordFailure(batch, record)?.let { return invalid(it) }
        }
        if (batch.counters != deriveCounters(batch.records)) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_COUNTERS)
        if (!SHA256.matches(batch.bindingDigest) || batch.bindingDigest != batchBindingDigest(batch.copy(bindingDigest = ""))) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.VALIDATION_BINDING_DIGEST_MISMATCH)
        if (!SHA256.matches(batch.logicalDigest) || batch.logicalDigest != batchLogicalDigest(batch.copy(logicalDigest = ""))) return invalid(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.VALIDATION_LOGICAL_DIGEST_MISMATCH)
        return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationResultV1.Valid
    }

    fun batchBindingDigest(
        batch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1,
    ): String = sha256(
        buildString {
            appendLine(BATCH_BINDING_DIGEST_DOMAIN)
            appendLine("contractId=${batch.contractId}")
            appendLine("version=${batch.version}")
            appendLine("state=${batch.state}")
            appendLine("inputBindingDigest=${batch.inputBinding.bindingDigest}")
            appendLine("validatorReviewerRef=${batch.validatorReviewerRef}")
            appendLine("validationRound=${batch.validationRound}")
            appendLine("validationRevision=${batch.validationRevision}")
            batch.records.forEach { appendLine("validationRecordId=${it.validationRecordId}") }
        },
    )

    fun batchLogicalDigest(
        batch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1,
    ): String = sha256(
        buildString {
            appendLine(BATCH_LOGICAL_DIGEST_DOMAIN)
            appendLine("bindingDigest=${batch.bindingDigest}")
            batch.records.forEach { record ->
                appendLine("validationRecordId=${record.validationRecordId}")
                appendSelection(0, record.originalDecision)
                appendLine("validatorReviewerRef=${record.validatorReviewerRef}")
                appendLine("validationRound=${record.validationRound}")
                appendLine("validationRevision=${record.validationRevision}")
                appendLine("assessment=${record.assessment.name}")
                record.reasonCodes.forEach { appendLine("reasonCode=${it.name}") }
                record.evidenceReferenceIds.forEach { appendLine("evidenceReferenceId=$it") }
                appendLine("rationale=${record.rationale}")
                appendLine("route=${record.downstreamRoute.name}")
            }
            appendLine("counters=${batch.counters}")
        },
    )

    private fun inputBindingFailure(
        binding: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationInputBindingV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1? {
        if (binding.batchId != BATCH_ID || binding.submissionId != SUBMISSION_ID ||
            binding.batchFileBinding != FROZEN_INPUT_BINDING.batchFileBinding ||
            binding.originalInputBindingDigest != ORIGINAL_INPUT_BINDING_DIGEST ||
            binding.originalBatchLogicalDigest != ORIGINAL_BATCH_LOGICAL_DIGEST ||
            binding.originalReviewerRef != ORIGINAL_REVIEWER_REF ||
            binding.originalReviewRound != VALIDATION_ROUND || binding.originalRevision != VALIDATION_REVISION ||
            binding.originalSelections != FROZEN_SELECTIONS || binding.evidenceBindings != FROZEN_INPUT_BINDING.evidenceBindings
        ) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_INPUT_BINDING
        if (!binding.bindingDigest.matches(SHA256) || binding.bindingDigest != inputBindingDigest(binding.copy(bindingDigest = ""))) {
            return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INPUT_BINDING_DIGEST_MISMATCH
        }
        return null
    }

    private fun recordFailure(
        batch: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1,
        record: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1,
    ): HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1? {
        val expected = FROZEN_SELECTIONS.singleOrNull { it.reviewUnitId == record.originalDecision.reviewUnitId }
            ?: return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.UNKNOWN_REVIEW_UNIT
        if (record.originalDecision != expected) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.ORIGINAL_DECISION_MISMATCH
        if (record.validatorReviewerRef != batch.validatorReviewerRef) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_VALIDATOR_REVIEWER_REF
        if (record.validatorReviewerRef == batch.inputBinding.originalReviewerRef) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.VALIDATOR_NOT_INDEPENDENT
        if (!REVIEWER_REF.matches(record.validatorReviewerRef)) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_VALIDATOR_REVIEWER_REF
        if (record.validationRound != VALIDATION_ROUND) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_VALIDATION_ROUND
        if (record.validationRevision != VALIDATION_REVISION) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_VALIDATION_REVISION
        if (!SHA256.matches(record.validationRecordId) || record.validationRecordId != validationRecordId(batch.inputBinding, record.originalDecision, record.validatorReviewerRef, record.validationRound, record.validationRevision)) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_VALIDATION_RECORD_ID
        if (record.reasonCodes.isEmpty() || record.reasonCodes != record.reasonCodes.distinct().sortedBy { it.ordinal }) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_REASON_CODES
        if (record.evidenceReferenceIds != record.evidenceReferenceIds.distinct().sorted()) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_EVIDENCE_REFERENCE
        if (!validRationale(record.rationale, record.assessment)) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_RATIONALE

        val unitEvidence = batch.inputBinding.evidenceBindings.filter { it.reviewUnitId == record.originalDecision.reviewUnitId }
        val evidence = record.evidenceReferenceIds.map { id ->
            unitEvidence.singleOrNull { it.evidenceReferenceId == id }
                ?: if (batch.inputBinding.evidenceBindings.any { it.evidenceReferenceId == id }) {
                    return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.CROSS_UNIT_EVIDENCE_REFERENCE
                } else {
                    return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_EVIDENCE_REFERENCE
                }
        }
        if (evidence.isEmpty() && record.assessment == HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.CHALLENGE_ORIGINAL_DECISION) {
            return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_EVIDENCE_REFERENCE
        }
        val support = evidence.any {
            it.directness == HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT &&
                it.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION
        }
        val contradiction = evidence.any {
            it.directness == HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT &&
                it.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION
        }
        val unitHasContradiction = unitEvidence.any {
            it.directness == HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT &&
                it.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION
        }
        val expectedRoute = deriveRoute(record.originalDecision.decision, record.assessment)
        when (record.assessment) {
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.VALIDATE_ORIGINAL_DECISION -> {
                when (record.originalDecision.decision) {
                    HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION -> {
                        if (!support) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INSUFFICIENT_DIRECT_EVIDENCE
                        if (unitHasContradiction) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.UNADDRESSED_CONTRADICTING_EVIDENCE
                        if (record.reasonCodes.toSet() != setOf(
                                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_SUPPORTS_ORIGINAL_DECISION,
                                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.ORIGINAL_REASON_CODES_SUPPORTED,
                            )
                        ) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_REASON_CODES
                    }
                    HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION -> {
                        if (!contradiction) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INSUFFICIENT_DIRECT_EVIDENCE
                        if (record.originalDecision.reasonCodes.toSet() != setOf(
                                HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_SEMANTIC_MISMATCH,
                                HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED,
                            )
                        ) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.ORIGINAL_DECISION_IDENTITY_MISMATCH
                        if (record.reasonCodes.toSet() != setOf(
                                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DIRECT_EVIDENCE_CONTRADICTS_ORIGINAL_DECISION,
                                HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.ORIGINAL_REASON_CODES_SUPPORTED,
                            )
                        ) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_REASON_CODES
                    }
                    else -> return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_ASSESSMENT
                }
            }
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.CHALLENGE_ORIGINAL_DECISION -> {
                if (HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.REQUIRES_ADJUDICATION !in record.reasonCodes) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_REASON_CODES
                if (record.reasonCodes.none { it in setOf(
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.CONFLICTING_DIRECT_EVIDENCE,
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DECISION_EVIDENCE_BINDING_MISMATCH,
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.REVIEW_UNIT_SCOPE_MISMATCH,
                    )
                }) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_REASON_CODES
            }
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ABSTAIN_VALIDATION_INSUFFICIENT_EVIDENCE -> {
                if (record.reasonCodes != listOf(HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.INSUFFICIENT_DIRECT_EVIDENCE)) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_REASON_CODES
            }
            HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1.ESCALATE_VALIDATION_CONFLICT -> {
                if (HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.REQUIRES_ADJUDICATION !in record.reasonCodes ||
                    record.reasonCodes.none { it in setOf(
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.CONFLICTING_DIRECT_EVIDENCE,
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.DECISION_EVIDENCE_BINDING_MISMATCH,
                        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1.REVIEW_UNIT_SCOPE_MISMATCH,
                    ) }
                ) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_REASON_CODES
            }
        }
        if (record.downstreamRoute != expectedRoute) return HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1.INVALID_DOWNSTREAM_ROUTE
        return null
    }

    private fun validRationale(
        rationale: String,
        assessment: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1,
    ): Boolean {
        val normalized = rationale.trim().replace(Regex("\\s+"), " ")
        return rationale.isNotBlank() && rationale == normalized &&
            rationale.toByteArray(StandardCharsets.UTF_8).size <= MAX_RATIONALE_UTF8_BYTES &&
            rationale != assessment.name &&
            !rationale.contains('\u0000') &&
            !rationale.startsWith('/') &&
            !rationale.contains('\\') &&
            !WINDOWS_ABSOLUTE_PATH.matches(rationale) &&
            !rationale.contains("exception", ignoreCase = true) &&
            !rationale.contains("throwable", ignoreCase = true) &&
            !rationale.contains("stacktrace", ignoreCase = true)
    }

    private fun frozenEvidenceBindings(): List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationEvidenceBindingV1> = listOf(
        evidence("36848ad04b38db71496f4d19854d0ed5f1e09021873c78b407ca0b3365633f5e", "49c134d1fd2b99ae2c164cffdc5e553a6364f2cabfea7f057bb58e613aaef9f4", HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY),
        evidence("36848ad04b38db71496f4d19854d0ed5f1e09021873c78b407ca0b3365633f5e", "5912c6bde368803377441d627a04c74d1aecf08b43131b19d793196b32bf94f9", HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION),
        evidence("36848ad04b38db71496f4d19854d0ed5f1e09021873c78b407ca0b3365633f5e", "9ecfd9e8e601ffdb1f60620ff204882a32bbbfc355cd6b9b79f403866a40d9bd", HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION),
        evidence("4989e81ddc0df733fe4b1854c44405881aebc3f96b5f4b2d5cd7b0ba243b22b7", "280b91e4c4e10f95288308d50a0df31e3be773385c11afc104e8525127cc5b1e", HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY),
        evidence("4989e81ddc0df733fe4b1854c44405881aebc3f96b5f4b2d5cd7b0ba243b22b7", "8a9e75f184ab7e1e4dd81c0b6d805ddf252dce54d8d39244b892e53ad59e7bcd", HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION),
        evidence("4989e81ddc0df733fe4b1854c44405881aebc3f96b5f4b2d5cd7b0ba243b22b7", "b4a88e7492228b36d6919cbc75959520b86d8505cf1f85b93764f21538cdfe09", HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY),
        evidence("9d5a924222950404aa590be81e5f175016cc228a0257378c3e137429c4304d70", "4712bc9b78ecfc168ea1902518ff625f92eff4ee14a9d1938a5c66fc4dd8b39e", HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION),
        evidence("9d5a924222950404aa590be81e5f175016cc228a0257378c3e137429c4304d70", "896f30747f7fcfcb29f64e9170e9ca6798ad796a996e79d7b5432b146fde2020", HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION),
        evidence("9d5a924222950404aa590be81e5f175016cc228a0257378c3e137429c4304d70", "d6f7e596663b3547c30bb4afcf386cdc97bc32d170860ed39486f23ec719dabf", HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY),
        evidence("d3f4420350583f5837b83a635cb910a388f1e62427134921a16c49e0830b8b50", "045ee67398a6f7355b0faef3760ceb0d24bf22515b9e67d84c7931e96d2c9fba", HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY),
        evidence("d3f4420350583f5837b83a635cb910a388f1e62427134921a16c49e0830b8b50", "2b225f611ce2dc5eed79f4238b0a24d328318638549fb63f3a9611f1fc29e027", HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION),
        evidence("d3f4420350583f5837b83a635cb910a388f1e62427134921a16c49e0830b8b50", "dbcacd77e3944b77661262caa8845e24b1a1cb2eb6b78726cb7595dc7ecf5d93", HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD, HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTEXT_ONLY),
    ).sortedWith(compareBy({ it.reviewUnitId }, { it.evidenceReferenceId }))

    private fun evidence(
        reviewUnitId: String,
        evidenceReferenceId: String,
        kind: HimZeroCandidateRecoveryHumanReviewEvidenceKindV1,
        position: HimZeroCandidateRecoveryHumanReviewEvidencePositionV1,
    ) = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationEvidenceBindingV1(
        reviewUnitId,
        evidenceReferenceId,
        kind,
        HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT,
        position,
    )

    private fun selectionOrder(reviewUnitId: String): Int = FROZEN_SELECTIONS.indexOfFirst { it.reviewUnitId == reviewUnitId }.let { if (it < 0) Int.MAX_VALUE else it }

    private fun recordComparator() = compareBy<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1>({ selectionOrder(it.originalDecision.reviewUnitId) }, { it.validatorReviewerRef }, { it.validationRound }, { it.validationRevision })

    private fun validationIdentity(record: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1): String = listOf(record.originalDecision.reviewUnitId, record.validatorReviewerRef, record.validationRound, record.validationRevision).joinToString("\u0000")

    private fun originalDecisionIdentity(selection: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1): String = listOf(selection.reviewUnitId, selection.stableEntryId, selection.canonicalEntityId, selection.decision.name, selection.revision, selection.evidenceReferenceIds.sorted().joinToString(",")).joinToString("\u0000")

    private fun StringBuilder.appendFileBinding(label: String, binding: HimZeroCandidateRecoveryHumanReviewFileBindingV1) {
        appendLine("$label.relativePath=${binding.relativePath}")
        appendLine("$label.byteSize=${binding.byteSize}")
        appendLine("$label.sha256=${binding.sha256}")
        appendLine("$label.logicalDigest=${binding.logicalDigest}")
    }

    private fun StringBuilder.appendSelection(index: Int, selection: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1) {
        appendLine("selection[$index].reviewUnitId=${selection.reviewUnitId}")
        appendLine("selection[$index].stableEntryId=${selection.stableEntryId}")
        appendLine("selection[$index].canonicalEntityId=${selection.canonicalEntityId}")
        appendLine("selection[$index].decision=${selection.decision.name}")
        selection.reasonCodes.forEach { appendLine("selection[$index].reasonCode=${it.name}") }
        selection.evidenceReferenceIds.sorted().forEach { appendLine("selection[$index].evidenceReferenceId=$it") }
        appendLine("selection[$index].revision=${selection.revision}")
        appendLine("selection[$index].alternativeCanonicalProposal=${selection.alternativeCanonicalProposal ?: ""}")
        appendLine("selection[$index].reviewerNote=${selection.reviewerNote ?: ""}")
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun invalid(reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1) =
        HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationResultV1.Invalid(reason)

    private val SHA256 = Regex("[0-9a-f]{64}")
    private val REVIEWER_REF = Regex("[A-Za-z0-9._:-]{1,128}")
    private val WINDOWS_ABSOLUTE_PATH = Regex("[A-Za-z]:[/\\\\].*")
}

enum class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1 {
    VALIDATE_ORIGINAL_DECISION,
    CHALLENGE_ORIGINAL_DECISION,
    ABSTAIN_VALIDATION_INSUFFICIENT_EVIDENCE,
    ESCALATE_VALIDATION_CONFLICT,
}

enum class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1 {
    DIRECT_EVIDENCE_SUPPORTS_ORIGINAL_DECISION,
    DIRECT_EVIDENCE_CONTRADICTS_ORIGINAL_DECISION,
    ORIGINAL_REASON_CODES_SUPPORTED,
    ORIGINAL_REASON_CODES_INCOMPLETE,
    INSUFFICIENT_DIRECT_EVIDENCE,
    CONFLICTING_DIRECT_EVIDENCE,
    DECISION_EVIDENCE_BINDING_MISMATCH,
    REVIEW_UNIT_SCOPE_MISMATCH,
    VALIDATOR_INDEPENDENCE_VIOLATION,
    REQUIRES_ADJUDICATION,
}

enum class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1 {
    POTENTIAL_POSITIVE_GOLD_CANDIDATE,
    POTENTIAL_NEGATIVE_SUPERVISION_CANDIDATE,
    REQUIRES_ADJUDICATION,
    NOT_ELIGIBLE,
}

enum class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1 {
    INVALID_CONTRACT_ID,
    INVALID_CONTRACT_VERSION,
    INVALID_STATE,
    INVALID_INPUT_BINDING,
    INPUT_BINDING_DIGEST_MISMATCH,
    INVALID_BATCH_BINDING,
    UNKNOWN_REVIEW_UNIT,
    INVALID_RECORD_COUNT,
    INVALID_RECORD_ORDER,
    ORIGINAL_DECISION_MISMATCH,
    ORIGINAL_DECISION_IDENTITY_MISMATCH,
    INVALID_VALIDATOR_REVIEWER_REF,
    VALIDATOR_NOT_INDEPENDENT,
    INVALID_VALIDATION_ROUND,
    INVALID_VALIDATION_REVISION,
    DUPLICATE_VALIDATION_IDENTITY,
    INVALID_VALIDATION_RECORD_ID,
    INVALID_ASSESSMENT,
    INVALID_REASON_CODES,
    INVALID_RATIONALE,
    INVALID_EVIDENCE_REFERENCE,
    CROSS_UNIT_EVIDENCE_REFERENCE,
    INSUFFICIENT_DIRECT_EVIDENCE,
    UNADDRESSED_CONTRADICTING_EVIDENCE,
    INVALID_DOWNSTREAM_ROUTE,
    INVALID_COUNTERS,
    VALIDATION_RECORD_DIGEST_MISMATCH,
    VALIDATION_BINDING_DIGEST_MISMATCH,
    VALIDATION_LOGICAL_DIGEST_MISMATCH,
    FORBIDDEN_GOLD_SEMANTICS,
    FORBIDDEN_NEGATIVE_SUPERVISION_SEMANTICS,
    FORBIDDEN_AUTHORITY_MUTATION_SEMANTICS,
}

sealed interface HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationResultV1 {
    val valid: Boolean

    data object Valid : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationResultV1 {
        override val valid: Boolean = true
    }

    data class Invalid(
        val reason: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationFailureReasonV1,
    ) : HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationResultV1 {
        override val valid: Boolean = false
    }
}

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationInputBindingV1(
    val batchId: String,
    val submissionId: String,
    val batchFileBinding: HimZeroCandidateRecoveryHumanReviewFileBindingV1,
    val originalInputBindingDigest: String,
    val originalBatchLogicalDigest: String,
    val originalReviewerRef: String,
    val originalReviewRound: Int,
    val originalRevision: Int,
    val originalSelections: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1>,
    val evidenceBindings: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationEvidenceBindingV1>,
    val bindingDigest: String,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationEvidenceBindingV1(
    val reviewUnitId: String,
    val evidenceReferenceId: String,
    val kind: HimZeroCandidateRecoveryHumanReviewEvidenceKindV1,
    val directness: HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1,
    val position: HimZeroCandidateRecoveryHumanReviewEvidencePositionV1,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1(
    val validationRecordId: String,
    val originalDecision: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionSelectionV1,
    val validatorReviewerRef: String,
    val validationRound: Int,
    val validationRevision: Int,
    val assessment: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationAssessmentV1,
    val reasonCodes: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationReasonCodeV1>,
    val evidenceReferenceIds: List<String>,
    val rationale: String,
) {
    val downstreamRoute: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationDownstreamRouteV1
        get() = HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationContractV1.deriveRoute(originalDecision.decision, assessment)
}

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationCountersV1(
    val totalValidationRecords: Int,
    val independentlyValidatedRecords: Int,
    val challengedRecords: Int,
    val abstainedRecords: Int,
    val escalatedRecords: Int,
    val potentialPositiveGoldCandidates: Int,
    val potentialNegativeSupervisionCandidates: Int,
    val requiresAdjudicationRecords: Int,
    val notEligibleRecords: Int,
    val distinctReviewUnits: Int,
    val distinctOriginalDecisionIdentities: Int,
    val referencedEvidenceCount: Int,
    val distinctReferencedEvidenceCount: Int,
)

data class HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationBatchV1(
    val contractId: String,
    val version: String,
    val state: String,
    val inputBinding: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationInputBindingV1,
    val validatorReviewerRef: String,
    val validationRound: Int,
    val validationRevision: Int,
    val records: List<HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationRecordV1>,
    val counters: HimZeroCandidateRecoveryHumanReviewP1PilotDecisionValidationCountersV1,
    val bindingDigest: String,
    val logicalDigest: String,
)
