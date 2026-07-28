package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.quality

import de.shopme.tools.knowledge.off.nutrition.reference.CanonicalOFFNutritionReferenceCandidate
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityFilter
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityRejectionReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OFFNutritionReferenceQualityFilterTest {

    @Test
    fun filter_acceptsPlausibleNutritionCandidate() {

        val candidate =
            createCandidate(
                sourceId =
                    "2222222222222",
                canonicalId =
                    "plain yogurt",
                nutrition =
                    mapOf(
                        "energyKcalPer100g" to 61.0,
                        "fatPer100g" to 3.3,
                        "saturatedFatPer100g" to 2.1,
                        "carbohydratesPer100g" to 4.7,
                        "sugarsPer100g" to 4.7,
                        "proteinsPer100g" to 3.5,
                        "saltPer100g" to 0.1
                    )
            )

        val result =
            OFFNutritionReferenceQualityFilter()
                .filter(
                    candidates =
                        listOf(candidate)
                )

        assertEquals(
            1,
            result.inputCandidateCount
        )

        assertEquals(
            1,
            result.acceptedCandidateCount
        )

        assertEquals(
            0,
            result.rejectedCandidateCount
        )

        assertEquals(
            listOf(candidate),
            result.acceptedCandidates
        )

        assertTrue(
            result.rejections.isEmpty()
        )

        assertTrue(
            result.countsByReason.isEmpty()
        )
    }

    @Test
    fun filter_rejectsEmptyNutritionPayload() {

        val candidate =
            createCandidate(
                sourceId =
                    "empty-payload",
                canonicalId =
                    "empty product",
                nutrition =
                    emptyMap()
            )

        val result =
            OFFNutritionReferenceQualityFilter()
                .filter(
                    candidates =
                        listOf(candidate)
                )

        assertEquals(
            1,
            result.inputCandidateCount
        )

        assertEquals(
            0,
            result.acceptedCandidateCount
        )

        assertEquals(
            1,
            result.rejectedCandidateCount
        )

        assertEquals(
            listOf(
                OFFNutritionReferenceQualityRejectionReason
                    .EMPTY_NUTRITION_PAYLOAD
            ),
            result.rejections.single().reasons
        )

        assertEquals(
            mapOf(
                OFFNutritionReferenceQualityRejectionReason
                    .EMPTY_NUTRITION_PAYLOAD to 1
            ),
            result.countsByReason
        )
    }

    @Test
    fun filter_rejectsNonFiniteNutritionValue() {

        val candidate =
            createCandidate(
                sourceId =
                    "non-finite-value",
                canonicalId =
                    "invalid product",
                nutrition =
                    mapOf(
                        "energyKcalPer100g" to
                                Double.POSITIVE_INFINITY,
                        "fatPer100g" to 10.0,
                        "carbohydratesPer100g" to 20.0,
                        "proteinsPer100g" to 5.0
                    )
            )

        val result =
            OFFNutritionReferenceQualityFilter()
                .filter(
                    candidates =
                        listOf(candidate)
                )

        assertEquals(
            listOf(
                OFFNutritionReferenceQualityRejectionReason
                    .NON_FINITE_NUTRITION_VALUE,
                OFFNutritionReferenceQualityRejectionReason
                    .NUTRITION_VALUE_ABOVE_MAXIMUM
            )
                .sortedBy { reason ->
                    reason.name
                },
            result.rejections.single().reasons
        )

        assertEquals(
            mapOf(
                OFFNutritionReferenceQualityRejectionReason
                    .NON_FINITE_NUTRITION_VALUE to 1,
                OFFNutritionReferenceQualityRejectionReason
                    .NUTRITION_VALUE_ABOVE_MAXIMUM to 1
            )
                .toSortedMap(
                    compareBy { reason ->
                        reason.name
                    }
                ),
            result.countsByReason
        )
    }

    @Test
    fun filter_rejectsUnsupportedNutritionKey() {

        val candidate =
            createCandidate(
                sourceId =
                    "unsupported-key",
                canonicalId =
                    "invalid product",
                nutrition =
                    mapOf(
                        "energyKcalPer100g" to 100.0,
                        "unknownNutrientPer100g" to 5.0
                    )
            )

        val result =
            OFFNutritionReferenceQualityFilter()
                .filter(
                    candidates =
                        listOf(candidate)
                )

        assertEquals(
            listOf(
                OFFNutritionReferenceQualityRejectionReason
                    .UNSUPPORTED_NUTRITION_KEY
            ),
            result.rejections.single().reasons
        )

        assertEquals(
            mapOf(
                OFFNutritionReferenceQualityRejectionReason
                    .UNSUPPORTED_NUTRITION_KEY to 1
            ),
            result.countsByReason
        )
    }

    @Test
    fun filter_rejectsNegativeNutritionValue() {

        val candidate =
            createCandidate(
                sourceId =
                    "negative-fat",
                canonicalId =
                    "invalid product",
                nutrition =
                    mapOf(
                        "energyKcalPer100g" to 100.0,
                        "fatPer100g" to -1.0,
                        "carbohydratesPer100g" to 10.0,
                        "proteinsPer100g" to 5.0
                    )
            )

        val result =
            OFFNutritionReferenceQualityFilter()
                .filter(
                    candidates =
                        listOf(candidate)
                )

        assertEquals(
            0,
            result.acceptedCandidateCount
        )

        assertEquals(
            1,
            result.rejectedCandidateCount
        )

        assertEquals(
            listOf(
                OFFNutritionReferenceQualityRejectionReason
                    .NEGATIVE_NUTRITION_VALUE
            ),
            result.rejections.single().reasons
        )

        assertEquals(
            mapOf(
                OFFNutritionReferenceQualityRejectionReason
                    .NEGATIVE_NUTRITION_VALUE to 1
            ),
            result.countsByReason
        )
    }

    @Test
    fun filter_rejectsValuesAboveMaximum() {

        val candidate =
            createCandidate(
                sourceId =
                    "invalid-energy",
                canonicalId =
                    "invalid product",
                nutrition =
                    mapOf(
                        "energyKcalPer100g" to 1_200.0,
                        "fatPer100g" to 10.0,
                        "carbohydratesPer100g" to 20.0,
                        "proteinsPer100g" to 5.0
                    )
            )

        val result =
            OFFNutritionReferenceQualityFilter()
                .filter(
                    candidates =
                        listOf(candidate)
                )

        assertEquals(
            listOf(
                OFFNutritionReferenceQualityRejectionReason
                    .NUTRITION_VALUE_ABOVE_MAXIMUM
            ),
            result.rejections.single().reasons
        )

        assertEquals(
            mapOf(
                OFFNutritionReferenceQualityRejectionReason
                    .NUTRITION_VALUE_ABOVE_MAXIMUM to 1
            ),
            result.countsByReason
        )
    }

    @Test
    fun filter_rejectsZeroOnlyPayload() {

        val candidate =
            createCandidate(
                sourceId =
                    "zero-only",
                canonicalId =
                    "zero product",
                nutrition =
                    mapOf(
                        "energyKcalPer100g" to 0.0,
                        "fatPer100g" to 0.0,
                        "carbohydratesPer100g" to 0.0,
                        "proteinsPer100g" to 0.0,
                        "saltPer100g" to 0.0
                    )
            )

        val result =
            OFFNutritionReferenceQualityFilter()
                .filter(
                    candidates =
                        listOf(candidate)
                )

        assertEquals(
            listOf(
                OFFNutritionReferenceQualityRejectionReason
                    .ZERO_ONLY_NUTRITION_PAYLOAD
            ),
            result.rejections.single().reasons
        )

        assertEquals(
            mapOf(
                OFFNutritionReferenceQualityRejectionReason
                    .ZERO_ONLY_NUTRITION_PAYLOAD to 1
            ),
            result.countsByReason
        )
    }

    @Test
    fun filter_rejectsInvalidNutrientRelationships() {

        val candidate =
            createCandidate(
                sourceId =
                    "invalid-relationships",
                canonicalId =
                    "invalid product",
                nutrition =
                    mapOf(
                        "energyKcalPer100g" to 100.0,
                        "fatPer100g" to 2.0,
                        "saturatedFatPer100g" to 4.0,
                        "carbohydratesPer100g" to 5.0,
                        "sugarsPer100g" to 8.0,
                        "proteinsPer100g" to 4.0
                    )
            )

        val result =
            OFFNutritionReferenceQualityFilter()
                .filter(
                    candidates =
                        listOf(candidate)
                )

        val expectedReasons =
            listOf(
                OFFNutritionReferenceQualityRejectionReason
                    .SATURATED_FAT_EXCEEDS_TOTAL_FAT,
                OFFNutritionReferenceQualityRejectionReason
                    .SUGARS_EXCEED_CARBOHYDRATES
            )
                .sortedBy { reason ->
                    reason.name
                }

        assertEquals(
            expectedReasons,
            result.rejections.single().reasons
        )

        assertEquals(
            expectedReasons
                .associateWith {
                    1
                }
                .toSortedMap(
                    compareBy { reason ->
                        reason.name
                    }
                ),
            result.countsByReason
        )
    }

    @Test
    fun filter_rejectsImplausibleMacronutrientSum() {

        val candidate =
            createCandidate(
                sourceId =
                    "invalid-sum",
                canonicalId =
                    "invalid product",
                nutrition =
                    mapOf(
                        "energyKcalPer100g" to 500.0,
                        "fatPer100g" to 40.0,
                        "carbohydratesPer100g" to 50.0,
                        "proteinsPer100g" to 20.0
                    )
            )

        val result =
            OFFNutritionReferenceQualityFilter()
                .filter(
                    candidates =
                        listOf(candidate)
                )

        assertEquals(
            listOf(
                OFFNutritionReferenceQualityRejectionReason
                    .MACRONUTRIENT_SUM_EXCEEDS_100_GRAMS
            ),
            result.rejections.single().reasons
        )

        assertEquals(
            mapOf(
                OFFNutritionReferenceQualityRejectionReason
                    .MACRONUTRIENT_SUM_EXCEEDS_100_GRAMS to 1
            ),
            result.countsByReason
        )
    }

    @Test
    fun filter_collectsMultipleRejectionReasonsDeterministically() {

        val candidate =
            createCandidate(
                sourceId =
                    "multiple-reasons",
                canonicalId =
                    "invalid product",
                nutrition =
                    mapOf(
                        "energyKcalPer100g" to 1_200.0,
                        "fatPer100g" to -1.0,
                        "saturatedFatPer100g" to 2.0,
                        "carbohydratesPer100g" to 80.0,
                        "sugarsPer100g" to 90.0,
                        "proteinsPer100g" to 40.0
                    )
            )

        val result =
            OFFNutritionReferenceQualityFilter()
                .filter(
                    candidates =
                        listOf(candidate)
                )

        val expectedReasons =
            listOf(
                OFFNutritionReferenceQualityRejectionReason
                    .MACRONUTRIENT_SUM_EXCEEDS_100_GRAMS,
                OFFNutritionReferenceQualityRejectionReason
                    .NEGATIVE_NUTRITION_VALUE,
                OFFNutritionReferenceQualityRejectionReason
                    .NUTRITION_VALUE_ABOVE_MAXIMUM,
                OFFNutritionReferenceQualityRejectionReason
                    .SATURATED_FAT_EXCEEDS_TOTAL_FAT,
                OFFNutritionReferenceQualityRejectionReason
                    .SUGARS_EXCEED_CARBOHYDRATES
            )
                .sortedBy { reason ->
                    reason.name
                }

        assertEquals(
            expectedReasons,
            result.rejections.single().reasons
        )

        assertEquals(
            expectedReasons.size,
            result.rejections
                .single()
                .reasons
                .distinct()
                .size
        )

        assertEquals(
            expectedReasons
                .associateWith {
                    1
                }
                .toSortedMap(
                    compareBy { reason ->
                        reason.name
                    }
                ),
            result.countsByReason
        )
    }

    @Test
    fun filter_sortsAcceptedCandidatesAndRejectionsDeterministically() {

        val acceptedLater =
            createCandidate(
                sourceId =
                    "2222222222222",
                canonicalId =
                    "yogurt"
            )

        val rejectedLater =
            createCandidate(
                sourceId =
                    "4444444444444",
                canonicalId =
                    "invalid yogurt",
                nutrition =
                    mapOf(
                        "energyKcalPer100g" to 0.0,
                        "fatPer100g" to 0.0
                    )
            )

        val acceptedEarlier =
            createCandidate(
                sourceId =
                    "1111111111111",
                canonicalId =
                    "apple"
            )

        val rejectedEarlier =
            createCandidate(
                sourceId =
                    "3333333333333",
                canonicalId =
                    "invalid apple",
                nutrition =
                    mapOf(
                        "energyKcalPer100g" to 0.0,
                        "fatPer100g" to 0.0
                    )
            )

        val result =
            OFFNutritionReferenceQualityFilter()
                .filter(
                    candidates =
                        listOf(
                            rejectedLater,
                            acceptedLater,
                            rejectedEarlier,
                            acceptedEarlier
                        )
                )

        assertEquals(
            listOf(
                "1111111111111",
                "2222222222222"
            ),
            result.acceptedCandidates
                .map { candidate ->
                    candidate.sourceId
                }
        )

        assertEquals(
            listOf(
                "3333333333333",
                "4444444444444"
            ),
            result.rejections
                .map { rejection ->
                    rejection.sourceId
                }
        )

        assertEquals(
            mapOf(
                OFFNutritionReferenceQualityRejectionReason
                    .ZERO_ONLY_NUTRITION_PAYLOAD to 2
            ),
            result.countsByReason
        )
    }

    private fun createCandidate(
        sourceId: String,
        canonicalId: String,
        nutrition: Map<String, Double> =
            mapOf(
                "energyKcalPer100g" to 61.0,
                "fatPer100g" to 3.3,
                "saturatedFatPer100g" to 2.1,
                "carbohydratesPer100g" to 4.7,
                "sugarsPer100g" to 4.7,
                "proteinsPer100g" to 3.5,
                "saltPer100g" to 0.1
            )
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
                sortedSetOf(
                    canonicalId
                ),
            nutrition =
                nutrition.toSortedMap(),
            productName =
                canonicalId,
            brand =
                "example brand",
            categories =
                "example category",
            singleIngredientNutritionAliases =
                sortedSetOf(
                    canonicalId
                ),
            source =
                "open_food_facts",
            sourceVersion =
                "1",
            sourceConfidence =
                1.0
        )
}