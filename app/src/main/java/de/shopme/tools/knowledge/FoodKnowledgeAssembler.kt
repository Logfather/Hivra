package de.shopme.tools.knowledge

import de.shopme.domain.food.FoodKnowledgeEntry
import de.shopme.tools.knowledge.compiler.CompilerContext

class FoodKnowledgeAssembler :
    FoodKnowledgeBuilder {

    override fun build(
        context: CompilerContext
    ): FoodKnowledgeEntry {

        return FoodKnowledgeEntry(

            inputName = context.inputName,

            normalizedName = context.normalizedName,

            category = context.foodCategory,

            tags = context.tags.toSet(),

            nutritionReference = context.nutritionReference,

            carbonReference = context.carbonReference,

            glycemicIndex = context.glycemicIndex,

            production = context.production,

            allergens = context.allergens.toSet(),

            dietClassifications = context.dietClassifications.toSet(),

            seasonality = context.seasonality.toList(),

            carbonFootprint = context.carbonFootprint,

            ingredients = context.ingredients.toSet(),

            ingredientGraph = context.ingredientGraph,

            recipeGraph = context.recipeGraph,

            recipes = context.recipes.toSet(),

            taxonomyPaths =
                context.taxonomyPaths
                    .map { path ->
                        path.toList()
                    },

            taxonomyPath =
                context.taxonomyPath.toList(),

            waterFootprint =
                context.waterFootprint,

            biodiversity = context.biodiversity,

            pollinator = context.pollinator,

            foodMiles = context.foodMiles,

            packaging = context.packaging,

            processing = context.processing,

            locality = context.locality,

            nutritionFacts = context.nutritionFacts,

            animalWelfare = context.animalWelfare,

            fairTrade = context.fairTrade,

            pesticide = context.pesticide,

            waterStress = context.waterStress,

            nutriScore = context.nutriScore,

            carbonImpact = context.carbonImpact

        )

    }

}