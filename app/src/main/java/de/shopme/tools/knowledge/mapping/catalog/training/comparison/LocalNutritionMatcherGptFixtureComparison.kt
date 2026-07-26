package de.shopme.tools.knowledge.mapping.catalog.training.comparison

data class LocalNutritionMatcherGptFixtureComparison(
    val version: Int = 1,
    val comparisonType: String =
        "LOCAL_NUTRITION_MATCHER_VS_GPT_5_5",
    val datasetFile: String,
    val modelFile: String,
    val testCatalogKeyCount: Int,
    val testExampleCount: Int,
    val positiveFixtureCount: Int,
    val negativeFixtureCount: Int,
    val thresholds:
    List<LocalNutritionMatcherThresholdComparison>,
    val recommendedThreshold:
    LocalNutritionMatcherRecommendedThreshold?,
    val topOne:
    LocalNutritionMatcherTopOneComparison,
    val modelVersion: Int,
    val modelType: String,
    val featureNames: List<String>,
    val featureCount: Int,
    val productionDecisionThreshold: Double,
    val thresholdMinimumPrecision: Double,
    val thresholdMaximumFalsePositiveRate: Double,
    val thresholdMinimumPredictedPositiveCount: Int,
    val thresholdPolicySatisfied: Boolean,
    val productionThreshold:
    LocalNutritionMatcherThresholdComparison,
    val historicalTopOneAccuracy: Double,
    val historicalMeanPositiveRank: Double,
    val topOneAccuracyDelta: Double,
    val meanPositiveRankDelta: Double,
)

data class LocalNutritionMatcherThresholdComparison(
    val threshold: Double,
    val exampleCount: Int,
    val positiveCount: Int,
    val negativeCount: Int,

    val truePositive: Int,
    val falsePositive: Int,
    val trueNegative: Int,
    val falseNegative: Int,

    val precision: Double,
    val recall: Double,
    val f1: Double,
    val specificity: Double,
    val balancedAccuracy: Double,

    val predictedPositiveCount: Int,
    val predictedNegativeCount: Int,

    val acceptedOriginalMatchRecall: Double,
    val acceptedRepresentativeRecall: Double,
    val rejectedSelectedFalsePositiveRate: Double,
    val noMatchFalsePositiveRate: Double,
    val alternativeFalsePositiveRate: Double
)

data class LocalNutritionMatcherRecommendedThreshold(
    val threshold: Double,
    val minimumPrecision: Double,
    val precision: Double,
    val recall: Double,
    val f1: Double,
    val truePositive: Int,
    val falsePositive: Int,
    val falseNegative: Int
)

data class LocalNutritionMatcherTopOneComparison(
    val eligibleCatalogKeyCount: Int,
    val correctCatalogKeyCount: Int,
    val accuracy: Double,
    val meanPositiveRank: Double,
    val positiveAtRankOneCount: Int
)

data class CompareLocalNutritionMatcherAgainstGptFixturesResult(
    val comparison:
    LocalNutritionMatcherGptFixtureComparison,
    val outputFile: String
)