package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.analysis

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalExpansionSemanticRuleType

data class CanonicalSemanticReviewBacklogAnalysis(
    val version: Int,

    val sourceBaselineId: String,
    val sourceBaselineCatalogSha256: String,

    val generatedCandidateCount: Int,
    val evaluatedCandidateCount: Int,

    val acceptedCandidateCount: Int,
    val rejectedCandidateCount: Int,
    val reviewRequiredCandidateCount: Int,

    val reviewRequiredShare: Double,

    val reviewedCategoryCount: Int,
    val reviewedFamilyCount: Int,
    val reviewedAxisCount: Int,
    val reviewedDistinctValueCount: Int,

    /**
     * Anzahl Review-Kandidaten, bei denen mindestens eine explizite
     * Familien-Achsen-Policy fehlt.
     *
     * Kandidaten werden hier nur einmal gezählt.
     */
    val candidatesWithMissingFamilyAxisPolicyCount: Int,

    val candidatesWithMissingFamilyAxisPolicyShare: Double,

    val missingFamilyAxisPolicyGapCount: Int,

    val reviewFindingCountsByRuleType:
    Map<CanonicalExpansionSemanticRuleType, Int>,

    val categories:
    List<CanonicalSemanticReviewCategoryAnalysis>,

    val families:
    List<CanonicalSemanticReviewFamilyAnalysis>,

    val axes:
    List<CanonicalSemanticReviewAxisAnalysis>,

    val values:
    List<CanonicalSemanticReviewValueAnalysis>,

    val missingFamilyAxisPolicyGaps:
    List<CanonicalMissingFamilyAxisPolicyGap>,

    val highestPriorityImplementationKey: String?,

    val highestPriorityRecommendation:
    CanonicalSemanticBacklogRecommendation?,

    val highestPriorityAffectedCandidateCount: Int,

    val highestPriorityAffectedReviewShare: Double,

    val implementationReady: Boolean,

    val implementationBlockers: List<String>,

    val observations: List<String>,

    val completeReviewCoverage: Boolean,
    val valid: Boolean
) {

    init {
        require(version > 0)
        require(sourceBaselineId.isNotBlank())

        require(
            SHA_256_REGEX.matches(
                sourceBaselineCatalogSha256
            )
        )

        require(generatedCandidateCount >= 0)
        require(evaluatedCandidateCount == generatedCandidateCount)

        require(acceptedCandidateCount >= 0)
        require(rejectedCandidateCount >= 0)
        require(reviewRequiredCandidateCount >= 0)

        require(
            evaluatedCandidateCount ==
                    acceptedCandidateCount +
                    rejectedCandidateCount +
                    reviewRequiredCandidateCount
        )

        require(reviewRequiredShare in 0.0..1.0)

        require(reviewedCategoryCount == categories.size)
        require(reviewedFamilyCount == families.size)
        require(reviewedAxisCount == axes.size)

        require(reviewedDistinctValueCount == values.size)

        require(
            candidatesWithMissingFamilyAxisPolicyCount in
                    0..reviewRequiredCandidateCount
        )

        require(
            candidatesWithMissingFamilyAxisPolicyShare in
                    0.0..1.0
        )

        require(
            missingFamilyAxisPolicyGapCount ==
                    missingFamilyAxisPolicyGaps.size
        )

        require(
            reviewFindingCountsByRuleType ==
                    reviewFindingCountsByRuleType
                        .toList()
                        .sortedBy {
                            it.first.name
                        }
                        .associate {
                            it
                        }
        )

        require(
            categories.map { it.rank } ==
                    (1..categories.size).toList()
        )

        require(
            families.map { it.rank } ==
                    (1..families.size).toList()
        )

        require(
            axes.map { it.rank } ==
                    (1..axes.size).toList()
        )

        require(
            values.map { it.rank } ==
                    (1..values.size).toList()
        )

        require(
            missingFamilyAxisPolicyGaps.map {
                it.rank
            } ==
                    (1..missingFamilyAxisPolicyGaps.size)
                        .toList()
        )

        require(
            highestPriorityAffectedCandidateCount >= 0
        )

        require(
            highestPriorityAffectedReviewShare in
                    0.0..1.0
        )

        if (missingFamilyAxisPolicyGaps.isEmpty()) {
            require(highestPriorityImplementationKey == null)
            require(highestPriorityRecommendation == null)
            require(highestPriorityAffectedCandidateCount == 0)
            require(highestPriorityAffectedReviewShare == 0.0)
        } else {
            val highestPriorityGap =
                missingFamilyAxisPolicyGaps.first()

            require(
                highestPriorityImplementationKey ==
                        highestPriorityGap.implementationKey
            )

            require(
                highestPriorityRecommendation ==
                        highestPriorityGap.recommendation
            )

            require(
                highestPriorityAffectedCandidateCount ==
                        highestPriorityGap.affectedCandidateCount
            )

            require(
                highestPriorityAffectedReviewShare ==
                        highestPriorityGap.affectedReviewShare
            )
        }

        require(
            implementationBlockers ==
                    implementationBlockers
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        )

        require(
            observations ==
                    observations
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
        )

        require(
            implementationReady ==
                    (
                            implementationBlockers.isEmpty() &&
                                    missingFamilyAxisPolicyGaps.isNotEmpty()
                            )
        )

        require(
            completeReviewCoverage ==
                    (
                            reviewRequiredCandidateCount ==
                                    if (reviewRequiredCandidateCount == 0) {
                                        0
                                    } else {
                                        categories.sumOf {
                                            it.reviewRequiredCandidateCount
                                        }
                                    }
                            )
        )

        require(
            valid ==
                    (
                            completeReviewCoverage &&
                                    implementationBlockers.isEmpty()
                            )
        )
    }

    companion object {
        const val CURRENT_VERSION = 1

        private val SHA_256_REGEX =
            Regex("[0-9a-f]{64}")
    }
}