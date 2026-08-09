package de.shopme.testing.system.tools.knowledge.catalog.review.analysis

import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewAutomationAssessment
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassification

data class CatalogReviewBacklogAnalysisResult(
    val version: Int,

    val classifiedEntryCount: Int,

    val potentiallyDeterministicCount: Int,
    val manualReviewRequiredCount: Int,
    val conflictingEvidenceCount: Int,
    val splitRequiredCount: Int,

    val potentiallyDeterministicShare: Double,
    val manualOnlyShare: Double,

    val classificationCount: Int,
    val affectedCategoryCount: Int,

    val countsByClassification:
    Map<CatalogReviewBacklogClassification, Int>,

    val countsByAutomationAssessment:
    Map<CatalogReviewAutomationAssessment, Int>,

    val classificationAnalyses:
    List<CatalogReviewClassificationAnalysis>,

    val categoryAnalyses:
    List<CatalogReviewCategoryAnalysis>,

    val prioritizedRecommendations:
    List<CatalogReviewResolutionRecommendation>,

    val nextRecommendedImplementation:
    CatalogReviewResolutionRecommendation?,

    val valid: Boolean
) {

    init {
        require(version > 0)
        require(classifiedEntryCount >= 0)

        require(potentiallyDeterministicCount >= 0)
        require(manualReviewRequiredCount >= 0)
        require(conflictingEvidenceCount >= 0)
        require(splitRequiredCount >= 0)

        require(potentiallyDeterministicShare in 0.0..1.0)
        require(manualOnlyShare in 0.0..1.0)

        require(classificationCount >= 0)
        require(affectedCategoryCount >= 0)

        require(
            potentiallyDeterministicCount +
                    manualReviewRequiredCount +
                    conflictingEvidenceCount +
                    splitRequiredCount ==
                    classifiedEntryCount
        ) {
            "Assessment counts must cover all classified entries."
        }

        require(
            countsByClassification.values.sum() ==
                    classifiedEntryCount
        ) {
            "countsByClassification must cover all entries."
        }

        require(
            countsByAutomationAssessment.values.sum() ==
                    classifiedEntryCount
        ) {
            "countsByAutomationAssessment must cover all entries."
        }

        require(
            classificationCount ==
                    classificationAnalyses.size
        ) {
            "classificationCount must equal classificationAnalyses size."
        }

        require(
            affectedCategoryCount ==
                    categoryAnalyses.size
        ) {
            "affectedCategoryCount must equal categoryAnalyses size."
        }

        require(
            classificationAnalyses ==
                    classificationAnalyses.sortedWith(
                        compareByDescending<CatalogReviewClassificationAnalysis> {
                            it.entryCount
                        }.thenBy {
                            it.classification.name
                        }
                    )
        ) {
            "classificationAnalyses must be deterministically sorted."
        }

        require(
            categoryAnalyses ==
                    categoryAnalyses.sortedWith(
                        compareByDescending<CatalogReviewCategoryAnalysis> {
                            it.entryCount
                        }.thenBy {
                            it.category
                        }
                    )
        ) {
            "categoryAnalyses must be deterministically sorted."
        }

        require(
            prioritizedRecommendations ==
                    prioritizedRecommendations.distinct()
        ) {
            "prioritizedRecommendations must not contain duplicates."
        }

        if (prioritizedRecommendations.isEmpty()) {
            require(nextRecommendedImplementation == null) {
                "Empty recommendations require no next implementation."
            }
        } else {
            require(
                nextRecommendedImplementation ==
                        prioritizedRecommendations.first()
            ) {
                "nextRecommendedImplementation must equal first priority."
            }
        }

        require(valid) {
            "Catalog review backlog analysis must be valid."
        }
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}