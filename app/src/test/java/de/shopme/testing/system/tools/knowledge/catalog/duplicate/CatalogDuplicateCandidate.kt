package de.shopme.testing.system.tools.knowledge.catalog.duplicate

data class CatalogDuplicateCandidate(
    val sourceIndex: Int,
    val itemName: String,
    val normalizedKey: String,
    val matchScore: Double,
    val reasons: Set<CatalogDuplicateReason>
)