package de.shopme.tools.knowledge.him.canonical.family.groundtruth

enum class HimValidationDecision {
    APPROVED,
    REJECTED,
    REVIEW_REQUIRED,
    INSUFFICIENT_EVIDENCE,
    UNKNOWN,
    CONFLICT,
    KNOWN_RELATION_EVIDENCE,
}

enum class HimContractCheckStatus {
    PASS,
    FAIL,
}

data class HimContractCheck(
    val contractRule: String,
    val status: HimContractCheckStatus,
) {
    init {
        require(contractRule.isNotBlank())
    }
}

enum class HimConflictState {
    NONE,
    UNRESOLVED,
}

data class HimGroundTruthConflict(
    val state: HimConflictState,
    val evidenceReferences: List<HimEvidenceReference>,
) {
    init {
        require(state != HimConflictState.NONE || evidenceReferences.isEmpty())
        require(state != HimConflictState.UNRESOLVED || evidenceReferences.isNotEmpty())
    }
}

data class HimValidationAuthoritySnapshot(
    val canonicalFamilyAuthoritySha256: HimSha256,
    val entityIdRegistrySha256: HimSha256,
)

data class HimGroundTruthValidation(
    val validationReference: HimValidationReference,
    val candidateReference: HimCandidateReference,
    val decision: HimValidationDecision,
    val approvalConfidence: HimApprovalConfidence,
    val evidenceSufficiency: HimEvidenceSufficiency,
    val contractChecks: List<HimContractCheck>,
    val conflict: HimGroundTruthConflict,
    val replacementCandidateReference: HimCandidateReference?,
    val validatedAgainst: HimValidationAuthoritySnapshot,
)

data class HimGroundTruthProvenance(
    val candidateReference: HimCandidateReference,
    val validationReference: HimValidationReference,
    val evidenceReferences: List<HimEvidenceReference>,
    val decision: HimValidationDecision,
    val approvalConfidence: HimApprovalConfidence,
    val contractChecks: List<HimContractCheck>,
    val conflict: HimGroundTruthConflict,
    val shortRationale: String,
    val validatedAgainst: HimValidationAuthoritySnapshot,
    val contractVersion: String,
    val validatorVersion: String,
) {
    init {
        require(decision == HimValidationDecision.APPROVED)
        require(shortRationale.isNotBlank())
        require(contractVersion.isNotBlank())
        require(validatorVersion.isNotBlank())
    }
}

data class HimNegativeGroundTruthProvenance(
    val candidateReference: HimCandidateReference,
    val validationReference: HimValidationReference,
    val evidenceReferences: List<HimEvidenceReference>,
    val decision: HimValidationDecision,
    val contractChecks: List<HimContractCheck>,
    val shortRationale: String,
    val validatedAgainst: HimValidationAuthoritySnapshot,
    val contractVersion: String,
    val validatorVersion: String,
) {
    init {
        require(decision == HimValidationDecision.REJECTED)
        require(shortRationale.isNotBlank())
        require(contractVersion.isNotBlank())
        require(validatorVersion.isNotBlank())
    }
}
