package de.shopme.tools.knowledge.off.nutrition.reference.aggregation

data class OFFNutritionReferenceNutrientStatistics(
    val observationCount: Int,
    val minimum: Double,
    val median: Double,
    val maximum: Double
) {

    init {
        require(observationCount > 0) {
            "Nutrient statistics observationCount must be positive."
        }

        require(minimum.isFinite()) {
            "Nutrient statistics minimum must be finite."
        }

        require(median.isFinite()) {
            "Nutrient statistics median must be finite."
        }

        require(maximum.isFinite()) {
            "Nutrient statistics maximum must be finite."
        }

        require(minimum <= median) {
            "Nutrient statistics minimum must not exceed median."
        }

        require(median <= maximum) {
            "Nutrient statistics median must not exceed maximum."
        }
    }
}