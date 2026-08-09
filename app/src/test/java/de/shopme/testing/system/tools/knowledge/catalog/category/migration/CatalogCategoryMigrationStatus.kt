package de.shopme.testing.system.tools.knowledge.catalog.category.migration

enum class CatalogCategoryMigrationStatus {
    ALREADY_CANONICAL,
    MIGRATED_DIRECTLY,
    MIGRATED_BY_PRODUCT_RULE,
    UNRESOLVED
}