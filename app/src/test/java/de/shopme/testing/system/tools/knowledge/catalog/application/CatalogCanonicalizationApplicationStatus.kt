package de.shopme.testing.system.tools.knowledge.catalog.application

enum class CatalogCanonicalizationApplicationStatus {
    APPLIED,
    SKIPPED_REVIEW_REQUIRED,
    REMOVED,
    MERGED_INTO_TARGET,
    FAILED
}