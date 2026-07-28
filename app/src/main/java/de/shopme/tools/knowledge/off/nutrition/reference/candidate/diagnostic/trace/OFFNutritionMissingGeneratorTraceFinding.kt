package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.trace

data class OFFNutritionMissingGeneratorTraceFinding(
    val catalogIndex: Int,
    val catalogKey: String,
    val normalizedEnglish: String,

    val rawOFFProductMatchCount: Int,
    val rawOFFProductWithUsableNutritionCount: Int,

    val matchedRawProductIds: List<String>,
    val matchedRawProductWithUsableNutritionIds: List<String>,
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
            rawOFFProductWithUsableNutritionCount <=
                    rawOFFProductMatchCount
        ) {
            "Usable raw OFF product count must not exceed " +
                    "raw OFF product match count."
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

        require(
            matchedRawProductWithUsableNutritionIds.all(
                matchedRawProductIds::contains
            )
        ) {
            "matchedRawProductWithUsableNutritionIds must be " +
                    "contained in matchedRawProductIds."
        }

        require(
            matchedRawProductIds.size <=
                    rawOFFProductMatchCount
        ) {
            "Persisted raw OFF product IDs must not exceed " +
                    "raw OFF product match count."
        }

        require(
            matchedRawProductWithUsableNutritionIds.size <=
                    rawOFFProductWithUsableNutritionCount
        ) {
            "Persisted usable raw OFF product IDs must not exceed " +
                    "usable raw OFF product count."
        }

        requireSortedDistinct(
            values =
                diagnosticReasons,
            fieldName =
                "diagnosticReasons"
        )

        require(diagnosticReasons.isNotEmpty()) {
            "diagnosticReasons must not be empty."
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