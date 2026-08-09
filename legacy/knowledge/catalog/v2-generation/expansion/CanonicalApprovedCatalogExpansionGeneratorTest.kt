package de.shopme.testing.system.tools.knowledge.catalog.expansion.approval

import de.shopme.testing.system.tools.knowledge.catalog.baseline
.CanonicalFoodCatalogBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate
.CanonicalCatalogExpansionCandidateReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalCatalogExpansionSemanticValidationReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalApprovedCatalogExpansionGeneratorTest {

    @Test
    fun materializeOnlySemanticallyAcceptedCandidates() {
        val fixtures =
            readFixtures()

        val result =
            CanonicalApprovedCatalogExpansionGenerator()
                .generate(
                    baseline =
                        fixtures.baseline,

                    baselineItems =
                        fixtures.baselineItems,

                    candidates =
                        fixtures.candidates,

                    semanticValidation =
                        fixtures.semanticValidation
                )

        assertTrue(
            result.valid,
            result.blockers.joinToString()
        )

        assertEquals(
            fixtures.semanticValidation.acceptedCandidateCount,
            result.materializedEntryCount
        )

        assertEquals(
            result.baselineEntryCount +
                    result.materializedEntryCount,
            result.expandedCatalogEntryCount
        )

        assertEquals(
            0,
            result.baselineKeyCollisionCount
        )

        assertEquals(
            0,
            result.expansionKeyCollisionCount
        )

        assertTrue(result.completeCandidateCoverage)
        assertTrue(result.exactCatalogArithmeticValid)
        assertTrue(result.uniqueNormalizedKeys)
        assertTrue(result.deterministicOrderValid)

        assertTrue(
            CanonicalExpandedCatalogItemOrder
                .isSorted(
                    result.expandedCatalogItems
                ),
            "Expanded catalog must use the canonical deterministic catalog order."
        )
    }

    @Test
    fun generateDeterministically() {
        val fixtures =
            readFixtures()

        val generator =
            CanonicalApprovedCatalogExpansionGenerator()

        val first =
            generator.generate(
                fixtures.baseline,
                fixtures.baselineItems,
                fixtures.candidates,
                fixtures.semanticValidation
            )

        val second =
            generator.generate(
                fixtures.baseline,
                fixtures.baselineItems,
                fixtures.candidates,
                fixtures.semanticValidation
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
                            BASELINE_METADATA_PATH
                        )
                    ),

            baselineItems =
                CanonicalNormalizedCatalogReader()
                    .read(
                        File(
                            projectDirectory,
                            NORMALIZED_BASELINE_PATH
                        )
                    ),

            candidates =
                CanonicalCatalogExpansionCandidateReader()
                    .read(
                        File(
                            projectDirectory,
                            CANDIDATE_PATH
                        )
                    ),

            semanticValidation =
                CanonicalCatalogExpansionSemanticValidationReader()
                    .read(
                        File(
                            projectDirectory,
                            SEMANTIC_VALIDATION_PATH
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
                BASELINE_METADATA_PATH
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

    private data class Fixtures(
        val baseline:
        de.shopme.testing.system.tools.knowledge.catalog.baseline
        .CanonicalFoodCatalogBaseline,

        val baselineItems:
        List<
                de.shopme.testing.system.tools.knowledge.catalog.model
                .CatalogFoodItem
                >,

        val candidates:
        de.shopme.testing.system.tools.knowledge.catalog.expansion
        .candidate
        .CanonicalCatalogExpansionCandidateGenerationResult,

        val semanticValidation:
        de.shopme.testing.system.tools.knowledge.catalog.expansion
        .semantic
        .CanonicalCatalogExpansionSemanticValidationResult
    )

    private companion object {
        const val BASELINE_METADATA_PATH =
            "data/generated/knowledge/catalog/baseline/" +
                    "canonical-food-catalog-baseline.json"

        const val NORMALIZED_BASELINE_PATH =
            "data/generated/knowledge/catalog/normalized/" +
                    "catalog.normalized.json"

        const val CANDIDATE_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-expansion-candidates.json"

        const val SEMANTIC_VALIDATION_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-catalog-expansion-semantic-validation.json"
    }
}