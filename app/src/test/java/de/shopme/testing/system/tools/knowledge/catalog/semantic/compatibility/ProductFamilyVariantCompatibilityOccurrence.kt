package de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility

import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantType

data class ProductFamilyVariantCompatibilityOccurrence(
    val itemName: String,
    val normalizedItem: String,
    val category: String,
    val family: String,
    val normalizedFamily: String,
    val variantRawValue: String,
    val variantCanonicalKey: String,
    val variantType: SemanticVariantType,
    val decision: ProductFamilyVariantCompatibilityDecision,
    val reason: ProductFamilyVariantCompatibilityReason
)

data class ProductFamilyVariantCompatibilityGap(
    val family: String,
    val normalizedFamily: String,
    val category: String,
    val occurrenceCount: Int,
    val variantKeys: List<String>,
    val exampleItems: List<String>
)

data class ProductFamilyVariantCompatibilityAuditReport(
    val schemaVersion: Int,
    val inputFile: String,

    val catalogEntryCount: Int,
    val variantBearingEntryCount: Int,
    val variantOccurrenceCount: Int,

    val allowOccurrenceCount: Int,
    val rejectOccurrenceCount: Int,
    val reviewOccurrenceCount: Int,

    val allowedEntryCount: Int,
    val rejectedEntryCount: Int,
    val reviewEntryCount: Int,

    val countsByDecision: Map<String, Int>,
    val countsByReason: Map<String, Int>,
    val rejectedCountsByFamily: Map<String, Int>,
    val rejectedCountsByVariant: Map<String, Int>,

    val reviewGaps: List<ProductFamilyVariantCompatibilityGap>,
    val occurrences: List<ProductFamilyVariantCompatibilityOccurrence>
)