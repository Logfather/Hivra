package de.shopme.tools.knowledge.ai.builder.runtime.partition

import de.shopme.tools.knowledge.ai.builder.allergen.MergedCandidateAllergenKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.animalwelfare.MergedCandidateAnimalWelfareKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.artifact.GeneratedKnowledgeArtifactWriter
import de.shopme.tools.knowledge.ai.builder.diet.MergedCandidateDietKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.environment.MergedCandidateEnvironmentalImpactKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.foodmiles.MergedCandidateFoodMilesKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.nutriscore.MergedCandidateNutriScoreKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.nutrition.MergedCandidateNutritionKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.pesticides.MergedCandidatePesticidesKnowledgeBuilder
import de.shopme.tools.knowledge.ai.builder.processing.MergedCandidateProcessingKnowledgeBuilder
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

        require(
            partitionIndex >= 0
        ) {
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
            candidates =
                mergedCandidates
        )

        /*
         * Productive Product-Only Food Knowledge scope.
         *
         * Only the 12 currently active dimensions are built here.
         * Legacy dimensions remain available in source code but are no
         * longer produced by the productive Server Knowledge pipeline.
         */
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

        val allergenKnowledge =
            MergedCandidateAllergenKnowledgeBuilder()
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

        val pesticidesKnowledge =
            MergedCandidatePesticidesKnowledgeBuilder()
                .build(
                    mergedCandidates
                )

        val foodMilesKnowledge =
            MergedCandidateFoodMilesKnowledgeBuilder()
                .build(
                    mergedCandidates
                )

        val nutriScoreKnowledge =
            MergedCandidateNutriScoreKnowledgeBuilder()
                .build(
                    mergedCandidates
                )

        val dietKnowledge =
            MergedCandidateDietKnowledgeBuilder()
                .build(
                    mergedCandidates
                )

        val animalWelfareKnowledge =
            MergedCandidateAnimalWelfareKnowledgeBuilder()
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

        counts.addArtifactEntryCounts(
            nutrition =
                nutritionKnowledge.entries.size,
            environmentalImpact =
                environmentalImpactKnowledge.entries.size,
            allergens =
                allergenKnowledge.entries.size,
            taxonomy =
                foodTaxonomyKnowledge.entries.size,
            processing =
                processingKnowledge.entries.size,
            water =
                waterKnowledge.entries.size,
            waterStress =
                waterStressKnowledge.entries.size,
            pesticides =
                pesticidesKnowledge.entries.size,
            foodMiles =
                foodMilesKnowledge.entries.size,
            nutriScore =
                nutriScoreKnowledge.entries.size,
            diet =
                dietKnowledge.entries.size,
            animalWelfare =
                animalWelfareKnowledge.entries.size
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

        require(
            entryCount >= 0
        ) {
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

        const val ALLERGENS_FILE_NAME =
            "allergens.json"

        const val FOOD_TAXONOMY_FILE_NAME =
            "food_taxonomy.json"

        const val PROCESSING_FILE_NAME =
            "processing.json"

        const val WATER_FILE_NAME =
            "water_footprint.json"

        const val WATER_STRESS_FILE_NAME =
            "water_stress.json"

        const val PESTICIDES_FILE_NAME =
            "pesticides.json"

        const val FOOD_MILES_FILE_NAME =
            "food_miles.json"

        const val NUTRI_SCORE_FILE_NAME =
            "nutri_score.json"

        const val DIET_FILE_NAME =
            "diet_classification.json"

        const val ANIMAL_WELFARE_FILE_NAME =
            "animal_welfare.json"

        private const val PARTITION_INDEX_WIDTH =
            4
    }
}

data class PartitionedRuntimeKnowledgeArtifactBuildResult(
    val shardRootDirectory: File,
    val counts: PartitionedRuntimeKnowledgeArtifactCounts
)

