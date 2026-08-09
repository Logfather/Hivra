package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalThirdGeneralizedSemanticPolicyWaveImpactAnalyzerTest {

    @Test
    fun preserveThirdGeneralizedWaveImpactDeterministically() {
        val projectDirectory =
            resolveProjectDirectory()

        val reportFile =
            File(
                projectDirectory,
                IMPACT_REPORT_PATH
            )

        require(reportFile.isFile) {
            "Frozen generalized Wave-3 impact report is missing: " +
                    reportFile.absolutePath
        }

        require(reportFile.length() > 0L) {
            "Frozen generalized Wave-3 impact report is empty: " +
                    reportFile.absolutePath
        }

        val reader =
            CanonicalSemanticPolicyBatchImpactAnalysisReader()

        val first =
            reader.read(reportFile)

        val second =
            reader.read(reportFile)

        assertEquals(
            first,
            second,
            "Persisted Wave-3 impact report must be deterministic."
        )

        assertTrue(first.valid)

        CanonicalThirdGeneralizedSemanticPolicyWaveImpactReference
            .validate(first)
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
                    "canonical-third-generalized-wave-impact-analysis.json"
    }
}