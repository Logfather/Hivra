package de.shopme.testing.system.tools.knowledge.catalog.normalization


enum class CatalogNormalizationChangeType {
    TRIMMED_WHITESPACE,
    COLLAPSED_WHITESPACE,
    NORMALIZED_CASE,
    NORMALIZED_HYPHEN,
    NORMALIZED_APOSTROPHE,
    NORMALIZED_UMLAUT,
    REMOVED_DUPLICATE_TOKEN,
    SORTED_TOKEN_LIST,
    RECOMPUTED_NORMALIZED_KEY,
    REPAIRED_PLURAL,
    REMOVED_EMPTY_ALIAS,
    REMOVED_SELF_ALIAS
}
data class CatalogNormalizationChange(
    val type: CatalogNormalizationChangeType,
    val field: String,
    val before: String?,
    val after: String?,
    val reason: String
)