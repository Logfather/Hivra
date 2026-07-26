package de.shopme.tools.knowledge.ai.builder.runtime

import de.shopme.tools.knowledge.agribalyse.extractor.AgribalyseCandidateExtractor
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
import de.shopme.tools.knowledge.ciqual.extractor.CiqualNutritionCandidateExtractor
import de.shopme.tools.knowledge.ciqual.model.CiqualSourceFiles
import de.shopme.tools.knowledge.ki_candidates.CanonicalKnowledgeCandidate
import de.shopme.tools.knowledge.ki_candidates.KnowledgeCandidateMergeAccumulator
import de.shopme.tools.knowledge.ki_candidates.KnowledgeDimensionCandidateType
import de.shopme.tools.knowledge.ki_candidates.normalizer.KnowledgeCandidateNormalizer
import de.shopme.tools.knowledge.off.extractor.OFFCandidateExtractor
import java.io.File

class MultiSourceRuntimeKnowledgeBuild {

    fun build(
        offFile: File,
        agribalyseFile: File,
        outputDir: File,
        maxOffCandidates: Int? = null,
        ciqualDirectory: File? = null
    ): MultiSourceRuntimeKnowledgeBuildResult {

        val offBatchSize =
            1_000

        val normalizer =
            KnowledgeCandidateNormalizer()

        val accumulator =
            KnowledgeCandidateMergeAccumulator()

        var offCandidateCount =
            0

        var normalizedCandidateCount =
            0

        val batch =
            mutableListOf<CanonicalKnowledgeCandidate>()

        OFFCandidateExtractor()
            .forEachCandidate(
                file = offFile,
                maxCandidates = maxOffCandidates
            ) { candidate ->

                batch += candidate
                offCandidateCount++

                if (offCandidateCount % 100_000 == 0) {
                    println(
                        "OFF processed=$offCandidateCount " +
                                "merged=${accumulator.candidateCount()}"
                    )
                }

                if (batch.size >= offBatchSize) {
                    val normalizedBatch =
                        batch.map { batchCandidate ->
                            normalizer.normalize(batchCandidate)
                        }

                    normalizedCandidateCount +=
                        normalizedBatch.size

                    accumulator.add(
                        normalizedBatch
                    )

                    batch.clear()
                }
            }

        if (batch.isNotEmpty()) {
            val normalizedBatch =
                batch.map { candidate ->
                    normalizer.normalize(candidate)
                }

            normalizedCandidateCount +=
                normalizedBatch.size

            accumulator.add(
                normalizedBatch
            )

            batch.clear()
        }

        val agribalyseCandidates =
            AgribalyseCandidateExtractor()
                .extract(
                    file = agribalyseFile
                )



        val normalizedAgribalyseCandidates =
            normalizer.normalize(
                candidates = agribalyseCandidates
            )

        normalizedCandidateCount +=
            normalizedAgribalyseCandidates.size

        accumulator.add(
            normalizedAgribalyseCandidates
        )

        val ciqualCandidates =
            ciqualDirectory
                ?.let { directory ->
                    require(
                        directory.isDirectory
                    ) {
                        "CIQUAL directory does not exist or is not a directory: " +
                                directory.path
                    }

                    CiqualNutritionCandidateExtractor()
                        .extract(
                            files =
                                CiqualSourceFiles.fromDirectory(
                                    directory
                                )
                        )
                }
                .orEmpty()

        val normalizedCiqualCandidates =
            normalizer.normalize(
                candidates = ciqualCandidates
            )

        normalizedCandidateCount +=
            normalizedCiqualCandidates.size

        accumulator.add(
            normalizedCiqualCandidates
        )

        val merged =
            accumulator.candidates()

        val inputCandidateCount =
            offCandidateCount +
                    agribalyseCandidates.size +
                    ciqualCandidates.size

        //KNOWLEDGE ARTIFACTS//
        //________________________________________//

        val waterKnowledge =
            MergedCandidateWaterKnowledgeBuilder()
                .build(merged)

        val waterStressKnowledge =
            MergedCandidateWaterStressKnowledgeBuilder()
                .build(merged)

        val nutritionKnowledge =
            MergedCandidateNutritionKnowledgeBuilder()
                .build(merged)

        val environmentalImpactKnowledge =
            MergedCandidateEnvironmentalImpactKnowledgeBuilder()
                .build(merged)

        val ingredientsKnowledge =
            MergedCandidateIngredientsKnowledgeBuilder()
                .build(merged)

        val allergenKnowledge =
            MergedCandidateAllergenKnowledgeBuilder()
                .build(merged)

        val packagingKnowledge =
            MergedCandidatePackagingKnowledgeBuilder()
                .build(merged)

        val foodTaxonomyKnowledge =
            MergedCandidateFoodTaxonomyKnowledgeBuilder()
                .build(merged)

        val biodiversityKnowledge =
            MergedCandidateBiodiversityKnowledgeBuilder()
                .build(merged)

        val processingKnowledge =
            MergedCandidateProcessingKnowledgeBuilder()
                .build(merged)

        val pollinatorKnowledge =
            MergedCandidatePollinatorKnowledgeBuilder()
                .build(merged)

        val pesticidesKnowledge =
            MergedCandidatePesticidesKnowledgeBuilder()
                .build(merged)

        val productionKnowledge =
            MergedCandidateProductionKnowledgeBuilder()
                .build(merged)

        val foodMilesKnowledge =
            MergedCandidateFoodMilesKnowledgeBuilder()
                .build(merged)

        val localityKnowledge =
            MergedCandidateLocalityKnowledgeBuilder()
                .build(merged)

        val nutriScoreKnowledge =
            MergedCandidateNutriScoreKnowledgeBuilder()
                .build(merged)

        val seasonalityKnowledge =
            MergedCandidateSeasonalityKnowledgeBuilder()
                .build(merged)

        val dietKnowledge =
            MergedCandidateDietKnowledgeBuilder()
                .build(merged)

        val fairTradeKnowledge =
            MergedCandidateFairtradeKnowledgeBuilder()
                .build(
                    candidates = merged
                )

        val animalWelfareKnowledge =
            MergedCandidateAnimalWelfareKnowledgeBuilder()
                .build(
                    candidates = merged
                )

        val recipeKnowledge =
            MergedCandidateRecipeKnowledgeBuilder()
                .build(
                    candidates = merged
                )

        val ingredientGraphKnowledge =
            MergedCandidateIngredientGraphKnowledgeBuilder()
                .build(
                    candidates = merged
                )

        val recipeGraphKnowledge =
            MergedCandidateRecipeGraphKnowledgeBuilder()
                .build(
                    candidates = merged
                )

        //KNOWLEDGE WRITER//
        //________________________________________//

        val writer =
            GeneratedKnowledgeArtifactWriter()

        val nutritionFile =
            writer.write(
                outputDir = outputDir,
                fileName = "nutrition.json",
                artifact = nutritionKnowledge
            )

        val environmentalImpactFile =
            writer.write(
                outputDir = outputDir,
                fileName = "environmental_impact.json",
                artifact = environmentalImpactKnowledge
            )

        val ingredientsFile =
            writer.write(
                outputDir = outputDir,
                fileName = "ingredients.json",
                artifact = ingredientsKnowledge
            )

        val allergenFile =
            writer.write(
                outputDir = outputDir,
                fileName = "allergens.json",
                artifact = allergenKnowledge
            )

        val packagingFile =
            writer.write(
                outputDir = outputDir,
                fileName = "packaging.json",
                artifact = packagingKnowledge
            )

        val foodTaxonomyFile =
            writer.write(
                outputDir = outputDir,
                fileName = "food_taxonomy.json",
                artifact = foodTaxonomyKnowledge
            )

        val processingFile =
            writer.write(
                outputDir = outputDir,
                fileName = "processing.json",
                artifact = processingKnowledge
            )

        val waterFile =
            writer.write(
                outputDir = outputDir,
                fileName = "water_footprint.json",
                artifact = waterKnowledge
            )

        val waterStressFile =
            writer.write(
                outputDir = outputDir,
                fileName = "water_stress.json",
                artifact = waterStressKnowledge
            )

        val biodiversityFile =
            writer.write(
                outputDir = outputDir,
                fileName = "biodiversity.json",
                artifact = biodiversityKnowledge
            )

        val pollinatorFile =
            writer.write(
                outputDir = outputDir,
                fileName = "pollinator.json",
                artifact = pollinatorKnowledge
            )

        val pesticidesFile =
            writer.write(
                outputDir = outputDir,
                fileName = "pesticides.json",
                artifact = pesticidesKnowledge
            )

        val productionFile =
            writer.write(
                outputDir = outputDir,
                fileName = "production.json",
                artifact = productionKnowledge
            )

        val foodMilesFile =
            writer.write(
                outputDir = outputDir,
                fileName = "food_miles.json",
                artifact = foodMilesKnowledge
            )

        val localityFile =
            writer.write(
                outputDir = outputDir,
                fileName = "locality.json",
                artifact = localityKnowledge
            )

        val nutriScoreFile =
            writer.write(
                outputDir = outputDir,
                fileName = "nutri_score.json",
                artifact = nutriScoreKnowledge
            )

        val seasonalityFile =
            writer.write(
                outputDir = outputDir,
                fileName = "seasonality.json",
                artifact = seasonalityKnowledge
            )

        val dietFile =
            writer.write(
                outputDir = outputDir,
                fileName = "diet_classification.json",
                artifact = dietKnowledge
            )

        val fairTradeFile =
            if (fairTradeKnowledge.entries.isNotEmpty()) {
                writer.write(
                    outputDir = outputDir,
                    fileName = "fairtrade.json",
                    artifact = fairTradeKnowledge
                )
            } else {
                outputDir.resolve(
                    "fairtrade.json"
                )
            }

        val animalWelfareFile =
            if (animalWelfareKnowledge.entries.isNotEmpty()) {
                writer.write(
                    outputDir = outputDir,
                    fileName = "animal_welfare.json",
                    artifact = animalWelfareKnowledge
                )
            } else {
                outputDir.resolve(
                    "animal_welfare.json"
                )
            }


        val recipeFile =
            if (recipeKnowledge.entries.isNotEmpty()) {
                writer.write(
                    outputDir = outputDir,
                    fileName = "recipes.json",
                    artifact = recipeKnowledge
                )
            } else {
                outputDir.resolve(
                    "recipes.json"
                )
            }

        val ingredientGraphFile =
            if (ingredientGraphKnowledge.entries.isNotEmpty()) {
                writer.write(
                    outputDir = outputDir,
                    fileName = "ingredient_graph.json",
                    artifact = ingredientGraphKnowledge
                )
            } else {
                outputDir.resolve(
                    "ingredient_graph.json"
                )
            }

        val recipeGraphFile =
            if (recipeGraphKnowledge.entries.isNotEmpty()) {
                writer.write(
                    outputDir = outputDir,
                    fileName = "recipe_graph.json",
                    artifact = recipeGraphKnowledge
                )
            } else {
                outputDir.resolve(
                    "recipe_graph.json"
                )
            }

        //KNOWLEDGE CANDIDATES COUNTS//
        //________________________________________//

        val nutritionCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.NUTRITION
                }
            }

        val ingredientsCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.INGREDIENTS
                }
            }

        val environmentalCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.ENVIRONMENTAL_IMPACT
                }
            }

        val multiDimensionCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.size > 1
            }

        val allergensCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.ALLERGENS
                }
            }

        val taxonomyCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.TAXONOMY
                }
            }

        val packagingCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.PACKAGING
                }
            }

        val processingCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.PROCESSING
                }
            }

        val waterCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.WATER
                }
            }

        val waterStressCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.WATER_STRESS
                }
            }

        val biodiversityCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.BIODIVERSITY
                }
            }

        val pollinatorCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.POLLINATOR
                }
            }

        val pesticidesCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.PESTICIDES
                }
            }

        val productionCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.PRODUCTION
                }
            }

        val foodMilesCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.FOOD_MILES
                }
            }

        val localityCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.LOCALITY
                }
            }

        val nutriScoreCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.NUTRI_SCORE
                }
            }

        val seasonalityCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.SEASONALITY
                }
            }

        val dietCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.DIET
                }
            }

        val fairTradeCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.FAIRTRADE
                }
            }

        val animalWelfareCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.ANIMAL_WELFARE
                }
            }

        val recipeCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.RECIPE
                }
            }

        val ingredientGraphCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.INGREDIENT_GRAPH
                }
            }

        val recipeGraphCandidateCount =
            merged.count { candidate ->
                candidate.dimensions.any { dimension ->
                    dimension.dimension ==
                            KnowledgeDimensionCandidateType.RECIPE_GRAPH
                }
            }

        return MultiSourceRuntimeKnowledgeBuildResult(
            offCandidateCount = offCandidateCount,
            agribalyseCandidateCount = agribalyseCandidates.size,
            ciqualCandidateCount = ciqualCandidates.size,
            inputCandidateCount = inputCandidateCount,
            normalizedCandidateCount = normalizedCandidateCount,
            mergedCandidateCount = accumulator.candidateCount(),
            conflictCount = accumulator.conflictCount(),
            nutritionCandidateCount = nutritionCandidateCount,
            environmentalImpactCandidateCount = environmentalCandidateCount,
            multiDimensionCandidateCount = multiDimensionCandidateCount,
            nutritionArtifactEntryCount = nutritionKnowledge.entries.size,
            environmentalImpactArtifactEntryCount = environmentalImpactKnowledge.entries.size,
            nutritionArtifactFile = nutritionFile,
            environmentalImpactArtifactFile = environmentalImpactFile,
            blockedHighFanoutKeys = accumulator.blockedHighFanoutKeys(),
            ingredientsCandidateCount = ingredientsCandidateCount,
            ingredientsArtifactEntryCount = ingredientsKnowledge.entries.size,
            ingredientsArtifactFile = ingredientsFile,
            allergensCandidateCount = allergensCandidateCount,
            allergenArtifactEntryCount = allergenKnowledge.entries.size,
            allergenArtifactFile = allergenFile,
            packagingCandidateCount = packagingCandidateCount,
            packagingArtifactEntryCount = packagingKnowledge.entries.size,
            packagingArtifactFile = packagingFile,
            taxonomyCandidateCount = taxonomyCandidateCount,
            taxonomyArtifactEntryCount = foodTaxonomyKnowledge.entries.size,
            taxonomyArtifactFile = foodTaxonomyFile,
            processingCandidateCount = processingCandidateCount,
            processingArtifactEntryCount = processingKnowledge.entries.size,
            processingArtifactFile = processingFile,
            waterCandidateCount = waterCandidateCount,
            waterArtifactEntryCount = waterKnowledge.entries.size,
            waterArtifactFile = waterFile,
            waterStressCandidateCount = waterStressCandidateCount,
            waterStressArtifactEntryCount = waterStressKnowledge.entries.size,
            waterStressArtifactFile = waterStressFile,
            biodiversityCandidateCount = biodiversityCandidateCount,
            biodiversityArtifactEntryCount = biodiversityKnowledge.entries.size,
            biodiversityArtifactFile = biodiversityFile,
            pollinatorCandidateCount = pollinatorCandidateCount,
            pollinatorArtifactEntryCount = pollinatorKnowledge.entries.size,
            pollinatorArtifactFile = pollinatorFile,
            pesticidesCandidateCount = pesticidesCandidateCount,
            pesticidesArtifactEntryCount = pesticidesKnowledge.entries.size,
            pesticidesArtifactFile = pesticidesFile,
            productionCandidateCount = productionCandidateCount,
            productionArtifactEntryCount = productionKnowledge.entries.size,
            productionArtifactFile = productionFile,
            foodMilesCandidateCount = foodMilesCandidateCount,
            foodMilesArtifactEntryCount = foodMilesKnowledge.entries.size,
            foodMilesArtifactFile = foodMilesFile,
            localityCandidateCount = localityCandidateCount,
            localityArtifactEntryCount = localityKnowledge.entries.size,
            localityArtifactFile = localityFile,
            nutriScoreCandidateCount = nutriScoreCandidateCount,
            nutriScoreArtifactEntryCount = nutriScoreKnowledge.entries.size,
            nutriScoreArtifactFile = nutriScoreFile,
            seasonalityCandidateCount = seasonalityCandidateCount,
            seasonalityArtifactEntryCount = seasonalityKnowledge.entries.size,
            seasonalityArtifactFile = seasonalityFile,
            dietCandidateCount = dietCandidateCount,
            dietArtifactEntryCount = dietKnowledge.entries.size,
            dietArtifactFile = dietFile,
            fairTradeCandidateCount = fairTradeCandidateCount,
            fairTradeArtifactEntryCount = fairTradeKnowledge.entries.size,
            fairTradeArtifactFile = fairTradeFile,
            animalWelfareCandidateCount = animalWelfareCandidateCount,
            animalWelfareArtifactEntryCount = animalWelfareKnowledge.entries.size,
            animalWelfareArtifactFile = animalWelfareFile,
            recipeCandidateCount = recipeCandidateCount,
            recipeArtifactEntryCount = recipeKnowledge.entries.size,
            recipeArtifactFile = recipeFile,
            ingredientGraphCandidateCount = ingredientGraphCandidateCount,
            ingredientGraphArtifactEntryCount = ingredientGraphKnowledge.entries.size,
            ingredientGraphArtifactFile = ingredientGraphFile,
            recipeGraphCandidateCount = recipeGraphCandidateCount,
            recipeGraphArtifactEntryCount = recipeGraphKnowledge.entries.size,
            recipeGraphArtifactFile = recipeGraphFile,
        )
    }
}