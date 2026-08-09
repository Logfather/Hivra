package de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility

import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantTaxonomyRegistry

object ProductFamilySemanticProfileRegistry {

    private val explicitProfiles =
        mapOf(

            // Dairy
            key("Butter") to
                    ProductFamilySemanticProfile.FAT_SPREAD,

            key("Frischkäse") to
                    ProductFamilySemanticProfile.FRESH_DAIRY,

            key("Joghurt") to
                    ProductFamilySemanticProfile.FRESH_DAIRY,

            key("Hartkäse") to
                    ProductFamilySemanticProfile.CHEESE,

            key("Schnittkäse") to
                    ProductFamilySemanticProfile.CHEESE,

            // Bakery
            key("Brot") to
                    ProductFamilySemanticProfile.BAKED_GOOD,

            key("Baguette") to
                    ProductFamilySemanticProfile.BAKED_GOOD,

            key("Brötchen") to
                    ProductFamilySemanticProfile.BAKED_GOOD,

            key("Knäckebrot") to
                    ProductFamilySemanticProfile.BAKED_GOOD,

            key("Toastbrot") to
                    ProductFamilySemanticProfile.BAKED_GOOD,

            key("Cracker") to
                    ProductFamilySemanticProfile.BAKED_GOOD,

            // Beverages
            key("Fruchtsaft") to
                    ProductFamilySemanticProfile.JUICE,

            key("Mineralwasser") to
                    ProductFamilySemanticProfile.WATER,

            key("Limonade") to
                    ProductFamilySemanticProfile.BEVERAGE,

            key("Kaffee") to
                    ProductFamilySemanticProfile.BEVERAGE,

            key("Tee") to
                    ProductFamilySemanticProfile.BEVERAGE,

            key("Haferdrinks") to
                    ProductFamilySemanticProfile.BEVERAGE,

            // Meat
            key("Hähnchenfleisch") to
                    ProductFamilySemanticProfile.RAW_MEAT,

            key("Putenfleisch") to
                    ProductFamilySemanticProfile.RAW_MEAT,

            key("Kalbfleisch") to
                    ProductFamilySemanticProfile.RAW_MEAT,

            key("Lammfleisch") to
                    ProductFamilySemanticProfile.RAW_MEAT,

            key("Brühwurst") to
                    ProductFamilySemanticProfile.PROCESSED_MEAT,

            key("Rohwurst") to
                    ProductFamilySemanticProfile.PROCESSED_MEAT,

            // Fish
            key("Frischfisch") to
                    ProductFamilySemanticProfile.FRESH_FISH,

            key("Fischfilets") to
                    ProductFamilySemanticProfile.FRESH_FISH,

            key("Fischkonserven") to
                    ProductFamilySemanticProfile.PROCESSED_FISH,

            // Cereals / pasta
            key("Müsli") to
                    ProductFamilySemanticProfile.CEREAL,

            key("Frühstückscerealien") to
                    ProductFamilySemanticProfile.CEREAL,

            key("Weizennudeln") to
                    ProductFamilySemanticProfile.PASTA,

            key("Weizenmehl") to
                    ProductFamilySemanticProfile.FLOUR,

            // Other
            key("Kartoffelchips") to
                    ProductFamilySemanticProfile.SNACK,

            key("Milchschokolade") to
                    ProductFamilySemanticProfile.CHOCOLATE,

            key("Dunkle Schokolade") to
                    ProductFamilySemanticProfile.CHOCOLATE,

            key("Einzelgewürze") to
                    ProductFamilySemanticProfile.SPICE,

            key("Gemüsekonserven") to
                    ProductFamilySemanticProfile.PRESERVED_VEGETABLE,

            key("Pizza") to
                    ProductFamilySemanticProfile.READY_MEAL,

            key("Fleischgerichte") to
                    ProductFamilySemanticProfile.READY_MEAL,

            key("Nudelgerichte") to
                    ProductFamilySemanticProfile.READY_MEAL,

            key("Reisgerichte") to
                    ProductFamilySemanticProfile.READY_MEAL,

            key("Nüsse") to
                    ProductFamilySemanticProfile.NUT,

            // Plant alternatives
            key("Fleischalternativen") to
                    ProductFamilySemanticProfile.MEAT_ALTERNATIVE,

            key("Sojadrinks") to
                    ProductFamilySemanticProfile.PLANT_DRINK,

            // Legumes
            key("Bohnen") to
                    ProductFamilySemanticProfile.LEGUME,

            key("Linsen") to
                    ProductFamilySemanticProfile.LEGUME,

            key("Hülsenfruchtkonserven") to
                    ProductFamilySemanticProfile.PRESERVED_LEGUME,

            // Fruit spreads / sauces
            key("Fruchtaufstriche") to
                    ProductFamilySemanticProfile.FRUIT_SPREAD,

            key("Tomatensaucen") to
                    ProductFamilySemanticProfile.SAUCE,

            // Flour
            key("Dinkelmehl") to
                    ProductFamilySemanticProfile.FLOUR,

            key("Roggenmehl") to
                    ProductFamilySemanticProfile.FLOUR,

            // Breakfast
            key("Porridge") to
                    ProductFamilySemanticProfile.PORRIDGE,

            key("Süße Frühstücksprodukte") to
                    ProductFamilySemanticProfile.CEREAL,

            // Pasta
            key("Eiernudeln") to
                    ProductFamilySemanticProfile.PASTA,

            // Whole grain
            key("Weizenkörner") to
                    ProductFamilySemanticProfile.WHOLE_GRAIN,

            // Meat
            key("Wildfleisch") to
                    ProductFamilySemanticProfile.RAW_MEAT,

            key("Schinken") to
                    ProductFamilySemanticProfile.PROCESSED_MEAT,

            key("Kochwurst") to
                    ProductFamilySemanticProfile.PROCESSED_MEAT,

            key("Salami") to
                    ProductFamilySemanticProfile.PROCESSED_MEAT,

            // Seasonings
            key("Gewürzmischungen") to
                    ProductFamilySemanticProfile.SPICE,

            key("Kräuter") to
                    ProductFamilySemanticProfile.HERB,

            // Spreads
            key("Nussaufstriche") to
                    ProductFamilySemanticProfile.NUT_SPREAD,

            // Drinks
            key("Eistee") to
                    ProductFamilySemanticProfile.BEVERAGE,

            key("Colagetränke") to
                    ProductFamilySemanticProfile.BEVERAGE,

            // Bakery snacks
            key("Laugengebäck-Snacks") to
                    ProductFamilySemanticProfile.SNACK,

            // Prepared dishes
            key("Eintöpfe") to
                    ProductFamilySemanticProfile.SOUP_STEW,

            key("Suppen") to
                    ProductFamilySemanticProfile.SOUP_STEW,

            key("Gemüsegerichte") to
                    ProductFamilySemanticProfile.READY_MEAL,

            key("Internationale Gerichte") to
                    ProductFamilySemanticProfile.READY_MEAL,

            key("Kartoffelgerichte") to
                    ProductFamilySemanticProfile.READY_MEAL,

            key("Vegane Gerichte") to
                    ProductFamilySemanticProfile.READY_MEAL,

            key("Vegetarische Gerichte") to
                    ProductFamilySemanticProfile.READY_MEAL,

            key("Fischgerichte") to
                    ProductFamilySemanticProfile.FISH_READY_MEAL,

            key("Salatdressings") to
                    ProductFamilySemanticProfile.DRESSING,

            key("Kerne und Saaten") to
                    ProductFamilySemanticProfile.SEED,

            key("Maissnacks") to
                    ProductFamilySemanticProfile.SNACK,

            key("Snackmischungen") to
                    ProductFamilySemanticProfile.SNACK,

            key("Tortillachips") to
                    ProductFamilySemanticProfile.SNACK,

            key("Fruchtnektar") to
                    ProductFamilySemanticProfile.FRUIT_NECTAR,

            key("Hafer") to
                    ProductFamilySemanticProfile.WHOLE_GRAIN,

            key("Obstkonserven") to
                    ProductFamilySemanticProfile.PRESERVED_FRUIT,

            key("Gefüllte Schokolade") to
                    ProductFamilySemanticProfile.CONFECTIONERY,

            key("Pralinen") to
                    ProductFamilySemanticProfile.CONFECTIONERY,

            key("Schokoladenriegel") to
                    ProductFamilySemanticProfile.CONFECTIONERY,

            key("Fruchtgummi") to
                    ProductFamilySemanticProfile.CONFECTIONERY,

            key("Krustentiere") to
                    ProductFamilySemanticProfile.CRUSTACEAN,

            key("Erbsen") to
                    ProductFamilySemanticProfile.LEGUME,

            key("Langkornreis") to
                    ProductFamilySemanticProfile.RICE,

            key("Basmatireis") to
                    ProductFamilySemanticProfile.RICE,

            key("Streichwurst") to
                    ProductFamilySemanticProfile.PROCESSED_MEAT,

            key("Eingelegte Lebensmittel") to
                    ProductFamilySemanticProfile.PICKLED_FOOD,

            key("Fischerzeugnisse") to
                    ProductFamilySemanticProfile.SEAFOOD_PRODUCT,

            key("Asiatische Saucen") to
                    ProductFamilySemanticProfile.SAUCE,

            key("Gemüsechips") to
                    ProductFamilySemanticProfile.SNACK,

            key("Gemüseaufstriche") to
                    ProductFamilySemanticProfile.VEGETABLE_SPREAD,

            key("Schokoladenaufstriche") to
                    ProductFamilySemanticProfile.CHOCOLATE_SPREAD,

            key("Herzhafte Frühstücksprodukte") to
                    ProductFamilySemanticProfile.BREAKFAST_PRODUCT,

            key("Fertiggerichtkonserven") to
                    ProductFamilySemanticProfile.CANNED_READY_MEAL,

            key("Vollkornnudeln") to
                    ProductFamilySemanticProfile.PASTA_VARIANT,

            key("Wurstalternativen") to
                    ProductFamilySemanticProfile.MEAT_ALTERNATIVE_PROCESSED,

            key("Mandeldrinks") to
                    ProductFamilySemanticProfile.PLANT_DRINK,

            key("Popcorn") to
                    ProductFamilySemanticProfile.POPCORN,
        )

    fun profileFor(
        family: String
    ): ProductFamilySemanticProfile =
        explicitProfiles[
            key(family)
        ]
            ?: ProductFamilySemanticProfile.GENERIC

    private fun key(
        value: String
    ): String =
        SemanticVariantTaxonomyRegistry
            .normalizeLookupKey(value)
}