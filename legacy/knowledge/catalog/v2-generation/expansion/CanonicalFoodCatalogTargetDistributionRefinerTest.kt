package de.shopme.testing.system.tools.knowledge.catalog.expansion.refinement

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.category.CanonicalFoodCategoryRegistry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.CanonicalFoodCatalogTargetDistributionReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalFoodCatalogTargetDistributionRefinerTest {

    @Test
    fun preserveCanonicalTenThousandItemTarget() {
        val projectDirectory =
            resolveProjectDirectory()

        val baseline =
            CanonicalFoodCatalogBaselineReader()
                .read(
                    File(
                        projectDirectory,
                        "data/generated/knowledge/catalog/baseline/" +
                                "canonical-food-catalog-baseline.json"
                    )
                )

        val sourceDistribution =
            CanonicalFoodCatalogTargetDistributionReader()
                .read(
                    File(
                        projectDirectory,
                        "data/generated/knowledge/catalog/expansion/" +
                                "canonical-food-catalog-target-distribution.json"
                    )
                )

        val refined =
            CanonicalFoodCatalogTargetDistributionRefiner()
                .refine(
                    baseline = baseline,
                    targetDistribution =
                        sourceDistribution,
                    categoryRegistry =
                        CanonicalFoodCategoryRegistry()
                )

        assertTrue(
            refined.valid,
            refined.blockers.joinToString()
        )

        assertEquals(
            10_000,
            refined.refinedTargetEntryCount
        )

        assertEquals(
            6_581,
            refined.requiredExpansionEntryCount
        )

        assertEquals(
            25,
            refined.categoryCount
        )

        assertEquals(
            0,
            refined.changedCategoryCount
        )

        assertEquals(
            sourceDistribution.targetCountsByCategory,
            refined.refinedTargetCountsByCategory
        )

        assertTrue(
            refined.categories.all {
                CanonicalCatalogExcludedSkuAxis.BRAND in
                        it.excludedSkuAxes
            }
        )

        assertTrue(
            refined.categories.all {
                CanonicalCatalogExcludedSkuAxis.PACKAGE_SIZE in
                        it.excludedSkuAxes
            }
        )

        assertTrue(
            refined.categories.all {
                it.allowedIdentityAxes.isNotEmpty()
            }
        )
    }

    private fun resolveProjectDirectory(): File {
        val workingDirectory =
            File(
                System.getProperty("user.dir")
            ).canonicalFile

        val baselinePath =
            "data/generated/knowledge/catalog/baseline/" +
                    "canonical-food-catalog-baseline.json"

        return when {
            File(
                workingDirectory,
                baselinePath
            ).isFile ->
                workingDirectory

            workingDirectory.name == "app" ->
                requireNotNull(
                    workingDirectory.parentFile
                )

            else ->
                error(
                    "Could not resolve project directory from " +
                            workingDirectory.absolutePath
                )
        }
    }
}