package de.shopme.testing.system.tools.knowledge.catalog.expansion.family

data class CanonicalProductFamilyCoverage(
    val version: Int,

    val sourceBaselineId: String,
    val sourceBaselineCatalogSha256: String,

    val baselineEntryCount: Int,

    /**
     * Frühere Planungsgröße.
     */
    val originalPlanningTargetEntryCount: Int,

    /**
     * Aus der derzeitigen Produktfamilienabdeckung abgeleiteter Zielwert.
     *
     * Der Wert muss nicht exakt 10.000 betragen.
     */
    val recommendedTargetEntryCount: Int,

    val minimumRecommendedTargetEntryCount: Int,
    val maximumAllowedTargetEntryCount: Int,

    val requiredExpansionEntryCount: Int,

    val categoryCount: Int,
    val productFamilyCount: Int,

    val categories:
    List<CanonicalProductFamilyCategoryCoverage>,

    val recommendedTargetCountsByCategory:
    Map<String, Int>,

    val familyCountsByCategory:
    Map<String, Int>,

    val completeCategoryAllocation: Boolean,
    val completeFamilyAllocation: Boolean,

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

        require(originalPlanningTargetEntryCount == 10_000)

        require(
            minimumRecommendedTargetEntryCount ==
                    MINIMUM_RECOMMENDED_TARGET_ENTRY_COUNT
        )

        require(
            maximumAllowedTargetEntryCount ==
                    MAXIMUM_ALLOWED_TARGET_ENTRY_COUNT
        )

        require(
            recommendedTargetEntryCount in
                    minimumRecommendedTargetEntryCount..
                    maximumAllowedTargetEntryCount
        ) {
            "Recommended canonical target must be between " +
                    "$minimumRecommendedTargetEntryCount and " +
                    "$maximumAllowedTargetEntryCount entries, but was " +
                    "$recommendedTargetEntryCount."
        }

        require(
            requiredExpansionEntryCount ==
                    recommendedTargetEntryCount -
                    baselineEntryCount
        )

        require(requiredExpansionEntryCount > 0)

        require(categoryCount == categories.size)

        require(
            productFamilyCount ==
                    categories.sumOf {
                        it.familyCount
                    }
        )

        require(
            categories ==
                    categories.sortedBy {
                        it.category
                    }
        )

        require(
            categories.map { it.category }
                .distinct()
                .size ==
                    categories.size
        )

        require(
            recommendedTargetCountsByCategory ==
                    recommendedTargetCountsByCategory
                        .toSortedMap()
        )

        require(
            recommendedTargetCountsByCategory.values.sum() ==
                    recommendedTargetEntryCount
        )

        require(
            familyCountsByCategory ==
                    familyCountsByCategory.toSortedMap()
        )

        require(
            familyCountsByCategory.values.sum() ==
                    productFamilyCount
        )

        require(
            completeCategoryAllocation ==
                    (
                            recommendedTargetCountsByCategory.size ==
                                    categoryCount
                            )
        )

        require(
            completeFamilyAllocation ==
                    categories.all {
                        it.allocatedFamilyTargetEntryCount ==
                                it.recommendedTargetEntryCount
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
                                    completeCategoryAllocation &&
                                    completeFamilyAllocation
                            )
        )
    }

    companion object {
        const val CURRENT_VERSION = 1

        const val MINIMUM_RECOMMENDED_TARGET_ENTRY_COUNT =
            10_000

        const val MAXIMUM_ALLOWED_TARGET_ENTRY_COUNT =
            15_000

        private val SHA_256_REGEX =
            Regex("[0-9a-f]{64}")
    }
}