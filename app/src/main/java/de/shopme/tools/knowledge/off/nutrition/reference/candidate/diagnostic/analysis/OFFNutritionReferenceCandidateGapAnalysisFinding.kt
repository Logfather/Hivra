package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.analysis

data class OFFNutritionReferenceCandidateGapAnalysisFinding(
    val catalogIndex: Int,
    val catalogKey: String,
    val normalizedEnglish: String,
    val firstMissingStage: String,

    val rawOFFProductMatchCount: Int,
    val rawOFFProductWithAnyNutritionCount: Int,
    val rawOFFProductWithUsableNutritionCount: Int,

    val referenceCandidateMatchCount: Int,
    val referenceAggregateMatchCount: Int,
    val matcherCandidateMatchCount: Int,

    val matchedRawProductIds: List<String>,
    val matchedRawProductWithUsableNutritionIds: List<String>,
    val matchedRawProductNames: List<String>,
    val diagnosticReasons: List<String>,

    val cause: OFFNutritionReferenceCandidateGapCause,
    val priority: OFFNutritionReferenceCandidateGapPriority,
    val recommendedAction: String
) {

    init {
        require(catalogIndex >= 0) {
            "catalogIndex must not be negative."
        }

        require(catalogKey.isNotBlank()) {
            "catalogKey must not be blank."
        }

        require(normalizedEnglish.isNotBlank()) {
            "normalizedEnglish must not be blank."
        }

        require(firstMissingStage.isNotBlank()) {
            "firstMissingStage must not be blank."
        }

        require(rawOFFProductMatchCount >= 0) {
            "rawOFFProductMatchCount must not be negative."
        }

        require(rawOFFProductWithAnyNutritionCount >= 0) {
            "rawOFFProductWithAnyNutritionCount must not be negative."
        }

        require(rawOFFProductWithUsableNutritionCount >= 0) {
            "rawOFFProductWithUsableNutritionCount must not be negative."
        }

        require(referenceCandidateMatchCount >= 0) {
            "referenceCandidateMatchCount must not be negative."
        }

        require(referenceAggregateMatchCount >= 0) {
            "referenceAggregateMatchCount must not be negative."
        }

        require(matcherCandidateMatchCount >= 0) {
            "matcherCandidateMatchCount must not be negative."
        }

        require(
            rawOFFProductWithUsableNutritionCount <=
                    rawOFFProductWithAnyNutritionCount
        ) {
            "Usable nutrition count must not exceed any-nutrition count."
        }

        require(
            rawOFFProductWithAnyNutritionCount <=
                    rawOFFProductMatchCount
        ) {
            "Any-nutrition count must not exceed raw product match count."
        }

        requireSortedDistinct(
            values =
                matchedRawProductIds,
            fieldName =
                "matchedRawProductIds"
        )

        requireSortedDistinct(
            values =
                matchedRawProductWithUsableNutritionIds,
            fieldName =
                "matchedRawProductWithUsableNutritionIds"
        )

        requireSortedDistinct(
            values =
                matchedRawProductNames,
            fieldName =
                "matchedRawProductNames"
        )

        requireSortedDistinct(
            values =
                diagnosticReasons,
            fieldName =
                "diagnosticReasons"
        )

        require(
            matchedRawProductWithUsableNutritionIds.all(
                matchedRawProductIds::contains
            )
        ) {
            "Usable nutrition product IDs must be contained in " +
                    "matchedRawProductIds."
        }

        require(
            matchedRawProductIds.size <=
                    rawOFFProductMatchCount
        ) {
            "Persisted raw product IDs must not exceed raw product match count."
        }

        require(
            matchedRawProductWithUsableNutritionIds.size <=
                    rawOFFProductWithUsableNutritionCount
        ) {
            "Persisted usable product IDs must not exceed usable nutrition count."
        }

        require(recommendedAction.isNotBlank()) {
            "recommendedAction must not be blank."
        }
    }

    private fun requireSortedDistinct(
        values: List<String>,
        fieldName: String
    ) {
        require(
            values ==
                    values
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        ) {
            "$fieldName must contain non-blank values and be " +
                    "deterministically sorted and distinct."
        }
    }
}