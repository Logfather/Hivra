package de.shopme.testing.system.tools.knowledge.catalog.expansion.refinement

data class CanonicalCatalogCategoryScope(
    val category: String,

    val baselineEntryCount: Int,
    val proposedTargetEntryCount: Int,
    val refinedTargetEntryCount: Int,

    val allowedIdentityAxes:
    List<CanonicalCatalogIdentityAxis>,

    val excludedSkuAxes:
    List<CanonicalCatalogExcludedSkuAxis>,

    val scopeRationale: String,

    val targetChanged: Boolean
) {

    init {
        require(category.isNotBlank())
        require(category == category.trim())
        require(category == category.lowercase()) {
            "Category must use a canonical lowercase key: $category"
        }

        require(baselineEntryCount >= 0)
        require(proposedTargetEntryCount > 0)
        require(refinedTargetEntryCount >= baselineEntryCount)

        require(
            targetChanged ==
                    (
                            proposedTargetEntryCount !=
                                    refinedTargetEntryCount
                            )
        )

        require(allowedIdentityAxes.isNotEmpty())

        require(
            allowedIdentityAxes ==
                    allowedIdentityAxes
                        .distinct()
                        .sortedBy { it.name }
        ) {
            "allowedIdentityAxes must be unique and sorted."
        }

        require(excludedSkuAxes.isNotEmpty())

        require(
            excludedSkuAxes ==
                    excludedSkuAxes
                        .distinct()
                        .sortedBy { it.name }
        ) {
            "excludedSkuAxes must be unique and sorted."
        }

        require(scopeRationale.isNotBlank())
        require(scopeRationale == scopeRationale.trim())
    }
}