package de.shopme.tools.knowledge.nutrition

data class NutritionFacts(

    val calories: Double,

    val protein: Double,

    val fat: Double,

    val saturatedFat: Double,

    val carbohydrates: Double,

    val sugar: Double,

    val fiber: Double,

    val salt: Double,

    /*
     * Gibt an, welche Werte in der ursprünglichen Nutrition-Quelle
     * tatsächlich vorhanden waren.
     *
     * Bestehende Konstruktoraufrufe ohne presentNutrients repräsentieren
     * vollständige NutritionFacts und bleiben dadurch kompatibel.
     */
    val presentNutrients: Set<String> =
        ALL_NUTRIENT_KEYS
) {

    fun isPresent(
        nutrientKey: String
    ): Boolean =
        nutrientKey in
                presentNutrients

    companion object {

        const val CALORIES =
            "calories"

        const val PROTEIN =
            "protein"

        const val FAT =
            "fat"

        const val SATURATED_FAT =
            "saturatedFat"

        const val CARBOHYDRATES =
            "carbohydrates"

        const val SUGAR =
            "sugar"

        const val FIBER =
            "fiber"

        const val SALT =
            "salt"

        val ALL_NUTRIENT_KEYS:
                Set<String> =
            sortedSetOf(
                CALORIES,
                PROTEIN,
                FAT,
                SATURATED_FAT,
                CARBOHYDRATES,
                SUGAR,
                FIBER,
                SALT
            )
    }
}