package de.shopme.testing.system.tools.knowledge.catalog.language

import de.shopme.testing.system.tools.knowledge.catalog.validation.CatalogIssueSeverity

data class CatalogLanguageValidationResult(
    val inputEntryCount: Int,
    val affectedEntryCount: Int,
    val issueCount: Int,
    val errorCount: Int,
    val warningCount: Int,
    val infoCount: Int,
    val issueCountsByType: Map<CatalogLanguageIssueType, Int>,
    val issueCountsByField: Map<String, Int>,
    val affectedSourceIndices: List<Int>,
    val issues: List<CatalogLanguageIssue>,
    val valid: Boolean
) {

    init {
        require(inputEntryCount >= 0) {
            "inputEntryCount must not be negative."
        }

        require(affectedEntryCount >= 0) {
            "affectedEntryCount must not be negative."
        }

        require(issueCount >= 0) {
            "issueCount must not be negative."
        }

        require(errorCount >= 0) {
            "errorCount must not be negative."
        }

        require(warningCount >= 0) {
            "warningCount must not be negative."
        }

        require(infoCount >= 0) {
            "infoCount must not be negative."
        }

        require(affectedEntryCount <= inputEntryCount) {
            "affectedEntryCount must not exceed inputEntryCount."
        }

        require(issueCount == issues.size) {
            "issueCount must equal issues size."
        }

        require(errorCount + warningCount + infoCount == issueCount) {
            "Severity counts must sum to issueCount."
        }

        require(issueCountsByType.values.all { it >= 0 }) {
            "issueCountsByType must not contain negative values."
        }

        require(issueCountsByField.values.all { it >= 0 }) {
            "issueCountsByField must not contain negative values."
        }

        require(issueCountsByType.values.sum() == issueCount) {
            "Sum of issueCountsByType must equal issueCount."
        }

        require(issueCountsByField.values.sum() == issueCount) {
            "Sum of issueCountsByField must equal issueCount."
        }

        require(
            issueCountsByType.keys.toList() ==
                    issueCountsByType.keys.sortedBy { it.name }
        ) {
            "issueCountsByType must be sorted by enum name."
        }

        require(
            issueCountsByField.keys.toList() ==
                    issueCountsByField.keys.sorted()
        ) {
            "issueCountsByField must be sorted by field name."
        }

        require(
            affectedSourceIndices == affectedSourceIndices.sorted()
        ) {
            "affectedSourceIndices must be sorted."
        }

        require(
            affectedSourceIndices.distinct().size ==
                    affectedSourceIndices.size
        ) {
            "affectedSourceIndices must not contain duplicates."
        }

        require(affectedEntryCount == affectedSourceIndices.size) {
            "affectedEntryCount must equal affectedSourceIndices size."
        }

        require(
            affectedSourceIndices.all { sourceIndex ->
                issues.any { it.sourceIndex == sourceIndex }
            }
        ) {
            "Every affected source index must be represented by an issue."
        }

        require(
            valid == issues.none {
                it.severity == CatalogIssueSeverity.ERROR
            }
        ) {
            "valid must be true exactly when no ERROR issue exists."
        }
    }
}