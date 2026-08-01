package de.shopme.tools.knowledge.ai.builder.runtime.partition

import de.shopme.tools.knowledge.ai.builder.allergen.MergedCandidateAllergenKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.animalwelfare.MergedCandidateAnimalWelfareKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.artifact.GeneratedKnowledgeArtifactWriter
import de.shopme.tools.knowledge.ai.builder.biodiversity.MergedCandidateBiodiversityKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.diet.MergedCandidateDietKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.environment.MergedCandidateEnvironmentalImpactKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.fairtrade.MergedCandidateFairtradeKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.foodmiles.MergedCandidateFoodMilesKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.ingredientgraph.MergedCandidateIngredientGraphKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.ingredients.MergedCandidateIngredientsKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.locality.MergedCandidateLocalityKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.nutriscore.MergedCandidateNutriScoreKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.nutrition.MergedCandidateNutritionKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.packaging.MergedCandidatePackagingKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.pesticides.MergedCandidatePesticidesKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.pollinator.MergedCandidatePollinatorKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.processing.MergedCandidateProcessingKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.production.MergedCandidateProductionKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.recipe.MergedCandidateRecipeKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.recipegraph.MergedCandidateRecipeGraphKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.seasonality.MergedCandidateSeasonalityKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.taxonomy.MergedCandidateFoodTaxonomyKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.water.MergedCandidateWaterKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.waterstress.MergedCandidateWaterStressKnowledgeBuilder
import de.shopme.tools.knowledge.ki_candidates.CanonicalKnowledgeCandidate
import de.shopme.tools.knowledge.ki_candidates.KnowledgeDimensionCandidateType
import java.io.File

