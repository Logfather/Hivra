package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

import de.shopme.testing.system.tools.knowledge.catalog.expansion.approval.CanonicalApprovedCatalogExpansionResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalCatalogExpansionSemanticValidationResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyImplementationBatchResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure.CanonicalBoundedSemanticPolicyClosurePlan
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation.CanonicalCuratedSemanticPolicyBatchManifest
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySet
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType

class CanonicalBoundedSemanticPolicyWaveFiveImpactAnalyzer {

    fun analyze(
        baseline:
        CanonicalSemanticPolicyBatchImpactBaseline,

        closurePlan:
        CanonicalBoundedSemanticPolicyClosurePlan,

        curatedManifest:
        CanonicalCuratedSemanticPolicyBatchManifest,

        currentPolicySet:
        CanonicalFamilyAxisSemanticPolicySet,

        currentSemanticValidation:
        CanonicalCatalogExpansionSemanticValidationResult,

        currentPolicyBatches:
        CanonicalSemanticPolicyImplementationBatchResult,

        currentApprovedExpansion:
        CanonicalApprovedCatalogExpansionResult
    ): CanonicalSemanticPolicyBatchImpactAnalysis {
        require(baseline.valid)
        require(closurePlan.valid)
        require(curatedManifest.valid)
        require(currentPolicySet.valid)
        require(currentSemanticValidation.valid)
        require(currentPolicyBatches.valid)
        require(currentApprovedExpansion.valid)

        require(
            baseline.batchKey ==
                    CanonicalBoundedSemanticPolicyWaveFiveImpactBaselineFactory
                        .WAVE_KEY
        )

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

        val waveBatches =
            curatedManifest.batches
                .filter { batch ->
                    batch.curationId.startsWith(
                        WAVE_CURATION_ID_PREFIX
                    )
                }
                .sortedBy { batch ->
                    batch.sourceImplementationBatchKey
                }

        require(waveBatches.isNotEmpty()) {
            "The curated manifest contains no persisted bounded Wave-5 batches."
        }

        require(
            waveBatches.all { batch ->
                batch.valid &&
                        batch.complete
            }
        ) {
            "Persisted bounded Wave 5 contains an invalid or incomplete batch."
        }

        val wavePolicies:
                List<CanonicalFamilyAxisSemanticPolicyEntry> =
            waveBatches
                .flatMap { batch ->
                    batch.policies
                }
                .groupBy { policy ->
                    policy.identityKey
                }
                .toSortedMap()
                .map { (identityKey, policies) ->
                    val distinctPolicies =
                        policies.distinct()

                    require(
                        distinctPolicies.size ==
                                1
                    ) {
                        "Wave 5 contains divergent policies for identity " +
                                "'$identityKey': $distinctPolicies"
                    }

                    distinctPolicies.single()
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
                    EXPECTED_PERSISTED_WAVE_POLICY_COUNT
        ) {
            "Expected $EXPECTED_PERSISTED_WAVE_POLICY_COUNT persisted " +
                    "Wave-5 policies, but found ${wavePolicies.size}."
        }


        val currentPoliciesByIdentity =
            currentPolicySet.entries
                .associateBy { policy ->
                    policy.identityKey
                }

        wavePolicies.forEach { expectedPolicy ->
            require(
                currentPoliciesByIdentity[
                    expectedPolicy.identityKey
                ] ==
                        expectedPolicy
            ) {
                "Applied Wave-5 policy is missing or divergent: " +
                        expectedPolicy.identityKey
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

        val policyCountBefore =
            CanonicalBoundedSemanticPolicyWaveFiveImpactBaselineFactory
                .POLICY_COUNT_BEFORE

        val policyCountAfter =
            currentPolicySet.policyCount

        val policyCountDelta =
            policyCountAfter -
                    policyCountBefore

        val curatedPolicyCountDelta =
            wavePolicies.count { policy ->
                policy.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .CURATED_ALLOWED_VALUES
            }

        val notApplicablePolicyCountDelta =
            wavePolicies.count { policy ->
                policy.policyType ==
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

        val wavePolicyCount =
            wavePolicies.size

        val wavePolicyCompositionValid =
            curatedPolicyCountDelta +
                    notApplicablePolicyCountDelta ==
                    wavePolicyCount

        val wavePoliciesAppliedExactly =
            wavePolicies.all { expectedPolicy ->
                currentPoliciesByIdentity[
                    expectedPolicy.identityKey
                ] ==
                        expectedPolicy
            }

        val expectedPolicyGapClosureReached =
            closedPolicyGapCount >=
                    wavePolicyCount

        val policyExpansionValid =
            policyCountDelta >=
                    wavePolicyCount &&
                    wavePolicyCompositionValid &&
                    wavePoliciesAppliedExactly

        val catalogExpansionArithmeticValid =
            materializedEntryDelta ==
                    expandedCatalogEntryDelta &&
                    materializedEntryDelta ==
                    acceptedDelta

        val blockers =
            buildList {
                if (!semanticDecisionArithmeticValid) {
                    add(
                        "Semantic decision arithmetic is inconsistent."
                    )
                }

                if (!reviewBacklogReduced) {
                    add(
                        "Wave 5 did not reduce the semantic review backlog."
                    )
                }

                if (!expectedPolicyGapClosureReached) {
                    add(
                        "Wave 5 contains $wavePolicyCount policies, but only " +
                                "$closedPolicyGapCount policy gaps were closed."
                    )
                }

                if (!policyExpansionValid) {
                    add(
                        "Wave-5 policy expansion is inconsistent: " +
                                "wavePolicies=$wavePolicyCount, " +
                                "totalPolicyDelta=$policyCountDelta, " +
                                "curatedWavePolicies=$curatedPolicyCountDelta, " +
                                "notApplicableWavePolicies=" +
                                "$notApplicablePolicyCountDelta."
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

        const val WAVE_CURATION_ID_PREFIX =
            "curated-wave-005-"

        const val EXPECTED_PERSISTED_WAVE_POLICY_COUNT =
            505
    }
}