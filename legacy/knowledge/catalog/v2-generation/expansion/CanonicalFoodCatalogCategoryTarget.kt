package de.shopme.testing.system.tools.knowledge.catalog.expansion

data class CanonicalFoodCatalogCategoryTarget(
    val category: String,

    val baselineEntryCount: Int,
    val targetEntryCount: Int,
    val expansionEntryCount: Int,

    val targetShare: Double,
    val expansionShare: Double,

    val status:
    CanonicalFoodCatalogCategoryTargetStatus
) {

    init {
        require(category.isNotBlank())
        require(category == category.trim())
        require(category == category.lowercase()) {
            "Category must use a canonical lowercase key: $category"
        }

        require(baselineEntryCount >= 0)
        require(targetEntryCount > 0)
        require(expansionEntryCount >= 0)

        require(
            expansionEntryCount ==
                    targetEntryCount - baselineEntryCount
        ) {
            "Expansion count is inconsistent for category '$category'."
        }

        require(targetShare in 0.0..1.0)
        require(expansionShare in 0.0..1.0)

        require(
            status ==
                    when {
                        baselineEntryCount < targetEntryCount ->
                            CanonicalFoodCatalogCategoryTargetStatus
                                .EXPANSION_REQUIRED

                        baselineEntryCount == targetEntryCount ->
                            CanonicalFoodCatalogCategoryTargetStatus
                                .TARGET_REACHED

                        else ->
                            CanonicalFoodCatalogCategoryTargetStatus
                                .BASELINE_EXCEEDS_TARGET
                    }
        ) {
            "Category status is inconsistent for '$category'."
        }
    }
}