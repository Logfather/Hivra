package de.shopme.testing.system.tools.knowledge.catalog.canonicalization

enum class CatalogCanonicalizationAction {
    KEEP,
    NORMALIZE,
    RENAME,
    MERGE,
    MOVE_CATEGORY,
    REMOVE_NON_FOOD,
    SPLIT,
    REVIEW
}