package de.shopme.tools.knowledge.off.nutrition.reference.quality.distribution

data class OFFNutritionValueRange(
    val observationCount: Long,
    val minimum: Double?,
    val maximum: Double?
) {

    init {
        require(observationCount >= 0L)

        if (observationCount == 0L) {
            require(minimum == null)
            require(maximum == null)
        } else {
            require(minimum != null)
            require(maximum != null)
            require(minimum <= maximum)
        }
    }
}