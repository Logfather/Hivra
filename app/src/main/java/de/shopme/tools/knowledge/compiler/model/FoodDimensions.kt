package de.shopme.tools.knowledge.compiler.model

import de.shopme.domain.food.GlycemicIndexLevel
import de.shopme.tools.knowledge.animalwelfare.AnimalWelfare
import de.shopme.tools.knowledge.biodiversity.BiodiversityScore
import de.shopme.tools.knowledge.carbon.CarbonFootprint
import de.shopme.tools.knowledge.fairtrade.FairTrade
import de.shopme.tools.knowledge.locality.Locality
import de.shopme.tools.knowledge.nutrition.NutritionFacts
import de.shopme.tools.knowledge.packaging.Packaging
import de.shopme.tools.knowledge.pesticides.Pesticide
import de.shopme.tools.knowledge.pollinator.PollinatorScore
import de.shopme.tools.knowledge.waterfootprint.WaterFootprint

data class FoodDimensions(

    val nutrition: NutritionFacts? = null,

    val glycemic: GlycemicIndexLevel? = null,

    val carbon: CarbonFootprint? = null,

    val water: WaterFootprint? = null,

    val biodiversity: BiodiversityScore? = null,

    val pollinator: PollinatorScore? = null,

    val locality: Locality? = null,

    val packaging: Packaging? = null,

    val fairTrade: FairTrade? = null,

    val animalWelfare: AnimalWelfare? = null,

    val pesticide: Pesticide? = null

)