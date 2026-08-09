package de.shopme.testing.system.tools.knowledge.catalog.category

data class CatalogCategoryValidationResult(
    val inputEntryCount: Int,
    val categorizedEntryCount: Int,
    val missingCategoryCount: Int,
    val unknownCategoryCount: Int,
    val validCategoryCount: Int,
    val uniqueAssignedCategoryCount: Int,
    val rootCategoryAssignmentCount: Int,
    val categoryCounts: Map<String, Int>,
    val unknownCategoryCounts: Map<String, Int>,
    val suggestedCategoryMappings: Map<String, String>,
    val unusedCategoryKeys: List<String>,
    val issues: List<CatalogCategoryIssue>,
    val valid: Boolean
) {

    init {
        require(inputEntryCount >= 0) {
            "inputEntryCount must not be negative."
        }

        require(categorizedEntryCount >= 0) {
            "categorizedEntryCount must not be negative."
        }

        require(missingCategoryCount >= 0) {
            "missingCategoryCount must not be negative."
        }

        require(unknownCategoryCount >= 0) {
            "unknownCategoryCount must not be negative."
        }

        require(validCategoryCount >= 0) {
            "validCategoryCount must not be negative."
        }

        require(uniqueAssignedCategoryCount >= 0) {
            "uniqueAssignedCategoryCount must not be negative."
        }

        require(rootCategoryAssignmentCount >= 0) {
            "rootCategoryAssignmentCount must not be negative."
        }

        require(categorizedEntryCount + missingCategoryCount == inputEntryCount) {
            "categorizedEntryCount plus missingCategoryCount must equal inputEntryCount."
        }

        require(validCategoryCount + unknownCategoryCount == categorizedEntryCount) {
            "validCategoryCount plus unknownCategoryCount must equal categorizedEntryCount."
        }

        require(categoryCounts.values.all { it >= 0 }) {
            "categoryCounts must not contain negative values."
        }

        require(unknownCategoryCounts.values.all { it >= 0 }) {
            "unknownCategoryCounts must not contain negative values."
        }

        require(categoryCounts.values.sum() == validCategoryCount) {
            "Sum of categoryCounts must equal validCategoryCount."
        }

        require(unknownCategoryCounts.values.sum() == unknownCategoryCount) {
            "Sum of unknownCategoryCounts must equal unknownCategoryCount."
        }

        require(uniqueAssignedCategoryCount == categoryCounts.size) {
            "uniqueAssignedCategoryCount must equal categoryCounts size."
        }

        require(unusedCategoryKeys.distinct().size == unusedCategoryKeys.size) {
            "unusedCategoryKeys must not contain duplicates."
        }

        require(
            unusedCategoryKeys == unusedCategoryKeys.sorted()
        ) {
            "unusedCategoryKeys must be sorted."
        }

        require(
            categoryCounts.keys.toList() == categoryCounts.keys.sorted()
        ) {
            "categoryCounts must be sorted by key."
        }

        require(
            unknownCategoryCounts.keys.toList() ==
                    unknownCategoryCounts.keys.sorted()
        ) {
            "unknownCategoryCounts must be sorted by key."
        }

        require(
            suggestedCategoryMappings.keys.toList() ==
                    suggestedCategoryMappings.keys.sorted()
        ) {
            "suggestedCategoryMappings must be sorted by key."
        }
    }
}