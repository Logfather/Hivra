package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.evaluation

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionConflictMatchType
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionConflictNutrient
import java.io.File

data class ResultingNutritionConflictEvaluation(
    val aggregateFile: File,
    val runtimeFile: File,
    val conflictEntryCount: Long,
    val evaluatedConflictEntryCount: Long,
    val countsByClassification:
    Map<ResultingNutritionConflictClassification, Long>,
    val countsBySeverity:
    Map<ResultingNutritionConflictSeverity, Long>,
    val countsByMatchType:
    Map<ResultingNutritionConflictMatchType, Long>,
    val countsByConflictingNutrientCount: Map<Int, Long>,
    val classificationsByNutrient:
    Map<
            ResultingNutritionConflictNutrient,
            Map<ResultingNutritionConflictClassification, Long>
            >,
    val uniformScaleMismatchCount: Long,
    val energyUnitConversionMismatchCount: Long,
    val singleNutrientConflictCount: Long,
    val multiNutrientProfileConflictCount: Long,
    val examples:
    List<ResultingNutritionConflictEvaluationExample>,
    val omittedExampleCount: Long,
    val durationMillis: Long
) {

    init {
        require(conflictEntryCount >= 0L)
        require(evaluatedConflictEntryCount >= 0L)
        require(uniformScaleMismatchCount >= 0L)
        require(energyUnitConversionMismatchCount >= 0L)
        require(singleNutrientConflictCount >= 0L)
        require(multiNutrientProfileConflictCount >= 0L)
        require(omittedExampleCount >= 0L)
        require(durationMillis >= 0L)

        require(
            evaluatedConflictEntryCount ==
                    conflictEntryCount
        ) {
            "Every Nutrition conflict entry must be evaluated."
        }

        require(
            evaluatedConflictEntryCount ==
                    countsByClassification.values.sum()
        ) {
            "Classification counts must equal evaluated conflicts."
        }

        require(
            evaluatedConflictEntryCount ==
                    countsBySeverity.values.sum()
        ) {
            "Severity counts must equal evaluated conflicts."
        }

        require(
            evaluatedConflictEntryCount ==
                    countsByMatchType.values.sum()
        ) {
            "Match-type counts must equal evaluated conflicts."
        }

        require(
            evaluatedConflictEntryCount ==
                    countsByConflictingNutrientCount.values.sum()
        ) {
            "Conflicting-nutrient counts must equal evaluated conflicts."
        }

        require(
            evaluatedConflictEntryCount ==
                    uniformScaleMismatchCount +
                    energyUnitConversionMismatchCount +
                    singleNutrientConflictCount +
                    multiNutrientProfileConflictCount
        ) {
            "Primary conflict classification counts are inconsistent."
        }

        require(
            examples.size.toLong() +
                    omittedExampleCount ==
                    evaluatedConflictEntryCount
        ) {
            "Reported and omitted examples must equal evaluated conflicts."
        }

        require(
            examples ==
                    examples.sortedWith(
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
        ) {
            "Conflict evaluation examples must be deterministically sorted."
        }
    }
}

data class ResultingNutritionConflictEvaluationExample(
    val aggregateCanonicalId: String,
    val runtimeCanonicalId: String,
    val matchType: ResultingNutritionConflictMatchType,
    val classification: ResultingNutritionConflictClassification,
    val severity: ResultingNutritionConflictSeverity,
    val conflictCount: Int,
    val conflictingNutrients:
    List<ResultingNutritionConflictNutrient>,
    val maximumAbsoluteDifference: Double,
    val maximumToleranceMultiple: Double,
    val inferredScaleFactor: Double?,
    val scaleFactorRelativeSpread: Double?,
    val rationale: String
) {

    init {
        require(aggregateCanonicalId.isNotBlank())
        require(runtimeCanonicalId.isNotBlank())
        require(conflictCount > 0)
        require(conflictingNutrients.size == conflictCount)
        require(maximumAbsoluteDifference > 0.0)
        require(maximumToleranceMultiple > 1.0)
        require(rationale.isNotBlank())

        require(
            conflictingNutrients ==
                    conflictingNutrients
                        .distinct()
                        .sortedBy { nutrient ->
                            nutrient.name
                        }
        ) {
            "Conflicting nutrients must be unique and sorted."
        }

        if (inferredScaleFactor != null) {
            require(inferredScaleFactor > 0.0)
            require(inferredScaleFactor.isFinite())
        }

        if (scaleFactorRelativeSpread != null) {
            require(scaleFactorRelativeSpread >= 0.0)
            require(scaleFactorRelativeSpread.isFinite())
        }
    }
}

enum class ResultingNutritionConflictClassification {

    /**
     * Mindestens drei Nährstoffe weisen einen weitgehend gemeinsamen
     * multiplikativen Faktor auf.
     *
     * Dieses Muster passt beispielsweise zu Portions-/100-g-
     * Abweichungen, beweist diese Ursache aber nicht.
     */
    LIKELY_UNIFORM_SCALE_MISMATCH,

    /**
     * Der Energiewert unterscheidet sich ungefähr um den Faktor 4,184
     * oder dessen Kehrwert.
     *
     * Dieses Muster passt zu einer möglichen kcal-/kJ-Verwechslung.
     */
    LIKELY_ENERGY_UNIT_CONVERSION_MISMATCH,

    /**
     * Genau ein Nährstoff überschreitet seine Vergleichstoleranz.
     */
    SINGLE_NUTRIENT_CONFLICT,

    /**
     * Mehrere Nährstoffe konfligieren, ohne ein hinreichend
     * einheitliches Skalierungsmuster zu bilden.
     */
    MULTI_NUTRIENT_PROFILE_CONFLICT
}

enum class ResultingNutritionConflictSeverity {
    LOW,
    MEDIUM,
    HIGH,
    EXTREME
}