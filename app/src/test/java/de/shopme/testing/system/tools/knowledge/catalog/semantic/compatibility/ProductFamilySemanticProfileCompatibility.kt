package de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility

import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantType

object ProductFamilySemanticProfileCompatibility {

    private val identityExcludedTypes =
        setOf(
            SemanticVariantType.NUTRITION_CLAIM,
            SemanticVariantType.ALLERGEN_CLAIM,
            SemanticVariantType.DIET_CLAIM,
            SemanticVariantType.GENERIC_PLACEHOLDER
        )

    private val allowedTypes =
        mapOf(

            ProductFamilySemanticProfile.BAKED_GOOD to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.PROCESSING,
                        SemanticVariantType.STORAGE_STATE,
                        SemanticVariantType.STYLE
                    ),

            ProductFamilySemanticProfile.RAW_MEAT to
                    setOf(
                        SemanticVariantType.CUT,
                        SemanticVariantType.FORM,
                        SemanticVariantType.PREPARATION,
                        SemanticVariantType.STORAGE_STATE
                    ),

            ProductFamilySemanticProfile.PROCESSED_MEAT to
                    setOf(
                        SemanticVariantType.PROCESSING,
                        SemanticVariantType.FORM,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.COMPOSITION
                    ),

            ProductFamilySemanticProfile.FRESH_FISH to
                    setOf(
                        SemanticVariantType.CUT,
                        SemanticVariantType.FORM,
                        SemanticVariantType.PREPARATION,
                        SemanticVariantType.STORAGE_STATE
                    ),

            ProductFamilySemanticProfile.PROCESSED_FISH to
                    setOf(
                        SemanticVariantType.PROCESSING,
                        SemanticVariantType.FORM,
                        SemanticVariantType.FLAVOR
                    ),

            ProductFamilySemanticProfile.CHEESE to
                    setOf(
                        SemanticVariantType.MATURATION,
                        SemanticVariantType.COMPOSITION,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FORM
                    ),

            ProductFamilySemanticProfile.FRESH_DAIRY to
                    setOf(
                        SemanticVariantType.COMPOSITION,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.INGREDIENT
                    ),

            ProductFamilySemanticProfile.FAT_SPREAD to
                    setOf(
                        SemanticVariantType.COMPOSITION,
                        SemanticVariantType.INGREDIENT
                    ),

            ProductFamilySemanticProfile.BEVERAGE to
                    setOf(
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.JUICE to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.WATER to
                    setOf(
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.COMPOSITION
                    ),

            ProductFamilySemanticProfile.CEREAL to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FORM,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.PASTA to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FORM
                    ),

            ProductFamilySemanticProfile.FLOUR to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FORM
                    ),

            ProductFamilySemanticProfile.NUT to
                    setOf(
                        SemanticVariantType.FORM,
                        SemanticVariantType.PROCESSING,
                        SemanticVariantType.FLAVOR
                    ),

            ProductFamilySemanticProfile.SNACK to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.CHOCOLATE to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.COMPOSITION
                    ),

            ProductFamilySemanticProfile.SPICE to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FORM
                    ),

            ProductFamilySemanticProfile.PRESERVED_VEGETABLE to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FORM,
                        SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.READY_MEAL to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.PREPARATION,
                        SemanticVariantType.STORAGE_STATE,
                        SemanticVariantType.STYLE
                    ),

            ProductFamilySemanticProfile.BAKING_INGREDIENT to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FORM,
                        SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.MEAT_ALTERNATIVE to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FORM,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.PROCESSING,
                        SemanticVariantType.STORAGE_STATE
                    ),

            ProductFamilySemanticProfile.PLANT_DRINK to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.PORRIDGE to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FORM,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.WHOLE_GRAIN to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FORM,
                        SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.LEGUME to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FORM,
                        SemanticVariantType.PROCESSING,
                        SemanticVariantType.STORAGE_STATE
                    ),

            ProductFamilySemanticProfile.PRESERVED_LEGUME to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FORM,
                        SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.NUT_SPREAD to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.TEXTURE,
                        SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.FRUIT_SPREAD to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.TEXTURE,
                        SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.SAUCE to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.TEXTURE,
                        SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.HERB to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FORM,
                        SemanticVariantType.PROCESSING,
                        SemanticVariantType.STORAGE_STATE
                    ),

            ProductFamilySemanticProfile.SOUP_STEW to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.PREPARATION,
                        SemanticVariantType.TEXTURE,
                        SemanticVariantType.STORAGE_STATE,
                        SemanticVariantType.STYLE
                    ),

            ProductFamilySemanticProfile.FISH_READY_MEAL to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.PREPARATION,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.STORAGE_STATE
                    ),

            ProductFamilySemanticProfile.DRESSING to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.TEXTURE,
                        SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.SEED to
                    setOf(
                        SemanticVariantType.FORM,
                        SemanticVariantType.PROCESSING,
                        SemanticVariantType.FLAVOR
                    ),

            ProductFamilySemanticProfile.FRUIT_NECTAR to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.PRESERVED_FRUIT to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FORM,
                        SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.CONFECTIONERY to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.TEXTURE,
                        SemanticVariantType.FORM
                    ),

            ProductFamilySemanticProfile.CRUSTACEAN to
                    setOf(
                        SemanticVariantType.FORM,
                        SemanticVariantType.PREPARATION,
                        SemanticVariantType.STORAGE_STATE
                    ),

            ProductFamilySemanticProfile.PICKLED_FOOD to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FORM,
                        SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.SEAFOOD_PRODUCT to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FORM,
                        SemanticVariantType.PROCESSING,
                        SemanticVariantType.STORAGE_STATE
                    ),

            ProductFamilySemanticProfile.VEGETABLE_SPREAD to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.TEXTURE,
                        SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.CHOCOLATE_SPREAD to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.TEXTURE
                    ),

            ProductFamilySemanticProfile.BREAKFAST_PRODUCT to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.FORM,
                        SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.CANNED_READY_MEAL to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.PREPARATION,
                        SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.PASTA_VARIANT to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FORM
                    ),

            ProductFamilySemanticProfile.MEAT_ALTERNATIVE_PROCESSED to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FORM,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.RICE to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FORM,
                        SemanticVariantType.PROCESSING
                    ),

            ProductFamilySemanticProfile.POPCORN to
                    setOf(
                        SemanticVariantType.INGREDIENT,
                        SemanticVariantType.FLAVOR,
                        SemanticVariantType.PROCESSING
                    )
        )

    fun isAllowed(
        profile: ProductFamilySemanticProfile,
        type: SemanticVariantType
    ): Boolean {

        if (type in identityExcludedTypes) {
            return false
        }

        return type in (
                allowedTypes[profile]
                    ?: emptySet()
                )
    }
}