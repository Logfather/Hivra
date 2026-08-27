package de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval

import java.security.MessageDigest

/** Pure shape and semantic contract for human review of zero-candidate recovery records. */
object HimZeroCandidateRecoveryHumanReviewContractV1 {
    const val VERSION = "HIM_ZERO_CANDIDATE_RECOVERY_HUMAN_REVIEW_CONTRACT_V1"
    const val REVIEW_UNIT_ID_DOMAIN_SEPARATOR = "HIM_ZERO_CANDIDATE_RECOVERY_REVIEW_UNIT_V1"
    const val DURABLE_DECISION_BATCH_ROOT =
        "data/knowledge/him/canonical-family/human-review/zero-candidate-recovery/v1/decision-batches"
    const val DERIVED_REPORT_ROOT =
        "build/knowledge/reports/him/evidence-alignment/catalog-audit/zero-candidate-recovery-human-review/v1"

    fun reviewUnitId(stableEntryId: String, canonicalEntityId: String): String =
        sha256(
            buildString {
                appendLine(REVIEW_UNIT_ID_DOMAIN_SEPARATOR)
                appendLine("contract=$VERSION")
                appendLine("stable-entry-id=$stableEntryId")
                appendLine("canonical-entity-id=$canonicalEntityId")
            },
        )

    internal fun sha256(value: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

enum class HimZeroCandidateRecoveryHumanReviewDecisionV1 {
    CONFIRM_ASSOCIATION,
    REJECT_ASSOCIATION,
    ABSTAIN_INSUFFICIENT_EVIDENCE,
    ABSTAIN_AMBIGUOUS,
    ABSTAIN_CONFLICTING_EVIDENCE,
    ESCALATE_OUT_OF_SCOPE,
}

enum class HimZeroCandidateRecoveryHumanReviewReasonCodeV1 {
    DIRECT_ASSOCIATION_SUPPORTED,
    DIRECT_SEMANTIC_MISMATCH,
    DIRECT_ASSOCIATION_CONTRADICTED,
    REQUIRED_DIRECT_EVIDENCE_MISSING,
    INDIRECT_EVIDENCE_ONLY,
    MULTIPLE_PLAUSIBLE_INTERPRETATIONS,
    MULTIPLE_PLAUSIBLE_CANONICAL_TARGETS,
    BOUND_EVIDENCE_CONFLICT,
    NEW_CANONICAL_REQUIRED,
    IDENTITY_OR_VARIANT_REVIEW_REQUIRED,
    AUTHORITY_MUTATION_REQUIRED,
    GOVERNANCE_DECISION_REQUIRED,
}

enum class HimZeroCandidateRecoveryHumanReviewEvidenceKindV1 {
    ORIGIN_CORPUS_RECORD,
    SOURCE_EVIDENCE_PROJECTION,
    CANONICAL_CATALOG_RECORD,
    CANONICAL_FAMILY_AUTHORITY_RECORD,
    CROSS_SOURCE_RECORD,
}

enum class HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1 {
    DIRECT,
    INDIRECT,
}

enum class HimZeroCandidateRecoveryHumanReviewEvidencePositionV1 {
    SUPPORTS_ASSOCIATION,
    CONTRADICTS_ASSOCIATION,
    CONTEXT_ONLY,
}

enum class HimZeroCandidateRecoveryHumanReviewDownstreamRouteV1 {
    POSITIVE_GOLD_CANDIDATE_AFTER_INDEPENDENT_VALIDATION,
    NEGATIVE_SUPERVISION_CANDIDATE_AFTER_INDEPENDENT_VALIDATION,
    NONE,
}

enum class HimZeroCandidateRecoveryHumanReviewValidationStateV1 {
    UNVALIDATED,
    VALIDATED,
    INVALID,
}

enum class HimZeroCandidateRecoveryHumanReviewConflictStateV1 {
    NONE,
    OPPOSING_EVIDENCE,
    OPPOSING_REVIEWERS,
}

enum class HimZeroCandidateRecoveryHumanReviewValidationErrorV1 {
    BLANK_STABLE_ENTRY_ID,
    BLANK_CANONICAL_ENTITY_ID,
    INVALID_REVIEW_UNIT_ID,
    INVALID_REVIEWER_REF,
    INVALID_REVIEW_ROUND,
    INVALID_REVISION,
    EMPTY_REASON_CODES,
    INCOMPATIBLE_REASON_CODE,
    INVALID_EVIDENCE_REFERENCE,
    MISSING_REQUIRED_DIRECT_EVIDENCE,
    REJECT_WITHOUT_DIRECT_CONTRADICTION,
    CONFLICT_WITHOUT_OPPOSING_EVIDENCE,
    INVALID_ALTERNATIVE_CANONICAL_PROPOSAL,
    FORBIDDEN_DOWNSTREAM_STATE,
    DUPLICATE_EVIDENCE_REFERENCE,
    DUPLICATE_REASON_CODE,
    INVALID_REVIEW_NOTE,
}

sealed interface HimZeroCandidateRecoveryHumanReviewValidationResultV1 {
    val valid: Boolean

    object Valid : HimZeroCandidateRecoveryHumanReviewValidationResultV1 {
        override val valid: Boolean = true
    }

    data class Invalid(
        val error: HimZeroCandidateRecoveryHumanReviewValidationErrorV1,
    ) : HimZeroCandidateRecoveryHumanReviewValidationResultV1 {
        override val valid: Boolean = false
    }
}

data class HimZeroCandidateRecoveryHumanReviewFileBindingV1(
    val relativePath: String,
    val byteSize: Long,
    val sha256: String,
    val logicalDigest: String,
) {
    fun validate(): HimZeroCandidateRecoveryHumanReviewValidationResultV1 {
        if (relativePath.isBlank() || relativePath.startsWith('/') || relativePath.contains('\\')) {
            return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INVALID_EVIDENCE_REFERENCE)
        }
        if (byteSize < 0 || !SHA256.matches(sha256) || !SHA256.matches(logicalDigest)) {
            return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INVALID_EVIDENCE_REFERENCE)
        }
        return valid()
    }
}

