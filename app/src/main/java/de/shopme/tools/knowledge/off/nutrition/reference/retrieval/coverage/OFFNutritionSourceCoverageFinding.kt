package de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage

data class OFFNutritionSourceCoverageFinding(
    val catalogIndex: Int,
    val catalogKey: String,
    val normalizedEnglish: String,
    val retrievalTerms: List<String>,

    val rawOFFProductMatchCount: Int,
    val rawOFFProductWithAnyNutritionCount: Int,
    val rawOFFProductWithUsableNutritionCount: Int,

    val referenceCandidateMatchCount: Int,
    val referenceAggregateMatchCount: Int,
    val matcherCandidateMatchCount: Int,

    val firstMissingStage: OFFNutritionSourceCoverageStage,

    val matchedRawProductNames: List<String>,
    val matchedReferenceCandidateAliases: List<String>,
    val matchedReferenceAggregateAliases: List<String>,
    val matchedMatcherCandidateAliases: List<String>,

    val reasons: List<String>
) {

    init {
        require(catalogIndex >= 0)
        require(catalogKey.isNotBlank())
        require(normalizedEnglish.isNotBlank())

        require(rawOFFProductMatchCount >= 0)
        require(rawOFFProductWithAnyNutritionCount >= 0)
        require(rawOFFProductWithUsableNutritionCount >= 0)
        require(referenceCandidateMatchCount >= 0)
        require(referenceAggregateMatchCount >= 0)
        require(matcherCandidateMatchCount >= 0)

        require(
            rawOFFProductWithAnyNutritionCount <=
                    rawOFFProductMatchCount
        )

        require(
            rawOFFProductWithUsableNutritionCount <=
                    rawOFFProductWithAnyNutritionCount
        )

        require(reasons.isNotEmpty())

        requireSortedDistinct(
            values = retrievalTerms,
            fieldName = "retrievalTerms"
        )

        requireSortedDistinct(
            values = matchedRawProductNames,
            fieldName = "matchedRawProductNames"
        )

        requireSortedDistinct(
            values = matchedReferenceCandidateAliases,
            fieldName = "matchedReferenceCandidateAliases"
        )

        requireSortedDistinct(
            values = matchedReferenceAggregateAliases,
            fieldName = "matchedReferenceAggregateAliases"
        )

        requireSortedDistinct(
            values = matchedMatcherCandidateAliases,
            fieldName = "matchedMatcherCandidateAliases"
        )

        requireSortedDistinct(
            values = reasons,
            fieldName = "reasons"
        )
    }

    private fun requireSortedDistinct(
        values: List<String>,
        fieldName: String
    ) {
        require(
            values ==
                    values
                        .distinct()
                        .sorted()
        ) {
            "$fieldName must be deterministically sorted and distinct."
        }
    }
}