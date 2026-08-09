package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalFirstSemanticPolicyBatchImpactAnalyzerTest {

    @Test
    fun preserveFirstBatchImpactDeterministically() {
        val projectDirectory =
            resolveProjectDirectory()

        val inputFile =
            File(
                projectDirectory,
                IMPACT_REPORT_PATH
            )

        val reader =
            CanonicalSemanticPolicyBatchImpactAnalysisReader()

        val first =
            reader.read(inputFile)

        val second =
            reader.read(inputFile)

        assertEquals(
            first,
            second,
            "Persisted first-batch impact must be deterministic."
        )

        assertTrue(first.valid)

        CanonicalFirstSemanticPolicyBatchImpactReference
            .validate(
                analysis =
                    first
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
                SEMANTIC_VALIDATION_PATH
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

        const val IMPACT_REPORT_PATH =
            "data/generated/knowledge/catalog/expansion/impact/" +
                    "canonical-first-semantic-policy-batch-impact-analysis.json"

        const val SEMANTIC_VALIDATION_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-expansion-semantic-validation.json"
    }
}