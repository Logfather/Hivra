package de.shopme.tools.knowledge.mapping.catalog.rebuild

data class CanonicalIdentityKnowledgeEvidenceClassificationReport(
    val version: Int,
    val inputRelationshipCount: Int,
    val keepIdentityCount: Int,
    val reviewCount: Int,
    val insufficientEvidenceCount: Int,
    val classifications:
    List<CanonicalIdentityKnowledgeClassification>
)