package de.shopme.testing.system.tools.knowledge.catalog.expansion.variant

data class CanonicalProductFamilyVariantCoverageEntry(
    val familyKey: String,
    val category: String,
    val displayName: String,

    /**
     * Bisherige Zielallokation der Familie aus dem Product-Family-Coverage-
     * Artefakt.
     */
    val allocatedTargetEntryCount: Int,

    val axisRequirementCount: Int,
    val requiredAxisCount: Int,
    val recommendedAxisCount: Int,
    val optionalAxisCount: Int,

    val requirements:
    List<CanonicalVariantAxisCoverageRequirement>,

    /**
     * Summe der empfohlenen Achsenausprägungen.
     *
     * Dies ist bewusst keine kartesische Produktmenge.
     */
    val recommendedVariantValueCoverageCount: Int,

    val crossAxisCombinationAllowedCount: Int,
    val crossAxisCombinationRestrictedCount: Int,

    val completeAxisCoverage: Boolean,
    val valid: Boolean
) {

    init {
        require(familyKey.isNotBlank())
        require(category.isNotBlank())
        require(displayName.isNotBlank())

        require(allocatedTargetEntryCount > 0)

        require(axisRequirementCount == requirements.size)
        require(axisRequirementCount > 0)

        require(
            requirements ==
                    requirements.sortedBy {
                        it.axis.name
                    }
        ) {
            "Variant requirements must be sorted by axis for '$familyKey'."
        }

        require(
            requirements.map { it.axis }
                .distinct()
                .size ==
                    requirements.size
        ) {
            "Variant requirements must use unique axes for '$familyKey'."
        }

        require(
            requiredAxisCount ==
                    requirements.count {
                        it.criticality ==
                                CanonicalVariantAxisCriticality.REQUIRED
                    }
        )

        require(
            recommendedAxisCount ==
                    requirements.count {
                        it.criticality ==
                                CanonicalVariantAxisCriticality.RECOMMENDED
                    }
        )

        require(
            optionalAxisCount ==
                    requirements.count {
                        it.criticality ==
                                CanonicalVariantAxisCriticality.OPTIONAL
                    }
        )

        require(
            axisRequirementCount ==
                    requiredAxisCount +
                    recommendedAxisCount +
                    optionalAxisCount
        )

        require(
            recommendedVariantValueCoverageCount ==
                    requirements.sumOf {
                        it.recommendedRelevantValueCount
                    }
        )

        require(
            crossAxisCombinationAllowedCount ==
                    requirements.count {
                        it.allowCrossAxisCombination
                    }
        )

        require(
            crossAxisCombinationRestrictedCount ==
                    requirements.count {
                        !it.allowCrossAxisCombination
                    }
        )

        require(
            completeAxisCoverage ==
                    (
                            requirements.isNotEmpty() &&
                                    requirements.all {
                                        it.minimumRelevantValueCount > 0
                                    }
                            )
        )

        require(
            valid ==
                    completeAxisCoverage
        )
    }
}