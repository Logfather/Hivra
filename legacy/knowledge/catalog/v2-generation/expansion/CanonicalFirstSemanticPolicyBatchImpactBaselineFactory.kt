package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

class CanonicalFirstSemanticPolicyBatchImpactBaselineFactory {

    fun create():
            CanonicalSemanticPolicyBatchImpactBaseline =
        CanonicalSemanticPolicyBatchImpactBaseline(
            version =
                CanonicalSemanticPolicyBatchImpactBaseline
                    .CURRENT_VERSION,

            batchKey =
                FIRST_BATCH_KEY,

            generatedCandidateCount =
                9_304,

            acceptedCandidateCount =
                17,

            rejectedCandidateCount =
                322,

            reviewRequiredCandidateCount =
                8_965,

            missingPolicyGapCount =
                1_709,

            materializedEntryCount =
                17,

            expandedCatalogEntryCount =
                3_436,

            valid =
                true
        )

    private companion object {
        const val FIRST_BATCH_KEY =
            "semantic-policy-batch-vegetables-plant-species-001"
    }
}