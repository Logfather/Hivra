package de.shopme.testing.system.tools.knowledge.catalog.normalization

data class CatalogNormalizationResult(
    val sourceIndex: Int,

    val originalItemName: String,
    val originalNormalized: String?,
    val originalPlural: String?,
    val originalColloquial: List<String>,
    val originalPhoneticTokens: List<String>,
    val originalAutocompleteTokens: List<String>,

    val computedCanonicalName: String,
    val computedNormalizedKey: String,
    val computedPlural: String?,
    val normalizedColloquial: List<String>,
    val normalizedPhoneticTokens: List<String>,
    val normalizedAutocompleteTokens: List<String>,

    val changes: List<CatalogNormalizationChange>
) {

    init {
        require(sourceIndex >= 0) {
            "sourceIndex must not be negative."
        }

        require(originalItemName.isNotBlank()) {
            "originalItemName must not be blank."
        }

        require(computedCanonicalName.isNotBlank()) {
            "computedCanonicalName must not be blank."
        }

        require(computedNormalizedKey.isNotBlank()) {
            "computedNormalizedKey must not be blank."
        }

        require(originalColloquial.none(String::isBlank)) {
            "originalColloquial must not contain blank values."
        }

        require(originalPhoneticTokens.none(String::isBlank)) {
            "originalPhoneticTokens must not contain blank values."
        }

        require(originalAutocompleteTokens.none(String::isBlank)) {
            "originalAutocompleteTokens must not contain blank values."
        }

        require(normalizedColloquial.none(String::isBlank)) {
            "normalizedColloquial must not contain blank values."
        }

        require(normalizedPhoneticTokens.none(String::isBlank)) {
            "normalizedPhoneticTokens must not contain blank values."
        }

        require(normalizedAutocompleteTokens.none(String::isBlank)) {
            "normalizedAutocompleteTokens must not contain blank values."
        }

        require(
            normalizedColloquial.distinct().size ==
                    normalizedColloquial.size
        ) {
            "normalizedColloquial must not contain duplicates."
        }

        require(
            normalizedPhoneticTokens.distinct().size ==
                    normalizedPhoneticTokens.size
        ) {
            "normalizedPhoneticTokens must not contain duplicates."
        }

        require(
            normalizedAutocompleteTokens.distinct().size ==
                    normalizedAutocompleteTokens.size
        ) {
            "normalizedAutocompleteTokens must not contain duplicates."
        }

        require(changes.distinct().size == changes.size) {
            "changes must not contain duplicates."
        }
    }
}