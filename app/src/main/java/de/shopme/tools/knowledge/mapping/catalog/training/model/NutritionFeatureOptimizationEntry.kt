package de.shopme.tools.knowledge.mapping.catalog.training.model

data class NutritionFeatureOptimizationEntry(
    val featureName: String,
    val baselineFeatureCount: Int,
    val ablatedFeatureCount: Int,
    val baselineTestPrecision: Double,
    val ablatedTestPrecision: Double,
    val precisionDelta: Double,
    val baselineTestRecall: Double,
    val ablatedTestRecall: Double,
    val recallDelta: Double,
    val baselineTestF1: Double,
    val ablatedTestF1: Double,
    val f1Delta: Double,
    val baselineTestBalancedAccuracy: Double,
    val ablatedTestBalancedAccuracy: Double,
    val balancedAccuracyDelta: Double,
    val classification: NutritionFeatureOptimizationClassification,
)