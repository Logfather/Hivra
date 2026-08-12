package de.shopme.tools.knowledge.catalog.canonical.completeness.model

enum class CanonicalFoodGapEvidence {

    TAXONOMY,

    SOURCE,

    TAXONOMY_AND_SOURCE
}

data class CanonicalFoodCatalogGap(
    val proposedItemname: String,
    val proposedNormalized: String,
    val proposedCategory: String,
    val evidence: CanonicalFoodGapEvidence,
    val taxonomyRequired: Boolean,
    val sourceEvidenceCount: Int,
    val sourceExamples: List<String>
)

data class CanonicalFoodCompletenessAuditResult(
    val version: Int,
    val catalogEntryCount: Int,
    val taxonomyRequiredIdentityCount: Int,
    val taxonomyMissingIdentityCount: Int,
    val sourceIdentityRecordCount: Int,
    val sourceGapCandidateCount: Int,
    val combinedGapCount: Int,
    val gaps: List<CanonicalFoodCatalogGap>
)