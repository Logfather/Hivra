package de.shopme.testing.system.tools.knowledge.catalog.review.reclassification

data class CatalogMisclassifiedTypoEntry(
    val sourceIndex: Int,
    val sourceName: String,
    val sourceCategory: String?,

    val targetSourceIndex: Int,
    val targetName: String,
    val targetCategory: String?,

    val originalSubtype: String,

    val reclassifiedAs:
    CatalogMisclassifiedTypoClass,

    val recommendation:
    CatalogMisclassifiedTypoRecommendation,

    val matchedMarkers: List<String>,
    val reasons: List<String>
) {

    init {
        require(sourceIndex >= 0)
        require(targetSourceIndex >= 0)
        require(sourceIndex != targetSourceIndex)

        require(sourceName.isNotBlank())
        require(targetName.isNotBlank())
        require(originalSubtype.isNotBlank())

        require(
            matchedMarkers ==
                    matchedMarkers
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        ) {
            "matchedMarkers must be normalized and sorted."
        }

        require(reasons.isNotEmpty())

        require(
            reasons ==
                    reasons
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        ) {
            "reasons must be normalized and sorted."
        }
    }
}