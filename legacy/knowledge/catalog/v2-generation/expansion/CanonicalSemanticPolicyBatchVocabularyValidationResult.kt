package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch

data class CanonicalSemanticPolicyBatchVocabularyValidationResult(
    val entryCount: Int,
    val referencedValueCount: Int,
    val issueCount: Int,
    val issues: List<String>,
    val valid: Boolean
) {

    init {
        require(entryCount > 0)
        require(referencedValueCount >= 0)
        require(issueCount == issues.size)

        require(
            issues ==
                    issues
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        )

        require(
            valid ==
                    issues.isEmpty()
        )
    }
}