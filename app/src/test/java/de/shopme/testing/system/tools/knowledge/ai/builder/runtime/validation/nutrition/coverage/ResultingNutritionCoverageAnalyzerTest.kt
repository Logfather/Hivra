package de.shopme.testing.system.tools.knowledge.ai.builder.runtime.validation.nutrition.coverage

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.coverage.ResultingNutritionCoverageAnalyzer
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ResultingNutritionCoverageAnalyzerTest {

    @Test
    fun analyze_measuresCoveredMissingAndAdditionalEntries() {

        val directory =
            Files.createTempDirectory(
                "nutrition-coverage"
            )
                .toFile()

        try {
            val aggregateFile =
                directory.resolve(
                    "off-nutrition-reference-aggregates.json"
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
                      "energyKcalPer100g": 52.0
                    }
                  },
                  {
                    "canonicalId": "banana",
                    "nutrition": {
                      "energyKcalPer100g": 89.0
                    }
                  },
                  {
                    "canonicalId": "pear",
                    "nutrition": {
                      "energyKcalPer100g": 57.0
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
                      "calories": 52.0
                    },
                    "banana": {
                      "calories": 89.0
                    },
                    "ciqual-only": {
                      "calories": 100.0
                    }
                  }
                }
                """.trimIndent()
            )

            val result =
                ResultingNutritionCoverageAnalyzer()
                    .analyze(
                        aggregateFile =
                            aggregateFile,
                        runtimeFile =
                            runtimeFile
                    )

            assertEquals(
                3L,
                result.aggregateEntryCount
            )

            assertEquals(
                3L,
                result.runtimeEntryCount
            )

            assertEquals(
                2L,
                result.coveredAggregateEntryCount
            )

            assertEquals(
                1L,
                result.missingRuntimeEntryCount
            )

            assertEquals(
                1L,
                result.additionalRuntimeEntryCount
            )

            assertEquals(
                2.0 / 3.0,
                result.aggregateCoverageRate
            )

            assertEquals(
                listOf(
                    "pear"
                ),
                result.missingRuntimeCanonicalIds
            )

            assertEquals(
                listOf(
                    "ciqual-only"
                ),
                result.additionalRuntimeCanonicalIds
            )

            assertFalse(
                result.isComplete
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun analyze_limitsExamplesButKeepsCompleteCounts() {

        val directory =
            Files.createTempDirectory(
                "nutrition-coverage-limited"
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
                  {"canonicalId": "a"},
                  {"canonicalId": "b"},
                  {"canonicalId": "c"}
                ]
                """.trimIndent()
            )

            runtimeFile.writeText(
                """
                {
                  "entries": {
                    "x": {},
                    "y": {},
                    "z": {}
                  }
                }
                """.trimIndent()
            )

            val result =
                ResultingNutritionCoverageAnalyzer(
                    maximumReportedMissingRuntimeCanonicalIds =
                        1,
                    maximumReportedAdditionalRuntimeCanonicalIds =
                        1
                )
                    .analyze(
                        aggregateFile =
                            aggregateFile,
                        runtimeFile =
                            runtimeFile
                    )

            assertEquals(
                3L,
                result.missingRuntimeEntryCount
            )

            assertEquals(
                3L,
                result.additionalRuntimeEntryCount
            )

            assertEquals(
                listOf(
                    "a"
                ),
                result.missingRuntimeCanonicalIds
            )

            assertEquals(
                listOf(
                    "x"
                ),
                result.additionalRuntimeCanonicalIds
            )

            assertEquals(
                2L,
                result.omittedMissingRuntimeCanonicalIdCount
            )

            assertEquals(
                2L,
                result.omittedAdditionalRuntimeCanonicalIdCount
            )
        } finally {
            directory.deleteRecursively()
        }
    }
}