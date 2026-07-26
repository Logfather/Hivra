package de.shopme.tools.knowledge.off.nutrition.reference.validation

import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.CanonicalOFFNutritionReferenceAggregate
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.OFFNutritionReferenceNutrientStatistics
import kotlin.math.abs
import kotlin.math.max

class OFFNutritionReferenceAggregateValidator {

    fun validate(
        aggregates: List<CanonicalOFFNutritionReferenceAggregate>
    ): OFFNutritionReferenceAggregateValidationResult {

        val sortedAggregates =
            aggregates.sortedBy { aggregate ->
                aggregate.canonicalId
            }

        require(
            sortedAggregates
                .map { aggregate ->
                    aggregate.canonicalId
                }
                .distinct()
                .size ==
                    sortedAggregates.size
        ) {
            "Aggregate validation input contains duplicate canonical IDs."
        }

        val entries =
            sortedAggregates.map { aggregate ->
                validateAggregate(
                    aggregate =
                        aggregate
                )
            }

        val entryByCanonicalId =
            entries.associateBy { entry ->
                entry.canonicalId
            }

        val validatedAggregates =
            sortedAggregates.filter { aggregate ->
                entryByCanonicalId
                    .getValue(aggregate.canonicalId)
                    .status !=
                        OFFNutritionReferenceAggregateValidationStatus.REJECTED
            }

        val rejectedAggregates =
            sortedAggregates.filter { aggregate ->
                entryByCanonicalId
                    .getValue(aggregate.canonicalId)
                    .status ==
                        OFFNutritionReferenceAggregateValidationStatus.REJECTED
            }

        return OFFNutritionReferenceAggregateValidationResult(
            inputAggregateCount =
                sortedAggregates.size,
            acceptedAggregateCount =
                entries.count { entry ->
                    entry.status ==
                            OFFNutritionReferenceAggregateValidationStatus.ACCEPTED
                },
            warningAggregateCount =
                entries.count { entry ->
                    entry.status ==
                            OFFNutritionReferenceAggregateValidationStatus.WARNING
                },
            rejectedAggregateCount =
                entries.count { entry ->
                    entry.status ==
                            OFFNutritionReferenceAggregateValidationStatus.REJECTED
                },
            totalWarningCount =
                entries.sumOf { entry ->
                    entry.warningCount
                },
            totalErrorCount =
                entries.sumOf { entry ->
                    entry.errorCount
                },
            validatedAggregates =
                validatedAggregates,
            rejectedAggregates =
                rejectedAggregates,
            entries =
                entries
        )
    }

    fun validateAggregate(
        aggregate: CanonicalOFFNutritionReferenceAggregate
    ): OFFNutritionReferenceAggregateValidationEntry {

        val issues =
            mutableListOf<OFFNutritionReferenceAggregateValidationIssue>()

        validateStructuralIntegrity(
            aggregate =
                aggregate,
            issues =
                issues
        )

        validateNutritionValues(
            aggregate =
                aggregate,
            issues =
                issues
        )

        validateNutrientStatistics(
            aggregate =
                aggregate,
            issues =
                issues
        )

        validateObservationCoverage(
            aggregate =
                aggregate,
            issues =
                issues
        )

        validateVariation(
            aggregate =
                aggregate,
            issues =
                issues
        )

        validateEnergyConsistency(
            aggregate =
                aggregate,
            issues =
                issues
        )

        validateProfileCount(
            aggregate =
                aggregate,
            issues =
                issues
        )

        val sortedIssues =
            issues.sortedWith(
                compareBy<
                        OFFNutritionReferenceAggregateValidationIssue
                        >(
                    { issue ->
                        issue.severity.name
                    },
                    { issue ->
                        issue.type.name
                    },
                    { issue ->
                        issue.nutrientKey.orEmpty()
                    },
                    { issue ->
                        issue.message
                    }
                )
            )

        val warningCount =
            sortedIssues.count { issue ->
                issue.severity ==
                        OFFNutritionReferenceAggregateIssueSeverity.WARNING
            }

        val errorCount =
            sortedIssues.count { issue ->
                issue.severity ==
                        OFFNutritionReferenceAggregateIssueSeverity.ERROR
            }

        val status =
            when {
                errorCount > 0 ->
                    OFFNutritionReferenceAggregateValidationStatus.REJECTED

                warningCount > 0 ->
                    OFFNutritionReferenceAggregateValidationStatus.WARNING

                else ->
                    OFFNutritionReferenceAggregateValidationStatus.ACCEPTED
            }

        return OFFNutritionReferenceAggregateValidationEntry(
            canonicalId =
                aggregate.canonicalId,
            status =
                status,
            profileCount =
                aggregate.profileCount,
            nutrientCount =
                aggregate.nutrition.size,
            warningCount =
                warningCount,
            errorCount =
                errorCount,
            issues =
                sortedIssues
        )
    }

