package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis

data class CanonicalSemanticPolicyImplementationBatch(
    val batchIndex: Int,

    val batchKey: String,

    val category: String,

    val axis:
    CanonicalProductFamilyVariantAxis,

    val priority:
    CanonicalSemanticPolicyBatchPriority,

    val gapCount: Int,

    val affectedCandidateCount: Int,

    val affectedReviewShare: Double,

    val observedValueCount: Int,

    val observedValues: List<String>,

    val gaps:
    List<CanonicalSemanticPolicyImplementationGap>,

    val complete: Boolean
) {

    init {
        require(batchIndex > 0)

        require(category.isNotBlank())

        require(
            batchKey ==
                    createBatchKey(
                        category = category,
                        axis = axis,
                        batchIndex = batchIndex
                    )
        )

        require(gapCount == gaps.size)
        require(gapCount > 0)

        require(
            gaps ==
                    gaps.sortedWith(
                        compareByDescending<
                                CanonicalSemanticPolicyImplementationGap
                                > {
                            it.affectedCandidateCount
                        }.thenBy {
                            it.familyKey
                        }
                    )
        ) {
            "Batch gaps must be deterministically sorted."
        }

        require(
            gaps.all {
                it.category == category &&
                        it.axis == axis
            }
        )

        require(
            affectedCandidateCount ==
                    gaps.sumOf {
                        it.affectedCandidateCount
                    }
        )

        require(
            affectedReviewShare in 0.0..1.0
        )

        require(
            observedValues ==
                    observedValues
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        )

        require(observedValueCount == observedValues.size)

        require(
            observedValues ==
                    gaps
                        .flatMap {
                            it.observedValues
                        }
                        .distinct()
                        .sorted()
        )

        require(
            complete ==
                    (
                            gaps.isNotEmpty() &&
                                    gaps.all {
                                        it.affectedCandidateCount > 0 &&
                                                it.observedValues.isNotEmpty()
                                    }
                            )
        )
    }

    companion object {

        fun createBatchKey(
            category: String,
            axis: CanonicalProductFamilyVariantAxis,
            batchIndex: Int
        ): String =
            buildString {
                append("semantic-policy-batch-")
                append(
                    category
                        .lowercase()
                        .replace('_', '-')
                )
                append('-')
                append(
                    axis.name
                        .lowercase()
                        .replace('_', '-')
                )
                append('-')
                append(
                    batchIndex
                        .toString()
                        .padStart(
                            length = 3,
                            padChar = '0'
                        )
                )
            }
    }
}