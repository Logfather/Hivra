package de.shopme.testing.system.tools.knowledge.catalog.review

import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationAction

data class CatalogReviewBacklogEntry(
    val sourceIndex: Int,
    val originalItemName: String,
    val originalCategory: String?,
    val originalNormalizedKey: String?,

    val originalAction: CatalogCanonicalizationAction,
    val originalAutomatic: Boolean,

    val resultingItemName: String?,
    val resultingCategory: String?,
    val resultingNormalizedKey: String?,
    val mergeTargetSourceIndex: Int?,

    val originalReasons: List<String>,
    val status: CatalogReviewBacklogStatus,
    val resolutionReason: String
) {

    init {
        require(sourceIndex >= 0) {
            "sourceIndex must not be negative."
        }

        require(originalItemName.isNotBlank()) {
            "originalItemName must not be blank."
        }

        require(originalReasons.none(String::isBlank)) {
            "originalReasons must not contain blank values."
        }

        require(
            originalReasons.distinct().size ==
                    originalReasons.size
        ) {
            "originalReasons must not contain duplicates."
        }

        require(
            originalReasons ==
                    originalReasons.sorted()
        ) {
            "originalReasons must be deterministically sorted."
        }

        require(resolutionReason.isNotBlank()) {
            "resolutionReason must not be blank."
        }

        if (
            status ==
            CatalogReviewBacklogStatus
                .RESOLVED_BY_DUPLICATE_MERGE
        ) {
            requireNotNull(mergeTargetSourceIndex) {
                "Resolved duplicate merge must contain a target."
            }

            require(mergeTargetSourceIndex != sourceIndex) {
                "Resolved duplicate must not target itself."
            }
        }

        if (
            status ==
            CatalogReviewBacklogStatus
                .STILL_SPLIT_REQUIRED
        ) {
            require(
                originalAction ==
                        CatalogCanonicalizationAction.SPLIT
            ) {
                "STILL_SPLIT_REQUIRED requires original SPLIT action."
            }
        }
    }
}