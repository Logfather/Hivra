package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.analysis

data class CanonicalSemanticReviewCategoryAnalysis(
    val rank: Int,

    val category: String,

    val generatedCandidateCount: Int,
    val reviewRequiredCandidateCount: Int,

    val reviewShareWithinCategory: Double,
    val shareOfCompleteReviewBacklog: Double,

    val affectedFamilyCount: Int,
    val affectedAxisCount: Int,
    val missingFamilyAxisPolicyCount: Int
) {

    init {
        require(rank > 0)
        require(category.isNotBlank())

        require(generatedCandidateCount > 0)

        require(
            reviewRequiredCandidateCount in
                    0..generatedCandidateCount
        )

        require(reviewShareWithinCategory in 0.0..1.0)
        require(shareOfCompleteReviewBacklog in 0.0..1.0)

        require(affectedFamilyCount >= 0)
        require(affectedAxisCount >= 0)
        require(missingFamilyAxisPolicyCount >= 0)
    }
}