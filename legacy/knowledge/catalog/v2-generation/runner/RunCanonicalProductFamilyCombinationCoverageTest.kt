package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.combination.CanonicalProductFamilyCombinationCoveragePlanner
import de.shopme.testing.system.tools.knowledge.catalog.expansion.combination.CanonicalProductFamilyCombinationCoverageReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.combination.CanonicalProductFamilyCombinationCoverageWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.target.CanonicalCatalogTargetDerivationReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.value.CanonicalConcreteVariantValueCoverageReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalProductFamilyCombinationCoverageTest {

    @Test
    fun curateCanonicalProductFamilyVariantCombinations() {
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

        val valueCoverage =
            CanonicalConcreteVariantValueCoverageReader()
                .read(
                    File(
                        projectDirectory,
                        VALUE_COVERAGE_PATH
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
            CanonicalProductFamilyCombinationCoveragePlanner()
                .plan(
                    baseline = baseline,
                    concreteValueCoverage =
                        valueCoverage,
                    targetDerivation =
                        targetDerivation
                )

        assertTrue(
            coverage.valid,
            "Canonical combination coverage is invalid: " +
                    coverage.blockers.joinToString()
        )

        val outputFile =
            File(
                projectDirectory,
                OUTPUT_PATH
            )

        CanonicalProductFamilyCombinationCoverageWriter()
            .write(
                coverage = coverage,
                outputFile = outputFile
            )

        val persisted =
            CanonicalProductFamilyCombinationCoverageReader()
                .read(outputFile)

        assertEquals(coverage, persisted)

        assertEquals(25, persisted.categoryCount)
        assertEquals(240, persisted.productFamilyCount)
        assertEquals(12_723, persisted.derivedTargetEntryCount)
        assertEquals(9_304, persisted.requiredExpansionEntryCount)

        assertEquals(
            persisted.productFamilyCount,
            persisted
                .familiesWithCompleteCombinationCoverageCount
        )

        assertEquals(
            0,
            persisted
                .familiesWithIncompleteCombinationCoverageCount
        )

        assertTrue(persisted.cartesianExpansionForbidden)
        assertTrue(persisted.readyForCandidateGeneration)

        println(
            buildString {
                appendLine(
                    "Canonical product-family combination coverage"
                )
                appendLine(
                    "---------------------------------------------"
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
                    "Combination templates: " +
                            persisted.combinationTemplateCount
                )
                appendLine(
                    "Single-axis templates: " +
                            persisted.singleAxisTemplateCount
                )
                appendLine(
                    "Anchored-pair templates: " +
                            persisted.anchoredPairTemplateCount
                )
                appendLine(
                    "Curated-triple templates: " +
                            persisted.curatedTripleTemplateCount
                )
                appendLine(
                    "Curated combination capacity: " +
                            persisted.curatedCombinationCapacity
                )
                appendLine(
                    "Complete family coverage: " +
                            persisted
                                .familiesWithCompleteCombinationCoverageCount +
                            "/" +
                            persisted.productFamilyCount
                )
                appendLine(
                    "Cartesian expansion forbidden: " +
                            persisted.cartesianExpansionForbidden
                )
                appendLine(
                    "Ready for candidate generation: " +
                            persisted.readyForCandidateGeneration
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
                requireNotNull(
                    System.getProperty("user.dir")
                )
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

        const val VALUE_COVERAGE_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-concrete-variant-value-coverage.json"

        const val TARGET_DERIVATION_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-target-derivation.json"

        const val OUTPUT_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-product-family-variant-combination-coverage.json"
    }
}