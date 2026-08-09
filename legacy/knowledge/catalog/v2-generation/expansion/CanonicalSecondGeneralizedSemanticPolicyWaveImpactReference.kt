package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

object CanonicalSecondGeneralizedSemanticPolicyWaveImpactReference {

    const val WAVE_KEY =
        "generalized-semantic-policy-wave-002"

    const val GENERATED_CANDIDATE_COUNT =
        9_304

    const val ACCEPTED_BEFORE =
        146

    const val REJECTED_BEFORE =
        1_634

    const val REVIEW_REQUIRED_BEFORE =
        7_524

    const val MISSING_POLICY_GAPS_BEFORE =
        1_641

    const val MISSING_POLICY_GAPS_AFTER =
        1_609

    const val CLOSED_POLICY_GAP_COUNT =
        32

    const val MATERIALIZED_ENTRIES_BEFORE =
        146

    const val EXPANDED_CATALOG_ENTRIES_BEFORE =
        3_565

    const val POLICY_COUNT_BEFORE =
        94

    const val POLICY_COUNT_AFTER =
        126

    const val POLICY_COUNT_DELTA =
        32

    const val CURATED_POLICY_COUNT_DELTA =
        10

    const val NOT_APPLICABLE_POLICY_COUNT_DELTA =
        22

    fun validate(
        analysis:
        CanonicalSemanticPolicyBatchImpactAnalysis
    ) {
        require(analysis.valid)

        require(
            analysis.batchKey ==
                    WAVE_KEY
        )

        require(
            analysis.generatedCandidateCount ==
                    GENERATED_CANDIDATE_COUNT
        )

        require(
            analysis.acceptedBefore ==
                    ACCEPTED_BEFORE
        )

        require(
            analysis.rejectedBefore ==
                    REJECTED_BEFORE
        )

        require(
            analysis.reviewRequiredBefore ==
                    REVIEW_REQUIRED_BEFORE
        )

        require(
            analysis.missingPolicyGapsBefore ==
                    MISSING_POLICY_GAPS_BEFORE
        )

        require(
            analysis.missingPolicyGapsAfter ==
                    MISSING_POLICY_GAPS_AFTER
        )

        require(
            analysis.closedPolicyGapCount ==
                    CLOSED_POLICY_GAP_COUNT
        )

        require(
            analysis.materializedEntriesBefore ==
                    MATERIALIZED_ENTRIES_BEFORE
        )

        require(
            analysis.expandedCatalogEntriesBefore ==
                    EXPANDED_CATALOG_ENTRIES_BEFORE
        )

        require(
            analysis.policyCountBefore ==
                    POLICY_COUNT_BEFORE
        )

        require(
            analysis.policyCountAfter ==
                    POLICY_COUNT_AFTER
        )

        require(
            analysis.policyCountDelta ==
                    POLICY_COUNT_DELTA
        )

        require(
            analysis.curatedPolicyCountDelta ==
                    CURATED_POLICY_COUNT_DELTA
        )

        require(
            analysis.notApplicablePolicyCountDelta ==
                    NOT_APPLICABLE_POLICY_COUNT_DELTA
        )

        require(
            analysis.acceptedAfter >=
                    analysis.acceptedBefore
        )

        require(
            analysis.rejectedAfter >=
                    analysis.rejectedBefore
        )

        require(
            analysis.reviewRequiredAfter <
                    analysis.reviewRequiredBefore
        )

        require(
            analysis.resolvedReviewCandidateCount >
                    0
        )

        require(
            analysis.acceptedDelta +
                    analysis.rejectedDelta +
                    analysis.reviewRequiredDelta ==
                    0
        )

        require(
            analysis.materializedEntryDelta ==
                    analysis.acceptedDelta
        )

        require(
            analysis.expandedCatalogEntryDelta ==
                    analysis.materializedEntryDelta
        )

        require(analysis.semanticDecisionArithmeticValid)
        require(analysis.reviewBacklogReduced)
        require(analysis.expectedPolicyGapClosureReached)
        require(analysis.policyExpansionValid)
        require(analysis.catalogExpansionArithmeticValid)
    }
}