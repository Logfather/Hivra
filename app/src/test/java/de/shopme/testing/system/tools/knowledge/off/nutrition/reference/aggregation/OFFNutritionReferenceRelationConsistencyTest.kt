package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.aggregation

import de.shopme.tools.knowledge.off.nutrition.reference.CanonicalOFFNutritionReferenceCandidate
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.OFFNutritionReferenceAggregator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OFFNutritionReferenceRelationConsistencyTest {

    private val aggregator =
        OFFNutritionReferenceAggregator()

    @Test
    fun aggregate_repairsSugarCarbohydrateRelationshipWithRealProfile() {

        val result =
            aggregator.aggregate(
                candidates =
                    listOf(
                        candidate(
                            sourceId =
                                "a",
                            canonicalId =
                                "7 up",
                            nutrition =
                                mapOf(
                                    CARBOHYDRATES_KEY to
                                            7.8,
                                    SUGARS_KEY to
                                            7.8
                                )
                        ),
                        candidate(
                            sourceId =
                                "b",
                            canonicalId =
                                "7 up",
                            nutrition =
                                mapOf(
                                    CARBOHYDRATES_KEY to
                                            11.46
                                )
                        ),
                        candidate(
                            sourceId =
                                "c",
                            canonicalId =
                                "7 up",
                            nutrition =
                                mapOf(
                                    CARBOHYDRATES_KEY to
                                            30.95,
                                    SUGARS_KEY to
                                            30.95
                                )
                        )
                    )
            )

        val aggregate =
            result.aggregates.single()

        /*
         * Unabhängige Mediane wären:
         *
         * carbohydrates = 11.46
         * sugars = 19.375
         *
         * Dieses Paar wäre ungültig.
         *
         * Der nächstgelegene reale konsistente Kandidat ist Profil "a".
         */
        assertEquals(
            7.8,
            aggregate.nutrition[
                CARBOHYDRATES_KEY
            ]
        )

        assertEquals(
            7.8,
            aggregate.nutrition[
                SUGARS_KEY
            ]
        )

        assertTrue(
            requireNotNull(
                aggregate.nutrition[
                    SUGARS_KEY
                ]
            ) <=
                    requireNotNull(
                        aggregate.nutrition[
                            CARBOHYDRATES_KEY
                        ]
                    ) +
                    TOLERANCE
        )
    }

    @Test
    fun aggregate_repairsSaturatedFatRelationshipWithRealProfile() {

        val result =
            aggregator.aggregate(
                candidates =
                    listOf(
                        candidate(
                            sourceId =
                                "a",
                            canonicalId =
                                "5 grains",
                            nutrition =
                                mapOf(
                                    FAT_KEY to
                                            0.0,
                                    SATURATED_FAT_KEY to
                                            0.0
                                )
                        ),
                        candidate(
                            sourceId =
                                "b",
                            canonicalId =
                                "5 grains",
                            nutrition =
                                mapOf(
                                    FAT_KEY to
                                            0.847
                                )
                        ),
                        candidate(
                            sourceId =
                                "c",
                            canonicalId =
                                "5 grains",
                            nutrition =
                                mapOf(
                                    FAT_KEY to
                                            10.7,
                                    SATURATED_FAT_KEY to
                                            7.14
                                )
                        )
                    )
            )

        val aggregate =
            result.aggregates.single()

        /*
         * Unabhängige Mediane wären:
         *
         * fat = 0.847
         * saturatedFat = 3.57
         *
         * Der nächstgelegene reale konsistente Kandidat ist Profil "a".
         */
        assertEquals(
            0.0,
            aggregate.nutrition[
                FAT_KEY
            ]
        )

        assertEquals(
            0.0,
            aggregate.nutrition[
                SATURATED_FAT_KEY
            ]
        )

        assertTrue(
            requireNotNull(
                aggregate.nutrition[
                    SATURATED_FAT_KEY
                ]
            ) <=
                    requireNotNull(
                        aggregate.nutrition[
                            FAT_KEY
                        ]
                    ) +
                    TOLERANCE
        )
    }

    @Test
    fun aggregate_removesChildWhenNoCompleteRelationProfileExists() {

        val result =
            aggregator.aggregate(
                candidates =
                    listOf(
                        candidate(
                            sourceId =
                                "a",
                            canonicalId =
                                "incomplete",
                            nutrition =
                                mapOf(
                                    CARBOHYDRATES_KEY to
                                            2.0
                                )
                        ),
                        candidate(
                            sourceId =
                                "b",
                            canonicalId =
                                "incomplete",
                            nutrition =
                                mapOf(
                                    SUGARS_KEY to
                                            5.0
                                )
                        )
                    )
            )

        val aggregate =
            result.aggregates.single()

        assertEquals(
            2.0,
            aggregate.nutrition[
                CARBOHYDRATES_KEY
            ]
        )

        assertFalse(
            aggregate.nutrition.containsKey(
                SUGARS_KEY
            )
        )

        assertTrue(
            aggregate.nutrientStatistics.containsKey(
                CARBOHYDRATES_KEY
            )
        )

        assertFalse(
            aggregate.nutrientStatistics.containsKey(
                SUGARS_KEY
            )
        )

        assertEquals(
            aggregate.nutrition.keys,
            aggregate.nutrientStatistics.keys
        )
    }

    @Test
    fun aggregate_isDeterministicAcrossInputOrder() {

        val first =
            candidate(
                sourceId =
                    "a",
                canonicalId =
                    "deterministic",
                nutrition =
                    mapOf(
                        CARBOHYDRATES_KEY to
                                7.8,
                        SUGARS_KEY to
                                7.8
                    )
            )

        val second =
            candidate(
                sourceId =
                    "b",
                canonicalId =
                    "deterministic",
                nutrition =
                    mapOf(
                        CARBOHYDRATES_KEY to
                                11.46
                    )
            )

        val third =
            candidate(
                sourceId =
                    "c",
                canonicalId =
                    "deterministic",
                nutrition =
                    mapOf(
                        CARBOHYDRATES_KEY to
                                30.95,
                        SUGARS_KEY to
                                30.95
                    )
            )

        val forward =
            aggregator.aggregate(
                candidates =
                    listOf(
                        first,
                        second,
                        third
                    )
            )
                .aggregates
                .single()

        val reverse =
            aggregator.aggregate(
                candidates =
                    listOf(
                        third,
                        second,
                        first
                    )
            )
                .aggregates
                .single()

        assertEquals(
            forward,
            reverse
        )
    }

    private fun candidate(
        sourceId: String,
        canonicalId: String,
        nutrition: Map<String, Double>
    ): CanonicalOFFNutritionReferenceCandidate =
        CanonicalOFFNutritionReferenceCandidate(
            sourceId =
                sourceId,
            canonicalId =
                canonicalId,
            aliases =
                sortedSetOf(
                    canonicalId
                ),
            matchAliases =
                emptySet(),
            nutrition =
                nutrition.toSortedMap(),
            productName =
                canonicalId,
            brand =
                "test",
            categories =
                "test",
            singleIngredientNutritionAliases =
                emptySet(),
            source =
                "open_food_facts",
            sourceVersion =
                "1",
            sourceConfidence =
                1.0
        )

    private companion object {

        const val FAT_KEY =
            "fatPer100g"

        const val SATURATED_FAT_KEY =
            "saturatedFatPer100g"

        const val CARBOHYDRATES_KEY =
            "carbohydratesPer100g"

        const val SUGARS_KEY =
            "sugarsPer100g"

        const val TOLERANCE =
            0.5
    }
}