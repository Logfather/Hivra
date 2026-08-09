package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate
.CanonicalCatalogExpansionCandidateReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalCatalogExpansionSemanticValidationReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalCatalogExpansionSemanticValidationWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalCatalogExpansionSemanticValidator
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySetReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalCatalogExpansionSemanticValidationTest {

    @Test
    fun validateCanonicalExpansionCandidateSemantics() {
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
            CanonicalFamilyAxisSemanticPolicySetReader()
                .read(
                    File(
                        projectDirectory,
                        SEMANTIC_POLICY_PATH
                    )
                )

        val result =
            CanonicalCatalogExpansionSemanticValidator(
                policySet = policySet
            )
                .validate(candidates)

        assertTrue(
            result.valid,
            "Expansion semantic validation is invalid: " +
                    result.blockers.joinToString()
        )

        val outputFile =
            File(
                projectDirectory,
                OUTPUT_PATH
            )

        CanonicalCatalogExpansionSemanticValidationWriter()
            .write(
                result = result,
                outputFile = outputFile
            )

        val persisted =
            CanonicalCatalogExpansionSemanticValidationReader()
                .read(outputFile)

        assertEquals(result, persisted)

        assertEquals(
            9_304,
            persisted.generatedCandidateCount
        )

        assertEquals(
            persisted.generatedCandidateCount,
            persisted.evaluatedCandidateCount
        )

        assertEquals(
            persisted.evaluatedCandidateCount,
            persisted.acceptedCandidateCount +
                    persisted.rejectedCandidateCount +
                    persisted.reviewRequiredCandidateCount
        )

        assertTrue(
            persisted.completeCandidateCoverage
        )

        assertTrue(
            persisted.deterministicOrderValid
        )

        println(
            buildString {
                appendLine(
                    "Canonical expansion candidate semantic validation"
                )
                appendLine(
                    "-------------------------------------------------"
                )
                appendLine(
                    "Generated candidates: " +
                            persisted.generatedCandidateCount
                )
                appendLine(
                    "Evaluated candidates: " +
                            persisted.evaluatedCandidateCount
                )
                appendLine(
                    "Accepted candidates: " +
                            persisted.acceptedCandidateCount
                )
                appendLine(
                    "Rejected candidates: " +
                            persisted.rejectedCandidateCount
                )
                appendLine(
                    "Review required: " +
                            persisted.reviewRequiredCandidateCount
                )
                appendLine(
                    "Acceptance share: " +
                            persisted.acceptanceShare
                )
                appendLine(
                    "Rejection share: " +
                            persisted.rejectionShare
                )
                appendLine(
                    "Review-required share: " +
                            persisted.reviewRequiredShare
                )
                appendLine(
                    "Categories: " +
                            persisted.categoryCount
                )
                appendLine(
                    "Complete candidate coverage: " +
                            persisted.completeCandidateCoverage
                )
                appendLine(
                    "Deterministic order valid: " +
                            persisted.deterministicOrderValid
                )
                append(
                    "Validation valid: " +
                            persisted.valid
                )
            }
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

    private companion object {
        const val CANDIDATE_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-expansion-candidates.json"

        const val OUTPUT_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-expansion-semantic-validation.json"

        const val SEMANTIC_POLICY_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-family-axis-semantic-policies.json"
    }
}