data class HimZeroCandidateRecoveryHumanReviewReviewUnitV1(
    val stableEntryId: String,
    val canonicalEntityId: String,
) {
    val reviewUnitId: String
        get() = HimZeroCandidateRecoveryHumanReviewContractV1.reviewUnitId(stableEntryId, canonicalEntityId)

    fun validate(): HimZeroCandidateRecoveryHumanReviewValidationResultV1 {
        if (stableEntryId.isBlank()) return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.BLANK_STABLE_ENTRY_ID)
        if (!SHA256.matches(stableEntryId)) return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INVALID_REVIEW_UNIT_ID)
        if (canonicalEntityId.isBlank()) return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.BLANK_CANONICAL_ENTITY_ID)
        if (!ENTITY.matches(canonicalEntityId)) return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INVALID_REVIEW_UNIT_ID)
        if (!SHA256.matches(reviewUnitId)) return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INVALID_REVIEW_UNIT_ID)
        return valid()
    }
}

data class HimZeroCandidateRecoveryHumanReviewEvidenceReferenceV1(
    val evidenceReferenceId: String,
    val kind: HimZeroCandidateRecoveryHumanReviewEvidenceKindV1,
    val directness: HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1,
    val position: HimZeroCandidateRecoveryHumanReviewEvidencePositionV1,
    val artifactReference: String,
    val recordReference: String,
    val artifactSha256: String,
    val artifactLogicalDigest: String?,
    val fieldReferences: List<String>,
) {
    fun validate(): HimZeroCandidateRecoveryHumanReviewValidationResultV1 {
        if (!SHA256.matches(evidenceReferenceId) || artifactReference.isBlank() || recordReference.isBlank()) {
            return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INVALID_EVIDENCE_REFERENCE)
        }
        if (artifactReference.startsWith('/') || artifactReference.contains('\\') || !SHA256.matches(artifactSha256)) {
            return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INVALID_EVIDENCE_REFERENCE)
        }
        if (artifactLogicalDigest != null && !SHA256.matches(artifactLogicalDigest)) {
            return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INVALID_EVIDENCE_REFERENCE)
        }
        if (fieldReferences.any { it.isBlank() } || fieldReferences != fieldReferences.distinct().sorted()) {
            return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INVALID_EVIDENCE_REFERENCE)
        }
        return valid()
    }
}

