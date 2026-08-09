package de.shopme.testing.system.tools.knowledge.catalog.expansion.family

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaseline
import de.shopme.testing.system.tools.knowledge.catalog.expansion.refinement.CanonicalFoodCatalogRefinedTargetDistribution

class CanonicalProductFamilyCoveragePlanner {

    fun plan(
        baseline:
        CanonicalFoodCatalogBaseline,

        refinedTargetDistribution:
        CanonicalFoodCatalogRefinedTargetDistribution
    ): CanonicalProductFamilyCoverage {
        require(baseline.valid) {
            "Canonical catalog baseline must be valid."
        }

        require(refinedTargetDistribution.valid) {
            "Refined target distribution must be valid."
        }

        require(
            refinedTargetDistribution.sourceBaselineId ==
                    baseline.baselineId
        ) {
            "Refined target distribution references another baseline."
        }

        require(
            refinedTargetDistribution
                .sourceBaselineCatalogSha256 ==
                    baseline.catalogArtifact.sha256
        ) {
            "Refined target distribution references another catalog hash."
        }

        val blockers =
            mutableListOf<String>()

        val targetCategories =
            refinedTargetDistribution
                .refinedTargetCountsByCategory
                .keys

        val policyCategories =
            CanonicalProductFamilyPolicy.categories()

        (targetCategories - policyCategories)
            .sorted()
            .forEach { category ->
                blockers +=
                    "Target category '$category' has no product-family policy."
            }

        (policyCategories - targetCategories)
            .sorted()
            .forEach { category ->
                blockers +=
                    "Product-family category '$category' has no target."
            }

        val recommendedTargetEntryCount =
            refinedTargetDistribution
                .refinedTargetEntryCount

        if (
            recommendedTargetEntryCount <
            CanonicalProductFamilyCoverage
                .MINIMUM_RECOMMENDED_TARGET_ENTRY_COUNT
        ) {
            blockers +=
                "Recommended target $recommendedTargetEntryCount is below " +
                        "the canonical minimum of " +
                        CanonicalProductFamilyCoverage
                            .MINIMUM_RECOMMENDED_TARGET_ENTRY_COUNT +
                        "."
        }

        if (
            recommendedTargetEntryCount >
            CanonicalProductFamilyCoverage
                .MAXIMUM_ALLOWED_TARGET_ENTRY_COUNT
        ) {
            blockers +=
                "Recommended target $recommendedTargetEntryCount exceeds " +
                        "the canonical upper limit of " +
                        CanonicalProductFamilyCoverage
                            .MAXIMUM_ALLOWED_TARGET_ENTRY_COUNT +
                        "."
        }

        val categoryCoverages =
            refinedTargetDistribution
                .refinedTargetCountsByCategory
                .map { (category, categoryTarget) ->
                    val families =
                        CanonicalProductFamilyPolicy
                            .familiesFor(category)

                    require(families.isNotEmpty()) {
                        "No product families exist for '$category'."
                    }

                    val allocations =
                        allocateTargets(
                            categoryTargetEntryCount =
                                categoryTarget,
                            families =
                                families
                        )

                    val entries =
                        families.map { family ->
                            val allocation =
                                requireNotNull(
                                    allocations[
                                        family.key
                                    ]
                                )

                            CanonicalProductFamilyCoverageEntry(
                                familyKey =
                                    family.key,

                                category =
                                    family.category,

                                displayName =
                                    family.displayName,

                                allocationWeight =
                                    family.allocationWeight,

                                baselineCategoryEntryCount =
                                    baseline.categoryCounts[
                                        category
                                    ] ?: 0,

                                categoryTargetEntryCount =
                                    categoryTarget,

                                allocatedTargetEntryCount =
                                    allocation,

                                allocatedTargetShareOfCategory =
                                    allocation.toDouble() /
                                            categoryTarget.toDouble(),

                                allocatedTargetShareOfCatalog =
                                    allocation.toDouble() /
                                            recommendedTargetEntryCount
                                                .toDouble(),

                                allowedVariantAxes =
                                    family.allowedVariantAxes,

                                rationale =
                                    family.rationale
                            )
                        }
                            .sortedBy {
                                it.familyKey
                            }

                    CanonicalProductFamilyCategoryCoverage(
                        category =
                            category,

                        baselineEntryCount =
                            baseline.categoryCounts[
                                category
                            ] ?: 0,

                        recommendedTargetEntryCount =
                            categoryTarget,

                        familyCount =
                            entries.size,

                        families =
                            entries,

                        allocatedFamilyTargetEntryCount =
                            entries.sumOf {
                                it.allocatedTargetEntryCount
                            },

                        valid = true
                    )
                }
                .sortedBy {
                    it.category
                }

        val sortedBlockers =
            blockers
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .sorted()

        return CanonicalProductFamilyCoverage(
            version =
                CanonicalProductFamilyCoverage
                    .CURRENT_VERSION,

            sourceBaselineId =
                baseline.baselineId,

            sourceBaselineCatalogSha256 =
                baseline.catalogArtifact.sha256,

            baselineEntryCount =
                baseline.finalOutputEntryCount,

            originalPlanningTargetEntryCount =
                10_000,

            recommendedTargetEntryCount =
                recommendedTargetEntryCount,

            minimumRecommendedTargetEntryCount =
                CanonicalProductFamilyCoverage
                    .MINIMUM_RECOMMENDED_TARGET_ENTRY_COUNT,

            maximumAllowedTargetEntryCount =
                CanonicalProductFamilyCoverage
                    .MAXIMUM_ALLOWED_TARGET_ENTRY_COUNT,

            requiredExpansionEntryCount =
                recommendedTargetEntryCount -
                        baseline.finalOutputEntryCount,

            categoryCount =
                categoryCoverages.size,

            productFamilyCount =
                categoryCoverages.sumOf {
                    it.familyCount
                },

            categories =
                categoryCoverages,

            recommendedTargetCountsByCategory =
                categoryCoverages.associate {
                    it.category to
                            it.recommendedTargetEntryCount
                }.toSortedMap(),

            familyCountsByCategory =
                categoryCoverages.associate {
                    it.category to
                            it.familyCount
                }.toSortedMap(),

            completeCategoryAllocation =
                categoryCoverages.size ==
                        refinedTargetDistribution
                            .categoryCount,

            completeFamilyAllocation =
                categoryCoverages.all {
                    it.allocatedFamilyTargetEntryCount ==
                            it.recommendedTargetEntryCount
                },

            blockers =
                sortedBlockers,

            valid =
                sortedBlockers.isEmpty()
        )
    }

