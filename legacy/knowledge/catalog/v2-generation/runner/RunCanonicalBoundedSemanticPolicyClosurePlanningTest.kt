package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch
.CanonicalSemanticPolicyImplementationBatchReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure
.CanonicalBoundedSemanticPolicyClosurePlanFactory
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure
.CanonicalBoundedSemanticPolicyClosurePlanReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure
.CanonicalBoundedSemanticPolicyClosurePlanWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalBoundedSemanticPolicyClosurePlanningTest {

    @Test
    fun planClosureThroughWaveSix() {
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

        val outputFile =
            File(
                projectDirectory,
                OUTPUT_PATH
            )

        val planningMode: String

        val plan =
            if (
                openBatches.missingPolicyGapCount >
                0
            ) {
                planningMode =
                    "GENERATED_FROM_OPEN_BACKLOG"

                CanonicalBoundedSemanticPolicyClosurePlanFactory()
                    .create(
                        openBatches =
                            openBatches
                    )
            } else {
                planningMode =
                    "PRESERVED_COMPLETED_CLOSURE"

                require(
                    outputFile.isFile
                ) {
                    "Completed semantic-policy closure requires the " +
                            "preserved closure plan: " +
                            outputFile.absolutePath
                }

                require(
                    outputFile.length() >
                            0L
                ) {
                    "Preserved closure plan is empty: " +
                            outputFile.absolutePath
                }

                CanonicalBoundedSemanticPolicyClosurePlanReader()
                    .read(
                        outputFile
                    )
            }

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

        if (
            openBatches.missingPolicyGapCount >
            0
        ) {
            CanonicalBoundedSemanticPolicyClosurePlanWriter()
                .write(
                    plan =
                        plan,

                    outputFile =
                        outputFile
                )
        }

        val persisted =
            CanonicalBoundedSemanticPolicyClosurePlanReader()
                .read(
                    outputFile
                )

        assertEquals(
            plan,
            persisted
        )

        println(
            buildString {
                appendLine(
                    "Bounded semantic policy closure plan"
                )
                appendLine(
                    "------------------------------------"
                )
                appendLine(
                    "Planning mode: $planningMode"
                )
                appendLine(
                    "Plan: ${persisted.planId}"
                )
                appendLine(
                    "Open review candidates: " +
                            persisted.sourceReviewRequiredCandidateCount
                )
                appendLine(
                    "Open implementation batches: " +
                            persisted.sourceImplementationBatchCount
                )
                appendLine(
                    "Open policy gaps: " +
                            persisted.sourceMissingPolicyGapCount
                )
                appendLine(
                    "Remaining waves: " +
                            persisted.waveCount
                )

                persisted.waves.forEach { wave ->
                    appendLine(
                        "Wave ${wave.waveNumber}: " +
                                "${wave.assignedGapCount} gaps, " +
                                "${wave.assignedBatchCount} source batches, " +
                                "${wave.affectedCandidateReferenceCount} " +
                                "candidate references"
                    )
                }

                appendLine(
                    "Complete gap coverage: " +
                            persisted.completeGapCoverage
                )
                appendLine(
                    "Bounded by Wave 6: " +
                            persisted.boundedByFinalWave
                )
                append(
                    "Plan valid: " +
                            persisted.valid
                )
            }
        )
    }

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
                        OUTPUT_PATH
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

        const val OPEN_BATCH_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-batches/" +
                    "canonical-semantic-policy-implementation-batches.json"

        const val OUTPUT_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-closure/" +
                    "canonical-bounded-semantic-policy-closure-plan.json"
    }
}