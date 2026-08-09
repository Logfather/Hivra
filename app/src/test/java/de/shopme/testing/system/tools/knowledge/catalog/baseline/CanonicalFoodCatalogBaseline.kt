package de.shopme.testing.system.tools.knowledge.catalog.baseline

data class CanonicalFoodCatalogBaseline(
    val version: Int,

    /**
     * Stabil aus den fachlich relevanten Baseline-Daten abgeleitete ID.
     *
     * Format:
     * canonical-food-catalog-v1-<erste 16 Zeichen des Katalog-SHA-256>
     */
    val baselineId: String,

    val catalogArtifact:
    CatalogBaselineArtifactReference,

    val normalizedCatalogEntryCount: Int,
    val canonicalCategoryCount: Int,

    val categoryCounts: Map<String, Int>,

    val applicationInputEntryCount: Int,
    val applicationOutputEntryCount: Int,

    val duplicateMergedEntryCount: Int,
    val semanticTypoRemovedEntryCount: Int,
    val finalOutputEntryCount: Int,

    val validationIssueCount: Int,
    val normalizedCatalogValid: Boolean,
    val pipelineValid: Boolean,

    val supportingArtifacts:
    Map<String, CatalogBaselineArtifactReference>,

    val valid: Boolean
) {

    init {
        require(version > 0)

        require(
            BASELINE_ID_REGEX.matches(baselineId)
        ) {
            "Invalid canonical catalog baselineId: $baselineId"
        }

        require(normalizedCatalogEntryCount > 0)
        require(canonicalCategoryCount > 0)

        require(applicationInputEntryCount > 0)
        require(applicationOutputEntryCount > 0)

        require(duplicateMergedEntryCount >= 0)
        require(semanticTypoRemovedEntryCount >= 0)
        require(finalOutputEntryCount > 0)

        require(validationIssueCount >= 0)

        require(
            categoryCounts.keys.all(String::isNotBlank)
        )

        require(
            categoryCounts.values.all { it > 0 }
        )

        require(
            categoryCounts ==
                    categoryCounts.toSortedMap()
        ) {
            "categoryCounts must be sorted by category key."
        }

        require(
            categoryCounts.values.sum() ==
                    finalOutputEntryCount
        ) {
            "Category counts must cover every final catalog entry."
        }

        require(
            normalizedCatalogEntryCount ==
                    finalOutputEntryCount
        ) {
            "Normalized catalog count must equal final output count."
        }

        require(
            canonicalCategoryCount ==
                    categoryCounts.size
        ) {
            "canonicalCategoryCount must equal categoryCounts size."
        }

        require(
            applicationOutputEntryCount -
                    semanticTypoRemovedEntryCount ==
                    finalOutputEntryCount
        ) {
            "Final output count is inconsistent with semantic typo removal."
        }

        require(
            validationIssueCount == 0
        ) {
            "A baseline cannot be frozen with validation issues."
        }

        require(normalizedCatalogValid)
        require(pipelineValid)

        require(
            supportingArtifacts.isNotEmpty()
        )

        require(
            supportingArtifacts ==
                    supportingArtifacts.toSortedMap()
        ) {
            "supportingArtifacts must be sorted by key."
        }

        require(valid)
    }

    companion object {
        const val CURRENT_VERSION = 1

        private val BASELINE_ID_REGEX =
            Regex(
                "canonical-food-catalog-v[1-9][0-9]*-[0-9a-f]{16}"
            )
    }
}