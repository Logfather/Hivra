package de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate

import de.shopme.testing.system.tools.knowledge.catalog.baseline
.CanonicalFoodCatalogBaseline
import de.shopme.testing.system.tools.knowledge.catalog.expansion.combination
.CanonicalProductFamilyCombinationCoverage
import de.shopme.testing.system.tools.knowledge.catalog.expansion.target
.CanonicalCatalogTargetDerivation
import de.shopme.testing.system.tools.knowledge.catalog.expansion.value
.CanonicalConcreteVariantValueCoverage

class CanonicalCatalogExpansionCandidateGenerator(
    private val baselineFamilyAllocator:
    CanonicalBaselineFamilyAllocator =
        CanonicalBaselineFamilyAllocator(),

    private val combinationMaterializer:
    CanonicalExpansionCombinationMaterializer =
        CanonicalExpansionCombinationMaterializer()
) {

    fun generate(
        baseline:
        CanonicalFoodCatalogBaseline,

        targetDerivation:
        CanonicalCatalogTargetDerivation,

        concreteValueCoverage:
        CanonicalConcreteVariantValueCoverage,

        combinationCoverage:
        CanonicalProductFamilyCombinationCoverage
    ): CanonicalCatalogExpansionCandidateGenerationResult {
        require(baseline.valid)
        require(targetDerivation.valid)
        require(concreteValueCoverage.valid)
        require(combinationCoverage.valid)

        require(
            baseline.baselineId ==
                    targetDerivation.sourceBaselineId
        )

        require(
            baseline.baselineId ==
                    concreteValueCoverage.sourceBaselineId
        )

        require(
            baseline.baselineId ==
                    combinationCoverage.sourceBaselineId
        )

        require(
            targetDerivation.derivedTargetEntryCount ==
                    concreteValueCoverage.derivedTargetEntryCount
        )

        require(
            targetDerivation.derivedTargetEntryCount ==
                    combinationCoverage.derivedTargetEntryCount
        )

        val valueFamiliesByIdentity =
            concreteValueCoverage.categories
                .flatMap {
                    it.families
                }
                .associateBy {
                    familyIdentity(
                        category = it.category,
                        familyKey = it.familyKey
                    )
                }

        val combinationFamiliesByIdentity =
            combinationCoverage.categories
                .flatMap {
                    it.families
                }
                .associateBy {
                    familyIdentity(
                        category = it.category,
                        familyKey = it.familyKey
                    )
                }

        val blockers =
            mutableListOf<String>()

        val provisionalCategoryResults =
            targetDerivation.categories
                .map { categoryTarget ->
                    val baselineCount =
                        baseline.categoryCounts[
                            categoryTarget.category
                        ] ?: 0

                    val estimatedBaselineByFamily =
                        baselineFamilyAllocator.allocate(
                            categoryBaselineEntryCount =
                                baselineCount,

                            families =
                                categoryTarget.families
                        )

                    val familyResults =
                        categoryTarget.families
                            .map { familyTarget ->
                                val identity =
                                    familyIdentity(
                                        category =
                                            familyTarget.category,

                                        familyKey =
                                            familyTarget.familyKey
                                    )

                                val valueFamily =
                                    requireNotNull(
                                        valueFamiliesByIdentity[
                                            identity
                                        ]
                                    )

                                val combinationFamily =
                                    requireNotNull(
                                        combinationFamiliesByIdentity[
                                            identity
                                        ]
                                    )

                                val estimatedBaselineCount =
                                    requireNotNull(
                                        estimatedBaselineByFamily[
                                            familyTarget.familyKey
                                        ]
                                    )

                                val requiredExpansionCount =
                                    familyTarget
                                        .derivedTargetEntryCount -
                                            estimatedBaselineCount

                                val generated =
                                    generateFamilyCandidates(
                                        category =
                                            familyTarget.category,

                                        familyKey =
                                            familyTarget.familyKey,

                                        familyDisplayName =
                                            familyTarget.displayName,

                                        requiredCandidateCount =
                                            requiredExpansionCount,

                                        valueFamily =
                                            valueFamily,

                                        combinationFamily =
                                            combinationFamily
                                    )

                                if (
                                    generated.size !=
                                    requiredExpansionCount
                                ) {
                                    blockers +=
                                        "Family '${familyTarget.familyKey}' " +
                                                "generated ${generated.size} of " +
                                                "$requiredExpansionCount required " +
                                                "candidates."
                                }

                                CanonicalExpansionCandidateFamilyResult(
                                    familyKey =
                                        familyTarget.familyKey,

                                    category =
                                        familyTarget.category,

                                    familyDisplayName =
                                        familyTarget.displayName,

                                    sourceTargetEntryCount =
                                        familyTarget
                                            .sourceAllocatedTargetEntryCount,

                                    derivedTargetEntryCount =
                                        familyTarget
                                            .derivedTargetEntryCount,

                                    estimatedBaselineFamilyEntryCount =
                                        estimatedBaselineCount,

                                    requiredExpansionEntryCount =
                                        requiredExpansionCount,

                                    generatedCandidateCount =
                                        generated.size,

                                    candidates =
                                        generated,

                                    complete =
                                        generated.size ==
                                                requiredExpansionCount
                                )
                            }
                            .sortedBy {
                                it.familyKey
                            }

                    CanonicalExpansionCandidateCategoryResult(
                        category =
                            categoryTarget.category,

                        baselineEntryCount =
                            baselineCount,

                        derivedTargetEntryCount =
                            categoryTarget
                                .derivedTargetEntryCount,

                        requiredExpansionEntryCount =
                            categoryTarget
                                .derivedTargetEntryCount -
                                    baselineCount,

                        productFamilyCount =
                            familyResults.size,

                        families =
                            familyResults,

                        generatedCandidateCount =
                            familyResults.sumOf {
                                it.generatedCandidateCount
                            },

                        complete =
                            familyResults.all {
                                it.complete
                            } &&
                                    familyResults.sumOf {
                                        it.generatedCandidateCount
                                    } ==
                                    categoryTarget
                                        .derivedTargetEntryCount -
                                    baselineCount
                    )
                }
                .sortedBy {
                    it.category
                }

        val orderedCandidatesWithoutGlobalIndex =
            provisionalCategoryResults
                .flatMap {
                    it.families
                }
                .flatMap {
                    it.candidates
                }
                .sortedWith(
                    compareBy<
                            CanonicalCatalogExpansionCandidate
                            > {
                        it.category
                    }.thenBy {
                        it.familyKey
                    }.thenBy {
                        it.familyCandidateIndex
                    }
                )

        val candidates =
            orderedCandidatesWithoutGlobalIndex
                .mapIndexed { index, candidate ->
                    candidate.copy(
                        candidateIndex =
                            index + 1
                    )
                }

        val candidatesByFamily =
            candidates.groupBy {
                familyIdentity(
                    category = it.category,
                    familyKey = it.familyKey
                )
            }

        val categories =
            provisionalCategoryResults
                .map { category ->
                    category.copy(
                        families =
                            category.families.map { family ->
                                family.copy(
                                    candidates =
                                        candidatesByFamily[
                                            familyIdentity(
                                                category =
                                                    family.category,

                                                familyKey =
                                                    family.familyKey
                                            )
                                        ].orEmpty()
                                )
                            }
                    )
                }

        val duplicateCandidateKeys =
            candidates
                .groupingBy {
                    it.candidateKey
                }
                .eachCount()
                .filterValues {
                    it > 1
                }

        duplicateCandidateKeys
            .keys
            .sorted()
            .forEach { key ->
                blockers +=
                    "Duplicate generated candidate key '$key'."
            }

        val sortedBlockers =
            blockers
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .sorted()

        val exactExpansionCountReached =
            candidates.size ==
                    targetDerivation
                        .requiredExpansionFromBaselineEntryCount

        val deterministicOrderValid =
            candidates ==
                    candidates.sortedWith(
                        compareBy<
                                CanonicalCatalogExpansionCandidate
                                > {
                            it.category
                        }.thenBy {
                            it.familyKey
                        }.thenBy {
                            it.familyCandidateIndex
                        }
                    )

        return CanonicalCatalogExpansionCandidateGenerationResult(
            version =
                CanonicalCatalogExpansionCandidateGenerationResult
                    .CURRENT_VERSION,

            sourceBaselineId =
                baseline.baselineId,

            sourceBaselineCatalogSha256 =
                baseline.catalogArtifact.sha256,

            baselineEntryCount =
                baseline.finalOutputEntryCount,

            derivedTargetEntryCount =
                targetDerivation.derivedTargetEntryCount,

            requiredExpansionEntryCount =
                targetDerivation
                    .requiredExpansionFromBaselineEntryCount,

            categoryCount =
                categories.size,

            productFamilyCount =
                categories.sumOf {
                    it.productFamilyCount
                },

            generatedCandidateCount =
                candidates.size,

            categories =
                categories,

            candidates =
                candidates,

            uniqueCandidateKeyCount =
                candidates.map {
                    it.candidateKey
                }.distinct().size,

            uniqueProposedNormalizedKeyCount =
                candidates.map {
                    it.proposedNormalizedKey
                }.distinct().size,

            candidatesRequiringSemanticValidationCount =
                candidates.count {
                    it.requiresSemanticValidation
                },

            exactExpansionCountReached =
                exactExpansionCountReached,

            deterministicOrderValid =
                deterministicOrderValid,

            blockers =
                sortedBlockers,

            valid =
                sortedBlockers.isEmpty() &&
                        exactExpansionCountReached &&
                        deterministicOrderValid &&
                        categories.all {
                            it.complete
                        }
        )
    }

    private fun generateFamilyCandidates(
        category: String,
        familyKey: String,
        familyDisplayName: String,
        requiredCandidateCount: Int,

        valueFamily:
        de.shopme.testing.system.tools.knowledge.catalog.expansion.value
        .CanonicalProductFamilyConcreteValueCoverage,

        combinationFamily:
        de.shopme.testing.system.tools.knowledge.catalog.expansion
        .combination
        .CanonicalProductFamilyCombinationCoverageEntry
    ): List<CanonicalCatalogExpansionCandidate> {
        require(requiredCandidateCount >= 0)

        if (requiredCandidateCount == 0) {
            return emptyList()
        }

        val candidates =
            mutableListOf<CanonicalCatalogExpansionCandidate>()

        val seenNormalizedKeys =
            mutableSetOf<String>()

        combinationFamily.templates
            .sortedWith(
                compareByDescending<
                        de.shopme.testing.system.tools.knowledge.catalog
                        .expansion.combination
                        .CanonicalVariantCombinationTemplate
                        > {
                    it.required
                }.thenBy {
                    it.mode.ordinal
                }.thenBy {
                    it.key
                }
            )
            .forEach { template ->
                if (
                    candidates.size >=
                    requiredCandidateCount
                ) {
                    return@forEach
                }

                combinationMaterializer
                    .materialize(
                        family =
                            valueFamily,
                        template =
                            template
                    )
                    .forEach { variantValues ->
                        if (
                            candidates.size >=
                            requiredCandidateCount
                        ) {
                            return@forEach
                        }

                        val normalizedKey =
                            CanonicalExpansionCandidateNaming
                                .proposedNormalizedKey(
                                    familyKey =
                                        familyKey,
                                    variantValues =
                                        variantValues
                                )

                        if (
                            !seenNormalizedKeys.add(
                                normalizedKey
                            )
                        ) {
                            return@forEach
                        }

                        val familyCandidateIndex =
                            candidates.size + 1

                        candidates +=
                            CanonicalCatalogExpansionCandidate(
                                candidateIndex = 1,

                                candidateKey =
                                    CanonicalExpansionCandidateNaming
                                        .candidateKey(
                                            category =
                                                category,
                                            familyKey =
                                                familyKey,
                                            variantValues =
                                                variantValues
                                        ),

                                category =
                                    category,

                                familyKey =
                                    familyKey,

                                familyDisplayName =
                                    familyDisplayName,

                                familyCandidateIndex =
                                    familyCandidateIndex,

                                templateKey =
                                    template.key,

                                combinationMode =
                                    template.mode,

                                variantValues =
                                    variantValues,

                                proposedCanonicalName =
                                    CanonicalExpansionCandidateNaming
                                        .proposedCanonicalName(
                                            familyDisplayName =
                                                familyDisplayName,
                                            variantValues =
                                                variantValues
                                        ),

                                proposedNormalizedKey =
                                    normalizedKey,

                                status =
                                    CanonicalExpansionCandidateStatus
                                        .REQUIRES_SEMANTIC_VALIDATION,

                                requiresSemanticValidation =
                                    true
                            )
                    }
            }

        return candidates
            .sortedBy {
                it.familyCandidateIndex
            }
    }

    private fun familyIdentity(
        category: String,
        familyKey: String
    ): String =
        "$category::$familyKey"
}