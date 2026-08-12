package de.shopme.testing.system.tools.knowledge.catalog.taxonomy.hierarchical

import de.shopme.tools.knowledge.catalog.taxonomy.hierarchical.HierarchicalCanonicalGermanFoodTaxonomyRegistry
import de.shopme.tools.knowledge.catalog.taxonomy.hierarchical.HierarchicalFoodTaxonomyIndex
import kotlin.test.Test
import kotlin.test.assertTrue

class HierarchicalHerbTaxonomyTest {

    private val index =
        HierarchicalFoodTaxonomyIndex(
            HierarchicalCanonicalGermanFoodTaxonomyRegistry
                .taxonomy
        )

    @Test
    fun freshHerbsBelongToVegetables() {

        assertTrue(
            index.containsPath(
                listOf(
                    "vegetables",
                    "herbs",
                    "fresh-herbs"
                )
            )
        )
    }

    @Test
    fun driedHerbsBelongToSpicesAndSeasonings() {

        assertTrue(
            index.containsPath(
                listOf(
                    "spices-and-seasonings",
                    "herbs",
                    "dried-herbs"
                )
            )
        )
    }
}