class PartitionedRuntimeKnowledgeArtifactBuild(
    private val shardRootDirectory: File
) {

    init {
        require(
            shardRootDirectory.mkdirs() ||
                    shardRootDirectory.isDirectory
        ) {
            "Could not create artifact shard directory: " +
                    shardRootDirectory.path
        }
    }

    private val writer =
        GeneratedKnowledgeArtifactWriter()

    private val counts =
        MutablePartitionedRuntimeKnowledgeArtifactCounts()

    fun addPartition(
        partitionIndex: Int,
        mergedCandidates: List<CanonicalKnowledgeCandidate>
    ) {
        require(partitionIndex >= 0) {
            "partitionIndex must not be negative."
        }

        if (mergedCandidates.isEmpty()) {
            return
        }

        val partitionDirectory =
            shardRootDirectory.resolve(
                partitionDirectoryName(
                    partitionIndex
                )
            )

        require(
            partitionDirectory.mkdirs() ||
                    partitionDirectory.isDirectory
        ) {
            "Could not create artifact partition directory: " +
                    partitionDirectory.path
        }

        counts.addCandidateCounts(
            mergedCandidates
        )

        val nutritionKnowledge =
            MergedCandidateNutritionKnowledgeBuilder()
                .build(
                    mergedCandidates
                )

        val environmentalImpactKnowledge =
            MergedCandidateEnvironmentalImpactKnowledgeBuilder()
                .build(
                    mergedCandidates
                )

        val ingredientsKnowledge =
            MergedCandidateIngredientsKnowledgeBuilder()
                .build(
                    mergedCandidates
                )

        val allergenKnowledge =
            MergedCandidateAllergenKnowledgeBuilder()
                .build(
                    mergedCandidates
                )

        val packagingKnowledge =
            MergedCandidatePackagingKnowledgeBuilder()
                .build(
                    mergedCandidates
                )

        val foodTaxonomyKnowledge =
            MergedCandidateFoodTaxonomyKnowledgeBuilder()
                .build(
                    mergedCandidates
                )

        val processingKnowledge =
            MergedCandidateProcessingKnowledgeBuilder()
                .build(
                    mergedCandidates
                )

        val waterKnowledge =
            MergedCandidateWaterKnowledgeBuilder()
                .build(
                    mergedCandidates
                )

        val waterStressKnowledge =
            MergedCandidateWaterStressKnowledgeBuilder()
                .build(
                    mergedCandidates
                )

        val biodiversityKnowledge =
            MergedCandidateBiodiversityKnowledgeBuilder()
                .build(
                    mergedCandidates
                )

        val pollinatorKnowledge =
            MergedCandidatePollinatorKnowledgeBuilder()
                .build(
                    mergedCandidates
                )

        val pesticidesKnowledge =
            MergedCandidatePesticidesKnowledgeBuilder()
                .build(
                    mergedCandidates
                )

        val productionKnowledge =
            MergedCandidateProductionKnowledgeBuilder()
                .build(
                    mergedCandidates
                )

        val foodMilesKnowledge =
            MergedCandidateFoodMilesKnowledgeBuilder()
                .build(
                    mergedCandidates
                )

        val localityKnowledge =
            MergedCandidateLocalityKnowledgeBuilder()
                .build(
                    mergedCandidates
                )

        val nutriScoreKnowledge =
            MergedCandidateNutriScoreKnowledgeBuilder()
                .build(
                    mergedCandidates
                )

        val seasonalityKnowledge =
            MergedCandidateSeasonalityKnowledgeBuilder()
                .build(
                    mergedCandidates
                )

        val dietKnowledge =
            MergedCandidateDietKnowledgeBuilder()
                .build(
                    mergedCandidates
                )

        val fairTradeKnowledge =
            MergedCandidateFairtradeKnowledgeBuilder()
                .build(
                    candidates =
                        mergedCandidates
                )

        val animalWelfareKnowledge =
            MergedCandidateAnimalWelfareKnowledgeBuilder()
                .build(
                    candidates =
                        mergedCandidates
                )

        val recipeKnowledge =
            MergedCandidateRecipeKnowledgeBuilder()
                .build(
                    candidates =
                        mergedCandidates
                )

        val ingredientGraphKnowledge =
            MergedCandidateIngredientGraphKnowledgeBuilder()
                .build(
                    candidates =
                        mergedCandidates
                )

        val recipeGraphKnowledge =
            MergedCandidateRecipeGraphKnowledgeBuilder()
                .build(
                    candidates =
                        mergedCandidates
                )

        writeWhenNotEmpty(
            entryCount =
                nutritionKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    NUTRITION_FILE_NAME,
                artifact =
                    nutritionKnowledge
            )
        }

        writeWhenNotEmpty(
            entryCount =
                environmentalImpactKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    ENVIRONMENTAL_IMPACT_FILE_NAME,
                artifact =
                    environmentalImpactKnowledge
            )
        }

        writeWhenNotEmpty(
            entryCount =
                ingredientsKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    INGREDIENTS_FILE_NAME,
                artifact =
                    ingredientsKnowledge
            )
        }

        writeWhenNotEmpty(
           entryCount =
                allergenKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    ALLERGENS_FILE_NAME,
                artifact =
                    allergenKnowledge
            )
        }

        writeWhenNotEmpty(
           entryCount =
                packagingKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    PACKAGING_FILE_NAME,
                artifact =
                    packagingKnowledge
            )
        }

        writeWhenNotEmpty(
           entryCount =
                foodTaxonomyKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    FOOD_TAXONOMY_FILE_NAME,
                artifact =
                    foodTaxonomyKnowledge
            )
        }

        writeWhenNotEmpty(
            entryCount =
                processingKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    PROCESSING_FILE_NAME,
                artifact =
                    processingKnowledge
            )
        }

        writeWhenNotEmpty(
           entryCount =
                waterKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    WATER_FILE_NAME,
                artifact =
                    waterKnowledge
            )
        }

        writeWhenNotEmpty(
            entryCount =
                waterStressKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    WATER_STRESS_FILE_NAME,
                artifact =
                    waterStressKnowledge
            )
        }

        writeWhenNotEmpty(
            entryCount =
                biodiversityKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    BIODIVERSITY_FILE_NAME,
                artifact =
                    biodiversityKnowledge
            )
        }

        writeWhenNotEmpty(
            entryCount =
                pollinatorKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    POLLINATOR_FILE_NAME,
                artifact =
                    pollinatorKnowledge
            )
        }

        writeWhenNotEmpty(
           entryCount =
                pesticidesKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    PESTICIDES_FILE_NAME,
                artifact =
                    pesticidesKnowledge
            )
        }

        writeWhenNotEmpty(
            entryCount =
                productionKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    PRODUCTION_FILE_NAME,
                artifact =
                    productionKnowledge
            )
        }

        writeWhenNotEmpty(
            entryCount =
                foodMilesKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    FOOD_MILES_FILE_NAME,
                artifact =
                    foodMilesKnowledge
            )
        }

        writeWhenNotEmpty(
            entryCount =
                localityKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    LOCALITY_FILE_NAME,
                artifact =
                    localityKnowledge
            )
        }

        writeWhenNotEmpty(
            entryCount =
                nutriScoreKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    NUTRI_SCORE_FILE_NAME,
                artifact =
                    nutriScoreKnowledge
            )
        }

        writeWhenNotEmpty(
            entryCount =
                seasonalityKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    SEASONALITY_FILE_NAME,
                artifact =
                    seasonalityKnowledge
            )
        }

        writeWhenNotEmpty(
            entryCount =
                dietKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    DIET_FILE_NAME,
                artifact =
                    dietKnowledge
            )
        }

        writeWhenNotEmpty(
            entryCount =
                fairTradeKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    FAIRTRADE_FILE_NAME,
                artifact =
                    fairTradeKnowledge
            )
        }

        writeWhenNotEmpty(
            entryCount =
                animalWelfareKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    ANIMAL_WELFARE_FILE_NAME,
                artifact =
                    animalWelfareKnowledge
            )
        }

        writeWhenNotEmpty(
            entryCount =
                recipeKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    RECIPES_FILE_NAME,
                artifact =
                    recipeKnowledge
            )
        }

        writeWhenNotEmpty(
            entryCount =
                ingredientGraphKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    INGREDIENT_GRAPH_FILE_NAME,
                artifact =
                    ingredientGraphKnowledge
            )
        }

        writeWhenNotEmpty(
            entryCount =
                recipeGraphKnowledge.entries.size
        ) {
            writer.write(
                outputDir =
                    partitionDirectory,
                fileName =
                    RECIPE_GRAPH_FILE_NAME,
                artifact =
                    recipeGraphKnowledge
            )
        }

        counts.addArtifactEntryCounts(
            nutrition =
                nutritionKnowledge.entries.size,
            environmentalImpact =
                environmentalImpactKnowledge.entries.size,
            ingredients =
                ingredientsKnowledge.entries.size,
            allergens =
                allergenKnowledge.entries.size,
            packaging =
                packagingKnowledge.entries.size,
            taxonomy =
                foodTaxonomyKnowledge.entries.size,
            processing =
                processingKnowledge.entries.size,
            water =
                waterKnowledge.entries.size,
            waterStress =
                waterStressKnowledge.entries.size,
            biodiversity =
                biodiversityKnowledge.entries.size,
            pollinator =
                pollinatorKnowledge.entries.size,
            pesticides =
                pesticidesKnowledge.entries.size,
            production =
                productionKnowledge.entries.size,
            foodMiles =
                foodMilesKnowledge.entries.size,
            locality =
                localityKnowledge.entries.size,
            nutriScore =
                nutriScoreKnowledge.entries.size,
            seasonality =
                seasonalityKnowledge.entries.size,
            diet =
                dietKnowledge.entries.size,
            fairTrade =
                fairTradeKnowledge.entries.size,
            animalWelfare =
                animalWelfareKnowledge.entries.size,
            recipe =
                recipeKnowledge.entries.size,
            ingredientGraph =
                ingredientGraphKnowledge.entries.size,
            recipeGraph =
                recipeGraphKnowledge.entries.size
        )
    }

    fun finish():
            PartitionedRuntimeKnowledgeArtifactBuildResult =
        PartitionedRuntimeKnowledgeArtifactBuildResult(
            shardRootDirectory =
                shardRootDirectory,
            counts =
                counts.snapshot()
        )

    private inline fun writeWhenNotEmpty(
        entryCount: Int,
        write: () -> File
    ): File? {
        require(entryCount >= 0) {
            "entryCount must not be negative."
        }

        if (entryCount == 0) {
            return null
        }

        return write()
    }

    private fun partitionDirectoryName(
        partitionIndex: Int
    ): String =
        "partition-" +
                partitionIndex
                    .toString()
                    .padStart(
                        PARTITION_INDEX_WIDTH,
                        '0'
                    )

    companion object {

        const val NUTRITION_FILE_NAME =
            "nutrition.json"

        const val ENVIRONMENTAL_IMPACT_FILE_NAME =
            "environmental_impact.json"

        const val INGREDIENTS_FILE_NAME =
            "ingredients.json"

        const val ALLERGENS_FILE_NAME =
            "allergens.json"

        const val PACKAGING_FILE_NAME =
            "packaging.json"

        const val FOOD_TAXONOMY_FILE_NAME =
            "food_taxonomy.json"

        const val PROCESSING_FILE_NAME =
            "processing.json"

        const val WATER_FILE_NAME =
            "water_footprint.json"

        const val WATER_STRESS_FILE_NAME =
            "water_stress.json"

        const val BIODIVERSITY_FILE_NAME =
            "biodiversity.json"

        const val POLLINATOR_FILE_NAME =
            "pollinator.json"

        const val PESTICIDES_FILE_NAME =
            "pesticides.json"

        const val PRODUCTION_FILE_NAME =
            "production.json"

        const val FOOD_MILES_FILE_NAME =
            "food_miles.json"

        const val LOCALITY_FILE_NAME =
            "locality.json"

        const val NUTRI_SCORE_FILE_NAME =
            "nutri_score.json"

        const val SEASONALITY_FILE_NAME =
            "seasonality.json"

        const val DIET_FILE_NAME =
            "diet_classification.json"

        const val FAIRTRADE_FILE_NAME =
            "fairtrade.json"

        const val ANIMAL_WELFARE_FILE_NAME =
            "animal_welfare.json"

        const val RECIPES_FILE_NAME =
            "recipes.json"

        const val INGREDIENT_GRAPH_FILE_NAME =
            "ingredient_graph.json"

        const val RECIPE_GRAPH_FILE_NAME =
            "recipe_graph.json"

        private const val PARTITION_INDEX_WIDTH =
            4
    }
}

