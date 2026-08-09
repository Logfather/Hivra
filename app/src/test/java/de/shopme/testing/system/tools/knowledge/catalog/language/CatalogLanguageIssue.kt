package de.shopme.testing.system.tools.knowledge.catalog.language

import de.shopme.testing.system.tools.knowledge.catalog.validation.CatalogIssueSeverity

data class CatalogLanguageIssue(
    val type: CatalogLanguageIssueType,
    val severity: CatalogIssueSeverity,
    val sourceIndex: Int,
    val itemName: String,
    val field: String,
    val originalValue: String?,
    val suggestedValue: String?,
    val matchedTerms: List<String>,
    val message: String
) {

    init {
        require(sourceIndex >= 0) {
            "sourceIndex must not be negative."
        }

        require(itemName.isNotBlank()) {
            "itemName must not be blank."
        }

        require(field.isNotBlank()) {
            "field must not be blank."
        }

        require(matchedTerms.none { it.isBlank() }) {
            "matchedTerms must not contain blank values."
        }

        require(matchedTerms.distinct().size == matchedTerms.size) {
            "matchedTerms must not contain duplicates."
        }

        require(matchedTerms == matchedTerms.sorted()) {
            "matchedTerms must be sorted."
        }

        require(message.isNotBlank()) {
            "message must not be blank."
        }
    }
}