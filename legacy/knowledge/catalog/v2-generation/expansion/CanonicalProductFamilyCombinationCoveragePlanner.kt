package de.shopme.testing.system.tools.knowledge.catalog.expansion.combination

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaseline
import de.shopme.testing.system.tools.knowledge.catalog.expansion.target.CanonicalCatalogTargetDerivation
import de.shopme.testing.system.tools.knowledge.catalog.expansion.value.CanonicalConcreteVariantValueCoverage

class CanonicalProductFamilyCombinationCoveragePlanner(
    private val templateFactory:
    CanonicalVariantCombinationTemplateFactory =
        CanonicalVariantCombinationTemplateFactory()
) {

    fun plan(
        baseline:
        CanonicalFoodCatalogBaseline,

        concreteValueCoverage:
        CanonicalConcreteVariantValueCoverage,

        targetDerivation:
        CanonicalCatalogTargetDerivation
    ): CanonicalProductFamilyCombinationCoverage {
        require(baseline.valid)
        require(concreteValueCoverage.valid)
        require(targetDerivation.valid)

        require(
            baseline.baselineId ==
                    concreteValueCoverage.sourceBaselineId
        )

        require(
            baseline.baselineId ==
                    targetDerivation.sourceBaselineId
        )

        require(
            baseline.catalogArtifact.sha256 ==
                    concreteValueCoverage
                        .sourceBaselineCatalogSha256
        )

        require(
            baseline.catalogArtifact.sha256 ==
                    targetDerivation
                        .sourceBaselineCatalogSha256
        )

        require(
            concreteValueCoverage.derivedTargetEntryCount ==
                    targetDerivation.derivedTargetEntryCount
        )

        val targetCategoriesByKey =
            targetDerivation.categories
                .associateBy { it.category }

        val blockers =
            mutableListOf<String>()

        val categories =
            concreteValueCoverage.categories
                .map { categoryCoverage ->
                    val targetCategory =
                        requireNotNull(
                            targetCategoriesByKey[
                                categoryCoverage.category
                            ]
                        )

                    val families =
                        categoryCoverage.families
                            .map { familyCoverage ->
                                val templates =
                                    templateFactory.create(
                                        familyCoverage
                                    )

                                val capacity =
                                    templates.sumOf {
                                        it.maximumMaterializedCombinationCount
                                    }

                                val targetCapacityReached =
                                    capacity >=
                                            familyCoverage
                                                .allocatedTargetEntryCount

                                if (!targetCapacityReached) {
                                    blockers +=
                                        "Combination capacity for family " +
                                                "'${familyCoverage.familyKey}' " +
                                                "is $capacity but target is " +
                                                familyCoverage
                                                    .allocatedTargetEntryCount +
                                                "."
                                }

                                val requiredTemplateCount =
                                    templates.count {
                                        it.required
                                    }

                                if (requiredTemplateCount == 0) {
                                    blockers +=
                                        "Family '${familyCoverage.familyKey}' " +
                                                "has no required combination template."
                                }

                                CanonicalProductFamilyCombinationCoverageEntry(
                                    familyKey =
                                        familyCoverage.familyKey,

                                    category =
                                        familyCoverage.category,

                                    displayName =
                                        familyCoverage.displayName,

                                    derivedTargetEntryCount =
                                        familyCoverage
                                            .allocatedTargetEntryCount,

                                    templateCount =
                                        templates.size,

                                    templates =
                                        templates,

                                    curatedCombinationCapacity =
                                        capacity,

                                    requiredTemplateCount =
                                        requiredTemplateCount,

                                    optionalTemplateCount =
                                        templates.count {
                                            !it.required
                                        },

                                    targetCapacityReached =
                                        targetCapacityReached,

                                    cartesianExpansionForbidden =
                                        true,

                                    valid =
                                        templates.isNotEmpty() &&
                                                requiredTemplateCount > 0 &&
                                                targetCapacityReached
                                )
                            }
                            .sortedBy {
                                it.familyKey
                            }

                    CanonicalCategoryCombinationCoverage(
                        category =
                            categoryCoverage.category,

                        derivedTargetEntryCount =
                            targetCategory
                                .derivedTargetEntryCount,

                        productFamilyCount =
                            families.size,

                        families =
                            families,

                        templateCount =
                            families.sumOf {
                                it.templateCount
                            },

                        curatedCombinationCapacity =
                            families.sumOf {
                                it.curatedCombinationCapacity
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
            categories.flatMap {
                it.families
            }

        val allTemplates =
            allFamilies.flatMap {
                it.templates
            }

        val sortedBlockers =
            blockers
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .sorted()

        val completeCount =
            allFamilies.count {
                it.valid
            }

        val incompleteCount =
            allFamilies.size -
                    completeCount

        val ready =
            sortedBlockers.isEmpty() &&
                    incompleteCount == 0

        return CanonicalProductFamilyCombinationCoverage(
            version =
                CanonicalProductFamilyCombinationCoverage
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

            categoryCount =
                categories.size,

            productFamilyCount =
                allFamilies.size,

            combinationTemplateCount =
                allTemplates.size,

            curatedCombinationCapacity =
                allFamilies.sumOf {
                    it.curatedCombinationCapacity
                },

            singleAxisTemplateCount =
                allTemplates.count {
                    it.mode ==
                            CanonicalVariantCombinationMode.SINGLE_AXIS
                },

            anchoredPairTemplateCount =
                allTemplates.count {
                    it.mode ==
                            CanonicalVariantCombinationMode.ANCHORED_PAIR
                },

            curatedTripleTemplateCount =
                allTemplates.count {
                    it.mode ==
                            CanonicalVariantCombinationMode.CURATED_TRIPLE
                },

            categories =
                categories,

            familiesWithCompleteCombinationCoverageCount =
                completeCount,

            familiesWithIncompleteCombinationCoverageCount =
                incompleteCount,

            cartesianExpansionForbidden =
                true,

            readyForCandidateGeneration =
                ready,

            blockers =
                sortedBlockers,

            valid =
                ready
        )
    }
}