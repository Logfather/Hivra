package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic

import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate.CanonicalCatalogExpansionCandidateReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalBeefAnimalSpeciesPolicyIntegrationTest {

    @Test
    fun eliminateBeefAnimalSpeciesMissingPolicyFindings() {
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

        val beefAnimalSpeciesCandidates =
            candidates.candidates
                .filter { candidate ->
                    candidate.familyKey == "beef" &&
                            candidate.variantValues.any { value ->
                                value.axis ==
                                        CanonicalProductFamilyVariantAxis
                                            .ANIMAL_SPECIES
                            }
                }

        assertTrue(
            beefAnimalSpeciesCandidates.isNotEmpty(),
            "Expected generated beef × ANIMAL_SPECIES candidates."
        )

        val candidateIndices =
            beefAnimalSpeciesCandidates
                .map {
                    it.candidateIndex
                }
                .toSet()

        val relevantEntries =
            result.entries
                .filter {
                    it.candidateIndex in candidateIndices
                }

        assertEquals(
            beefAnimalSpeciesCandidates.size,
            relevantEntries.size,
            "Every generated beef × ANIMAL_SPECIES candidate must be validated."
        )

        assertTrue(
            relevantEntries.none { entry ->
                entry.findings.any { finding ->
                    finding.ruleKey ==
                            "missing-family-axis-policy" &&
                            finding.message.contains("'beef'") &&
                            finding.message.contains("'ANIMAL_SPECIES'")
                }
            },
            "Beef animal-species candidates must no longer contain a " +
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