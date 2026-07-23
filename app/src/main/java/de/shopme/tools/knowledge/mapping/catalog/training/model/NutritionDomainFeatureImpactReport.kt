package de.shopme.tools.knowledge.mapping.catalog.training.model

data class NutritionDomainFeatureImpactReport(
    val version: Int,
    val sourceComparisonReportVersion: Int,
    val domainFeatureCount: Int,
    val harmfulCount: Int,
    val neutralCount: Int,
    val beneficialCount: Int,
    val harmfulFeatureNames: List<String>,
    val neutralFeatureNames: List<String>,
    val beneficialFeatureNames: List<String>,
    val impacts: List<NutritionDomainFeatureImpact>,
)