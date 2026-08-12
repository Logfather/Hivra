package de.shopme.testing.system.tools.knowledge.catalog.canonical.rebuild.base

import de.shopme.tools.knowledge.catalog.canonical.rebuild.base.CanonicalFoodBaseIdentityResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CanonicalFoodBaseIdentityResolverTest {

    private val resolver =
        CanonicalFoodBaseIdentityResolver()

    @Test
    fun frozenStrawberriesResolveToStrawberries() {

        val result =
            resolver.resolve(
                itemname =
                    "Gefrorene Erdbeeren",
                category =
                    "fruit"
            )

        assertFalse(
            result.rejected
        )

        assertEquals(
            "Erdbeeren",
            result.itemname
        )

        assertEquals(
            "erdbeeren",
            result.normalized
        )

        assertEquals(
            listOf(
                "tiefgekühlt"
            ),
            result.extractedVariants
        )
    }

    @Test
    fun frozenCocktailTomatoesAreRejected() {

        val result =
            resolver.resolve(
                itemname =
                    "Gefrorene Cocktailtomaten",
                category =
                    "ready-meals"
            )

        assertTrue(
            result.rejected
        )

        assertEquals(
            "Cocktailtomaten",
            result.itemname
        )

        assertEquals(
            listOf(
                "tiefgekühlt"
            ),
            result.extractedVariants
        )
    }

    @Test
    fun organicRyeBreadResolvesToRyeBreadWithoutBioVariant() {

        val result =
            resolver.resolve(
                itemname =
                    "Roggenbrot Bio",
                category =
                    "bakery"
            )

        assertFalse(
            result.rejected
        )

        assertEquals(
            "Roggenbrot",
            result.itemname
        )

        assertTrue(
            result.extractedVariants
                .isEmpty()
        )
    }

    @Test
    fun standardColaResolvesToColaWithoutVariant() {

        val result =
            resolver.resolve(
                itemname =
                    "Cola Standard",
                category =
                    "beverages"
            )

        assertFalse(
            result.rejected
        )

        assertEquals(
            "Cola",
            result.itemname
        )

        assertTrue(
            result.extractedVariants
                .isEmpty()
        )
    }
}