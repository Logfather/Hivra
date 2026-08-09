package de.shopme.testing.system.tools.knowledge.catalog.expansion.combination

data class CanonicalProductFamilyCombinationCoverageEntry(
    val familyKey: String,
    val category: String,
    val displayName: String,

    val derivedTargetEntryCount: Int,

    val templateCount: Int,

    val templates:
    List<CanonicalVariantCombinationTemplate>,

    /**
     * Summe der expliziten Materialisierungslimits aller Templates.
     *
     * Dies ist eine Kapazitätsgrenze, keine erzeugte Artikelanzahl.
     */
    val curatedCombinationCapacity: Int,

    val requiredTemplateCount: Int,
    val optionalTemplateCount: Int,

    val targetCapacityReached: Boolean,

    val cartesianExpansionForbidden: Boolean,

    val valid: Boolean
) {

    init {
        require(familyKey.isNotBlank())
        require(category.isNotBlank())
        require(displayName.isNotBlank())

        require(derivedTargetEntryCount > 0)

        require(templateCount == templates.size)
        require(templateCount > 0)

        require(
            templates ==
                    templates.sortedBy { it.key }
        )

        require(
            templates.map { it.key }
                .distinct()
                .size ==
                    templates.size
        )

        require(
            curatedCombinationCapacity ==
                    templates.sumOf {
                        it.maximumMaterializedCombinationCount
                    }
        )

        require(
            requiredTemplateCount ==
                    templates.count { it.required }
        )

        require(
            optionalTemplateCount ==
                    templates.count { !it.required }
        )

        require(
            templateCount ==
                    requiredTemplateCount +
                    optionalTemplateCount
        )

        require(
            targetCapacityReached ==
                    (
                            curatedCombinationCapacity >=
                                    derivedTargetEntryCount
                            )
        )

        require(cartesianExpansionForbidden)

        require(
            valid ==
                    (
                            templates.isNotEmpty() &&
                                    requiredTemplateCount > 0 &&
                                    targetCapacityReached &&
                                    cartesianExpansionForbidden
                            )
        )
    }
}