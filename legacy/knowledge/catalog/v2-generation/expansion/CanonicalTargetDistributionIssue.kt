package de.shopme.testing.system.tools.knowledge.catalog.expansion.refinement

data class CanonicalTargetDistributionIssue(
    val type: CanonicalTargetDistributionIssueType,

    val severity:
    CanonicalTargetDistributionIssueSeverity,

    val category: String?,

    val message: String
) {

    init {
        category?.let {
            require(it.isNotBlank())
        }

        require(message.isNotBlank())
        require(message == message.trim())
    }
}