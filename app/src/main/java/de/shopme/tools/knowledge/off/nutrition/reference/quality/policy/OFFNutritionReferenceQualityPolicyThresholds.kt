package de.shopme.tools.knowledge.off.nutrition.reference.quality.policy

data class OFFNutritionReferenceQualityPolicyThresholds(
    val maximumEnergyKcalPer100g: Double,
    val maximumComponentGramsPer100g: Double,
    val maximumMacronutrientSumGramsPer100g: Double,
    val relationshipToleranceGramsPer100g: Double
) {

    init {
        require(maximumEnergyKcalPer100g > 0.0)
        require(maximumComponentGramsPer100g > 0.0)
        require(maximumMacronutrientSumGramsPer100g > 0.0)
        require(relationshipToleranceGramsPer100g >= 0.0)

        require(maximumEnergyKcalPer100g.isFinite())
        require(maximumComponentGramsPer100g.isFinite())
        require(maximumMacronutrientSumGramsPer100g.isFinite())
        require(relationshipToleranceGramsPer100g.isFinite())
    }
}