data class HimZeroCandidateRecoveryHumanReviewAlternativeCanonicalProposalV1(
    val canonicalEntityId: String,
    val state: String = "UNVALIDATED_REVIEW_PROPOSAL",
)

data class HimZeroCandidateRecoveryHumanReviewDecisionRecordV1(
    val contractId: String,
    val reviewUnit: HimZeroCandidateRecoveryHumanReviewReviewUnitV1,
    val reviewerRef: String,
    val reviewRound: Int,
    val revision: Int,
    val decision: HimZeroCandidateRecoveryHumanReviewDecisionV1,
    val reasonCodes: List<HimZeroCandidateRecoveryHumanReviewReasonCodeV1>,
    val evidenceReferences: List<HimZeroCandidateRecoveryHumanReviewEvidenceReferenceV1>,
    val alternativeCanonicalProposal: HimZeroCandidateRecoveryHumanReviewAlternativeCanonicalProposalV1? = null,
    val reviewerNote: String? = null,
) {
    val reviewState: String
        get() = "SINGLE_REVIEW_CANDIDATE_ONLY"

    val downstreamRoute: HimZeroCandidateRecoveryHumanReviewDownstreamRouteV1
        get() = when (decision) {
            HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION ->
                HimZeroCandidateRecoveryHumanReviewDownstreamRouteV1.POSITIVE_GOLD_CANDIDATE_AFTER_INDEPENDENT_VALIDATION
            HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION ->
                HimZeroCandidateRecoveryHumanReviewDownstreamRouteV1.NEGATIVE_SUPERVISION_CANDIDATE_AFTER_INDEPENDENT_VALIDATION
            else -> HimZeroCandidateRecoveryHumanReviewDownstreamRouteV1.NONE
        }

    fun validate(): HimZeroCandidateRecoveryHumanReviewValidationResultV1 {
        if (contractId != HimZeroCandidateRecoveryHumanReviewContractV1.VERSION) {
            return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.FORBIDDEN_DOWNSTREAM_STATE)
        }
        reviewUnit.validate().invalidOrNull()?.let { return invalid(it) }
        if (!REVIEWER_REF.matches(reviewerRef)) return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INVALID_REVIEWER_REF)
        if (reviewRound < 1) return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INVALID_REVIEW_ROUND)
        if (revision < 1) return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INVALID_REVISION)
        if (reasonCodes.isEmpty()) return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.EMPTY_REASON_CODES)
        if (reasonCodes != reasonCodes.distinct().sortedBy { it.ordinal }) {
            return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.DUPLICATE_REASON_CODE)
        }
        if (evidenceReferences != evidenceReferences.sortedBy { it.evidenceReferenceId }) {
            return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INVALID_EVIDENCE_REFERENCE)
        }
        if (evidenceReferences.map { it.evidenceReferenceId }.distinct().size != evidenceReferences.size) {
            return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.DUPLICATE_EVIDENCE_REFERENCE)
        }
        evidenceReferences.forEach { evidence ->
            evidence.validate().invalidOrNull()?.let { return invalid(it) }
        }
        if (reviewerNote != null && (reviewerNote.length > MAX_NOTE_LENGTH || reviewerNote.contains('\u0000'))) {
            return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INVALID_REVIEW_NOTE)
        }

        val expectedReasons = compatibleReasons(decision)
        if (reasonCodes.any { it !in expectedReasons }) {
            return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INCOMPATIBLE_REASON_CODE)
        }
        val direct = evidenceReferences.filter {
            it.directness == HimZeroCandidateRecoveryHumanReviewEvidenceDirectnessV1.DIRECT
        }
        val directSupport = direct.filter {
            it.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.SUPPORTS_ASSOCIATION
        }
        val directContradiction = direct.filter {
            it.position == HimZeroCandidateRecoveryHumanReviewEvidencePositionV1.CONTRADICTS_ASSOCIATION
        }
        val hasSourceEvidence = direct.any { it.kind.isSourceSide() }
        val hasTargetEvidence = direct.any { it.kind.isTargetSide() }

        when (decision) {
            HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION -> {
                if (!hasSourceEvidence || !hasTargetEvidence || directSupport.isEmpty() || directContradiction.isNotEmpty()) {
                    return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.MISSING_REQUIRED_DIRECT_EVIDENCE)
                }
            }
            HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION -> {
                if (!hasSourceEvidence || !hasTargetEvidence) {
                    return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.MISSING_REQUIRED_DIRECT_EVIDENCE)
                }
                if (directContradiction.isEmpty()) {
                    return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.REJECT_WITHOUT_DIRECT_CONTRADICTION)
                }
            }
            HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_INSUFFICIENT_EVIDENCE -> Unit
            HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_AMBIGUOUS -> {
                if (evidenceReferences.isEmpty()) return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.MISSING_REQUIRED_DIRECT_EVIDENCE)
            }
            HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_CONFLICTING_EVIDENCE -> {
                if (evidenceReferences.size < 2 || directSupport.isEmpty() || directContradiction.isEmpty()) {
                    return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.CONFLICT_WITHOUT_OPPOSING_EVIDENCE)
                }
            }
            HimZeroCandidateRecoveryHumanReviewDecisionV1.ESCALATE_OUT_OF_SCOPE -> Unit
        }

        val proposal = alternativeCanonicalProposal
        if (proposal != null) {
            if (decision != HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION ||
                !ENTITY.matches(proposal.canonicalEntityId) ||
                proposal.canonicalEntityId == reviewUnit.canonicalEntityId ||
                proposal.state != "UNVALIDATED_REVIEW_PROPOSAL" ||
                !hasSourceEvidence || !hasTargetEvidence || directSupport.isEmpty()
            ) {
                return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INVALID_ALTERNATIVE_CANONICAL_PROPOSAL)
            }
        }
        return valid()
    }
}