data class PartitionedRuntimeKnowledgeArtifactBuildResult(
    val shardRootDirectory: File,
    val counts: PartitionedRuntimeKnowledgeArtifactCounts
)

data class PartitionedRuntimeKnowledgeArtifactCounts(
    val nutritionCandidateCount: Int,
    val environmentalCandidateCount: Int,
    val multiDimensionCandidateCount: Int,
    val ingredientsCandidateCount: Int,
    val allergensCandidateCount: Int,
    val packagingCandidateCount: Int,
    val taxonomyCandidateCount: Int,
    val processingCandidateCount: Int,
    val waterCandidateCount: Int,
    val waterStressCandidateCount: Int,
    val biodiversityCandidateCount: Int,
    val pollinatorCandidateCount: Int,
    val pesticidesCandidateCount: Int,
    val productionCandidateCount: Int,
    val foodMilesCandidateCount: Int,
    val localityCandidateCount: Int,
    val nutriScoreCandidateCount: Int,
    val seasonalityCandidateCount: Int,
    val dietCandidateCount: Int,
    val fairTradeCandidateCount: Int,
    val animalWelfareCandidateCount: Int,
    val recipeCandidateCount: Int,
    val ingredientGraphCandidateCount: Int,
    val recipeGraphCandidateCount: Int,

    val nutritionArtifactEntryCount: Int,
    val environmentalImpactArtifactEntryCount: Int,
    val ingredientsArtifactEntryCount: Int,
    val allergenArtifactEntryCount: Int,
    val packagingArtifactEntryCount: Int,
    val taxonomyArtifactEntryCount: Int,
    val processingArtifactEntryCount: Int,
    val waterArtifactEntryCount: Int,
    val waterStressArtifactEntryCount: Int,
    val biodiversityArtifactEntryCount: Int,
    val pollinatorArtifactEntryCount: Int,
    val pesticidesArtifactEntryCount: Int,
    val productionArtifactEntryCount: Int,
    val foodMilesArtifactEntryCount: Int,
    val localityArtifactEntryCount: Int,
    val nutriScoreArtifactEntryCount: Int,
    val seasonalityArtifactEntryCount: Int,
    val dietArtifactEntryCount: Int,
    val fairTradeArtifactEntryCount: Int,
    val animalWelfareArtifactEntryCount: Int,
    val recipeArtifactEntryCount: Int,
    val ingredientGraphArtifactEntryCount: Int,
    val recipeGraphArtifactEntryCount: Int
)

