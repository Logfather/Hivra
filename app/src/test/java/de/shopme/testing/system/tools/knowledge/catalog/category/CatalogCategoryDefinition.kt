package de.shopme.testing.system.tools.knowledge.catalog.category

data class CatalogCategoryDefinition(
    val key: String,
    val displayName: String,
    val parentKey: String?,
    val allowedChildKeys: Set<String>,
    val foodOnly: Boolean
)