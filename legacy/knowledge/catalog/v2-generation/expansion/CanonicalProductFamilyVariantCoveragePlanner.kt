package de.shopme.testing.system.tools.knowledge.catalog.expansion.variant

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaseline
import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyCoverage

class CanonicalProductFamilyVariantCoveragePlanner {

    fun plan(
        baseline:
        CanonicalFoodCatalogBaseline,

        productFamilyCoverage:
        CanonicalProductFamilyCoverage
    ): CanonicalProductFamilyVariantCoverage {
        require(baseline.valid) {
            "Canonical catalog baseline must be valid."
        }

        require(productFamilyCoverage.valid) {
            "Canonical product-family coverage must be valid."
        }

        require(
            productFamilyCoverage.sourceBaselineId ==
                    baseline.baselineId
        ) {
            "Product-family coverage references another baseline."
        }

        require(
            productFamilyCoverage.sourceBaselineCatalogSha256 ==
                    baseline.catalogArtifact.sha256
        ) {
            "Product-family coverage references another catalog hash."
        }

        val blockers =
            mutableListOf<String>()

        val categoryCoverages =
            productFamilyCoverage.categories
                .map { categoryCoverage ->
                    val familyVariantCoverages =
                        categoryCoverage.families
                            .map { familyCoverage ->
                                val requirements =
                                    familyCoverage
                                        .allowedVariantAxes
                                        .map {
                                            CanonicalVariantAxisCoveragePolicy
                                                .requirementFor(it)
                                        }
                                        .sortedBy {
                                            it.axis.name
                                        }

                                if (requirements.isEmpty()) {
                                    blockers +=
                                        "Product family " +
                                                "'${familyCoverage.familyKey}' " +
                                                "has no variant requirements."
                                }

                                val complete =
                                    requirements.isNotEmpty() &&
                                            requirements.all {
                                                it.minimumRelevantValueCount > 0
                                            }

                                CanonicalProductFamilyVariantCoverageEntry(
                                    familyKey =
                                        familyCoverage.familyKey,

                                    category =
                                        familyCoverage.category,

                                    displayName =
                                        familyCoverage.displayName,

                                    allocatedTargetEntryCount =
                                        familyCoverage
                                            .allocatedTargetEntryCount,

                                    axisRequirementCount =
                                        requirements.size,

                                    requiredAxisCount =
                                        requirements.count {
                                            it.criticality ==
                                                    CanonicalVariantAxisCriticality
                                                        .REQUIRED
                                        },

                                    recommendedAxisCount =
                                        requirements.count {
                                            it.criticality ==
                                                    CanonicalVariantAxisCriticality
                                                        .RECOMMENDED
                                        },

                                    optionalAxisCount =
                                        requirements.count {
                                            it.criticality ==
                                                    CanonicalVariantAxisCriticality
                                                        .OPTIONAL
                                        },

                                    requirements =
                                        requirements,

                                    recommendedVariantValueCoverageCount =
                                        requirements.sumOf {
                                            it.recommendedRelevantValueCount
                                        },

                                    crossAxisCombinationAllowedCount =
                                        requirements.count {
                                            it.allowCrossAxisCombination
                                        },

                                    crossAxisCombinationRestrictedCount =
                                        requirements.count {
                                            !it.allowCrossAxisCombination
                                        },

                                    completeAxisCoverage =
                                        complete,

                                    valid =
                                        complete
                                )
                            }
                            .sortedBy {
                                it.familyKey
                            }

                    CanonicalCategoryVariantCoverage(
                        category =
                            categoryCoverage.category,

                        productFamilyCount =
                            familyVariantCoverages.size,

                        families =
                            familyVariantCoverages,

                        allocatedTargetEntryCount =
                            familyVariantCoverages.sumOf {
                                it.allocatedTargetEntryCount
                            },

                        totalAxisRequirementCount =
                            familyVariantCoverages.sumOf {
                                it.axisRequirementCount
                            },

                        totalRecommendedVariantValueCoverageCount =
                            familyVariantCoverages.sumOf {
                                it.recommendedVariantValueCoverageCount
                            },

                        completeFamilyCoverage =
                            familyVariantCoverages.all {
                                it.completeAxisCoverage
                            },

                        valid =
                            familyVariantCoverages.all {
                                it.valid
                            }
                    )
                }
                .sortedBy {
                    it.category
                }

        val allFamilies =
            categoryCoverages.flatMap {
                it.families
            }

        val sortedBlockers =
            blockers
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .sorted()

        val completeCount =
            allFamilies.count {
                it.completeAxisCoverage
            }

        val incompleteCount =
            allFamilies.size -
                    completeCount

        val ready =
            sortedBlockers.isEmpty() &&
                    incompleteCount == 0

        return CanonicalProductFamilyVariantCoverage(
            version =
                CanonicalProductFamilyVariantCoverage
                    .CURRENT_VERSION,

            sourceBaselineId =
                baseline.baselineId,

            sourceBaselineCatalogSha256 =
                baseline.catalogArtifact.sha256,

            baselineEntryCount =
                baseline.finalOutputEntryCount,

            sourceProductFamilyTargetEntryCount =
                productFamilyCoverage
                    .recommendedTargetEntryCount,

            currentRecommendedTargetEntryCount =
                productFamilyCoverage
                    .recommendedTargetEntryCount,

            minimumAllowedTargetEntryCount =
                productFamilyCoverage
                    .minimumRecommendedTargetEntryCount,

            maximumAllowedTargetEntryCount =
                productFamilyCoverage
                    .maximumAllowedTargetEntryCount,

            categoryCount =
                categoryCoverages.size,

            productFamilyCount =
                allFamilies.size,

            categories =
                categoryCoverages,

            totalAxisRequirementCount =
                categoryCoverages.sumOf {
                    it.totalAxisRequirementCount
                },

            totalRecommendedVariantValueCoverageCount =
                categoryCoverages.sumOf {
                    it.totalRecommendedVariantValueCoverageCount
                },

            requiredAxisCount =
                allFamilies.sumOf {
                    it.requiredAxisCount
                },

            recommendedAxisCount =
                allFamilies.sumOf {
                    it.recommendedAxisCount
                },

            optionalAxisCount =
                allFamilies.sumOf {
                    it.optionalAxisCount
                },

            familiesWithCompleteVariantCoverageCount =
                completeCount,

            familiesWithIncompleteVariantCoverageCount =
                incompleteCount,

            readyForTargetDerivation =
                ready,

            blockers =
                sortedBlockers,

            valid =
                ready
        )
    }
}