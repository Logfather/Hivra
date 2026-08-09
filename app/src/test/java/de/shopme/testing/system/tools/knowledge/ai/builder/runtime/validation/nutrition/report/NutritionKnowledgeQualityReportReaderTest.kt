package de.shopme.testing.system.tools.knowledge.ai.builder.runtime.validation.nutrition.report

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.report.NutritionKnowledgeQualityReportFiles
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.report.NutritionKnowledgeQualityReportReader
import de.shopme.tools.knowledge.off.nutrition.reference.freeze.OFFNutritionSourceSnapshotStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NutritionKnowledgeQualityReportReaderTest {

    @Test
    fun read_readsAndValidatesProductiveNutritionQualityReports() {

        val projectDirectory =
            File(
                requireNotNull(
                    System.getProperty(
                        "user.dir"
                    )
                ) {
                    "System property user.dir is unavailable."
                }
            )
                .let { workingDirectory ->
                    if (
                        workingDirectory.name ==
                        "app"
                    ) {
                        workingDirectory.parentFile
                    } else {
                        workingDirectory
                    }
                }

        val report =
            NutritionKnowledgeQualityReportReader()
                .read(
                    NutritionKnowledgeQualityReportFiles
                        .productive(
                            projectDirectory
                        )
                )

        assertTrue(
            report.approved
        )

        assertTrue(
            report.validation.valid
        )

        assertEquals(
            512_102L,
            report.validation.entryCount
        )

        assertEquals(
            0L,
            report.validation.warningCount
        )

        assertEquals(
            0L,
            report.validation.errorCount
        )

        assertEquals(
            0L,
            report.validation.rejectedEntryCount
        )

        assertEquals(
            511_016L,
            report.coverage.aggregateEntryCount
        )

        assertEquals(
            512_102L,
            report.coverage.runtimeEntryCount
        )

        assertEquals(
            94.88646148065814,
            report.exactCoveragePercentage,
            1e-12
        )

        assertEquals(
            99.53093445215022,
            report.effectiveCoveragePercentage,
            1e-12
        )

        assertEquals(
            2_397L,
            report.coverageGapClassification
                .trueMissingRuntimeEntryCount
        )

        assertEquals(
            3_483L,
            report.coverageGapClassification
                .trueAdditionalRuntimeEntryCount
        )

        assertEquals(
            140L,
            report.coverageGapClassification
                .normalizationCollisionGroupCount
        )

        assertFalse(
            report.coverageGapClassification.complete
        )

        assertEquals(
            408L,
            report.conflicts.conflictEntryCount
        )

        assertEquals(
            1_762L,
            report.conflicts.nutrientConflictCount
        )

        assertEquals(
            0.08021721563685194,
            report.entryConflictPercentage,
            1e-12
        )

        assertEquals(
            0.04733250953634557,
            report.nutrientConflictPercentage,
            1e-12
        )

        assertTrue(
            report.conflictPolicy.decision.approved
        )

        assertEquals(
            49L,
            report.conflictPolicy.decision
                .extremeConflictCount
        )

        assertEquals(
            50L,
            report.conflictPolicy.decision
                .maximumExtremeConflictCount
        )

        assertFalse(
            report.conflictPolicy.policy
                .automaticCorrectionAllowed
        )

        assertFalse(
            report.conflictPolicy.policy
                .automaticEntryRejectionAllowed
        )

        assertEquals(
            OFFNutritionSourceSnapshotStatus.APPROVED,
            report.offNutritionSourceSnapshot.status
        )

        assertEquals(
            511_016L,
            report.offNutritionSourceSnapshot
                .aggregateEntryCount
        )

        assertEquals(
            918_329_581L,
            report.offNutritionSourceSnapshot
                .frozenFileSizeBytes
        )

        assertEquals(
            report.offNutritionSourceSnapshot.sourceSha256,
            report.offNutritionSourceSnapshot.frozenSha256
        )
    }
}