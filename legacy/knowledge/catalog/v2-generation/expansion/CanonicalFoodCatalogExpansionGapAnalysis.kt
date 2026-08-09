package de.shopme.testing.system.tools.knowledge.catalog.expansion.analysis

data class CanonicalFoodCatalogExpansionGapAnalysis(
    val version: Int,

    val sourceBaselineId: String,
    val sourceBaselineCatalogSha256: String,

    val baselineEntryCount: Int,
    val targetEntryCount: Int,
    val requiredExpansionEntryCount: Int,

    val analyzedCategoryCount: Int,

    val categories:
    List<CanonicalFoodCatalogCategoryGap>,

    val categoryCountsByPriority:
    Map<CanonicalFoodCatalogExpansionGapPriority, Int>,

    val expansionCountsByPriority:
    Map<CanonicalFoodCatalogExpansionGapPriority, Int>,

    val largestAbsoluteGapCategory: String,
    val lowestBaselineCoverageCategory: String,
    val highestGrowthFactorCategory: String,

    val topFiveExpansionEntryCount: Int,
    val topFiveExpansionShare: Double,

    val categoriesBelowTwentyFivePercentCoverage: Int,
    val categoriesBelowFiftyPercentCoverage: Int,

    val totalBaselineCountFromCategories: Int,
    val totalTargetCountFromCategories: Int,
    val totalExpansionCountFromCategories: Int,

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
        require(targetEntryCount > baselineEntryCount)

        require(
            requiredExpansionEntryCount ==
                    targetEntryCount - baselineEntryCount
        )

        require(analyzedCategoryCount > 0)

        require(
            analyzedCategoryCount ==
                    categories.size
        )

        require(
            categories.map { it.category }
                .distinct()
                .size ==
                    categories.size
        )

        require(
            categories.map { it.gapRank } ==
                    (1..categories.size).toList()
        ) {
            "Gap ranks must be continuous and start at one."
        }

        require(
            categories ==
                    categories.sortedWith(
                        compareByDescending<
                                CanonicalFoodCatalogCategoryGap
                                > {
                            it.expansionEntryCount
                        }.thenBy {
                            it.category
                        }
                    )
        ) {
            "Categories must be sorted by expansion gap descending."
        }

        require(
            categoryCountsByPriority.values.sum() ==
                    analyzedCategoryCount
        )

        require(
            expansionCountsByPriority.values.sum() ==
                    requiredExpansionEntryCount
        )

        require(
            totalBaselineCountFromCategories ==
                    baselineEntryCount
        )

        require(
            totalTargetCountFromCategories ==
                    targetEntryCount
        )

        require(
            totalExpansionCountFromCategories ==
                    requiredExpansionEntryCount
        )

        require(largestAbsoluteGapCategory.isNotBlank())
        require(lowestBaselineCoverageCategory.isNotBlank())
        require(highestGrowthFactorCategory.isNotBlank())

        require(topFiveExpansionEntryCount > 0)
        require(topFiveExpansionShare in 0.0..1.0)

        require(categoriesBelowTwentyFivePercentCoverage >= 0)
        require(categoriesBelowFiftyPercentCoverage >= 0)

        require(
            categoriesBelowTwentyFivePercentCoverage <=
                    categoriesBelowFiftyPercentCoverage
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
            valid == blockers.isEmpty()
        )
    }

    companion object {
        const val CURRENT_VERSION = 1

        private val SHA_256_REGEX =
            Regex("[0-9a-f]{64}")
    }
}