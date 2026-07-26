package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.validation

import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.CanonicalOFFNutritionReferenceAggregate
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.OFFNutritionReferenceNutrientStatistics
import de.shopme.tools.knowledge.off.nutrition.reference.validation.OFFNutritionReferenceAggregateIssueType
import de.shopme.tools.knowledge.off.nutrition.reference.validation.OFFNutritionReferenceAggregateValidationStatus
import de.shopme.tools.knowledge.off.nutrition.reference.validation.OFFNutritionReferenceAggregateValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OFFNutritionReferenceAggregateValidatorTest {

    @Test
    fun validate_acceptsConsistentAggregate() {

        val aggregate =
            createAggregate(
                canonicalId =
                    "plain yogurt",
                profileCount =
                    3,
                nutrition =
                    sortedMapOf(
                        "energyKcalPer100g" to 61.0,
                        "fatPer100g" to 3.3,
                        "carbohydratesPer100g" to 4.7,
                        "proteinsPer100g" to 3.5,
                        "saltPer100g" to 0.1
                    )
            )

        val result =
            OFFNutritionReferenceAggregateValidator()
                .validate(
                    aggregates =
                        listOf(aggregate)
                )

        assertEquals(
            1,
            result.acceptedAggregateCount
        )

        assertEquals(
            0,
            result.warningAggregateCount
        )

        assertEquals(
            0,
            result.rejectedAggregateCount
        )

        assertEquals(
            aggregate,
            result.validatedAggregates.single()
        )
    }

    @Test
    fun validate_warnsForHighVariation() {

        val aggregate =
            createAggregate(
                canonicalId =
                    "mixed yogurt",
                profileCount =
                    4,
                nutrition =
                    sortedMapOf(
                        "energyKcalPer100g" to 100.0,
                        "fatPer100g" to 2.0,
                        "carbohydratesPer100g" to 10.0,
                        "proteinsPer100g" to 4.0,
                        "saltPer100g" to 0.1
                    ),
                statisticsOverrides =
                    mapOf(
                        "fatPer100g" to
                                OFFNutritionReferenceNutrientStatistics(
                                    observationCount =
                                        4,
                                    minimum =
                                        0.0,
                                    median =
                                        2.0,
                                    maximum =
                                        20.0
                                )
                    )
            )

        val entry =
            OFFNutritionReferenceAggregateValidator()
                .validate(
                    listOf(aggregate)
                )
                .entries
                .single()

        assertEquals(
            OFFNutritionReferenceAggregateValidationStatus.WARNING,
            entry.status
        )

        assertTrue(
            entry.issues.any { issue ->
                issue.type ==
                        OFFNutritionReferenceAggregateIssueType
                            .HIGH_NUTRIENT_VARIATION &&
                        issue.nutrientKey ==
                        "fatPer100g"
            }
        )
    }

    @Test
    fun validate_warnsForLowObservationCoverage() {

        val aggregate =
            createAggregate(
                canonicalId =
                    "incomplete product",
                profileCount =
                    10,
                nutrition =
                    sortedMapOf(
                        "energyKcalPer100g" to 100.0,
                        "fatPer100g" to 2.0,
                        "carbohydratesPer100g" to 10.0,
                        "proteinsPer100g" to 4.0,
                        "saltPer100g" to 0.1
                    ),
                statisticsOverrides =
                    mapOf(
                        "proteinsPer100g" to
                                OFFNutritionReferenceNutrientStatistics(
                                    observationCount =
                                        2,
                                    minimum =
                                        3.0,
                                    median =
                                        4.0,
                                    maximum =
                                        5.0
                                )
                    )
            )

        val entry =
            OFFNutritionReferenceAggregateValidator()
                .validate(
                    listOf(aggregate)
                )
                .entries
                .single()

        assertEquals(
            OFFNutritionReferenceAggregateValidationStatus.WARNING,
            entry.status
        )

        assertTrue(
            entry.issues.any { issue ->
                issue.type ==
                        OFFNutritionReferenceAggregateIssueType
                            .LOW_NUTRIENT_OBSERVATION_COVERAGE &&
                        issue.nutrientKey ==
                        "proteinsPer100g"
            }
        )
    }

    @Test
    fun validate_rejectsExtremeNutritionValue() {

        val aggregate =
            createAggregate(
                canonicalId =
                    "invalid product",
                profileCount =
                    1,
                nutrition =
                    sortedMapOf(
                        "energyKcalPer100g" to 100.0,
                        "fatPer100g" to 120.0
                    )
            )

        val result =
            OFFNutritionReferenceAggregateValidator()
                .validate(
                    listOf(aggregate)
                )

        assertEquals(
            0,
            result.validatedAggregates.size
        )

        assertEquals(
            1,
            result.rejectedAggregateCount
        )

        assertEquals(
            OFFNutritionReferenceAggregateValidationStatus.REJECTED,
            result.entries.single().status
        )

        assertTrue(
            result.entries.single()
                .issues
                .any { issue ->
                    issue.type ==
                            OFFNutritionReferenceAggregateIssueType
                                .EXTREME_NUTRIENT_VALUE
                }
        )
    }

    @Test
    fun validate_warnsForEnergyMacronutrientMismatch() {

        val aggregate =
            createAggregate(
                canonicalId =
                    "energy mismatch",
                profileCount =
                    3,
                nutrition =
                    sortedMapOf(
                        "energyKcalPer100g" to 500.0,
                        "fatPer100g" to 1.0,
                        "carbohydratesPer100g" to 5.0,
                        "proteinsPer100g" to 2.0,
                        "saltPer100g" to 0.1
                    )
            )

        val entry =
            OFFNutritionReferenceAggregateValidator()
                .validate(
                    listOf(aggregate)
                )
                .entries
                .single()

        assertEquals(
            OFFNutritionReferenceAggregateValidationStatus.WARNING,
            entry.status
        )

        assertTrue(
            entry.issues.any { issue ->
                issue.type ==
                        OFFNutritionReferenceAggregateIssueType
                            .ENERGY_MACRONUTRIENT_MISMATCH
            }
        )
    }

    @Test
    fun validate_isIndependentOfInputOrder() {

        val apple =
            createAggregate(
                canonicalId =
                    "apple",
                profileCount =
                    1,
                nutrition =
                    sortedMapOf(
                        "energyKcalPer100g" to 52.0,
                        "fatPer100g" to 0.2
                    )
            )

        val pear =
            createAggregate(
                canonicalId =
                    "pear",
                profileCount =
                    1,
                nutrition =
                    sortedMapOf(
                        "energyKcalPer100g" to 57.0,
                        "fatPer100g" to 0.1
                    )
            )

        val validator =
            OFFNutritionReferenceAggregateValidator()

        val forward =
            validator.validate(
                listOf(
                    apple,
                    pear
                )
            )

        val reverse =
            validator.validate(
                listOf(
                    pear,
                    apple
                )
            )

        assertEquals(
            forward,
            reverse
        )
    }

    private fun createAggregate(
        canonicalId: String,
        profileCount: Int,
        nutrition: Map<String, Double>,
        statisticsOverrides:
        Map<String, OFFNutritionReferenceNutrientStatistics> =
            emptyMap()
    ): CanonicalOFFNutritionReferenceAggregate {

        val statistics =
            nutrition
                .mapValues { (key, value) ->
                    statisticsOverrides[key]
                        ?: OFFNutritionReferenceNutrientStatistics(
                            observationCount =
                                profileCount,
                            minimum =
                                value,
                            median =
                                value,
                            maximum =
                                value
                        )
                }
                .toSortedMap()

        val sourceIds =
            (1..profileCount)
                .map { index ->
                    index
                        .toString()
                        .padStart(
                            length =
                                3,
                            padChar =
                                '0'
                        )
                }

        return CanonicalOFFNutritionReferenceAggregate(
            canonicalId =
                canonicalId,
            aliases =
                sortedSetOf(canonicalId),
            matchAliases =
                emptySet(),
            singleIngredientNutritionAliases =
                emptySet(),
            nutrition =
                nutrition.toSortedMap(),
            nutrientStatistics =
                statistics,
            profileCount =
                profileCount,
            sourceIds =
                sourceIds,
            representativeSourceId =
                sourceIds.first(),
            source =
                "open_food_facts_aggregate",
            sourceVersion =
                "1",
            sourceConfidence =
                1.0
        )
    }
}