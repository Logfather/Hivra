package de.shopme.testing.system.tools.knowledge.catalog.removal

enum class CatalogSemanticTypoRemovalReason {
    INCORRECT_MERGE_TARGET,
    SEMANTICALLY_DISTINCT_PRODUCT_PAIR,
    AMBIGUOUS_GENERIC_PRODUCT,
    REDUNDANT_LEGACY_ENTRY,
    UNSAFE_CANONICAL_IDENTITY
}