package de.shopme.testing.system.tools.knowledge.catalog.expansion.refinement

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaseline
import de.shopme.testing.system.tools.knowledge.catalog.category.CanonicalFoodCategoryRegistry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.CanonicalFoodCatalogTargetDistribution

class CanonicalFoodCatalogTargetDistributionRefiner {

    fun refine(
        baseline:
        CanonicalFoodCatalogBaseline,

        targetDistribution:
        CanonicalFoodCatalogTargetDistribution,

        categoryRegistry:
        CanonicalFoodCategoryRegistry
    ): CanonicalFoodCatalogRefinedTargetDistribution {
        require(baseline.valid) {
            "Canonical food catalog baseline must be valid."
        }

        require(targetDistribution.valid) {
            "Source target distribution must be valid."
        }

        val issues =
            mutableListOf<CanonicalTargetDistributionIssue>()

        if (
            targetDistribution.sourceBaselineId !=
            baseline.baselineId ||
            targetDistribution.sourceBaselineCatalogSha256 !=
            baseline.catalogArtifact.sha256
        ) {
            issues +=
                issue(
                    type =
                        CanonicalTargetDistributionIssueType
                            .INVALID_BASELINE_REFERENCE,
                    message =
                        "Target distribution does not reference the " +
                                "supplied frozen baseline."
                )
        }

        if (targetDistribution.targetEntryCount != 10_000) {
            issues +=
                issue(
                    type =
                        CanonicalTargetDistributionIssueType
                            .GLOBAL_TARGET_MISMATCH,
                    message =
                        "Canonical target must contain exactly 10,000 entries."
                )
        }

        val targetCategories =
            targetDistribution
                .targetCountsByCategory
                .keys

        val baselineCategories =
            baseline.categoryCounts.keys

        val policyCategories =
            CanonicalCatalogCategoryScopePolicy
                .supportedCategories()

        val registryCategories: Set<String> =
            categoryRegistry.keys()

        (targetCategories - registryCategories)
            .sorted()
            .forEach { category ->
                issues +=
                    issue(
                        type =
                            CanonicalTargetDistributionIssueType
                                .UNKNOWN_CATEGORY,
                        category =
                            category,
                        message =
                            "Target category '$category' is absent from " +
                                    "the canonical category registry."
                    )
            }

        (baselineCategories - targetCategories)
            .sorted()
            .forEach { category ->
                issues +=
                    issue(
                        type =
                            CanonicalTargetDistributionIssueType
                                .MISSING_CATEGORY,
                        category =
                            category,
                        message =
                            "Baseline category '$category' has no target."
                    )
            }

        (targetCategories - policyCategories)
            .sorted()
            .forEach { category ->
                issues +=
                    issue(
                        type =
                            CanonicalTargetDistributionIssueType
                                .EMPTY_IDENTITY_SCOPE,
                        category =
                            category,
                        message =
                            "Category '$category' has no canonical identity scope."
                    )
            }

        val scopes =
            targetDistribution.categories
                .map { target ->
                    val baselineCount =
                        baseline.categoryCounts[
                            target.category
                        ] ?: 0

                    if (
                        target.targetEntryCount <
                        baselineCount
                    ) {
                        issues +=
                            issue(
                                type =
                                    CanonicalTargetDistributionIssueType
                                        .TARGET_BELOW_BASELINE,
                                category =
                                    target.category,
                                message =
                                    "Target for '${target.category}' is " +
                                            "${target.targetEntryCount}, below " +
                                            "baseline count $baselineCount."
                            )
                    }

                    val targetShare =
                        target.targetEntryCount.toDouble() /
                                targetDistribution
                                    .targetEntryCount
                                    .toDouble()

                    if (targetShare > MAX_SINGLE_CATEGORY_SHARE) {
                        issues +=
                            CanonicalTargetDistributionIssue(
                                type =
                                    CanonicalTargetDistributionIssueType
                                        .EXCESSIVE_SINGLE_CATEGORY_SHARE,
                                severity =
                                    CanonicalTargetDistributionIssueSeverity
                                        .WARNING,
                                category =
                                    target.category,
                                message =
                                    "Category '${target.category}' uses " +
                                            "$targetShare of the global target."
                            )
                    }

                    CanonicalCatalogCategoryScope(
                        category =
                            target.category,

                        baselineEntryCount =
                            baselineCount,

                        proposedTargetEntryCount =
                            target.targetEntryCount,

                        /*
                         * Die aktuelle Verteilung bleibt bestehen.
                         * Die Verfeinerung friert ihre kanonische Semantik
                         * und nicht eine SKU-basierte Interpretation ein.
                         */
                        refinedTargetEntryCount =
                            target.targetEntryCount,

                        allowedIdentityAxes =
                            CanonicalCatalogCategoryScopePolicy
                                .identityAxesFor(
                                    target.category
                                ),

                        excludedSkuAxes =
                            CanonicalCatalogCategoryScopePolicy
                                .excludedSkuAxesFor(
                                    target.category
                                ),

                        scopeRationale =
                            CanonicalCatalogCategoryScopePolicy
                                .rationaleFor(
                                    target.category
                                ),

                        targetChanged = false
                    )
                }
                .sortedBy {
                    it.category
                }

        val refinedTargets =
            scopes.associate {
                it.category to
                        it.refinedTargetEntryCount
            }.toSortedMap()

        if (refinedTargets.values.sum() != 10_000) {
            issues +=
                issue(
                    type =
                        CanonicalTargetDistributionIssueType
                            .CATEGORY_TARGET_SUM_MISMATCH,
                    message =
                        "Refined category targets do not sum to 10,000."
                )
        }

        val sortedIssues =
            issues
                .distinct()
                .sortedWith(
                    compareBy<
                            CanonicalTargetDistributionIssue
                            > {
                        it.severity.name
                    }.thenBy {
                        it.type.name
                    }.thenBy {
                        it.category.orEmpty()
                    }.thenBy {
                        it.message
                    }
                )

        val blockers =
            sortedIssues
                .filter {
                    it.severity ==
                            CanonicalTargetDistributionIssueSeverity
                                .ERROR
                }
                .map {
                    it.message
                }
                .distinct()
                .sorted()

        return CanonicalFoodCatalogRefinedTargetDistribution(
            version =
                CanonicalFoodCatalogRefinedTargetDistribution
                    .CURRENT_VERSION,

            sourceBaselineId =
                baseline.baselineId,

            sourceBaselineCatalogSha256 =
                baseline.catalogArtifact.sha256,

            sourceTargetEntryCount =
                targetDistribution.targetEntryCount,

            refinedTargetEntryCount =
                refinedTargets.values.sum(),

            baselineEntryCount =
                baseline.finalOutputEntryCount,

            requiredExpansionEntryCount =
                refinedTargets.values.sum() -
                        baseline.finalOutputEntryCount,

            categoryCount =
                scopes.size,

            changedCategoryCount =
                scopes.count {
                    it.targetChanged
                },

            methodology =
                CanonicalFoodCatalogTargetMethodology(
                    catalogConcept =
                        CanonicalFoodCatalogTargetMethodology
                            .CANONICAL_CONCEPT,

                    canonicalUnitDefinition =
                        "One entry represents one canonical food type or " +
                                "a materially distinct food variant.",

                    includedVariationRule =
                        "A variant may be separate when ingredients, food " +
                                "identity, processing, preservation, physical " +
                                "form, allergens or nutrition change materially.",

                    excludedVariationRule =
                        "Brand, manufacturer, retailer, EAN, package size, " +
                                "price, promotion and package design never create " +
                                "a separate canonical food by themselves.",

                    targetInterpretation =
                        "The 10,000 target measures canonical food coverage, " +
                                "not the number of retail SKUs.",

                    marketCoverageClaim =
                        "No percentage claim for all German retail SKUs is " +
                                "made. Completion means 100 percent coverage of " +
                                "the explicitly defined canonical target universe."
                ),

            categories =
                scopes,

            refinedTargetCountsByCategory =
                refinedTargets,

            validationIssueCount =
                sortedIssues.size,

            validationIssueCountsByType =
                sortedIssues
                    .groupingBy {
                        it.type
                    }
                    .eachCount()
                    .toList()
                    .sortedBy {
                        it.first.name
                    }
                    .associate {
                        it
                    },

            issues =
                sortedIssues,

            blockers =
                blockers,

            valid =
                blockers.isEmpty()
        )
    }

    private fun issue(
        type: CanonicalTargetDistributionIssueType,
        category: String? = null,
        message: String
    ): CanonicalTargetDistributionIssue =
        CanonicalTargetDistributionIssue(
            type = type,
            severity =
                CanonicalTargetDistributionIssueSeverity.ERROR,
            category = category,
            message = message
        )

    private companion object {
        const val MAX_SINGLE_CATEGORY_SHARE = 0.15
    }
}