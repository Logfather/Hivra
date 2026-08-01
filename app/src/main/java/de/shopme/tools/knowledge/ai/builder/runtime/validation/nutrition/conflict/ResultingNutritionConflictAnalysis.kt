package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict

import java.io.File

data class ResultingNutritionConflictAnalysis(
    val aggregateFile: File,
    val runtimeFile: File,
    val aggregateEntryCount: Long,
    val runtimeEntryCount: Long,
    val exactMatchedEntryCount: Long,
    val normalizationEquivalentMatchedEntryCount: Long,
    val matchedEntryCount: Long,
    val comparableEntryCount: Long,
    val nonComparableEntryCount: Long,
    val conflictEntryCount: Long,
    val conflictFreeEntryCount: Long,
    val nutrientComparisonCount: Long,
    val nutrientConflictCount: Long,
    val entryConflictRate: Double,
    val nutrientConflictRate: Double,
    val comparisonCountsByNutrient:
    Map<ResultingNutritionConflictNutrient, Long>,
    val conflictCountsByNutrient:
    Map<ResultingNutritionConflictNutrient, Long>,
    val maximumAbsoluteDifferenceByNutrient:
    Map<ResultingNutritionConflictNutrient, Double>,
    val conflictExamples:
    List<ResultingNutritionConflictExample>,
    val omittedConflictExampleCount: Long,
    val durationMillis: Long
) {

    init {
        require(aggregateEntryCount >= 0L)
        require(runtimeEntryCount >= 0L)
        require(exactMatchedEntryCount >= 0L)
        require(normalizationEquivalentMatchedEntryCount >= 0L)
        require(matchedEntryCount >= 0L)
        require(comparableEntryCount >= 0L)
        require(nonComparableEntryCount >= 0L)
        require(conflictEntryCount >= 0L)
        require(conflictFreeEntryCount >= 0L)
        require(nutrientComparisonCount >= 0L)
        require(nutrientConflictCount >= 0L)
        require(entryConflictRate in 0.0..1.0)
        require(nutrientConflictRate in 0.0..1.0)
        require(omittedConflictExampleCount >= 0L)
        require(durationMillis >= 0L)

        require(
            exactMatchedEntryCount +
                    normalizationEquivalentMatchedEntryCount ==
                    matchedEntryCount
        ) {
            "Exact and normalization-equivalent matches must equal " +
                    "matchedEntryCount."
        }

        require(
            comparableEntryCount +
                    nonComparableEntryCount ==
                    matchedEntryCount
        ) {
            "Comparable and non-comparable entries must equal " +
                    "matchedEntryCount."
        }

        require(
            conflictEntryCount +
                    conflictFreeEntryCount ==
                    comparableEntryCount
        ) {
            "Conflict and conflict-free entries must equal " +
                    "comparableEntryCount."
        }

        require(
            nutrientComparisonCount ==
                    comparisonCountsByNutrient.values.sum()
        ) {
            "Nutrient comparison counts are inconsistent."
        }

        require(
            nutrientConflictCount ==
                    conflictCountsByNutrient.values.sum()
        ) {
            "Nutrient conflict counts are inconsistent."
        }

        require(
            conflictExamples.size.toLong() +
                    omittedConflictExampleCount ==
                    conflictEntryCount
        ) {
            "Reported and omitted conflict examples must equal " +
                    "conflictEntryCount."
        }
    }
}

data class ResultingNutritionConflictExample(
    val aggregateCanonicalId: String,
    val runtimeCanonicalId: String,
    val matchType: ResultingNutritionConflictMatchType,
    val conflictCount: Int,
    val maximumAbsoluteDifference: Double,
    val conflicts: List<ResultingNutritionNutrientConflict>
) {

    init {
        require(aggregateCanonicalId.isNotBlank())
        require(runtimeCanonicalId.isNotBlank())
        require(conflictCount > 0)
        require(maximumAbsoluteDifference > 0.0)
        require(conflicts.size == conflictCount)

        require(
            conflicts ==
                    conflicts.sortedBy { conflict ->
                        conflict.nutrient.name
                    }
        ) {
            "Conflict details must be sorted by nutrient."
        }
    }
}

data class ResultingNutritionNutrientConflict(
    val nutrient: ResultingNutritionConflictNutrient,
    val aggregateValue: Double,
    val runtimeValue: Double,
    val absoluteDifference: Double,
    val tolerance: Double
) {

    init {
        require(aggregateValue.isFinite())
        require(runtimeValue.isFinite())
        require(absoluteDifference > tolerance)
        require(tolerance >= 0.0)
    }
}

enum class ResultingNutritionConflictMatchType {
    EXACT,
    NORMALIZATION_EQUIVALENT
}

enum class ResultingNutritionConflictNutrient(
    val aggregateKey: String,
    val runtimeKey: String,
    val absoluteTolerance: Double
) {
    ENERGY(
        aggregateKey = "energyKcalPer100g",
        runtimeKey = "calories",
        absoluteTolerance = 5.0
    ),

    FAT(
        aggregateKey = "fatPer100g",
        runtimeKey = "fat",
        absoluteTolerance = 0.5
    ),

    SATURATED_FAT(
        aggregateKey = "saturatedFatPer100g",
        runtimeKey = "saturatedFat",
        absoluteTolerance = 0.5
    ),

    CARBOHYDRATES(
        aggregateKey = "carbohydratesPer100g",
        runtimeKey = "carbohydrates",
        absoluteTolerance = 0.5
    ),

    SUGARS(
        aggregateKey = "sugarsPer100g",
        runtimeKey = "sugar",
        absoluteTolerance = 0.5
    ),

    FIBER(
        aggregateKey = "fiberPer100g",
        runtimeKey = "fiber",
        absoluteTolerance = 0.5
    ),

    PROTEIN(
        aggregateKey = "proteinsPer100g",
        runtimeKey = "protein",
        absoluteTolerance = 0.5
    ),

    SALT(
        aggregateKey = "saltPer100g",
        runtimeKey = "salt",
        absoluteTolerance = 0.5
    )
}