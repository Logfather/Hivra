package de.shopme.testing.system.tools.knowledge.catalog.semantic.plausibility.value

data class FamilyVariantValuePlausibilityOccurrence(
    val itemName: String,
    val normalizedItem: String,
    val category: String,
    val family: String,

    val variantDisplayName: String,
    val variantCanonicalKey: String,

    val decision: FamilyVariantValuePlausibilityDecision,
    val reason: FamilyVariantValuePlausibilityReason
)

data class FamilyVariantValuePlausibilityGap(
    val family: String,
    val variantCanonicalKey: String,
    val occurrenceCount: Int,
    val exampleItems: List<String>
)

data class FamilyVariantValuePlausibilityAuditReport(
    val schemaVersion: Int,
    val inputEntryCount: Int,
    val variantBearingEntryCount: Int,
    val variantOccurrenceCount: Int,

    val allowOccurrenceCount: Int,
    val rejectOccurrenceCount: Int,
    val reviewOccurrenceCount: Int,

    val affectedRejectEntryCount: Int,
    val affectedReviewEntryCount: Int,

    val countsByReason: Map<String, Int>,
    val rejectedValues: Map<String, Int>,

    val reviewGaps: List<FamilyVariantValuePlausibilityGap>,
    val occurrences: List<FamilyVariantValuePlausibilityOccurrence>
)