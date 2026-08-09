package de.shopme.testing.system.tools.knowledge.catalog.expansion.family

data class CanonicalProductFamilyCoverageEntry(
    val familyKey: String,
    val category: String,
    val displayName: String,

    val allocationWeight: Int,

    val baselineCategoryEntryCount: Int,
    val categoryTargetEntryCount: Int,

    /**
     * Zielbestand, der dieser Familie innerhalb ihrer Kategorie
     * deterministisch zugeordnet wurde.
     */
    val allocatedTargetEntryCount: Int,

    val allocatedTargetShareOfCategory: Double,
    val allocatedTargetShareOfCatalog: Double,

    val allowedVariantAxes:
    List<CanonicalProductFamilyVariantAxis>,

    val rationale: String
) {

    init {
        require(familyKey.isNotBlank())
        require(category.isNotBlank())
        require(displayName.isNotBlank())

        require(allocationWeight > 0)

        require(baselineCategoryEntryCount >= 0)
        require(categoryTargetEntryCount > 0)
        require(allocatedTargetEntryCount > 0)

        require(
            allocatedTargetEntryCount <=
                    categoryTargetEntryCount
        )

        require(
            allocatedTargetShareOfCategory in 0.0..1.0
        )

        require(
            allocatedTargetShareOfCatalog in 0.0..1.0
        )

        require(allowedVariantAxes.isNotEmpty())
        require(rationale.isNotBlank())
    }
}