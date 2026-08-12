package de.shopme.tools.knowledge.catalog.canonical.rebuild.model

data class LegacyCanonicalFoodItem(
    val itemname: String,
    val category: String,
    val production: String?,
    val normalized: String,
    val plural: String?,
    val colloquial: List<String>,
    val phoneticTokens: List<String>,
    val autocompleteTokens: List<String>,
    val normalizedEnglish: String?
)