package de.shopme.testing.system.tools.knowledge.ai.builder.runtime.validation.nutrition

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.ResultingNutritionKnowledgeValidationReason
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.ResultingNutritionKnowledgeValidator
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResultingNutritionKnowledgeValidatorTest {

    @Test
    fun validate_acceptsValidNutritionArtifact() {

        val directory =
            Files.createTempDirectory(
                "nutrition-result-validation"
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
                    "apple": {
                      "calories": 52.0,
                      "protein": 0.3,
                      "fat": 0.2,
                      "saturatedFat": 0.0,
                      "carbohydrates": 14.0,
                      "sugar": 10.0,
                      "fiber": 2.4,
                      "salt": 0.0
                    },
                    "banana": {
                      "calories": 89.0,
                      "protein": 1.1,
                      "fat": 0.3,
                      "saturatedFat": 0.1,
                      "carbohydrates": 23.0,
                      "sugar": 12.0,
                      "fiber": 2.6,
                      "salt": 0.0
                    }
                  }
                }
                """.trimIndent()
            )

            val result =
                ResultingNutritionKnowledgeValidator()
                    .validate(
                        inputFile =
                            inputFile
                    )

            assertTrue(
                result.isValid
            )

            assertEquals(
                2L,
                result.entryCount
            )

            assertEquals(
                2L,
                result.validEntryCount
            )

            assertEquals(
                0L,
                result.warningEntryCount
            )

            assertEquals(
                0L,
                result.rejectedEntryCount
            )

            assertEquals(
                0L,
                result.warningCount
            )

            assertEquals(
                0L,
                result.errorCount
            )

            assertTrue(
                result.countsByReason.isEmpty()
            )

            assertTrue(
                result.warningIssues.isEmpty()
            )

            assertTrue(
                result.errorIssues.isEmpty()
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun validate_rejectsInvalidNutritionRelationships() {

        val directory =
            Files.createTempDirectory(
                "nutrition-result-invalid"
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
                    "invalid": {
                      "calories": 1200.0,
                      "protein": 4.0,
                      "fat": 5.0,
                      "saturatedFat": 8.0,
                      "carbohydrates": 10.0,
                      "sugar": 20.0,
                      "fiber": 0.0,
                      "salt": 0.0
                    }
                  }
                }
                """.trimIndent()
            )

            val result =
                ResultingNutritionKnowledgeValidator()
                    .validate(
                        inputFile =
                            inputFile
                    )

            assertFalse(
                result.isValid
            )

            assertEquals(
                1L,
                result.entryCount
            )

            assertEquals(
                0L,
                result.validEntryCount
            )

            assertEquals(
                0L,
                result.warningEntryCount
            )

            assertEquals(
                1L,
                result.rejectedEntryCount
            )

            assertEquals(
                3L,
                result.errorCount
            )

            assertEquals(
                3,
                result.reportedErrorCount
            )

            assertEquals(
                0L,
                result.omittedErrorCount
            )

            assertEquals(
                1L,
                result.countsByReason[
                    ResultingNutritionKnowledgeValidationReason
                        .ENERGY_ABOVE_MAXIMUM
                ]
            )

            assertEquals(
                1L,
                result.countsByReason[
                    ResultingNutritionKnowledgeValidationReason
                        .SATURATED_FAT_EXCEEDS_TOTAL_FAT
                ]
            )

            assertEquals(
                1L,
                result.countsByReason[
                    ResultingNutritionKnowledgeValidationReason
                        .SUGARS_EXCEED_CARBOHYDRATES
                ]
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun validate_limitsReportedIssuesButKeepsTotalCounts() {

        val directory =
            Files.createTempDirectory(
                "nutrition-result-limited-issues"
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
                      "calories": 1200.0
                    },
                    "b": {
                      "calories": 1300.0
                    },
                    "c": {
                      "calories": 1400.0
                    }
                  }
                }
                """.trimIndent()
            )

            val result =
                ResultingNutritionKnowledgeValidator(
                    maximumReportedErrors =
                        1
                )
                    .validate(
                        inputFile =
                            inputFile
                    )

            assertFalse(
                result.isValid
            )

            assertEquals(
                3L,
                result.entryCount
            )

            assertEquals(
                3L,
                result.rejectedEntryCount
            )

            assertEquals(
                3L,
                result.errorCount
            )

            assertEquals(
                1,
                result.reportedErrorCount
            )

            assertEquals(
                2L,
                result.omittedErrorCount
            )

            assertEquals(
                1,
                result.errorIssues.size
            )

            assertEquals(
                3L,
                result.countsByReason[
                    ResultingNutritionKnowledgeValidationReason
                        .ENERGY_ABOVE_MAXIMUM
                ]
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun validate_acceptsActualRuntimeNutritionSchema() {

        val directory =
            Files.createTempDirectory(
                "nutrition-runtime-schema"
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
                    "apple": {
                      "calories": 52.0,
                      "protein": 0.3,
                      "fat": 0.2,
                      "saturatedFat": 0.0,
                      "carbohydrates": 14.0,
                      "sugar": 10.0,
                      "fiber": 2.4,
                      "salt": 0.0
                    }
                  }
                }
                """.trimIndent()
            )

            val result =
                ResultingNutritionKnowledgeValidator()
                    .validate(
                        inputFile =
                            inputFile
                    )

            assertTrue(
                result.isValid
            )

            assertEquals(
                1L,
                result.entryCount
            )

            assertEquals(
                1L,
                result.validEntryCount
            )

            assertEquals(
                0L,
                result.warningCount
            )

            assertEquals(
                0L,
                result.errorCount
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun validate_doesNotCompareRelationshipWhenParentValueWasMissing() {

        val directory =
            Files.createTempDirectory(
                "nutrition-missing-parent"
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
                "powdered sugar": {
                  "calories": 405.0,
                  "protein": 0.0,
                  "fat": 0.0,
                  "saturatedFat": 0.0,
                  "carbohydrates": 0.0,
                  "sugar": 97.0,
                  "fiber": 0.0,
                  "salt": 0.0,
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
                ResultingNutritionKnowledgeValidator()
                    .validate(
                        inputFile =
                            inputFile
                    )

            assertTrue(
                result.isValid
            )

            assertEquals(
                0L,
                result.errorCount
            )

            assertFalse(
                result.countsByReason.containsKey(
                    ResultingNutritionKnowledgeValidationReason
                        .SUGARS_EXCEED_CARBOHYDRATES
                )
            )
        } finally {
            directory.deleteRecursively()
        }
    }
}