private class MutablePartitionedRuntimeKnowledgeArtifactCounts {

    private var nutritionCandidateCount =
        0

    private var environmentalCandidateCount =
        0

    private var multiDimensionCandidateCount =
        0

    private var ingredientsCandidateCount =
        0

    private var allergensCandidateCount =
        0

    private var packagingCandidateCount =
        0

    private var taxonomyCandidateCount =
        0

    private var processingCandidateCount =
        0

    private var waterCandidateCount =
        0

    private var waterStressCandidateCount =
        0

    private var biodiversityCandidateCount =
        0

    private var pollinatorCandidateCount =
        0

    private var pesticidesCandidateCount =
        0

    private var productionCandidateCount =
        0

    private var foodMilesCandidateCount =
        0

    private var localityCandidateCount =
        0

    private var nutriScoreCandidateCount =
        0

    private var seasonalityCandidateCount =
        0

    private var dietCandidateCount =
        0

    private var fairTradeCandidateCount =
        0

    private var animalWelfareCandidateCount =
        0

    private var recipeCandidateCount =
        0

    private var ingredientGraphCandidateCount =
        0

    private var recipeGraphCandidateCount =
        0

    private var nutritionArtifactEntryCount =
        0

    private var environmentalImpactArtifactEntryCount =
        0

