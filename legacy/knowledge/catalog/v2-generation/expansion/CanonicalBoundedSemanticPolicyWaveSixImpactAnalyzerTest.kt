package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalBoundedSemanticPolicyWaveSixImpactAnalyzerTest {

    @Test
    fun preserveBoundedWaveSixImpactDeterministically() {
        val projectDirectory =
            resolveProjectDirectory()

        val reportFile =
            File(
                projectDirectory,
                IMPACT_REPORT_PATH
            )

        require(reportFile.isFile) {
            "Frozen bounded Wave-6 impact report is missing: " +
                    reportFile.absolutePath
        }

        require(reportFile.length() > 0L) {
            "Frozen bounded Wave-6 impact report is empty: " +
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
            "Persisted bounded Wave-6 impact must be deterministic."
        )

        assertTrue(first.valid)

        assertEquals(
            CanonicalBoundedSemanticPolicyWaveSixImpactAnalyzer
                .WAVE_SIX_KEY,
            first.batchKey
        )

        assertEquals(
            EXPECTED_REMAINING_POLICY_GAP_COUNT_BEFORE_WAVE_SIX,
            first.missingPolicyGapsBefore
        )

        assertEquals(
            0,
            first.missingPolicyGapsAfter
        )

        assertEquals(
            EXPECTED_REMAINING_POLICY_GAP_COUNT_BEFORE_WAVE_SIX,
            first.closedPolicyGapCount
        )

        assertEquals(
            EXPECTED_REMAINING_POLICY_GAP_COUNT_BEFORE_WAVE_SIX,
            first.policyCountDelta
        )

        assertTrue(first.semanticDecisionArithmeticValid)
        assertTrue(first.reviewBacklogReduced)
        assertTrue(first.expectedPolicyGapClosureReached)
        assertTrue(first.policyExpansionValid)
        assertTrue(first.catalogExpansionArithmeticValid)
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
                    "app" &&
                    File(
                        requireNotNull(
                            workingDirectory.parentFile
                        ),
                        IMPACT_REPORT_PATH
                    ).isFile ->
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

        const val EXPECTED_REMAINING_POLICY_GAP_COUNT_BEFORE_WAVE_SIX =
            40

        const val IMPACT_REPORT_PATH =
            "data/generated/knowledge/catalog/expansion/impact/" +
                    "canonical-bounded-wave-006-impact-analysis.json"
    }
}