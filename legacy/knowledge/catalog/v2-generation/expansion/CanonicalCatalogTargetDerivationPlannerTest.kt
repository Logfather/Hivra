package de.shopme.testing.system.tools.knowledge.catalog.expansion.target

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.variant.CanonicalProductFamilyVariantCoverageReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalCatalogTargetDerivationPlannerTest {

    @Test
    fun deriveBoundedCanonicalCatalogTarget() {
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

        val variantCoverage =
            CanonicalProductFamilyVariantCoverageReader()
                .read(
                    File(
                        projectDirectory,
                        VARIANT_COVERAGE_PATH
                    )
                )

        val result =
            CanonicalCatalogTargetDerivationPlanner()
                .derive(
                    baseline = baseline,
                    variantCoverage =
                        variantCoverage
                )

        assertTrue(
            result.valid,
            result.blockers.joinToString()
        )

        assertEquals(
            baseline.finalOutputEntryCount,
            result.baselineEntryCount
        )

        assertEquals(
            variantCoverage.currentRecommendedTargetEntryCount,
            result.sourceTargetEntryCount
        )

        assertTrue(
            result.derivedTargetEntryCount in
                    10_000..15_000
        )

        assertTrue(
            result.derivedTargetEntryCount >=
                    result.sourceTargetEntryCount
        )

        assertEquals(
            result.derivedTargetEntryCount -
                    result.sourceTargetEntryCount,
            result.additionalDerivedEntryCount
        )

        assertEquals(
            result.derivedTargetEntryCount -
                    result.baselineEntryCount,
            result.requiredExpansionFromBaselineEntryCount
        )

        assertEquals(
            result.additionalDerivedEntryCount,
            result.additionalCountsByCategory
                .values
                .sum()
        )

        assertEquals(
            result.derivedTargetEntryCount,
            result.derivedTargetCountsByCategory
                .values
                .sum()
        )

        assertTrue(result.completeAdditionalAllocation)
        assertTrue(result.withinAllowedTargetRange)
    }

    @Test
    fun deriveDeterministically() {
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

        val variantCoverage =
            CanonicalProductFamilyVariantCoverageReader()
                .read(
                    File(
                        projectDirectory,
                        VARIANT_COVERAGE_PATH
                    )
                )

        val planner =
            CanonicalCatalogTargetDerivationPlanner()

        val first =
            planner.derive(
                baseline,
                variantCoverage
            )

        val second =
            planner.derive(
                baseline,
                variantCoverage
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

        const val VARIANT_COVERAGE_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-product-family-variant-coverage.json"
    }
}