    private var ingredientsArtifactEntryCount =
        0

    private var allergenArtifactEntryCount =
        0

    private var packagingArtifactEntryCount =
        0

    private var taxonomyArtifactEntryCount =
        0

    private var processingArtifactEntryCount =
        0

    private var waterArtifactEntryCount =
        0

    private var waterStressArtifactEntryCount =
        0

    private var biodiversityArtifactEntryCount =
        0

    private var pollinatorArtifactEntryCount =
        0

    private var pesticidesArtifactEntryCount =
        0

    private var productionArtifactEntryCount =
        0

    private var foodMilesArtifactEntryCount =
        0

    private var localityArtifactEntryCount =
        0

    private var nutriScoreArtifactEntryCount =
        0

    private var seasonalityArtifactEntryCount =
        0

    private var dietArtifactEntryCount =
        0

    private var fairTradeArtifactEntryCount =
        0

    private var animalWelfareArtifactEntryCount =
        0

    private var recipeArtifactEntryCount =
        0

    private var ingredientGraphArtifactEntryCount =
        0

    private var recipeGraphArtifactEntryCount =
        0

    fun addCandidateCounts(
        candidates: List<CanonicalKnowledgeCandidate>
    ) {
        candidates.forEach { candidate ->
            val dimensions =
                candidate.dimensions
                    .asSequence()
                    .map { dimension ->
                        dimension.dimension
                    }
                    .toSet()

            if (dimensions.size > 1) {
                multiDimensionCandidateCount++
            }

            dimensions.forEach { dimension ->
                when (dimension) {
                    KnowledgeDimensionCandidateType.NUTRITION ->
                        nutritionCandidateCount++

                    KnowledgeDimensionCandidateType.ENVIRONMENTAL_IMPACT ->
                        environmentalCandidateCount++

                    KnowledgeDimensionCandidateType.INGREDIENTS ->
                        ingredientsCandidateCount++

                    KnowledgeDimensionCandidateType.ALLERGENS ->
                        allergensCandidateCount++

                    KnowledgeDimensionCandidateType.PACKAGING ->
                        packagingCandidateCount++

                    KnowledgeDimensionCandidateType.TAXONOMY ->
                        taxonomyCandidateCount++

                    KnowledgeDimensionCandidateType.PROCESSING ->
                        processingCandidateCount++

                    KnowledgeDimensionCandidateType.WATER ->
                        waterCandidateCount++

                    KnowledgeDimensionCandidateType.WATER_STRESS ->
                        waterStressCandidateCount++

                    KnowledgeDimensionCandidateType.BIODIVERSITY ->
                        biodiversityCandidateCount++

                    KnowledgeDimensionCandidateType.POLLINATOR ->
                        pollinatorCandidateCount++

                    KnowledgeDimensionCandidateType.PESTICIDES ->
                        pesticidesCandidateCount++

                    KnowledgeDimensionCandidateType.PRODUCTION ->
                        productionCandidateCount++

                    KnowledgeDimensionCandidateType.FOOD_MILES ->
                        foodMilesCandidateCount++

                    KnowledgeDimensionCandidateType.LOCALITY ->
                        localityCandidateCount++

                    KnowledgeDimensionCandidateType.NUTRI_SCORE ->
                        nutriScoreCandidateCount++

                    KnowledgeDimensionCandidateType.SEASONALITY ->
                        seasonalityCandidateCount++

                    KnowledgeDimensionCandidateType.DIET ->
                        dietCandidateCount++

                    KnowledgeDimensionCandidateType.FAIRTRADE ->
                        fairTradeCandidateCount++

                    KnowledgeDimensionCandidateType.ANIMAL_WELFARE ->
                        animalWelfareCandidateCount++

                    KnowledgeDimensionCandidateType.RECIPE ->
                        recipeCandidateCount++

                    KnowledgeDimensionCandidateType.INGREDIENT_GRAPH ->
                        ingredientGraphCandidateCount++

                    KnowledgeDimensionCandidateType.RECIPE_GRAPH ->
                        recipeGraphCandidateCount++

                    else -> {
                        /*
                         * Dimensionen ohne aktuell integrierten Runtime-Artifact-
                         * Builder werden bewusst nicht in diesen Build-Counts
                         * erfasst.
                         *
                         * Aktuell betrifft das insbesondere:
                         * - CARBON
                         * - GLYCEMIC
                         * - CARBON_IMPACT
                         */
                    }
                }
            }
        }
    }

