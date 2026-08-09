package de.shopme.testing.system.tools.knowledge.catalog.review.analysis

import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassification

data class CatalogReviewCategoryAnalysis(
    val category: String,
    val entryCount: Int,
    val shareOfBacklog: Double,

    val potentiallyDeterministicCount: Int,
    val manualReviewRequiredCount: Int,
    val conflictingEvidenceCount: Int,
    val splitRequiredCount: Int,

    val countsByClassification:
    Map<CatalogReviewBacklogClassification, Int>,

    val dominantClassification:
    CatalogReviewBacklogClassification,

    val exampleSourceIndices: List<Int>
) {

    init {
        require(category.isNotBlank()) {
            "category must not be blank."
        }

        require(entryCount > 0) {
            "entryCount must be positive."
        }

        require(shareOfBacklog in 0.0..1.0) {
            "shareOfBacklog must be between 0 and 1."
        }

        require(
            potentiallyDeterministicCount +
                    manualReviewRequiredCount +
                    conflictingEvidenceCount +
                    splitRequiredCount ==
                    entryCount
        ) {
            "Assessment counts must cover the category."
        }

        require(
            countsByClassification.values.sum() ==
                    entryCount
        ) {
            "Classification counts must cover the category."
        }

        require(
            countsByClassification[
                dominantClassification
            ] != null
        ) {
            "dominantClassification must occur in the category."
        }

        require(
            exampleSourceIndices ==
                    exampleSourceIndices
                        .distinct()
                        .sorted()
        ) {
            "exampleSourceIndices must be unique and sorted."
        }
    }
}