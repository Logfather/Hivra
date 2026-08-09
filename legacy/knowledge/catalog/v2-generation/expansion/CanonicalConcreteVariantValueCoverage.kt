package de.shopme.testing.system.tools.knowledge.catalog.expansion.value

data class CanonicalConcreteVariantValueCoverage(
    val version: Int,

    val sourceBaselineId: String,
    val sourceBaselineCatalogSha256: String,

    val baselineEntryCount: Int,

    val derivedTargetEntryCount: Int,
    val requiredExpansionEntryCount: Int,

    val minimumAllowedTargetEntryCount: Int,
    val maximumAllowedTargetEntryCount: Int,

    val canonicalAxisValueSetCount: Int,
    val canonicalVariantValueCount: Int,

    val categoryCount: Int,
    val productFamilyCount: Int,

    val axisCoverageCount: Int,
    val selectedFamilyAxisValueCount: Int,

    val categories:
    List<CanonicalCategoryConcreteValueCoverage>,

    val axisValueSets:
    List<CanonicalVariantAxisValueSet>,

    val familiesWithCompleteValueCoverageCount: Int,
    val familiesWithIncompleteValueCoverageCount: Int,

    val readyForCombinationCuration: Boolean,

    val blockers: List<String>,

    val valid: Boolean
) {

    init {
        require(version > 0)
        require(sourceBaselineId.isNotBlank())

        require(
            SHA_256_REGEX.matches(
                sourceBaselineCatalogSha256
            )
        )

        require(baselineEntryCount > 0)

        require(
            derivedTargetEntryCount in
                    minimumAllowedTargetEntryCount..
                    maximumAllowedTargetEntryCount
        )

        require(
            requiredExpansionEntryCount ==
                    derivedTargetEntryCount -
                    baselineEntryCount
        )

        require(canonicalAxisValueSetCount == axisValueSets.size)

        require(
            canonicalVariantValueCount ==
                    axisValueSets.sumOf {
                        it.valueCount
                    }
        )

        require(
            axisValueSets ==
                    axisValueSets.sortedBy {
                        it.axis.name
                    }
        )

        require(
            axisValueSets.map { it.axis }
                .distinct()
                .size ==
                    axisValueSets.size
        )

        require(categoryCount == categories.size)

        require(
            productFamilyCount ==
                    categories.sumOf {
                        it.productFamilyCount
                    }
        )

        require(
            axisCoverageCount ==
                    categories.sumOf {
                        it.totalAxisCoverageCount
                    }
        )

        require(
            selectedFamilyAxisValueCount ==
                    categories.sumOf {
                        it.totalSelectedValueCount
                    }
        )

        require(
            categories ==
                    categories.sortedBy {
                        it.category
                    }
        )

        val allFamilies =
            categories.flatMap {
                it.families
            }

        require(
            familiesWithCompleteValueCoverageCount ==
                    allFamilies.count {
                        it.valid
                    }
        )

        require(
            familiesWithIncompleteValueCoverageCount ==
                    allFamilies.count {
                        !it.valid
                    }
        )

        require(
            productFamilyCount ==
                    familiesWithCompleteValueCoverageCount +
                    familiesWithIncompleteValueCoverageCount
        )

        require(
            blockers ==
                    blockers
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        )

        require(
            readyForCombinationCuration ==
                    (
                            blockers.isEmpty() &&
                                    familiesWithIncompleteValueCoverageCount == 0
                            )
        )

        require(
            valid ==
                    readyForCombinationCuration
        )
    }

    companion object {
        const val CURRENT_VERSION = 1

        private val SHA_256_REGEX =
            Regex("[0-9a-f]{64}")
    }
}