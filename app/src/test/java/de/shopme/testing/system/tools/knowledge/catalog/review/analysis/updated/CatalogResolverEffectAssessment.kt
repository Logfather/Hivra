package de.shopme.testing.system.tools.knowledge.catalog.review.analysis.updated

import de.shopme.testing.system.tools.knowledge.catalog.review.analysis.CatalogReviewResolutionRecommendation
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassification

data class CatalogResolverEffectAssessment(
    val implementedRecommendation:
    CatalogReviewResolutionRecommendation,

    val affectedClassification:
    CatalogReviewBacklogClassification,

    val baselineAvailable: Boolean,

    val previousEntryCount: Int?,
    val remainingEntryCount: Int,
    val resolvedEntryCount: Int?,
    val resolutionRate: Double?,

    val previousPotentiallyDeterministicCount: Int?,
    val remainingPotentiallyDeterministicCount: Int,
    val resolvedPotentiallyDeterministicCount: Int?,

    val status: CatalogUpdatedReportStatus,
    val rationale: String
) {

    init {
        require(remainingEntryCount >= 0)
        require(remainingPotentiallyDeterministicCount >= 0)

        require(
            remainingPotentiallyDeterministicCount <=
                    remainingEntryCount
        ) {
            "Remaining deterministic count exceeds remaining entry count."
        }

        if (baselineAvailable) {
            val requiredPreviousEntryCount =
                requireNotNull(previousEntryCount) {
                    "Available baseline requires previousEntryCount."
                }

            val requiredResolvedEntryCount =
                requireNotNull(resolvedEntryCount) {
                    "Available baseline requires resolvedEntryCount."
                }

            val requiredResolutionRate =
                requireNotNull(resolutionRate) {
                    "Available baseline requires resolutionRate."
                }

            val requiredPreviousDeterministicCount =
                requireNotNull(
                    previousPotentiallyDeterministicCount
                ) {
                    "Available baseline requires previous deterministic count."
                }

            val requiredResolvedDeterministicCount =
                requireNotNull(
                    resolvedPotentiallyDeterministicCount
                ) {
                    "Available baseline requires resolved deterministic count."
                }

            require(
                remainingEntryCount <=
                        requiredPreviousEntryCount
            ) {
                "Remaining entry count exceeds baseline entry count."
            }

            require(
                requiredResolvedEntryCount ==
                        requiredPreviousEntryCount -
                        remainingEntryCount
            ) {
                "resolvedEntryCount is inconsistent."
            }

            require(
                remainingPotentiallyDeterministicCount <=
                        requiredPreviousDeterministicCount
            ) {
                "Remaining deterministic count exceeds baseline."
            }

            require(
                requiredResolvedDeterministicCount ==
                        requiredPreviousDeterministicCount -
                        remainingPotentiallyDeterministicCount
            ) {
                "Resolved deterministic count is inconsistent."
            }

            require(requiredResolutionRate in 0.0..1.0)
        } else {
            require(previousEntryCount == null)
            require(resolvedEntryCount == null)
            require(resolutionRate == null)

            require(
                previousPotentiallyDeterministicCount == null
            )

            require(
                resolvedPotentiallyDeterministicCount == null
            )

            require(
                status ==
                        CatalogUpdatedReportStatus
                            .EFFECT_BASELINE_MISSING
            ) {
                "Missing baseline requires EFFECT_BASELINE_MISSING status."
            }
        }

        require(rationale.isNotBlank())
    }
}