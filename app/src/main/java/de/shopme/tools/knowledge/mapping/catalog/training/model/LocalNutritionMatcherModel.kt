package de.shopme.tools.knowledge.mapping.catalog.training.model

data class LocalNutritionMatcherModel(
    val version: Int,
    val modelType: String =
        "WEIGHTED_LOGISTIC_REGRESSION",
    val datasetType: String =
        "NUTRITION_CATALOG_SERVER_MATCHER",
    val featureNames: List<String>,
    val featureMeans: List<Double>,
    val featureStandardDeviations: List<Double>,
    val coefficients: List<Double>,
    val intercept: Double,
    val decisionThreshold: Double,
    val decisionThresholdOptimization:
    LocalNutritionMatcherThresholdOptimizationMetadata,

    /**
     * Wird ausschließlich aus verfügbaren diagnosticScore-Werten
     * des Trainingssplits berechnet.
     *
     * Beispiele ohne Score verwenden diesen Wert. Dadurch ist ein
     * fehlender Score nach der Standardisierung neutral und verrät
     * nicht mehr die Herkunft des Beispiels.
     */
    val diagnosticScoreImputationValue: Double,

    val training:
    LocalNutritionMatcherTrainingMetadata,
    val metrics:
    LocalNutritionMatcherModelMetrics,
)

data class LocalNutritionMatcherTrainingMetadata(
    val datasetFile: String,
    val datasetVersion: Int,
    val exampleCount: Int,
    val trainingExampleCount: Int,
    val testExampleCount: Int,
    val trainingCatalogKeyCount: Int,
    val testCatalogKeyCount: Int,
    val positiveClassWeight: Double,
    val negativeClassWeight: Double,
    val learningRate: Double,
    val iterationCount: Int,
    val l2Regularization: Double,
    val splitModulo: Int,
    val testBuckets: List<Int>
)

data class LocalNutritionMatcherModelMetrics(
    val training:
    LocalNutritionMatcherClassificationMetrics,
    val test:
    LocalNutritionMatcherClassificationMetrics,
    val trainingByRole:
    List<LocalNutritionMatcherRoleMetrics>,
    val testByRole:
    List<LocalNutritionMatcherRoleMetrics>
)

data class LocalNutritionMatcherClassificationMetrics(
    val exampleCount: Int,
    val positiveCount: Int,
    val negativeCount: Int,
    val truePositive: Int,
    val falsePositive: Int,
    val trueNegative: Int,
    val falseNegative: Int,
    val accuracy: Double,
    val precision: Double,
    val recall: Double,
    val f1: Double,
    val balancedAccuracy: Double,
    val averageLogLoss: Double
)

data class LocalNutritionMatcherRoleMetrics(
    val role: String,
    val exampleCount: Int,
    val positiveCount: Int,
    val negativeCount: Int,
    val predictedPositiveCount: Int,
    val truePositive: Int,
    val falsePositive: Int,
    val trueNegative: Int,
    val falseNegative: Int,
    val precision: Double,
    val recall: Double,
    val falsePositiveRate: Double
)

data class TrainLocalNutritionMatcherModelResult(
    val model: LocalNutritionMatcherModel,
    val outputFile: String
)

data class LocalNutritionMatcherThresholdOptimizationMetadata(
    val calibrationExampleCount: Int,
    val minimumPrecision: Double,
    val maximumFalsePositiveRate: Double,
    val minimumPredictedPositiveCount: Int,
    val policySatisfied: Boolean,
    val evaluatedThresholdCount: Int,
    val selectedPrecision: Double,
    val selectedRecall: Double,
    val selectedFalsePositiveRate: Double,
    val selectedF1: Double,
    val selectedTruePositiveCount: Int,
    val selectedFalsePositiveCount: Int,
    val selectedTrueNegativeCount: Int,
    val selectedFalseNegativeCount: Int,
)