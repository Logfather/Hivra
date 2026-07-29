package de.shopme.tools.knowledge.off.nutrition.reference.quality.policy

data class OFFNutritionReferenceQualityPolicyEvidence(
    val generatedCandidateCount: Long,
    val acceptedCandidateCount: Long,
    val rejectedCandidateCount: Long,
    val acceptanceRate: Double,
    val rejectionReasonOccurrenceCount: Long,
    val multipleReasonRejectionCount: Long,
    val macronutrientSumRejectionCount: Long,
    val macronutrientSumAbove150Count: Long,
    val aboveMaximumRejectionCount: Long,
    val zeroOnlyRejectionCount: Long,
    val sugarsExceedCarbohydratesCount: Long,
    val saturatedFatExceedsTotalFatCount: Long,
    val negativeNutritionValueCount: Long
) {

    init {
        require(generatedCandidateCount >= 0L)
        require(acceptedCandidateCount >= 0L)
        require(rejectedCandidateCount >= 0L)
        require(rejectionReasonOccurrenceCount >= 0L)
        require(multipleReasonRejectionCount >= 0L)
        require(macronutrientSumRejectionCount >= 0L)
        require(macronutrientSumAbove150Count >= 0L)
        require(aboveMaximumRejectionCount >= 0L)
        require(zeroOnlyRejectionCount >= 0L)
        require(sugarsExceedCarbohydratesCount >= 0L)
        require(saturatedFatExceedsTotalFatCount >= 0L)
        require(negativeNutritionValueCount >= 0L)

        require(
            generatedCandidateCount ==
                    acceptedCandidateCount + rejectedCandidateCount
        ) {
            "Generated candidate count must equal accepted plus rejected."
        }

        require(
            acceptanceRate.isFinite() &&
                    acceptanceRate in 0.0..1.0
        )

        val expectedAcceptanceRate =
            if (generatedCandidateCount == 0L) {
                0.0
            } else {
                acceptedCandidateCount.toDouble() /
                        generatedCandidateCount.toDouble()
            }

        require(
            kotlin.math.abs(
                acceptanceRate - expectedAcceptanceRate
            ) <= ACCEPTANCE_RATE_TOLERANCE
        ) {
            "Acceptance rate is inconsistent with candidate counts."
        }

        require(
            multipleReasonRejectionCount <=
                    rejectedCandidateCount
        )

        require(
            macronutrientSumAbove150Count <=
                    macronutrientSumRejectionCount
        )
    }

    private companion object {

        const val ACCEPTANCE_RATE_TOLERANCE =
            1e-12
    }
}