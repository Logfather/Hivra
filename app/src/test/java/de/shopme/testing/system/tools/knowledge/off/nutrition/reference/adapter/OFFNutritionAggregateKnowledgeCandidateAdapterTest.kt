package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.adapter

import de.shopme.tools.knowledge.ki_candidates.KnowledgeDimensionCandidateType
import de.shopme.tools.knowledge.off.nutrition.reference.adapter.OFFNutritionAggregateKnowledgeCandidateAdapter
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.CanonicalOFFNutritionReferenceAggregate
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.OFFNutritionReferenceNutrientStatistics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class OFFNutritionAggregateKnowledgeCandidateAdapterTest {

    private val adapter =
        OFFNutritionAggregateKnowledgeCandidateAdapter()

    @Test
    fun adapt_mapsAggregateToCanonicalKnowledgeCandidate() {

        val aggregate =
            createAggregate(
                canonicalId =
                    "banana",
                aliases =
                    sortedSetOf(
                        "banana",
                        "banane"
                    ),
                matchAliases =
                    sortedSetOf(
                        "dessert banana",
                        "fruit banana"
                    ),
                singleIngredientNutritionAliases =
                    sortedSetOf(
                        "bananas",
                        "raw banana"
                    ),
                nutrition =
                    sortedMapOf(
                        "carbohydratesPer100g" to
                                22.8,
                        "energyKcalPer100g" to
                                89.0,
                        "fatPer100g" to
                                0.3,
                        "proteinsPer100g" to
                                1.1
                    ),
                sourceIds =
                    listOf(
                        "001",
                        "002",
                        "003"
                    ),
                representativeSourceId =
                    "002",
                source =
                    "open_food_facts_aggregate",
                sourceVersion =
                    "1",
                sourceConfidence =
                    0.95
            )

        val candidate =
            adapter.adapt(
                aggregate =
                    aggregate
            )

        assertEquals(
            "banana",
            candidate.canonicalId
        )

        assertEquals(
            sortedSetOf(
                "banana",
                "banane"
            ),
            candidate.aliases
        )

        assertEquals(
            sortedSetOf(
                "bananas",
                "dessert banana",
                "fruit banana",
                "raw banana"
            ),
            candidate.matchAliases
        )

        assertEquals(
            1,
            candidate.dimensions.size
        )

        val nutritionDimension =
            candidate.dimensions.single()

        assertEquals(
            KnowledgeDimensionCandidateType.NUTRITION,
            nutritionDimension.dimension
        )

        assertEquals(
            aggregate.nutrition,
            nutritionDimension.payload
        )

        assertEquals(
            "open_food_facts_aggregate",
            candidate.metadata.source
        )

        assertEquals(
            "002",
            candidate.metadata.sourceId
        )

        assertEquals(
            "1",
            candidate.metadata.version
        )

        assertEquals(
            0.95,
            candidate.metadata.confidence
        )

        assertEquals(
            sortedMapOf(
                OFFNutritionAggregateKnowledgeCandidateAdapter
                    .PROFILE_COUNT_ATTRIBUTE to
                        "3",
                OFFNutritionAggregateKnowledgeCandidateAdapter
                    .SOURCE_ID_COUNT_ATTRIBUTE to
                        "3"
            ),
            candidate.metadata.attributes
        )
    }

    @Test
    fun adapt_mergesSingleIngredientAliasesIntoMatchAliases() {

        val aggregate =
            createAggregate(
                canonicalId =
                    "apple",
                aliases =
                    sortedSetOf(
                        "apple"
                    ),
                matchAliases =
                    sortedSetOf(
                        "apples"
                    ),
                singleIngredientNutritionAliases =
                    sortedSetOf(
                        "raw apple"
                    )
            )

        val candidate =
            adapter.adapt(
                aggregate =
                    aggregate
            )

        assertEquals(
            sortedSetOf(
                "apples",
                "raw apple"
            ),
            candidate.matchAliases
        )
    }

    @Test
    fun adapt_removesCanonicalAndCanonicalAliasesFromMatchAliases() {

        val aggregate =
            createAggregate(
                canonicalId =
                    "apple",
                aliases =
                    sortedSetOf(
                        "apfel",
                        "apple"
                    ),
                matchAliases =
                    sortedSetOf(
                        "apfel",
                        "apple",
                        "fruit apple"
                    ),
                singleIngredientNutritionAliases =
                    sortedSetOf(
                        "apple",
                        "raw apple"
                    )
            )

        val candidate =
            adapter.adapt(
                aggregate =
                    aggregate
            )

        assertEquals(
            sortedSetOf(
                "fruit apple",
                "raw apple"
            ),
            candidate.matchAliases
        )
    }

    @Test
    fun adapt_trimsAliasesAndProducesDeterministicSets() {

        val aggregate =
            createAggregate(
                canonicalId =
                    "apple",
                aliases =
                    sortedSetOf(
                        " apple ",
                        "apfel"
                    ),
                matchAliases =
                    sortedSetOf(
                        " fruit apple ",
                        "malus domestica"
                    ),
                singleIngredientNutritionAliases =
                    sortedSetOf(
                        " raw apple "
                    )
            )

        val candidate =
            adapter.adapt(
                aggregate =
                    aggregate
            )

        assertEquals(
            listOf(
                "apfel",
                "apple"
            ),
            candidate.aliases.toList()
        )

        assertEquals(
            listOf(
                "fruit apple",
                "malus domestica",
                "raw apple"
            ),
            candidate.matchAliases.toList()
        )
    }

    @Test
    fun adapt_copiesNutritionPayloadIntoSortedMap() {

        val nutrition =
            sortedMapOf(
                "energyKcalPer100g" to
                        52.0,
                "fatPer100g" to
                        0.2
            )

        val aggregate =
            createAggregate(
                canonicalId =
                    "apple",
                nutrition =
                    nutrition
            )

        val candidate =
            adapter.adapt(
                aggregate =
                    aggregate
            )

        val payload =
            candidate.dimensions
                .single()
                .payload

        assertTrue(
            payload is Map<*, *>
        )

        @Suppress("UNCHECKED_CAST")
        val nutritionPayload =
            payload as Map<String, Double>

        assertEquals(
            listOf(
                "energyKcalPer100g",
                "fatPer100g"
            ),
            nutritionPayload.keys.toList()
        )

        assertEquals(
            nutrition,
            nutritionPayload
        )

        assertNotSame(
            aggregate.nutrition,
            nutritionPayload
        )
    }

    @Test
    fun adapt_multipleAggregatesPreservesInputOrder() {

        val banana =
            createAggregate(
                canonicalId =
                    "banana"
            )

        val apple =
            createAggregate(
                canonicalId =
                    "apple"
            )

        val candidates =
            adapter.adapt(
                aggregates =
                    listOf(
                        banana,
                        apple
                    )
            )

        assertEquals(
            listOf(
                "banana",
                "apple"
            ),
            candidates.map { candidate ->
                candidate.canonicalId
            }
        )
    }

    private fun createAggregate(
        canonicalId: String,
        aliases: Set<String> =
            sortedSetOf(
                canonicalId
            ),
        matchAliases: Set<String> =
            emptySet(),
        singleIngredientNutritionAliases: Set<String> =
            emptySet(),
        nutrition: Map<String, Double> =
            sortedMapOf(
                "energyKcalPer100g" to
                        50.0,
                "fatPer100g" to
                        0.1
            ),
        sourceIds: List<String> =
            listOf(
                "001"
            ),
        representativeSourceId: String =
            sourceIds.first(),
        source: String =
            "open_food_facts_aggregate",
        sourceVersion: String =
            "1",
        sourceConfidence: Double =
            1.0
    ): CanonicalOFFNutritionReferenceAggregate {

        val sortedNutrition =
            nutrition.toSortedMap()

        val nutrientStatistics =
            sortedNutrition
                .mapValues { (_, value) ->
                    OFFNutritionReferenceNutrientStatistics(
                        observationCount =
                            sourceIds.size,
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
                aliases.toSortedSet(),
            matchAliases =
                matchAliases.toSortedSet(),
            singleIngredientNutritionAliases =
                singleIngredientNutritionAliases
                    .toSortedSet(),
            nutrition =
                sortedNutrition,
            nutrientStatistics =
                nutrientStatistics,
            profileCount =
                sourceIds.size,
            sourceIds =
                sourceIds.sorted(),
            representativeSourceId =
                representativeSourceId,
            source =
                source,
            sourceVersion =
                sourceVersion,
            sourceConfidence =
                sourceConfidence
        )
    }
}