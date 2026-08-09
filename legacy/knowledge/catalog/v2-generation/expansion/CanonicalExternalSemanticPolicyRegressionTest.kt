package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy

import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate.CanonicalCatalogExpansionCandidateReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalCatalogExpansionSemanticValidationReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalCatalogExpansionSemanticValidator
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalExternalSemanticPolicyRegressionTest {

    @Test
    fun preserveCurrentSemanticValidationDecisions() {
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

        val expected =
            CanonicalCatalogExpansionSemanticValidationReader()
                .read(
                    File(
                        projectDirectory,
                        CURRENT_VALIDATION_PATH
                    )
                )

        val policySet =
            CanonicalFamilyAxisSemanticPolicySetReader()
                .read(
                    File(
                        projectDirectory,
                        POLICY_PATH
                    )
                )

        val actual =
            CanonicalCatalogExpansionSemanticValidator(
                policySet = policySet
            )
                .validate(candidates)

        assertTrue(
            actual.valid,
            "Semantic validation with external policy set must be valid."
        )

        assertEquals(
            expected.generatedCandidateCount,
            actual.generatedCandidateCount,
            "Generated candidate count changed after policy externalization."
        )

        assertEquals(
            expected.evaluatedCandidateCount,
            actual.evaluatedCandidateCount,
            "Evaluated candidate count changed after policy externalization."
        )

        assertEquals(
            expected.acceptedCandidateCount,
            actual.acceptedCandidateCount,
            "Accepted candidate count changed after policy externalization."
        )

        assertEquals(
            expected.rejectedCandidateCount,
            actual.rejectedCandidateCount,
            "Rejected candidate count changed after policy externalization."
        )

        assertEquals(
            expected.reviewRequiredCandidateCount,
            actual.reviewRequiredCandidateCount,
            "Review-required candidate count changed after policy externalization."
        )

        assertEquals(
            expected.decisionCounts,
            actual.decisionCounts,
            "Decision distribution changed after policy externalization."
        )

        assertEquals(
            expected.entries.map { entry ->
                entry.candidateIndex to
                        entry.decision
            },
            actual.entries.map { entry ->
                entry.candidateIndex to
                        entry.decision
            },
            "External policy migration must preserve every semantic decision."
        )

        assertEquals(
            expected.entries.map { entry ->
                entry.candidateIndex to
                        entry.findings
            },
            actual.entries.map { entry ->
                entry.candidateIndex to
                        entry.findings
            },
            "External policy migration must preserve every semantic finding."
        )

        assertTrue(
            actual.completeCandidateCoverage,
            "External policy validation must cover every generated candidate."
        )

        assertTrue(
            actual.deterministicOrderValid,
            "External policy validation order must remain deterministic."
        )
    }

    private fun resolveProjectDirectory(): File {
        val workingDirectory =
            File(
                requireNotNull(
                    System.getProperty("user.dir")
                ) {
                    "System property 'user.dir' is not available."
                }
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
                ) {
                    "Could not resolve project directory from app directory."
                }

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

        const val CURRENT_VALIDATION_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-expansion-semantic-validation.json"

        const val POLICY_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-family-axis-semantic-policies.json"
    }
}