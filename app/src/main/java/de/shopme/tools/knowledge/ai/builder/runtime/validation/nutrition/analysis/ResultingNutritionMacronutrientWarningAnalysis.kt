package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.analysis

import java.io.File

data class ResultingNutritionMacronutrientWarningAnalysis(
    val inputFile: File,
    val inputFileSizeBytes: Long,
    val entryCount: Long,
    val warningEntryCount: Long,
    val fiberDoubleCountingCandidateCount: Long,
    val coreMacronutrientExcessCount: Long,
    val countsByClassification:
    Map<MacronutrientWarningClassification, Long>,
    val countsByExcessBucket:
    Map<MacronutrientWarningExcessBucket, Long>,
    val maximumMacronutrientSum: Double,
    val maximumExcessGrams: Double,
    val examples:
    List<ResultingNutritionMacronutrientWarningExample>,
    val durationMillis: Long
) {

    val classifiedWarningCount: Long
        get() =
            fiberDoubleCountingCandidateCount +
                    coreMacronutrientExcessCount

    init {
        require(inputFileSizeBytes >= 0L)
        require(entryCount >= 0L)
        require(warningEntryCount >= 0L)
        require(fiberDoubleCountingCandidateCount >= 0L)
        require(coreMacronutrientExcessCount >= 0L)
        require(maximumMacronutrientSum >= 0.0)
        require(maximumExcessGrams >= 0.0)
        require(durationMillis >= 0L)

        require(
            warningEntryCount ==
                    classifiedWarningCount
        ) {
            "Every warning entry must have exactly one classification."
        }

        require(
            warningEntryCount ==
                    countsByClassification.values.sum()
        ) {
            "Warning count must equal classification count sum."
        }

        require(
            warningEntryCount ==
                    countsByExcessBucket.values.sum()
        ) {
            "Warning count must equal excess bucket count sum."
        }

        require(
            examples.all { example ->
                example.macronutrientSum >
                        WARNING_THRESHOLD_GRAMS
            }
        ) {
            "Every example must exceed the warning threshold."
        }
    }

    private companion object {

        const val WARNING_THRESHOLD_GRAMS =
            105.0
    }
}

data class ResultingNutritionMacronutrientWarningExample(
    val canonicalId: String,
    val classification:
    MacronutrientWarningClassification,
    val presentMacronutrientCount: Int,
    val fat: Double?,
    val carbohydrates: Double?,
    val fiber: Double?,
    val protein: Double?,
    val coreMacronutrientSum: Double,
    val macronutrientSum: Double,
    val excessGrams: Double
) {

    init {
        require(canonicalId.isNotBlank())
        require(presentMacronutrientCount >= 3)
        require(coreMacronutrientSum >= 0.0)
        require(macronutrientSum >= coreMacronutrientSum)
        require(excessGrams > 0.0)
    }
}

enum class MacronutrientWarningClassification {

    /**
     * fat + carbohydrates + protein liegen noch innerhalb des Grenzwerts.
     *
     * Die Überschreitung entsteht erst durch das zusätzliche Addieren
     * von fiber. Das ist ein starker Hinweis auf eine mögliche
     * Doppelzählung, weil Ballaststoffe je nach Datenquelle bereits
     * Bestandteil der Kohlenhydratdefinition sein können.
     */
    FIBER_DOUBLE_COUNTING_CANDIDATE,

    /**
     * Bereits fat + carbohydrates + protein überschreiten den Grenzwert.
     *
     * Dieser Fall kann nicht allein durch das zusätzliche Addieren von
     * fiber erklärt werden.
     */
    CORE_MACRONUTRIENT_SUM_EXCEEDS_MAXIMUM
}

enum class MacronutrientWarningExcessBucket {
    UP_TO_1_GRAM,
    UP_TO_5_GRAMS,
    UP_TO_10_GRAMS,
    UP_TO_20_GRAMS,
    MORE_THAN_20_GRAMS
}