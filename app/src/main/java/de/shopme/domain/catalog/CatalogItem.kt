package de.shopme.domain.catalog

import de.shopme.domain.catalog.model.KnowledgeReferences

data class CatalogItem(

    val itemname: String,
    val category: String,
    val production: String,
    val normalized: String,
    val plural: String,
    val colloquial: List<String>,
    val phonetic_tokens: List<String>,
    val autocomplete_tokens: List<String>,
    val nutritionReference: String? = null,
    val knowledge: KnowledgeReferences? = KnowledgeReferences(),
    val taxonomyPaths: List<List<String>> = emptyList()

) {

    fun nutritionKnowledgeReference(): String? =

        knowledge
            ?.nutrition
            ?.reference
            ?.takeIf {
                it.isNotBlank()
            }

            ?: nutritionReference
                ?.takeIf {
                    it.isNotBlank()
                }

    fun nutritionKnowledgeSource(): String? =

        knowledge
            ?.nutrition
            ?.source
            ?.takeIf {
                it.isNotBlank()
            }

    fun allergenKnowledgeSource(): String? =

        knowledge
            ?.allergens
            ?.source
            ?.takeIf {
                it.isNotBlank()
            }

    fun ingredientKnowledgeSource(): String? =

        knowledge
            ?.ingredients
            ?.source
            ?.takeIf {
                it.isNotBlank()
            }


}