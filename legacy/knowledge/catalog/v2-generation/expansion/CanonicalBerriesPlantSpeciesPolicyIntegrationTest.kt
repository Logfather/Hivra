package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic

import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate.CanonicalCatalogExpansionCandidateReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalBerriesPlantSpeciesPolicyIntegrationTest {

    @Test
    fun eliminateBerriesPlantSpeciesMissingPolicyFindings() {
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

        val berryPlantSpeciesCandidates =
            candidates.candidates
                .filter { candidate ->
                    candidate.familyKey == "berries" &&
                            candidate.variantValues.any { value ->
                                value.axis ==
                                        CanonicalProductFamilyVariantAxis
                                            .PLANT_SPECIES
                            }
                }

        assertEquals(
            70,
            berryPlantSpeciesCandidates.size,
            "Backlog baseline for berries × PLANT_SPECIES changed."
        )

        val candidateIndices =
            berryPlantSpeciesCandidates
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
            berryPlantSpeciesCandidates.size,
            relevantEntries.size
        )

        assertTrue(
            relevantEntries.none { entry ->
                entry.findings.any { finding ->
                    finding.ruleKey ==
                            "missing-family-axis-policy" &&
                            finding.message.contains(
                                "'berries'"
                            ) &&
                            finding.message.contains(
                                "'PLANT_SPECIES'"
                            )
                }
            },
            "Berry plant-species candidates must no longer contain a " +
                    "missing PLANT_SPECIES policy finding."
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