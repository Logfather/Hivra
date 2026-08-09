package de.shopme.testing.system.tools.knowledge.catalog.report

data class CatalogQualityReport(
    val version: Int,
    val generatedAt: String?,
    val inputFile: String,
    val totalEntryCount: Int,
    val uniqueItemNameCount: Int,
    val uniqueNormalizedKeyCount: Int,
    val missingNormalizedKeyCount: Int,
    val duplicateNormalizedKeyCount: Int,
    val duplicateItemNameCount: Int,
    val missingCategoryCount: Int,
    val invalidPluralCount: Int,
    val languageIssueCount: Int,
    val nonFoodCandidateCount: Int,
    val canonicalizationActionCounts: Map<String, Int>,
    val issueCountsBySeverity: Map<String, Int>,
    val valid: Boolean
)