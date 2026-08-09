package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

class CanonicalBoundedSemanticPolicyWaveFiveImpactBaselineFactory {

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
                2_347,

            rejectedCandidateCount =
                2_341,

            reviewRequiredCandidateCount =
                4_616,

            missingPolicyGapCount =
                1_051,

            materializedEntryCount =
                2_347,

            expandedCatalogEntryCount =
                5_766,

            valid =
                true
        )

    companion object {

        const val WAVE_KEY =
            "generalized-semantic-policy-wave-005"

        const val POLICY_COUNT_BEFORE =
            684
    }
}