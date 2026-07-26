package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.deduplication

import de.shopme.tools.knowledge.off.nutrition.reference.deduplication.OFFNutritionReferenceDeduplicationResult
import de.shopme.tools.knowledge.off.nutrition.reference.deduplication.OFFNutritionReferenceDuplicateGroup
import de.shopme.tools.knowledge.off.nutrition.reference.deduplication.OFFNutritionReferenceDuplicateReportWriter
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OFFNutritionReferenceDuplicateReportWriterTest {

    @Test
    fun write_persistsDeterministicDuplicateReport() {

        val outputFile =
            Files
                .createTempDirectory(
                    "off-nutrition-duplicate-report"
                )
                .resolve(
                    "duplicate-report.json"
                )
                .toFile()

        val group =
            OFFNutritionReferenceDuplicateGroup(
                canonicalId =
                    "plain yogurt",
                nutritionFingerprint =
                    "fatPer100g=3.3|proteinsPer100g=3.5",
                representativeSourceId =
                    "111",
                mergedSourceIds =
                    listOf(
                        "111",
                        "222"
                    )
            )

        val result =
            OFFNutritionReferenceDeduplicationResult(
                inputCandidateCount =
                    2,
                outputCandidateCount =
                    1,
                removedDuplicateCount =
                    1,
                duplicateGroupCount =
                    1,
                candidates =
                    listOf(
                        TestCandidateFactory.create(
                            sourceId =
                                "111",
                            canonicalId =
                                "plain yogurt"
                        )
                    ),
                duplicateGroups =
                    listOf(group)
            )

        OFFNutritionReferenceDuplicateReportWriter()
            .write(
                result =
                    result,
                outputFile =
                    outputFile
            )

        assertTrue(outputFile.isFile)

        val firstContent =
            outputFile.readText()

        OFFNutritionReferenceDuplicateReportWriter()
            .write(
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
                "\"removedDuplicateCount\": 1"
            )
        )

        assertTrue(
            firstContent.contains(
                "\"representativeSourceId\": \"111\""
            )
        )
    }
}