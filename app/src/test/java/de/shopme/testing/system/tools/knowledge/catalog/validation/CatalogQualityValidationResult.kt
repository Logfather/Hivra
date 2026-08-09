package de.shopme.testing.system.tools.knowledge.catalog.validation

import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogQualityIssue
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogQualityIssueType

data class CatalogQualityValidationResult(
    val inputEntryCount: Int,
    val validEntryCount: Int,
    val affectedEntryCount: Int,
    val issueCount: Int,
    val errorCount: Int,
    val warningCount: Int,
    val infoCount: Int,
    val uniqueItemNameCount: Int,
    val uniqueNormalizedKeyCount: Int,
    val missingItemNameCount: Int,
    val missingCategoryCount: Int,
    val missingNormalizedKeyCount: Int,
    val duplicateItemNameCount: Int,
    val duplicateNormalizedKeyCount: Int,
    val invalidPluralCount: Int,
    val issueCountsByType: Map<CatalogQualityIssueType, Int>,
    val issueCountsBySeverity: Map<CatalogIssueSeverity, Int>,
    val issueCountsByField: Map<String, Int>,
    val affectedSourceIndices: List<Int>,
    val issues: List<CatalogQualityIssue>,
    val valid: Boolean
) {

    init {
        require(inputEntryCount >= 0) {
            "inputEntryCount must not be negative."
        }

        require(validEntryCount >= 0) {
            "validEntryCount must not be negative."
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

        require(uniqueItemNameCount >= 0) {
            "uniqueItemNameCount must not be negative."
        }

        require(uniqueNormalizedKeyCount >= 0) {
            "uniqueNormalizedKeyCount must not be negative."
        }

        require(missingItemNameCount >= 0) {
            "missingItemNameCount must not be negative."
        }

        require(missingCategoryCount >= 0) {
            "missingCategoryCount must not be negative."
        }

        require(missingNormalizedKeyCount >= 0) {
            "missingNormalizedKeyCount must not be negative."
        }

        require(duplicateItemNameCount >= 0) {
            "duplicateItemNameCount must not be negative."
        }

        require(duplicateNormalizedKeyCount >= 0) {
            "duplicateNormalizedKeyCount must not be negative."
        }

        require(invalidPluralCount >= 0) {
            "invalidPluralCount must not be negative."
        }

        require(validEntryCount <= inputEntryCount) {
            "validEntryCount must not exceed inputEntryCount."
        }

        require(affectedEntryCount <= inputEntryCount) {
            "affectedEntryCount must not exceed inputEntryCount."
        }

        require(validEntryCount + affectedEntryCount == inputEntryCount) {
            "validEntryCount plus affectedEntryCount must equal inputEntryCount."
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

        require(issueCountsBySeverity.values.all { it >= 0 }) {
            "issueCountsBySeverity must not contain negative values."
        }

        require(issueCountsByField.values.all { it >= 0 }) {
            "issueCountsByField must not contain negative values."
        }

        require(issueCountsByType.values.sum() == issueCount) {
            "Sum of issueCountsByType must equal issueCount."
        }

        require(issueCountsBySeverity.values.sum() == issueCount) {
            "Sum of issueCountsBySeverity must equal issueCount."
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
            issueCountsBySeverity.keys.toList() ==
                    issueCountsBySeverity.keys.sortedBy { it.name }
        ) {
            "issueCountsBySeverity must be sorted by enum name."
        }

        require(
            issueCountsByField.keys.toList() ==
                    issueCountsByField.keys.sorted()
        ) {
            "issueCountsByField must be sorted by field name."
        }

        require(
            affectedSourceIndices ==
                    affectedSourceIndices.sorted()
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
                issues.any { issue ->
                    issue.sourceIndex == sourceIndex
                }
            }
        ) {
            "Every affectedSourceIndex must be represented by an issue."
        }

        require(
            issues
                .mapNotNull { it.sourceIndex }
                .distinct()
                .sorted() ==
                    affectedSourceIndices
        ) {
            "affectedSourceIndices must exactly match issue source indices."
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