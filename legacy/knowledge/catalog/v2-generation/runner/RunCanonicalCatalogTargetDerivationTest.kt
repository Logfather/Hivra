package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.target.CanonicalCatalogTargetDerivationPlanner
import de.shopme.testing.system.tools.knowledge.catalog.expansion.target.CanonicalCatalogTargetDerivationReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.target.CanonicalCatalogTargetDerivationWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.variant.CanonicalProductFamilyVariantCoverageReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalCatalogTargetDerivationTest {

    @Test
    fun deriveCanonicalCatalogTargetFromVariantCoverage() {
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

        val derivation =
            CanonicalCatalogTargetDerivationPlanner()
                .derive(
                    baseline = baseline,
                    variantCoverage =
                        variantCoverage
                )

        assertTrue(
            derivation.valid,
            "Canonical target derivation is invalid: " +
                    derivation.blockers.joinToString()
        )

        val outputFile =
            File(
                projectDirectory,
                OUTPUT_PATH
            )

        CanonicalCatalogTargetDerivationWriter()
            .write(
                derivation = derivation,
                outputFile = outputFile
            )

        val persisted =
            CanonicalCatalogTargetDerivationReader()
                .read(outputFile)

        assertEquals(
            derivation,
            persisted
        )

        assertTrue(
            persisted.derivedTargetEntryCount in
                    10_000..15_000
        )

        assertEquals(
            persisted.derivedTargetEntryCount,
            persisted.derivedTargetCountsByCategory
                .values
                .sum()
        )

        assertEquals(
            persisted.additionalDerivedEntryCount,
            persisted.additionalCountsByCategory
                .values
                .sum()
        )

        assertTrue(persisted.completeAdditionalAllocation)
        assertTrue(persisted.withinAllowedTargetRange)

        println(
            buildString {
                appendLine(
                    "Canonical catalog target derivation"
                )
                appendLine(
                    "-----------------------------------"
                )
                appendLine(
                    "Baseline entries: " +
                            persisted.baselineEntryCount
                )
                appendLine(
                    "Source planning target: " +
                            persisted.sourceTargetEntryCount
                )
                appendLine(
                    "Derived target: " +
                            persisted.derivedTargetEntryCount
                )
                appendLine(
                    "Allowed target range: " +
                            persisted.minimumAllowedTargetEntryCount +
                            ".." +
                            persisted.maximumAllowedTargetEntryCount
                )
                appendLine(
                    "Complexity uplift: " +
                            persisted.upliftBasisPoints +
                            " basis points"
                )
                appendLine(
                    "Complexity uplift share: " +
                            persisted.upliftShare
                )
                appendLine(
                    "Additional entries above source target: " +
                            persisted.additionalDerivedEntryCount
                )
                appendLine(
                    "Required expansion from baseline: " +
                            persisted
                                .requiredExpansionFromBaselineEntryCount
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
                    "Complete additional allocation: " +
                            persisted.completeAdditionalAllocation
                )
                appendLine(
                    "Within allowed range: " +
                            persisted.withinAllowedTargetRange
                )
                append(
                    "Derivation valid: " +
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

        const val OUTPUT_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-target-derivation.json"
    }
}