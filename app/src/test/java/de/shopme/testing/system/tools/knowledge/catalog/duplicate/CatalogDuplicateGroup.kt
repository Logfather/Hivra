package de.shopme.testing.system.tools.knowledge.catalog.duplicate

data class CatalogDuplicateGroup(
    val groupId: String,
    val canonicalCandidateSourceIndex: Int,
    val canonicalCandidateName: String,
    val members: List<CatalogDuplicateCandidate>,
    val confidence: Double,
    val recommendation: CatalogDuplicateRecommendation
)