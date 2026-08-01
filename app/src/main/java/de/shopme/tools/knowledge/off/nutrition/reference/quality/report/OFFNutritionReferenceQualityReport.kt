package de.shopme.tools.knowledge.off.nutrition.reference.quality.report

import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityFilterResult
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityRejectionReason

data class OFFNutritionReferenceQualityReport(
    val version: Int,
    val inputCandidateCount: Int,
    val acceptedCandidateCount: Int,
    val rejectedCandidateCount: Int,
    val rejectionReasonOccurrenceCount: Int,
    val acceptanceRate: Double,
    val countsByReason:
    Map<OFFNutritionReferenceQualityRejectionReason, Int>
) {

    init {
        require(version > 0) {
            "Version must be greater than zero."
        }

        require(inputCandidateCount >= 0) {
            "Input candidate count must not be negative."
        }

        require(acceptedCandidateCount >= 0) {
            "Accepted candidate count must not be negative."
        }

        require(rejectedCandidateCount >= 0) {
            "Rejected candidate count must not be negative."
        }

        require(rejectionReasonOccurrenceCount >= 0) {
            "Rejection reason occurrence count must not be negative."
        }

        require(
            acceptedCandidateCount +
                    rejectedCandidateCount ==
                    inputCandidateCount
        ) {
            "Accepted and rejected candidate counts must cover all input candidates."
        }

        require(
            acceptanceRate in 0.0..1.0
        ) {
            "Acceptance rate must be between zero and one."
        }

        require(
            countsByReason.keys.toList() ==
                    countsByReason.keys.sortedBy { reason ->
                        reason.name
                    }
        ) {
            "Counts by reason must be ordered deterministically by reason name."
        }

        require(
            countsByReason.values.all { count ->
                count >= 0
            }
        ) {
            "Counts by reason must not contain negative values."
        }

        require(
            countsByReason.values.sum() ==
                    rejectionReasonOccurrenceCount
        ) {
            "Rejection reason occurrence count must equal the sum of counts by reason."
        }
    }

    companion object {

        const val CURRENT_VERSION =
            1

        fun from(
            filterResult:
            OFFNutritionReferenceQualityFilterResult
        ): OFFNutritionReferenceQualityReport {

            val completeCountsByReason =
                OFFNutritionReferenceQualityRejectionReason
                    .entries
                    .associateWith { reason ->
                        filterResult.countsByReason[reason]
                            ?: 0
                    }
                    .toSortedMap(
                        compareBy { reason ->
                            reason.name
                        }
                    )

            val acceptanceRate =
                if (filterResult.inputCandidateCount == 0) {
                    0.0
                } else {
                    filterResult.acceptedCandidateCount
                        .toDouble() /
                            filterResult.inputCandidateCount
                                .toDouble()
                }

            return OFFNutritionReferenceQualityReport(
                version =
                    CURRENT_VERSION,
                inputCandidateCount =
                    filterResult.inputCandidateCount,
                acceptedCandidateCount =
                    filterResult.acceptedCandidateCount,
                rejectedCandidateCount =
                    filterResult.rejectedCandidateCount,
                rejectionReasonOccurrenceCount =
                    completeCountsByReason.values.sum(),
                acceptanceRate =
                    acceptanceRate,
                countsByReason =
                    completeCountsByReason
            )
        }
    }
}