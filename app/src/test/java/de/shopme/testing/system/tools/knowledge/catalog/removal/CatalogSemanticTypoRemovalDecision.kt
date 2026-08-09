package de.shopme.testing.system.tools.knowledge.catalog.removal

data class CatalogSemanticTypoRemovalDecision(
    val sourceIndex: Int,

    val itemName: String,
    val category: String?,
    val normalizedKey: String?,

    val previousMergeTargetSourceIndex: Int?,
    val previousMergeTargetName: String?,

    val reason: CatalogSemanticTypoRemovalReason,

    val evidence: List<String>
) {

    init {
        require(sourceIndex >= 0)
        require(itemName.isNotBlank())

        previousMergeTargetSourceIndex?.let {
            require(it >= 0)
            require(it != sourceIndex)
        }

        require(evidence.isNotEmpty())

        require(
            evidence ==
                    evidence
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        ) {
            "evidence must be normalized, unique and sorted."
        }
    }
}