    private fun validateStructuralIntegrity(
        aggregate: CanonicalOFFNutritionReferenceAggregate,
        issues: MutableList<OFFNutritionReferenceAggregateValidationIssue>
    ) {

        if (aggregate.canonicalId.isBlank()) {
            issues +=
                error(
                    type =
                        OFFNutritionReferenceAggregateIssueType.EMPTY_CANONICAL_ID,
                    message =
                        "Aggregate canonicalId is blank."
                )
        }

        if (aggregate.nutrition.isEmpty()) {
            issues +=
                error(
                    type =
                        OFFNutritionReferenceAggregateIssueType.EMPTY_NUTRITION,
                    message =
                        "Aggregate contains no nutrition values."
                )
        }

        if (
            aggregate.nutrition.keys !=
            aggregate.nutrientStatistics.keys
        ) {
            issues +=
                error(
                    type =
                        OFFNutritionReferenceAggregateIssueType
                            .NUTRITION_STATISTICS_KEY_MISMATCH,
                    message =
                        "Nutrition and nutrient-statistics keys differ."
                )
        }
    }

    private fun validateNutritionValues(
        aggregate: CanonicalOFFNutritionReferenceAggregate,
        issues: MutableList<OFFNutritionReferenceAggregateValidationIssue>
    ) {

        aggregate.nutrition
            .toSortedMap()
            .forEach { (nutrientKey, value) ->

                if (!value.isFinite()) {
                    issues +=
                        error(
                            type =
                                OFFNutritionReferenceAggregateIssueType
                                    .NON_FINITE_NUTRIENT_VALUE,
                            nutrientKey =
                                nutrientKey,
                            message =
                                "Aggregated nutrient value is not finite."
                        )

                    return@forEach
                }

                if (value < 0.0) {
                    issues +=
                        error(
                            type =
                                OFFNutritionReferenceAggregateIssueType
                                    .NEGATIVE_NUTRIENT_VALUE,
                            nutrientKey =
                                nutrientKey,
                            message =
                                "Aggregated nutrient value is negative.",
                            observedValue =
                                value,
                            threshold =
                                0.0
                        )
                }

                when {
                    nutrientKey == ENERGY_KEY &&
                            value > MAXIMUM_ENERGY_KCAL_PER_100G -> {

                        issues +=
                            error(
                                type =
                                    OFFNutritionReferenceAggregateIssueType
                                        .EXTREME_ENERGY_VALUE,
                                nutrientKey =
                                    nutrientKey,
                                message =
                                    "Energy exceeds physical validation limit.",
                                observedValue =
                                    value,
                                threshold =
                                    MAXIMUM_ENERGY_KCAL_PER_100G
                            )
                    }

                    nutrientKey != ENERGY_KEY &&
                            value > MAXIMUM_NUTRIENT_GRAMS_PER_100G -> {

                        issues +=
                            error(
                                type =
                                    OFFNutritionReferenceAggregateIssueType
                                        .EXTREME_NUTRIENT_VALUE,
                                nutrientKey =
                                    nutrientKey,
                                message =
                                    "Nutrient exceeds 100 g per 100 g.",
                                observedValue =
                                    value,
                                threshold =
                                    MAXIMUM_NUTRIENT_GRAMS_PER_100G
                            )
                    }
                }
            }
    }

