package de.shopme.testing.system.tools.knowledge.catalog.expansion.variant

data class CanonicalCategoryVariantCoverage(
    val category: String,

    val productFamilyCount: Int,

    val families:
    List<CanonicalProductFamilyVariantCoverageEntry>,

    val allocatedTargetEntryCount: Int,

    val totalAxisRequirementCount: Int,
    val totalRecommendedVariantValueCoverageCount: Int,

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
            allocatedTargetEntryCount ==
                    families.sumOf {
                        it.allocatedTargetEntryCount
                    }
        )

        require(
            totalAxisRequirementCount ==
                    families.sumOf {
                        it.axisRequirementCount
                    }
        )

        require(
            totalRecommendedVariantValueCoverageCount ==
                    families.sumOf {
                        it.recommendedVariantValueCoverageCount
                    }
        )

        require(
            completeFamilyCoverage ==
                    families.all {
                        it.completeAxisCoverage
                    }
        )

        require(
            valid ==
                    (
                            completeFamilyCoverage &&
                                    families.all {
                                        it.valid
                                    }
                            )
        )
    }
}