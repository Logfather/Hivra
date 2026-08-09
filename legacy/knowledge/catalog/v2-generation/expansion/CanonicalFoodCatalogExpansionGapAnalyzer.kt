package de.shopme.testing.system.tools.knowledge.catalog.expansion.analysis

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaseline
import de.shopme.testing.system.tools.knowledge.catalog.expansion.CanonicalFoodCatalogTargetDistribution

class CanonicalFoodCatalogExpansionGapAnalyzer {

    fun analyze(
        baseline:
        CanonicalFoodCatalogBaseline,

        targetDistribution:
        CanonicalFoodCatalogTargetDistribution
    ): CanonicalFoodCatalogExpansionGapAnalysis {
        require(baseline.valid) {
            "Canonical food catalog baseline must be valid."
        }

        require(targetDistribution.valid) {
            "Canonical food catalog target distribution must be valid."
        }

        require(
            targetDistribution.sourceBaselineId ==
                    baseline.baselineId
        ) {
            "Target distribution references a different baseline ID."
        }

        require(
            targetDistribution.sourceBaselineCatalogSha256 ==
                    baseline.catalogArtifact.sha256
        ) {
            "Target distribution references a different baseline catalog."
        }

        val blockers =
            buildList {
                val baselineCategories =
                    baseline.categoryCounts.keys

                val targetCategories =
                    targetDistribution
                        .targetCountsByCategory
                        .keys

                val missingTargetCategories =
                    baselineCategories -
                            targetCategories

                missingTargetCategories
                    .sorted()
                    .forEach { category ->
                        add(
                            "Baseline category '$category' has no target."
                        )
                    }

                val unknownTargetCategories =
                    targetCategories -
                            baselineCategories

                unknownTargetCategories
                    .sorted()
                    .forEach { category ->
                        add(
                            "Target category '$category' is absent from " +
                                    "the frozen baseline."
                        )
                    }

                targetDistribution.categories
                    .filter {
                        it.expansionEntryCount < 0
                    }
                    .forEach {
                        add(
                            "Category '${it.category}' has a negative gap."
                        )
                    }
            }
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .sorted()

        val requiredExpansionEntryCount =
            targetDistribution
                .requiredExpansionEntryCount

        require(requiredExpansionEntryCount > 0)

        val unsortedGaps =
            targetDistribution.categories
                .map { categoryTarget ->
                    val baselineCount =
                        categoryTarget.baselineEntryCount

                    val targetCount =
                        categoryTarget.targetEntryCount

                    val expansionCount =
                        categoryTarget.expansionEntryCount

                    val expansionShare =
                        expansionCount.toDouble() /
                                requiredExpansionEntryCount
                                    .toDouble()

                    CanonicalFoodCatalogCategoryGap(
                        category =
                            categoryTarget.category,

                        baselineEntryCount =
                            baselineCount,

                        targetEntryCount =
                            targetCount,

                        expansionEntryCount =
                            expansionCount,

                        baselineCoverage =
                            baselineCount.toDouble() /
                                    targetCount.toDouble(),

                        requiredGrowthFactor =
                            if (baselineCount == 0) {
                                null
                            } else {
                                targetCount.toDouble() /
                                        baselineCount.toDouble()
                            },

                        shareOfRequiredExpansion =
                            expansionShare,

                        categoryGapShare =
                            expansionCount.toDouble() /
                                    targetCount.toDouble(),

                        gapRank = 1,

                        priority =
                            priorityFor(
                                shareOfRequiredExpansion =
                                    expansionShare
                            )
                    )
                }

        val categories =
            unsortedGaps
                .sortedWith(
                    compareByDescending<
                            CanonicalFoodCatalogCategoryGap
                            > {
                        it.expansionEntryCount
                    }.thenBy {
                        it.category
                    }
                )
                .mapIndexed { index, gap ->
                    gap.copy(
                        gapRank = index + 1
                    )
                }

        val categoryCountsByPriority =
            categories
                .groupingBy {
                    it.priority
                }
                .eachCount()
                .toList()
                .sortedBy {
                    it.first.name
                }
                .associate {
                    it
                }

        val expansionCountsByPriority =
            categories
                .groupBy {
                    it.priority
                }
                .mapValues { (_, gaps) ->
                    gaps.sumOf {
                        it.expansionEntryCount
                    }
                }
                .toList()
                .sortedBy {
                    it.first.name
                }
                .associate {
                    it
                }

        val largestAbsoluteGap =
            categories.first()

        val lowestBaselineCoverage =
            categories.minWithOrNull(
                compareBy<
                        CanonicalFoodCatalogCategoryGap
                        > {
                    it.baselineCoverage
                }.thenBy {
                    it.category
                }
            ) ?: error(
                "No categories available for gap analysis."
            )

        val highestGrowthFactor =
            categories.maxWithOrNull(
                compareBy<
                        CanonicalFoodCatalogCategoryGap
                        > {
                    it.requiredGrowthFactor
                        ?: Double.POSITIVE_INFINITY
                }.thenByDescending {
                    it.category
                }
            ) ?: error(
                "No categories available for growth analysis."
            )

        val topFive =
            categories.take(5)

        val topFiveExpansionEntryCount =
            topFive.sumOf {
                it.expansionEntryCount
            }

        return CanonicalFoodCatalogExpansionGapAnalysis(
            version =
                CanonicalFoodCatalogExpansionGapAnalysis
                    .CURRENT_VERSION,

            sourceBaselineId =
                baseline.baselineId,

            sourceBaselineCatalogSha256 =
                baseline.catalogArtifact.sha256,

            baselineEntryCount =
                baseline.finalOutputEntryCount,

            targetEntryCount =
                targetDistribution.targetEntryCount,

            requiredExpansionEntryCount =
                requiredExpansionEntryCount,

            analyzedCategoryCount =
                categories.size,

            categories =
                categories,

            categoryCountsByPriority =
                categoryCountsByPriority,

            expansionCountsByPriority =
                expansionCountsByPriority,

            largestAbsoluteGapCategory =
                largestAbsoluteGap.category,

            lowestBaselineCoverageCategory =
                lowestBaselineCoverage.category,

            highestGrowthFactorCategory =
                highestGrowthFactor.category,

            topFiveExpansionEntryCount =
                topFiveExpansionEntryCount,

            topFiveExpansionShare =
                topFiveExpansionEntryCount.toDouble() /
                        requiredExpansionEntryCount.toDouble(),

            categoriesBelowTwentyFivePercentCoverage =
                categories.count {
                    it.baselineCoverage < 0.25
                },

            categoriesBelowFiftyPercentCoverage =
                categories.count {
                    it.baselineCoverage < 0.50
                },

            totalBaselineCountFromCategories =
                categories.sumOf {
                    it.baselineEntryCount
                },

            totalTargetCountFromCategories =
                categories.sumOf {
                    it.targetEntryCount
                },

            totalExpansionCountFromCategories =
                categories.sumOf {
                    it.expansionEntryCount
                },

            blockers =
                blockers,

            valid =
                blockers.isEmpty()
        )
    }

    private fun priorityFor(
        shareOfRequiredExpansion: Double
    ): CanonicalFoodCatalogExpansionGapPriority =
        when {
            shareOfRequiredExpansion >= 0.08 ->
                CanonicalFoodCatalogExpansionGapPriority
                    .CRITICAL

            shareOfRequiredExpansion >= 0.05 ->
                CanonicalFoodCatalogExpansionGapPriority
                    .HIGH

            shareOfRequiredExpansion >= 0.025 ->
                CanonicalFoodCatalogExpansionGapPriority
                    .MEDIUM

            else ->
                CanonicalFoodCatalogExpansionGapPriority
                    .LOW
        }
}