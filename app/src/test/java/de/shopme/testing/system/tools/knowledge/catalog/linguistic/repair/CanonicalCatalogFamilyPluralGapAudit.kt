package de.shopme.testing.system.tools.knowledge.catalog.linguistic.repair

data class CanonicalCatalogFamilyPluralGap(
    val family: String,
    val normalizedFamily: String,
    val occurrenceCount: Int,
    val exampleItems: List<String>
)

data class CanonicalCatalogFamilyPluralGapAuditReport(
    val schemaVersion: Int,
    val inputEntryCount: Int,
    val variantEntryCount: Int,
    val familyPluralResolvedCount: Int,
    val familyPluralFallbackCount: Int,
    val distinctFallbackFamilyCount: Int,
    val gaps: List<CanonicalCatalogFamilyPluralGap>
)