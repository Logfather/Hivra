package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyImplementationBatchResult
import java.security.MessageDigest

class CanonicalBoundedSemanticPolicyClosurePlanFactory {

    fun create(
        openBatches:
        CanonicalSemanticPolicyImplementationBatchResult
    ): CanonicalBoundedSemanticPolicyClosurePlan {
        require(openBatches.valid)

        val sourceBatches:
                List<SourceBatch> =
            openBatches.batches
                .map { batch ->
                    val gaps =
                        batch.gaps
                            .map { gap ->
                                SourceGap(
                                    sourceBatchKey =
                                        batch.batchKey,

                                    category =
                                        gap.category,

                                    familyKey =
                                        gap.familyKey,

                                    axis =
                                        gap.axis.name,

                                    gapKey =
                                        gap.gapKey,

                                    affectedCandidateCount =
                                        gap.affectedCandidateCount,

                                    observedValues =
                                        gap.observedValues
                                            .map(String::trim)
                                            .filter(String::isNotBlank)
                                            .distinct()
                                            .sorted(),

                                    implementationKey =
                                        gap.implementationKey
                                )
                            }
                            .sortedWith(
                                sourceGapComparator
                            )

                    SourceBatch(
                        batchKey =
                            batch.batchKey,

                        category =
                            batch.category,

                        axis =
                            batch.axis.name,

                        affectedCandidateReferenceCount =
                            gaps.sumOf {
                                it.affectedCandidateCount
                            },

                        gaps =
                            gaps
                    )
                }
                .sortedWith(
                    compareByDescending<SourceBatch> {
                        it.gaps.size
                    }.thenByDescending {
                        it.affectedCandidateReferenceCount
                    }.thenBy {
                        it.category
                    }.thenBy {
                        it.axis
                    }.thenBy {
                        it.batchKey
                    }
                )

        val allGaps:
                List<SourceGap> =
            sourceBatches
                .flatMap { sourceBatch ->
                    sourceBatch.gaps
                }

        require(
            allGaps.size ==
                    openBatches.missingPolicyGapCount
        ) {
            "Open gap count does not match implementation-batch result: " +
                    "expected=${openBatches.missingPolicyGapCount}, " +
                    "actual=${allGaps.size}."
        }

        require(
            allGaps
                .map { gap ->
                    gap.gapKey
                }
                .distinct()
                .size ==
                    allGaps.size
        ) {
            "Open semantic-policy gaps must have unique identities."
        }

        val batchBuckets:
                MutableMap<Int, MutableList<SourceBatch>> =
            (FIRST_WAVE_NUMBER..FINAL_WAVE_NUMBER)
                .associateWith {
                    mutableListOf<SourceBatch>()
                }
                .toMutableMap()

        val gapLoads:
                MutableMap<Int, Int> =
            (FIRST_WAVE_NUMBER..FINAL_WAVE_NUMBER)
                .associateWith {
                    0
                }
                .toMutableMap()

        val candidateLoads:
                MutableMap<Int, Int> =
            (FIRST_WAVE_NUMBER..FINAL_WAVE_NUMBER)
                .associateWith {
                    0
                }
                .toMutableMap()

        /*
         * Source-Batches sind atomar:
         *
         * Ein Implementation-Batch wird vollständig genau einer Wave
         * zugeordnet. Er wird niemals über mehrere Waves aufgeteilt.
         */
        sourceBatches.forEach { sourceBatch ->
            val targetWave =
                batchBuckets
                    .keys
                    .minWith(
                        compareBy<Int> { waveNumber ->
                            requireNotNull(
                                gapLoads[waveNumber]
                            )
                        }.thenBy { waveNumber ->
                            requireNotNull(
                                candidateLoads[waveNumber]
                            )
                        }.thenBy { waveNumber ->
                            requireNotNull(
                                batchBuckets[waveNumber]
                            ).size
                        }.thenBy { waveNumber ->
                            waveNumber
                        }
                    )

            requireNotNull(
                batchBuckets[targetWave]
            ).add(
                sourceBatch
            )

            gapLoads[targetWave] =
                requireNotNull(
                    gapLoads[targetWave]
                ) +
                        sourceBatch.gaps.size

            candidateLoads[targetWave] =
                requireNotNull(
                    candidateLoads[targetWave]
                ) +
                        sourceBatch.affectedCandidateReferenceCount
        }

        val waves =
            batchBuckets
                .entries
                .sortedBy { entry ->
                    entry.key
                }
                .map { (waveNumber, assignedBatches) ->
                    val assignedGaps =
                        assignedBatches
                            .flatMap { sourceBatch ->
                                sourceBatch.gaps
                            }
                            .sortedWith(
                                sourceGapComparator
                            )

                    createWave(
                        waveNumber =
                            waveNumber,

                        assignedGaps =
                            assignedGaps
                    )
                }

        val assignedGapCount =
            waves.sumOf { wave ->
                wave.assignedGapCount
            }

        val uniqueGapIdentityCount =
            waves
                .flatMap { wave ->
                    wave.assignments
                }
                .map { assignment ->
                    assignment.identityKey
                }
                .distinct()
                .size

        val sourceBatchWaveAssignments =
            waves
                .flatMap { wave ->
                    wave.assignments.map { assignment ->
                        assignment.sourceBatchKey to
                                wave.waveNumber
                    }
                }
                .groupBy(
                    keySelector = { assignment ->
                        assignment.first
                    },
                    valueTransform = { assignment ->
                        assignment.second
                    }
                )

        val expectedSourceBatchKeys =
            sourceBatches
                .map { sourceBatch ->
                    sourceBatch.batchKey
                }
                .toSet()

        val sourceBatchesAssignedAtomically =
            sourceBatchWaveAssignments.keys ==
                    expectedSourceBatchKeys &&
                    sourceBatchWaveAssignments
                        .values
                        .all { waveNumbers ->
                            waveNumbers
                                .distinct()
                                .size ==
                                    1
                        }

        val completeGapCoverage =
            assignedGapCount ==
                    openBatches.missingPolicyGapCount &&
                    uniqueGapIdentityCount ==
                    openBatches.missingPolicyGapCount

        val deterministicOrderValid =
            waves ==
                    waves.sortedBy { wave ->
                        wave.waveNumber
                    } &&
                    waves.all { wave ->
                        wave.assignments ==
                                wave.assignments.sortedWith(
                                    assignmentComparator
                                )
                    }

        val boundedByFinalWave =
            waves.size ==
                    REMAINING_WAVE_COUNT &&
                    waves.firstOrNull()
                        ?.waveNumber ==
                    FIRST_WAVE_NUMBER &&
                    waves.lastOrNull()
                        ?.waveNumber ==
                    FINAL_WAVE_NUMBER &&
                    waves.none { wave ->
                        wave.waveNumber >
                                FINAL_WAVE_NUMBER
                    }

        val maximumSourceBatchGapCount =
            sourceBatches
                .maxOf { sourceBatch ->
                    sourceBatch.gaps.size
                }

        val balancedWaveSizes =
            waves
                .map { wave ->
                    wave.assignedGapCount
                }
                .let { counts ->
                    counts.isNotEmpty() &&
                            requireNotNull(
                                counts.maxOrNull()
                            ) -
                            requireNotNull(
                                counts.minOrNull()
                            ) <=
                            maximumSourceBatchGapCount
                }

        val valid =
            completeGapCoverage &&
                    deterministicOrderValid &&
                    boundedByFinalWave &&
                    balancedWaveSizes &&
                    sourceBatchesAssignedAtomically &&
                    waves.all { wave ->
                        wave.valid &&
                                wave.complete
                    }

        return CanonicalBoundedSemanticPolicyClosurePlan(
            version =
                CanonicalBoundedSemanticPolicyClosurePlan
                    .CURRENT_VERSION,

            planId =
                createPlanId(
                    waves =
                        waves
                ),

            firstWaveNumber =
                FIRST_WAVE_NUMBER,

            finalWaveNumber =
                FINAL_WAVE_NUMBER,

            sourceReviewRequiredCandidateCount =
                openBatches.reviewRequiredCandidateCount,

            sourceMissingPolicyGapCount =
                openBatches.missingPolicyGapCount,

            sourceImplementationBatchCount =
                openBatches.batchCount,

            waveCount =
                waves.size,

            waves =
                waves,

            assignedGapCount =
                assignedGapCount,

            uniqueGapIdentityCount =
                uniqueGapIdentityCount,

            completeGapCoverage =
                completeGapCoverage,

            deterministicOrderValid =
                deterministicOrderValid,

            boundedByFinalWave =
                boundedByFinalWave,

            valid =
                valid
        )
    }

