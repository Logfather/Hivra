package de.shopme.testing.system.tools.knowledge.catalog.expansion

object CanonicalFoodCatalogTargetPolicy {

    const val VERSION = 1

    val TARGET_COUNTS_BY_CATEGORY: Map<String, Int> =
        sortedMapOf(
            "bakery" to 550,
            "baking-ingredients" to 180,
            "beverages" to 850,
            "breakfast" to 250,
            "canned-food" to 450,
            "confectionery" to 450,
            "dairy" to 600,
            "fish" to 350,
            "flour" to 150,
            "fruit" to 550,
            "grains" to 250,
            "legumes" to 170,
            "meat" to 450,
            "oils" to 180,
            "pasta" to 280,
            "plant-based-alternatives" to 400,
            "plant-based-drinks" to 220,
            "ready-meals" to 800,
            "rice" to 200,
            "sauces" to 400,
            "sausage" to 400,
            "snacks" to 500,
            "spices" to 350,
            "spreads" to 220,
            "vegetables" to 800
        )

    init {
        require(
            TARGET_COUNTS_BY_CATEGORY.values.sum() ==
                    CanonicalFoodCatalogTargetDistribution
                        .CANONICAL_TARGET_ENTRY_COUNT
        ) {
            "Canonical food catalog category targets must sum to 10,000."
        }

        require(
            TARGET_COUNTS_BY_CATEGORY.keys.all {
                it.isNotBlank() &&
                        it == it.trim() &&
                        it == it.lowercase()
            }
        )

        require(
            TARGET_COUNTS_BY_CATEGORY.values.all {
                it > 0
            }
        )
    }
}