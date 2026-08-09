package de.shopme.testing.system.tools.knowledge.catalog.expansion.target

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaseline
import de.shopme.testing.system.tools.knowledge.catalog.expansion.variant.CanonicalProductFamilyVariantCoverage
import de.shopme.testing.system.tools.knowledge.catalog.expansion.variant.CanonicalProductFamilyVariantCoverageEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.variant.CanonicalVariantAxisCoverageRequirement
import de.shopme.testing.system.tools.knowledge.catalog.expansion.variant.CanonicalVariantAxisCriticality

class CanonicalCatalogTargetDerivationPlanner {

    fun derive(
        baseline:
        CanonicalFoodCatalogBaseline,

        variantCoverage:
        CanonicalProductFamilyVariantCoverage
    ): CanonicalCatalogTargetDerivation {
        require(baseline.valid) {
            "Canonical catalog baseline must be valid."
        }

        require(variantCoverage.valid) {
            "Canonical product-family variant coverage must be valid."
        }

        require(variantCoverage.readyForTargetDerivation) {
            "Variant coverage is not ready for target derivation."
        }

        require(
            variantCoverage.sourceBaselineId ==
                    baseline.baselineId
        ) {
            "Variant coverage references another baseline."
        }

        require(
            variantCoverage.sourceBaselineCatalogSha256 ==
                    baseline.catalogArtifact.sha256
        ) {
            "Variant coverage references another catalog hash."
        }

        val allFamilies =
            variantCoverage.categories
                .flatMap {
                    it.families
                }
                .sortedWith(
                    compareBy<
                            CanonicalProductFamilyVariantCoverageEntry
                            > {
                        it.category
                    }.thenBy {
                        it.familyKey
                    }
                )

        require(allFamilies.isNotEmpty())

        val crossAxisCombinationAllowedCount =
            allFamilies.sumOf {
                it.crossAxisCombinationAllowedCount
            }

        val crossAxisCombinationRestrictedCount =
            allFamilies.sumOf {
                it.crossAxisCombinationRestrictedCount
            }

        val upliftBasisPoints =
            CanonicalCatalogTargetDerivationPolicy
                .deriveUpliftBasisPoints(
                    totalAxisRequirementCount =
                        variantCoverage
                            .totalAxisRequirementCount,

                    totalRecommendedVariantValueCoverageCount =
                        variantCoverage
                            .totalRecommendedVariantValueCoverageCount,

                    requiredAxisCount =
                        variantCoverage.requiredAxisCount,

                    optionalAxisCount =
                        variantCoverage.optionalAxisCount,

                    crossAxisCombinationAllowedCount =
                        crossAxisCombinationAllowedCount
                )

        val sourceTargetEntryCount =
            variantCoverage
                .currentRecommendedTargetEntryCount

        val derivedTargetEntryCount =
            CanonicalCatalogTargetDerivationPolicy
                .deriveTargetEntryCount(
                    currentTargetEntryCount =
                        sourceTargetEntryCount,

                    upliftBasisPoints =
                        upliftBasisPoints
                )

        val additionalDerivedEntryCount =
            derivedTargetEntryCount -
                    sourceTargetEntryCount

        require(additionalDerivedEntryCount >= 0)

        val familyWeights =
            allFamilies
                .map { family ->
                    FamilyWeight(
                        family =
                            family,

                        weight =
                            complexityWeight(family)
                    )
                }

        val additionalAllocationByFamily =
            allocateAdditionalEntries(
                additionalEntryCount =
                    additionalDerivedEntryCount,

                familyWeights =
                    familyWeights
            )

        val totalComplexityWeight =
            familyWeights.sumOf {
                it.weight
            }

        val categoryTargets =
            variantCoverage.categories
                .map { categoryCoverage ->
                    val familyTargets =
                        categoryCoverage.families
                            .map { family ->
                                val additionalAllocation =
                                    requireNotNull(
                                        additionalAllocationByFamily[
                                            familyIdentity(
                                                category =
                                                    family.category,

                                                familyKey =
                                                    family.familyKey
                                            )
                                        ]
                                    )

                                val familyWeight =
                                    requireNotNull(
                                        familyWeights
                                            .firstOrNull {
                                                it.family.category ==
                                                        family.category &&
                                                        it.family.familyKey ==
                                                        family.familyKey
                                            }
                                    ).weight

                                CanonicalDerivedProductFamilyTarget(
                                    familyKey =
                                        family.familyKey,

                                    category =
                                        family.category,

                                    displayName =
                                        family.displayName,

                                    sourceAllocatedTargetEntryCount =
                                        family
                                            .allocatedTargetEntryCount,

                                    additionalDerivedEntryCount =
                                        additionalAllocation,

                                    derivedTargetEntryCount =
                                        family
                                            .allocatedTargetEntryCount +
                                                additionalAllocation,

                                    requiredAxisCount =
                                        family.requiredAxisCount,

                                    recommendedAxisCount =
                                        family.recommendedAxisCount,

                                    optionalAxisCount =
                                        family.optionalAxisCount,

                                    recommendedVariantValueCoverageCount =
                                        family
                                            .recommendedVariantValueCoverageCount,

                                    crossAxisCombinationAllowedCount =
                                        family
                                            .crossAxisCombinationAllowedCount,

                                    complexityWeight =
                                        familyWeight,

                                    additionalAllocationShare =
                                        if (
                                            additionalDerivedEntryCount == 0
                                        ) {
                                            0.0
                                        } else {
                                            additionalAllocation.toDouble() /
                                                    additionalDerivedEntryCount
                                                        .toDouble()
                                        }
                                )
                            }
                            .sortedBy {
                                it.familyKey
                            }

                    CanonicalDerivedCategoryTarget(
                        category =
                            categoryCoverage.category,

                        sourceTargetEntryCount =
                            familyTargets.sumOf {
                                it.sourceAllocatedTargetEntryCount
                            },

                        additionalDerivedEntryCount =
                            familyTargets.sumOf {
                                it.additionalDerivedEntryCount
                            },

                        derivedTargetEntryCount =
                            familyTargets.sumOf {
                                it.derivedTargetEntryCount
                            },

                        productFamilyCount =
                            familyTargets.size,

                        families =
                            familyTargets,

                        complexityWeight =
                            familyTargets.sumOf {
                                it.complexityWeight
                            },

                        valid = true
                    )
                }
                .sortedBy {
                    it.category
                }

        val blockers =
            buildList {
                if (
                    derivedTargetEntryCount <
                    CanonicalCatalogTargetDerivationPolicy
                        .MINIMUM_TARGET_ENTRY_COUNT
                ) {
                    add(
                        "Derived target $derivedTargetEntryCount is below " +
                                "the allowed minimum."
                    )
                }

                if (
                    derivedTargetEntryCount >
                    CanonicalCatalogTargetDerivationPolicy
                        .MAXIMUM_TARGET_ENTRY_COUNT
                ) {
                    add(
                        "Derived target $derivedTargetEntryCount exceeds " +
                                "the allowed maximum."
                    )
                }

                if (
                    categoryTargets.sumOf {
                        it.additionalDerivedEntryCount
                    } !=
                    additionalDerivedEntryCount
                ) {
                    add(
                        "Additional target allocation is incomplete."
                    )
                }

                if (totalComplexityWeight <= 0L) {
                    add(
                        "Variant complexity weight must be positive."
                    )
                }
            }
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .sorted()

        return CanonicalCatalogTargetDerivation(
            version =
                CanonicalCatalogTargetDerivation
                    .CURRENT_VERSION,

            sourceBaselineId =
                baseline.baselineId,

            sourceBaselineCatalogSha256 =
                baseline.catalogArtifact.sha256,

            baselineEntryCount =
                baseline.finalOutputEntryCount,

            sourceTargetEntryCount =
                sourceTargetEntryCount,

            derivedTargetEntryCount =
                derivedTargetEntryCount,

            minimumAllowedTargetEntryCount =
                CanonicalCatalogTargetDerivationPolicy
                    .MINIMUM_TARGET_ENTRY_COUNT,

            maximumAllowedTargetEntryCount =
                CanonicalCatalogTargetDerivationPolicy
                    .MAXIMUM_TARGET_ENTRY_COUNT,

            upliftBasisPoints =
                upliftBasisPoints,

            upliftShare =
                upliftBasisPoints.toDouble() /
                        10_000.0,

            additionalDerivedEntryCount =
                additionalDerivedEntryCount,

            requiredExpansionFromBaselineEntryCount =
                derivedTargetEntryCount -
                        baseline.finalOutputEntryCount,

            categoryCount =
                categoryTargets.size,

            productFamilyCount =
                allFamilies.size,

            totalAxisRequirementCount =
                variantCoverage
                    .totalAxisRequirementCount,

            totalRecommendedVariantValueCoverageCount =
                variantCoverage
                    .totalRecommendedVariantValueCoverageCount,

            requiredAxisCount =
                variantCoverage.requiredAxisCount,

            recommendedAxisCount =
                variantCoverage.recommendedAxisCount,

            optionalAxisCount =
                variantCoverage.optionalAxisCount,

            crossAxisCombinationAllowedCount =
                crossAxisCombinationAllowedCount,

            crossAxisCombinationRestrictedCount =
                crossAxisCombinationRestrictedCount,

            categories =
                categoryTargets,

            derivedTargetCountsByCategory =
                categoryTargets.associate {
                    it.category to
                            it.derivedTargetEntryCount
                }.toSortedMap(),

            additionalCountsByCategory =
                categoryTargets.associate {
                    it.category to
                            it.additionalDerivedEntryCount
                }.toSortedMap(),

            completeAdditionalAllocation =
                categoryTargets.sumOf {
                    it.additionalDerivedEntryCount
                } ==
                        additionalDerivedEntryCount,

            withinAllowedTargetRange =
                derivedTargetEntryCount in
                        CanonicalCatalogTargetDerivationPolicy
                            .MINIMUM_TARGET_ENTRY_COUNT..
                        CanonicalCatalogTargetDerivationPolicy
                            .MAXIMUM_TARGET_ENTRY_COUNT,

            derivationMethod =
                "Deterministic bounded complexity uplift derived from " +
                        "variant-value density, required-axis share, permitted " +
                        "cross-axis combinations and optional-axis penalty. " +
                        "Additional entries are allocated by product-family " +
                        "variant complexity using largest remainders. No retail " +
                        "SKU dimensions and no Cartesian variant expansion are used.",

            blockers =
                blockers,

            valid =
                blockers.isEmpty()
        )
    }

