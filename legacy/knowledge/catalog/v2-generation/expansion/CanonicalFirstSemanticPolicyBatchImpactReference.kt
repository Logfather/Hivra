package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

object CanonicalFirstSemanticPolicyBatchImpactReference {

    const val BATCH_KEY =
        "semantic-policy-batch-vegetables-plant-species-001"

    const val GENERATED_CANDIDATE_COUNT =
        9_304

    const val ACCEPTED_BEFORE =
        17

    const val ACCEPTED_AFTER =
        20

    const val REJECTED_BEFORE =
        322

    const val REJECTED_AFTER =
        825

    const val REVIEW_REQUIRED_BEFORE =
        8_965

    const val REVIEW_REQUIRED_AFTER =
        8_459

    const val RESOLVED_REVIEW_CANDIDATE_COUNT =
        506

    const val MISSING_POLICY_GAPS_BEFORE =
        1_709

    const val MISSING_POLICY_GAPS_AFTER =
        1_697

    const val CLOSED_POLICY_GAP_COUNT =
        12

    const val MATERIALIZED_ENTRIES_BEFORE =
        17

    const val MATERIALIZED_ENTRIES_AFTER =
        20

    const val EXPANDED_CATALOG_ENTRIES_BEFORE =
        3_436

    const val EXPANDED_CATALOG_ENTRIES_AFTER =
        3_439

    const val POLICY_COUNT_DELTA =
        12

    const val CURATED_POLICY_COUNT_DELTA =
        10

    const val NOT_APPLICABLE_POLICY_COUNT_DELTA =
        2

    fun validate(
        analysis:
        CanonicalSemanticPolicyBatchImpactAnalysis
    ) {
        require(analysis.valid)

        require(analysis.batchKey == BATCH_KEY)

        require(
            analysis.generatedCandidateCount ==
                    GENERATED_CANDIDATE_COUNT
        )

        require(analysis.acceptedBefore == ACCEPTED_BEFORE)
        require(analysis.acceptedAfter == ACCEPTED_AFTER)
        require(analysis.acceptedDelta == 3)

        require(analysis.rejectedBefore == REJECTED_BEFORE)
        require(analysis.rejectedAfter == REJECTED_AFTER)
        require(analysis.rejectedDelta == 503)

        require(
            analysis.reviewRequiredBefore ==
                    REVIEW_REQUIRED_BEFORE
        )

        require(
            analysis.reviewRequiredAfter ==
                    REVIEW_REQUIRED_AFTER
        )

        require(analysis.reviewRequiredDelta == -506)

        require(
            analysis.resolvedReviewCandidateCount ==
                    RESOLVED_REVIEW_CANDIDATE_COUNT
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
            analysis.materializedEntriesAfter ==
                    MATERIALIZED_ENTRIES_AFTER
        )

        require(analysis.materializedEntryDelta == 3)

        require(
            analysis.expandedCatalogEntriesBefore ==
                    EXPANDED_CATALOG_ENTRIES_BEFORE
        )

        require(
            analysis.expandedCatalogEntriesAfter ==
                    EXPANDED_CATALOG_ENTRIES_AFTER
        )

        require(analysis.expandedCatalogEntryDelta == 3)

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

        require(analysis.semanticDecisionArithmeticValid)
        require(analysis.reviewBacklogReduced)
        require(analysis.expectedPolicyGapClosureReached)
        require(analysis.policyExpansionValid)
        require(analysis.catalogExpansionArithmeticValid)
    }
}