    private fun createWave(
        waveNumber: Int,
        assignedGaps: List<SourceGap>
    ): CanonicalBoundedSemanticPolicyClosureWave {
        val assignments =
            assignedGaps
                .sortedWith(
                    sourceGapComparator
                )
                .mapIndexed { index, gap ->
                    CanonicalBoundedSemanticPolicyClosureAssignment(
                        assignmentIndex =
                            index + 1,

                        waveNumber =
                            waveNumber,

                        sourceBatchKey =
                            gap.sourceBatchKey,

                        category =
                            gap.category,

                        familyKey =
                            gap.familyKey,

                        axis =
                            gap.axis,

                        gapKey =
                            gap.gapKey,

                        affectedCandidateCount =
                            gap.affectedCandidateCount,

                        observedValues =
                            gap.observedValues,

                        implementationKey =
                            gap.implementationKey
                    )
                }

        return CanonicalBoundedSemanticPolicyClosureWave(
            waveNumber =
                waveNumber,

            waveKey =
                "generalized-semantic-policy-wave-" +
                        waveNumber
                            .toString()
                            .padStart(
                                length = 3,
                                padChar = '0'
                            ),

            assignedBatchCount =
                assignments
                    .map { assignment ->
                        assignment.sourceBatchKey
                    }
                    .distinct()
                    .size,

            assignedGapCount =
                assignments.size,

            affectedCandidateReferenceCount =
                assignments.sumOf { assignment ->
                    assignment.affectedCandidateCount
                },

            assignments =
                assignments,

            complete =
                assignments.all { assignment ->
                    assignment.waveNumber ==
                            waveNumber
                },

            valid =
                assignments.isNotEmpty() &&
                        assignments
                            .map { assignment ->
                                assignment.identityKey
                            }
                            .distinct()
                            .size ==
                        assignments.size
        )
    }

