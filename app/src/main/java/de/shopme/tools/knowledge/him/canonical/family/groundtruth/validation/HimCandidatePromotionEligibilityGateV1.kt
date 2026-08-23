package de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation

import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDataset

object HimCandidatePromotionEligibilityContractV1 {
    const val VERSION =
        "HIM_CANDIDATE_PROMOTION_ELIGIBILITY_GATE_V1"
}

enum class HimCandidatePromotionEligibilityStatus {
    PROMOTION_ELIGIBLE,
    BLOCKED_VALIDATION_MISSING,
    BLOCKED_REJECTED,
    BLOCKED_DEFERRED,
    BLOCKED_CANDIDATE_MISSING,
    BLOCKED_DATASET_MISMATCH,
}

data class HimCandidatePromotionEligibilityResultV1(
    val contractVersion: String =
        HimCandidatePromotionEligibilityContractV1.VERSION,
    val candidateReference: HimCandidateReference,
    val status: HimCandidatePromotionEligibilityStatus,
    val validationReference:
    HimCandidateValidationDecisionReference? = null,
) {
    init {
        require(
            contractVersion ==
                    HimCandidatePromotionEligibilityContractV1.VERSION
        )

        when (status) {
            HimCandidatePromotionEligibilityStatus.PROMOTION_ELIGIBLE,
            HimCandidatePromotionEligibilityStatus.BLOCKED_REJECTED,
            HimCandidatePromotionEligibilityStatus.BLOCKED_DEFERRED,
            HimCandidatePromotionEligibilityStatus.BLOCKED_DATASET_MISMATCH,
                -> require(validationReference != null)

            HimCandidatePromotionEligibilityStatus.BLOCKED_VALIDATION_MISSING,
            HimCandidatePromotionEligibilityStatus.BLOCKED_CANDIDATE_MISSING,
                -> Unit
        }
    }

    val promotionEligible: Boolean
        get() =
            status ==
                    HimCandidatePromotionEligibilityStatus.PROMOTION_ELIGIBLE
}

object HimCandidatePromotionEligibilityGateV1 {

    fun evaluate(
        candidateReference: HimCandidateReference,
        candidateDataset: HimCandidateDataset,
        currentCandidateDatasetDigest: HimSha256,
        validationLedger: HimCandidateValidationLedgerV1,
    ): HimCandidatePromotionEligibilityResultV1 {

        val candidateExists =
            candidateDataset.candidates.any {
                it.candidate.candidateReference ==
                        candidateReference
            }

        if (!candidateExists) {
            return HimCandidatePromotionEligibilityResultV1(
                candidateReference =
                    candidateReference,
                status =
                    HimCandidatePromotionEligibilityStatus
                        .BLOCKED_CANDIDATE_MISSING,
            )
        }

        val validation =
            validationLedger.validations.find {
                it.candidateReference ==
                        candidateReference
            }
                ?: return HimCandidatePromotionEligibilityResultV1(
                    candidateReference =
                        candidateReference,
                    status =
                        HimCandidatePromotionEligibilityStatus
                            .BLOCKED_VALIDATION_MISSING,
                )

        if (
            validation.candidateDatasetDigest !=
            currentCandidateDatasetDigest
        ) {
            return HimCandidatePromotionEligibilityResultV1(
                candidateReference =
                    candidateReference,
                status =
                    HimCandidatePromotionEligibilityStatus
                        .BLOCKED_DATASET_MISMATCH,
                validationReference =
                    validation.validationReference,
            )
        }

        return when (validation.decision) {
            HimCandidateValidationDecision.APPROVE ->
                HimCandidatePromotionEligibilityResultV1(
                    candidateReference =
                        candidateReference,
                    status =
                        HimCandidatePromotionEligibilityStatus
                            .PROMOTION_ELIGIBLE,
                    validationReference =
                        validation.validationReference,
                )

            HimCandidateValidationDecision.REJECT ->
                HimCandidatePromotionEligibilityResultV1(
                    candidateReference =
                        candidateReference,
                    status =
                        HimCandidatePromotionEligibilityStatus
                            .BLOCKED_REJECTED,
                    validationReference =
                        validation.validationReference,
                )

            HimCandidateValidationDecision.DEFER ->
                HimCandidatePromotionEligibilityResultV1(
                    candidateReference =
                        candidateReference,
                    status =
                        HimCandidatePromotionEligibilityStatus
                            .BLOCKED_DEFERRED,
                    validationReference =
                        validation.validationReference,
                )
        }
    }

    fun requireEligible(
        candidateReference: HimCandidateReference,
        candidateDataset: HimCandidateDataset,
        currentCandidateDatasetDigest: HimSha256,
        validationLedger: HimCandidateValidationLedgerV1,
    ): HimCandidatePromotionEligibilityResultV1 {
        val result =
            evaluate(
                candidateReference =
                    candidateReference,
                candidateDataset =
                    candidateDataset,
                currentCandidateDatasetDigest =
                    currentCandidateDatasetDigest,
                validationLedger =
                    validationLedger,
            )

        require(result.promotionEligible) {
            "Candidate is not promotion eligible: ${result.status}"
        }

        return result
    }
}