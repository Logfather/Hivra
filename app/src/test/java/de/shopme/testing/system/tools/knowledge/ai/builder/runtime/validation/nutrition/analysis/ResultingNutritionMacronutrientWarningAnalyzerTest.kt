package de.shopme.testing.system.tools.knowledge.ai.builder.runtime.validation.nutrition.analysis

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.analysis.MacronutrientWarningClassification
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.analysis.MacronutrientWarningExcessBucket
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.analysis.ResultingNutritionMacronutrientWarningAnalyzer
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class ResultingNutritionMacronutrientWarningAnalyzerTest {

    @Test
    fun analyze_classifiesMacronutrientWarnings() {

        val directory =
            Files.createTempDirectory(
                "macronutrient-warning-analysis"
            )
                .toFile()

        try {
            val inputFile =
                directory.resolve(
                    "nutrition.json"
                )

            inputFile.writeText(
                """
                {
                  "entries": {
                    "valid": {
                      "fat": 10.0,
                      "carbohydrates": 60.0,
                      "fiber": 5.0,
                      "protein": 20.0,
                      "presentNutrients": [
                        "fat",
                        "carbohydrates",
                        "fiber",
                        "protein"
                      ]
                    },
                    "fiber-double-counting": {
                      "fat": 10.0,
                      "carbohydrates": 70.0,
                      "fiber": 15.0,
                      "protein": 15.0,
                      "presentNutrients": [
                        "fat",
                        "carbohydrates",
                        "fiber",
                        "protein"
                      ]
                    },
                    "core-excess": {
                      "fat": 40.0,
                      "carbohydrates": 60.0,
                      "fiber": 5.0,
                      "protein": 20.0,
                      "presentNutrients": [
                        "fat",
                        "carbohydrates",
                        "fiber",
                        "protein"
                      ]
                    },
                    "insufficient-values": {
                      "fat": 60.0,
                      "carbohydrates": 60.0,
                      "fiber": 0.0,
                      "protein": 0.0,
                      "presentNutrients": [
                        "fat",
                        "carbohydrates"
                      ]
                    }
                  }
                }
                """.trimIndent()
            )

            val result =
                ResultingNutritionMacronutrientWarningAnalyzer()
                    .analyze(
                        inputFile =
                            inputFile
                    )

            assertEquals(
                4L,
                result.entryCount
            )

            assertEquals(
                2L,
                result.warningEntryCount
            )

            assertEquals(
                1L,
                result.fiberDoubleCountingCandidateCount
            )

            assertEquals(
                1L,
                result.coreMacronutrientExcessCount
            )

            assertEquals(
                1L,
                result.countsByClassification[
                    MacronutrientWarningClassification
                        .FIBER_DOUBLE_COUNTING_CANDIDATE
                ]
            )

            assertEquals(
                1L,
                result.countsByClassification[
                    MacronutrientWarningClassification
                        .CORE_MACRONUTRIENT_SUM_EXCEEDS_MAXIMUM
                ]
            )

            assertEquals(
                1L,
                result.countsByExcessBucket[
                    MacronutrientWarningExcessBucket
                        .UP_TO_5_GRAMS
                ]
            )

            assertEquals(
                1L,
                result.countsByExcessBucket[
                    MacronutrientWarningExcessBucket
                        .UP_TO_20_GRAMS
                ]
            )

            assertEquals(
                2,
                result.examples.size
            )

            assertEquals(
                "core-excess",
                result.examples.first().canonicalId
            )

            assertEquals(
                125.0,
                result.maximumMacronutrientSum
            )

            assertEquals(
                20.0,
                result.maximumExcessGrams
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun analyze_limitsExamplesButKeepsCompleteCounts() {

        val directory =
            Files.createTempDirectory(
                "macronutrient-warning-analysis-limit"
            )
                .toFile()

        try {
            val inputFile =
                directory.resolve(
                    "nutrition.json"
                )

            inputFile.writeText(
                """
                {
                  "entries": {
                    "a": {
                      "fat": 40.0,
                      "carbohydrates": 60.0,
                      "protein": 10.0
                    },
                    "b": {
                      "fat": 50.0,
                      "carbohydrates": 60.0,
                      "protein": 10.0
                    },
                    "c": {
                      "fat": 60.0,
                      "carbohydrates": 60.0,
                      "protein": 10.0
                    }
                  }
                }
                """.trimIndent()
            )

            val result =
                ResultingNutritionMacronutrientWarningAnalyzer(
                    maximumReportedExamples =
                        1
                )
                    .analyze(
                        inputFile =
                            inputFile
                    )

            assertEquals(
                3L,
                result.warningEntryCount
            )

            assertEquals(
                3L,
                result.coreMacronutrientExcessCount
            )

            assertEquals(
                1,
                result.examples.size
            )

            assertEquals(
                "c",
                result.examples.single().canonicalId
            )

            assertEquals(
                130.0,
                result.examples.single().macronutrientSum
            )
        } finally {
            directory.deleteRecursively()
        }
    }
}