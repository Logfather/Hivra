package de.shopme.testing.system.tools.knowledge.catalog.expansion.family

data class CanonicalProductFamilyCategoryCoverage(
    val category: String,

    val baselineEntryCount: Int,

    /**
     * Aktuell empfohlener Zielbestand.
     *
     * Dieser Wert darf zukünftig über dem früheren 10.000er-Anteil liegen,
     * sofern die Produktfamilienanalyse dies fachlich begründet.
     */
    val recommendedTargetEntryCount: Int,

    val familyCount: Int,

    val families:
    List<CanonicalProductFamilyCoverageEntry>,

    val allocatedFamilyTargetEntryCount: Int,

    val valid: Boolean
) {

    init {
        require(category.isNotBlank())
        require(baselineEntryCount >= 0)
        require(recommendedTargetEntryCount >= baselineEntryCount)

        require(familyCount > 0)
        require(familyCount == families.size)

        require(
            families ==
                    families.sortedBy {
                        it.familyKey
                    }
        )

        require(
            families.map { it.familyKey }
                .distinct()
                .size ==
                    families.size
        )

        require(
            families.all {
                it.category == category
            }
        )

        require(
            allocatedFamilyTargetEntryCount ==
                    families.sumOf {
                        it.allocatedTargetEntryCount
                    }
        )

        require(
            allocatedFamilyTargetEntryCount ==
                    recommendedTargetEntryCount
        ) {
            "Product-family targets for '$category' must cover the complete " +
                    "recommended category target."
        }

        require(valid)
    }
}