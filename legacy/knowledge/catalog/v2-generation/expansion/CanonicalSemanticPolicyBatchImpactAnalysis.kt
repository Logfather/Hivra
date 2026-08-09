package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

data class CanonicalSemanticPolicyBatchImpactAnalysis(
    val version: Int,

    val batchKey: String,

    val generatedCandidateCount: Int,

    val acceptedBefore: Int,
    val acceptedAfter: Int,
    val acceptedDelta: Int,

    val rejectedBefore: Int,
    val rejectedAfter: Int,
    val rejectedDelta: Int,

    val reviewRequiredBefore: Int,
    val reviewRequiredAfter: Int,
    val reviewRequiredDelta: Int,

    val resolvedReviewCandidateCount: Int,
    val resolvedReviewShare: Double,

    val missingPolicyGapsBefore: Int,
    val missingPolicyGapsAfter: Int,
    val missingPolicyGapDelta: Int,
    val closedPolicyGapCount: Int,

    val materializedEntriesBefore: Int,
    val materializedEntriesAfter: Int,
    val materializedEntryDelta: Int,

    val expandedCatalogEntriesBefore: Int,
    val expandedCatalogEntriesAfter: Int,
    val expandedCatalogEntryDelta: Int,

    val policyCountBefore: Int,
    val policyCountAfter: Int,
    val policyCountDelta: Int,

    val curatedPolicyCountDelta: Int,
    val notApplicablePolicyCountDelta: Int,

    val semanticDecisionArithmeticValid: Boolean,
    val reviewBacklogReduced: Boolean,
    val expectedPolicyGapClosureReached: Boolean,
    val policyExpansionValid: Boolean,
    val catalogExpansionArithmeticValid: Boolean,

    val blockers: List<String>,

    val valid: Boolean
) {

    init {
        require(version > 0)
        require(batchKey.isNotBlank())

        require(generatedCandidateCount > 0)

        require(acceptedBefore >= 0)
        require(acceptedAfter >= 0)
        require(acceptedDelta == acceptedAfter - acceptedBefore)

        require(rejectedBefore >= 0)
        require(rejectedAfter >= 0)
        require(rejectedDelta == rejectedAfter - rejectedBefore)

        require(reviewRequiredBefore >= 0)
        require(reviewRequiredAfter >= 0)

        require(
            reviewRequiredDelta ==
                    reviewRequiredAfter -
                    reviewRequiredBefore
        )

        require(
            resolvedReviewCandidateCount ==
                    reviewRequiredBefore -
                    reviewRequiredAfter
        )

        require(resolvedReviewCandidateCount >= 0)

        require(resolvedReviewShare in 0.0..1.0)

        require(missingPolicyGapsBefore >= 0)
        require(missingPolicyGapsAfter >= 0)

        require(
            missingPolicyGapDelta ==
                    missingPolicyGapsAfter -
                    missingPolicyGapsBefore
        )

        require(
            closedPolicyGapCount ==
                    missingPolicyGapsBefore -
                    missingPolicyGapsAfter
        )

        require(closedPolicyGapCount >= 0)

        require(
            materializedEntryDelta ==
                    materializedEntriesAfter -
                    materializedEntriesBefore
        )

        require(
            expandedCatalogEntryDelta ==
                    expandedCatalogEntriesAfter -
                    expandedCatalogEntriesBefore
        )

        require(
            policyCountDelta ==
                    policyCountAfter -
                    policyCountBefore
        )

        require(
            blockers ==
                    blockers
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .distinct()
                        .sorted()
        )

        require(
            valid ==
                    (
                            blockers.isEmpty() &&
                                    semanticDecisionArithmeticValid &&
                                    reviewBacklogReduced &&
                                    expectedPolicyGapClosureReached &&
                                    policyExpansionValid &&
                                    catalogExpansionArithmeticValid
                            )
        )
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}