    private fun createPlanId(
        waves:
        List<CanonicalBoundedSemanticPolicyClosureWave>
    ): String {
        val payload =
            buildString {
                waves.forEach { wave ->
                    append(wave.waveNumber)
                    append('|')
                    append(wave.waveKey)
                    append('\n')

                    wave.assignments.forEach { assignment ->
                        append(assignment.gapKey)
                        append('|')
                        append(assignment.sourceBatchKey)
                        append('|')
                        append(assignment.affectedCandidateCount)
                        append('\n')
                    }
                }
            }

        val hash =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    payload.toByteArray(
                        Charsets.UTF_8
                    )
                )
                .joinToString("") { byte ->
                    "%02x".format(
                        byte.toInt() and 0xff
                    )
                }
                .take(16)

        return "canonical-bounded-semantic-policy-closure-v1-$hash"
    }

    private data class SourceBatch(
        val batchKey: String,
        val category: String,
        val axis: String,
        val affectedCandidateReferenceCount: Int,
        val gaps: List<SourceGap>
    ) {

        init {
            require(batchKey.isNotBlank())
            require(category.isNotBlank())
            require(axis.isNotBlank())
            require(affectedCandidateReferenceCount >= 0)
            require(gaps.isNotEmpty())

            require(
                gaps.all { gap ->
                    gap.sourceBatchKey ==
                            batchKey
                }
            )
        }
    }

    private data class SourceGap(
        val sourceBatchKey: String,
        val category: String,
        val familyKey: String,
        val axis: String,
        val gapKey: String,
        val affectedCandidateCount: Int,
        val observedValues: List<String>,
        val implementationKey: String
    ) {

        init {
            require(sourceBatchKey.isNotBlank())
            require(category.isNotBlank())
            require(familyKey.isNotBlank())
            require(axis.isNotBlank())
            require(gapKey.isNotBlank())
            require(affectedCandidateCount >= 0)
            require(implementationKey.isNotBlank())

            require(
                observedValues ==
                        observedValues
                            .map(String::trim)
                            .filter(String::isNotBlank)
                            .distinct()
                            .sorted()
            )
        }
    }

    private companion object {

        const val FIRST_WAVE_NUMBER =
            4

        const val FINAL_WAVE_NUMBER =
            6

        const val REMAINING_WAVE_COUNT =
            3

        val sourceGapComparator:
                Comparator<SourceGap> =
            compareBy<SourceGap> { gap ->
                gap.category
            }.thenBy { gap ->
                gap.axis
            }.thenBy { gap ->
                gap.familyKey
            }.thenBy { gap ->
                gap.gapKey
            }

        val assignmentComparator:
                Comparator<CanonicalBoundedSemanticPolicyClosureAssignment> =
            compareBy<
                    CanonicalBoundedSemanticPolicyClosureAssignment
                    > { assignment ->
                assignment.category
            }.thenBy { assignment ->
                assignment.axis
            }.thenBy { assignment ->
                assignment.familyKey
            }.thenBy { assignment ->
                assignment.gapKey
            }
    }
}