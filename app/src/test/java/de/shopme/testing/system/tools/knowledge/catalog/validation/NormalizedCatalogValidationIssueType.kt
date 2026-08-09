package de.shopme.testing.system.tools.knowledge.catalog.validation

enum class NormalizedCatalogValidationIssueType {
    EMPTY_ITEM_NAME,
    MISSING_NORMALIZED_KEY,
    INVALID_NORMALIZED_KEY,
    DUPLICATE_NORMALIZED_KEY,
    MISSING_CATEGORY,
    UNKNOWN_CATEGORY,
    DUPLICATE_ITEM_NAME,
    BLANK_LIST_VALUE,
    DUPLICATE_LIST_VALUE,
    NON_DETERMINISTIC_ORDER
}