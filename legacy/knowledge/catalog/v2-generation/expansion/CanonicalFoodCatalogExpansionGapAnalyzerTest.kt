package de.shopme.testing.system.tools.knowledge.catalog.expansion.analysis

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaseline
import de.shopme.testing.system.tools.knowledge.catalog.baseline.CatalogBaselineArtifactReference
import de.shopme.testing.system.tools.knowledge.catalog.expansion.CanonicalFoodCatalogCategoryTarget
import de.shopme.testing.system.tools.knowledge.catalog.expansion.CanonicalFoodCatalogCategoryTargetStatus
import de.shopme.testing.system.tools.knowledge.catalog.expansion.CanonicalFoodCatalogTargetDistribution
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalFoodCatalogExpansionGapAnalyzerTest {

    private val analyzer =
        CanonicalFoodCatalogExpansionGapAnalyzer()

    @Test
    fun rankCategoriesByAbsoluteExpansionGap() {
        val baseline =
            baseline(
                categoryCounts =
                    sortedMapOf(
                        "bakery" to 100,
                        "beverages" to 100,
                        "vegetables" to 100
                    )
            )

        val distribution =
            distribution(
                baseline = baseline,
                targets =
                    sortedMapOf(
                        "bakery" to 200,
                        "beverages" to 500,
                        "vegetables" to 300
                    )
            )

        val result =
            analyzer.analyze(
                baseline = baseline,
                targetDistribution = distribution
            )

        assertEquals(
            listOf(
                "beverages",
                "vegetables",
                "bakery"
            ),
            result.categories.map {
                it.category
            }
        )

        assertEquals(
            listOf(1, 2, 3),
            result.categories.map {
                it.gapRank
            }
        )

        assertEquals(
            "beverages",
            result.largestAbsoluteGapCategory
        )

        assertTrue(result.valid)
    }

    @Test
    fun analyzeDeterministically() {
        val baseline =
            baseline(
                categoryCounts =
                    sortedMapOf(
                        "bakery" to 100,
                        "beverages" to 100
                    )
            )

        val distribution =
            distribution(
                baseline = baseline,
                targets =
                    sortedMapOf(
                        "bakery" to 300,
                        "beverages" to 500
                    )
            )

        val first =
            analyzer.analyze(
                baseline,
                distribution
            )

        val second =
            analyzer.analyze(
                baseline,
                distribution
            )

        assertEquals(first, second)
    }

    private fun baseline(
        categoryCounts: Map<String, Int>
    ): CanonicalFoodCatalogBaseline {
        val entryCount =
            categoryCounts.values.sum()

        return CanonicalFoodCatalogBaseline(
            version = 1,

            baselineId =
                "canonical-food-catalog-v1-" +
                        "0123456789abcdef",

            catalogArtifact =
                CatalogBaselineArtifactReference(
                    relativePath =
                        "data/generated/catalog.json",
                    byteCount = 1L,
                    sha256 = "0".repeat(64)
                ),

            normalizedCatalogEntryCount =
                entryCount,

            canonicalCategoryCount =
                categoryCounts.size,

            categoryCounts =
                categoryCounts.toSortedMap(),

            applicationInputEntryCount =
                entryCount,

            applicationOutputEntryCount =
                entryCount,

            duplicateMergedEntryCount = 0,
            semanticTypoRemovedEntryCount = 0,

            finalOutputEntryCount =
                entryCount,

            validationIssueCount = 0,
            normalizedCatalogValid = true,
            pipelineValid = true,

            supportingArtifacts =
                sortedMapOf(
                    "fixture" to
                            CatalogBaselineArtifactReference(
                                relativePath =
                                    "data/generated/fixture.json",
                                byteCount = 1L,
                                sha256 = "1".repeat(64)
                            )
                ),

            valid = true
        )
    }

    private fun distribution(
        baseline: CanonicalFoodCatalogBaseline,
        targets: Map<String, Int>
    ): CanonicalFoodCatalogTargetDistribution {
        val targetCount =
            targets.values.sum()

        val requiredExpansion =
            targetCount -
                    baseline.finalOutputEntryCount

        val categories =
            targets.map { (category, target) ->
                val baselineCount =
                    baseline.categoryCounts[
                        category
                    ] ?: 0

                val expansion =
                    target - baselineCount

                CanonicalFoodCatalogCategoryTarget(
                    category = category,
                    baselineEntryCount =
                        baselineCount,
                    targetEntryCount =
                        target,
                    expansionEntryCount =
                        expansion,
                    targetShare =
                        target.toDouble() /
                                targetCount.toDouble(),
                    expansionShare =
                        expansion.toDouble() /
                                requiredExpansion.toDouble(),
                    status =
                        CanonicalFoodCatalogCategoryTargetStatus
                            .EXPANSION_REQUIRED
                )
            }.sortedBy {
                it.category
            }

        return CanonicalFoodCatalogTargetDistribution(
            version = 1,

            sourceBaselineId =
                baseline.baselineId,

            sourceBaselineCatalogSha256 =
                baseline.catalogArtifact.sha256,

            baselineEntryCount =
                baseline.finalOutputEntryCount,

            targetEntryCount =
                targetCount,

            requiredExpansionEntryCount =
                requiredExpansion,

            baselineCategoryCount =
                baseline.canonicalCategoryCount,

            targetCategoryCount =
                categories.size,

            categories =
                categories,

            targetCountsByCategory =
                targets.toSortedMap(),

            baselineCountsByCategory =
                baseline.categoryCounts.toSortedMap(),

            expansionCountsByCategory =
                categories.associate {
                    it.category to
                            it.expansionEntryCount
                }.toSortedMap(),

            categoriesRequiringExpansionCount =
                categories.size,

            categoriesAtTargetCount = 0,
            categoriesExceedingTargetCount = 0,

            unallocatedBaselineCategories =
                emptyMap(),

            blockers =
                emptyList(),

            valid = true
        )
    }
}