package de.shopme.testing.system.tools.knowledge.multisource

import de.shopme.tools.knowledge.ai.builder.runtime.MultiSourceRuntimeKnowledgeBuild
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MultiSourceKnowledgeCandidateMergeTest {

    @Test
    fun mergeOpenFoodFactsAgribalyseAndCiqualCandidates() {
        val result =
            MultiSourceRuntimeKnowledgeBuild()
                .build(
                    offNutritionAggregateFile =
                        File(
                            "../data/generated/knowledge/references/off/" +
                                    "off-nutrition-reference-aggregates.json"
                        ),
                    offFile =
                        File(
                            "../data/preview/openfoodfacts/off-products-preview-50k.jsonl.gz"
                        ),
                    agribalyseFile =
                        File(
                            "../data/generated/agribalyse/agribalyse-foods.slim.tsv"
                        ),
                    ciqualDirectory =
                        File(
                            "../data/raw/ciqual/Ciqual"
                        ),
                    outputDir =
                        File(
                            "../data/generated/runtime"
                        ),
                    maxOffCandidates =
                        50_000,
                    maxOffNutritionAggregates =
                        50_000,
                )

        printBlockedFanoutKeys(
            result.blockedHighFanoutKeys
        )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("MULTI SOURCE RUNTIME KNOWLEDGE BUILD")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("OFF candidates=${result.offCandidateCount}")
        println("Agribalyse candidates=${result.agribalyseCandidateCount}")
        println("CIQUAL candidates=${result.ciqualCandidateCount}")
        println("Input candidates=${result.inputCandidateCount}")
        println("Normalized=${result.normalizedCandidateCount}")
        println("Merged=${result.mergedCandidateCount}")
        println("Conflicts=${result.conflictCount}")
        println()
        println("Nutrition candidates=${result.nutritionCandidateCount}")
        println("Environmental candidates=${result.environmentalImpactCandidateCount}")
        println("Multi dimension=${result.multiDimensionCandidateCount}")
        println()
        println("Nutrition artifact entries=${result.nutritionArtifactEntryCount}")
        println("Environmental artifact entries=${result.environmentalImpactArtifactEntryCount}")
        println()
        println("Nutrition artifact=${result.nutritionArtifactFile.path}")
        println("Environmental artifact=${result.environmentalImpactArtifactFile.path}")

        println("Ingredients candidates=${result.ingredientsCandidateCount}")

        println("Ingredients artifact entries=${result.ingredientsArtifactEntryCount}")
        println("Ingredients artifact=${result.ingredientsArtifactFile.path}")

        println("Allergens candidates=${result.allergensCandidateCount}")

        println("Allergen artifact entries=${result.allergenArtifactEntryCount}")
        println("Allergen artifact=${result.allergenArtifactFile.path}")

        println("Packaging candidates=${result.packagingCandidateCount}")

        println("Packaging artifact entries=${result.packagingArtifactEntryCount}")
        println("Packaging artifact=${result.packagingArtifactFile.path}")

        println("Taxonomy candidates=${result.taxonomyCandidateCount}")

        println("Taxonomy artifact entries=${result.taxonomyArtifactEntryCount}")
        println("Taxonomy artifact=${result.taxonomyArtifactFile.path}")

        println("Processing candidates=${result.processingCandidateCount}")

        println("Processing artifact entries=${result.processingArtifactEntryCount}")
        println("Processing artifact=${result.processingArtifactFile.path}")

        println("Water candidates=${result.waterCandidateCount}")
        println("Water artifact entries=${result.waterArtifactEntryCount}")
        println("Water artifact=${result.waterArtifactFile.path}")

        println("Water Stress candidates=${result.waterStressCandidateCount}")
        println("Water Stress artifact entries=${result.waterStressArtifactEntryCount}")
        println("Water Stress artifact=${result.waterStressArtifactFile.path}")

        println("Biodiversity candidates=${result.biodiversityCandidateCount}")
        println("Biodiversity artifact entries=${result.biodiversityArtifactEntryCount}")
        println("Biodiversity artifact=${result.biodiversityArtifactFile.path}")

        println("Pollinator candidates=${result.pollinatorCandidateCount}")
        println("Pollinator artifact entries=${result.pollinatorArtifactEntryCount}")
        println("Pollinator artifact=${result.pollinatorArtifactFile.path}")

        println("Pesticides candidates=${result.pesticidesCandidateCount}")
        println("Pesticides artifact entries=${result.pesticidesArtifactEntryCount}")
        println("Pesticides artifact=${result.pesticidesArtifactFile.path}")

        println("Production candidates=${result.productionCandidateCount}")
        println("Production artifact entries=${result.productionArtifactEntryCount}")
        println("Production artifact=${result.productionArtifactFile.path}")

        println("Food Miles candidates=${result.foodMilesCandidateCount}")
        println("Food Miles artifact entries=${result.foodMilesArtifactEntryCount}")
        println("Food Miles artifact=${result.foodMilesArtifactFile.path}")

        println("Locality candidates=${result.localityCandidateCount}")
        println("Locality artifact entries=${result.localityArtifactEntryCount}")
        println("Locality artifact=${result.localityArtifactFile.path}")

        println("Nutri Score candidates=${result.nutriScoreCandidateCount}")
        println("Nutri Score artifact entries=${result.nutriScoreArtifactEntryCount}")
        println("Nutri Score artifact=${result.nutriScoreArtifactFile.path}")

        println("Seasonality candidates=${result.seasonalityCandidateCount}")
        println("Seasonality artifact entries=${result.seasonalityArtifactEntryCount}")
        println("Seasonality artifact=${result.seasonalityArtifactFile.path}")

        println("Diet candidates=${result.dietCandidateCount}")
        println("Diet artifact entries=${result.dietArtifactEntryCount}")
        println("Diet artifact=${result.dietArtifactFile.path}")

        println("FairTrade candidates=${result.fairTradeCandidateCount}")
        println("FairTrade artifact entries=${result.fairTradeArtifactEntryCount}")
        println("FairTrade artifact=${result.fairTradeArtifactFile.path}")

        println("Animal Welfare candidates=${result.animalWelfareCandidateCount}")
        println("Animal Welfare artifact entries=${result.animalWelfareArtifactEntryCount}")
        println("Animal Welfare artifact=${result.animalWelfareArtifactFile.path}")

        println("Recipe candidates=${result.recipeCandidateCount}")
        println("Recipe artifact entries=${result.recipeArtifactEntryCount}")
        println("Recipe artifact=${result.recipeArtifactFile.path}")

        println("Ingredient Graph candidates=${result.ingredientGraphCandidateCount}")
        println("Ingredient Graph artifact entries=${result.ingredientGraphArtifactEntryCount}")
        println("Ingredient Graph artifact=${result.ingredientGraphArtifactFile.path}")

        println("Recipe Graph candidates=${result.recipeGraphCandidateCount}")
        println("Recipe Graph artifact entries=${result.recipeGraphArtifactEntryCount}")
        println("Recipe Graph artifact=${result.recipeGraphArtifactFile.path}")


        //############################################################################//

        assertTrue(result.ingredientsCandidateCount > 0)

        assertTrue(
            result.offCandidateCount > 0,
            "Open Food Facts must produce candidates."
        )

        assertTrue(
            result.agribalyseCandidateCount > 0,
            "Agribalyse must produce candidates."
        )

        assertTrue(
            result.ciqualCandidateCount > 0,
            "CIQUAL must produce nutrition candidates."
        )

        val expectedInputCandidateCount =
            result.offCandidateCount +
                    result.offNutritionAggregateCount +
                    result.agribalyseCandidateCount +
                    result.ciqualCandidateCount

        assertEquals(
            expectedInputCandidateCount,
            result.inputCandidateCount,
            "Input candidate count must equal the sum of raw OFF, " +
                    "OFF nutrition aggregate, Agribalyse and CIQUAL candidates."
        )

        assertTrue(
            result.offNutritionAggregateCount > 0,
            "Expected OFF nutrition aggregates to be included in the build."
        )

        assertTrue(
            result.normalizedCandidateCount > 0,
            "The combined source candidates must produce normalized candidates."
        )

        assertTrue(
            result.normalizedCandidateCount <= result.inputCandidateCount,
            "Normalization must not increase the number of source candidates."
        )

        assertTrue(
            result.mergedCandidateCount > 0,
            "The combined source candidates must produce merged candidates."
        )

        assertTrue(
            result.mergedCandidateCount <= result.normalizedCandidateCount,
            "Merging must not increase the number of normalized candidates."
        )

        assertTrue(result.nutritionCandidateCount > 0)
        assertTrue(result.environmentalImpactCandidateCount > 0)

        assertTrue(result.multiDimensionCandidateCount > 0)

        assertTrue(result.nutritionArtifactEntryCount > 0)
        assertTrue(result.environmentalImpactArtifactEntryCount > 0)

        assertTrue(result.nutritionArtifactFile.exists())
        assertTrue(result.environmentalImpactArtifactFile.exists())

        assertTrue(result.ingredientsArtifactEntryCount > 0)
        assertTrue(result.ingredientsArtifactFile.exists())

        assertTrue(result.allergensCandidateCount > 0)

        assertTrue(result.allergenArtifactEntryCount > 0)
        assertTrue(result.allergenArtifactFile.exists())

        assertTrue(result.packagingCandidateCount > 0)

        assertTrue(result.packagingArtifactEntryCount > 0)
        assertTrue(result.packagingArtifactFile.exists())

        assertTrue(result.taxonomyCandidateCount > 0)

        assertTrue(result.taxonomyArtifactEntryCount > 0)
        assertTrue(result.taxonomyArtifactFile.exists())

        assertTrue(result.processingCandidateCount > 0)

        assertTrue(result.processingArtifactEntryCount > 0)
        assertTrue(result.processingArtifactFile.exists())

        assertTrue(result.waterCandidateCount > 0)
        assertTrue(result.waterArtifactEntryCount > 0)
        assertTrue(result.waterArtifactFile.exists())

        assertTrue(result.waterStressCandidateCount > 0)
        assertTrue(result.waterStressArtifactEntryCount > 0)
        assertTrue(result.waterStressArtifactFile.exists())

        assertTrue(result.biodiversityCandidateCount > 0)
        assertTrue(result.biodiversityArtifactEntryCount > 0)
        assertTrue(result.biodiversityArtifactFile.exists())

        assertTrue(result.pollinatorCandidateCount > 0)
        assertTrue(result.pollinatorArtifactEntryCount > 0)
        assertTrue(result.pollinatorArtifactFile.exists())

        assertTrue(result.pesticidesCandidateCount > 0)
        assertTrue(result.pesticidesArtifactEntryCount > 0)
        assertTrue(result.pesticidesArtifactFile.exists())

        assertTrue(result.productionCandidateCount > 0)
        assertTrue(result.productionArtifactEntryCount > 0)
        assertTrue(result.productionArtifactFile.exists())

        assertTrue(result.foodMilesCandidateCount > 0)
        assertTrue(result.foodMilesArtifactEntryCount > 0)
        assertTrue(result.foodMilesArtifactFile.exists())

        assertTrue(result.localityCandidateCount > 0)
        assertTrue(result.localityArtifactEntryCount > 0)
        assertTrue(result.localityArtifactFile.exists())

        assertTrue(result.nutriScoreCandidateCount > 0)
        assertTrue(result.nutriScoreArtifactEntryCount > 0)
        assertTrue(result.nutriScoreArtifactFile.exists())

        assertTrue(result.seasonalityCandidateCount > 0)
        assertTrue(result.seasonalityArtifactEntryCount > 0)
        assertTrue(result.seasonalityArtifactFile.exists())

        assertTrue(result.dietCandidateCount > 0)
        assertTrue(result.dietArtifactEntryCount > 0)
        assertTrue(result.dietArtifactFile.exists())

        assertTrue(result.fairTradeCandidateCount > 0)
        assertTrue(result.fairTradeArtifactEntryCount > 0)
        assertTrue(result.fairTradeArtifactFile.exists())

        assertTrue(result.animalWelfareCandidateCount > 0)
        assertTrue(result.animalWelfareArtifactEntryCount > 0)
        assertTrue(result.animalWelfareArtifactFile.exists())

        assertTrue(result.recipeCandidateCount > 0)
        assertTrue(result.recipeArtifactEntryCount > 0)
        assertTrue(result.recipeArtifactFile.exists())

        assertTrue(result.ingredientGraphCandidateCount > 0)
        assertTrue(result.ingredientGraphArtifactEntryCount > 0)
        assertTrue(result.ingredientGraphArtifactFile.exists())

//        assertEquals(0, result.recipeGraphCandidateCount)
//        assertEquals(0, result.recipeGraphArtifactEntryCount)
//        assertFalse(result.recipeGraphArtifactFile.exists())

        assertTrue(result.recipeGraphCandidateCount > 0)
        assertTrue(result.recipeGraphArtifactEntryCount > 0)
        assertTrue(result.recipeGraphArtifactFile.exists())
    }

    private fun printBlockedFanoutKeys(
        keys: Map<String, Int>
    ) {
        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("BLOCKED HIGH FANOUT MATCH KEYS")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        keys
            .entries
            .sortedByDescending { it.value }
            .take(30)
            .forEach {
                println("${it.key}=${it.value}")
            }
    }
}