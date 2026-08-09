package de.shopme.testing.system.tools.knowledge.catalog.runner

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaselineReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.CanonicalFoodCatalogTargetDistributionReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.analysis.CanonicalFoodCatalogExpansionGapAnalysisReader
import de.shopme.testing.system.tools.knowledge.catalog.expansion.analysis.CanonicalFoodCatalogExpansionGapAnalysisWriter
import de.shopme.testing.system.tools.knowledge.catalog.expansion.analysis.CanonicalFoodCatalogExpansionGapAnalyzer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalFoodCatalogExpansionGapAnalysisTest {

    @Test
    fun analyzeCanonicalFoodCatalogExpansionGaps() {
        val projectDirectory =
            resolveProjectDirectory()

        val baselineFile =
            File(
                projectDirectory,
                "data/generated/knowledge/catalog/baseline/" +
                        "canonical-food-catalog-baseline.json"
            )

        val targetDistributionFile =
            File(
                projectDirectory,
                "data/generated/knowledge/catalog/expansion/" +
                        "canonical-food-catalog-target-distribution.json"
            )

        val outputFile =
            File(
                projectDirectory,
                "data/generated/knowledge/catalog/expansion/" +
                        "canonical-food-catalog-expansion-gap-analysis.json"
            )

        val baseline =
            CanonicalFoodCatalogBaselineReader()
                .read(
                    inputFile =
                        baselineFile
                )

        val targetDistribution =
            CanonicalFoodCatalogTargetDistributionReader()
                .read(
                    inputFile =
                        targetDistributionFile
                )

        val analysis =
            CanonicalFoodCatalogExpansionGapAnalyzer()
                .analyze(
                    baseline =
                        baseline,

                    targetDistribution =
                        targetDistribution
                )

        assertTrue(
            analysis.valid,
            "Canonical catalog expansion gap analysis is invalid: " +
                    analysis.blockers.joinToString()
        )

        CanonicalFoodCatalogExpansionGapAnalysisWriter()
            .write(
                analysis =
                    analysis,
                outputFile =
                    outputFile
            )

        val persisted =
            CanonicalFoodCatalogExpansionGapAnalysisReader()
                .read(
                    inputFile =
                        outputFile
                )

        assertEquals(
            analysis,
            persisted,
            "Persisted expansion gap analysis differs from generated result."
        )

        assertEquals(
            expected = 3_419,
            actual =
                persisted.baselineEntryCount
        )

        assertEquals(
            expected = 10_000,
            actual =
                persisted.targetEntryCount
        )

        assertEquals(
            expected = 6_581,
            actual =
                persisted.requiredExpansionEntryCount
        )

        assertEquals(
            expected = 25,
            actual =
                persisted.analyzedCategoryCount
        )

        assertEquals(
            expected = 6_581,
            actual =
                persisted.totalExpansionCountFromCategories
        )

        assertEquals(
            expected = 10_000,
            actual =
                persisted.totalTargetCountFromCategories
        )

        println(
            buildString {
                appendLine(
                    "Canonical food catalog expansion gap analysis"
                )
                appendLine(
                    "---------------------------------------------"
                )
                appendLine(
                    "Baseline entries: " +
                            persisted.baselineEntryCount
                )
                appendLine(
                    "Target entries: " +
                            persisted.targetEntryCount
                )
                appendLine(
                    "Required expansion: " +
                            persisted.requiredExpansionEntryCount
                )
                appendLine(
                    "Analyzed categories: " +
                            persisted.analyzedCategoryCount
                )
                appendLine(
                    "Largest absolute gap: " +
                            persisted.largestAbsoluteGapCategory
                )
                appendLine(
                    "Lowest baseline coverage: " +
                            persisted.lowestBaselineCoverageCategory
                )
                appendLine(
                    "Highest growth factor: " +
                            persisted.highestGrowthFactorCategory
                )
                appendLine(
                    "Top-five expansion entries: " +
                            persisted.topFiveExpansionEntryCount
                )
                appendLine(
                    "Top-five expansion share: " +
                            persisted.topFiveExpansionShare
                )
                appendLine(
                    "Categories below 25% coverage: " +
                            persisted
                                .categoriesBelowTwentyFivePercentCoverage
                )
                appendLine(
                    "Categories below 50% coverage: " +
                            persisted
                                .categoriesBelowFiftyPercentCoverage
                )
                append(
                    "Analysis valid: " +
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

        val relativeBaselinePath =
            "data/generated/knowledge/catalog/baseline/" +
                    "canonical-food-catalog-baseline.json"

        return when {
            File(
                workingDirectory,
                relativeBaselinePath
            ).isFile ->
                workingDirectory

            workingDirectory.name == "app" &&
                    File(
                        requireNotNull(
                            workingDirectory.parentFile
                        ),
                        relativeBaselinePath
                    ).isFile ->
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
}