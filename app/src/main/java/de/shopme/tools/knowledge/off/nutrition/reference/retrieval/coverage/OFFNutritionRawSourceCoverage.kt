package de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage

data class OFFNutritionRawSourceCoverage(
    val scannedProductCount: Long,
    val entriesByCatalogIndex:
    Map<Int, OFFNutritionRawSourceCoverageEntry>
) {

    init {
        require(scannedProductCount >= 0)
    }
}

data class OFFNutritionRawSourceCoverageEntry(
    val productMatchCount: Int,
    val productWithAnyNutritionCount: Int,
    val productWithUsableNutritionCount: Int,
    val matchedProductNames: List<String>,
    val matchedProductIds: List<String>,
    val matchedProductWithUsableNutritionIds: List<String>
) {

    init {
        require(productMatchCount >= 0)
        require(productWithAnyNutritionCount >= 0)
        require(productWithUsableNutritionCount >= 0)

        require(
            productWithAnyNutritionCount <=
                    productMatchCount
        )

        require(
            productWithUsableNutritionCount <=
                    productWithAnyNutritionCount
        )

        require(
            matchedProductNames ==
                    matchedProductNames
                        .distinct()
                        .sorted()
        )
    }
}