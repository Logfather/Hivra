package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalFirstGeneralizedSemanticPolicyWaveImpactAnalyzerTest {

    @Test
    fun preserveFirstGeneralizedWaveImpactDeterministically() {
        val projectDirectory =
            resolveProjectDirectory()

        val reportFile =
            File(
                projectDirectory,
                IMPACT_REPORT_PATH
            )

        require(reportFile.isFile) {
            "Frozen generalized-wave impact report is missing: " +
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
            second
        )

        assertTrue(first.valid)

        CanonicalFirstGeneralizedSemanticPolicyWaveImpactReference
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
                    "canonical-first-generalized-wave-impact-analysis.json"
    }
}