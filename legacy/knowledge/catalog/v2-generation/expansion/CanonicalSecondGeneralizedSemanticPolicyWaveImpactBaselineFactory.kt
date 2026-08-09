package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

class CanonicalSecondGeneralizedSemanticPolicyWaveImpactBaselineFactory {

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
                146,

            rejectedCandidateCount =
                1_634,

            reviewRequiredCandidateCount =
                7_524,

            missingPolicyGapCount =
                1_641,

            materializedEntryCount =
                146,

            expandedCatalogEntryCount =
                3_565,

            valid =
                true
        )

    private companion object {
        const val WAVE_KEY =
            "generalized-semantic-policy-wave-002"
    }
}