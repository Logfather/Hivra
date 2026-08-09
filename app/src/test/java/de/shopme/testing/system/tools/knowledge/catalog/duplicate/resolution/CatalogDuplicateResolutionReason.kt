package de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution

enum class CatalogDuplicateResolutionReason {
    IDENTICAL_NORMALIZED_KEY,
    IDENTICAL_CANONICAL_NAME_AND_CATEGORY,
    DETERMINISTIC_SINGULAR_PLURAL_VARIANT,
    DETERMINISTIC_TYPO_VARIANT
}