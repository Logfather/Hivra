package de.shopme.testing.system.tools.knowledge.catalog.expansion

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaseline

class CanonicalFoodCatalogTargetDistributionPlanner {

    fun plan(
        baseline:
        CanonicalFoodCatalogBaseline
    ): CanonicalFoodCatalogTargetDistribution {
        require(baseline.valid) {
            "Canonical food catalog baseline must be valid."
        }

        val targetCountsByCategory =
            CanonicalFoodCatalogTargetPolicy
                .TARGET_COUNTS_BY_CATEGORY
                .toSortedMap()

        val baselineCountsByCategory =
            baseline.categoryCounts
                .toSortedMap()

        val unallocatedBaselineCategories =
            baselineCountsByCategory
                .filterKeys {
                    it !in targetCountsByCategory
                }
                .toSortedMap()

        val blockers =
            buildList {
                unallocatedBaselineCategories
                    .forEach { (category, count) ->
                        add(
                            "Baseline category '$category' with $count " +
                                    "entries has no target allocation."
                        )
                    }

                targetCountsByCategory
                    .forEach { (category, targetCount) ->
                        val baselineCount =
                            baselineCountsByCategory[
                                category
                            ] ?: 0

                        if (baselineCount > targetCount) {
                            add(
                                "Baseline category '$category' already " +
                                        "contains $baselineCount entries and " +
                                        "exceeds its target of $targetCount."
                            )
                        }
                    }
            }
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .sorted()

        val requiredExpansionEntryCount =
            CanonicalFoodCatalogTargetDistribution
                .CANONICAL_TARGET_ENTRY_COUNT -
                    baseline.finalOutputEntryCount

        require(requiredExpansionEntryCount > 0) {
            "Baseline already reaches or exceeds the global target."
        }

        val categories =
            targetCountsByCategory
                .map { (category, targetCount) ->
                    val baselineCount =
                        baselineCountsByCategory[
                            category
                        ] ?: 0

                    val expansionCount =
                        maxOf(
                            targetCount - baselineCount,
                            0
                        )

                    CanonicalFoodCatalogCategoryTarget(
                        category =
                            category,

                        baselineEntryCount =
                            baselineCount,

                        targetEntryCount =
                            targetCount,

                        expansionEntryCount =
                            expansionCount,

                        targetShare =
                            targetCount.toDouble() /
                                    CanonicalFoodCatalogTargetDistribution
                                        .CANONICAL_TARGET_ENTRY_COUNT
                                        .toDouble(),

                        expansionShare =
                            if (
                                requiredExpansionEntryCount == 0
                            ) {
                                0.0
                            } else {
                                expansionCount.toDouble() /
                                        requiredExpansionEntryCount
                                            .toDouble()
                            },

                        status =
                            when {
                                baselineCount < targetCount ->
                                    CanonicalFoodCatalogCategoryTargetStatus
                                        .EXPANSION_REQUIRED

                                baselineCount == targetCount ->
                                    CanonicalFoodCatalogCategoryTargetStatus
                                        .TARGET_REACHED

                                else ->
                                    CanonicalFoodCatalogCategoryTargetStatus
                                        .BASELINE_EXCEEDS_TARGET
                            }
                    )
                }
                .sortedBy {
                    it.category
                }

        val expansionCountsByCategory =
            categories
                .associate {
                    it.category to
                            it.expansionEntryCount
                }
                .toSortedMap()

        /*
         * Diese Invariante ist wichtig:
         *
         * Wenn keine Kategorie über ihrem Ziel liegt und keine Baseline-
         * Kategorie unverteilt bleibt, müssen alle Kategorie-Gaps gemeinsam
         * exakt den globalen Ausbau auf 10.000 ergeben.
         */
        if (
            blockers.isEmpty() &&
            unallocatedBaselineCategories.isEmpty()
        ) {
            require(
                expansionCountsByCategory.values.sum() ==
                        requiredExpansionEntryCount
            ) {
                "Category expansion gaps do not sum to the required " +
                        "global expansion."
            }
        }

        return CanonicalFoodCatalogTargetDistribution(
            version =
                CanonicalFoodCatalogTargetDistribution
                    .CURRENT_VERSION,

            sourceBaselineId =
                baseline.baselineId,

            sourceBaselineCatalogSha256 =
                baseline.catalogArtifact.sha256,

            baselineEntryCount =
                baseline.finalOutputEntryCount,

            targetEntryCount =
                CanonicalFoodCatalogTargetDistribution
                    .CANONICAL_TARGET_ENTRY_COUNT,

            requiredExpansionEntryCount =
                requiredExpansionEntryCount,

            baselineCategoryCount =
                baseline.canonicalCategoryCount,

            targetCategoryCount =
                categories.size,

            categories =
                categories,

            targetCountsByCategory =
                targetCountsByCategory,

            baselineCountsByCategory =
                baselineCountsByCategory,

            expansionCountsByCategory =
                expansionCountsByCategory,

            categoriesRequiringExpansionCount =
                categories.count {
                    it.status ==
                            CanonicalFoodCatalogCategoryTargetStatus
                                .EXPANSION_REQUIRED
                },

            categoriesAtTargetCount =
                categories.count {
                    it.status ==
                            CanonicalFoodCatalogCategoryTargetStatus
                                .TARGET_REACHED
                },

            categoriesExceedingTargetCount =
                categories.count {
                    it.status ==
                            CanonicalFoodCatalogCategoryTargetStatus
                                .BASELINE_EXCEEDS_TARGET
                },

            unallocatedBaselineCategories =
                unallocatedBaselineCategories,

            blockers =
                blockers,

            valid =
                blockers.isEmpty() &&
                        unallocatedBaselineCategories.isEmpty()
        )
    }
}