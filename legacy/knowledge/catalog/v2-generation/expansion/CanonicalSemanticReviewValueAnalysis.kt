package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.analysis

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family
.CanonicalProductFamilyVariantAxis

data class CanonicalSemanticReviewValueAnalysis(
    val rank: Int,

    val axis:
    CanonicalProductFamilyVariantAxis,

    val valueKey: String,
    val displayName: String,

    val affectedCandidateCount: Int,
    val shareOfCompleteReviewBacklog: Double,

    val affectedCategoryCount: Int,
    val affectedFamilyCount: Int,

    val missingFamilyPolicyCandidateCount: Int
) {

    init {
        require(rank > 0)

        require(valueKey.isNotBlank())
        require(displayName.isNotBlank())

        require(affectedCandidateCount > 0)
        require(shareOfCompleteReviewBacklog in 0.0..1.0)

        require(affectedCategoryCount > 0)
        require(affectedFamilyCount > 0)

        require(
            missingFamilyPolicyCandidateCount in
                    0..affectedCandidateCount
        )
    }
}