package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

class CanonicalThirdGeneralizedSemanticPolicyWaveImpactBaselineFactory {

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
                183,

            rejectedCandidateCount =
                2_062,

            reviewRequiredCandidateCount =
                7_059,

            missingPolicyGapCount =
                1_609,

            materializedEntryCount =
                183,

            expandedCatalogEntryCount =
                3_602,

            valid =
                true
        )

    private companion object {
        const val WAVE_KEY =
            "generalized-semantic-policy-wave-003"
    }
}