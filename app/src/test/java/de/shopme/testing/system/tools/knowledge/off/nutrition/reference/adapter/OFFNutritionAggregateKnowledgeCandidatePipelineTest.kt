package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.adapter

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.ki_candidates.KnowledgeDimensionCandidateType
import de.shopme.tools.knowledge.off.nutrition.reference.adapter.OFFNutritionAggregateKnowledgeCandidateAdapter
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.CanonicalOFFNutritionReferenceAggregate
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.OFFNutritionReferenceAggregateDatasetReader
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.OFFNutritionReferenceNutrientStatistics
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class OFFNutritionAggregateKnowledgeCandidatePipelineTest {

    @Test
    fun readAndAdapt_preservesDeterministicDatasetOrder() {

        val temporaryDirectory =
            Files.createTempDirectory(
                "off-nutrition-aggregate-adapter"
            )
                .toFile()

        try {
            val inputFile =
                temporaryDirectory.resolve(
                    "aggregates.json"
                )

            val aggregates =
                listOf(
                    createAggregate(
                        canonicalId =
                            "apple",
                        energy =
                            52.0
                    ),
                    createAggregate(
                        canonicalId =
                            "banana",
                        energy =
                            89.0
                    )
                )

            inputFile.writeText(
                GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create()
                    .toJson(
                        aggregates
                    ) +
                        "\n",
                StandardCharsets.UTF_8
            )

            val adapter =
                OFFNutritionAggregateKnowledgeCandidateAdapter()

            val candidates =
                mutableListOf<
                        de.shopme.tools.knowledge.ki_candidates
                        .CanonicalKnowledgeCandidate
                        >()

            val readCount =
                OFFNutritionReferenceAggregateDatasetReader()
                    .forEachAggregate(
                        inputFile =
                            inputFile
                    ) { aggregate ->

                        candidates +=
                            adapter.adapt(
                                aggregate =
                                    aggregate
                            )
                    }

            assertEquals(
                2,
                readCount
            )

            assertEquals(
                listOf(
                    "apple",
                    "banana"
                ),
                candidates.map { candidate ->
                    candidate.canonicalId
                }
            )

            assertEquals(
                listOf(
                    KnowledgeDimensionCandidateType.NUTRITION
                ),
                candidates
                    .first()
                    .dimensions
                    .map { dimension ->
                        dimension.dimension
                    }
            )

            assertEquals(
                sortedMapOf(
                    "energyKcalPer100g" to
                            52.0,
                    "fatPer100g" to
                            0.1
                ),
                candidates
                    .first()
                    .dimensions
                    .single()
                    .payload
            )
        } finally {
            temporaryDirectory.deleteRecursively()
        }
    }

    private fun createAggregate(
        canonicalId: String,
        energy: Double
    ): CanonicalOFFNutritionReferenceAggregate {

        val nutrition =
            sortedMapOf(
                "energyKcalPer100g" to
                        energy,
                "fatPer100g" to
                        0.1
            )

        val nutrientStatistics =
            nutrition
                .mapValues { (_, value) ->
                    OFFNutritionReferenceNutrientStatistics(
                        observationCount =
                            1,
                        minimum =
                            value,
                        median =
                            value,
                        maximum =
                            value
                    )
                }
                .toSortedMap()

        return CanonicalOFFNutritionReferenceAggregate(
            canonicalId =
                canonicalId,
            aliases =
                sortedSetOf(
                    canonicalId
                ),
            matchAliases =
                emptySet(),
            singleIngredientNutritionAliases =
                emptySet(),
            nutrition =
                nutrition,
            nutrientStatistics =
                nutrientStatistics,
            profileCount =
                1,
            sourceIds =
                listOf(
                    "source-$canonicalId"
                ),
            representativeSourceId =
                "source-$canonicalId",
            source =
                "open_food_facts_aggregate",
            sourceVersion =
                "1",
            sourceConfidence =
                1.0
        )
    }
}