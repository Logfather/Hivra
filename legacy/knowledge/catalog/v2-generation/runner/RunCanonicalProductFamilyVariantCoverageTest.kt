package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyCoverageReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.variant.CanonicalProductFamilyVariantCoveragePlanner
import de.shopme.testing.system.tools.knowledge.catalog.expansion.variant.CanonicalProductFamilyVariantCoverageReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.variant.CanonicalProductFamilyVariantCoverageWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalProductFamilyVariantCoverageTest {

    @Test
    fun defineCanonicalProductFamilyVariantCoverage() {
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

        val productFamilyCoverage =
            CanonicalProductFamilyCoverageReader()
                .read(
                    File(
                        projectDirectory,
                        PRODUCT_FAMILY_COVERAGE_PATH
                    )
                )

        val coverage =
            CanonicalProductFamilyVariantCoveragePlanner()
                .plan(
                    baseline = baseline,
                    productFamilyCoverage =
                        productFamilyCoverage
                )

        assertTrue(
            coverage.valid,
            "Canonical variant coverage is invalid: " +
                    coverage.blockers.joinToString()
        )

        val outputFile =
            File(
                projectDirectory,
                OUTPUT_PATH
            )

        CanonicalProductFamilyVariantCoverageWriter()
            .write(
                coverage = coverage,
                outputFile = outputFile
            )

        val persisted =
            CanonicalProductFamilyVariantCoverageReader()
                .read(outputFile)

        assertEquals(
            coverage,
            persisted
        )

        assertEquals(
            25,
            persisted.categoryCount
        )

        assertEquals(
            productFamilyCoverage.productFamilyCount,
            persisted.productFamilyCount
        )

        assertEquals(
            persisted.productFamilyCount,
            persisted
                .familiesWithCompleteVariantCoverageCount
        )

        assertEquals(
            0,
            persisted
                .familiesWithIncompleteVariantCoverageCount
        )

        assertTrue(
            persisted.readyForTargetDerivation
        )

        assertTrue(
            persisted.currentRecommendedTargetEntryCount in
                    10_000..15_000
        )

        println(
            buildString {
                appendLine(
                    "Canonical product-family variant coverage"
                )
                appendLine(
                    "-----------------------------------------"
                )
                appendLine(
                    "Baseline entries: " +
                            persisted.baselineEntryCount
                )
                appendLine(
                    "Current recommended target: " +
                            persisted.currentRecommendedTargetEntryCount
                )
                appendLine(
                    "Allowed target range: " +
                            persisted.minimumAllowedTargetEntryCount +
                            ".." +
                            persisted.maximumAllowedTargetEntryCount
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
                    "Axis requirements: " +
                            persisted.totalAxisRequirementCount
                )
                appendLine(
                    "Recommended variant values: " +
                            persisted
                                .totalRecommendedVariantValueCoverageCount
                )
                appendLine(
                    "Required axes: " +
                            persisted.requiredAxisCount
                )
                appendLine(
                    "Recommended axes: " +
                            persisted.recommendedAxisCount
                )
                appendLine(
                    "Optional axes: " +
                            persisted.optionalAxisCount
                )
                appendLine(
                    "Complete family coverage: " +
                            persisted
                                .familiesWithCompleteVariantCoverageCount +
                            "/" +
                            persisted.productFamilyCount
                )
                appendLine(
                    "Ready for target derivation: " +
                            persisted.readyForTargetDerivation
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

        const val PRODUCT_FAMILY_COVERAGE_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-food-catalog-product-family-coverage.json"

        const val OUTPUT_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-product-family-variant-coverage.json"
    }
}