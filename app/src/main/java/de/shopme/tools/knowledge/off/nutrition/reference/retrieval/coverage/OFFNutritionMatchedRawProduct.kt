package de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage

data class OFFNutritionMatchedRawProduct(
    val sourceProductId: String,
    val productName: String,
    val hasAnyNutrition: Boolean,
    val hasUsableNutrition: Boolean
) {

    init {
        require(sourceProductId.isNotBlank()) {
            "sourceProductId must not be blank."
        }

        require(productName.isNotBlank()) {
            "productName must not be blank."
        }

        require(
            !hasUsableNutrition ||
                    hasAnyNutrition
        ) {
            "A product with usable nutrition must also have nutrition."
        }
    }
}