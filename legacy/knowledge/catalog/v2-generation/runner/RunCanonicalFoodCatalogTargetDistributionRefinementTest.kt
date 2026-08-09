package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.category.CanonicalFoodCategoryRegistry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.CanonicalFoodCatalogTargetDistributionReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.refinement.CanonicalFoodCatalogRefinedTargetDistributionReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.refinement.CanonicalFoodCatalogRefinedTargetDistributionWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.refinement.CanonicalFoodCatalogTargetDistributionRefiner
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalFoodCatalogTargetDistributionRefinementTest {

    @Test
    fun validateAndRefineCanonicalTargetDistribution() {
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

        val sourceDistribution =
            CanonicalFoodCatalogTargetDistributionReader()
                .read(
                    File(
                        projectDirectory,
                        SOURCE_DISTRIBUTION_PATH
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
            "Refined target distribution is invalid: " +
                    refined.blockers.joinToString()
        )

        val outputFile =
            File(
                projectDirectory,
                OUTPUT_PATH
            )

        CanonicalFoodCatalogRefinedTargetDistributionWriter()
            .write(
                distribution = refined,
                outputFile = outputFile
            )

        val persisted =
            CanonicalFoodCatalogRefinedTargetDistributionReader()
                .read(outputFile)

        assertEquals(refined, persisted)

        assertEquals(10_000, persisted.refinedTargetEntryCount)
        assertEquals(6_581, persisted.requiredExpansionEntryCount)
        assertEquals(25, persisted.categoryCount)
        assertEquals(0, persisted.changedCategoryCount)

        println(
            buildString {
                appendLine(
                    "Canonical 10,000-item target refinement"
                )
                appendLine(
                    "---------------------------------------"
                )
                appendLine(
                    "Catalog concept: " +
                            persisted.methodology.catalogConcept
                )
                appendLine(
                    "Baseline entries: " +
                            persisted.baselineEntryCount
                )
                appendLine(
                    "Refined target entries: " +
                            persisted.refinedTargetEntryCount
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
                    "Changed targets: " +
                            persisted.changedCategoryCount
                )
                appendLine(
                    "Validation issues: " +
                            persisted.validationIssueCount
                )
                append(
                    "Distribution valid: " +
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

        const val SOURCE_DISTRIBUTION_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-food-catalog-target-distribution.json"

        const val OUTPUT_PATH =
            "data/generated/knowledge/catalog/expansion/" +
                    "canonical-food-catalog-refined-target-distribution.json"
    }
}