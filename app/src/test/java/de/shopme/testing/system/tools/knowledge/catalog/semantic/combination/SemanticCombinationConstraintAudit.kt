package de.shopme.testing.system.tools.knowledge.catalog.semantic.combination

import de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility.ProductFamilySemanticProfile

data class SemanticCombinationAuditEntry(
    val itemName: String,
    val normalizedItem: String,
    val category: String,
    val family: String,
    val normalizedFamily: String,
    val profile: ProductFamilySemanticProfile,
    val variantKeys: List<String>,
    val variantTypes: List<String>,
    val decision: SemanticCombinationDecision,
    val reason: SemanticCombinationConstraintReason
)

data class SemanticCombinationReviewGap(
    val profile: ProductFamilySemanticProfile,
    val typeSignature: String,
    val occurrenceCount: Int,
    val exampleItems: List<String>
)

data class SemanticCombinationConstraintAuditReport(
    val schemaVersion: Int,
    val inputFile: String,
    val catalogEntryCount: Int,

    val combinationEntryCount: Int,
    val combinationVariantOccurrenceCount: Int,

    val allowEntryCount: Int,
    val rejectEntryCount: Int,
    val reviewEntryCount: Int,

    val countsByDecision: Map<String, Int>,
    val countsByReason: Map<String, Int>,
    val countsByProfile: Map<String, Int>,

    val rejectedCombinationSignatures: Map<String, Int>,
    val reviewGaps: List<SemanticCombinationReviewGap>,

    val entries: List<SemanticCombinationAuditEntry>
)