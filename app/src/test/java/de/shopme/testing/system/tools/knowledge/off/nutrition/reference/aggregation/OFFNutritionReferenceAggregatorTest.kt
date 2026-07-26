package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.aggregation

import de.shopme.tools.knowledge.off.nutrition.reference.CanonicalOFFNutritionReferenceCandidate
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.OFFNutritionReferenceAggregator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OFFNutritionReferenceAggregatorTest {

    @Test
    fun aggregate_buildsOneAggregatePerCanonicalId() {

        val firstApple =
            createCandidate(
                sourceId =
                    "111",
                canonicalId =
                    "apple",
                fat =
                    0.2,
                carbohydrates =
                    10.0,
                proteins =
                    0.3
            )

        val secondApple =
            createCandidate(
                sourceId =
                    "222",
                canonicalId =
                    "apple",
                fat =
                    0.4,
                carbohydrates =
                    12.0,
                proteins =
                    0.5
            )

        val pear =
            createCandidate(
                sourceId =
                    "333",
                canonicalId =
                    "pear",
                fat =
                    0.1,
                carbohydrates =
                    11.0,
                proteins =
                    0.4
            )

        val result =
            OFFNutritionReferenceAggregator()
                .aggregate(
                    candidates =
                        listOf(
                            pear,
                            secondApple,
                            firstApple
                        )
                )

        assertEquals(
            expected =
                3,
            actual =
                result.inputCandidateCount
        )

        assertEquals(
            expected =
                2,
            actual =
                result.aggregateCount
        )

        assertEquals(
            expected =
                listOf(
                    "apple",
                    "pear"
                ),
            actual =
                result.aggregates
                    .map { aggregate ->
                        aggregate.canonicalId
                    }
        )

        val apple =
            result.aggregates.first()

        assertEquals(
            expected =
                2,
            actual =
                apple.profileCount
        )

        assertEquals(
            expected =
                0.3,
            actual =
                apple.nutrition.getValue(
                    "fatPer100g"
                ),
            absoluteTolerance =
                DOUBLE_TOLERANCE
        )

        assertEquals(
            expected =
                11.0,
            actual =
                apple.nutrition.getValue(
                    "carbohydratesPer100g"
                ),
            absoluteTolerance =
                DOUBLE_TOLERANCE
        )

        assertEquals(
            expected =
                0.4,
            actual =
                apple.nutrition.getValue(
                    "proteinsPer100g"
                ),
            absoluteTolerance =
                DOUBLE_TOLERANCE
        )
    }

    @Test
    fun aggregate_usesMedianInsteadOfMean() {

        val candidates =
            listOf(
                createCandidate(
                    sourceId =
                        "111",
                    canonicalId =
                        "yogurt",
                    fat =
                        1.0
                ),
                createCandidate(
                    sourceId =
                        "222",
                    canonicalId =
                        "yogurt",
                    fat =
                        2.0
                ),
                createCandidate(
                    sourceId =
                        "333",
                    canonicalId =
                        "yogurt",
                    fat =
                        30.0
                )
            )

        val aggregate =
            OFFNutritionReferenceAggregator()
                .aggregate(
                    candidates =
                        candidates
                )
                .aggregates
                .single()

        val statistics =
            aggregate.nutrientStatistics
                .getValue(
                    "fatPer100g"
                )

        assertEquals(
            expected =
                2.0,
            actual =
                aggregate.nutrition.getValue(
                    "fatPer100g"
                ),
            absoluteTolerance =
                DOUBLE_TOLERANCE
        )

        assertEquals(
            expected =
                3,
            actual =
                statistics.observationCount
        )

        assertEquals(
            expected =
                1.0,
            actual =
                statistics.minimum,
            absoluteTolerance =
                DOUBLE_TOLERANCE
        )

        assertEquals(
            expected =
                2.0,
            actual =
                statistics.median,
            absoluteTolerance =
                DOUBLE_TOLERANCE
        )

        assertEquals(
            expected =
                30.0,
            actual =
                statistics.maximum,
            absoluteTolerance =
                DOUBLE_TOLERANCE
        )
    }

    @Test
    fun aggregate_doesNotTreatMissingNutrientsAsZero() {

        val withProtein =
            createCandidate(
                sourceId =
                    "111",
                canonicalId =
                    "product",
                proteins =
                    8.0
            )

        val withoutProtein =
            createCandidate(
                sourceId =
                    "222",
                canonicalId =
                    "product",
                proteins =
                    null
            )

        val aggregate =
            OFFNutritionReferenceAggregator()
                .aggregate(
                    candidates =
                        listOf(
                            withoutProtein,
                            withProtein
                        )
                )
                .aggregates
                .single()

        assertEquals(
            expected =
                8.0,
            actual =
                aggregate.nutrition.getValue(
                    "proteinsPer100g"
                ),
            absoluteTolerance =
                DOUBLE_TOLERANCE
        )

        assertEquals(
            expected =
                1,
            actual =
                aggregate.nutrientStatistics
                    .getValue(
                        "proteinsPer100g"
                    )
                    .observationCount
        )
    }

    @Test
    fun aggregate_unionsAliasesDeterministically() {

        val first =
            createCandidate(
                sourceId =
                    "111",
                canonicalId =
                    "apple",
                aliases =
                    setOf(
                        "apple",
                        "fresh apple"
                    ),
                matchAliases =
                    setOf(
                        "fruit"
                    )
            )

        val second =
            createCandidate(
                sourceId =
                    "222",
                canonicalId =
                    "apple",
                aliases =
                    setOf(
                        "dessert apple"
                    ),
                matchAliases =
                    setOf(
                        "fresh fruit"
                    )
            )

        val aggregate =
            OFFNutritionReferenceAggregator()
                .aggregate(
                    candidates =
                        listOf(
                            second,
                            first
                        )
                )
                .aggregates
                .single()

        assertEquals(
            expected =
                sortedSetOf(
                    "apple",
                    "dessert apple",
                    "fresh apple"
                ),
            actual =
                aggregate.aliases
        )

        assertEquals(
            expected =
                sortedSetOf(
                    "fresh fruit",
                    "fruit"
                ),
            actual =
                aggregate.matchAliases
        )
    }

    @Test
    fun aggregate_isIndependentOfInputOrder() {

        val first =
            createCandidate(
                sourceId =
                    "111",
                canonicalId =
                    "apple",
                fat =
                    0.2
            )

        val second =
            createCandidate(
                sourceId =
                    "222",
                canonicalId =
                    "apple",
                fat =
                    0.4
            )

        val aggregator =
            OFFNutritionReferenceAggregator()

        val forward =
            aggregator.aggregate(
                candidates =
                    listOf(
                        first,
                        second
                    )
            )

        val reverse =
            aggregator.aggregate(
                candidates =
                    listOf(
                        second,
                        first
                    )
            )

        assertEquals(
            expected =
                forward,
            actual =
                reverse
        )
    }

    @Test
    fun median_handlesOddAndEvenObservationCounts() {

        val aggregator =
            OFFNutritionReferenceAggregator()

        assertEquals(
            expected =
                2.0,
            actual =
                aggregator.median(
                    sortedValues =
                        listOf(
                            1.0,
                            2.0,
                            100.0
                        )
                ),
            absoluteTolerance =
                DOUBLE_TOLERANCE
        )

        assertEquals(
            expected =
                2.5,
            actual =
                aggregator.median(
                    sortedValues =
                        listOf(
                            1.0,
                            2.0,
                            3.0,
                            4.0
                        )
                ),
            absoluteTolerance =
                DOUBLE_TOLERANCE
        )
    }

    @Test
    fun aggregate_selectsRepresentativeClosestToMedian() {

        val low =
            createCandidate(
                sourceId =
                    "111",
                canonicalId =
                    "milk",
                fat =
                    1.0
            )

        val middle =
            createCandidate(
                sourceId =
                    "222",
                canonicalId =
                    "milk",
                fat =
                    2.0
            )

        val high =
            createCandidate(
                sourceId =
                    "333",
                canonicalId =
                    "milk",
                fat =
                    10.0
            )

        val aggregate =
            OFFNutritionReferenceAggregator()
                .aggregate(
                    candidates =
                        listOf(
                            high,
                            low,
                            middle
                        )
                )
                .aggregates
                .single()

        assertEquals(
            expected =
                "222",
            actual =
                aggregate.representativeSourceId
        )

        assertTrue(
            aggregate.representativeSourceId in
                    aggregate.sourceIds
        )
    }

    private fun createCandidate(
        sourceId: String,
        canonicalId: String,
        fat: Double =
            1.0,
        carbohydrates: Double =
            10.0,
        proteins: Double? =
            2.0,
        aliases: Set<String> =
            setOf(canonicalId),
        matchAliases: Set<String> =
            emptySet()
    ): CanonicalOFFNutritionReferenceCandidate {

        val nutrition =
            sortedMapOf(
                "energyKcalPer100g" to
                        100.0,
                "fatPer100g" to
                        fat,
                "carbohydratesPer100g" to
                        carbohydrates,
                "saltPer100g" to
                        0.1
            )

        proteins?.let { proteinValue ->
            nutrition["proteinsPer100g"] =
                proteinValue
        }

        return CanonicalOFFNutritionReferenceCandidate(
            sourceId =
                sourceId,
            canonicalId =
                canonicalId,
            aliases =
                aliases.toSortedSet(),
            matchAliases =
                matchAliases.toSortedSet(),
            nutrition =
                nutrition,
            productName =
                canonicalId,
            brand =
                "example",
            categories =
                "example category",
            singleIngredientNutritionAliases =
                emptySet(),
            source =
                "open_food_facts",
            sourceVersion =
                "1",
            sourceConfidence =
                1.0
        )
    }

    private companion object {

        const val DOUBLE_TOLERANCE =
            1e-9
    }
}