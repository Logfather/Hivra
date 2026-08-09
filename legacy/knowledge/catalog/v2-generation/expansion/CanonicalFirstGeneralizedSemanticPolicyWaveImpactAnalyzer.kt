package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

import de.shopme.testing.system.tools.knowledge.catalog.expansion.approval.CanonicalApprovedCatalogExpansionResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalCatalogExpansionSemanticValidationResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyImplementationBatchResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalFirstGeneralizedSemanticPolicyWaveFactory
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySet
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType

class CanonicalFirstGeneralizedSemanticPolicyWaveImpactAnalyzer(
    private val waveFactory:
    CanonicalFirstGeneralizedSemanticPolicyWaveFactory =
        CanonicalFirstGeneralizedSemanticPolicyWaveFactory()
) {

    fun analyze(
        baseline:
        CanonicalSemanticPolicyBatchImpactBaseline,

        currentSemanticValidation:
        CanonicalCatalogExpansionSemanticValidationResult,

        currentPolicyBatches:
        CanonicalSemanticPolicyImplementationBatchResult,

        currentApprovedExpansion:
        CanonicalApprovedCatalogExpansionResult,

        currentPolicySet:
        CanonicalFamilyAxisSemanticPolicySet
    ): CanonicalSemanticPolicyBatchImpactAnalysis {
        require(baseline.valid)
        require(currentSemanticValidation.valid)
        require(currentPolicyBatches.valid)
        require(currentApprovedExpansion.valid)
        require(currentPolicySet.valid)

        require(
            baseline.batchKey ==
                    WAVE_KEY
        ) {
            "Unexpected generalized-wave impact baseline: " +
                    baseline.batchKey
        }

        require(
            baseline.generatedCandidateCount ==
                    currentSemanticValidation.generatedCandidateCount
        )

        require(
            baseline.generatedCandidateCount ==
                    currentPolicyBatches.generatedCandidateCount
        )

        require(
            baseline.generatedCandidateCount ==
                    currentApprovedExpansion.generatedCandidateCount
        )

        val wavePolicies:
                List<CanonicalFamilyAxisSemanticPolicyEntry> =
            waveFactory
                .createBatches()
                .flatMap { batch ->
                    batch.policies
                }
                .sortedWith(
                    compareBy<
                            CanonicalFamilyAxisSemanticPolicyEntry
                            > { policy ->
                        policy.familyKey
                    }.thenBy { policy ->
                        policy.axis.name
                    }
                )

        require(
            wavePolicies.size ==
                    EXPECTED_POLICY_COUNT_DELTA
        )

        require(
            wavePolicies
                .map {
                    it.identityKey
                }
                .distinct()
                .size ==
                    EXPECTED_POLICY_COUNT_DELTA
        )

        val currentPoliciesByIdentity =
            currentPolicySet.entries
                .associateBy {
                    it.identityKey
                }

        wavePolicies.forEach { expected ->
            require(
                currentPoliciesByIdentity[
                    expected.identityKey
                ] ==
                        expected
            ) {
                "Wave-1 policy is missing or divergent: " +
                        expected.identityKey
            }
        }

        val acceptedAfter =
            currentSemanticValidation.acceptedCandidateCount

        val rejectedAfter =
            currentSemanticValidation.rejectedCandidateCount

        val reviewRequiredAfter =
            currentSemanticValidation.reviewRequiredCandidateCount

        val acceptedDelta =
            acceptedAfter -
                    baseline.acceptedCandidateCount

        val rejectedDelta =
            rejectedAfter -
                    baseline.rejectedCandidateCount

        val reviewRequiredDelta =
            reviewRequiredAfter -
                    baseline.reviewRequiredCandidateCount

        val resolvedReviewCandidateCount =
            baseline.reviewRequiredCandidateCount -
                    reviewRequiredAfter

        val resolvedReviewShare =
            if (
                baseline.reviewRequiredCandidateCount ==
                0
            ) {
                0.0
            } else {
                resolvedReviewCandidateCount.toDouble() /
                        baseline.reviewRequiredCandidateCount.toDouble()
            }

        val missingPolicyGapsAfter =
            currentPolicyBatches.missingPolicyGapCount

        val missingPolicyGapDelta =
            missingPolicyGapsAfter -
                    baseline.missingPolicyGapCount

        val closedPolicyGapCount =
            baseline.missingPolicyGapCount -
                    missingPolicyGapsAfter

        val materializedEntriesAfter =
            currentApprovedExpansion.materializedEntryCount

        val materializedEntryDelta =
            materializedEntriesAfter -
                    baseline.materializedEntryCount

        val expandedCatalogEntriesAfter =
            currentApprovedExpansion.expandedCatalogEntryCount

        val expandedCatalogEntryDelta =
            expandedCatalogEntriesAfter -
                    baseline.expandedCatalogEntryCount

        val policyCountAfter =
            currentPolicySet.policyCount

        val policyCountBefore =
            policyCountAfter -
                    EXPECTED_POLICY_COUNT_DELTA

        val policyCountDelta =
            policyCountAfter -
                    policyCountBefore

        val curatedPolicyCountDelta =
            wavePolicies.count {
                it.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .CURATED_ALLOWED_VALUES
            }

        val notApplicablePolicyCountDelta =
            wavePolicies.count {
                it.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .NOT_APPLICABLE
            }

        val semanticDecisionArithmeticValid =
            acceptedAfter +
                    rejectedAfter +
                    reviewRequiredAfter ==
                    baseline.generatedCandidateCount &&
                    acceptedDelta +
                    rejectedDelta +
                    reviewRequiredDelta ==
                    0

        val reviewBacklogReduced =
            reviewRequiredAfter <
                    baseline.reviewRequiredCandidateCount &&
                    resolvedReviewCandidateCount >
                    0

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

        val catalogExpansionArithmeticValid =
            materializedEntryDelta ==
                    expandedCatalogEntryDelta &&
                    materializedEntryDelta ==
                    acceptedDelta

        val blockers =
            buildList {
                if (!semanticDecisionArithmeticValid) {
                    add(
                        "Semantic decision arithmetic changed."
                    )
                }

                if (!reviewBacklogReduced) {
                    add(
                        "Generalized policy wave did not reduce the " +
                                "review backlog."
                    )
                }

                if (!expectedPolicyGapClosureReached) {
                    add(
                        "Expected $EXPECTED_CLOSED_POLICY_GAP_COUNT " +
                                "closed policy gaps, but observed " +
                                "$closedPolicyGapCount."
                    )
                }

                if (!policyExpansionValid) {
                    add(
                        "Wave policy deltas are inconsistent: " +
                                "total=$policyCountDelta, " +
                                "curated=$curatedPolicyCountDelta, " +
                                "notApplicable=$notApplicablePolicyCountDelta."
                    )
                }

                if (!catalogExpansionArithmeticValid) {
                    add(
                        "Accepted, materialized and expanded-catalog " +
                                "deltas are inconsistent."
                    )
                }

                if (acceptedDelta < 0) {
                    add(
                        "Accepted candidate count decreased."
                    )
                }

                if (rejectedDelta < 0) {
                    add(
                        "Rejected candidate count decreased."
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
                reviewRequiredAfter,

            reviewRequiredDelta =
                reviewRequiredDelta,

            resolvedReviewCandidateCount =
                resolvedReviewCandidateCount,

            resolvedReviewShare =
                resolvedReviewShare,

            missingPolicyGapsBefore =
                baseline.missingPolicyGapCount,

            missingPolicyGapsAfter =
                missingPolicyGapsAfter,

            missingPolicyGapDelta =
                missingPolicyGapDelta,

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
                policyCountBefore,

            policyCountAfter =
                policyCountAfter,

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
        const val WAVE_KEY =
            "generalized-semantic-policy-wave-001"

        const val EXPECTED_CLOSED_POLICY_GAP_COUNT =
            33

        const val EXPECTED_POLICY_COUNT_DELTA =
            33

        const val EXPECTED_CURATED_POLICY_COUNT_DELTA =
            29

        const val EXPECTED_NOT_APPLICABLE_POLICY_COUNT_DELTA =
            4
    }
}