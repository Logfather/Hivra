package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.freeze

import de.shopme.tools.knowledge.off.nutrition.reference.freeze.OFFNutritionSourceSnapshotValidator
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ValidateFrozenOFFNutritionSourceTest {

    @Test
    fun validateFrozenOFFNutritionSource() {

        val projectRoot =
            resolveProjectRoot()

        val sourceAggregateFile =
            projectRoot.resolve(
                "data/generated/knowledge/references/off/" +
                        "off-nutrition-reference-aggregates.json"
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

        val result =
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
            result.valid,
            buildString {
                appendLine(
                    "Frozen OFF Nutrition source no longer matches its " +
                            "approved snapshot."
                )
                result.issues.forEach { issue ->
                    appendLine(
                        "- $issue"
                    )
                }
            }
        )
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