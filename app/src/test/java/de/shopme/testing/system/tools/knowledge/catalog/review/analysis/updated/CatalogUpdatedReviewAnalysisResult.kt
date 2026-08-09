package de.shopme.testing.system.tools.knowledge.catalog.review.analysis.updated

import de.shopme.testing.system.tools.knowledge.catalog.review.analysis.CatalogReviewResolutionRecommendation
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassification

data class CatalogUpdatedReviewAnalysisResult(
    val version: Int,

    val classifiedEntryCount: Int,
    val potentiallyDeterministicCount: Int,
    val manualOnlyCount: Int,

    val deterministicShare: Double,
    val manualOnlyShare: Double,

    val highestPriorityClassification:
    CatalogReviewBacklogClassification?,

    val nextRecommendedImplementation:
    CatalogReviewResolutionRecommendation?,

    val resolverEffectAssessment:
    CatalogResolverEffectAssessment,

    val remainingDeterministicClassifications:
    Map<CatalogReviewBacklogClassification, Int>,

    val remainingManualClassifications:
    Map<CatalogReviewBacklogClassification, Int>,

    val implementationReady: Boolean,
    val implementationBlockers: List<String>,
    val observations: List<String>,

    val valid: Boolean
) {

    init {
        require(version > 0)

        require(classifiedEntryCount >= 0)
        require(potentiallyDeterministicCount >= 0)
        require(manualOnlyCount >= 0)

        require(
            potentiallyDeterministicCount +
                    manualOnlyCount ==
                    classifiedEntryCount
        ) {
            "Deterministic and manual counts must cover all entries."
        }

        require(deterministicShare in 0.0..1.0)
        require(manualOnlyShare in 0.0..1.0)

        require(
            remainingDeterministicClassifications
                .values
                .sum() ==
                    potentiallyDeterministicCount
        ) {
            "Deterministic classification counts are inconsistent."
        }

        require(
            implementationBlockers.none(String::isBlank)
        )

        require(
            implementationBlockers ==
                    implementationBlockers
                        .distinct()
                        .sorted()
        )

        require(observations.none(String::isBlank))

        require(
            observations ==
                    observations
                        .distinct()
                        .sorted()
        )

        if (classifiedEntryCount == 0) {
            require(highestPriorityClassification == null)
            require(nextRecommendedImplementation == null)
        }

        if (implementationReady) {
            require(nextRecommendedImplementation != null) {
                "Ready implementation requires a recommendation."
            }

            require(implementationBlockers.isEmpty()) {
                "Ready implementation must not contain blockers."
            }
        }

        require(valid) {
            "Updated review analysis must be valid."
        }
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}