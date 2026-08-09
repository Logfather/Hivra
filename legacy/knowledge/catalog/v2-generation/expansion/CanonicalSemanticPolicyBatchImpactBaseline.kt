package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

data class CanonicalSemanticPolicyBatchImpactBaseline(
    val version: Int,

    val batchKey: String,

    val generatedCandidateCount: Int,

    val acceptedCandidateCount: Int,
    val rejectedCandidateCount: Int,
    val reviewRequiredCandidateCount: Int,

    val missingPolicyGapCount: Int,

    val materializedEntryCount: Int,
    val expandedCatalogEntryCount: Int,

    val valid: Boolean
) {

    init {
        require(version > 0)
        require(batchKey.isNotBlank())

        require(generatedCandidateCount > 0)

        require(acceptedCandidateCount >= 0)
        require(rejectedCandidateCount >= 0)
        require(reviewRequiredCandidateCount >= 0)

        require(
            generatedCandidateCount ==
                    acceptedCandidateCount +
                    rejectedCandidateCount +
                    reviewRequiredCandidateCount
        ) {
            "Semantic baseline decision counts must cover every candidate."
        }

        require(missingPolicyGapCount >= 0)
        require(materializedEntryCount >= 0)
        require(expandedCatalogEntryCount >= 0)

        require(
            valid ==
                    (
                            generatedCandidateCount > 0 &&
                                    expandedCatalogEntryCount >=
                                    materializedEntryCount
                            )
        )
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}