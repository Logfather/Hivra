package de.shopme.testing.system.tools.knowledge.catalog.expansion.combination

data class CanonicalCategoryCombinationCoverage(
    val category: String,

    val derivedTargetEntryCount: Int,

    val productFamilyCount: Int,

    val families:
    List<CanonicalProductFamilyCombinationCoverageEntry>,

    val templateCount: Int,
    val curatedCombinationCapacity: Int,

    val completeFamilyCoverage: Boolean,

    val valid: Boolean
) {

    init {
        require(category.isNotBlank())
        require(derivedTargetEntryCount > 0)

        require(productFamilyCount == families.size)
        require(productFamilyCount > 0)

        require(
            families ==
                    families.sortedBy { it.familyKey }
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
                it.derivedTargetEntryCount
            } ==
                    derivedTargetEntryCount
        )

        require(
            templateCount ==
                    families.sumOf {
                        it.templateCount
                    }
        )

        require(
            curatedCombinationCapacity ==
                    families.sumOf {
                        it.curatedCombinationCapacity
                    }
        )

        require(
            completeFamilyCoverage ==
                    families.all {
                        it.valid
                    }
        )

        require(valid == completeFamilyCoverage)
    }
}