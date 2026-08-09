package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.analysis

data class CanonicalSemanticReviewFamilyAnalysis(
    val rank: Int,

    val category: String,
    val familyKey: String,
    val familyDisplayName: String,

    val generatedCandidateCount: Int,
    val reviewRequiredCandidateCount: Int,

    val reviewShareWithinFamily: Double,
    val shareOfCompleteReviewBacklog: Double,

    val affectedAxisCount: Int,
    val distinctAffectedValueCount: Int,
    val missingAxisPolicyCount: Int,

    val priority:
    CanonicalSemanticPolicyPriority,

    val recommendation:
    CanonicalSemanticBacklogRecommendation
) {

    init {
        require(rank > 0)

        require(category.isNotBlank())
        require(familyKey.isNotBlank())
        require(familyDisplayName.isNotBlank())

        require(generatedCandidateCount > 0)

        require(
            reviewRequiredCandidateCount in
                    0..generatedCandidateCount
        )

        require(reviewShareWithinFamily in 0.0..1.0)
        require(shareOfCompleteReviewBacklog in 0.0..1.0)

        require(affectedAxisCount >= 0)
        require(distinctAffectedValueCount >= 0)
        require(missingAxisPolicyCount >= 0)
    }
}