package de.shopme.tools.knowledge.ai.builder.runtime

import de.shopme.tools.knowledge.agribalyse.extractor.AgribalyseCandidateExtractor
import de.shopme.tools.knowledge.ai.builder.runtime.partition.PartitionedRuntimeKnowledgeArtifactBuild
import de.shopme.tools.knowledge.ai.builder.runtime.partition.PartitionedRuntimeKnowledgeArtifactShardMerger
import de.shopme.tools.knowledge.ciqual.extractor.CiqualNutritionCandidateExtractor
import de.shopme.tools.knowledge.ciqual.model.CiqualSourceFiles
import de.shopme.tools.knowledge.ki_candidates.CanonicalKnowledgeCandidate
import de.shopme.tools.knowledge.ki_candidates.KnowledgeDimensionCandidateType
import de.shopme.tools.knowledge.ki_candidates.normalizer.KnowledgeCandidateNormalizer
import de.shopme.tools.knowledge.ki_candidates.partition.KnowledgeCandidatePartitioner
import de.shopme.tools.knowledge.ki_candidates.partition.PartitionedKnowledgeCandidateMerger
import de.shopme.tools.knowledge.ki_candidates.partition.PartitionedKnowledgeCandidateStore
import de.shopme.tools.knowledge.off.extractor.OFFCandidateExtractor
import de.shopme.tools.knowledge.off.nutrition.reference.adapter.OFFNutritionAggregateKnowledgeCandidateAdapter
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.OFFNutritionReferenceAggregateDatasetReader
import java.io.File

class MultiSourceRuntimeKnowledgeBuild {

