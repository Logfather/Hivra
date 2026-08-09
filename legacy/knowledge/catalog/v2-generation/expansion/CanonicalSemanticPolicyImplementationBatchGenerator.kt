package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch

import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate.CanonicalCatalogExpansionCandidateGenerationResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalCatalogExpansionSemanticValidationResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalExpansionSemanticDecision
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyIndex
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySet

class CanonicalSemanticPolicyImplementationBatchGenerator(
    private val maximumGapsPerBatch: Int =
        DEFAULT_MAXIMUM_GAPS_PER_BATCH
) {

    init {
        require(maximumGapsPerBatch > 0)
    }

    fun generate(
        candidates:
        CanonicalCatalogExpansionCandidateGenerationResult,

        semanticValidation:
        CanonicalCatalogExpansionSemanticValidationResult,

        policySet:
        CanonicalFamilyAxisSemanticPolicySet
    ): CanonicalSemanticPolicyImplementationBatchResult {
        require(candidates.valid)
        require(semanticValidation.valid)
        require(policySet.valid)

        require(
            candidates.sourceBaselineId ==
                    semanticValidation.sourceBaselineId
        )

        require(
            candidates.sourceBaselineCatalogSha256 ==
                    semanticValidation.sourceBaselineCatalogSha256
        )

        require(
            candidates.generatedCandidateCount ==
                    semanticValidation.evaluatedCandidateCount
        )

        val policyIndex =
            CanonicalFamilyAxisSemanticPolicyIndex(
                policySet = policySet
            )

        val candidateByIndex =
            candidates.candidates
                .associateBy {
                    it.candidateIndex
                }

        require(
            candidateByIndex.size ==
                    candidates.generatedCandidateCount
        )

        val reviewCandidates =
            semanticValidation.entries
                .asSequence()
                .filter {
                    it.decision ==
                            CanonicalExpansionSemanticDecision
                                .REVIEW_REQUIRED
                }
                .map { validationEntry ->
                    val candidate =
                        requireNotNull(
                            candidateByIndex[
                                validationEntry.candidateIndex
                            ]
                        )

                    require(
                        candidate.candidateKey ==
                                validationEntry.candidateKey
                    )

                    candidate
                }
                .sortedBy {
                    it.candidateIndex
                }
                .toList()

        require(
            reviewCandidates.size ==
                    semanticValidation
                        .reviewRequiredCandidateCount
        )

        val accumulators =
            linkedMapOf<
                    String,
                    GapAccumulator
                    >()

        reviewCandidates.forEach { candidate ->
            candidate.variantValues
                .sortedBy {
                    it.axis.name
                }
                .forEach { variantValue ->
                    val completePolicy =
                        policyIndex.hasCompletePolicy(
                            familyKey =
                                candidate.familyKey,

                            axis =
                                variantValue.axis
                        )

                    if (!completePolicy) {
                        val gapKey =
                            CanonicalSemanticPolicyImplementationGap
                                .createGapKey(
                                    familyKey =
                                        candidate.familyKey,

                                    axis =
                                        variantValue.axis
                                )

                        val accumulator =
                            accumulators.getOrPut(
                                gapKey
                            ) {
                                GapAccumulator(
                                    category =
                                        candidate.category,

                                    familyKey =
                                        candidate.familyKey,

                                    axis =
                                        variantValue.axis
                                )
                            }

                        require(
                            accumulator.category ==
                                    candidate.category
                        ) {
                            "Family '${candidate.familyKey}' appears in " +
                                    "multiple categories."
                        }

                        accumulator.candidateIndices +=
                            candidate.candidateIndex

                        accumulator.observedValues +=
                            variantValue.valueKey
                    }
                }
        }

        val reviewRequiredCount =
            semanticValidation
                .reviewRequiredCandidateCount

        val unsortedGaps =
            accumulators
                .values
                .map { accumulator ->
                    val affectedCandidateCount =
                        accumulator
                            .candidateIndices
                            .size

                    val observedValues =
                        accumulator
                            .observedValues
                            .toList()
                            .sorted()

                    UnindexedGap(
                        category =
                            accumulator.category,

                        familyKey =
                            accumulator.familyKey,

                        axis =
                            accumulator.axis,

                        affectedCandidateCount =
                            affectedCandidateCount,

                        affectedReviewShare =
                            if (reviewRequiredCount == 0) {
                                0.0
                            } else {
                                affectedCandidateCount
                                    .toDouble() /
                                        reviewRequiredCount
                                            .toDouble()
                            },

                        observedValues =
                            observedValues
                    )
                }

        val sortedUnindexedGaps =
            unsortedGaps
                .sortedWith(
                    compareByDescending<
                            UnindexedGap
                            > {
                        it.affectedCandidateCount
                    }.thenBy {
                        it.category
                    }.thenBy {
                        it.axis.name
                    }.thenBy {
                        it.familyKey
                    }
                )

        val gaps =
            sortedUnindexedGaps
                .mapIndexed { index, gap ->
                    CanonicalSemanticPolicyImplementationGap(
                        gapIndex =
                            index + 1,

                        gapKey =
                            CanonicalSemanticPolicyImplementationGap
                                .createGapKey(
                                    familyKey =
                                        gap.familyKey,

                                    axis =
                                        gap.axis
                                ),

                        category =
                            gap.category,

                        familyKey =
                            gap.familyKey,

                        axis =
                            gap.axis,

                        affectedCandidateCount =
                            gap.affectedCandidateCount,

                        affectedReviewShare =
                            gap.affectedReviewShare,

                        observedValueCount =
                            gap.observedValues.size,

                        observedValues =
                            gap.observedValues,

                        recommendation =
                            if (
                                gap.observedValues.size == 1
                            ) {
                                CanonicalSemanticPolicyImplementationRecommendation
                                    .REVIEW_CLOSED_IDENTITY
                            } else {
                                CanonicalSemanticPolicyImplementationRecommendation
                                    .CURATE_ALLOWED_VALUES
                            },

                        implementationKey =
                            "implement-${gap.familyKey}-" +
                                    gap.axis.name
                                        .lowercase()
                                        .replace('_', '-') +
                                    "-policy"
                    )
                }

        val batchPartitions =
            gaps
                .groupBy {
                    BatchGroupKey(
                        category =
                            it.category,

                        axis =
                            it.axis
                    )
                }
                .toList()
                .sortedWith(
                    compareByDescending<
                            Pair<
                                    BatchGroupKey,
                                    List<
                                            CanonicalSemanticPolicyImplementationGap
                                            >
                                    >
                            > {
                        it.second.sumOf { gap ->
                            gap.affectedCandidateCount
                        }
                    }.thenBy {
                        it.first.category
                    }.thenBy {
                        it.first.axis.name
                    }
                )
                .flatMap { (groupKey, groupedGaps) ->
                    groupedGaps
                        .sortedWith(
                            compareByDescending<
                                    CanonicalSemanticPolicyImplementationGap
                                    > {
                                it.affectedCandidateCount
                            }.thenBy {
                                it.familyKey
                            }
                        )
                        .chunked(
                            maximumGapsPerBatch
                        )
                        .map { chunk ->
                            UnindexedBatch(
                                category =
                                    groupKey.category,

                                axis =
                                    groupKey.axis,

                                gaps =
                                    chunk
                            )
                        }
                }
                .sortedWith(
                    compareByDescending<
                            UnindexedBatch
                            > {
                        it.gaps.sumOf { gap ->
                            gap.affectedCandidateCount
                        }
                    }.thenBy {
                        it.category
                    }.thenBy {
                        it.axis.name
                    }.thenBy {
                        it.gaps.first().familyKey
                    }
                )

        val batches =
            batchPartitions
                .mapIndexed { index, batch ->
                    val batchIndex =
                        index + 1

                    val affectedCandidateCount =
                        batch.gaps.sumOf {
                            it.affectedCandidateCount
                        }

                    val affectedReviewShare =
                        if (reviewRequiredCount == 0) {
                            0.0
                        } else {
                            affectedCandidateCount
                                .toDouble() /
                                    reviewRequiredCount
                                        .toDouble()
                        }

                    val observedValues =
                        batch.gaps
                            .flatMap {
                                it.observedValues
                            }
                            .distinct()
                            .sorted()

                    CanonicalSemanticPolicyImplementationBatch(
                        batchIndex =
                            batchIndex,

                        batchKey =
                            CanonicalSemanticPolicyImplementationBatch
                                .createBatchKey(
                                    category =
                                        batch.category,

                                    axis =
                                        batch.axis,

                                    batchIndex =
                                        batchIndex
                                ),

                        category =
                            batch.category,

                        axis =
                            batch.axis,

                        priority =
                            priorityFor(
                                affectedReviewShare
                            ),

                        gapCount =
                            batch.gaps.size,

                        affectedCandidateCount =
                            affectedCandidateCount,

                        affectedReviewShare =
                            affectedReviewShare,

                        observedValueCount =
                            observedValues.size,

                        observedValues =
                            observedValues,

                        gaps =
                            batch.gaps,

                        complete =
                            batch.gaps.isNotEmpty() &&
                                    batch.gaps.all {
                                        it.affectedCandidateCount > 0 &&
                                                it.observedValues.isNotEmpty()
                                    }
                    )
                }

        val blockers =
            buildList {
                if (
                    gaps.isNotEmpty() &&
                    batches.isEmpty()
                ) {
                    add(
                        "Open family-axis semantic policy gaps exist, but no " +
                                "semantic policy implementation batches were generated."
                    )
                }

                val batchedGapKeys =
                    batches
                        .flatMap {
                            it.gaps
                        }
                        .map {
                            it.gapKey
                        }
                        .sorted()

                val sourceGapKeys =
                    gaps
                        .map {
                            it.gapKey
                        }
                        .sorted()

                if (batchedGapKeys != sourceGapKeys) {
                    add(
                        "Implementation batches do not cover every missing " +
                                "family-axis policy gap exactly once."
                    )
                }
            }
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .sorted()

        val completeGapCoverage =
            batches
                .flatMap {
                    it.gaps
                }
                .map {
                    it.gapKey
                }
                .sorted() ==
                    gaps
                        .map {
                            it.gapKey
                        }
                        .sorted()

        val deterministicOrderValid =
            gaps ==
                    gaps.sortedWith(
                        compareByDescending<
                                CanonicalSemanticPolicyImplementationGap
                                > {
                            it.affectedCandidateCount
                        }.thenBy {
                            it.category
                        }.thenBy {
                            it.axis.name
                        }.thenBy {
                            it.familyKey
                        }
                    ) &&
                    batches.map {
                        it.batchIndex
                    } ==
                    (1..batches.size).toList()

        return CanonicalSemanticPolicyImplementationBatchResult(
            version =
                CanonicalSemanticPolicyImplementationBatchResult
                    .CURRENT_VERSION,

            sourceBaselineId =
                candidates.sourceBaselineId,

            sourceBaselineCatalogSha256 =
                candidates.sourceBaselineCatalogSha256,

            sourcePolicySetId =
                policySet.policySetId,

            generatedCandidateCount =
                candidates.generatedCandidateCount,

            reviewRequiredCandidateCount =
                reviewRequiredCount,

            missingPolicyGapCount =
                gaps.size,

            batchCount =
                batches.size,

            highPriorityBatchCount =
                batches.count {
                    it.priority ==
                            CanonicalSemanticPolicyBatchPriority.HIGH
                },

            mediumPriorityBatchCount =
                batches.count {
                    it.priority ==
                            CanonicalSemanticPolicyBatchPriority.MEDIUM
                },

            lowPriorityBatchCount =
                batches.count {
                    it.priority ==
                            CanonicalSemanticPolicyBatchPriority.LOW
                },

            coveredCategoryCount =
                gaps
                    .map {
                        it.category
                    }
                    .distinct()
                    .size,

            coveredFamilyCount =
                gaps
                    .map {
                        it.familyKey
                    }
                    .distinct()
                    .size,

            coveredAxisCount =
                gaps
                    .map {
                        it.axis
                    }
                    .distinct()
                    .size,

            affectedCandidateReferenceCount =
                gaps.sumOf {
                    it.affectedCandidateCount
                },

            batches =
                batches,

            gaps =
                gaps,

            completeGapCoverage =
                completeGapCoverage,

            deterministicOrderValid =
                deterministicOrderValid,

            blockers =
                blockers,

            valid =
                blockers.isEmpty() &&
                        completeGapCoverage &&
                        deterministicOrderValid &&
                        batches.all {
                            it.complete
                        }
        )
    }

    private fun priorityFor(
        affectedReviewShare: Double
    ): CanonicalSemanticPolicyBatchPriority =
        when {
            affectedReviewShare >=
                    HIGH_PRIORITY_MINIMUM_REVIEW_SHARE ->
                CanonicalSemanticPolicyBatchPriority.HIGH

            affectedReviewShare >=
                    MEDIUM_PRIORITY_MINIMUM_REVIEW_SHARE ->
                CanonicalSemanticPolicyBatchPriority.MEDIUM

            else ->
                CanonicalSemanticPolicyBatchPriority.LOW
        }

    private data class GapAccumulator(
        val category: String,
        val familyKey: String,
        val axis:
        CanonicalProductFamilyVariantAxis,

        val candidateIndices:
        MutableSet<Int> =
            sortedSetOf(),

        val observedValues:
        MutableSet<String> =
            sortedSetOf()
    )

    private data class UnindexedGap(
        val category: String,
        val familyKey: String,
        val axis:
        CanonicalProductFamilyVariantAxis,
        val affectedCandidateCount: Int,
        val affectedReviewShare: Double,
        val observedValues: List<String>
    )

    private data class BatchGroupKey(
        val category: String,
        val axis:
        CanonicalProductFamilyVariantAxis
    )

    private data class UnindexedBatch(
        val category: String,
        val axis:
        CanonicalProductFamilyVariantAxis,
        val gaps:
        List<CanonicalSemanticPolicyImplementationGap>
    )

    private companion object {
        const val DEFAULT_MAXIMUM_GAPS_PER_BATCH =
            50

        const val HIGH_PRIORITY_MINIMUM_REVIEW_SHARE =
            0.025

        const val MEDIUM_PRIORITY_MINIMUM_REVIEW_SHARE =
            0.005
    }
}