package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

class CanonicalFirstGeneralizedSemanticPolicyWaveImpactBaselineFactory {

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
                49,

            rejectedCandidateCount =
                1_395,

            reviewRequiredCandidateCount =
                7_860,

            missingPolicyGapCount =
                1_674,

            materializedEntryCount =
                49,

            expandedCatalogEntryCount =
                3_468,

            valid =
                true
        )

    private companion object {
        const val WAVE_KEY =
            "generalized-semantic-policy-wave-001"
    }
}