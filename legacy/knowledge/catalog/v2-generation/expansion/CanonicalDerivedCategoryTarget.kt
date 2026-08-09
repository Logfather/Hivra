package de.shopme.testing.system.tools.knowledge.catalog.expansion.target

data class CanonicalDerivedCategoryTarget(
    val category: String,

    val sourceTargetEntryCount: Int,
    val additionalDerivedEntryCount: Int,
    val derivedTargetEntryCount: Int,

    val productFamilyCount: Int,

    val families:
    List<CanonicalDerivedProductFamilyTarget>,

    val complexityWeight: Long,

    val valid: Boolean
) {

    init {
        require(category.isNotBlank())

        require(sourceTargetEntryCount > 0)
        require(additionalDerivedEntryCount >= 0)

        require(
            derivedTargetEntryCount ==
                    sourceTargetEntryCount +
                    additionalDerivedEntryCount
        )

        require(productFamilyCount == families.size)
        require(productFamilyCount > 0)

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
            families.sumOf {
                it.sourceAllocatedTargetEntryCount
            } ==
                    sourceTargetEntryCount
        )

        require(
            families.sumOf {
                it.additionalDerivedEntryCount
            } ==
                    additionalDerivedEntryCount
        )

        require(
            families.sumOf {
                it.derivedTargetEntryCount
            } ==
                    derivedTargetEntryCount
        )

        require(
            complexityWeight ==
                    families.sumOf {
                        it.complexityWeight
                    }
        )

        require(valid)
    }
}