    private fun validateNutrientStatistics(
        aggregate: CanonicalOFFNutritionReferenceAggregate,
        issues: MutableList<OFFNutritionReferenceAggregateValidationIssue>
    ) {

        aggregate.nutrientStatistics
            .toSortedMap()
            .forEach { (nutrientKey, statistics) ->

                validateStatisticsEntry(
                    aggregate =
                        aggregate,
                    nutrientKey =
                        nutrientKey,
                    statistics =
                        statistics,
                    issues =
                        issues
                )
            }
    }

    private fun validateStatisticsEntry(
        aggregate: CanonicalOFFNutritionReferenceAggregate,
        nutrientKey: String,
        statistics: OFFNutritionReferenceNutrientStatistics,
        issues: MutableList<OFFNutritionReferenceAggregateValidationIssue>
    ) {

        if (statistics.observationCount <= 0) {
            issues +=
                error(
                    type =
                        OFFNutritionReferenceAggregateIssueType
                            .INVALID_OBSERVATION_COUNT,
                    nutrientKey =
                        nutrientKey,
                    message =
                        "Nutrient observation count is not positive.",
                    observedValue =
                        statistics.observationCount.toDouble(),
                    threshold =
                        1.0
                )
        }

        if (
            statistics.observationCount >
            aggregate.profileCount
        ) {
            issues +=
                error(
                    type =
                        OFFNutritionReferenceAggregateIssueType
                            .OBSERVATION_COUNT_EXCEEDS_PROFILE_COUNT,
                    nutrientKey =
                        nutrientKey,
                    message =
                        "Nutrient observation count exceeds profile count.",
                    observedValue =
                        statistics.observationCount.toDouble(),
                    threshold =
                        aggregate.profileCount.toDouble()
                )
        }

        if (
            statistics.median < statistics.minimum ||
            statistics.median > statistics.maximum
        ) {
            issues +=
                error(
                    type =
                        OFFNutritionReferenceAggregateIssueType
                            .NUTRIENT_MEDIAN_OUTSIDE_RANGE,
                    nutrientKey =
                        nutrientKey,
                    message =
                        "Median is outside minimum/maximum range.",
                    observedValue =
                        statistics.median
                )
        }

        val aggregateValue =
            aggregate.nutrition[nutrientKey]

        if (
            aggregateValue != null &&
            abs(
                aggregateValue -
                        statistics.median
            ) > DOUBLE_TOLERANCE
        ) {
            issues +=
                error(
                    type =
                        OFFNutritionReferenceAggregateIssueType
                            .MEDIAN_VALUE_MISMATCH,
                    nutrientKey =
                        nutrientKey,
                    message =
                        "Persisted nutrition value differs from median.",
                    observedValue =
                        aggregateValue,
                    threshold =
                        statistics.median
                )
        }
    }

    private fun validateObservationCoverage(
        aggregate: CanonicalOFFNutritionReferenceAggregate,
        issues: MutableList<OFFNutritionReferenceAggregateValidationIssue>
    ) {

        if (aggregate.profileCount < MINIMUM_PROFILES_FOR_COVERAGE_CHECK) {
            return
        }

        CORE_NUTRIENT_KEYS
            .sorted()
            .forEach { nutrientKey ->

                val statistics =
                    aggregate.nutrientStatistics[nutrientKey]
                        ?: return@forEach

                val coverage =
                    statistics.observationCount.toDouble() /
                            aggregate.profileCount.toDouble()

                if (
                    coverage <
                    MINIMUM_CORE_NUTRIENT_OBSERVATION_COVERAGE
                ) {
                    issues +=
                        warning(
                            type =
                                OFFNutritionReferenceAggregateIssueType
                                    .LOW_NUTRIENT_OBSERVATION_COVERAGE,
                            nutrientKey =
                                nutrientKey,
                            message =
                                "Core nutrient has low observation coverage.",
                            observedValue =
                                coverage,
                            threshold =
                                MINIMUM_CORE_NUTRIENT_OBSERVATION_COVERAGE
                        )
                }
            }
    }

