package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch

import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate.CanonicalCatalogExpansionCandidateReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalCatalogExpansionSemanticValidationReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySetReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalSemanticPolicyImplementationBatchGeneratorTest {

    @Test
    fun generateCompleteDeterministicPolicyBatches() {
        val fixtures =
            readFixtures()

        val result =
            CanonicalSemanticPolicyImplementationBatchGenerator()
                .generate(
                    candidates =
                        fixtures.candidates,

                    semanticValidation =
                        fixtures.semanticValidation,

                    policySet =
                        fixtures.policySet
                )

        assertTrue(
            result.valid,
            result.blockers.joinToString()
        )

        assertEquals(
            fixtures.semanticValidation
                .reviewRequiredCandidateCount,
            result.reviewRequiredCandidateCount
        )

        assertEquals(
            result.missingPolicyGapCount,
            result.gaps.size
        )

        assertEquals(
            result.missingPolicyGapCount,
            result.batches
                .sumOf {
                    it.gapCount
                }
        )

        assertEquals(
            result.gaps
                .map {
                    it.gapKey
                }
                .sorted(),
            result.batches
                .flatMap {
                    it.gaps
                }
                .map {
                    it.gapKey
                }
                .sorted()
        )

        assertEquals(
            result.missingPolicyGapCount,
            result.gaps
                .map {
                    it.gapKey
                }
                .distinct()
                .size
        )

        assertTrue(result.completeGapCoverage)
        assertTrue(result.deterministicOrderValid)

        assertTrue(
            result.batches.all {
                it.gapCount <= 50
            }
        )
    }

    @Test
    fun generateDeterministically() {
        val fixtures =
            readFixtures()

        val generator =
            CanonicalSemanticPolicyImplementationBatchGenerator()

        val first =
            generator.generate(
                candidates =
                    fixtures.candidates,

                semanticValidation =
                    fixtures.semanticValidation,

                policySet =
                    fixtures.policySet
            )

        val second =
            generator.generate(
                candidates =
                    fixtures.candidates,

                semanticValidation =
                    fixtures.semanticValidation,

                policySet =
                    fixtures.policySet
            )

        assertEquals(first, second)
    }

    @Test
    fun acceptCompletedSemanticPolicyClosure() {
        val fixtures =
            readFixtures()

        val result =
            CanonicalSemanticPolicyImplementationBatchGenerator()
                .generate(
                    candidates =
                        fixtures.candidates,

                    semanticValidation =
                        fixtures.semanticValidation,

                    policySet =
                        fixtures.policySet
                )

        assertTrue(
            result.valid,
            result.blockers.joinToString(
                separator =
                    System.lineSeparator()
            )
        )

        assertEquals(
            0,
            result.missingPolicyGapCount
        )

        assertEquals(
            0,
            result.batchCount
        )

        assertTrue(
            result.batches.isEmpty()
        )

        assertTrue(
            result.completeGapCoverage
        )

        assertTrue(
            result.blockers.isEmpty()
        )
    }

    private fun readFixtures(): Fixtures {
        val projectDirectory =
            resolveProjectDirectory()

        return Fixtures(
            candidates =
                CanonicalCatalogExpansionCandidateReader()
                    .read(
                        File(
                            projectDirectory,
                            CANDIDATE_PATH
                        )
                    ),

            semanticValidation =
                CanonicalCatalogExpansionSemanticValidationReader()
                    .read(
                        File(
                            projectDirectory,
                            SEMANTIC_VALIDATION_PATH
                        )
                    ),

            policySet =
                CanonicalFamilyAxisSemanticPolicySetReader()
                    .read(
                        File(
                            projectDirectory,
                            POLICY_SET_PATH
                        )
                    )
        )
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

    private data class Fixtures(
        val candidates:
        de.shopme.testing.system.tools.knowledge.catalog.expansion
        .candidate
        .CanonicalCatalogExpansionCandidateGenerationResult,

        val semanticValidation:
        de.shopme.testing.system.tools.knowledge.catalog.expansion
        .semantic
        .CanonicalCatalogExpansionSemanticValidationResult,

        val policySet:
        de.shopme.testing.system.tools.knowledge.catalog.expansion
        .semantic
        .policy
        .CanonicalFamilyAxisSemanticPolicySet
    )

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
    }
}