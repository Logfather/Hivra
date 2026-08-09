package de.shopme.testing.system.tools.knowledge.catalog.expansion.combination

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.target.CanonicalCatalogTargetDerivationReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.value.CanonicalConcreteVariantValueCoverageReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalProductFamilyCombinationCoveragePlannerTest {

    @Test
    fun curateCompleteVariantCombinationCoverage() {
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

        val result =
            CanonicalProductFamilyCombinationCoveragePlanner()
                .plan(
                    baseline = baseline,
                    concreteValueCoverage =
                        valueCoverage,
                    targetDerivation =
                        targetDerivation
                )

        assertTrue(
            result.valid,
            result.blockers.joinToString()
        )

        assertEquals(25, result.categoryCount)
        assertEquals(240, result.productFamilyCount)
        assertEquals(12_723, result.derivedTargetEntryCount)
        assertEquals(9_304, result.requiredExpansionEntryCount)

        assertEquals(
            result.productFamilyCount,
            result
                .familiesWithCompleteCombinationCoverageCount
        )

        assertEquals(
            0,
            result
                .familiesWithIncompleteCombinationCoverageCount
        )

        assertTrue(result.combinationTemplateCount > 2_066)

        assertTrue(
            result.curatedCombinationCapacity >=
                    result.derivedTargetEntryCount
        )

        assertTrue(result.cartesianExpansionForbidden)
        assertTrue(result.readyForCandidateGeneration)
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

        val planner =
            CanonicalProductFamilyCombinationCoveragePlanner()

        val first =
            planner.plan(
                baseline,
                valueCoverage,
                targetDerivation
            )

        val second =
            planner.plan(
                baseline,
                valueCoverage,
                targetDerivation
            )

        assertEquals(first, second)
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
                    "Could not resolve project directory from: " +
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
    }
}