    fun addArtifactEntryCounts(
        nutrition: Int,
        environmentalImpact: Int,
        ingredients: Int,
        allergens: Int,
        packaging: Int,
        taxonomy: Int,
        processing: Int,
        water: Int,
        waterStress: Int,
        biodiversity: Int,
        pollinator: Int,
        pesticides: Int,
        production: Int,
        foodMiles: Int,
        locality: Int,
        nutriScore: Int,
        seasonality: Int,
        diet: Int,
        fairTrade: Int,
        animalWelfare: Int,
        recipe: Int,
        ingredientGraph: Int,
        recipeGraph: Int
    ) {
        nutritionArtifactEntryCount +=
            nutrition

        environmentalImpactArtifactEntryCount +=
            environmentalImpact

        ingredientsArtifactEntryCount +=
            ingredients

        allergenArtifactEntryCount +=
            allergens

        packagingArtifactEntryCount +=
            packaging

        taxonomyArtifactEntryCount +=
            taxonomy

        processingArtifactEntryCount +=
            processing

        waterArtifactEntryCount +=
            water

        waterStressArtifactEntryCount +=
            waterStress

        biodiversityArtifactEntryCount +=
            biodiversity

        pollinatorArtifactEntryCount +=
            pollinator

        pesticidesArtifactEntryCount +=
            pesticides

        productionArtifactEntryCount +=
            production

        foodMilesArtifactEntryCount +=
            foodMiles

        localityArtifactEntryCount +=
            locality

        nutriScoreArtifactEntryCount +=
            nutriScore

        seasonalityArtifactEntryCount +=
            seasonality

        dietArtifactEntryCount +=
            diet

        fairTradeArtifactEntryCount +=
            fairTrade

        animalWelfareArtifactEntryCount +=
            animalWelfare

        recipeArtifactEntryCount +=
            recipe

        ingredientGraphArtifactEntryCount +=
            ingredientGraph

        recipeGraphArtifactEntryCount +=
            recipeGraph
    }

