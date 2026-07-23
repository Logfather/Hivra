package de.shopme.tools.knowledge.mapping.catalog.training.model

data class NutritionDomainFeatureImpact(
    val featureName: String,
    val deltaPrecision: Double,
    val deltaRecall: Double,
    val deltaF1: Double,
    val deltaBalancedAccuracy: Double,
    val deltaFalsePositiveCount: Int,
    val deltaFalseNegativeCount: Int,
    val classification:
    NutritionDomainFeatureImpactClassification,
)