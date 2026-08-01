package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.evaluation

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionConflictAnalysis
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionConflictExample
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionConflictNutrient
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionNutrientConflict
import kotlin.math.abs
import kotlin.math.ln

class ResultingNutritionConflictEvaluator(
    private val maximumReportedExamples: Int =
        DEFAULT_MAXIMUM_REPORTED_EXAMPLES
) {

    init {
        require(maximumReportedExamples >= 0)
    }

    fun evaluate(
        analysis: ResultingNutritionConflictAnalysis
    ): ResultingNutritionConflictEvaluation {

        /*
         * Die Evaluation benötigt jeden Konflikteintrag.
         *
         * Der produktive Runner muss den Conflict Analyzer deshalb mit
         * einer hinreichend großen Example-Grenze ausführen.
         */
        require(
            analysis.omittedConflictExampleCount == 0L
        ) {
            "Nutrition conflict evaluation requires all conflict " +
                    "examples. omittedConflictExampleCount=" +
                    analysis.omittedConflictExampleCount
        }

        require(
            analysis.conflictExamples.size.toLong() ==
                    analysis.conflictEntryCount
        ) {
            "Conflict examples must cover every conflict entry."
        }

        val startedAt =
            System.currentTimeMillis()

        val evaluatedExamples =
            analysis.conflictExamples
                .map(::evaluateExample)

        val countsByClassification =
            evaluatedExamples
                .groupingBy { example ->
                    example.classification
                }
                .eachCount()
                .mapValues { (_, count) ->
                    count.toLong()
                }
                .toSortedMap(
                    compareBy { classification ->
                        classification.name
                    }
                )

        val countsBySeverity =
            evaluatedExamples
                .groupingBy { example ->
                    example.severity
                }
                .eachCount()
                .mapValues { (_, count) ->
                    count.toLong()
                }
                .toSortedMap(
                    compareBy { severity ->
                        severity.name
                    }
                )

        val countsByMatchType =
            evaluatedExamples
                .groupingBy { example ->
                    example.matchType
                }
                .eachCount()
                .mapValues { (_, count) ->
                    count.toLong()
                }
                .toSortedMap(
                    compareBy { matchType ->
                        matchType.name
                    }
                )

        val countsByConflictingNutrientCount =
            evaluatedExamples
                .groupingBy { example ->
                    example.conflictCount
                }
                .eachCount()
                .mapValues { (_, count) ->
                    count.toLong()
                }
                .toSortedMap()

        val classificationsByNutrient =
            ResultingNutritionConflictNutrient
                .entries
                .mapNotNull { nutrient ->

                    val classifications =
                        evaluatedExamples
                            .asSequence()
                            .filter { example ->
                                nutrient in
                                        example.conflictingNutrients
                            }
                            .groupingBy { example ->
                                example.classification
                            }
                            .eachCount()
                            .mapValues { (_, count) ->
                                count.toLong()
                            }
                            .toSortedMap(
                                compareBy { classification ->
                                    classification.name
                                }
                            )

                    if (classifications.isEmpty()) {
                        null
                    } else {
                        nutrient to classifications
                    }
                }
                .toMap()
                .toSortedMap(
                    compareBy { nutrient ->
                        nutrient.name
                    }
                )

        val sortedExamples =
            evaluatedExamples
                .sortedWith(
                    compareByDescending<
                            ResultingNutritionConflictEvaluationExample
                            > { example ->
                        example.severity.ordinal
                    }
                        .thenByDescending { example ->
                            example.maximumToleranceMultiple
                        }
                        .thenBy { example ->
                            example.aggregateCanonicalId
                        }
                )

        val reportedExamples =
            sortedExamples
                .take(
                    maximumReportedExamples
                )

        return ResultingNutritionConflictEvaluation(
            aggregateFile =
                analysis.aggregateFile,
            runtimeFile =
                analysis.runtimeFile,
            conflictEntryCount =
                analysis.conflictEntryCount,
            evaluatedConflictEntryCount =
                evaluatedExamples.size.toLong(),
            countsByClassification =
                countsByClassification,
            countsBySeverity =
                countsBySeverity,
            countsByMatchType =
                countsByMatchType,
            countsByConflictingNutrientCount =
                countsByConflictingNutrientCount,
            classificationsByNutrient =
                classificationsByNutrient,
            uniformScaleMismatchCount =
                count(
                    examples =
                        evaluatedExamples,
                    classification =
                        ResultingNutritionConflictClassification
                            .LIKELY_UNIFORM_SCALE_MISMATCH
                ),
            energyUnitConversionMismatchCount =
                count(
                    examples =
                        evaluatedExamples,
                    classification =
                        ResultingNutritionConflictClassification
                            .LIKELY_ENERGY_UNIT_CONVERSION_MISMATCH
                ),
            singleNutrientConflictCount =
                count(
                    examples =
                        evaluatedExamples,
                    classification =
                        ResultingNutritionConflictClassification
                            .SINGLE_NUTRIENT_CONFLICT
                ),
            multiNutrientProfileConflictCount =
                count(
                    examples =
                        evaluatedExamples,
                    classification =
                        ResultingNutritionConflictClassification
                            .MULTI_NUTRIENT_PROFILE_CONFLICT
                ),
            examples =
                reportedExamples,
            omittedExampleCount =
                evaluatedExamples.size.toLong() -
                        reportedExamples.size.toLong(),
            durationMillis =
                System.currentTimeMillis() -
                        startedAt
        )
    }

    private fun evaluateExample(
        example: ResultingNutritionConflictExample
    ): ResultingNutritionConflictEvaluationExample {

        val maximumToleranceMultiple =
            example.conflicts
                .maxOf { conflict ->
                    conflict.absoluteDifference /
                            conflict.tolerance
                }

        val severity =
            classifySeverity(
                maximumToleranceMultiple =
                    maximumToleranceMultiple
            )

        val scaleEvidence =
            calculateScaleEvidence(
                conflicts =
                    example.conflicts
            )

        val classification =
            when {
                scaleEvidence != null ->
                    ResultingNutritionConflictClassification
                        .LIKELY_UNIFORM_SCALE_MISMATCH

                isLikelyEnergyUnitConversionMismatch(
                    example =
                        example
                ) ->
                    ResultingNutritionConflictClassification
                        .LIKELY_ENERGY_UNIT_CONVERSION_MISMATCH

                example.conflictCount == 1 ->
                    ResultingNutritionConflictClassification
                        .SINGLE_NUTRIENT_CONFLICT

                else ->
                    ResultingNutritionConflictClassification
                        .MULTI_NUTRIENT_PROFILE_CONFLICT
            }

        val rationale =
            rationale(
                classification =
                    classification,
                example =
                    example,
                scaleEvidence =
                    scaleEvidence
            )

        return ResultingNutritionConflictEvaluationExample(
            aggregateCanonicalId =
                example.aggregateCanonicalId,
            runtimeCanonicalId =
                example.runtimeCanonicalId,
            matchType =
                example.matchType,
            classification =
                classification,
            severity =
                severity,
            conflictCount =
                example.conflictCount,
            conflictingNutrients =
                example.conflicts
                    .map { conflict ->
                        conflict.nutrient
                    }
                    .distinct()
                    .sortedBy { nutrient ->
                        nutrient.name
                    },
            maximumAbsoluteDifference =
                example.maximumAbsoluteDifference,
            maximumToleranceMultiple =
                maximumToleranceMultiple,
            inferredScaleFactor =
                scaleEvidence?.medianScaleFactor,
            scaleFactorRelativeSpread =
                scaleEvidence?.relativeSpread,
            rationale =
                rationale
        )
    }

    private fun calculateScaleEvidence(
        conflicts: List<ResultingNutritionNutrientConflict>
    ): ScaleEvidence? {

        val ratios =
            conflicts
                .asSequence()
                .filter { conflict ->
                    conflict.aggregateValue >
                            MINIMUM_SCALE_VALUE &&
                            conflict.runtimeValue >
                            MINIMUM_SCALE_VALUE
                }
                .map { conflict ->
                    conflict.runtimeValue /
                            conflict.aggregateValue
                }
                .filter { ratio ->
                    ratio.isFinite() &&
                            ratio > 0.0
                }
                .toList()
                .sorted()

        if (
            ratios.size <
            MINIMUM_SCALE_NUTRIENT_COUNT
        ) {
            return null
        }

        val medianScaleFactor =
            median(
                sortedValues =
                    ratios
            )

        if (
            medianScaleFactor in
            MINIMUM_NEAR_IDENTITY_SCALE..
            MAXIMUM_NEAR_IDENTITY_SCALE
        ) {
            return null
        }

        val relativeSpread =
            ratios
                .maxOf { ratio ->
                    abs(
                        ln(
                            ratio /
                                    medianScaleFactor
                        )
                    )
                }

        if (
            relativeSpread >
            MAXIMUM_LOG_SCALE_SPREAD
        ) {
            return null
        }

        return ScaleEvidence(
            medianScaleFactor =
                medianScaleFactor,
            relativeSpread =
                relativeSpread,
            supportingNutrientCount =
                ratios.size
        )
    }

    private fun isLikelyEnergyUnitConversionMismatch(
        example: ResultingNutritionConflictExample
    ): Boolean {

        /*
         * Eine kcal-/kJ-Verwechslung wird nur dann als primäre Ursache
         * klassifiziert, wenn ausschließlich der Energiewert konfligiert.
         *
         * Sobald weitere Nährstoffe abweichen, handelt es sich um einen
         * umfassenderen Profilkonflikt. Ein zufällig ähnlicher Energiefaktor
         * darf diesen nicht überdecken.
         */
        if (example.conflictCount != 1) {
            return false
        }

        val energyConflict =
            example.conflicts
                .singleOrNull()
                ?.takeIf { conflict ->
                    conflict.nutrient ==
                            ResultingNutritionConflictNutrient.ENERGY
                }
                ?: return false

        if (
            energyConflict.aggregateValue <= 0.0 ||
            energyConflict.runtimeValue <= 0.0
        ) {
            return false
        }

        val ratio =
            energyConflict.runtimeValue /
                    energyConflict.aggregateValue

        return approximatelyEqual(
            actual =
                ratio,
            expected =
                KILOJOULE_PER_KILOCALORIE
        ) ||
                approximatelyEqual(
                    actual =
                        ratio,
                    expected =
                        1.0 /
                                KILOJOULE_PER_KILOCALORIE
                )
    }

    private fun approximatelyEqual(
        actual: Double,
        expected: Double
    ): Boolean {

        val relativeDifference =
            abs(
                actual -
                        expected
            ) /
                    expected

        return relativeDifference <=
                ENERGY_UNIT_RELATIVE_TOLERANCE
    }

    private fun classifySeverity(
        maximumToleranceMultiple: Double
    ): ResultingNutritionConflictSeverity =
        when {
            maximumToleranceMultiple <=
                    LOW_MAXIMUM_TOLERANCE_MULTIPLE ->
                ResultingNutritionConflictSeverity.LOW

            maximumToleranceMultiple <=
                    MEDIUM_MAXIMUM_TOLERANCE_MULTIPLE ->
                ResultingNutritionConflictSeverity.MEDIUM

            maximumToleranceMultiple <=
                    HIGH_MAXIMUM_TOLERANCE_MULTIPLE ->
                ResultingNutritionConflictSeverity.HIGH

            else ->
                ResultingNutritionConflictSeverity.EXTREME
        }

    private fun rationale(
        classification: ResultingNutritionConflictClassification,
        example: ResultingNutritionConflictExample,
        scaleEvidence: ScaleEvidence?
    ): String =
        when (classification) {
            ResultingNutritionConflictClassification
                .LIKELY_UNIFORM_SCALE_MISMATCH -> {

                requireNotNull(
                    scaleEvidence
                )

                "At least ${scaleEvidence.supportingNutrientCount} " +
                        "conflicting nutrients share an approximately " +
                        "uniform scale factor of " +
                        "${scaleEvidence.medianScaleFactor}. " +
                        "This is compatible with a serving/base-unit " +
                        "mismatch but does not prove that cause."
            }

            ResultingNutritionConflictClassification
                .LIKELY_ENERGY_UNIT_CONVERSION_MISMATCH -> {

                "The energy-value ratio is close to 4.184 or its " +
                        "reciprocal, which is compatible with a " +
                        "kcal/kJ unit-conversion mismatch."
            }

            ResultingNutritionConflictClassification
                .SINGLE_NUTRIENT_CONFLICT -> {

                val nutrient =
                    example.conflicts
                        .single()
                        .nutrient

                "Only ${nutrient.name} exceeds its configured " +
                        "comparison tolerance."
            }

            ResultingNutritionConflictClassification
                .MULTI_NUTRIENT_PROFILE_CONFLICT -> {

                "Multiple nutrients exceed their comparison " +
                        "tolerances without a sufficiently uniform " +
                        "scale-factor pattern."
            }
        }

    private fun median(
        sortedValues: List<Double>
    ): Double {

        require(sortedValues.isNotEmpty())

        val middleIndex =
            sortedValues.size /
                    2

        return if (
            sortedValues.size %
            2 ==
            1
        ) {
            sortedValues[
                middleIndex
            ]
        } else {
            val lower =
                sortedValues[
                    middleIndex - 1
                ]

            val upper =
                sortedValues[
                    middleIndex
                ]

            lower +
                    (
                            upper -
                                    lower
                            ) /
                    2.0
        }
    }

    private fun count(
        examples:
        List<ResultingNutritionConflictEvaluationExample>,
        classification:
        ResultingNutritionConflictClassification
    ): Long =
        examples
            .count { example ->
                example.classification ==
                        classification
            }
            .toLong()

    private data class ScaleEvidence(
        val medianScaleFactor: Double,
        val relativeSpread: Double,
        val supportingNutrientCount: Int
    )

    private companion object {

        const val DEFAULT_MAXIMUM_REPORTED_EXAMPLES =
            250

        const val MINIMUM_SCALE_NUTRIENT_COUNT =
            3

        const val MINIMUM_SCALE_VALUE =
            0.01

        const val MINIMUM_NEAR_IDENTITY_SCALE =
            0.8

        const val MAXIMUM_NEAR_IDENTITY_SCALE =
            1.25

        /*
         * ln(1,25) ≈ 0,223.
         *
         * Damit darf jeder unterstützende Ratio-Wert maximal etwa
         * 25 Prozent vom Medianfaktor abweichen.
         */
        const val MAXIMUM_LOG_SCALE_SPREAD =
            0.22314355131420976

        const val KILOJOULE_PER_KILOCALORIE =
            4.184

        const val ENERGY_UNIT_RELATIVE_TOLERANCE =
            0.10

        const val LOW_MAXIMUM_TOLERANCE_MULTIPLE =
            2.0

        const val MEDIUM_MAXIMUM_TOLERANCE_MULTIPLE =
            10.0

        const val HIGH_MAXIMUM_TOLERANCE_MULTIPLE =
            50.0
    }
}