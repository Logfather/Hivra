package de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate

import de.shopme.testing.system.tools.knowledge.catalog.baseline
.CanonicalFoodCatalogBaselineReader
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

class CanonicalCatalogExpansionCandidateGeneratorTest {

    @Test
    fun generateExactCanonicalExpansionCandidateCount() {
        val fixtures =
            readFixtures()

        val result =
            CanonicalCatalogExpansionCandidateGenerator()
                .generate(
                    baseline =
                        fixtures.baseline,

                    targetDerivation =
                        fixtures.targetDerivation,

                    concreteValueCoverage =
                        fixtures.valueCoverage,

                    combinationCoverage =
                        fixtures.combinationCoverage
                )

        assertTrue(
            result.valid,
            result.blockers.joinToString()
        )

        assertEquals(3_419, result.baselineEntryCount)
        assertEquals(12_723, result.derivedTargetEntryCount)
        assertEquals(9_304, result.requiredExpansionEntryCount)
        assertEquals(9_304, result.generatedCandidateCount)

        assertEquals(25, result.categoryCount)
        assertEquals(240, result.productFamilyCount)

        assertEquals(
            result.generatedCandidateCount,
            result.uniqueCandidateKeyCount
        )

        assertEquals(
            result.generatedCandidateCount,
            result.uniqueProposedNormalizedKeyCount
        )

        assertEquals(
            result.generatedCandidateCount,
            result.candidatesRequiringSemanticValidationCount
        )

        assertTrue(result.exactExpansionCountReached)
        assertTrue(result.deterministicOrderValid)

        assertEquals(
            (1..9_304).toList(),
            result.candidates.map {
                it.candidateIndex
            }
        )
    }

    @Test
    fun generateDeterministically() {
        val fixtures =
            readFixtures()

        val generator =
            CanonicalCatalogExpansionCandidateGenerator()

        val first =
            generator.generate(
                fixtures.baseline,
                fixtures.targetDerivation,
                fixtures.valueCoverage,
                fixtures.combinationCoverage
            )

        val second =
            generator.generate(
                fixtures.baseline,
                fixtures.targetDerivation,
                fixtures.valueCoverage,
                fixtures.combinationCoverage
            )

        assertEquals(first, second)
    }

    private fun readFixtures(): Fixtures {
        val projectDirectory =
            resolveProjectDirectory()

        return Fixtures(
            baseline =
                CanonicalFoodCatalogBaselineReader()
                    .read(
                        File(
                            projectDirectory,
                            BASELINE_PATH
                        )
                    ),

            targetDerivation =
                CanonicalCatalogTargetDerivationReader()
                    .read(
                        File(
                            projectDirectory,
                            TARGET_DERIVATION_PATH
                        )
                    ),

            valueCoverage =
                CanonicalConcreteVariantValueCoverageReader()
                    .read(
                        File(
                            projectDirectory,
                            VALUE_COVERAGE_PATH
                        )
                    ),

            combinationCoverage =
                CanonicalProductFamilyCombinationCoverageReader()
                    .read(
                        File(
                            projectDirectory,
                            COMBINATION_COVERAGE_PATH
                        )
                    )
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
                    "Could not resolve project directory from: " +
                            workingDirectory.absolutePath
                )
        }
    }

    private data class Fixtures(
        val baseline:
        de.shopme.testing.system.tools.knowledge.catalog.baseline
        .CanonicalFoodCatalogBaseline,

        val targetDerivation:
        de.shopme.testing.system.tools.knowledge.catalog.expansion.target
        .CanonicalCatalogTargetDerivation,

        val valueCoverage:
        de.shopme.testing.system.tools.knowledge.catalog.expansion.value
        .CanonicalConcreteVariantValueCoverage,

        val combinationCoverage:
        de.shopme.testing.system.tools.knowledge.catalog.expansion
        .combination
        .CanonicalProductFamilyCombinationCoverage
    )

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
    }
}