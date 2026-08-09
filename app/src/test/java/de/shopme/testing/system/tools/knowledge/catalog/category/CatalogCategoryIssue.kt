package de.shopme.testing.system.tools.knowledge.catalog.category

import de.shopme.testing.system.tools.knowledge.catalog.validation.CatalogIssueSeverity

data class CatalogCategoryIssue(
    val type: CatalogCategoryIssueType,
    val severity: CatalogIssueSeverity,
    val sourceIndex: Int?,
    val itemName: String?,
    val originalCategory: String?,
    val normalizedCategory: String?,
    val suggestedCategoryKey: String?,
    val message: String
)