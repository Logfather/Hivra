package de.shopme.testing.system.tools.knowledge.catalog.expansion.variant

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyCoverageReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalProductFamilyVariantCoveragePlannerTest {

    @Test
    fun defineCompleteProductFamilyVariantCoverage() {
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

        val result =
            CanonicalProductFamilyVariantCoveragePlanner()
                .plan(
                    baseline = baseline,
                    productFamilyCoverage =
                        productFamilyCoverage
                )

        assertTrue(
            result.valid,
            result.blockers.joinToString()
        )

        assertEquals(
            25,
            result.categoryCount
        )

        assertEquals(
            productFamilyCoverage.productFamilyCount,
            result.productFamilyCount
        )

        assertEquals(
            result.productFamilyCount,
            result.familiesWithCompleteVariantCoverageCount
        )

        assertEquals(
            0,
            result.familiesWithIncompleteVariantCoverageCount
        )

        assertTrue(
            result.totalAxisRequirementCount >=
                    result.productFamilyCount
        )

        assertTrue(
            result.totalRecommendedVariantValueCoverageCount >
                    result.totalAxisRequirementCount
        )

        assertTrue(result.readyForTargetDerivation)

        assertEquals(
            productFamilyCoverage.recommendedTargetEntryCount,
            result.currentRecommendedTargetEntryCount
        )

        assertTrue(
            result.currentRecommendedTargetEntryCount in
                    10_000..15_000
        )
    }

    @Test
    fun planDeterministically() {
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

        val planner =
            CanonicalProductFamilyVariantCoveragePlanner()

        val first =
            planner.plan(
                baseline,
                productFamilyCoverage
            )

        val second =
            planner.plan(
                baseline,
                productFamilyCoverage
            )

        assertEquals(first, second)
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
                    "Could not resolve project directory from: " +
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
    }
}