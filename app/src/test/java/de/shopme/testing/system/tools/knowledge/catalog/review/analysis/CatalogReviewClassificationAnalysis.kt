package de.shopme.testing.system.tools.knowledge.catalog.review.analysis

import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassification

data class CatalogReviewClassificationAnalysis(
    val classification:
    CatalogReviewBacklogClassification,

    val entryCount: Int,
    val shareOfBacklog: Double,

    val potentiallyDeterministicCount: Int,
    val manualReviewRequiredCount: Int,
    val conflictingEvidenceCount: Int,
    val splitRequiredCount: Int,

    val entriesWithUniqueMergeTargetCount: Int,

    val priority:
    CatalogReviewResolutionPriority,

    val recommendation:
    CatalogReviewResolutionRecommendation,

    val rationale: String,

    val exampleSourceIndices: List<Int>
) {

    init {
        require(entryCount > 0) {
            "entryCount must be positive."
        }

        require(shareOfBacklog in 0.0..1.0) {
            "shareOfBacklog must be between 0 and 1."
        }

        require(potentiallyDeterministicCount >= 0)
        require(manualReviewRequiredCount >= 0)
        require(conflictingEvidenceCount >= 0)
        require(splitRequiredCount >= 0)
        require(entriesWithUniqueMergeTargetCount >= 0)

        require(
            potentiallyDeterministicCount +
                    manualReviewRequiredCount +
                    conflictingEvidenceCount +
                    splitRequiredCount ==
                    entryCount
        ) {
            "Assessment counts must cover the classification."
        }

        require(
            entriesWithUniqueMergeTargetCount <=
                    entryCount
        ) {
            "Unique merge-target count exceeds entry count."
        }

        require(rationale.isNotBlank()) {
            "rationale must not be blank."
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