/*
 * Legacy count fields intentionally remain part of this transitional result
 * contract because MultiSourceRuntimeKnowledgeBuild still consumes them.
 *
 * They are always zero in the productive 12-dimension build and can be
 * removed together with the corresponding MultiSource result fields in a
 * later API cleanup.
 */
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

    private var allergensCandidateCount =
        0

    private var taxonomyCandidateCount =
        0

    private var processingCandidateCount =
        0

    private var waterCandidateCount =
        0

    private var waterStressCandidateCount =
        0

    private var pesticidesCandidateCount =
        0

    private var foodMilesCandidateCount =
        0

    private var nutriScoreCandidateCount =
        0

    private var dietCandidateCount =
        0

    private var animalWelfareCandidateCount =
        0

    private var nutritionArtifactEntryCount =
        0

    private var environmentalImpactArtifactEntryCount =
        0

    private var allergenArtifactEntryCount =
        0

    private var taxonomyArtifactEntryCount =
        0

    private var processingArtifactEntryCount =
        0

    private var waterArtifactEntryCount =
        0

    private var waterStressArtifactEntryCount =
        0

    private var pesticidesArtifactEntryCount =
        0

    private var foodMilesArtifactEntryCount =
        0

    private var nutriScoreArtifactEntryCount =
        0

    private var dietArtifactEntryCount =
        0

    private var animalWelfareArtifactEntryCount =
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

                    KnowledgeDimensionCandidateType.ALLERGENS ->
                        allergensCandidateCount++

                    KnowledgeDimensionCandidateType.TAXONOMY ->
                        taxonomyCandidateCount++

                    KnowledgeDimensionCandidateType.PROCESSING ->
                        processingCandidateCount++

                    KnowledgeDimensionCandidateType.WATER ->
                        waterCandidateCount++

                    KnowledgeDimensionCandidateType.WATER_STRESS ->
                        waterStressCandidateCount++

                    KnowledgeDimensionCandidateType.PESTICIDES ->
                        pesticidesCandidateCount++

                    KnowledgeDimensionCandidateType.FOOD_MILES ->
                        foodMilesCandidateCount++

                    KnowledgeDimensionCandidateType.NUTRI_SCORE ->
                        nutriScoreCandidateCount++

                    KnowledgeDimensionCandidateType.DIET ->
                        dietCandidateCount++

                    KnowledgeDimensionCandidateType.ANIMAL_WELFARE ->
                        animalWelfareCandidateCount++

                    else -> {
                        /*
                         * Non-active and not-yet-integrated dimensions are not
                         * part of the productive 12-dimension artifact build.
                         */
                    }
                }
            }
        }
    }

    fun addArtifactEntryCounts(
        nutrition: Int,
        environmentalImpact: Int,
        allergens: Int,
        taxonomy: Int,
        processing: Int,
        water: Int,
        waterStress: Int,
        pesticides: Int,
        foodMiles: Int,
        nutriScore: Int,
        diet: Int,
        animalWelfare: Int
    ) {

        nutritionArtifactEntryCount +=
            nutrition

        environmentalImpactArtifactEntryCount +=
            environmentalImpact

        allergenArtifactEntryCount +=
            allergens

        taxonomyArtifactEntryCount +=
            taxonomy

        processingArtifactEntryCount +=
            processing

        waterArtifactEntryCount +=
            water

        waterStressArtifactEntryCount +=
            waterStress

        pesticidesArtifactEntryCount +=
            pesticides

        foodMilesArtifactEntryCount +=
            foodMiles

        nutriScoreArtifactEntryCount +=
            nutriScore

        dietArtifactEntryCount +=
            diet

        animalWelfareArtifactEntryCount +=
            animalWelfare
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

            /*
             * Legacy dimensions are no longer produced.
             */
            ingredientsCandidateCount =
                0,
            allergensCandidateCount =
                allergensCandidateCount,
            packagingCandidateCount =
                0,
            taxonomyCandidateCount =
                taxonomyCandidateCount,
            processingCandidateCount =
                processingCandidateCount,
            waterCandidateCount =
                waterCandidateCount,
            waterStressCandidateCount =
                waterStressCandidateCount,
            biodiversityCandidateCount =
                0,
            pollinatorCandidateCount =
                0,
            pesticidesCandidateCount =
                pesticidesCandidateCount,
            productionCandidateCount =
                0,
            foodMilesCandidateCount =
                foodMilesCandidateCount,
            localityCandidateCount =
                0,
            nutriScoreCandidateCount =
                nutriScoreCandidateCount,
            seasonalityCandidateCount =
                0,
            dietCandidateCount =
                dietCandidateCount,
            fairTradeCandidateCount =
                0,
            animalWelfareCandidateCount =
                animalWelfareCandidateCount,
            recipeCandidateCount =
                0,
            ingredientGraphCandidateCount =
                0,
            recipeGraphCandidateCount =
                0,

            nutritionArtifactEntryCount =
                nutritionArtifactEntryCount,
            environmentalImpactArtifactEntryCount =
                environmentalImpactArtifactEntryCount,
            ingredientsArtifactEntryCount =
                0,
            allergenArtifactEntryCount =
                allergenArtifactEntryCount,
            packagingArtifactEntryCount =
                0,
            taxonomyArtifactEntryCount =
                taxonomyArtifactEntryCount,
            processingArtifactEntryCount =
                processingArtifactEntryCount,
            waterArtifactEntryCount =
                waterArtifactEntryCount,
            waterStressArtifactEntryCount =
                waterStressArtifactEntryCount,
            biodiversityArtifactEntryCount =
                0,
            pollinatorArtifactEntryCount =
                0,
            pesticidesArtifactEntryCount =
                pesticidesArtifactEntryCount,
            productionArtifactEntryCount =
                0,
            foodMilesArtifactEntryCount =
                foodMilesArtifactEntryCount,
            localityArtifactEntryCount =
                0,
            nutriScoreArtifactEntryCount =
                nutriScoreArtifactEntryCount,
            seasonalityArtifactEntryCount =
                0,
            dietArtifactEntryCount =
                dietArtifactEntryCount,
            fairTradeArtifactEntryCount =
                0,
            animalWelfareArtifactEntryCount =
                animalWelfareArtifactEntryCount,
            recipeArtifactEntryCount =
                0,
            ingredientGraphArtifactEntryCount =
                0,
            recipeGraphArtifactEntryCount =
                0
        )
}