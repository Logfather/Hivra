package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalBoundedSemanticPolicyWaveFourImpactAnalyzerTest {

    @Test
    fun preserveBoundedWaveFourImpactDeterministically() {
        val projectDirectory =
            resolveProjectDirectory()

        val reportFile =
            File(
                projectDirectory,
                IMPACT_REPORT_PATH
            )

        require(reportFile.isFile) {
            "Frozen bounded Wave-4 impact report is missing: " +
                    reportFile.absolutePath
        }

        require(reportFile.length() > 0L) {
            "Frozen bounded Wave-4 impact report is empty: " +
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
            "Persisted bounded Wave-4 impact must be deterministic."
        )

        assertTrue(first.valid)

        assertEquals(
            CanonicalBoundedSemanticPolicyWaveFourImpactBaselineFactory
                .WAVE_KEY,
            first.batchKey
        )

        assertEquals(
            525,
            first.closedPolicyGapCount
        )

        assertEquals(
            525,
            first.policyCountDelta
        )

        assertEquals(
            525,
            first.curatedPolicyCountDelta +
                    first.notApplicablePolicyCountDelta
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
                IMPACT_REPORT_PATH
            ).isFile ->
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
                    "canonical-bounded-wave-004-impact-analysis.json"
    }
}