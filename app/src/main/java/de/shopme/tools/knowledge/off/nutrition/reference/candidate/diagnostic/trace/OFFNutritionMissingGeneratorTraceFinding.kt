package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.trace

data class OFFNutritionMissingGeneratorTraceFinding(
    val catalogIndex: Int,
    val catalogKey: String,
    val normalizedEnglish: String,
    val rawOFFProductMatchCount: Int,
    val rawOFFProductWithUsableNutritionCount: Int,
    val matchedRawProductNames: List<String>,
    val sourceCandidateMatch: OFFNutritionArtifactIdentityMatch,
    val qualityFilteredCandidateMatch: OFFNutritionArtifactIdentityMatch,
    val deduplicatedCandidateMatch: OFFNutritionArtifactIdentityMatch,
    val generatorTraceMatch: OFFNutritionArtifactIdentityMatch,
    val firstMissingStage: OFFNutritionMissingGeneratorTraceStage,
    val cause: OFFNutritionMissingGeneratorTraceCause,
    val diagnosticReasons: List<String>,
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

        require(rawOFFProductMatchCount >= 0) {
            "rawOFFProductMatchCount must not be negative."
        }

        require(rawOFFProductWithUsableNutritionCount >= 0) {
            "rawOFFProductWithUsableNutritionCount must not be negative."
        }

        require(
            matchedRawProductNames ==
                    matchedRawProductNames.distinct().sorted()
        ) {
            "matchedRawProductNames must be distinct and sorted."
        }

        require(diagnosticReasons.isNotEmpty()) {
            "diagnosticReasons must not be empty."
        }

        require(recommendedAction.isNotBlank()) {
            "recommendedAction must not be blank."
        }
    }
}