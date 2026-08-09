package de.shopme.testing.system.tools.knowledge.catalog.expansion.value

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaseline
import de.shopme.testing.system.tools.knowledge.catalog.expansion.target.CanonicalCatalogTargetDerivation
import de.shopme.testing.system.tools.knowledge.catalog.expansion.variant.CanonicalProductFamilyVariantCoverage

class CanonicalConcreteVariantValueCoveragePlanner(
    private val selector:
    CanonicalFamilyVariantValueSelector =
        CanonicalFamilyVariantValueSelector()
) {

    fun plan(
        baseline:
        CanonicalFoodCatalogBaseline,

        variantCoverage:
        CanonicalProductFamilyVariantCoverage,

        targetDerivation:
        CanonicalCatalogTargetDerivation
    ): CanonicalConcreteVariantValueCoverage {
        require(baseline.valid)
        require(variantCoverage.valid)
        require(targetDerivation.valid)

        require(
            baseline.baselineId ==
                    variantCoverage.sourceBaselineId
        )

        require(
            baseline.baselineId ==
                    targetDerivation.sourceBaselineId
        )

        require(
            baseline.catalogArtifact.sha256 ==
                    variantCoverage
                        .sourceBaselineCatalogSha256
        )

        require(
            baseline.catalogArtifact.sha256 ==
                    targetDerivation
                        .sourceBaselineCatalogSha256
        )

        require(
            variantCoverage.productFamilyCount ==
                    targetDerivation.productFamilyCount
        )

        val blockers =
            mutableListOf<String>()

        val targetFamiliesByIdentity =
            targetDerivation.categories
                .flatMap {
                    it.families
                }
                .associateBy {
                    "${it.category}::${it.familyKey}"
                }

        val categoryCoverages =
            variantCoverage.categories
                .map { category ->
                    val families =
                        category.families
                            .map { family ->
                                val targetFamily =
                                    requireNotNull(
                                        targetFamiliesByIdentity[
                                            "${family.category}::" +
                                                    family.familyKey
                                        ]
                                    )

                                val axisCoverages =
                                    family.requirements
                                        .map { requirement ->
                                            val selectedValues =
                                                selector.select(
                                                    familyKey =
                                                        family.familyKey,
                                                    category =
                                                        family.category,
                                                    requirement =
                                                        requirement
                                                )

                                            val minimumReached =
                                                selectedValues.size >=
                                                        requirement
                                                            .minimumRelevantValueCount

                                            val recommendedReached =
                                                selectedValues.size >=
                                                        requirement
                                                            .recommendedRelevantValueCount

                                            val maximumRespected =
                                                selectedValues.size <=
                                                        requirement
                                                            .maximumRelevantValueCount

                                            if (!minimumReached) {
                                                blockers +=
                                                    "Family '${family.familyKey}' " +
                                                            "does not reach minimum " +
                                                            "coverage for axis " +
                                                            "'${requirement.axis}'."
                                            }

                                            if (!recommendedReached) {
                                                blockers +=
                                                    "Family '${family.familyKey}' " +
                                                            "does not reach recommended " +
                                                            "coverage for axis " +
                                                            "'${requirement.axis}'."
                                            }

                                            if (!maximumRespected) {
                                                blockers +=
                                                    "Family '${family.familyKey}' " +
                                                            "exceeds maximum coverage " +
                                                            "for axis '${requirement.axis}'."
                                            }

                                            CanonicalFamilyAxisValueCoverage(
                                                axis =
                                                    requirement.axis,

                                                criticality =
                                                    requirement.criticality,

                                                selectionMode =
                                                    requirement.selectionMode,

                                                minimumRequiredValueCount =
                                                    requirement
                                                        .minimumRelevantValueCount,

                                                recommendedValueCount =
                                                    requirement
                                                        .recommendedRelevantValueCount,

                                                maximumAllowedValueCount =
                                                    requirement
                                                        .maximumRelevantValueCount,

                                                selectedValues =
                                                    selectedValues,

                                                selectedValueCount =
                                                    selectedValues.size,

                                                minimumCoverageReached =
                                                    minimumReached,

                                                recommendedCoverageReached =
                                                    recommendedReached,

                                                maximumLimitRespected =
                                                    maximumRespected,

                                                valid =
                                                    minimumReached &&
                                                            recommendedReached &&
                                                            maximumRespected
                                            )
                                        }
                                        .sortedBy {
                                            it.axis.name
                                        }

                                CanonicalProductFamilyConcreteValueCoverage(
                                    familyKey =
                                        family.familyKey,

                                    category =
                                        family.category,

                                    displayName =
                                        family.displayName,

                                    allocatedTargetEntryCount =
                                        targetFamily
                                            .derivedTargetEntryCount,

                                    axisCoverageCount =
                                        axisCoverages.size,

                                    axisCoverages =
                                        axisCoverages,

                                    totalSelectedValueCount =
                                        axisCoverages.sumOf {
                                            it.selectedValueCount
                                        },

                                    allMinimumCoveragesReached =
                                        axisCoverages.all {
                                            it.minimumCoverageReached
                                        },

                                    allRecommendedCoveragesReached =
                                        axisCoverages.all {
                                            it.recommendedCoverageReached
                                        },

                                    allMaximumLimitsRespected =
                                        axisCoverages.all {
                                            it.maximumLimitRespected
                                        },

                                    valid =
                                        axisCoverages.all {
                                            it.valid
                                        }
                                )
                            }
                            .sortedBy {
                                it.familyKey
                            }

                    CanonicalCategoryConcreteValueCoverage(
                        category =
                            category.category,

                        productFamilyCount =
                            families.size,

                        families =
                            families,

                        totalAxisCoverageCount =
                            families.sumOf {
                                it.axisCoverageCount
                            },

                        totalSelectedValueCount =
                            families.sumOf {
                                it.totalSelectedValueCount
                            },

                        completeFamilyCoverage =
                            families.all {
                                it.valid
                            },

                        valid =
                            families.all {
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

        val completeFamilyCount =
            allFamilies.count {
                it.valid
            }

        val incompleteFamilyCount =
            allFamilies.size -
                    completeFamilyCount

        val ready =
            sortedBlockers.isEmpty() &&
                    incompleteFamilyCount == 0

        return CanonicalConcreteVariantValueCoverage(
            version =
                CanonicalConcreteVariantValueCoverage
                    .CURRENT_VERSION,

            sourceBaselineId =
                baseline.baselineId,

            sourceBaselineCatalogSha256 =
                baseline.catalogArtifact.sha256,

            baselineEntryCount =
                baseline.finalOutputEntryCount,

            derivedTargetEntryCount =
                targetDerivation
                    .derivedTargetEntryCount,

            requiredExpansionEntryCount =
                targetDerivation
                    .requiredExpansionFromBaselineEntryCount,

            minimumAllowedTargetEntryCount =
                targetDerivation
                    .minimumAllowedTargetEntryCount,

            maximumAllowedTargetEntryCount =
                targetDerivation
                    .maximumAllowedTargetEntryCount,

            canonicalAxisValueSetCount =
                CanonicalVariantValuePolicy
                    .AXIS_VALUE_SETS
                    .size,

            canonicalVariantValueCount =
                CanonicalVariantValuePolicy
                    .AXIS_VALUE_SETS
                    .sumOf {
                        it.valueCount
                    },

            categoryCount =
                categoryCoverages.size,

            productFamilyCount =
                allFamilies.size,

            axisCoverageCount =
                categoryCoverages.sumOf {
                    it.totalAxisCoverageCount
                },

            selectedFamilyAxisValueCount =
                categoryCoverages.sumOf {
                    it.totalSelectedValueCount
                },

            categories =
                categoryCoverages,

            axisValueSets =
                CanonicalVariantValuePolicy
                    .AXIS_VALUE_SETS,

            familiesWithCompleteValueCoverageCount =
                completeFamilyCount,

            familiesWithIncompleteValueCoverageCount =
                incompleteFamilyCount,

            readyForCombinationCuration =
                ready,

            blockers =
                sortedBlockers,

            valid =
                ready
        )
    }
}