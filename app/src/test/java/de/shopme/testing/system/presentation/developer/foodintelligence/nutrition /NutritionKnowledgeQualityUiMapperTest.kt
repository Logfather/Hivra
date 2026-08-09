package de.shopme.testing.system.presentation.developer.foodintelligence.nutrition

import de.shopme.presentation.developer.foodintelligence.nutrition.NutritionKnowledgeQualityUiMapper
import de.shopme.presentation.developer.foodintelligence.nutrition.NutritionKnowledgeQualityUiStatus
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.report.NutritionKnowledgeQualityReportFiles
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.report.NutritionKnowledgeQualityReportReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NutritionKnowledgeQualityUiMapperTest {

    @Test
    fun map_mapsProductiveNutritionQualityReportToUiModel() {

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
                        requireNotNull(
                            workingDirectory.parentFile
                        ) {
                            "App directory has no project parent."
                        }
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

        val uiModel =
            NutritionKnowledgeQualityUiMapper()
                .map(
                    report
                )

        assertEquals(
            NutritionKnowledgeQualityUiStatus.APPROVED,
            uiModel.overallStatus.status
        )

        assertEquals(
            "APPROVED",
            uiModel.overallStatus.label
        )

        assertEquals(
            NutritionKnowledgeQualityUiStatus.APPROVED,
            uiModel.validation.status
        )

        assertEquals(
            "Gültig",
            uiModel.validation.statusLabel
        )

        assertEquals(
            "512.102",
            uiModel.validation.entryCount
        )

        assertEquals(
            "0 Fehler · 0 Warnungen · 0 abgelehnte Einträge",
            uiModel.validation.summary
        )

        assertEquals(
            "94,89 %",
            uiModel.coverage.exactCoveragePercentage
        )

        assertEquals(
            "99,53 %",
            uiModel.coverage.effectiveCoveragePercentage
        )

        assertEquals(
            "511.016",
            uiModel.coverage.aggregateEntryCount
        )

        assertEquals(
            "512.102",
            uiModel.coverage.runtimeEntryCount
        )

        assertEquals(
            "484.885",
            uiModel.coverage.exactMatchCount
        )

        assertEquals(
            "23.734",
            uiModel.coverage
                .normalizationEquivalentMatchCount
        )

        assertEquals(
            "2.397",
            uiModel.coverage.trueMissingEntryCount
        )

        assertEquals(
            "3.483",
            uiModel.coverage.trueAdditionalEntryCount
        )

        assertEquals(
            "140",
            uiModel.coverage
                .normalizationCollisionGroupCount
        )

        assertFalse(
            uiModel.coverage.complete
        )

        assertEquals(
            NutritionKnowledgeQualityUiStatus.WARNING,
            uiModel.conflicts.status
        )

        assertEquals(
            "Konflikte innerhalb der Policy",
            uiModel.conflicts.statusLabel
        )

        assertEquals(
            "408",
            uiModel.conflicts.conflictEntryCount
        )

        assertEquals(
            "1.762",
            uiModel.conflicts.nutrientConflictCount
        )

        assertEquals(
            "0,080217 %",
            uiModel.conflicts.entryConflictPercentage
        )

        assertEquals(
            "0,047333 %",
            uiModel.conflicts.nutrientConflictPercentage
        )

        assertEquals(
            NutritionKnowledgeQualityUiStatus.APPROVED,
            uiModel.conflictPolicy.status
        )

        assertEquals(
            "APPROVED",
            uiModel.conflictPolicy.statusLabel
        )

        assertEquals(
            "0,100 %",
            uiModel.conflictPolicy
                .maximumEntryConflictPercentage
        )

        assertEquals(
            "0,050 %",
            uiModel.conflictPolicy
                .maximumNutrientConflictPercentage
        )

        assertEquals(
            "49",
            uiModel.conflictPolicy.extremeConflictCount
        )

        assertEquals(
            "50",
            uiModel.conflictPolicy
                .maximumExtremeConflictCount
        )

        assertFalse(
            uiModel.conflictPolicy
                .automaticCorrectionAllowed
        )

        assertFalse(
            uiModel.conflictPolicy
                .automaticEntryRejectionAllowed
        )

        assertEquals(
            "Keine automatischen Änderungen",
            uiModel.conflictPolicy
                .automaticCorrectionLabel
        )

        assertEquals(
            "Keine automatischen Löschungen",
            uiModel.conflictPolicy
                .automaticEntryRejectionLabel
        )

        assertEquals(
            NutritionKnowledgeQualityUiStatus.APPROVED,
            uiModel.offFreeze.status
        )

        assertEquals(
            "Eingefroren und reproduzierbar",
            uiModel.offFreeze.statusLabel
        )

        assertEquals(
            "open_food_facts",
            uiModel.offFreeze.source
        )

        assertEquals(
            "511.016",
            uiModel.offFreeze.aggregateEntryCount
        )

        assertEquals(
            "918,3 MB",
            uiModel.offFreeze.frozenFileSize
        )

        assertTrue(
            uiModel.offFreeze.checksumVerified
        )

        assertEquals(
            "SHA-256 verifiziert",
            uiModel.offFreeze.checksumLabel
        )

        assertTrue(
            uiModel.offFreeze.validationApproved
        )

        assertTrue(
            uiModel.offFreeze.conflictPolicyApproved
        )
    }
}