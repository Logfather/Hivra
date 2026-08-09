package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic

import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate
.CanonicalCatalogExpansionCandidateReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.family
.CanonicalProductFamilyVariantAxis
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalBreadGrainTypePolicyIntegrationTest {

    @Test
    fun eliminateBreadGrainTypeMissingPolicyFindings() {
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

        val breadGrainCandidates =
            candidates.candidates
                .filter {
                    it.familyKey == "bread" &&
                            it.variantValues.any { value ->
                                value.axis ==
                                        CanonicalProductFamilyVariantAxis
                                            .GRAIN_TYPE
                            }
                }

        assertEquals(
            78,
            breadGrainCandidates.size,
            "Backlog baseline for bread × GRAIN_TYPE changed."
        )

        val breadGrainCandidateIndices =
            breadGrainCandidates
                .map {
                    it.candidateIndex
                }
                .toSet()

        val relevantEntries =
            result.entries
                .filter {
                    it.candidateIndex in
                            breadGrainCandidateIndices
                }

        assertEquals(
            breadGrainCandidates.size,
            relevantEntries.size
        )

        assertTrue(
            relevantEntries.none { entry ->
                entry.findings.any { finding ->
                    finding.ruleKey ==
                            "missing-family-axis-policy" &&
                            finding.message.contains(
                                "'GRAIN_TYPE'"
                            )
                }
            },
            "Bread grain-type candidates must no longer contain a " +
                    "missing GRAIN_TYPE policy finding."
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