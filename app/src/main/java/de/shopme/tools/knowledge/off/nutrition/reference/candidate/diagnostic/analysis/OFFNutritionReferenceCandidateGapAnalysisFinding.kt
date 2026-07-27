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

        require(recommendedAction.isNotBlank()) {
            "recommendedAction must not be blank."
        }
    }
}