package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.quality.report

import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityRejectionReason
import de.shopme.tools.knowledge.off.nutrition.reference.quality.report.OFFNutritionReferenceQualityReport
import de.shopme.tools.knowledge.off.nutrition.reference.quality.report.OFFNutritionReferenceQualityReportWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OFFNutritionReferenceQualityReportWriterTest {

    @get:Rule
    val temporaryFolder =
        TemporaryFolder()

    @Test
    fun write_persistsDeterministicJsonReport() {

        val countsByReason =
            OFFNutritionReferenceQualityRejectionReason
                .entries
                .associateWith { reason ->
                    when (reason) {
                        OFFNutritionReferenceQualityRejectionReason
                            .NEGATIVE_NUTRITION_VALUE ->
                            2

                        OFFNutritionReferenceQualityRejectionReason
                            .NUTRITION_VALUE_ABOVE_MAXIMUM ->
                            1

                        else ->
                            0
                    }
                }
                .toSortedMap(
                    compareBy { reason ->
                        reason.name
                    }
                )

        val report =
            OFFNutritionReferenceQualityReport(
                version =
                    1,
                inputCandidateCount =
                    10,
                acceptedCandidateCount =
                    7,
                rejectedCandidateCount =
                    3,
                rejectionReasonOccurrenceCount =
                    3,
                acceptanceRate =
                    0.7,
                countsByReason =
                    countsByReason
            )

        val outputFile =
            temporaryFolder.root
                .resolve(
                    "reports/off-nutrition-reference-quality.json"
                )
                .canonicalFile

        val writer =
            OFFNutritionReferenceQualityReportWriter()

        val firstWriteResult =
            writer.write(
                report =
                    report,
                outputFile =
                    outputFile
            )

        val firstContent =
            outputFile.readText()

        val secondWriteResult =
            writer.write(
                report =
                    report,
                outputFile =
                    outputFile
            )

        val secondContent =
            outputFile.readText()

        assertEquals(
            outputFile,
            firstWriteResult.outputFile
        )

        assertEquals(
            outputFile,
            secondWriteResult.outputFile
        )

        assertTrue(
            outputFile.isFile
        )

        assertTrue(
            firstWriteResult.writtenByteCount > 0L
        )

        assertEquals(
            firstWriteResult.writtenByteCount,
            secondWriteResult.writtenByteCount
        )

        assertEquals(
            firstContent,
            secondContent
        )

        assertTrue(
            firstContent.contains(
                "\"inputCandidateCount\": 10"
            )
        )

        assertTrue(
            firstContent.contains(
                "\"acceptedCandidateCount\": 7"
            )
        )

        assertTrue(
            firstContent.contains(
                "\"rejectedCandidateCount\": 3"
            )
        )

        assertTrue(
            firstContent.contains(
                "\"acceptanceRate\": 0.7"
            )
        )

        assertTrue(
            firstContent.contains(
                "\"NEGATIVE_NUTRITION_VALUE\": 2"
            )
        )

        assertTrue(
            firstContent.contains(
                "\"NUTRITION_VALUE_ABOVE_MAXIMUM\": 1"
            )
        )
    }
}