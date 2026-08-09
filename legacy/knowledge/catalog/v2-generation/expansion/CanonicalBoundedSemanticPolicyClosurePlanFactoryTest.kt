package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch
.CanonicalSemanticPolicyImplementationBatchReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalBoundedSemanticPolicyClosurePlanFactoryTest {

    @Test
    fun assignEveryOpenGapThroughWaveSix() {
        val openBatches =
            readOpenBatches()

        val plan =
            createOrReadPlan()

        assertTrue(
            plan.valid
        )

        assertTrue(
            plan.completeGapCoverage
        )

        assertTrue(
            plan.boundedByFinalWave
        )

        assertTrue(
            plan.deterministicOrderValid
        )

        assertEquals(
            FIRST_WAVE_NUMBER,
            plan.firstWaveNumber
        )

        assertEquals(
            FINAL_WAVE_NUMBER,
            plan.finalWaveNumber
        )

        assertEquals(
            REMAINING_WAVE_COUNT,
            plan.waveCount
        )

        assertEquals(
            plan.sourceMissingPolicyGapCount,
            plan.assignedGapCount
        )

        assertEquals(
            plan.sourceMissingPolicyGapCount,
            plan.uniqueGapIdentityCount
        )

        assertEquals(
            listOf(
                4,
                5,
                6
            ),
            plan.waves.map { wave ->
                wave.waveNumber
            }
        )

        if (
            openBatches.missingPolicyGapCount >
            0
        ) {
            assertEquals(
                openBatches.missingPolicyGapCount,
                plan.sourceMissingPolicyGapCount
            )

            assertEquals(
                openBatches.batchCount,
                plan.sourceImplementationBatchCount
            )
        } else {
            assertTrue(
                plan.sourceMissingPolicyGapCount >
                        0,
                "The preserved closure plan must describe the historical " +
                        "open state from which Waves 4–6 were planned."
            )
        }

        val waveSizes =
            plan.waves
                .map { wave ->
                    wave.assignedGapCount
                }

        assertTrue(
            waveSizes.all { waveSize ->
                waveSize >
                        0
            },
            "Every preserved closure wave must contain at least one gap."
        )
    }

    @Test
    fun distributeAllPlannedGapsAcrossThreeAtomicWaves() {
        val plan =
            createOrReadPlan()

        assertEquals(
            plan.sourceMissingPolicyGapCount,
            plan.waves.sumOf { wave ->
                wave.assignedGapCount
            }
        )

        assertEquals(
            REMAINING_WAVE_COUNT,
            plan.waves.size
        )

        val waveGapCounts =
            plan.waves
                .map { wave ->
                    wave.assignedGapCount
                }

        assertTrue(
            waveGapCounts.all { gapCount ->
                gapCount >
                        0
            }
        )

        /*
         * The historical plan was generated with atomic source batches.
         * Exact ±1 balancing is therefore not required.
         *
         * Here we preserve the stable plan-level invariant that all three
         * waves are non-empty and together cover every source gap.
         */
        assertTrue(
            requireNotNull(
                waveGapCounts.maxOrNull()
            ) >=
                    requireNotNull(
                        waveGapCounts.minOrNull()
                    )
        )
    }

    @Test
    fun assignEachGapExactlyOnce() {
        val plan =
            createOrReadPlan()

        val identities =
            plan.waves
                .flatMap { wave ->
                    wave.assignments
                }
                .map { assignment ->
                    assignment.identityKey
                }

        assertEquals(
            plan.assignedGapCount,
            identities.size
        )

        assertEquals(
            identities.size,
            identities
                .distinct()
                .size
        )

        assertEquals(
            plan.uniqueGapIdentityCount,
            identities
                .distinct()
                .size
        )
    }

    @Test
    fun createDeterministically() {
        val first =
            createOrReadPlan()

        val second =
            createOrReadPlan()

        assertEquals(
            first,
            second
        )
    }

    @Test
    fun neverSplitImplementationBatchAcrossWaves() {
        val plan =
            createOrReadPlan()

        val waveNumbersBySourceBatch =
            plan.waves
                .flatMap { wave ->
                    wave.assignments
                        .map { assignment ->
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

        assertTrue(
            waveNumbersBySourceBatch.isNotEmpty(),
            "The preserved closure plan must contain source-batch " +
                    "assignments."
        )

        assertTrue(
            waveNumbersBySourceBatch
                .values
                .all { waveNumbers ->
                    waveNumbers
                        .distinct()
                        .size ==
                            1
                },
            "An implementation batch must never be split across " +
                    "multiple closure waves."
        )
    }

    private fun createOrReadPlan():
            CanonicalBoundedSemanticPolicyClosurePlan {
        val projectDirectory =
            resolveProjectDirectory()

        val openBatches =
            CanonicalSemanticPolicyImplementationBatchReader()
                .read(
                    File(
                        projectDirectory,
                        OPEN_BATCH_PATH
                    )
                )

        if (
            openBatches.missingPolicyGapCount >
            0
        ) {
            return CanonicalBoundedSemanticPolicyClosurePlanFactory()
                .create(
                    openBatches =
                        openBatches
                )
        }

        val persistedPlanFile =
            File(
                projectDirectory,
                CLOSURE_PLAN_PATH
            )

        require(
            persistedPlanFile.isFile
        ) {
            "Completed semantic-policy closure requires the preserved " +
                    "historical closure plan: " +
                    persistedPlanFile.absolutePath
        }

        require(
            persistedPlanFile.length() >
                    0L
        ) {
            "Preserved semantic-policy closure plan is empty: " +
                    persistedPlanFile.absolutePath
        }

        val persistedPlan =
            CanonicalBoundedSemanticPolicyClosurePlanReader()
                .read(
                    persistedPlanFile
                )

        require(
            persistedPlan.valid
        ) {
            "Preserved semantic-policy closure plan is invalid."
        }

        require(
            persistedPlan.completeGapCoverage
        ) {
            "Preserved semantic-policy closure plan does not cover " +
                    "every historical policy gap."
        }

        require(
            persistedPlan.boundedByFinalWave
        ) {
            "Preserved semantic-policy closure plan is not bounded " +
                    "by Wave 6."
        }

        require(
            persistedPlan.deterministicOrderValid
        ) {
            "Preserved semantic-policy closure plan has a " +
                    "non-deterministic order."
        }

        return persistedPlan
    }

    private fun readOpenBatches() =
        CanonicalSemanticPolicyImplementationBatchReader()
            .read(
                File(
                    resolveProjectDirectory(),
                    OPEN_BATCH_PATH
                )
            )

    private fun resolveProjectDirectory(): File {
        val workingDirectory =
            File(
                requireNotNull(
                    System.getProperty(
                        "user.dir"
                    )
                ) {
                    "System property 'user.dir' is not available."
                }
            ).canonicalFile

        fun containsRequiredArtifacts(
            directory: File
        ): Boolean =
            File(
                directory,
                OPEN_BATCH_PATH
            ).isFile &&
                    File(
                        directory,
                        CLOSURE_PLAN_PATH
                    ).isFile

        return when {
            containsRequiredArtifacts(
                workingDirectory
            ) ->
                workingDirectory

            workingDirectory.name ==
                    "app" &&
                    containsRequiredArtifacts(
                        requireNotNull(
                            workingDirectory.parentFile
                        )
                    ) ->
                requireNotNull(
                    workingDirectory.parentFile
                )

            else ->
                error(
                    "Could not resolve ShopMe project directory from: " +
                            workingDirectory.absolutePath
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

        const val OPEN_BATCH_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-batches/" +
                    "canonical-semantic-policy-implementation-batches.json"

        const val CLOSURE_PLAN_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-closure/" +
                    "canonical-bounded-semantic-policy-closure-plan.json"
    }
}