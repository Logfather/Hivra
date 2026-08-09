package de.shopme.testing.system.tools.knowledge.catalog.semantic.plausibility

data class MarketPlausibilityReviewGap(
    val family: String,
    val category: String,
    val occurrenceCount: Int,
    val variantKeys: List<String>,
    val exampleItems: List<String>
)

data class MarketPlausibilityAuditReport(
    val schemaVersion: Int,
    val inputFile: String,

    val catalogEntryCount: Int,

    val acceptedEntryCount: Int,
    val rejectedEntryCount: Int,
    val reviewEntryCount: Int,

    val countsByReason: Map<String, Int>,
    val rejectedCountsByCategory: Map<String, Int>,
    val reviewCountsByCategory: Map<String, Int>,

    val reviewGaps: List<MarketPlausibilityReviewGap>,

    val results: List<MarketPlausibilityResult>
)