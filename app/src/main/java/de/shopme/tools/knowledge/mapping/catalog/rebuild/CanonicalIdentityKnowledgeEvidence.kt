package de.shopme.tools.knowledge.mapping.catalog.rebuild

enum class CanonicalIdentityKnowledgeComparison {
    EQUAL,
    DIFFERENT,
    MISSING
}

data class CanonicalIdentityKnowledgeEvidenceDimension(
    val comparison: CanonicalIdentityKnowledgeComparison
)

data class CanonicalIdentityKnowledgeEvidenceRelationship(
    val parent: String,
    val candidate: String,
    val parentNormalized: String,
    val candidateNormalized: String,
    val comparableDimensionCount: Int,
    val equalDimensionCount: Int,
    val differentDimensionCount: Int,
    val missingDimensionCount: Int,
    val evidenceState: String,
    val dimensions:
    Map<String, CanonicalIdentityKnowledgeEvidenceDimension>
)