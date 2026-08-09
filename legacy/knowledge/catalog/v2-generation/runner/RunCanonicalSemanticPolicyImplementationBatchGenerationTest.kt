package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate.CanonicalCatalogExpansionCandidateReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalCatalogExpansionSemanticValidationReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyImplementationBatchGenerator
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyImplementationBatchReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyImplementationBatchWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySetReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalSemanticPolicyImplementationBatchGenerationTest {

    @Test
    fun generateSemanticPolicyImplementationBatches() {
        val projectDirectory =
            resolveProjectDirectory()

        val candidates =
            CanonicalCatalogExpansionCandidateReader()
                .read(
                    File(
                        projectDirectory,
                        CANDIDATE_PATH
                    )
                )

        val semanticValidation =
            CanonicalCatalogExpansionSemanticValidationReader()
                .read(
                    File(
                        projectDirectory,
                        SEMANTIC_VALIDATION_PATH
                    )
                )

        val policySet =
            CanonicalFamilyAxisSemanticPolicySetReader()
                .read(
                    File(
                        projectDirectory,
                        POLICY_SET_PATH
                    )
                )

        val result =
            CanonicalSemanticPolicyImplementationBatchGenerator()
                .generate(
                    candidates = candidates,
                    semanticValidation = semanticValidation,
                    policySet = policySet
                )

        assertTrue(
            result.valid,
            result.blockers.joinToString()
        )

        assertEquals(
            semanticValidation.reviewRequiredCandidateCount,
            result.reviewRequiredCandidateCount
        )

        val outputFile =
            File(
                projectDirectory,
                OUTPUT_PATH
            )

        CanonicalSemanticPolicyImplementationBatchWriter()
            .write(
                result = result,
                outputFile = outputFile
            )

        val persisted =
            CanonicalSemanticPolicyImplementationBatchReader()
                .read(outputFile)

        assertEquals(result, persisted)

        val hasOpenPolicyGaps =
            persisted.missingPolicyGapCount > 0

        if (hasOpenPolicyGaps) {
            assertTrue(
                persisted.batches.isNotEmpty(),
                "Open policy gaps require at least one implementation batch."
            )

            assertTrue(
                persisted.gaps.isNotEmpty(),
                "Open policy gaps require at least one implementation gap."
            )

            val highestPriorityBatch =
                persisted.batches.first()

            val highestPriorityGap =
                persisted.gaps.first()

            println(
                buildString {
                    appendLine(
                        "Canonical semantic policy implementation batches"
                    )
                    appendLine(
                        "------------------------------------------------"
                    )
                    appendLine(
                        "Generated candidates: " +
                                persisted.generatedCandidateCount
                    )
                    appendLine(
                        "Review-required candidates: " +
                                persisted.reviewRequiredCandidateCount
                    )
                    appendLine(
                        "Missing family-axis policy gaps: " +
                                persisted.missingPolicyGapCount
                    )
                    appendLine(
                        "Implementation batches: " +
                                persisted.batchCount
                    )
                    appendLine(
                        "High-priority batches: " +
                                persisted.highPriorityBatchCount
                    )
                    appendLine(
                        "Medium-priority batches: " +
                                persisted.mediumPriorityBatchCount
                    )
                    appendLine(
                        "Low-priority batches: " +
                                persisted.lowPriorityBatchCount
                    )
                    appendLine(
                        "Covered categories: " +
                                persisted.coveredCategoryCount
                    )
                    appendLine(
                        "Covered families: " +
                                persisted.coveredFamilyCount
                    )
                    appendLine(
                        "Covered axes: " +
                                persisted.coveredAxisCount
                    )
                    appendLine(
                        "Affected candidate references: " +
                                persisted.affectedCandidateReferenceCount
                    )
                    appendLine(
                        "Highest-priority batch: " +
                                highestPriorityBatch.batchKey
                    )
                    appendLine(
                        "Highest-priority batch gaps: " +
                                highestPriorityBatch.gapCount
                    )
                    appendLine(
                        "Highest-priority batch affected candidates: " +
                                highestPriorityBatch.affectedCandidateCount
                    )
                    appendLine(
                        "Highest-priority implementation: " +
                                highestPriorityGap.implementationKey
                    )
                    appendLine(
                        "Complete gap coverage: " +
                                persisted.completeGapCoverage
                    )
                    append(
                        "Batch generation valid: " +
                                persisted.valid
                    )
                }
            )
        } else {
            assertEquals(
                0,
                persisted.batchCount
            )

            assertEquals(
                0,
                persisted.gaps.size
            )

            assertTrue(
                persisted.batches.isEmpty()
            )

            assertEquals(
                0,
                persisted.highPriorityBatchCount
            )

            assertEquals(
                0,
                persisted.mediumPriorityBatchCount
            )

            assertEquals(
                0,
                persisted.lowPriorityBatchCount
            )

            assertEquals(
                0,
                persisted.coveredCategoryCount
            )

            assertEquals(
                0,
                persisted.coveredFamilyCount
            )

            assertEquals(
                0,
                persisted.coveredAxisCount
            )

            assertEquals(
                0,
                persisted.affectedCandidateReferenceCount
            )

            assertTrue(
                persisted.completeGapCoverage
            )

            assertTrue(
                persisted.blockers.isEmpty()
            )

            assertTrue(
                persisted.valid
            )

            println(
                buildString {
                    appendLine(
                        "Canonical semantic policy implementation batches"
                    )
                    appendLine(
                        "------------------------------------------------"
                    )
                    appendLine(
                        "Generated candidates: " +
                                persisted.generatedCandidateCount
                    )
                    appendLine(
                        "Review-required candidates: " +
                                persisted.reviewRequiredCandidateCount
                    )
                    appendLine(
                        "Missing family-axis policy gaps: 0"
                    )
                    appendLine(
                        "Implementation batches: 0"
                    )
                    appendLine(
                        "High-priority batches: 0"
                    )
                    appendLine(
                        "Medium-priority batches: 0"
                    )
                    appendLine(
                        "Low-priority batches: 0"
                    )
                    appendLine(
                        "Covered categories: 0"
                    )
                    appendLine(
                        "Covered families: 0"
                    )
                    appendLine(
                        "Covered axes: 0"
                    )
                    appendLine(
                        "Affected candidate references: 0"
                    )
                    appendLine(
                        "Closure state: COMPLETE"
                    )
                    appendLine(
                        "Complete gap coverage: " +
                                persisted.completeGapCoverage
                    )
                    append(
                        "Batch generation valid: " +
                                persisted.valid
                    )
                }
            )
        }
    }

    private fun resolveProjectDirectory(): File {
        val workingDirectory =
            File(
                requireNotNull(
                    System.getProperty("user.dir")
                )
            ).canonicalFile

        return when {
            File(
                workingDirectory,
                CANDIDATE_PATH
            ).isFile ->
                workingDirectory

            workingDirectory.name == "app" ->
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

        const val CANDIDATE_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-expansion-candidates.json"

        const val SEMANTIC_VALIDATION_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-expansion-semantic-validation.json"

        const val POLICY_SET_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-family-axis-semantic-policies.json"

        const val OUTPUT_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "policy-batches/" +
                    "canonical-semantic-policy-implementation-batches.json"
    }
}