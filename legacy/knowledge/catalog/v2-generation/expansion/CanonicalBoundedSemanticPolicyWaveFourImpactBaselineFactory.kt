package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

class CanonicalBoundedSemanticPolicyWaveFourImpactBaselineFactory {

    fun create():
            CanonicalSemanticPolicyBatchImpactBaseline =
        CanonicalSemanticPolicyBatchImpactBaseline(
            version =
                CanonicalSemanticPolicyBatchImpactBaseline
                    .CURRENT_VERSION,

            batchKey =
                WAVE_KEY,

            generatedCandidateCount =
                9_304,

            acceptedCandidateCount =
                237,

            rejectedCandidateCount =
                2_321,

            reviewRequiredCandidateCount =
                6_746,

            missingPolicyGapCount =
                1_576,

            materializedEntryCount =
                237,

            expandedCatalogEntryCount =
                3_656,

            valid =
                true
        )

    companion object {

        const val WAVE_KEY =
            "generalized-semantic-policy-wave-004"

        const val POLICY_COUNT_BEFORE =
            159
    }
}