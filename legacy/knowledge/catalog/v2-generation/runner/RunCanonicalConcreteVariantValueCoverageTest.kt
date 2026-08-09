package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.target.CanonicalCatalogTargetDerivationReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.value.CanonicalConcreteVariantValueCoveragePlanner
import de.shopme.testing.system.tools.knowledge.catalog.expansion.value.CanonicalConcreteVariantValueCoverageReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.value.CanonicalConcreteVariantValueCoverageWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.variant.CanonicalProductFamilyVariantCoverageReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalConcreteVariantValueCoverageTest {

    @Test
    fun defineConcreteCanonicalVariantValues() {
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

        val coverage =
            CanonicalConcreteVariantValueCoveragePlanner()
                .plan(
                    baseline = baseline,
                    variantCoverage = variantCoverage,
                    targetDerivation = targetDerivation
                )

        assertTrue(
            coverage.valid,
            "Concrete variant value coverage is invalid: " +
                    coverage.blockers.joinToString()
        )

        val outputFile =
            File(
                projectDirectory,
                OUTPUT_PATH
            )

        CanonicalConcreteVariantValueCoverageWriter()
            .write(
                coverage = coverage,
                outputFile = outputFile
            )

        val persisted =
            CanonicalConcreteVariantValueCoverageReader()
                .read(outputFile)

        assertEquals(coverage, persisted)

        assertEquals(25, persisted.categoryCount)
        assertEquals(240, persisted.productFamilyCount)
        assertEquals(2_066, persisted.axisCoverageCount)
        assertEquals(12_723, persisted.derivedTargetEntryCount)

        assertEquals(
            persisted.productFamilyCount,
            persisted.familiesWithCompleteValueCoverageCount
        )

        assertEquals(
            0,
            persisted.familiesWithIncompleteValueCoverageCount
        )

        assertTrue(persisted.readyForCombinationCuration)

        println(
            buildString {
                appendLine(
                    "Concrete canonical variant value coverage"
                )
                appendLine(
                    "-----------------------------------------"
                )
                appendLine(
                    "Baseline entries: " +
                            persisted.baselineEntryCount
                )
                appendLine(
                    "Derived target: " +
                            persisted.derivedTargetEntryCount
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
                    "Axis value sets: " +
                            persisted.canonicalAxisValueSetCount
                )
                appendLine(
                    "Canonical variant values: " +
                            persisted.canonicalVariantValueCount
                )
                appendLine(
                    "Family-axis coverages: " +
                            persisted.axisCoverageCount
                )
                appendLine(
                    "Selected family-axis values: " +
                            persisted.selectedFamilyAxisValueCount
                )
                appendLine(
                    "Complete family coverage: " +
                            persisted
                                .familiesWithCompleteValueCoverageCount +
                            "/" +
                            persisted.productFamilyCount
                )
                appendLine(
                    "Ready for combination curation: " +
                            persisted.readyForCombinationCuration
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

        const val VARIANT_COVERAGE_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-product-family-variant-coverage.json"

        const val TARGET_DERIVATION_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-target-derivation.json"

        const val OUTPUT_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-concrete-variant-value-coverage.json"
    }
}