package de.shopme.testing.system.tools.knowledge.catalog.review.classification

import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationAction
import de.shopme.testing.system.tools.knowledge.catalog.review.CatalogReviewBacklogStatus

data class ClassifiedCatalogReviewBacklogEntry(
    val sourceIndex: Int,

    val itemName: String,
    val category: String?,
    val normalizedKey: String?,

    val originalAction: CatalogCanonicalizationAction,
    val backlogStatus: CatalogReviewBacklogStatus,

    val mergeTargetSourceIndex: Int?,

    val primaryClassification:
    CatalogReviewBacklogClassification,

    val detectedClassifications:
    List<CatalogReviewBacklogClassification>,

    val automationAssessment:
    CatalogReviewAutomationAssessment,

    val hasUniqueMergeTarget: Boolean,
    val hasMultipleConflictReasons: Boolean,

    val originalReasons: List<String>,
    val classificationReasons: List<String>
) {

    init {
        require(sourceIndex >= 0) {
            "sourceIndex must not be negative."
        }

        require(itemName.isNotBlank()) {
            "itemName must not be blank."
        }

        require(
            detectedClassifications.isNotEmpty()
        ) {
            "detectedClassifications must not be empty."
        }

        require(
            primaryClassification in
                    detectedClassifications
        ) {
            "primaryClassification must be part of " +
                    "detectedClassifications."
        }

        require(
            detectedClassifications ==
                    detectedClassifications
                        .distinct()
                        .sortedBy { it.name }
        ) {
            "detectedClassifications must be unique and sorted."
        }

        require(
            originalReasons.none(String::isBlank)
        ) {
            "originalReasons must not contain blank values."
        }

        require(
            originalReasons ==
                    originalReasons.distinct().sorted()
        ) {
            "originalReasons must be unique and sorted."
        }

        require(
            classificationReasons.isNotEmpty()
        ) {
            "classificationReasons must not be empty."
        }

        require(
            classificationReasons.none(String::isBlank)
        ) {
            "classificationReasons must not contain blank values."
        }

        require(
            classificationReasons ==
                    classificationReasons.distinct().sorted()
        ) {
            "classificationReasons must be unique and sorted."
        }

        require(
            hasUniqueMergeTarget ==
                    (mergeTargetSourceIndex != null)
        ) {
            "hasUniqueMergeTarget must match mergeTargetSourceIndex."
        }

        if (
            automationAssessment ==
            CatalogReviewAutomationAssessment
                .POTENTIALLY_DETERMINISTIC
        ) {
            require(hasUniqueMergeTarget) {
                "Potentially deterministic duplicate review requires " +
                        "a unique merge target."
            }

            require(!hasMultipleConflictReasons) {
                "Potentially deterministic review must not contain " +
                        "multiple conflict reasons."
            }
        }

        if (
            automationAssessment ==
            CatalogReviewAutomationAssessment
                .SPLIT_REQUIRED
        ) {
            require(
                primaryClassification ==
                        CatalogReviewBacklogClassification
                            .SPLIT_REQUIRED
            ) {
                "SPLIT_REQUIRED assessment requires matching " +
                        "classification."
            }
        }
    }
}