    private fun complexityWeight(
        family:
        CanonicalProductFamilyVariantCoverageEntry
    ): Long {
        val requirementWeight =
            family.requirements.sumOf {
                requirementWeight(it)
            }

        /*
         * Der bestehende Familienzielbestand bleibt Teil des Gewichts.
         * Dadurch werden große kanonische Familien nicht durch kleine,
         * achsenreiche Spezialfamilien verdrängt.
         */
        val sourceAllocationWeight =
            family.allocatedTargetEntryCount
                .toLong() *
                    SOURCE_ALLOCATION_WEIGHT_MULTIPLIER

        return maxOf(
            1L,
            sourceAllocationWeight +
                    requirementWeight
        )
    }

    private fun requirementWeight(
        requirement:
        CanonicalVariantAxisCoverageRequirement
    ): Long {
        val criticalityMultiplier =
            when (requirement.criticality) {
                CanonicalVariantAxisCriticality.REQUIRED ->
                    5L

                CanonicalVariantAxisCriticality.RECOMMENDED ->
                    3L

                CanonicalVariantAxisCriticality.OPTIONAL ->
                    1L
            }

        val combinationMultiplier =
            if (
                requirement.allowCrossAxisCombination
            ) {
                2L
            } else {
                1L
            }

        return requirement
            .recommendedRelevantValueCount
            .toLong() *
                criticalityMultiplier *
                combinationMultiplier
    }

