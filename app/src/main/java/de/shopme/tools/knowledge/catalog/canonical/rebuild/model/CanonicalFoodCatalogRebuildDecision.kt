package de.shopme.tools.knowledge.catalog.canonical.rebuild.model

enum class CanonicalFoodCatalogRebuildAction {

    KEEP_AS_CANONICAL,

    MERGE_AS_SOURCE_VARIANT,

    MERGE_AS_VARIANT,

    REJECT_SYNTHETIC,

    REJECT_BRAND,

    REPLACE_WITH_BASE_IDENTITY
}

data class CanonicalFoodCatalogRebuildDecision(
    val sourceItemname: String,
    val sourceNormalized: String,
    val sourceCategory: String,
    val action: CanonicalFoodCatalogRebuildAction,
    val targetNormalized: String?,
    val reason: String
)