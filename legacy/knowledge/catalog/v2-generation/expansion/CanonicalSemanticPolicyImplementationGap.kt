package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis

data class CanonicalSemanticPolicyImplementationGap(
    val gapIndex: Int,

    val gapKey: String,

    val category: String,

    val familyKey: String,

    val axis:
    CanonicalProductFamilyVariantAxis,

    val affectedCandidateCount: Int,

    val affectedReviewShare: Double,

    val observedValueCount: Int,

    val observedValues: List<String>,

    val recommendation:
    CanonicalSemanticPolicyImplementationRecommendation,

    val implementationKey: String
) {

    init {
        require(gapIndex > 0)

        require(
            gapKey ==
                    createGapKey(
                        familyKey = familyKey,
                        axis = axis
                    )
        )

        require(category.isNotBlank())
        require(familyKey.isNotBlank())

        require(affectedCandidateCount > 0)

        require(
            affectedReviewShare in 0.0..1.0
        )

        require(observedValueCount == observedValues.size)

        require(
            observedValues ==
                    observedValues
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        ) {
            "Observed values must be normalized, unique and sorted."
        }

        require(observedValues.isNotEmpty())

        require(
            recommendation ==
                    if (observedValueCount == 1) {
                        CanonicalSemanticPolicyImplementationRecommendation
                            .REVIEW_CLOSED_IDENTITY
                    } else {
                        CanonicalSemanticPolicyImplementationRecommendation
                            .CURATE_ALLOWED_VALUES
                    }
        )

        require(
            implementationKey ==
                    "implement-$familyKey-" +
                    axis.name
                        .lowercase()
                        .replace('_', '-') +
                    "-policy"
        )
    }

    companion object {

        fun createGapKey(
            familyKey: String,
            axis: CanonicalProductFamilyVariantAxis
        ): String =
            "$familyKey::${axis.name}"
    }
}