data class HimZeroCandidateRecoveryHumanReviewInputBindingV1(
    val corpusFileBinding: HimZeroCandidateRecoveryHumanReviewFileBindingV1,
    val corpusLogicalDigest: String,
    val corpusReportDigest: String,
    val corpusBindingDigest: String,
    val existingCorpusInputBinding: HimZeroCandidateRecoveryReviewCorpusInputBindingV1,
    val registryBinding: HimZeroCandidateRecoveryHumanReviewFileBindingV1?,
    val contractId: String,
    val contractVersion: String,
    val implementationHead: String,
    val bindingDigest: String,
) {
    fun validate(): HimZeroCandidateRecoveryHumanReviewValidationResultV1 {
        if (corpusFileBinding.validate() is HimZeroCandidateRecoveryHumanReviewValidationResultV1.Invalid) {
            return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INVALID_EVIDENCE_REFERENCE)
        }
        if (!SHA256.matches(corpusLogicalDigest) || !SHA256.matches(corpusReportDigest) || !SHA256.matches(corpusBindingDigest)) {
            return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INVALID_EVIDENCE_REFERENCE)
        }
        if (registryBinding?.validate() is HimZeroCandidateRecoveryHumanReviewValidationResultV1.Invalid) {
            return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.INVALID_EVIDENCE_REFERENCE)
        }
        if (contractId != HimZeroCandidateRecoveryHumanReviewContractV1.VERSION ||
            contractVersion != HimZeroCandidateRecoveryHumanReviewContractV1.VERSION ||
            !HEAD.matches(implementationHead) ||
            !SHA256.matches(bindingDigest)
        ) {
            return invalid(HimZeroCandidateRecoveryHumanReviewValidationErrorV1.FORBIDDEN_DOWNSTREAM_STATE)
        }
        return valid()
    }
}

