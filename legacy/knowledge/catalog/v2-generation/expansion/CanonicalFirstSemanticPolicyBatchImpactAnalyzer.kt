package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

import de.shopme.testing.system.tools.knowledge.catalog.expansion.approval.CanonicalApprovedCatalogExpansionResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalCatalogExpansionSemanticValidationResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyImplementationBatchResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySet

class CanonicalFirstSemanticPolicyBatchImpactAnalyzer {

    fun analyze(
        baseline:
        CanonicalSemanticPolicyBatchImpactBaseline,

        currentSemanticValidation:
        CanonicalCatalogExpansionSemanticValidationResult,

        currentPolicyBatches:
        CanonicalSemanticPolicyImplementationBatchResult,

        currentApprovedExpansion:
        CanonicalApprovedCatalogExpansionResult,

        basePolicySet:
        CanonicalFamilyAxisSemanticPolicySet,

        currentPolicySet:
        CanonicalFamilyAxisSemanticPolicySet
    ): CanonicalSemanticPolicyBatchImpactAnalysis {
        require(baseline.valid)
        require(currentSemanticValidation.valid)
        require(currentPolicyBatches.valid)
        require(currentApprovedExpansion.valid)
        require(basePolicySet.valid)
        require(currentPolicySet.valid)

        require(
            baseline.generatedCandidateCount ==
                    currentSemanticValidation
                        .generatedCandidateCount
        )

        require(
            baseline.generatedCandidateCount ==
                    currentPolicyBatches
                        .generatedCandidateCount
        )

        require(
            baseline.generatedCandidateCount ==
                    currentApprovedExpansion
                        .generatedCandidateCount
        )

        val acceptedAfter =
            currentSemanticValidation
                .acceptedCandidateCount

        val rejectedAfter =
            currentSemanticValidation
                .rejectedCandidateCount

        val reviewAfter =
            currentSemanticValidation
                .reviewRequiredCandidateCount

        val acceptedDelta =
            acceptedAfter -
                    baseline.acceptedCandidateCount

        val rejectedDelta =
            rejectedAfter -
                    baseline.rejectedCandidateCount

        val reviewDelta =
            reviewAfter -
                    baseline.reviewRequiredCandidateCount

        val resolvedReviewCandidateCount =
            baseline.reviewRequiredCandidateCount -
                    reviewAfter

        val resolvedReviewShare =
            if (
                baseline.reviewRequiredCandidateCount ==
                0
            ) {
                0.0
            } else {
                resolvedReviewCandidateCount
                    .toDouble() /
                        baseline
                            .reviewRequiredCandidateCount
                            .toDouble()
            }

        val missingPolicyGapsAfter =
            currentPolicyBatches
                .missingPolicyGapCount

        val closedPolicyGapCount =
            baseline.missingPolicyGapCount -
                    missingPolicyGapsAfter

        val materializedEntriesAfter =
            currentApprovedExpansion
                .materializedEntryCount

        val expandedCatalogEntriesAfter =
            currentApprovedExpansion
                .expandedCatalogEntryCount

        val policyCountDelta =
            currentPolicySet.policyCount -
                    basePolicySet.policyCount

        val curatedPolicyCountDelta =
            currentPolicySet
                .curatedAllowedValuesPolicyCount -
                    basePolicySet
                        .curatedAllowedValuesPolicyCount

        val notApplicablePolicyCountDelta =
            currentPolicySet
                .notApplicablePolicyCount -
                    basePolicySet
                        .notApplicablePolicyCount

        val semanticDecisionArithmeticValid =
            acceptedDelta +
                    rejectedDelta +
                    reviewDelta ==
                    0 &&
                    acceptedAfter +
                    rejectedAfter +
                    reviewAfter ==
                    baseline.generatedCandidateCount

        val reviewBacklogReduced =
            reviewAfter <
                    baseline.reviewRequiredCandidateCount &&
                    resolvedReviewCandidateCount > 0

        val expectedPolicyGapClosureReached =
            closedPolicyGapCount ==
                    EXPECTED_CLOSED_POLICY_GAP_COUNT

        val policyExpansionValid =
            policyCountDelta ==
                    EXPECTED_POLICY_COUNT_DELTA &&
                    curatedPolicyCountDelta ==
                    EXPECTED_CURATED_POLICY_COUNT_DELTA &&
                    notApplicablePolicyCountDelta ==
                    EXPECTED_NOT_APPLICABLE_POLICY_COUNT_DELTA

        val materializedEntryDelta =
            materializedEntriesAfter -
                    baseline.materializedEntryCount

        val expandedCatalogEntryDelta =
            expandedCatalogEntriesAfter -
                    baseline.expandedCatalogEntryCount

        val catalogExpansionArithmeticValid =
            materializedEntryDelta ==
                    expandedCatalogEntryDelta &&
                    materializedEntryDelta ==
                    acceptedDelta

        val blockers =
            buildList {
                if (!semanticDecisionArithmeticValid) {
                    add(
                        "Semantic decision deltas do not preserve the " +
                                "generated candidate total."
                    )
                }

                if (!reviewBacklogReduced) {
                    add(
                        "First semantic policy batch did not reduce the " +
                                "semantic review backlog."
                    )
                }

                if (!expectedPolicyGapClosureReached) {
                    add(
                        "Expected exactly 12 closed policy gaps, but " +
                                "observed $closedPolicyGapCount."
                    )
                }

                if (!policyExpansionValid) {
                    add(
                        "Policy-set deltas do not match the first batch: " +
                                "total=$policyCountDelta, " +
                                "curated=$curatedPolicyCountDelta, " +
                                "notApplicable=$notApplicablePolicyCountDelta."
                    )
                }

                if (!catalogExpansionArithmeticValid) {
                    add(
                        "Accepted-candidate, materialization and expanded-" +
                                "catalog deltas are inconsistent."
                    )
                }

                if (acceptedDelta < 0) {
                    add(
                        "Accepted candidate count decreased."
                    )
                }

                if (rejectedDelta < 0) {
                    add(
                        "Rejected candidate count decreased unexpectedly."
                    )
                }
            }
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .sorted()

        return CanonicalSemanticPolicyBatchImpactAnalysis(
            version =
                CanonicalSemanticPolicyBatchImpactAnalysis
                    .CURRENT_VERSION,

            batchKey =
                baseline.batchKey,

            generatedCandidateCount =
                baseline.generatedCandidateCount,

            acceptedBefore =
                baseline.acceptedCandidateCount,

            acceptedAfter =
                acceptedAfter,

            acceptedDelta =
                acceptedDelta,

            rejectedBefore =
                baseline.rejectedCandidateCount,

            rejectedAfter =
                rejectedAfter,

            rejectedDelta =
                rejectedDelta,

            reviewRequiredBefore =
                baseline.reviewRequiredCandidateCount,

            reviewRequiredAfter =
                reviewAfter,

            reviewRequiredDelta =
                reviewDelta,

            resolvedReviewCandidateCount =
                resolvedReviewCandidateCount,

            resolvedReviewShare =
                resolvedReviewShare,

            missingPolicyGapsBefore =
                baseline.missingPolicyGapCount,

            missingPolicyGapsAfter =
                missingPolicyGapsAfter,

            missingPolicyGapDelta =
                missingPolicyGapsAfter -
                        baseline.missingPolicyGapCount,

            closedPolicyGapCount =
                closedPolicyGapCount,

            materializedEntriesBefore =
                baseline.materializedEntryCount,

            materializedEntriesAfter =
                materializedEntriesAfter,

            materializedEntryDelta =
                materializedEntryDelta,

            expandedCatalogEntriesBefore =
                baseline.expandedCatalogEntryCount,

            expandedCatalogEntriesAfter =
                expandedCatalogEntriesAfter,

            expandedCatalogEntryDelta =
                expandedCatalogEntryDelta,

            policyCountBefore =
                basePolicySet.policyCount,

            policyCountAfter =
                currentPolicySet.policyCount,

            policyCountDelta =
                policyCountDelta,

            curatedPolicyCountDelta =
                curatedPolicyCountDelta,

            notApplicablePolicyCountDelta =
                notApplicablePolicyCountDelta,

            semanticDecisionArithmeticValid =
                semanticDecisionArithmeticValid,

            reviewBacklogReduced =
                reviewBacklogReduced,

            expectedPolicyGapClosureReached =
                expectedPolicyGapClosureReached,

            policyExpansionValid =
                policyExpansionValid,

            catalogExpansionArithmeticValid =
                catalogExpansionArithmeticValid,

            blockers =
                blockers,

            valid =
                blockers.isEmpty() &&
                        semanticDecisionArithmeticValid &&
                        reviewBacklogReduced &&
                        expectedPolicyGapClosureReached &&
                        policyExpansionValid &&
                        catalogExpansionArithmeticValid
        )
    }

    private companion object {
        const val EXPECTED_CLOSED_POLICY_GAP_COUNT =
            12

        const val EXPECTED_POLICY_COUNT_DELTA =
            12

        const val EXPECTED_CURATED_POLICY_COUNT_DELTA =
            10

        const val EXPECTED_NOT_APPLICABLE_POLICY_COUNT_DELTA =
            2
    }
}