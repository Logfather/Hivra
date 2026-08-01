package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.freeze

import de.shopme.tools.knowledge.off.nutrition.reference.freeze.FreezeOFFNutritionSource
import de.shopme.tools.knowledge.off.nutrition.reference.freeze.FreezeOFFNutritionSourceRequest
import de.shopme.tools.knowledge.off.nutrition.reference.freeze.OFFNutritionSourceSnapshotValidator
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunFreezeOFFNutritionSourceTest {

    @Test
    fun freezeApprovedOFFNutritionSource() {

        val projectRoot =
            resolveProjectRoot()

        val sourceAggregateFile =
            projectRoot.resolve(
                "data/generated/knowledge/references/off/" +
                        "off-nutrition-reference-aggregates.json"
            )
                .canonicalFile

        val validationReportFile =
            projectRoot.resolve(
                "data/generated/knowledge/reports/" +
                        "resulting-nutrition-knowledge-validation.json"
            )
                .canonicalFile

        val conflictPolicyFile =
            projectRoot.resolve(
                "data/generated/knowledge/policies/" +
                        "resulting-nutrition-conflict-policy.json"
            )
                .canonicalFile

        val frozenDirectory =
            projectRoot.resolve(
                "data/generated/knowledge/frozen/off/nutrition"
            )
                .canonicalFile

        val frozenAggregateFile =
            frozenDirectory.resolve(
                "off-nutrition-reference-aggregates.json"
            )
                .canonicalFile

        val snapshotFile =
            frozenDirectory.resolve(
                "off-nutrition-source-snapshot.json"
            )
                .canonicalFile

        require(sourceAggregateFile.isFile) {
            "Approved OFF Nutrition aggregate dataset does not exist: " +
                    sourceAggregateFile.absolutePath
        }

        require(validationReportFile.isFile) {
            "Resulting Nutrition validation report does not exist: " +
                    validationReportFile.absolutePath
        }

        require(conflictPolicyFile.isFile) {
            "Approved Nutrition conflict policy does not exist: " +
                    conflictPolicyFile.absolutePath
        }

        val result =
            FreezeOFFNutritionSource()
                .freeze(
                    request =
                        FreezeOFFNutritionSourceRequest(
                            sourceAggregateFile =
                                sourceAggregateFile,
                            resultingNutritionValidationReportFile =
                                validationReportFile,
                            nutritionConflictPolicyFile =
                                conflictPolicyFile,
                            frozenAggregateFile =
                                frozenAggregateFile,
                            snapshotFile =
                                snapshotFile
                        )
                )

        assertTrue(
            frozenAggregateFile.isFile,
            "Frozen OFF Nutrition aggregate file was not generated: " +
                    frozenAggregateFile.absolutePath
        )

        assertTrue(
            snapshotFile.isFile,
            "OFF Nutrition source snapshot was not generated: " +
                    snapshotFile.absolutePath
        )

        assertEquals(
            sourceAggregateFile.length(),
            frozenAggregateFile.length()
        )

        assertEquals(
            result.sourceSha256,
            result.frozenSha256
        )

        assertEquals(
            511_016L,
            result.aggregateEntryCount,
            "Unexpected frozen OFF Nutrition aggregate count."
        )

        val validation =
            OFFNutritionSourceSnapshotValidator()
                .validate(
                    snapshotFile =
                        snapshotFile,
                    frozenAggregateFile =
                        frozenAggregateFile,
                    expectedSourceAggregateFile =
                        sourceAggregateFile
                )

        assertTrue(
            validation.valid,
            buildString {
                appendLine(
                    "Frozen OFF Nutrition source snapshot is invalid."
                )
                appendLine(
                    "Issues=${validation.issues}"
                )
                appendLine(
                    "Snapshot=${snapshotFile.absolutePath}"
                )
                appendLine(
                    "Frozen=${frozenAggregateFile.absolutePath}"
                )
            }
        )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("FROZEN OFF NUTRITION SOURCE")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
        println(
            "Aggregate entries=" +
                    result.aggregateEntryCount
        )
        println(
            "Source size=" +
                    result.sourceFileSizeBytes
        )
        println(
            "Frozen size=" +
                    result.frozenFileSizeBytes
        )
        println(
            "SHA-256=" +
                    result.sourceSha256
        )
        println(
            "Frozen aggregate changed=" +
                    result.frozenAggregateChanged
        )
        println(
            "Snapshot changed=" +
                    result.snapshotChanged
        )
        println(
            "Frozen aggregate=" +
                    frozenAggregateFile.absolutePath
        )
        println(
            "Snapshot=" +
                    snapshotFile.absolutePath
        )
        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    private fun resolveProjectRoot(): File {

        val currentDirectory =
            File(".").canonicalFile

        return when {
            currentDirectory.name == "app" ->
                requireNotNull(
                    currentDirectory.parentFile
                )
                    .canonicalFile

            currentDirectory.resolve(
                "app"
            ).isDirectory ->
                currentDirectory

            else ->
                error(
                    "Could not resolve ShopMe project root from: " +
                            currentDirectory.absolutePath
                )
        }
    }
}