    private fun allocateAdditionalEntries(
        additionalEntryCount: Int,
        familyWeights: List<FamilyWeight>
    ): Map<String, Int> {
        require(additionalEntryCount >= 0)
        require(familyWeights.isNotEmpty())
        require(familyWeights.all { it.weight > 0L })

        if (additionalEntryCount == 0) {
            return familyWeights
                .associate {
                    familyIdentity(
                        category =
                            it.family.category,

                        familyKey =
                            it.family.familyKey
                    ) to 0
                }
                .toSortedMap()
        }

        val totalWeight =
            familyWeights.sumOf {
                it.weight
            }

        require(totalWeight > 0L)

        val allocations =
            familyWeights.map { familyWeight ->
                val weightedNumerator =
                    additionalEntryCount.toLong() *
                            familyWeight.weight

                FamilyAllocation(
                    identity =
                        familyIdentity(
                            category =
                                familyWeight.family.category,

                            familyKey =
                                familyWeight.family.familyKey
                        ),

                    baseAllocation =
                        (
                                weightedNumerator /
                                        totalWeight
                                ).toInt(),

                    remainder =
                        weightedNumerator %
                                totalWeight
                )
            }

        val baseAllocatedCount =
            allocations.sumOf {
                it.baseAllocation
            }

        val remainingCount =
            additionalEntryCount -
                    baseAllocatedCount

        require(remainingCount >= 0)

        val remainderRecipients =
            allocations
                .sortedWith(
                    compareByDescending<FamilyAllocation> {
                        it.remainder
                    }.thenBy {
                        it.identity
                    }
                )
                .take(remainingCount)
                .map {
                    it.identity
                }
                .toSet()

        val result =
            allocations.associate {
                it.identity to
                        (
                                it.baseAllocation +
                                        if (
                                            it.identity in
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
                    additionalEntryCount
        ) {
            "Derived family allocations do not cover the complete uplift."
        }

        return result
    }

    private fun familyIdentity(
        category: String,
        familyKey: String
    ): String =
        "$category::$familyKey"

    private data class FamilyWeight(
        val family:
        CanonicalProductFamilyVariantCoverageEntry,

        val weight: Long
    )

    private data class FamilyAllocation(
        val identity: String,
        val baseAllocation: Int,
        val remainder: Long
    )

    private companion object {
        const val SOURCE_ALLOCATION_WEIGHT_MULTIPLIER =
            10L
    }
}