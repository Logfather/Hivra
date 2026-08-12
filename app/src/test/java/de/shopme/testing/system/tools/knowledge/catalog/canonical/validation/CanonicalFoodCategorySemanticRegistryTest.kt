package de.shopme.testing.system.tools.knowledge.catalog.canonical.validation

import de.shopme.tools.knowledge.catalog.canonical.validation.CanonicalFoodCategorySemanticRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalFoodCategorySemanticRegistryTest {

    private val registry =
        CanonicalFoodCategorySemanticRegistry()

    @Test
    fun appleChipsDoNotResolveToFruit() {

        assertTrue(
            registry
                .expectedCategories(
                    "Apfelchips"
                )
                .isEmpty()
        )
    }

    @Test
    fun flaxOilResolvesToOilsAndNotFish() {

        val categories =
            registry
                .expectedCategories(
                    "Flachsöl"
                )

        assertEquals(
            setOf(
                "oils"
            ),
            categories
        )

        assertTrue(
            "fish" !in
                    categories
        )
    }

    @Test
    fun carrotJuiceResolvesToBeverages() {

        assertEquals(
            setOf(
                "beverages"
            ),
            registry
                .expectedCategories(
                    "Karottensaft"
                )
        )
    }

    @Test
    fun breadSpreadResolvesToSpreads() {

        assertEquals(
            setOf(
                "spreads",
                "confectionery"
            ),
            registry
                .expectedCategories(
                    "Brotaufstrich Tomate"
                )
        )
    }

    @Test
    fun plantBasedYogurtDoesNotResolveToDairy() {

        assertTrue(
            "plant-based-alternatives" in
                    registry
                        .expectedCategories(
                            "Haferjoghurt"
                        )
        )
    }

    @Test
    fun currywurstReadyMealResolvesToReadyMeals() {

        assertEquals(
            setOf(
                "ready-meals"
            ),
            registry
                .expectedCategories(
                    "Currywurst mit Pommes Fertiggericht"
                )
        )
    }
}