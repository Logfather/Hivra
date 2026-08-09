package de.shopme.testing.system.tools.knowledge.catalog.nonfood

data class CatalogNonFoodCandidate(
    val sourceIndex: Int,
    val itemName: String,
    val category: String?,
    val reasons: Set<CatalogNonFoodReason>,
    val matchedTerms: Set<String>,
    val confidence: Double,
    val recommendation: NonFoodRecommendation
)