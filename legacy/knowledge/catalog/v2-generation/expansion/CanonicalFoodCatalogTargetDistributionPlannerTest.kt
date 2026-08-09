package de.shopme.testing.system.tools.knowledge.catalog.expansion

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaseline
import de.shopme.testing.system.tools.knowledge.catalog.baseline.CatalogBaselineArtifactReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalFoodCatalogTargetDistributionPlannerTest {

    private val planner =
        CanonicalFoodCatalogTargetDistributionPlanner()

    @Test
    fun defineExactTenThousandItemTarget() {
        val baseline =
            baseline(
                categoryCounts =
                    sortedMapOf(
                        "bakery" to 100,
                        "baking-ingredients" to 50,
                        "beverages" to 100,
                        "breakfast" to 50,
                        "canned-food" to 100,
                        "confectionery" to 100,
                        "dairy" to 100,
                        "fish" to 100,
                        "flour" to 50,
                        "fruit" to 100,
                        "grains" to 100,
                        "legumes" to 50,
                        "meat" to 100,
                        "oils" to 50,
                        "pasta" to 100,
                        "plant-based-alternatives" to 100,
                        "plant-based-drinks" to 50,
                        "ready-meals" to 100,
                        "rice" to 50,
                        "sauces" to 100,
                        "sausage" to 100,
                        "snacks" to 100,
                        "spices" to 100,
                        "spreads" to 50,
                        "vegetables" to 100
                    )
            )

        val result =
            planner.plan(baseline)

        assertEquals(
            10_000,
            result.targetEntryCount
        )

        assertEquals(
            10_000,
            result.targetCountsByCategory
                .values
                .sum()
        )

        assertEquals(
            result.targetEntryCount -
                    result.baselineEntryCount,
            result.requiredExpansionEntryCount
        )

        assertEquals(
            result.requiredExpansionEntryCount,
            result.expansionCountsByCategory
                .values
                .sum()
        )

        assertTrue(result.valid)
    }

    @Test
    fun planDeterministically() {
        val baseline =
            baseline(
                categoryCounts =
                    CanonicalFoodCatalogTargetPolicy
                        .TARGET_COUNTS_BY_CATEGORY
                        .mapValues { (_, target) ->
                            maxOf(target / 3, 1)
                        }
                        .toSortedMap()
            )

        val first =
            planner.plan(baseline)

        val second =
            planner.plan(baseline)

        assertEquals(first, second)

        assertEquals(
            first.categories.sortedBy {
                it.category
            },
            first.categories
        )
    }

    private fun baseline(
        categoryCounts: Map<String, Int>
    ): CanonicalFoodCatalogBaseline {
        val entryCount =
            categoryCounts.values.sum()

        return CanonicalFoodCatalogBaseline(
            version =
                CanonicalFoodCatalogBaseline
                    .CURRENT_VERSION,

            baselineId =
                "canonical-food-catalog-v1-" +
                        "0123456789abcdef",

            catalogArtifact =
                CatalogBaselineArtifactReference(
                    relativePath =
                        "data/generated/knowledge/catalog/" +
                                "normalized/catalog.normalized.json",
                    byteCount = 1L,
                    sha256 =
                        "0".repeat(64)
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
                                sha256 =
                                    "1".repeat(64)
                            )
                ),

            valid = true
        )
    }
}