package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

class CanonicalSecondSemanticPolicyBatchImpactBaselineFactory {

    fun create():
            CanonicalSemanticPolicyBatchImpactBaseline =
        CanonicalSemanticPolicyBatchImpactBaseline(
            version =
                CanonicalSemanticPolicyBatchImpactBaseline
                    .CURRENT_VERSION,

            batchKey =
                SECOND_BATCH_KEY,

            generatedCandidateCount =
                9_304,

            acceptedCandidateCount =
                20,

            rejectedCandidateCount =
                825,

            reviewRequiredCandidateCount =
                8_459,

            missingPolicyGapCount =
                1_697,

            materializedEntryCount =
                20,

            expandedCatalogEntryCount =
                3_439,

            valid =
                true
        )

    private companion object {

        const val SECOND_BATCH_KEY =
            "semantic-policy-batch-beverages-flavor-profile-001"
    }
}