package de.shopme.testing.system.tools.knowledge.catalog.expansion

data class CanonicalFoodCatalogTargetDistribution(
    val version: Int,

    val sourceBaselineId: String,
    val sourceBaselineCatalogSha256: String,

    val baselineEntryCount: Int,
    val targetEntryCount: Int,
    val requiredExpansionEntryCount: Int,

    val baselineCategoryCount: Int,
    val targetCategoryCount: Int,

    val categories:
    List<CanonicalFoodCatalogCategoryTarget>,

    val targetCountsByCategory:
    Map<String, Int>,

    val baselineCountsByCategory:
    Map<String, Int>,

    val expansionCountsByCategory:
    Map<String, Int>,

    val categoriesRequiringExpansionCount: Int,
    val categoriesAtTargetCount: Int,
    val categoriesExceedingTargetCount: Int,

    val unallocatedBaselineCategories:
    Map<String, Int>,

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
        ) {
            "sourceBaselineCatalogSha256 must be a lowercase SHA-256."
        }

        require(baselineEntryCount > 0)
        require(targetEntryCount > baselineEntryCount)

        require(
            requiredExpansionEntryCount ==
                    targetEntryCount - baselineEntryCount
        )

        require(baselineCategoryCount > 0)
        require(targetCategoryCount > 0)

        require(
            targetCategoryCount ==
                    categories.size
        )

        require(
            categories ==
                    categories.sortedBy {
                        it.category
                    }
        ) {
            "Categories must be sorted by canonical category key."
        }

        require(
            categories.map { it.category }
                .distinct()
                .size ==
                    categories.size
        )

        require(
            targetCountsByCategory ==
                    targetCountsByCategory.toSortedMap()
        )

        require(
            baselineCountsByCategory ==
                    baselineCountsByCategory.toSortedMap()
        )

        require(
            expansionCountsByCategory ==
                    expansionCountsByCategory.toSortedMap()
        )

        require(
            targetCountsByCategory.values.sum() ==
                    targetEntryCount
        ) {
            "Category targets must sum to the global target."
        }

        require(
            baselineCountsByCategory.values.sum() ==
                    baselineEntryCount
        ) {
            "Baseline category counts must cover the baseline."
        }

        if (
            unallocatedBaselineCategories.isEmpty() &&
            categoriesExceedingTargetCount == 0
        ) {
            require(
                expansionCountsByCategory.values.sum() ==
                        requiredExpansionEntryCount
            ) {
                "Expansion category counts must cover the required expansion. " +
                        "expansionCountSum=" +
                        expansionCountsByCategory.values.sum() +
                        ", requiredExpansionEntryCount=" +
                        requiredExpansionEntryCount +
                        "."
            }
        }

        require(
            categoriesRequiringExpansionCount ==
                    categories.count {
                        it.status ==
                                CanonicalFoodCatalogCategoryTargetStatus
                                    .EXPANSION_REQUIRED
                    }
        )

        require(
            categoriesAtTargetCount ==
                    categories.count {
                        it.status ==
                                CanonicalFoodCatalogCategoryTargetStatus
                                    .TARGET_REACHED
                    }
        )

        require(
            categoriesExceedingTargetCount ==
                    categories.count {
                        it.status ==
                                CanonicalFoodCatalogCategoryTargetStatus
                                    .BASELINE_EXCEEDS_TARGET
                    }
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
            valid ==
                    (
                            blockers.isEmpty() &&
                                    unallocatedBaselineCategories.isEmpty() &&
                                    categoriesExceedingTargetCount == 0
                            )
        )
    }

    companion object {
        const val CURRENT_VERSION = 1
        const val CANONICAL_TARGET_ENTRY_COUNT = 10_000

        private val SHA_256_REGEX =
            Regex("[0-9a-f]{64}")
    }
}