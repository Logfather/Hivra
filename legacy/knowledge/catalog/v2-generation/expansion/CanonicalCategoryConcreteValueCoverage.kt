package de.shopme.testing.system.tools.knowledge.catalog.expansion.value

data class CanonicalCategoryConcreteValueCoverage(
    val category: String,

    val productFamilyCount: Int,

    val families:
    List<CanonicalProductFamilyConcreteValueCoverage>,

    val totalAxisCoverageCount: Int,
    val totalSelectedValueCount: Int,

    val completeFamilyCoverage: Boolean,

    val valid: Boolean
) {

    init {
        require(category.isNotBlank())

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
            totalAxisCoverageCount ==
                    families.sumOf {
                        it.axisCoverageCount
                    }
        )

        require(
            totalSelectedValueCount ==
                    families.sumOf {
                        it.totalSelectedValueCount
                    }
        )

        require(
            completeFamilyCoverage ==
                    families.all {
                        it.valid
                    }
        )

        require(
            valid ==
                    completeFamilyCoverage
        )
    }
}