    fun snapshot():
            PartitionedRuntimeKnowledgeArtifactCounts =
        PartitionedRuntimeKnowledgeArtifactCounts(
            nutritionCandidateCount =
                nutritionCandidateCount,
            environmentalCandidateCount =
                environmentalCandidateCount,
            multiDimensionCandidateCount =
                multiDimensionCandidateCount,
            ingredientsCandidateCount =
                ingredientsCandidateCount,
            allergensCandidateCount =
                allergensCandidateCount,
            packagingCandidateCount =
                packagingCandidateCount,
            taxonomyCandidateCount =
                taxonomyCandidateCount,
            processingCandidateCount =
                processingCandidateCount,
            waterCandidateCount =
                waterCandidateCount,
            waterStressCandidateCount =
                waterStressCandidateCount,
            biodiversityCandidateCount =
                biodiversityCandidateCount,
            pollinatorCandidateCount =
                pollinatorCandidateCount,
            pesticidesCandidateCount =
                pesticidesCandidateCount,
            productionCandidateCount =
                productionCandidateCount,
            foodMilesCandidateCount =
                foodMilesCandidateCount,
            localityCandidateCount =
                localityCandidateCount,
            nutriScoreCandidateCount =
                nutriScoreCandidateCount,
            seasonalityCandidateCount =
                seasonalityCandidateCount,
            dietCandidateCount =
                dietCandidateCount,
            fairTradeCandidateCount =
                fairTradeCandidateCount,
            animalWelfareCandidateCount =
                animalWelfareCandidateCount,
            recipeCandidateCount =
                recipeCandidateCount,
            ingredientGraphCandidateCount =
                ingredientGraphCandidateCount,
            recipeGraphCandidateCount =
                recipeGraphCandidateCount,
            nutritionArtifactEntryCount =
                nutritionArtifactEntryCount,
            environmentalImpactArtifactEntryCount =
                environmentalImpactArtifactEntryCount,
            ingredientsArtifactEntryCount =
                ingredientsArtifactEntryCount,
            allergenArtifactEntryCount =
                allergenArtifactEntryCount,
            packagingArtifactEntryCount =
                packagingArtifactEntryCount,
            taxonomyArtifactEntryCount =
                taxonomyArtifactEntryCount,
            processingArtifactEntryCount =
                processingArtifactEntryCount,
            waterArtifactEntryCount =
                waterArtifactEntryCount,
            waterStressArtifactEntryCount =
                waterStressArtifactEntryCount,
            biodiversityArtifactEntryCount =
                biodiversityArtifactEntryCount,
            pollinatorArtifactEntryCount =
                pollinatorArtifactEntryCount,
            pesticidesArtifactEntryCount =
                pesticidesArtifactEntryCount,
            productionArtifactEntryCount =
                productionArtifactEntryCount,
            foodMilesArtifactEntryCount =
                foodMilesArtifactEntryCount,
            localityArtifactEntryCount =
                localityArtifactEntryCount,
            nutriScoreArtifactEntryCount =
                nutriScoreArtifactEntryCount,
            seasonalityArtifactEntryCount =
                seasonalityArtifactEntryCount,
            dietArtifactEntryCount =
                dietArtifactEntryCount,
            fairTradeArtifactEntryCount =
                fairTradeArtifactEntryCount,
            animalWelfareArtifactEntryCount =
                animalWelfareArtifactEntryCount,
            recipeArtifactEntryCount =
                recipeArtifactEntryCount,
            ingredientGraphArtifactEntryCount =
                ingredientGraphArtifactEntryCount,
            recipeGraphArtifactEntryCount =
                recipeGraphArtifactEntryCount
        )
}