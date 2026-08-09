package de.shopme.testing.system.tools.knowledge.catalog.validation

data class NormalizedCatalogValidationIssue(
    val type: NormalizedCatalogValidationIssueType,
    val sourceIndex: Int?,
    val itemName: String?,
    val value: String?,
    val message: String
)