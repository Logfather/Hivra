package de.shopme.tools.knowledge.catalog.canonical.rebuild.model

data class CanonicalFoodIdentity(
    val itemname: String,
    val normalized: String,
    val category: String,
    val variants: List<String>,
    val sourceVariants: List<String>
)