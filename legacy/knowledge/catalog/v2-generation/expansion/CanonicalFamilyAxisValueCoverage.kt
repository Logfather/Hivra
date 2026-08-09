package de.shopme.testing.system.tools.knowledge.catalog.expansion.value

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.variant.CanonicalVariantAxisCriticality
import de.shopme.testing.system.tools.knowledge.catalog.expansion.variant.CanonicalVariantAxisSelectionMode

data class CanonicalFamilyAxisValueCoverage(
    val axis: CanonicalProductFamilyVariantAxis,

    val criticality: CanonicalVariantAxisCriticality,

    val selectionMode: CanonicalVariantAxisSelectionMode,

    val minimumRequiredValueCount: Int,
    val recommendedValueCount: Int,
    val maximumAllowedValueCount: Int,

    /**
     * Konkrete, für die spätere Familienanalyse vorausgewählte Werte.
     *
     * Dies ist noch keine finale kartesische Variantenkombination.
     */
    val selectedValues: List<CanonicalVariantValue>,

    val selectedValueCount: Int,

    val minimumCoverageReached: Boolean,
    val recommendedCoverageReached: Boolean,
    val maximumLimitRespected: Boolean,

    val valid: Boolean
) {

    init {
        require(minimumRequiredValueCount > 0)

        require(
            recommendedValueCount >=
                    minimumRequiredValueCount
        )

        require(
            maximumAllowedValueCount >=
                    recommendedValueCount
        )

        require(selectedValueCount == selectedValues.size)

        require(
            selectedValues ==
                    selectedValues.sortedBy {
                        it.key
                    }
        )

        require(
            selectedValues.map { it.key }
                .distinct()
                .size ==
                    selectedValues.size
        )

        require(
            minimumCoverageReached ==
                    (
                            selectedValueCount >=
                                    minimumRequiredValueCount
                            )
        )

        require(
            recommendedCoverageReached ==
                    (
                            selectedValueCount >=
                                    recommendedValueCount
                            )
        )

        require(
            maximumLimitRespected ==
                    (
                            selectedValueCount <=
                                    maximumAllowedValueCount
                            )
        )

        require(
            valid ==
                    (
                            minimumCoverageReached &&
                                    recommendedCoverageReached &&
                                    maximumLimitRespected
                            )
        )
    }
}