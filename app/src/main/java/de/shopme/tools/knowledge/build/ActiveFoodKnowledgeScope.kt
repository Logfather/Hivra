package de.shopme.tools.knowledge.build

/**
 * Productive Food Knowledge artifact scope.
 *
 * This is the single authority for Knowledge dimensions that currently
 * participate in the Product-Only Food Knowledge build.
 *
 * Legacy artifacts remain known explicitly so they can be archived and
 * potentially reactivated later, but they must not participate in productive
 * Server -> Runtime generation.
 */
object ActiveFoodKnowledgeScope {

    val activeArtifacts: Set<String> =
        sortedSetOf(
            "allergens.json",
            "animal_welfare.json",
            "diet_classification.json",
            "environmental_impact.json",
            "food_miles.json",
            "food_taxonomy.json",
            "nutri_score.json",
            "nutrition.json",
            "pesticides.json",
            "processing.json",
            "water_footprint.json",
            "water_stress.json"
        )

    val legacyArtifacts: Set<String> =
        sortedSetOf(
            "biodiversity.json",
            "fairtrade.json",
            "ingredient_graph.json",
            "ingredients.json",
            "locality.json",
            "packaging.json",
            "pollinator.json",
            "production.json",
            "recipe_graph.json",
            "recipes.json",
            "seasonality.json"
        )

    init {
        require(
            activeArtifacts.intersect(
                legacyArtifacts
            ).isEmpty()
        ) {
            "Active and legacy Food Knowledge artifact scopes overlap."
        }
    }

    fun isActive(
        fileName: String
    ): Boolean =
        fileName in activeArtifacts

    fun isLegacy(
        fileName: String
    ): Boolean =
        fileName in legacyArtifacts
}
