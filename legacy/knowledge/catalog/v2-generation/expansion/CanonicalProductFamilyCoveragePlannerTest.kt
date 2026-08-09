package de.shopme.testing.system.tools.knowledge.catalog.expansion.family

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.refinement.CanonicalFoodCatalogRefinedTargetDistributionReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalProductFamilyCoveragePlannerTest {

    @Test
    fun allocateCompleteCanonicalProductFamilyCoverage() {
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

        val result =
            CanonicalProductFamilyCoveragePlanner()
                .plan(
                    baseline = baseline,
                    refinedTargetDistribution =
                        refinedTarget
                )

        assertTrue(
            result.valid,
            result.blockers.joinToString()
        )

        assertEquals(
            expected = 25,
            actual = result.categoryCount
        )

        assertTrue(
            result.productFamilyCount >= 150,
            "Product-family coverage is unexpectedly narrow."
        )

        assertEquals(
            expected =
                result.recommendedTargetEntryCount,
            actual =
                result.categories.sumOf {
                    it.allocatedFamilyTargetEntryCount
                }
        )

        assertEquals(
            expected =
                result.recommendedTargetEntryCount,
            actual =
                result.recommendedTargetCountsByCategory
                    .values
                    .sum()
        )

        assertTrue(result.completeCategoryAllocation)
        assertTrue(result.completeFamilyAllocation)

        assertTrue(
            result.recommendedTargetEntryCount in
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

        val refinedTarget =
            CanonicalFoodCatalogRefinedTargetDistributionReader()
                .read(
                    File(
                        projectDirectory,
                        REFINED_TARGET_PATH
                    )
                )

        val planner =
            CanonicalProductFamilyCoveragePlanner()

        val first =
            planner.plan(
                baseline,
                refinedTarget
            )

        val second =
            planner.plan(
                baseline,
                refinedTarget
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

        const val REFINED_TARGET_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-food-catalog-refined-target-distribution.json"
    }
}