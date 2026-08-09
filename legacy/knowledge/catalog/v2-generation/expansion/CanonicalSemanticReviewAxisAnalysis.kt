package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.analysis

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family
.CanonicalProductFamilyVariantAxis

data class CanonicalSemanticReviewAxisAnalysis(
    val rank: Int,

    val axis:
    CanonicalProductFamilyVariantAxis,

    val affectedCandidateCount: Int,
    val shareOfCompleteReviewBacklog: Double,

    val affectedCategoryCount: Int,
    val affectedFamilyCount: Int,
    val distinctValueCount: Int,

    val missingFamilyPolicyCandidateCount: Int,
    val missingFamilyPolicyShare: Double
) {

    init {
        require(rank > 0)
        require(affectedCandidateCount > 0)

        require(shareOfCompleteReviewBacklog in 0.0..1.0)

        require(affectedCategoryCount > 0)
        require(affectedFamilyCount > 0)
        require(distinctValueCount > 0)

        require(
            missingFamilyPolicyCandidateCount in
                    0..affectedCandidateCount
        )

        require(missingFamilyPolicyShare in 0.0..1.0)
    }
}