    fun build(
        offFile: File,
        offNutritionAggregateFile: File,
        agribalyseFile: File,
        outputDir: File,
        maxOffCandidates: Int? = null,
        maxOffNutritionAggregates: Int? = null,
        ciqualDirectory: File? = null,
        candidatePartitionCount: Int =
            KnowledgeCandidatePartitioner.DEFAULT_PARTITION_COUNT
    ): MultiSourceRuntimeKnowledgeBuildResult {

        require(offFile.isFile) {
            "OFF input file does not exist: ${offFile.path}"
        }

        require(offNutritionAggregateFile.isFile) {
            "OFF nutrition aggregate file does not exist: " +
                    offNutritionAggregateFile.path
        }

        require(agribalyseFile.isFile) {
            "Agribalyse input file does not exist: ${agribalyseFile.path}"
        }

        require(candidatePartitionCount > 0) {
            "candidatePartitionCount must be greater than zero."
        }

        require(
            outputDir.mkdirs() ||
                    outputDir.isDirectory
        ) {
            "Could not create output directory: ${outputDir.path}"
        }

        val normalizer =
            KnowledgeCandidateNormalizer()

        val partitionDirectory =
            outputDir.resolve(
                CANDIDATE_PARTITION_DIRECTORY_NAME
            )

        resetDirectory(
            directory =
                partitionDirectory,
            description =
                "candidate partition directory"
        )

        val partitionStore =
            PartitionedKnowledgeCandidateStore(
                directory =
                    partitionDirectory,
                partitioner =
                    KnowledgeCandidatePartitioner(
                        partitionCount =
                            candidatePartitionCount
                    )
            )

        var offCandidateCount =
            0

        var offNutritionAggregateCount =
            0

        var normalizedCandidateCount =
            0

        val batch =
            mutableListOf<CanonicalKnowledgeCandidate>()

        fun flushBatch() {
            if (batch.isEmpty()) {
                return
            }

            val normalizedBatch =
                batch.map { candidate ->
                    normalizer.normalize(
                        candidate
                    )
                }

            normalizedCandidateCount +=
                normalizedBatch.size

            partitionStore.appendAll(
                normalizedBatch
            )

            batch.clear()
        }

        fun appendCandidate(
            candidate: CanonicalKnowledgeCandidate
        ) {
            batch +=
                candidate

            if (batch.size >= CANDIDATE_BATCH_SIZE) {
                flushBatch()
            }
        }

        try {
            /*
             * Das rohe OFF-Dataset bleibt Quelle für alle Dimensionen
             * außer Nutrition.
             *
             * Die ungefilterte OFF-Nutrition-Dimension wird entfernt.
             * Für OFF-Nutrition werden ausschließlich die validierten
             * Aggregate verwendet.
             */
            OFFCandidateExtractor()
                .forEachCandidate(
                    file =
                        offFile,
                    maxCandidates =
                        maxOffCandidates
                ) { rawCandidate ->

                    offCandidateCount++

                    val nonNutritionDimensions =
                        rawCandidate.dimensions
                            .filterNot { dimension ->
                                dimension.dimension ==
                                        KnowledgeDimensionCandidateType.NUTRITION
                            }

                    if (nonNutritionDimensions.isNotEmpty()) {
                        appendCandidate(
                            rawCandidate.copy(
                                dimensions =
                                    nonNutritionDimensions
                            )
                        )
                    }

                    if (
                        offCandidateCount %
                        PROGRESS_INTERVAL ==
                        0
                    ) {
                        println(
                            "OFF raw processed=" +
                                    offCandidateCount +
                                    " partitioned=" +
                                    partitionStore.candidateCount()
                        )
                    }
                }

            flushBatch()

            val aggregateAdapter =
                OFFNutritionAggregateKnowledgeCandidateAdapter()

            OFFNutritionReferenceAggregateDatasetReader()
                .forEachAggregate(
                    inputFile =
                        offNutritionAggregateFile,
                    maxAggregates =
                        maxOffNutritionAggregates
                ) { aggregate ->

                    appendCandidate(
                        aggregateAdapter.adapt(
                            aggregate =
                                aggregate
                        )
                    )

                    offNutritionAggregateCount++

                    if (
                        offNutritionAggregateCount %
                        PROGRESS_INTERVAL ==
                        0
                    ) {
                        println(
                            "OFF nutrition aggregates processed=" +
                                    offNutritionAggregateCount +
                                    " partitioned=" +
                                    partitionStore.candidateCount()
                        )
                    }
                }

            flushBatch()

            val agribalyseCandidates =
                AgribalyseCandidateExtractor()
                    .extract(
                        file =
                            agribalyseFile
                    )

            val normalizedAgribalyseCandidates =
                normalizer.normalize(
                    candidates =
                        agribalyseCandidates
                )

            normalizedCandidateCount +=
                normalizedAgribalyseCandidates.size

            partitionStore.appendAll(
                normalizedAgribalyseCandidates
            )

            val ciqualCandidates =
                ciqualDirectory
                    ?.let { directory ->
                        require(directory.isDirectory) {
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
                    candidates =
                        ciqualCandidates
                )

            normalizedCandidateCount +=
                normalizedCiqualCandidates.size

            partitionStore.appendAll(
                normalizedCiqualCandidates
            )

            /*
             * Alle Writer des Partition Stores müssen vor dem Lesen
             * geschlossen sein.
             */
            partitionStore.close()

            val inputCandidateCount =
                offCandidateCount +
                        offNutritionAggregateCount +
                        agribalyseCandidates.size +
                        ciqualCandidates.size

            val artifactShardDirectory =
                outputDir.resolve(
                    RUNTIME_ARTIFACT_SHARD_DIRECTORY_NAME
                )

            resetDirectory(
                directory =
                    artifactShardDirectory,
                description =
                    "runtime artifact shard directory"
            )

            val partitionedArtifactBuild =
                PartitionedRuntimeKnowledgeArtifactBuild(
                    shardRootDirectory =
                        artifactShardDirectory
                )

            /*
             * Jede Partition wird isoliert gemergt, in Runtime-Artefakte
             * überführt und unmittelbar als Shard persistiert.
             *
             * Es wird ausdrücklich keine globale merged-Liste erzeugt.
             */
            val partitionMergeResult =
                PartitionedKnowledgeCandidateMerger(
                    store =
                        partitionStore
                )
                    .forEachMergedPartition {
                            partitionIndex,
                            mergedCandidates,
                            partitionConflictCount,
                            partitionBlockedHighFanoutKeys ->

                        println(
                            "Knowledge partition processed=" +
                                    partitionIndex +
                                    " mergedCandidates=" +
                                    mergedCandidates.size +
                                    " conflicts=" +
                                    partitionConflictCount +
                                    " blockedHighFanoutKeys=" +
                                    partitionBlockedHighFanoutKeys.size
                        )

                        partitionedArtifactBuild.addPartition(
                            partitionIndex =
                                partitionIndex,
                            mergedCandidates =
                                mergedCandidates
                        )
                    }

            val partitionedArtifactBuildResult =
                partitionedArtifactBuild.finish()

            val artifactCounts =
                partitionedArtifactBuildResult.counts

            val artifactShardMergeResult =
                PartitionedRuntimeKnowledgeArtifactShardMerger(
                    shardRootDirectory =
                        artifactShardDirectory,
                    outputDirectory =
                        outputDir
                )
                    .merge()

            println(
                "Runtime artifact shard merge completed " +
                        "artifacts=" +
                        artifactShardMergeResult.artifactCount +
                        " shards=" +
                        artifactShardMergeResult.totalShardCount +
                        " entries=" +
                        artifactShardMergeResult.totalEntryCount
            )

            /*
             * In diesem Commit werden die partitionsweisen Runtime-
             * Artefakte als Shards erzeugt.
             *
             * Die folgenden File-Objekte definieren bereits die finalen
             * Zielpfade. Die Dateien selbst werden im nachfolgenden
             * External-Shard-Merge erzeugt.
             */
            val nutritionFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        NUTRITION_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            val environmentalImpactFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        ENVIRONMENTAL_IMPACT_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            val ingredientsFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        INGREDIENTS_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            val allergenFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        ALLERGENS_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            val packagingFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        PACKAGING_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            val foodTaxonomyFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        FOOD_TAXONOMY_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            val processingFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        PROCESSING_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            val waterFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        WATER_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            val waterStressFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        WATER_STRESS_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            val biodiversityFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        BIODIVERSITY_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            val pollinatorFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        POLLINATOR_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            val pesticidesFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        PESTICIDES_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            val productionFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        PRODUCTION_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            val foodMilesFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        FOOD_MILES_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            val localityFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        LOCALITY_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            val nutriScoreFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        NUTRI_SCORE_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            val seasonalityFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        SEASONALITY_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            val dietFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        DIET_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            val fairTradeFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        FAIRTRADE_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            val animalWelfareFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        ANIMAL_WELFARE_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            val recipeFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        RECIPES_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            val ingredientGraphFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        INGREDIENT_GRAPH_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            val recipeGraphFile =
                artifactShardMergeResult.outputFileOrDefault(
                    fileName =
                        RECIPE_GRAPH_FILE_NAME,
                    outputDirectory =
                        outputDir
                )

            return MultiSourceRuntimeKnowledgeBuildResult(
                offNutritionAggregateCount =
                    offNutritionAggregateCount,
                offCandidateCount =
                    offCandidateCount,
                agribalyseCandidateCount =
                    agribalyseCandidates.size,
                ciqualCandidateCount =
                    ciqualCandidates.size,
                inputCandidateCount =
                    inputCandidateCount,
                normalizedCandidateCount =
                    normalizedCandidateCount,
                mergedCandidateCount =
                    partitionMergeResult
                        .mergedCandidateCount
                        .toInt(),
                conflictCount =
                    partitionMergeResult
                        .conflictCount
                        .toInt(),
                blockedHighFanoutKeys =
                    partitionMergeResult
                        .blockedHighFanoutKeys,

                nutritionCandidateCount =
                    artifactCounts
                        .nutritionCandidateCount,
                nutritionArtifactEntryCount =
                    artifactCounts
                        .nutritionArtifactEntryCount,
                nutritionArtifactFile =
                    nutritionFile,

                environmentalImpactCandidateCount =
                    artifactCounts
                        .environmentalCandidateCount,
                environmentalImpactArtifactEntryCount =
                    artifactCounts
                        .environmentalImpactArtifactEntryCount,
                environmentalImpactArtifactFile =
                    environmentalImpactFile,

                multiDimensionCandidateCount =
                    artifactCounts
                        .multiDimensionCandidateCount,

                ingredientsCandidateCount =
                    artifactCounts
                        .ingredientsCandidateCount,
                ingredientsArtifactEntryCount =
                    artifactCounts
                        .ingredientsArtifactEntryCount,
                ingredientsArtifactFile =
                    ingredientsFile,

                allergensCandidateCount =
                    artifactCounts
                        .allergensCandidateCount,
                allergenArtifactEntryCount =
                    artifactCounts
                        .allergenArtifactEntryCount,
                allergenArtifactFile =
                    allergenFile,

                packagingCandidateCount =
                    artifactCounts
                        .packagingCandidateCount,
                packagingArtifactEntryCount =
                    artifactCounts
                        .packagingArtifactEntryCount,
                packagingArtifactFile =
                    packagingFile,

                taxonomyCandidateCount =
                    artifactCounts
                        .taxonomyCandidateCount,
                taxonomyArtifactEntryCount =
                    artifactCounts
                        .taxonomyArtifactEntryCount,
                taxonomyArtifactFile =
                    foodTaxonomyFile,

                processingCandidateCount =
                    artifactCounts
                        .processingCandidateCount,
                processingArtifactEntryCount =
                    artifactCounts
                        .processingArtifactEntryCount,
                processingArtifactFile =
                    processingFile,

                waterCandidateCount =
                    artifactCounts
                        .waterCandidateCount,
                waterArtifactEntryCount =
                    artifactCounts
                        .waterArtifactEntryCount,
                waterArtifactFile =
                    waterFile,

                waterStressCandidateCount =
                    artifactCounts
                        .waterStressCandidateCount,
                waterStressArtifactEntryCount =
                    artifactCounts
                        .waterStressArtifactEntryCount,
                waterStressArtifactFile =
                    waterStressFile,

                biodiversityCandidateCount =
                    artifactCounts
                        .biodiversityCandidateCount,
                biodiversityArtifactEntryCount =
                    artifactCounts
                        .biodiversityArtifactEntryCount,
                biodiversityArtifactFile =
                    biodiversityFile,

                pollinatorCandidateCount =
                    artifactCounts
                        .pollinatorCandidateCount,
                pollinatorArtifactEntryCount =
                    artifactCounts
                        .pollinatorArtifactEntryCount,
                pollinatorArtifactFile =
                    pollinatorFile,

                pesticidesCandidateCount =
                    artifactCounts
                        .pesticidesCandidateCount,
                pesticidesArtifactEntryCount =
                    artifactCounts
                        .pesticidesArtifactEntryCount,
                pesticidesArtifactFile =
                    pesticidesFile,

                productionCandidateCount =
                    artifactCounts
                        .productionCandidateCount,
                productionArtifactEntryCount =
                    artifactCounts
                        .productionArtifactEntryCount,
                productionArtifactFile =
                    productionFile,

                foodMilesCandidateCount =
                    artifactCounts
                        .foodMilesCandidateCount,
                foodMilesArtifactEntryCount =
                    artifactCounts
                        .foodMilesArtifactEntryCount,
                foodMilesArtifactFile =
                    foodMilesFile,

                localityCandidateCount =
                    artifactCounts
                        .localityCandidateCount,
                localityArtifactEntryCount =
                    artifactCounts
                        .localityArtifactEntryCount,
                localityArtifactFile =
                    localityFile,

                nutriScoreCandidateCount =
                    artifactCounts
                        .nutriScoreCandidateCount,
                nutriScoreArtifactEntryCount =
                    artifactCounts
                        .nutriScoreArtifactEntryCount,
                nutriScoreArtifactFile =
                    nutriScoreFile,

                seasonalityCandidateCount =
                    artifactCounts
                        .seasonalityCandidateCount,
                seasonalityArtifactEntryCount =
                    artifactCounts
                        .seasonalityArtifactEntryCount,
                seasonalityArtifactFile =
                    seasonalityFile,

                dietCandidateCount =
                    artifactCounts
                        .dietCandidateCount,
                dietArtifactEntryCount =
                    artifactCounts
                        .dietArtifactEntryCount,
                dietArtifactFile =
                    dietFile,

                fairTradeCandidateCount =
                    artifactCounts
                        .fairTradeCandidateCount,
                fairTradeArtifactEntryCount =
                    artifactCounts
                        .fairTradeArtifactEntryCount,
                fairTradeArtifactFile =
                    fairTradeFile,

                animalWelfareCandidateCount =
                    artifactCounts
                        .animalWelfareCandidateCount,
                animalWelfareArtifactEntryCount =
                    artifactCounts
                        .animalWelfareArtifactEntryCount,
                animalWelfareArtifactFile =
                    animalWelfareFile,

                recipeCandidateCount =
                    artifactCounts
                        .recipeCandidateCount,
                recipeArtifactEntryCount =
                    artifactCounts
                        .recipeArtifactEntryCount,
                recipeArtifactFile =
                    recipeFile,

                ingredientGraphCandidateCount =
                    artifactCounts
                        .ingredientGraphCandidateCount,
                ingredientGraphArtifactEntryCount =
                    artifactCounts
                        .ingredientGraphArtifactEntryCount,
                ingredientGraphArtifactFile =
                    ingredientGraphFile,

                recipeGraphCandidateCount =
                    artifactCounts
                        .recipeGraphCandidateCount,
                recipeGraphArtifactEntryCount =
                    artifactCounts
                        .recipeGraphArtifactEntryCount,
                recipeGraphArtifactFile =
                    recipeGraphFile
            )
        } finally {
            partitionStore.close()
        }
    }

    private fun resetDirectory(
        directory: File,
        description: String
    ) {
        if (directory.exists()) {
            require(
                directory.deleteRecursively()
            ) {
                "Could not delete previous $description: " +
                        directory.path
            }
        }

        require(
            directory.mkdirs() ||
                    directory.isDirectory
        ) {
            "Could not create $description: " +
                    directory.path
        }
    }

    private companion object {

        const val CANDIDATE_BATCH_SIZE =
            1_000

        const val PROGRESS_INTERVAL =
            100_000

        const val CANDIDATE_PARTITION_DIRECTORY_NAME =
            ".candidate-partitions"

        const val RUNTIME_ARTIFACT_SHARD_DIRECTORY_NAME =
            ".runtime-artifact-shards"

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
    }
}