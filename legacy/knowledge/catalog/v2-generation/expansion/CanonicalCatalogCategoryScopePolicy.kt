package de.shopme.testing.system.tools.knowledge.catalog.expansion.refinement

object CanonicalCatalogCategoryScopePolicy {

    private val COMMON_EXCLUDED_SKU_AXES =
        CanonicalCatalogExcludedSkuAxis.entries
            .sortedBy { it.name }

    private val DEFAULT_IDENTITY_AXES =
        listOf(
            CanonicalCatalogIdentityAxis.FOOD_TYPE,
            CanonicalCatalogIdentityAxis.PRIMARY_INGREDIENT,
            CanonicalCatalogIdentityAxis.RECIPE_TYPE,
            CanonicalCatalogIdentityAxis.PROCESSING_METHOD,
            CanonicalCatalogIdentityAxis.PRESERVATION_METHOD,
            CanonicalCatalogIdentityAxis.PHYSICAL_FORM,
            CanonicalCatalogIdentityAxis.PREPARATION_STATE,
            CanonicalCatalogIdentityAxis.FLAVOR_PROFILE,
            CanonicalCatalogIdentityAxis.DIETARY_FORM,
            CanonicalCatalogIdentityAxis.ALLERGEN_RELEVANT_VARIANT,
            CanonicalCatalogIdentityAxis
                .NUTRITIONALLY_RELEVANT_VARIANT
        ).sortedBy { it.name }

    private val CATEGORY_SPECIFIC_AXES:
            Map<String, List<CanonicalCatalogIdentityAxis>> =
        sortedMapOf(
            "bakery" to
                    axes(
                        CanonicalCatalogIdentityAxis.GRAIN_TYPE,
                        CanonicalCatalogIdentityAxis.RECIPE_TYPE,
                        CanonicalCatalogIdentityAxis.PROCESSING_METHOD
                    ),

            "baking-ingredients" to
                    axes(
                        CanonicalCatalogIdentityAxis.FOOD_TYPE,
                        CanonicalCatalogIdentityAxis.PRIMARY_INGREDIENT,
                        CanonicalCatalogIdentityAxis.PHYSICAL_FORM
                    ),

            "beverages" to
                    axes(
                        CanonicalCatalogIdentityAxis.FOOD_TYPE,
                        CanonicalCatalogIdentityAxis.PRIMARY_INGREDIENT,
                        CanonicalCatalogIdentityAxis.FLAVOR_PROFILE,
                        CanonicalCatalogIdentityAxis.SWEETENING_TYPE,
                        CanonicalCatalogIdentityAxis
                            .NUTRITIONALLY_RELEVANT_VARIANT
                    ),

            "breakfast" to DEFAULT_IDENTITY_AXES,
            "canned-food" to DEFAULT_IDENTITY_AXES,
            "confectionery" to
                    axes(
                        CanonicalCatalogIdentityAxis.FOOD_TYPE,
                        CanonicalCatalogIdentityAxis.PRIMARY_INGREDIENT,
                        CanonicalCatalogIdentityAxis.FLAVOR_PROFILE,
                        CanonicalCatalogIdentityAxis.SWEETENING_TYPE,
                        CanonicalCatalogIdentityAxis
                            .ALLERGEN_RELEVANT_VARIANT
                    ),

            "dairy" to
                    axes(
                        CanonicalCatalogIdentityAxis.FOOD_TYPE,
                        CanonicalCatalogIdentityAxis.FAT_LEVEL,
                        CanonicalCatalogIdentityAxis.PROCESSING_METHOD,
                        CanonicalCatalogIdentityAxis.RIPENING_OR_AGING,
                        CanonicalCatalogIdentityAxis.FLAVOR_PROFILE
                    ),

            "fish" to DEFAULT_IDENTITY_AXES,
            "flour" to
                    axes(
                        CanonicalCatalogIdentityAxis.GRAIN_TYPE,
                        CanonicalCatalogIdentityAxis.PROCESSING_METHOD,
                        CanonicalCatalogIdentityAxis.PHYSICAL_FORM
                    ),

            "fruit" to
                    axes(
                        CanonicalCatalogIdentityAxis.FOOD_TYPE,
                        CanonicalCatalogIdentityAxis.PREPARATION_STATE,
                        CanonicalCatalogIdentityAxis.PRESERVATION_METHOD
                    ),

            "grains" to DEFAULT_IDENTITY_AXES,
            "legumes" to DEFAULT_IDENTITY_AXES,
            "meat" to DEFAULT_IDENTITY_AXES,
            "oils" to DEFAULT_IDENTITY_AXES,
            "pasta" to DEFAULT_IDENTITY_AXES,
            "plant-based-alternatives" to DEFAULT_IDENTITY_AXES,
            "plant-based-drinks" to DEFAULT_IDENTITY_AXES,
            "ready-meals" to DEFAULT_IDENTITY_AXES,
            "rice" to DEFAULT_IDENTITY_AXES,
            "sauces" to DEFAULT_IDENTITY_AXES,
            "sausage" to DEFAULT_IDENTITY_AXES,
            "snacks" to DEFAULT_IDENTITY_AXES,
            "spices" to DEFAULT_IDENTITY_AXES,
            "spreads" to DEFAULT_IDENTITY_AXES,

            "vegetables" to
                    axes(
                        CanonicalCatalogIdentityAxis.FOOD_TYPE,
                        CanonicalCatalogIdentityAxis.PREPARATION_STATE,
                        CanonicalCatalogIdentityAxis.PRESERVATION_METHOD,
                        CanonicalCatalogIdentityAxis.PROCESSING_METHOD
                    )
        )

    fun identityAxesFor(
        category: String
    ): List<CanonicalCatalogIdentityAxis> =
        requireNotNull(
            CATEGORY_SPECIFIC_AXES[category]
        ) {
            "No canonical identity scope exists for category '$category'."
        }

    fun excludedSkuAxesFor(
        category: String
    ): List<CanonicalCatalogExcludedSkuAxis> {
        require(category in CATEGORY_SPECIFIC_AXES) {
            "No canonical category scope exists for '$category'."
        }

        return COMMON_EXCLUDED_SKU_AXES
    }

    fun rationaleFor(
        category: String
    ): String {
        require(category in CATEGORY_SPECIFIC_AXES)

        return (
                "Category '$category' counts canonical food types and " +
                        "meaningful food variants. Brand, retailer, EAN, package " +
                        "size, price and promotion do not create separate " +
                        "canonical entries."
                )
    }

    fun supportedCategories(): Set<String> =
        CATEGORY_SPECIFIC_AXES.keys

    private fun axes(
        vararg values: CanonicalCatalogIdentityAxis
    ): List<CanonicalCatalogIdentityAxis> =
        values
            .distinct()
            .sortedBy { it.name }
}