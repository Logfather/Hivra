package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic

import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate.CanonicalCatalogExpansionCandidateReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalPorkAnimalSpeciesPolicyIntegrationTest {

    @Test
    fun eliminatePorkAnimalSpeciesMissingPolicyFindings() {
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

        val porkAnimalSpeciesCandidates =
            candidates.candidates
                .filter { candidate ->
                    candidate.familyKey == "pork" &&
                            candidate.variantValues.any { value ->
                                value.axis ==
                                        CanonicalProductFamilyVariantAxis
                                            .ANIMAL_SPECIES
                            }
                }

        assertEquals(
            76,
            porkAnimalSpeciesCandidates.size,
            "Backlog baseline for pork × ANIMAL_SPECIES changed."
        )

        val candidateIndices =
            porkAnimalSpeciesCandidates
                .map {
                    it.candidateIndex
                }
                .toSet()

        val relevantEntries =
            result.entries
                .filter {
                    it.candidateIndex in
                            candidateIndices
                }

        assertEquals(
            porkAnimalSpeciesCandidates.size,
            relevantEntries.size
        )

        assertTrue(
            relevantEntries.none { entry ->
                entry.findings.any { finding ->
                    finding.ruleKey ==
                            "missing-family-axis-policy" &&
                            finding.message.contains(
                                "'pork'"
                            ) &&
                            finding.message.contains(
                                "'ANIMAL_SPECIES'"
                            )
                }
            },
            "Pork animal-species candidates must no longer contain a " +
                    "missing ANIMAL_SPECIES policy finding."
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