package de.shopme.tools.knowledge.mapping.catalog.training.model

data class NutritionMatcherModelComparisonReport(
    val version: Int = 1,
    val datasetFile: String,
    val baselineFeatureCount: Int,
    val extendedFeatureCount: Int,
    val baseline: NutritionMatcherModelComparisonSnapshot,
    val extended: NutritionMatcherModelComparisonSnapshot,
    val extendedDelta: NutritionMatcherModelMetricDelta,
    val singleFeatureComparisons:
    List<NutritionDomainFeatureComparison>,
    val recommendation:
    NutritionMatcherModelRecommendation,
)

data class NutritionMatcherModelComparisonSnapshot(
    val featureNames: List<String>,
    val trainingExampleCount: Int,
    val testExampleCount: Int,
    val testPrecision: Double,
    val testRecall: Double,
    val testF1: Double,
    val testBalancedAccuracy: Double,
    val testAverageLogLoss: Double,
    val testFalsePositiveCount: Int,
    val testFalseNegativeCount: Int,
    val testByRole:
    List<LocalNutritionMatcherRoleMetrics>,
)

data class NutritionMatcherModelMetricDelta(
    val precision: Double,
    val recall: Double,
    val f1: Double,
    val balancedAccuracy: Double,
    val averageLogLoss: Double,
    val falsePositiveCount: Int,
    val falseNegativeCount: Int,
)

data class NutritionDomainFeatureComparison(
    val featureName: String,
    val featureCount: Int,
    val testPrecision: Double,
    val testRecall: Double,
    val testF1: Double,
    val testBalancedAccuracy: Double,
    val testAverageLogLoss: Double,
    val delta: NutritionMatcherModelMetricDelta,
    val improvesF1: Boolean,
    val improvesBalancedAccuracy: Boolean,
    val improvesBothPrimaryMetrics: Boolean,
    val roleDeltas:
    List<NutritionMatcherRoleMetricDelta>,
)

data class NutritionMatcherRoleMetricDelta(
    val role: String,
    val baselineFalsePositiveRate: Double,
    val candidateFalsePositiveRate: Double,
    val falsePositiveRateDelta: Double,
    val baselineRecall: Double,
    val candidateRecall: Double,
    val recallDelta: Double,
)

data class NutritionMatcherModelRecommendation(
    val recommendedModel:
    NutritionMatcherRecommendedModel,
    val recommendedFeatureNames: List<String>,
    val reason: String,
    val baselineDominatesPrimaryMetrics: Boolean,
    val extendedDominatesPrimaryMetrics: Boolean,
)

enum class NutritionMatcherRecommendedModel {

    BASELINE,

    EXTENDED,
}