    private fun validateVariation(
        aggregate: CanonicalOFFNutritionReferenceAggregate,
        issues: MutableList<OFFNutritionReferenceAggregateValidationIssue>
    ) {

        aggregate.nutrientStatistics
            .toSortedMap()
            .forEach { (nutrientKey, statistics) ->

                if (
                    statistics.observationCount <
                    MINIMUM_OBSERVATIONS_FOR_VARIATION_CHECK
                ) {
                    return@forEach
                }

                val absoluteRange =
                    statistics.maximum -
                            statistics.minimum

                val minimumAbsoluteRange =
                    minimumAbsoluteRange(
                        nutrientKey =
                            nutrientKey
                    )

                val relativeRange =
                    absoluteRange /
                            max(
                                abs(statistics.median),
                                minimumRelativeRangeDenominator(
                                    nutrientKey =
                                        nutrientKey
                                )
                            )

                if (
                    absoluteRange >= minimumAbsoluteRange &&
                    relativeRange >=
                    MAXIMUM_RELATIVE_NUTRIENT_RANGE
                ) {
                    issues +=
                        warning(
                            type =
                                OFFNutritionReferenceAggregateIssueType
                                    .HIGH_NUTRIENT_VARIATION,
                            nutrientKey =
                                nutrientKey,
                            message =
                                "Nutrient range is high relative to its median.",
                            observedValue =
                                relativeRange,
                            threshold =
                                MAXIMUM_RELATIVE_NUTRIENT_RANGE
                        )
                }
            }
    }

    private fun validateEnergyConsistency(
        aggregate: CanonicalOFFNutritionReferenceAggregate,
        issues: MutableList<OFFNutritionReferenceAggregateValidationIssue>
    ) {

        val energy =
            aggregate.nutrition[ENERGY_KEY]
                ?: return

        val fat =
            aggregate.nutrition[FAT_KEY]
                ?: return

        val carbohydrates =
            aggregate.nutrition[CARBOHYDRATES_KEY]
                ?: return

        val proteins =
            aggregate.nutrition[PROTEINS_KEY]
                ?: return

        if (energy < MINIMUM_ENERGY_FOR_CONSISTENCY_CHECK) {
            return
        }

        val fiber =
            aggregate.nutrition[FIBER_KEY]
                ?: 0.0

        val estimatedEnergy =
            fat * FAT_KCAL_PER_GRAM +
                    carbohydrates * CARBOHYDRATE_KCAL_PER_GRAM +
                    proteins * PROTEIN_KCAL_PER_GRAM +
                    fiber * FIBER_KCAL_PER_GRAM

        val relativeDifference =
            abs(energy - estimatedEnergy) /
                    max(
                        energy,
                        MINIMUM_ENERGY_FOR_CONSISTENCY_CHECK
                    )

        if (
            relativeDifference >
            MAXIMUM_ENERGY_RELATIVE_DIFFERENCE
        ) {
            issues +=
                warning(
                    type =
                        OFFNutritionReferenceAggregateIssueType
                            .ENERGY_MACRONUTRIENT_MISMATCH,
                    nutrientKey =
                        ENERGY_KEY,
                    message =
                        "Energy differs substantially from macronutrient estimate.",
                    observedValue =
                        relativeDifference,
                    threshold =
                        MAXIMUM_ENERGY_RELATIVE_DIFFERENCE
                )
        }
    }

    private fun validateProfileCount(
        aggregate: CanonicalOFFNutritionReferenceAggregate,
        issues: MutableList<OFFNutritionReferenceAggregateValidationIssue>
    ) {

        if (
            aggregate.profileCount >=
            HIGH_PROFILE_COUNT_THRESHOLD
        ) {
            issues +=
                warning(
                    type =
                        OFFNutritionReferenceAggregateIssueType
                            .HIGH_PROFILE_COUNT,
                    message =
                        "Aggregate contains many distinct nutrition profiles.",
                    observedValue =
                        aggregate.profileCount.toDouble(),
                    threshold =
                        HIGH_PROFILE_COUNT_THRESHOLD.toDouble()
                )
        }
    }

    private fun minimumAbsoluteRange(
        nutrientKey: String
    ): Double =
        when (nutrientKey) {
            ENERGY_KEY ->
                MINIMUM_ENERGY_ABSOLUTE_RANGE

            SALT_KEY ->
                MINIMUM_SALT_ABSOLUTE_RANGE

            else ->
                MINIMUM_STANDARD_NUTRIENT_ABSOLUTE_RANGE
        }

