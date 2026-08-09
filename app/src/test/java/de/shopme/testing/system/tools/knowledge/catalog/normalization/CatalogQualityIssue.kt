package de.shopme.testing.system.tools.knowledge.catalog.normalization

import de.shopme.testing.system.tools.knowledge.catalog.validation.CatalogIssueSeverity


enum class CatalogQualityIssueType {
    EMPTY_ITEM_NAME,
    MISSING_CATEGORY,
    EMPTY_CATEGORY,
    MISSING_NORMALIZED_KEY,
    INVALID_NORMALIZED_KEY,
    DUPLICATE_NORMALIZED_KEY,
    EMPTY_ALIAS,
    DUPLICATE_ALIAS,
    SELF_ALIAS,
    INVALID_PLURAL,
    EMPTY_AUTOCOMPLETE_TOKEN,
    DUPLICATE_AUTOCOMPLETE_TOKEN,
    EMPTY_PHONETIC_TOKEN,
    DUPLICATE_PHONETIC_TOKEN,
    INVALID_ENGLISH_NAME,
    UNKNOWN_FIELD,
    INVALID_FIELD_TYPE
}

data class CatalogQualityIssue(
    val sourceIndex: Int?,
    val itemName: String?,
    val type: CatalogQualityIssueType,
    val severity: CatalogIssueSeverity,
    val field: String?,
    val message: String
)