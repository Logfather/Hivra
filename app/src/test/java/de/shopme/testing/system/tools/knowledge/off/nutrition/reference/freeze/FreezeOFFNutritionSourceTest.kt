package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.freeze

import de.shopme.tools.knowledge.off.nutrition.reference.freeze.FreezeOFFNutritionSource
import de.shopme.tools.knowledge.off.nutrition.reference.freeze.FreezeOFFNutritionSourceRequest
import de.shopme.tools.knowledge.off.nutrition.reference.freeze.OFFNutritionSourceSnapshotReader
import de.shopme.tools.knowledge.off.nutrition.reference.freeze.OFFNutritionSourceSnapshotStatus
import de.shopme.tools.knowledge.off.nutrition.reference.freeze.OFFNutritionSourceSnapshotValidator
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FreezeOFFNutritionSourceTest {

    @Test
    fun freeze_createsDeterministicApprovedSnapshot() {

        val directory =
            Files.createTempDirectory(
                "freeze-off-nutrition"
            )
                .toFile()

        try {
            val sourceFile =
                directory.resolve(
                    "source/off-nutrition-reference-aggregates.json"
                )

            val validationReportFile =
                directory.resolve(
                    "reports/resulting-nutrition-knowledge-validation.json"
                )

            val conflictPolicyFile =
                directory.resolve(
                    "policies/resulting-nutrition-conflict-policy.json"
                )

            val frozenFile =
                directory.resolve(
                    "frozen/off-nutrition-reference-aggregates.json"
                )

            val snapshotFile =
                directory.resolve(
                    "frozen/off-nutrition-source-snapshot.json"
                )

            sourceFile.parentFile.mkdirs()
            validationReportFile.parentFile.mkdirs()
            conflictPolicyFile.parentFile.mkdirs()

            sourceFile.writeText(
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
                  }
                ]
                """.trimIndent()
            )

            validationReportFile.writeText(
                """
                {
                  "entryCount": 2,
                  "warningCount": 0,
                  "errorCount": 0,
                  "rejectedEntryCount": 0,
                  "valid": true,
                  "countsByReason": {}
                }
                """.trimIndent()
            )

            conflictPolicyFile.writeText(
                """
                {
                  "version": 1,
                  "policy": {
                    "version": 1,
                    "status": "APPROVED"
                  },
                  "decision": {
                    "approved": true,
                    "violations": []
                  }
                }
                """.trimIndent()
            )

            val request =
                FreezeOFFNutritionSourceRequest(
                    sourceAggregateFile =
                        sourceFile,
                    resultingNutritionValidationReportFile =
                        validationReportFile,
                    nutritionConflictPolicyFile =
                        conflictPolicyFile,
                    frozenAggregateFile =
                        frozenFile,
                    snapshotFile =
                        snapshotFile
                )

            val firstResult =
                FreezeOFFNutritionSource()
                    .freeze(
                        request =
                            request
                    )

            assertTrue(
                firstResult.frozenAggregateChanged
            )

            assertTrue(
                firstResult.snapshotChanged
            )

            assertTrue(
                frozenFile.isFile
            )

            assertTrue(
                snapshotFile.isFile
            )

            assertEquals(
                sourceFile.readBytes().toList(),
                frozenFile.readBytes().toList()
            )

            assertEquals(
                2L,
                firstResult.aggregateEntryCount
            )

            assertEquals(
                firstResult.sourceSha256,
                firstResult.frozenSha256
            )

            val snapshot =
                OFFNutritionSourceSnapshotReader()
                    .read(
                        file =
                            snapshotFile
                    )

            assertEquals(
                OFFNutritionSourceSnapshotStatus.APPROVED,
                snapshot.status
            )

            assertTrue(
                snapshot.nutritionValidationApproved
            )

            assertTrue(
                snapshot.nutritionConflictPolicyApproved
            )

            val firstSnapshotText =
                snapshotFile.readText()

            val secondResult =
                FreezeOFFNutritionSource()
                    .freeze(
                        request =
                            request
                    )

            assertFalse(
                secondResult.frozenAggregateChanged
            )

            assertFalse(
                secondResult.snapshotChanged
            )

            assertEquals(
                firstSnapshotText,
                snapshotFile.readText()
            )

            val validation =
                OFFNutritionSourceSnapshotValidator()
                    .validate(
                        snapshotFile =
                            snapshotFile,
                        frozenAggregateFile =
                            frozenFile,
                        expectedSourceAggregateFile =
                            sourceFile
                    )

            assertTrue(
                validation.valid,
                "Unexpected snapshot validation issues: " +
                        validation.issues
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun validator_detectsModifiedFrozenArtifact() {

        val directory =
            Files.createTempDirectory(
                "freeze-off-nutrition-modified"
            )
                .toFile()

        try {
            val sourceFile =
                directory.resolve(
                    "source.json"
                )

            val validationReportFile =
                directory.resolve(
                    "validation.json"
                )

            val conflictPolicyFile =
                directory.resolve(
                    "policy.json"
                )

            val frozenFile =
                directory.resolve(
                    "frozen.json"
                )

            val snapshotFile =
                directory.resolve(
                    "snapshot.json"
                )

            sourceFile.writeText(
                """
                [
                  {
                    "canonicalId": "apple",
                    "nutrition": {
                      "energyKcalPer100g": 52.0
                    }
                  }
                ]
                """.trimIndent()
            )

            validationReportFile.writeText(
                """
                {
                  "valid": true
                }
                """.trimIndent()
            )

            conflictPolicyFile.writeText(
                """
                {
                  "policy": {
                    "version": 1,
                    "status": "APPROVED"
                  },
                  "decision": {
                    "approved": true,
                    "violations": []
                  }
                }
                """.trimIndent()
            )

            FreezeOFFNutritionSource()
                .freeze(
                    request =
                        FreezeOFFNutritionSourceRequest(
                            sourceAggregateFile =
                                sourceFile,
                            resultingNutritionValidationReportFile =
                                validationReportFile,
                            nutritionConflictPolicyFile =
                                conflictPolicyFile,
                            frozenAggregateFile =
                                frozenFile,
                            snapshotFile =
                                snapshotFile
                        )
                )

            frozenFile.appendText(
                "\n"
            )

            val validation =
                OFFNutritionSourceSnapshotValidator()
                    .validate(
                        snapshotFile =
                            snapshotFile,
                        frozenAggregateFile =
                            frozenFile
                    )

            assertFalse(
                validation.valid
            )

            assertTrue(
                validation.issues.any { issue ->
                    issue.contains(
                        "size differs"
                    )
                }
            )

            assertTrue(
                validation.issues.any { issue ->
                    issue.contains(
                        "SHA-256 differs"
                    )
                }
            )
        } finally {
            directory.deleteRecursively()
        }
    }
}