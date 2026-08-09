package de.shopme.testing.system.tools.knowledge.catalog.validation

data class NormalizedCatalogValidationResult(
    val version: Int,
    val inputEntryCount: Int,
    val issueCount: Int,
    val issueCountsByType:
    Map<NormalizedCatalogValidationIssueType, Int>,
    val issues: List<NormalizedCatalogValidationIssue>,
    val valid: Boolean
) {

    init {
        require(version > 0)
        require(inputEntryCount >= 0)
        require(issueCount == issues.size)
        require(issueCountsByType.values.sum() == issueCount)
        require(valid == issues.isEmpty())
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}