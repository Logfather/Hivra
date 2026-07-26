package de.shopme.tools.knowledge.mapping.catalog.training.model

data class NutritionFeatureOptimizationEvaluation(
    val version: Int,
    val baseline: NutritionFeatureOptimizationBaselineEvaluation,
    val classificationCounts:
    NutritionFeatureOptimizationClassificationCounts,
    val requiredFeatures:
    List<NutritionFeatureOptimizationFeatureEvaluation>,
    val neutralFeatures:
    List<NutritionFeatureOptimizationFeatureEvaluation>,
    val harmfulFeatures:
    List<NutritionFeatureOptimizationFeatureEvaluation>,
    val recommendedFeatureNames: List<String>,
    val recommendedFeatureCount: Int,
    val removedFeatureNames: List<String>,
    val removedFeatureCount: Int,
)

data class NutritionFeatureOptimizationBaselineEvaluation(
    val featureNames: List<String>,
    val featureCount: Int,
    val testPrecision: Double,
    val testRecall: Double,
    val testF1: Double,
    val testBalancedAccuracy: Double,
)

data class NutritionFeatureOptimizationClassificationCounts(
    val required: Int,
    val neutral: Int,
    val harmful: Int,
)

data class NutritionFeatureOptimizationFeatureEvaluation(
    val featureName: String,
    val classification:
    NutritionFeatureOptimizationClassification,
    val precisionDelta: Double,
    val recallDelta: Double,
    val f1Delta: Double,
    val balancedAccuracyDelta: Double,
)