package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

object CanonicalSecondSemanticPolicyBatchImpactReference {

    const val BATCH_KEY =
        "semantic-policy-batch-beverages-flavor-profile-001"

    const val GENERATED_CANDIDATE_COUNT =
        9_304

    const val ACCEPTED_BEFORE =
        20

    const val REJECTED_BEFORE =
        825

    const val REVIEW_REQUIRED_BEFORE =
        8_459

    const val MISSING_POLICY_GAPS_BEFORE =
        1_697

    const val MATERIALIZED_ENTRIES_BEFORE =
        20

    const val EXPANDED_CATALOG_ENTRIES_BEFORE =
        3_439

    const val CLOSED_POLICY_GAP_COUNT =
        15

    const val POLICY_COUNT_DELTA =
        15

    const val CURATED_POLICY_COUNT_DELTA =
        15

    const val NOT_APPLICABLE_POLICY_COUNT_DELTA =
        0

    fun validate(
        analysis:
        CanonicalSemanticPolicyBatchImpactAnalysis
    ) {
        require(analysis.valid)

        require(
            analysis.batchKey ==
                    BATCH_KEY
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
            analysis.materializedEntriesBefore ==
                    MATERIALIZED_ENTRIES_BEFORE
        )

        require(
            analysis.expandedCatalogEntriesBefore ==
                    EXPANDED_CATALOG_ENTRIES_BEFORE
        )

        require(
            analysis.closedPolicyGapCount ==
                    CLOSED_POLICY_GAP_COUNT
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
            analysis.missingPolicyGapsAfter ==
                    1_682
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