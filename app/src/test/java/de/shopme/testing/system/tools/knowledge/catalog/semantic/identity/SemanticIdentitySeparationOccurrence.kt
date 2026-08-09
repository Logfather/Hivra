package de.shopme.testing.system.tools.knowledge.catalog.semantic.identity

import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantType

data class SemanticIdentitySeparationOccurrence(
    val itemName: String,
    val normalizedItem: String,
    val category: String,
    val family: String,

    val variantRawValue: String,
    val variantCanonicalKey: String,
    val variantType: SemanticVariantType,

    val identityDecision: SemanticVariantIdentityDecision,
    val identityReason: SemanticVariantIdentityReason,

    val targetKnowledgeDimension: SemanticKnowledgeDimension
)

data class SemanticIdentitySeparationEntry(
    val itemName: String,
    val normalizedItem: String,
    val category: String,
    val family: String,

    val knowledgeOnlyVariantKeys: List<String>,
    val identityVariantKeys: List<String>,
    val reviewVariantKeys: List<String>,

    val hasKnowledgeOnlyVariant: Boolean,
    val hasIdentityVariant: Boolean,
    val hasReviewVariant: Boolean,

    val projectedBaseIdentity: String
)

data class SemanticIdentitySeparationAuditReport(
    val schemaVersion: Int,
    val inputFile: String,

    val catalogEntryCount: Int,
    val variantBearingEntryCount: Int,

    val knowledgeOnlyOccurrenceCount: Int,
    val identityAllowedOccurrenceCount: Int,
    val reviewOccurrenceCount: Int,

    val entriesWithKnowledgeOnlyVariants: Int,
    val pureKnowledgeAttributeEntryCount: Int,
    val mixedIdentityAndKnowledgeEntryCount: Int,

    val countsByType: Map<String, Int>,
    val countsByKnowledgeDimension: Map<String, Int>,

    val projectedBaseIdentityCollisions: Map<String, Int>,

    val entries: List<SemanticIdentitySeparationEntry>,
    val occurrences: List<SemanticIdentitySeparationOccurrence>
)