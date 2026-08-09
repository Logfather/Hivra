package de.shopme.testing.system.tools.knowledge.catalog.category.migration

data class CatalogCategoryMigrationResult(
    val originalCategory: String?,
    val resultingCategory: String?,
    val status: CatalogCategoryMigrationStatus,
    val ruleId: String?,
    val confidence: Double
) {

    init {
        require(confidence.isFinite()) {
            "confidence must be finite."
        }

        require(confidence in 0.0..1.0) {
            "confidence must be between 0.0 and 1.0."
        }

        require(
            status != CatalogCategoryMigrationStatus.ALREADY_CANONICAL ||
                    originalCategory == resultingCategory
        ) {
            "ALREADY_CANONICAL must preserve the original category."
        }

        require(
            status != CatalogCategoryMigrationStatus.UNRESOLVED ||
                    resultingCategory == null
        ) {
            "UNRESOLVED must not contain resultingCategory."
        }

        require(
            status == CatalogCategoryMigrationStatus.ALREADY_CANONICAL ||
                    status == CatalogCategoryMigrationStatus.UNRESOLVED ||
                    !ruleId.isNullOrBlank()
        ) {
            "A migrated category must contain ruleId."
        }
    }
}