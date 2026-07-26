package de.shopme.tools.knowledge.mapping.catalog.training.model

data class NutritionFeatureOptimizationReport(
    val version: Int,
    val baselineFeatureNames: List<String>,
    val baselineFeatureCount: Int,
    val baselineTestPrecision: Double,
    val baselineTestRecall: Double,
    val baselineTestF1: Double,
    val baselineTestBalancedAccuracy: Double,
    val requiredFeatureNames: List<String>,
    val neutralFeatureNames: List<String>,
    val harmfulFeatureNames: List<String>,
    val recommendedFeatureNames: List<String>,
    val entries: List<NutritionFeatureOptimizationEntry>,
)