package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyCoveragePlanner
import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyCoverageReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyCoverageWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.refinement.CanonicalFoodCatalogRefinedTargetDistributionReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalProductFamilyCoverageTest {

    @Test
    fun defineCanonicalProductFamilyCoverage() {
        val projectDirectory =
            resolveProjectDirectory()

        val baseline =
            CanonicalFoodCatalogBaselineReader()
                .read(
                    File(
                        projectDirectory,
                        BASELINE_PATH
                    )
                )

        val refinedTarget =
            CanonicalFoodCatalogRefinedTargetDistributionReader()
                .read(
                    File(
                        projectDirectory,
                        REFINED_TARGET_PATH
                    )
                )

        val coverage =
            CanonicalProductFamilyCoveragePlanner()
                .plan(
                    baseline = baseline,
                    refinedTargetDistribution =
                        refinedTarget
                )

        assertTrue(
            coverage.valid,
            "Canonical product-family coverage is invalid: " +
                    coverage.blockers.joinToString()
        )

        val outputFile =
            File(
                projectDirectory,
                OUTPUT_PATH
            )

        CanonicalProductFamilyCoverageWriter()
            .write(
                coverage = coverage,
                outputFile = outputFile
            )

        val persisted =
            CanonicalProductFamilyCoverageReader()
                .read(outputFile)

        assertEquals(coverage, persisted)

        assertEquals(25, persisted.categoryCount)

        assertTrue(
            persisted.recommendedTargetEntryCount in
                    10_000..15_000
        )

        assertEquals(
            persisted.recommendedTargetEntryCount,
            persisted.categories.sumOf {
                it.allocatedFamilyTargetEntryCount
            }
        )

        assertTrue(persisted.completeCategoryAllocation)
        assertTrue(persisted.completeFamilyAllocation)

        println(
            buildString {
                appendLine(
                    "Canonical product-family coverage"
                )
                appendLine(
                    "---------------------------------"
                )
                appendLine(
                    "Baseline entries: " +
                            persisted.baselineEntryCount
                )
                appendLine(
                    "Original planning target: " +
                            persisted.originalPlanningTargetEntryCount
                )
                appendLine(
                    "Recommended target: " +
                            persisted.recommendedTargetEntryCount
                )
                appendLine(
                    "Allowed target range: " +
                            persisted.minimumRecommendedTargetEntryCount +
                            ".." +
                            persisted.maximumAllowedTargetEntryCount
                )
                appendLine(
                    "Required expansion: " +
                            persisted.requiredExpansionEntryCount
                )
                appendLine(
                    "Categories: " +
                            persisted.categoryCount
                )
                appendLine(
                    "Product families: " +
                            persisted.productFamilyCount
                )
                appendLine(
                    "Complete category allocation: " +
                            persisted.completeCategoryAllocation
                )
                appendLine(
                    "Complete family allocation: " +
                            persisted.completeFamilyAllocation
                )
                append(
                    "Coverage valid: " +
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
                BASELINE_PATH
            ).isFile ->
                workingDirectory

            workingDirectory.name == "app" ->
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

    private companion object {
        const val BASELINE_PATH =
            "data/generated/knowledge/catalog/baseline/" +
                    "canonical-food-catalog-baseline.json"

        const val REFINED_TARGET_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-food-catalog-refined-target-distribution.json"

        const val OUTPUT_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-food-catalog-product-family-coverage.json"
    }
}