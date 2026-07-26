package de.shopme.tools.knowledge.off.nutrition.reference.retrieval

data class CatalogOFFNutritionRetrievalItem(
    val catalogIndex: Int,
    val catalogKey: String,
    val normalizedEnglish: String,
    val itemName: String,
    val category: String?,
    val production: String?,
    val retrievalTerms: List<String>
) {

    init {
        require(catalogIndex >= 0) {
            "Catalog index must not be negative."
        }

        require(catalogKey.isNotBlank()) {
            "Catalog key must not be blank."
        }

        require(normalizedEnglish.isNotBlank()) {
            "normalizedEnglish must not be blank."
        }

        require(itemName.isNotBlank()) {
            "Catalog item name must not be blank."
        }

        require(retrievalTerms.isNotEmpty()) {
            "Catalog retrieval item must contain retrieval terms."
        }

        require(
            retrievalTerms ==
                    retrievalTerms
                        .distinct()
                        .sorted()
        ) {
            "Catalog retrieval terms must be unique and sorted."
        }

        require(
            retrievalTerms.all(String::isNotBlank)
        ) {
            "Catalog retrieval terms must not contain blank strings."
        }
    }
}