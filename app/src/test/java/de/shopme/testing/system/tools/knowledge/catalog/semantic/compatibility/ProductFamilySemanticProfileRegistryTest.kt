package de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility

import kotlin.test.Test
import kotlin.test.assertEquals

class ProductFamilySemanticProfileRegistryTest {

    @Test
    fun resolvesKnownProfiles() {

        assertEquals(
            ProductFamilySemanticProfile.JUICE,
            ProductFamilySemanticProfileRegistry
                .profileFor("Fruchtsaft")
        )

        assertEquals(
            ProductFamilySemanticProfile.WATER,
            ProductFamilySemanticProfileRegistry
                .profileFor("Mineralwasser")
        )

        assertEquals(
            ProductFamilySemanticProfile.RAW_MEAT,
            ProductFamilySemanticProfileRegistry
                .profileFor("Hähnchenfleisch")
        )

        assertEquals(
            ProductFamilySemanticProfile.CHEESE,
            ProductFamilySemanticProfileRegistry
                .profileFor("Hartkäse")
        )

        assertEquals(
            ProductFamilySemanticProfile.SPICE,
            ProductFamilySemanticProfileRegistry
                .profileFor("Einzelgewürze")
        )

        assertEquals(
            ProductFamilySemanticProfile.MEAT_ALTERNATIVE,
            ProductFamilySemanticProfileRegistry
                .profileFor("Fleischalternativen")
        )

        assertEquals(
            ProductFamilySemanticProfile.LEGUME,
            ProductFamilySemanticProfileRegistry
                .profileFor("Linsen")
        )

        assertEquals(
            ProductFamilySemanticProfile.FRUIT_SPREAD,
            ProductFamilySemanticProfileRegistry
                .profileFor("Fruchtaufstriche")
        )

        assertEquals(
            ProductFamilySemanticProfile.PROCESSED_MEAT,
            ProductFamilySemanticProfileRegistry
                .profileFor("Schinken")
        )

        assertEquals(
            ProductFamilySemanticProfile.PLANT_DRINK,
            ProductFamilySemanticProfileRegistry
                .profileFor("Sojadrinks")
        )

        assertEquals(
            ProductFamilySemanticProfile.SOUP_STEW,
            ProductFamilySemanticProfileRegistry
                .profileFor("Suppen")
        )

        assertEquals(
            ProductFamilySemanticProfile.FISH_READY_MEAL,
            ProductFamilySemanticProfileRegistry.profileFor("Fischgerichte")
        )

        assertEquals(
            ProductFamilySemanticProfile.DRESSING,
            ProductFamilySemanticProfileRegistry.profileFor("Salatdressings")
        )

        assertEquals(
            ProductFamilySemanticProfile.SEED,
            ProductFamilySemanticProfileRegistry.profileFor("Kerne und Saaten")
        )

        assertEquals(
            ProductFamilySemanticProfile.PRESERVED_FRUIT,
            ProductFamilySemanticProfileRegistry.profileFor("Obstkonserven")
        )

        assertEquals(
            ProductFamilySemanticProfile.CONFECTIONERY,
            ProductFamilySemanticProfileRegistry.profileFor("Pralinen")
        )

        assertEquals(
            ProductFamilySemanticProfile.RICE,
            ProductFamilySemanticProfileRegistry.profileFor("Basmatireis")
        )
    }
}