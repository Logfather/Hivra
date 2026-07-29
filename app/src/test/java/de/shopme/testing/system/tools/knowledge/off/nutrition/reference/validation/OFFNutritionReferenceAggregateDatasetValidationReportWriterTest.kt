package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.validation

import de.shopme.tools.knowledge.off.nutrition.reference.validation.OFFNutritionReferenceAggregateDatasetValidationReportWriter
import de.shopme.tools.knowledge.off.nutrition.reference.validation.OFFNutritionReferenceAggregateDatasetValidationResult
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OFFNutritionReferenceAggregateDatasetValidationReportWriterTest {

    @Test
    fun write_persistsDeterministicDatasetValidationReport() {

        val outputFile =
            Files
                .createTempDirectory(
                    "off-nutrition-dataset-validation-report"
                )
                .resolve(
                    "validation-report.json"
                )
                .toFile()

        val result =
            OFFNutritionReferenceAggregateDatasetValidationResult(
                inputAggregateCount =
                    1,
                acceptedAggregateCount =
                    1,
                warningAggregateCount =
                    0,
                rejectedAggregateCount =
                    0,
                totalProfileCount =
                    1L,
                totalWarningCount =
                    0,
                totalErrorCount =
                    0,
                issueCountsByType =
                    sortedMapOf(),
                warningEntries =
                    emptyList(),
                rejectedEntries =
                    emptyList()
            )

        val writer =
            OFFNutritionReferenceAggregateDatasetValidationReportWriter()

        writer.write(
            result =
                result,
            outputFile =
                outputFile
        )

        val firstContent =
            outputFile.readText()

        writer.write(
            result =
                result,
            outputFile =
                outputFile
        )

        val secondContent =
            outputFile.readText()

        assertEquals(
            firstContent,
            secondContent
        )

        assertTrue(
            firstContent.contains(
                "\"valid\": true"
            )
        )

        assertTrue(
            firstContent.contains(
                "\"inputAggregateCount\": 1"
            )
        )

        assertTrue(
            firstContent.contains(
                "\"totalProfileCount\": 1"
            )
        )
    }
}