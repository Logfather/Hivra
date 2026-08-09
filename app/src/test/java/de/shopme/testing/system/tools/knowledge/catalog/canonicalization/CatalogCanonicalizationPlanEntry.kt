package de.shopme.testing.system.tools.knowledge.catalog.canonicalization

data class CatalogCanonicalizationPlanEntry(
    val sourceIndex: Int,
    val originalItemName: String,
    val action: CatalogCanonicalizationAction,
    val proposedCanonicalName: String?,
    val proposedNormalizedKey: String?,
    val proposedCategory: String?,
    val mergeTargetSourceIndex: Int?,
    val reasons: List<String>,
    val confidence: Double,
    val automatic: Boolean
)