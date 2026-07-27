package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.trace

data class OFFNutritionArtifactIdentityMatch(
    val matchedEntryCount: Int,
    val matchedIdentities: List<String>,
    val matchedSourceProductIds: List<String>,
    val matchedProductNames: List<String>
) {

    init {
        require(matchedEntryCount >= 0) {
            "matchedEntryCount must not be negative."
        }

        require(
            matchedIdentities ==
                    matchedIdentities.distinct().sorted()
        ) {
            "matchedIdentities must be distinct and sorted."
        }

        require(
            matchedSourceProductIds ==
                    matchedSourceProductIds.distinct().sorted()
        ) {
            "matchedSourceProductIds must be distinct and sorted."
        }

        require(
            matchedProductNames ==
                    matchedProductNames.distinct().sorted()
        ) {
            "matchedProductNames must be distinct and sorted."
        }
    }

    val present: Boolean
        get() =
            matchedEntryCount > 0

    companion object {

        val EMPTY =
            OFFNutritionArtifactIdentityMatch(
                matchedEntryCount =
                    0,
                matchedIdentities =
                    emptyList(),
                matchedSourceProductIds =
                    emptyList(),
                matchedProductNames =
                    emptyList()
            )
    }
}