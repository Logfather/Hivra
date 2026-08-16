package de.shopme.tools.knowledge.dimension

import de.shopme.tools.knowledge.dimension.capabilities.AllergenCapability
import de.shopme.tools.knowledge.dimension.capabilities.AnimalWelfareCapability
import de.shopme.tools.knowledge.dimension.capabilities.CarbonCapability
import de.shopme.tools.knowledge.dimension.capabilities.DietCapability
import de.shopme.tools.knowledge.dimension.capabilities.FoodMilesCapability
import de.shopme.tools.knowledge.dimension.capabilities.FoodTaxonomyCapability
import de.shopme.tools.knowledge.dimension.capabilities.NutriScoreCapability
import de.shopme.tools.knowledge.dimension.capabilities.PesticideCapability
import de.shopme.tools.knowledge.dimension.capabilities.ProcessingCapability
import de.shopme.tools.knowledge.dimension.capabilities.WaterCapability
import de.shopme.tools.knowledge.dimension.capabilities.WaterStressCapability

/**
 * Productive Food Knowledge capability registry.
 *
 * This registry contains only dimensions that belong to the currently active
 * Product-Only Food Knowledge scope.
 *
 * Inactive historical capabilities remain available in source code and may
 * be reactivated later, but they must not participate in the productive
 * Knowledge Explorer / Food Intelligence capability set.
 */
object DefaultKnowledgeDimensionRegistry {

    fun create() =
        KnowledgeDimensionRegistry(
            listOf(
                NutritionCapability(),

                CarbonCapability(),

                WaterCapability(),

                WaterStressCapability(),

                AllergenCapability(),

                FoodTaxonomyCapability(),

                ProcessingCapability(),

                FoodMilesCapability(),

                NutriScoreCapability(),

                DietCapability(),

                PesticideCapability(),

                AnimalWelfareCapability()
            )
        )
}