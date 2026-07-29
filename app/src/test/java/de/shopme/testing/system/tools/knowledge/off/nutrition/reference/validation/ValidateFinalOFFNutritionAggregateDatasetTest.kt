package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.validation

import de.shopme.tools.knowledge.off.nutrition.reference.validation.OFFNutritionReferenceAggregateDatasetValidationReportWriter
import de.shopme.tools.knowledge.off.nutrition.reference.validation.OFFNutritionReferenceAggregateDatasetValidator
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidateFinalOFFNutritionAggregateDatasetTest {

    @Test
    fun validateFinalOFFNutritionAggregateDataset() {

        val repositoryRoot =
            resolveRepositoryRoot()

        val aggregateDatasetFile =
            repositoryRoot.resolve(
                "data/generated/knowledge/references/off/" +
                        "off-nutrition-reference-aggregates.json"
            )

        val validationReportFile =
            repositoryRoot.resolve(
                "data/generated/knowledge/reports/" +
                        "off-nutrition-reference-aggregate-validation.json"
            )

        val result =
            OFFNutritionReferenceAggregateDatasetValidator()
                .validate(
                    inputFile =
                        aggregateDatasetFile
                )

        val writtenReportFile =
            OFFNutritionReferenceAggregateDatasetValidationReportWriter()
                .write(
                    result =
                        result,
                    outputFile =
                        validationReportFile
                )

        assertTrue(
            result.inputAggregateCount > 0
        )

        assertEquals(
            result.inputAggregateCount,
            result.acceptedAggregateCount +
                    result.warningAggregateCount +
                    result.rejectedAggregateCount
        )

        assertTrue(
            result.totalProfileCount > 0L
        )

        assertTrue(
            writtenReportFile.isFile
        )

        assertTrue(
            writtenReportFile.length() > 0L
        )

        assertTrue(
            actual =
                result.valid,
            message =
                "Final OFF nutrition aggregate dataset contains " +
                        "${result.rejectedAggregateCount} rejected aggregates " +
                        "and ${result.totalErrorCount} errors. " +
                        "See ${writtenReportFile.absolutePath}"
        )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("FINAL OFF NUTRITION AGGREGATE DATASET VALIDATION")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "inputAggregateCount=" +
                    result.inputAggregateCount
        )
        println(
            "acceptedAggregateCount=" +
                    result.acceptedAggregateCount
        )
        println(
            "warningAggregateCount=" +
                    result.warningAggregateCount
        )
        println(
            "rejectedAggregateCount=" +
                    result.rejectedAggregateCount
        )
        println(
            "totalProfileCount=" +
                    result.totalProfileCount
        )
        println(
            "totalWarningCount=" +
                    result.totalWarningCount
        )
        println(
            "totalErrorCount=" +
                    result.totalErrorCount
        )
        println(
            "issueCountsByType=" +
                    result.issueCountsByType
        )
        println(
            "validationReportFile=" +
                    writtenReportFile.absolutePath
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    private fun resolveRepositoryRoot(): File {

        val workingDirectory =
            File(".").canonicalFile

        return if (workingDirectory.name == "app") {
            requireNotNull(
                workingDirectory.parentFile
            ).canonicalFile
        } else {
            workingDirectory
        }
    }
}