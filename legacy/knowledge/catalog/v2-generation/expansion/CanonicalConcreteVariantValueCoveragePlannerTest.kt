package de.shopme.testing.system.tools.knowledge.catalog.expansion.value

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.target.CanonicalCatalogTargetDerivationReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.variant.CanonicalProductFamilyVariantCoverageReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalConcreteVariantValueCoveragePlannerTest {

    @Test
    fun defineCompleteConcreteVariantValueCoverage() {
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

        val targetDerivation =
            CanonicalCatalogTargetDerivationReader()
                .read(
                    File(
                        projectDirectory,
                        TARGET_DERIVATION_PATH
                    )
                )

        val result =
            CanonicalConcreteVariantValueCoveragePlanner()
                .plan(
                    baseline = baseline,
                    variantCoverage = variantCoverage,
                    targetDerivation = targetDerivation
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
            240,
            result.productFamilyCount
        )

        assertEquals(
            2_066,
            result.axisCoverageCount
        )

        assertEquals(
            result.productFamilyCount,
            result.familiesWithCompleteValueCoverageCount
        )

        assertEquals(
            0,
            result.familiesWithIncompleteValueCoverageCount
        )

        assertTrue(
            result.canonicalVariantValueCount > 100
        )

        assertTrue(
            result.selectedFamilyAxisValueCount >=
                    result.axisCoverageCount
        )

        assertEquals(
            12_723,
            result.derivedTargetEntryCount
        )

        assertTrue(result.readyForCombinationCuration)
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

        val variantCoverage =
            CanonicalProductFamilyVariantCoverageReader()
                .read(
                    File(
                        projectDirectory,
                        VARIANT_COVERAGE_PATH
                    )
                )

        val targetDerivation =
            CanonicalCatalogTargetDerivationReader()
                .read(
                    File(
                        projectDirectory,
                        TARGET_DERIVATION_PATH
                    )
                )

        val planner =
            CanonicalConcreteVariantValueCoveragePlanner()

        val first =
            planner.plan(
                baseline,
                variantCoverage,
                targetDerivation
            )

        val second =
            planner.plan(
                baseline,
                variantCoverage,
                targetDerivation
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

        const val TARGET_DERIVATION_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-target-derivation.json"
    }
}