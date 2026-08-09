package de.shopme.testing.system.tools.knowledge.catalog.semantic.variant

data class SemanticVariantOccurrence(
    val rawValue: String,
    val normalizedLookupKey: String,
    val canonicalKey: String?,
    val type: SemanticVariantType?,
    val itemName: String,
    val normalizedItem: String,
    val category: String
)

data class UnknownSemanticVariant(
    val rawValue: String,
    val normalizedLookupKey: String,
    val occurrenceCount: Int,
    val categories: List<String>,
    val exampleItems: List<String>
)

data class SemanticVariantTaxonomyAuditReport(
    val schemaVersion: Int,
    val inputFile: String,
    val catalogEntryCount: Int,
    val variantBearingEntryCount: Int,
    val variantOccurrenceCount: Int,
    val distinctVariantCount: Int,
    val classifiedDistinctVariantCount: Int,
    val unknownDistinctVariantCount: Int,
    val classificationCoverage: Double,
    val countsByType: Map<String, Int>,
    val unknownVariants: List<UnknownSemanticVariant>
)