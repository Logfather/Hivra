package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch

data class CanonicalSemanticPolicyImplementationBatchResult(
    val version: Int,

    val sourceBaselineId: String,
    val sourceBaselineCatalogSha256: String,

    val sourcePolicySetId: String,

    val generatedCandidateCount: Int,

    val reviewRequiredCandidateCount: Int,

    val missingPolicyGapCount: Int,

    val batchCount: Int,

    val highPriorityBatchCount: Int,
    val mediumPriorityBatchCount: Int,
    val lowPriorityBatchCount: Int,

    val coveredCategoryCount: Int,
    val coveredFamilyCount: Int,
    val coveredAxisCount: Int,

    val affectedCandidateReferenceCount: Int,

    val batches:
    List<CanonicalSemanticPolicyImplementationBatch>,

    val gaps:
    List<CanonicalSemanticPolicyImplementationGap>,

    val completeGapCoverage: Boolean,
    val deterministicOrderValid: Boolean,

    val blockers: List<String>,

    val valid: Boolean
) {

    init {
        require(version > 0)

        require(sourceBaselineId.isNotBlank())
        require(sourcePolicySetId.isNotBlank())

        require(
            SHA_256_REGEX.matches(
                sourceBaselineCatalogSha256
            )
        )

        require(generatedCandidateCount >= 0)
        require(reviewRequiredCandidateCount >= 0)

        require(missingPolicyGapCount >= 0)
        require(batchCount >= 0)

        require(highPriorityBatchCount >= 0)
        require(mediumPriorityBatchCount >= 0)
        require(lowPriorityBatchCount >= 0)

        require(coveredCategoryCount >= 0)
        require(coveredFamilyCount >= 0)
        require(coveredAxisCount >= 0)

        require(affectedCandidateReferenceCount >= 0)

        require(
            missingPolicyGapCount ==
                    gaps.size
        )

        require(
            batchCount ==
                    batches.size
        )

        require(
            gaps ==
                    gaps.sortedWith(
                        compareByDescending<
                                CanonicalSemanticPolicyImplementationGap
                                > { gap ->
                            gap.affectedCandidateCount
                        }.thenBy { gap ->
                            gap.category
                        }.thenBy { gap ->
                            gap.axis.name
                        }.thenBy { gap ->
                            gap.familyKey
                        }
                    )
        ) {
            "Policy gaps must be deterministically sorted."
        }

        require(
            gaps
                .map { gap ->
                    gap.gapIndex
                } ==
                    (1..missingPolicyGapCount).toList()
        ) {
            "Policy gap indices must be contiguous and start at 1."
        }

        require(
            gaps
                .map { gap ->
                    gap.gapKey
                }
                .distinct()
                .size ==
                    gaps.size
        ) {
            "Policy gap keys must be unique."
        }

        require(
            batches
                .map { batch ->
                    batch.batchIndex
                } ==
                    (1..batchCount).toList()
        ) {
            "Implementation batch indices must be contiguous and start at 1."
        }

        require(
            highPriorityBatchCount ==
                    batches.count { batch ->
                        batch.priority ==
                                CanonicalSemanticPolicyBatchPriority.HIGH
                    }
        )

        require(
            mediumPriorityBatchCount ==
                    batches.count { batch ->
                        batch.priority ==
                                CanonicalSemanticPolicyBatchPriority.MEDIUM
                    }
        )

        require(
            lowPriorityBatchCount ==
                    batches.count { batch ->
                        batch.priority ==
                                CanonicalSemanticPolicyBatchPriority.LOW
                    }
        )

        require(
            batchCount ==
                    highPriorityBatchCount +
                    mediumPriorityBatchCount +
                    lowPriorityBatchCount
        )

        require(
            coveredCategoryCount ==
                    gaps
                        .map { gap ->
                            gap.category
                        }
                        .distinct()
                        .size
        )

        require(
            coveredFamilyCount ==
                    gaps
                        .map { gap ->
                            gap.familyKey
                        }
                        .distinct()
                        .size
        )

        require(
            coveredAxisCount ==
                    gaps
                        .map { gap ->
                            gap.axis
                        }
                        .distinct()
                        .size
        )

        require(
            affectedCandidateReferenceCount ==
                    gaps.sumOf { gap ->
                        gap.affectedCandidateCount
                    }
        )

        val batchedGapKeys =
            batches
                .flatMap { batch ->
                    batch.gaps
                }
                .map { gap ->
                    gap.gapKey
                }
                .sorted()

        val sourceGapKeys =
            gaps
                .map { gap ->
                    gap.gapKey
                }
                .sorted()

        require(
            completeGapCoverage ==
                    (
                            batchedGapKeys ==
                                    sourceGapKeys
                            )
        ) {
            "Complete-gap-coverage flag does not match the actual " +
                    "implementation batch coverage."
        }

        require(
            blockers ==
                    blockers
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        ) {
            "Blockers must be normalized, unique and deterministically sorted."
        }

        require(
            valid ==
                    (
                            blockers.isEmpty() &&
                                    completeGapCoverage &&
                                    deterministicOrderValid &&
                                    batches.all { batch ->
                                        batch.complete
                                    }
                            )
        ) {
            "Validity does not match the implementation-batch result invariants."
        }
    }

    companion object {

        const val CURRENT_VERSION =
            1

        private val SHA_256_REGEX =
            Regex("[0-9a-f]{64}")
    }
}