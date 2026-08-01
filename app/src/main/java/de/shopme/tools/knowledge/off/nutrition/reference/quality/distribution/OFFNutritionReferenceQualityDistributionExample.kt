package de.shopme.tools.knowledge.off.nutrition.reference.quality.distribution

data class OFFNutritionReferenceQualityDistributionExample(
    val sourceId: String,
    val canonicalId: String,
    val productName: String?,
    val nutrition: Map<String, Double>,
    val reasons: List<String>
)