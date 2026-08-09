package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic

import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate
.CanonicalCatalogExpansionCandidateReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalCatalogExpansionSemanticValidatorTest {

    @Test
    fun validateEveryGeneratedExpansionCandidate() {
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

        val policySet =
            CanonicalSemanticPolicyTestFixtures
                .readPolicySet(projectDirectory)

        val result =
            CanonicalCatalogExpansionSemanticValidator(
                policySet = policySet
            )
                .validate(candidates)

        assertTrue(
            result.valid,
            result.blockers.joinToString()
        )

        assertEquals(
            9_304,
            result.generatedCandidateCount
        )

        assertEquals(
            result.generatedCandidateCount,
            result.evaluatedCandidateCount
        )

        assertEquals(
            result.evaluatedCandidateCount,
            result.acceptedCandidateCount +
                    result.rejectedCandidateCount +
                    result.reviewRequiredCandidateCount
        )

        assertEquals(
            25,
            result.categoryCount
        )

        assertEquals(
            (1..9_304).toList(),
            result.entries.map {
                it.candidateIndex
            }
        )

        assertTrue(
            result.completeCandidateCoverage
        )

        assertTrue(
            result.deterministicOrderValid
        )
    }

    @Test
    fun validateDeterministically() {
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

        val policySet =
            CanonicalSemanticPolicyTestFixtures
                .readPolicySet(projectDirectory)

        val validator =
            CanonicalCatalogExpansionSemanticValidator(policySet = policySet)

        val first =
            validator.validate(candidates)

        val second =
            validator.validate(candidates)

        assertEquals(first, second)
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
                    "Could not resolve project directory from: " +
                            workingDirectory.absolutePath
                )
        }
    }

    private companion object {
        const val CANDIDATE_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-expansion-candidates.json"
    }
}