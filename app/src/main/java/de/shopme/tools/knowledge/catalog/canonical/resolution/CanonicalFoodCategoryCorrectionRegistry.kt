package de.shopme.tools.knowledge.catalog.canonical.resolution

object CanonicalFoodCategoryCorrectionRegistry {

    private val corrections =
        mapOf(
            "gemueselasagne" to
                    "ready-meals"
        )

    fun correctedCategory(
        normalized: String,
        currentCategory: String
    ): String =
        corrections[
            normalized
        ]
            ?: currentCategory
}