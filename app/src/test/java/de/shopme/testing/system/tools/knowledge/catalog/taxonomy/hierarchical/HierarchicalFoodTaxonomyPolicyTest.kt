package de.shopme.testing.system.tools.knowledge.catalog.taxonomy.hierarchical

import de.shopme.tools.knowledge.catalog.taxonomy.hierarchical.HierarchicalCanonicalGermanFoodTaxonomyRegistry
import de.shopme.tools.knowledge.catalog.taxonomy.hierarchical.HierarchicalFoodTaxonomyIndex
import kotlin.test.Test
import kotlin.test.assertTrue

class HierarchicalFoodTaxonomyPolicyTest {

    private val index =
        HierarchicalFoodTaxonomyIndex(
            HierarchicalCanonicalGermanFoodTaxonomyRegistry
                .taxonomy
        )

    @Test
    fun greenSpeltIsIndependentGrainIdentity() {

        assertTrue(
            index.containsPath(
                listOf(
                    "grains-rice-and-legumes",
                    "grains",
                    "green-spelt"
                )
            )
        )

        assertTrue(
            index.containsPath(
                listOf(
                    "grains-rice-and-legumes",
                    "grains",
                    "spelt"
                )
            )
        )
    }

    @Test
    fun saltwaterFishGroupExistsForEelAssignment() {

        assertTrue(
            index.containsPath(
                listOf(
                    "fish-and-seafood",
                    "saltwater-fish"
                )
            )
        )
    }
}