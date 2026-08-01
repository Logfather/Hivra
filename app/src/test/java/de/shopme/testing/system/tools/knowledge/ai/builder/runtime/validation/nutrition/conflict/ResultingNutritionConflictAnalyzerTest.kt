package de.shopme.testing.system.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionConflictAnalyzer
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionConflictNutrient
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class ResultingNutritionConflictAnalyzerTest {

    @Test
    fun analyze_measuresExactNormalizedAndNutrientConflicts() {

        val directory =
            Files.createTempDirectory(
                "nutrition-conflict-analysis"
            )
                .toFile()

        try {
            val aggregateFile =
                directory.resolve(
                    "aggregates.json"
                )

            val runtimeFile =
                directory.resolve(
                    "nutrition.json"
                )

            aggregateFile.writeText(
                """
                [
                  {
                    "canonicalId": "apple",
                    "nutrition": {
                      "energyKcalPer100g": 52.0,
                      "fatPer100g": 0.2
                    }
                  },
                  {
                    "canonicalId": "fat-free milk",
                    "nutrition": {
                      "energyKcalPer100g": 40.0,
                      "fatPer100g": 0.1
                    }
                  },
                  {
                    "canonicalId": "pear",
                    "nutrition": {
                      "energyKcalPer100g": 57.0,
                      "sugarsPer100g": 10.0
                    }
                  }
                ]
                """.trimIndent()
            )

            runtimeFile.writeText(
                """
                {
                  "entries": {
                    "apple": {
                      "calories": 52.0,
                      "fat": 0.2,
                      "presentNutrients": [
                        "calories",
                        "fat"
                      ]
                    },
                    "fat free milk": {
                      "calories": 50.0,
                      "fat": 0.1,
                      "presentNutrients": [
                        "calories",
                        "fat"
                      ]
                    },
                    "pear": {
                      "calories": 57.0,
                      "sugar": 12.0,
                      "presentNutrients": [
                        "calories",
                        "sugar"
                      ]
                    }
                  }
                }
                """.trimIndent()
            )

            val result =
                ResultingNutritionConflictAnalyzer()
                    .analyze(
                        aggregateFile =
                            aggregateFile,
                        runtimeFile =
                            runtimeFile
                    )

            assertEquals(
                2L,
                result.exactMatchedEntryCount
            )

            assertEquals(
                1L,
                result.normalizationEquivalentMatchedEntryCount
            )

            assertEquals(
                3L,
                result.matchedEntryCount
            )

            assertEquals(
                3L,
                result.comparableEntryCount
            )

            assertEquals(
                2L,
                result.conflictEntryCount
            )

            assertEquals(
                1L,
                result.conflictFreeEntryCount
            )

            assertEquals(
                6L,
                result.nutrientComparisonCount
            )

            assertEquals(
                2L,
                result.nutrientConflictCount
            )

            assertEquals(
                1L,
                result.conflictCountsByNutrient[
                    ResultingNutritionConflictNutrient.ENERGY
                ]
            )

            assertEquals(
                1L,
                result.conflictCountsByNutrient[
                    ResultingNutritionConflictNutrient.SUGARS
                ]
            )

            assertEquals(
                2.0 / 3.0,
                result.entryConflictRate
            )

            assertEquals(
                2.0 / 6.0,
                result.nutrientConflictRate
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun analyze_doesNotTreatMissingRuntimeNutrientsAsConflicts() {

        val directory =
            Files.createTempDirectory(
                "nutrition-conflict-missing-values"
            )
                .toFile()

        try {
            val aggregateFile =
                directory.resolve(
                    "aggregates.json"
                )

            val runtimeFile =
                directory.resolve(
                    "nutrition.json"
                )

            aggregateFile.writeText(
                """
                [
                  {
                    "canonicalId": "apple",
                    "nutrition": {
                      "energyKcalPer100g": 52.0,
                      "fatPer100g": 0.2
                    }
                  }
                ]
                """.trimIndent()
            )

            runtimeFile.writeText(
                """
                {
                  "entries": {
                    "apple": {
                      "calories": 52.0,
                      "fat": 0.0,
                      "presentNutrients": [
                        "calories"
                      ]
                    }
                  }
                }
                """.trimIndent()
            )

            val result =
                ResultingNutritionConflictAnalyzer()
                    .analyze(
                        aggregateFile =
                            aggregateFile,
                        runtimeFile =
                            runtimeFile
                    )

            assertEquals(
                1L,
                result.nutrientComparisonCount
            )

            assertEquals(
                0L,
                result.nutrientConflictCount
            )

            assertEquals(
                0L,
                result.conflictEntryCount
            )

            assertEquals(
                1L,
                result.conflictFreeEntryCount
            )
        } finally {
            directory.deleteRecursively()
        }
    }
}