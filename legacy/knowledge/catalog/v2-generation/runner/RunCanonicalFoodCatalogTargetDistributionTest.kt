package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.CanonicalFoodCatalogTargetDistributionPlanner
import de.shopme.testing.system.tools.knowledge.catalog.expansion.CanonicalFoodCatalogTargetDistributionReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.CanonicalFoodCatalogTargetDistributionWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalFoodCatalogTargetDistributionTest {

    @Test
    fun defineCanonicalTenThousandItemTargetDistribution() {
        val projectDirectory =
            resolveProjectDirectory()

        val baselineFile =
            File(
                projectDirectory,
                "data/generated/knowledge/catalog/baseline/" +
                        "canonical-food-catalog-baseline.json"
            )

        val outputFile =
            File(
                projectDirectory,
                "data/generated/knowledge/catalog/expansion/" +
                        "canonical-food-catalog-target-distribution.json"
            )

        val baseline =
            CanonicalFoodCatalogBaselineReader()
                .read(
                    inputFile =
                        baselineFile
                )

        val distribution =
            CanonicalFoodCatalogTargetDistributionPlanner()
                .plan(
                    baseline =
                        baseline
                )

        assertTrue(
            distribution.valid,
            buildString {
                append(
                    "Canonical catalog target distribution is invalid."
                )

                if (distribution.blockers.isNotEmpty()) {
                    append(" Blockers: ")
                    append(
                        distribution.blockers
                            .joinToString()
                    )
                }
            }
        )

        CanonicalFoodCatalogTargetDistributionWriter()
            .write(
                distribution =
                    distribution,
                outputFile =
                    outputFile
            )

        val persisted =
            CanonicalFoodCatalogTargetDistributionReader()
                .read(
                    inputFile =
                        outputFile
                )

        assertEquals(
            distribution,
            persisted,
            "Persisted target distribution differs from generated result."
        )

        assertEquals(
            expected = 3_419,
            actual =
                persisted.baselineEntryCount
        )

        assertEquals(
            expected = 10_000,
            actual =
                persisted.targetEntryCount
        )

        assertEquals(
            expected = 6_581,
            actual =
                persisted.requiredExpansionEntryCount
        )

        assertEquals(
            expected = 10_000,
            actual =
                persisted.targetCountsByCategory
                    .values
                    .sum()
        )

        assertEquals(
            expected =
                persisted.requiredExpansionEntryCount,
            actual =
                persisted.expansionCountsByCategory
                    .values
                    .sum()
        )

        assertEquals(
            expected = 0,
            actual =
                persisted.categoriesExceedingTargetCount
        )

        assertTrue(
            persisted.unallocatedBaselineCategories
                .isEmpty()
        )

        println(
            buildString {
                appendLine(
                    "Canonical 10,000-item target distribution"
                )
                appendLine(
                    "-----------------------------------------"
                )
                appendLine(
                    "Baseline ID: " +
                            persisted.sourceBaselineId
                )
                appendLine(
                    "Baseline entries: " +
                            persisted.baselineEntryCount
                )
                appendLine(
                    "Target entries: " +
                            persisted.targetEntryCount
                )
                appendLine(
                    "Required expansion: " +
                            persisted.requiredExpansionEntryCount
                )
                appendLine(
                    "Target categories: " +
                            persisted.targetCategoryCount
                )
                appendLine(
                    "Categories requiring expansion: " +
                            persisted
                                .categoriesRequiringExpansionCount
                )
                appendLine(
                    "Categories already at target: " +
                            persisted.categoriesAtTargetCount
                )
                appendLine(
                    "Categories exceeding target: " +
                            persisted.categoriesExceedingTargetCount
                )
                append(
                    "Distribution valid: " +
                            persisted.valid
                )
            }
        )
    }

    private fun resolveProjectDirectory(): File {
        val workingDirectory =
            File(
                System.getProperty("user.dir")
            ).canonicalFile

        return when {
            File(
                workingDirectory,
                "data/generated/knowledge/catalog/baseline/" +
                        "canonical-food-catalog-baseline.json"
            ).isFile ->
                workingDirectory

            workingDirectory.name == "app" &&
                    File(
                        workingDirectory.parentFile,
                        "data/generated/knowledge/catalog/baseline/" +
                                "canonical-food-catalog-baseline.json"
                    ).isFile ->
                requireNotNull(
                    workingDirectory.parentFile
                )

            else ->
                error(
                    "Could not resolve ShopMe project directory from: " +
                            workingDirectory.absolutePath
                )
        }
    }
}