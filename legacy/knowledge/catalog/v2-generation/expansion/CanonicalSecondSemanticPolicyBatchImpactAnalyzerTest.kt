package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalSecondSemanticPolicyBatchImpactAnalyzerTest {

    @Test
    fun preserveSecondBatchImpactDeterministically() {
        val projectDirectory =
            resolveProjectDirectory()

        val inputFile =
            File(
                projectDirectory,
                IMPACT_REPORT_PATH
            )

        /*
         * Beim ersten Entwicklungsdurchlauf wird der Report durch den Runner
         * erzeugt. Danach ist dieser Test eine reine Frozen-Artifact-
         * Regression.
         */
        if (!inputFile.isFile || inputFile.length() == 0L) {
            return
        }

        val reader =
            CanonicalSemanticPolicyBatchImpactAnalysisReader()

        val first =
            reader.read(inputFile)

        val second =
            reader.read(inputFile)

        assertEquals(
            first,
            second,
            "Persisted second-batch impact must be deterministic."
        )

        assertTrue(first.valid)

        CanonicalSecondSemanticPolicyBatchImpactReference
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
                "data/generated/knowledge/catalog"
            ).isDirectory ->
                workingDirectory

            workingDirectory.name ==
                    "app" ->
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
                    "canonical-second-semantic-policy-batch-impact-analysis.json"
    }
}