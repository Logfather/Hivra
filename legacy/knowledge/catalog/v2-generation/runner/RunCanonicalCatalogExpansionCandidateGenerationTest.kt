package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.baseline
.CanonicalFoodCatalogBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate
.CanonicalCatalogExpansionCandidateGenerator
import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate
.CanonicalCatalogExpansionCandidateReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate
.CanonicalCatalogExpansionCandidateWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.combination
.CanonicalProductFamilyCombinationCoverageReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.target
.CanonicalCatalogTargetDerivationReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.value
.CanonicalConcreteVariantValueCoverageReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalCatalogExpansionCandidateGenerationTest {

    @Test
    fun generateCanonicalCatalogExpansionCandidates() {
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

        val targetDerivation =
            CanonicalCatalogTargetDerivationReader()
                .read(
                    File(
                        projectDirectory,
                        TARGET_DERIVATION_PATH
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

        val combinationCoverage =
            CanonicalProductFamilyCombinationCoverageReader()
                .read(
                    File(
                        projectDirectory,
                        COMBINATION_COVERAGE_PATH
                    )
                )

        val result =
            CanonicalCatalogExpansionCandidateGenerator()
                .generate(
                    baseline = baseline,
                    targetDerivation = targetDerivation,
                    concreteValueCoverage = valueCoverage,
                    combinationCoverage = combinationCoverage
                )

        assertTrue(
            result.valid,
            "Expansion candidate generation is invalid: " +
                    result.blockers.joinToString()
        )

        val outputFile =
            File(
                projectDirectory,
                OUTPUT_PATH
            )

        CanonicalCatalogExpansionCandidateWriter()
            .write(
                result = result,
                outputFile = outputFile
            )

        val persisted =
            CanonicalCatalogExpansionCandidateReader()
                .read(outputFile)

        assertEquals(result, persisted)

        assertEquals(3_419, persisted.baselineEntryCount)
        assertEquals(12_723, persisted.derivedTargetEntryCount)
        assertEquals(9_304, persisted.requiredExpansionEntryCount)
        assertEquals(9_304, persisted.generatedCandidateCount)

        assertEquals(
            persisted.generatedCandidateCount,
            persisted.uniqueCandidateKeyCount
        )

        assertTrue(persisted.exactExpansionCountReached)
        assertTrue(persisted.deterministicOrderValid)

        println(
            buildString {
                appendLine(
                    "Canonical catalog expansion candidate generation"
                )
                appendLine(
                    "------------------------------------------------"
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
                    "Generated candidates: " +
                            persisted.generatedCandidateCount
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
                    "Unique candidate keys: " +
                            persisted.uniqueCandidateKeyCount
                )
                appendLine(
                    "Unique normalized keys: " +
                            persisted.uniqueProposedNormalizedKeyCount
                )
                appendLine(
                    "Requires semantic validation: " +
                            persisted
                                .candidatesRequiringSemanticValidationCount
                )
                appendLine(
                    "Exact expansion count reached: " +
                            persisted.exactExpansionCountReached
                )
                appendLine(
                    "Deterministic order valid: " +
                            persisted.deterministicOrderValid
                )
                append(
                    "Generation valid: " +
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

        const val TARGET_DERIVATION_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-target-derivation.json"

        const val VALUE_COVERAGE_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-concrete-variant-value-coverage.json"

        const val COMBINATION_COVERAGE_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-product-family-variant-combination-coverage.json"

        const val OUTPUT_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-expansion-candidates.json"
    }
}