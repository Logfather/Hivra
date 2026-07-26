package de.shopme.tools.knowledge.ciqual.model

data class CiqualNutritionRecord(
    val foodCode: String,
    val frenchName: String,
    val englishName: String?,
    val groupCode: String?,
    val nutrition: CiqualNutritionValues,
    val constituentValues: Map<String, Double>
)

data class CiqualNutritionValues(
    val energyKcalPer100g: Double?,
    val fatPer100g: Double?,
    val saturatedFatPer100g: Double?,
    val carbohydratesPer100g: Double?,
    val sugarsPer100g: Double?,
    val fiberPer100g: Double?,
    val proteinsPer100g: Double?,
    val saltPer100g: Double?
) {

    fun hasAnyValue(): Boolean =
        listOf(
            energyKcalPer100g,
            fatPer100g,
            saturatedFatPer100g,
            carbohydratesPer100g,
            sugarsPer100g,
            fiberPer100g,
            proteinsPer100g,
            saltPer100g
        ).any { it != null }
}