    private fun minimumRelativeRangeDenominator(
        nutrientKey: String
    ): Double =
        when (nutrientKey) {
            ENERGY_KEY ->
                MINIMUM_ENERGY_RELATIVE_DENOMINATOR

            SALT_KEY ->
                MINIMUM_SALT_RELATIVE_DENOMINATOR

            else ->
                MINIMUM_STANDARD_RELATIVE_DENOMINATOR
        }

    private fun warning(
        type: OFFNutritionReferenceAggregateIssueType,
        nutrientKey: String? = null,
        message: String,
        observedValue: Double? = null,
        threshold: Double? = null
    ): OFFNutritionReferenceAggregateValidationIssue =
        OFFNutritionReferenceAggregateValidationIssue(
            type =
                type,
            severity =
                OFFNutritionReferenceAggregateIssueSeverity.WARNING,
            nutrientKey =
                nutrientKey,
            message =
                message,
            observedValue =
                observedValue,
            threshold =
                threshold
        )

    private fun error(
        type: OFFNutritionReferenceAggregateIssueType,
        nutrientKey: String? = null,
        message: String,
        observedValue: Double? = null,
        threshold: Double? = null
    ): OFFNutritionReferenceAggregateValidationIssue =
        OFFNutritionReferenceAggregateValidationIssue(
            type =
                type,
            severity =
                OFFNutritionReferenceAggregateIssueSeverity.ERROR,
            nutrientKey =
                nutrientKey,
            message =
                message,
            observedValue =
                observedValue,
            threshold =
                threshold
        )

    companion object {

        const val DOUBLE_TOLERANCE =
            1e-9

        const val MAXIMUM_ENERGY_KCAL_PER_100G =
            1_000.0

        const val MAXIMUM_NUTRIENT_GRAMS_PER_100G =
            100.0

        const val MINIMUM_PROFILES_FOR_COVERAGE_CHECK =
            3

        const val MINIMUM_CORE_NUTRIENT_OBSERVATION_COVERAGE =
            0.5

        const val MINIMUM_OBSERVATIONS_FOR_VARIATION_CHECK =
            3

        const val MAXIMUM_RELATIVE_NUTRIENT_RANGE =
            3.0

        const val MINIMUM_ENERGY_ABSOLUTE_RANGE =
            100.0

        const val MINIMUM_STANDARD_NUTRIENT_ABSOLUTE_RANGE =
            5.0

        const val MINIMUM_SALT_ABSOLUTE_RANGE =
            1.0

        const val MINIMUM_ENERGY_RELATIVE_DENOMINATOR =
            50.0

        const val MINIMUM_STANDARD_RELATIVE_DENOMINATOR =
            1.0

        const val MINIMUM_SALT_RELATIVE_DENOMINATOR =
            0.1

        const val MINIMUM_ENERGY_FOR_CONSISTENCY_CHECK =
            20.0

        const val MAXIMUM_ENERGY_RELATIVE_DIFFERENCE =
            0.35

        const val HIGH_PROFILE_COUNT_THRESHOLD =
            50

        const val FAT_KCAL_PER_GRAM =
            9.0

        const val CARBOHYDRATE_KCAL_PER_GRAM =
            4.0

        const val PROTEIN_KCAL_PER_GRAM =
            4.0

        const val FIBER_KCAL_PER_GRAM =
            2.0

        const val ENERGY_KEY =
            "energyKcalPer100g"

        const val FAT_KEY =
            "fatPer100g"

        const val CARBOHYDRATES_KEY =
            "carbohydratesPer100g"

        const val PROTEINS_KEY =
            "proteinsPer100g"

        const val FIBER_KEY =
            "fiberPer100g"

        const val SALT_KEY =
            "saltPer100g"

        val CORE_NUTRIENT_KEYS =
            setOf(
                ENERGY_KEY,
                FAT_KEY,
                CARBOHYDRATES_KEY,
                PROTEINS_KEY,
                SALT_KEY
            )
    }
}