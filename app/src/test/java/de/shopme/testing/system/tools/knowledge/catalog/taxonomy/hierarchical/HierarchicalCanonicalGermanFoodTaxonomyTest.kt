package de.shopme.testing.system.tools.knowledge.catalog.taxonomy.hierarchical

import de.shopme.tools.knowledge.catalog.taxonomy.hierarchical.HierarchicalCanonicalGermanFoodTaxonomyRegistry
import de.shopme.tools.knowledge.catalog.taxonomy.hierarchical.HierarchicalCanonicalGermanFoodTaxonomyValidator
import de.shopme.tools.knowledge.catalog.taxonomy.hierarchical.HierarchicalFoodTaxonomyIndex
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HierarchicalCanonicalGermanFoodTaxonomyTest {

    private val taxonomy =
        HierarchicalCanonicalGermanFoodTaxonomyRegistry
            .taxonomy

    private val index =
        HierarchicalFoodTaxonomyIndex(
            taxonomy
        )

    @Test
    fun hierarchicalTaxonomyIsValid() {

        val result =
            HierarchicalCanonicalGermanFoodTaxonomyValidator()
                .validate(
                    taxonomy
                )

        assertTrue(
            result.valid
        )

        assertTrue(
            result.maximumDepth >=
                    3
        )
    }

    @Test
    fun mediterraneanVegetablesExist() {

        assertTrue(
            index.containsPath(
                listOf(
                    "vegetables",
                    "mediterranean-vegetables",
                    "tomatoes"
                )
            )
        )

        assertTrue(
            index.containsPath(
                listOf(
                    "vegetables",
                    "mediterranean-vegetables",
                    "artichokes"
                )
            )
        )

        assertTrue(
            index.containsPath(
                listOf(
                    "vegetables",
                    "mediterranean-vegetables",
                    "avocados"
                )
            )
        )
    }

    @Test
    fun legumesAreHierarchical() {

        assertTrue(
            index.containsPath(
                listOf(
                    "grains-rice-and-legumes",
                    "legumes",
                    "lentils",
                    "red-lentils"
                )
            )
        )

        assertTrue(
            index.containsPath(
                listOf(
                    "grains-rice-and-legumes",
                    "legumes",
                    "beans",
                    "white-beans"
                )
            )
        )
    }

    @Test
    fun mincedMeatIsHierarchical() {

        assertTrue(
            index.containsPath(
                listOf(
                    "meat",
                    "minced-meat",
                    "beef-minced-meat"
                )
            )
        )

        assertTrue(
            index.containsPath(
                listOf(
                    "meat",
                    "minced-meat",
                    "mixed-minced-meat"
                )
            )
        )
    }

    @Test
    fun chocolateIsHierarchical() {

        assertTrue(
            index.containsPath(
                listOf(
                    "confectionery",
                    "chocolate",
                    "white-chocolate"
                )
            )
        )

        assertTrue(
            index.containsPath(
                listOf(
                    "confectionery",
                    "chocolate",
                    "dark-chocolate"
                )
            )
        )
    }

    @Test
    fun plantBasedFoodsIsNotGlobalDepartment() {

        assertFalse(
            taxonomy.departments
                .any {
                    it.id ==
                            "plant-based-foods"
                }
        )
    }

    @Test
    fun readyMealsIsNotGlobalDepartment() {

        assertFalse(
            taxonomy.departments
                .any {
                    it.id ==
                            "ready-meals"
                }
        )
    }

    @Test
    fun babyFoodRemainsGlobalDepartment() {

        assertTrue(
            taxonomy.departments
                .any {
                    it.id ==
                            "baby-and-toddler-food"
                }
        )
    }
}