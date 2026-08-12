package de.shopme.tools.knowledge.compiler

import de.shopme.domain.catalog.CatalogItem
import de.shopme.domain.food.FoodCategory
import de.shopme.domain.food.FoodTag
import de.shopme.domain.food.GlycemicIndexLevel
import de.shopme.tools.knowledge.allergen.Allergen
import de.shopme.tools.knowledge.animalwelfare.AnimalWelfare
import de.shopme.tools.knowledge.biodiversity.BiodiversityScore
import de.shopme.tools.knowledge.carbon.CarbonFootprint
import de.shopme.tools.knowledge.carbon.CarbonImpactLevel
import de.shopme.tools.knowledge.diet.DietClassification
import de.shopme.tools.knowledge.fairtrade.FairTrade
import de.shopme.tools.knowledge.foodmiles.FoodMiles
import de.shopme.tools.knowledge.ingredientgraph.IngredientGraphEntry
import de.shopme.tools.knowledge.locality.Locality
import de.shopme.tools.knowledge.nutriscore.NutriScore
import de.shopme.tools.knowledge.nutrition.NutritionFacts
import de.shopme.tools.knowledge.packaging.Packaging
import de.shopme.tools.knowledge.pesticides.Pesticide
import de.shopme.tools.knowledge.pollinator.PollinatorScore
import de.shopme.tools.knowledge.processing.ProcessingLevel
import de.shopme.tools.knowledge.production.ProductionMethod
import de.shopme.tools.knowledge.recipegraph.RecipeGraphEntry
import de.shopme.tools.knowledge.taxonomy.FoodTaxonomyEntry
import de.shopme.tools.knowledge.waterfootprint.WaterFootprint
import de.shopme.tools.knowledge.waterstress.WaterStress

data class CompilerContext(


    val catalogItem: CatalogItem,

    val inputName: String =
        catalogItem.itemname,

    var normalizedName: String =
        catalogItem.normalized,

    /**
     * Authoritative canonical taxonomy paths from the Product-Only master.
     *
     * A canonical food may belong to multiple taxonomy branches.
     */
    val taxonomyPaths: MutableList<List<String>> =
        catalogItem.taxonomyPaths
            .map { path ->
                path.toList()
            }
            .toMutableList(),

    /**
     * Legacy single-path projection.
     *
     * Kept temporarily for consumers that still expect exactly one taxonomy path.
     * Must never be used as the canonical taxonomy authority.
     */
    val taxonomyPath: MutableList<String> =
        catalogItem.taxonomyPaths
            .firstOrNull()
            .orEmpty()
            .toMutableList(),

    /**
     * Canonical supermarket category resolved from
     * food_taxonomy.json
     *
     * Example:
     * "Obst & Gemüse"
     */
    var supermarketCategory: String? = null,

    /**
     * Semantic domain category.
     *
     * Example:
     * FoodCategory.FRUIT
     */
    var foodCategory: FoodCategory =
        FoodCategory.UNKNOWN,

    /**
     * Semantic tags accumulated by compiler passes.
     */
    val tags: MutableSet<FoodTag> =
        mutableSetOf(),

    /**
     * Canonical nutrition reference.
     * Will later be enriched by dedicated compiler passes.
     */
    var nutritionReference: String? =
        catalogItem.nutritionKnowledgeReference(),

    /**
     * Nutrition facts resolved by NutritionCompilerPass.
     */
    var nutritionFacts: NutritionFacts? = null,

    /**
     * Production type copied from catalog and available
     * for later compiler passes.
     */
    var production: MutableSet<ProductionMethod> =
        mutableSetOf(),

    /**
     * Glycemic index copied from catalog and available
     * for later compiler passes.
     */
    var glycemicIndex: GlycemicIndexLevel =
        GlycemicIndexLevel.UNKNOWN,

    /**
     * Allergens copied from catalog and available
     * for later compiler passes.
     */
    val allergens: MutableSet<Allergen> =
        mutableSetOf(),

    /**
     * Dietary classifications accumulated by compiler passes.
     */
    val dietClassifications: MutableSet<DietClassification> =
        mutableSetOf(),

    /**
     * Seasonality resolved by SeasonalityCompilerPass.
     */
    val seasonality: MutableList<Int> =
        mutableListOf(),

    /**
     * Carbon footprint resolved by CarbonCompilerPass.
     */
    var carbonFootprint: CarbonFootprint? =
        null,

    var carbonReference: String? =
        null,

    /**
     * Ingredients resolved by IngredientCompilerPass.
     */
    var ingredients: MutableSet<String> =
        mutableSetOf(),

    /**
     * Recipes resolved by RecipeCompilerPass.
     */
    val recipes: MutableSet<String> =
        mutableSetOf(),

    /**
     * Water footprint resolved by WaterCompilerPass.
     */

    var waterFootprint: WaterFootprint? = null,

    /**
     * Biodiversity score resolved by BiodiversityCompilerPass.
     */
    var biodiversity: BiodiversityScore? = null,

    /**
     * Pollinator score resolved by PollinatorCompilerPass.
     */

    var pollinator: PollinatorScore? = null,

    /**
     * Food miles resolved by FoodMilesCompilerPass.
     */

    var foodMiles: FoodMiles? = null,

    /**
     * Packaging score resolved by PackagingCompilerPass.
     */
    var packaging: Packaging? = null,

    /**
     * Processing level resolved by ProcessingCompilerPass.
     */
    var processing: ProcessingLevel? = null,

    /**
     * Locality resolved by LocalityCompilerPass.
     */
    var locality: Locality? = null,

    /**
     * Fair Trade score resolved by FairTradeCompilerPass.
     */
    var fairTrade: FairTrade? = null,

    /**
     * Animal welfare score resolved by AnimalWelfareCompilerPass.
     */
    var animalWelfare: AnimalWelfare? = null,

    /**
     * Pesticide score resolved by PesticideCompilerPass.
     */
    var pesticide: Pesticide? = null,

    /**
     * Water stress score resolved by WaterStressCompilerPass.
     */
    var waterStress: WaterStress? = null,

    /**
     * Water stress score resolved by WaterStressCompilerPass.
     */
    var foodTaxonomy: FoodTaxonomyEntry? = null,

    /**
     * Ingredient graph resolved by IngredientGraphCompilerPass.
     */
    var ingredientGraph: IngredientGraphEntry? = null,

    /**
     * Recipe graph resolved by RecipeGraphCompilerPass.
     */
    var recipeGraph: RecipeGraphEntry? = null,

    /**
     * Carbon impact resolved by CarbonImpactCompilerPass.
     */
    var carbonImpact: CarbonImpactLevel? = null,

    /**
     * NutriScore resolved by NutriScoreCompilerPass.
     */
    var nutriScore: NutriScore? = null,

    )