    private fun allocateTargets(
        categoryTargetEntryCount: Int,
        families: List<CanonicalProductFamily>
    ): Map<String, Int> {
        require(categoryTargetEntryCount >= families.size) {
            "Category target $categoryTargetEntryCount is too small for " +
                    "${families.size} product families."
        }

        val totalWeight =
            families.sumOf {
                it.allocationWeight
            }

        /*
         * Jede Produktfamilie erhält zunächst mindestens einen Eintrag.
         */
        val distributableEntryCount =
            categoryTargetEntryCount -
                    families.size

        val rawAllocations =
            families.map { family ->
                val exactAdditionalAllocation =
                    distributableEntryCount.toDouble() *
                            family.allocationWeight.toDouble() /
                            totalWeight.toDouble()

                RawAllocation(
                    familyKey =
                        family.key,

                    baseAllocation =
                        1 +
                                exactAdditionalAllocation
                                    .toInt(),

                    fractionalRemainder =
                        exactAdditionalAllocation -
                                exactAdditionalAllocation
                                    .toInt()
                )
            }

        val allocatedBaseCount =
            rawAllocations.sumOf {
                it.baseAllocation
            }

        val remainingCount =
            categoryTargetEntryCount -
                    allocatedBaseCount

        val remainderRecipients =
            rawAllocations
                .sortedWith(
                    compareByDescending<RawAllocation> {
                        it.fractionalRemainder
                    }.thenBy {
                        it.familyKey
                    }
                )
                .take(remainingCount)
                .map {
                    it.familyKey
                }
                .toSet()

        val result =
            rawAllocations
                .associate { allocation ->
                    allocation.familyKey to
                            (
                                    allocation.baseAllocation +
                                            if (
                                                allocation.familyKey in
                                                remainderRecipients
                                            ) {
                                                1
                                            } else {
                                                0
                                            }
                                    )
                }
                .toSortedMap()

        require(
            result.values.sum() ==
                    categoryTargetEntryCount
        ) {
            "Allocated family targets do not cover the complete category."
        }

        require(
            result.values.all {
                it > 0
            }
        )

        return result
    }

    private data class RawAllocation(
        val familyKey: String,
        val baseAllocation: Int,
        val fractionalRemainder: Double
    )
}