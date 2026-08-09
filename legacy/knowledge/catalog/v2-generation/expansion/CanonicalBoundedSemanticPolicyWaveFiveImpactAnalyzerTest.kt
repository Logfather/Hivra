package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalBoundedSemanticPolicyWaveFiveImpactAnalyzerTest {

    @Test
    fun preserveBoundedWaveFiveImpactDeterministically() {
        val projectDirectory =
            resolveProjectDirectory()

        val reportFile =
            File(
                projectDirectory,
                IMPACT_REPORT_PATH
            )

        require(reportFile.isFile) {
            "Frozen bounded Wave-5 impact report is missing: " +
                    reportFile.absolutePath
        }

        require(reportFile.length() > 0L) {
            "Frozen bounded Wave-5 impact report is empty: " +
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
            "Persisted bounded Wave-5 impact must be deterministic."
        )

        assertTrue(first.valid)

        assertEquals(
            CanonicalBoundedSemanticPolicyWaveFiveImpactBaselineFactory
                .WAVE_KEY,
            first.batchKey
        )

        assertTrue(
            first.closedPolicyGapCount >=
                    EXPECTED_PERSISTED_WAVE_POLICY_COUNT
        )

        assertTrue(
            first.policyCountDelta >=
                    EXPECTED_PERSISTED_WAVE_POLICY_COUNT
        )

        assertEquals(
            EXPECTED_PERSISTED_WAVE_POLICY_COUNT,
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

        const val EXPECTED_PERSISTED_WAVE_POLICY_COUNT =
            505

        const val IMPACT_REPORT_PATH =
            "data/generated/knowledge/catalog/expansion/impact/" +
                    "canonical-bounded-wave-005-impact-analysis.json"
    }
}