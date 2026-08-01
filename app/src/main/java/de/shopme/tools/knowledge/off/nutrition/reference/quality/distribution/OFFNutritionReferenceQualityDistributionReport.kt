package de.shopme.tools.knowledge.off.nutrition.reference.quality.distribution

data class OFFNutritionReferenceQualityDistributionReport(
    val version: Int,
    val analyzedCandidateCount: Long,
    val acceptedCandidateCount: Long,
    val rejectedCandidateCount: Long,
    val rejectionReasonOccurrenceCount: Long,
    val multipleReasonRejectionCount: Long,
    val countsByReason: Map<String, Long>,
    val countsByReasonCombination: Map<String, Long>,
    val countsByReasonCount: Map<String, Long>,
    val negativeValueCountsByNutritionKey: Map<String, Long>,
    val aboveMaximumCountsByNutritionKey: Map<String, Long>,
    val aboveMaximumExcessBucketsByNutritionKey:
    Map<String, Map<String, Long>>,
    val macronutrientSumBuckets: Map<String, Long>,
    val sugarExcessBuckets: Map<String, Long>,
    val saturatedFatExcessBuckets: Map<String, Long>,
    val zeroOnlyCountsByPresentKeyCount: Map<String, Long>,
    val valueRangesByNutritionKey:
    Map<String, OFFNutritionValueRange>,
    val examplesByReason:
    Map<String, List<OFFNutritionReferenceQualityDistributionExample>>
) {

    init {
        require(version > 0)
        require(analyzedCandidateCount >= 0L)
        require(acceptedCandidateCount >= 0L)
        require(rejectedCandidateCount >= 0L)
        require(rejectionReasonOccurrenceCount >= 0L)
        require(multipleReasonRejectionCount >= 0L)

        require(
            analyzedCandidateCount ==
                    acceptedCandidateCount + rejectedCandidateCount
        ) {
            "Analyzed candidates must equal accepted plus rejected."
        }

        require(
            rejectionReasonOccurrenceCount ==
                    countsByReason.values.sum()
        ) {
            "Reason occurrence count must equal countsByReason sum."
        }

        require(
            rejectedCandidateCount ==
                    countsByReasonCombination.values.sum()
        ) {
            "Every rejection must belong to one reason combination."
        }

        require(
            rejectedCandidateCount ==
                    countsByReasonCount.values.sum()
        ) {
            "Every rejection must belong to one reason-count bucket."
        }
    }

    companion object {

        const val CURRENT_VERSION =
            1
    }
}