private fun HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.isSourceSide(): Boolean =
    this == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.ORIGIN_CORPUS_RECORD ||
        this == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.SOURCE_EVIDENCE_PROJECTION ||
        this == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CROSS_SOURCE_RECORD

private fun HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.isTargetSide(): Boolean =
    this == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_CATALOG_RECORD ||
        this == HimZeroCandidateRecoveryHumanReviewEvidenceKindV1.CANONICAL_FAMILY_AUTHORITY_RECORD

private fun compatibleReasons(
    decision: HimZeroCandidateRecoveryHumanReviewDecisionV1,
): Set<HimZeroCandidateRecoveryHumanReviewReasonCodeV1> = when (decision) {
    HimZeroCandidateRecoveryHumanReviewDecisionV1.CONFIRM_ASSOCIATION ->
        setOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_SUPPORTED)
    HimZeroCandidateRecoveryHumanReviewDecisionV1.REJECT_ASSOCIATION ->
        setOf(
            HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_SEMANTIC_MISMATCH,
            HimZeroCandidateRecoveryHumanReviewReasonCodeV1.DIRECT_ASSOCIATION_CONTRADICTED,
        )
    HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_INSUFFICIENT_EVIDENCE ->
        setOf(
            HimZeroCandidateRecoveryHumanReviewReasonCodeV1.REQUIRED_DIRECT_EVIDENCE_MISSING,
            HimZeroCandidateRecoveryHumanReviewReasonCodeV1.INDIRECT_EVIDENCE_ONLY,
        )
    HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_AMBIGUOUS ->
        setOf(
            HimZeroCandidateRecoveryHumanReviewReasonCodeV1.MULTIPLE_PLAUSIBLE_INTERPRETATIONS,
            HimZeroCandidateRecoveryHumanReviewReasonCodeV1.MULTIPLE_PLAUSIBLE_CANONICAL_TARGETS,
        )
    HimZeroCandidateRecoveryHumanReviewDecisionV1.ABSTAIN_CONFLICTING_EVIDENCE ->
        setOf(HimZeroCandidateRecoveryHumanReviewReasonCodeV1.BOUND_EVIDENCE_CONFLICT)
    HimZeroCandidateRecoveryHumanReviewDecisionV1.ESCALATE_OUT_OF_SCOPE ->
        setOf(
            HimZeroCandidateRecoveryHumanReviewReasonCodeV1.NEW_CANONICAL_REQUIRED,
            HimZeroCandidateRecoveryHumanReviewReasonCodeV1.IDENTITY_OR_VARIANT_REVIEW_REQUIRED,
            HimZeroCandidateRecoveryHumanReviewReasonCodeV1.AUTHORITY_MUTATION_REQUIRED,
            HimZeroCandidateRecoveryHumanReviewReasonCodeV1.GOVERNANCE_DECISION_REQUIRED,
        )
}

private fun valid(): HimZeroCandidateRecoveryHumanReviewValidationResultV1 =
    HimZeroCandidateRecoveryHumanReviewValidationResultV1.Valid

private fun invalid(
    error: HimZeroCandidateRecoveryHumanReviewValidationErrorV1,
): HimZeroCandidateRecoveryHumanReviewValidationResultV1 =
    HimZeroCandidateRecoveryHumanReviewValidationResultV1.Invalid(error)

private fun HimZeroCandidateRecoveryHumanReviewValidationResultV1.invalidOrNull(): HimZeroCandidateRecoveryHumanReviewValidationErrorV1? =
    (this as? HimZeroCandidateRecoveryHumanReviewValidationResultV1.Invalid)?.error

private val SHA256 = Regex("[0-9a-f]{64}")
private val HEAD = Regex("[0-9a-f]{40}")
private val ENTITY = Regex("[0-9A-Za-z]{6}")
private val REVIEWER_REF = Regex("[A-Za-z0-9._:-]{1,128}")
private const val MAX_NOTE_LENGTH = 1000
