package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

import de.shopme.testing.system.tools.knowledge.catalog.expansion.approval
.CanonicalApprovedCatalogExpansionResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalCatalogExpansionSemanticValidationResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch
.CanonicalSemanticPolicyImplementationBatchResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation
.CanonicalCuratedSemanticPolicyBatchManifest
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy
.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy
.CanonicalFamilyAxisSemanticPolicySet
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy
.CanonicalFamilyAxisSemanticPolicyType

class CanonicalBoundedSemanticPolicyWaveSixImpactAnalyzer {

    fun analyze(
        waveFiveImpact:
        CanonicalSemanticPolicyBatchImpactAnalysis,

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
        require(waveFiveImpact.valid)
        require(curatedManifest.valid)
        require(currentPolicySet.valid)
        require(currentSemanticValidation.valid)
        require(currentPolicyBatches.valid)
        require(currentApprovedExpansion.valid)

        require(
            waveFiveImpact.batchKey ==
                    WAVE_FIVE_KEY
        ) {
            "Wave-6 impact requires the frozen Wave-5 impact as baseline."
        }

        require(
            waveFiveImpact.generatedCandidateCount ==
                    currentSemanticValidation.generatedCandidateCount
        )

        require(
            waveFiveImpact.generatedCandidateCount ==
                    currentPolicyBatches.generatedCandidateCount
        )

        require(
            waveFiveImpact.generatedCandidateCount ==
                    currentApprovedExpansion.generatedCandidateCount
        )

        val waveSixBatches =
            curatedManifest.batches
                .filter { batch ->
                    batch.curationId.startsWith(
                        WAVE_SIX_CURATION_ID_PREFIX
                    )
                }
                .sortedBy { batch ->
                    batch.sourceImplementationBatchKey
                }

        require(
            waveSixBatches.size ==
                    EXPECTED_WAVE_SIX_BATCH_COUNT
        ) {
            "Expected $EXPECTED_WAVE_SIX_BATCH_COUNT persisted Wave-6 " +
                    "batches, but found ${waveSixBatches.size}."
        }

        require(
            waveSixBatches.all { batch ->
                batch.valid &&
                        batch.complete
            }
        ) {
            "Persisted bounded Wave 6 contains an invalid or incomplete batch."
        }

        val waveSixPolicies:
                List<CanonicalFamilyAxisSemanticPolicyEntry> =
            waveSixBatches
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
                        "Wave 6 contains divergent policies for identity " +
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
            waveSixPolicies.size ==
                    EXPECTED_WAVE_SIX_POLICY_COUNT
        ) {
            "Expected $EXPECTED_WAVE_SIX_POLICY_COUNT persisted Wave-6 " +
                    "policies, but found ${waveSixPolicies.size}."
        }

        val currentPoliciesByIdentity =
            currentPolicySet.entries
                .associateBy { policy ->
                    policy.identityKey
                }

        waveSixPolicies.forEach { expectedPolicy ->
            require(
                currentPoliciesByIdentity[
                    expectedPolicy.identityKey
                ] ==
                        expectedPolicy
            ) {
                "Applied Wave-6 policy is missing or divergent: " +
                        expectedPolicy.identityKey
            }
        }

        val generatedCandidateCount =
            waveFiveImpact.generatedCandidateCount

        val acceptedBefore =
            waveFiveImpact.acceptedAfter

        val acceptedAfter =
            currentSemanticValidation.acceptedCandidateCount

        val acceptedDelta =
            acceptedAfter -
                    acceptedBefore

        val rejectedBefore =
            waveFiveImpact.rejectedAfter

        val rejectedAfter =
            currentSemanticValidation.rejectedCandidateCount

        val rejectedDelta =
            rejectedAfter -
                    rejectedBefore

        val reviewRequiredBefore =
            waveFiveImpact.reviewRequiredAfter

        val reviewRequiredAfter =
            currentSemanticValidation.reviewRequiredCandidateCount

        val reviewRequiredDelta =
            reviewRequiredAfter -
                    reviewRequiredBefore

        val resolvedReviewCandidateCount =
            reviewRequiredBefore -
                    reviewRequiredAfter

        val resolvedReviewShare =
            if (reviewRequiredBefore == 0) {
                0.0
            } else {
                resolvedReviewCandidateCount.toDouble() /
                        reviewRequiredBefore.toDouble()
            }

        val missingPolicyGapsBefore =
            waveFiveImpact.missingPolicyGapsAfter

        val missingPolicyGapsAfter =
            currentPolicyBatches.missingPolicyGapCount

        val missingPolicyGapDelta =
            missingPolicyGapsAfter -
                    missingPolicyGapsBefore

        val closedPolicyGapCount =
            missingPolicyGapsBefore -
                    missingPolicyGapsAfter

        val materializedEntriesBefore =
            waveFiveImpact.materializedEntriesAfter

        val materializedEntriesAfter =
            currentApprovedExpansion.materializedEntryCount

        val materializedEntryDelta =
            materializedEntriesAfter -
                    materializedEntriesBefore

        val expandedCatalogEntriesBefore =
            waveFiveImpact.expandedCatalogEntriesAfter

        val expandedCatalogEntriesAfter =
            currentApprovedExpansion.expandedCatalogEntryCount

        val expandedCatalogEntryDelta =
            expandedCatalogEntriesAfter -
                    expandedCatalogEntriesBefore

        val policyCountBefore =
            waveFiveImpact.policyCountAfter

        val policyCountAfter =
            currentPolicySet.policyCount

        val policyCountDelta =
            policyCountAfter -
                    policyCountBefore

        val curatedPolicyCountDelta =
            waveSixPolicies.count { policy ->
                policy.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .CURATED_ALLOWED_VALUES
            }

        val notApplicablePolicyCountDelta =
            waveSixPolicies.count { policy ->
                policy.policyType ==
                        CanonicalFamilyAxisSemanticPolicyType
                            .NOT_APPLICABLE
            }

        val semanticDecisionArithmeticValid =
            acceptedAfter +
                    rejectedAfter +
                    reviewRequiredAfter ==
                    generatedCandidateCount &&
                    acceptedDelta +
                    rejectedDelta +
                    reviewRequiredDelta ==
                    0

        val reviewBacklogReduced =
            reviewRequiredAfter <
                    reviewRequiredBefore &&
                    resolvedReviewCandidateCount >
                    0

        val explicitWaveSixPolicyCount =
            waveSixPolicies.size

        val explicitWaveSixPolicyCompositionValid =
            curatedPolicyCountDelta +
                    notApplicablePolicyCountDelta ==
                    explicitWaveSixPolicyCount

        val explicitWaveSixPoliciesAppliedExactly =
            waveSixPolicies.all { expectedPolicy ->
                currentPoliciesByIdentity[
                    expectedPolicy.identityKey
                ] ==
                        expectedPolicy
            }

        val expectedPolicyGapClosureReached =
            missingPolicyGapsBefore >
                    0 &&
                    missingPolicyGapsAfter ==
                    0 &&
                    closedPolicyGapCount ==
                    missingPolicyGapsBefore

        val policyExpansionValid =
            policyCountDelta ==
                    closedPolicyGapCount &&
                    policyCountDelta >=
                    explicitWaveSixPolicyCount &&
                    explicitWaveSixPolicyCompositionValid &&
                    explicitWaveSixPoliciesAppliedExactly

        val catalogExpansionArithmeticValid =
            materializedEntryDelta ==
                    expandedCatalogEntryDelta &&
                    materializedEntryDelta ==
                    acceptedDelta

        val terminalClosureReached =
            currentPolicyBatches.missingPolicyGapCount ==
                    0 &&
                    currentPolicyBatches.batchCount ==
                    0 &&
                    currentPolicyBatches.gaps.isEmpty() &&
                    currentPolicyBatches.batches.isEmpty() &&
                    currentPolicyBatches.completeGapCoverage &&
                    currentPolicyBatches.deterministicOrderValid &&
                    currentPolicyBatches.blockers.isEmpty()

        val blockers =
            buildList {
                if (!semanticDecisionArithmeticValid) {
                    add(
                        "Semantic decision arithmetic is inconsistent."
                    )
                }

                if (!reviewBacklogReduced) {
                    add(
                        "Wave 6 did not reduce the semantic review backlog."
                    )
                }

                if (!expectedPolicyGapClosureReached) {
                    add(
                        "Wave 6 did not close the complete remaining semantic-policy " +
                                "backlog: before=$missingPolicyGapsBefore, " +
                                "after=$missingPolicyGapsAfter, " +
                                "closed=$closedPolicyGapCount."
                    )
                }

                if (!policyExpansionValid) {
                    add(
                        "Wave-6 policy expansion is inconsistent: " +
                                "closedPolicyGaps=$closedPolicyGapCount, " +
                                "totalPolicyDelta=$policyCountDelta, " +
                                "explicitWaveSixPolicies=$explicitWaveSixPolicyCount, " +
                                "curatedWaveSixPolicies=$curatedPolicyCountDelta, " +
                                "notApplicableWaveSixPolicies=" +
                                "$notApplicablePolicyCountDelta."
                    )
                }

                if (!catalogExpansionArithmeticValid) {
                    add(
                        "Accepted, materialized and expanded-catalog " +
                                "deltas are inconsistent."
                    )
                }

                if (!terminalClosureReached) {
                    add(
                        "Semantic-policy closure did not reach the valid " +
                                "terminal state with zero gaps and zero batches."
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
                WAVE_SIX_KEY,

            generatedCandidateCount =
                generatedCandidateCount,

            acceptedBefore =
                acceptedBefore,

            acceptedAfter =
                acceptedAfter,

            acceptedDelta =
                acceptedDelta,

            rejectedBefore =
                rejectedBefore,

            rejectedAfter =
                rejectedAfter,

            rejectedDelta =
                rejectedDelta,

            reviewRequiredBefore =
                reviewRequiredBefore,

            reviewRequiredAfter =
                reviewRequiredAfter,

            reviewRequiredDelta =
                reviewRequiredDelta,

            resolvedReviewCandidateCount =
                resolvedReviewCandidateCount,

            resolvedReviewShare =
                resolvedReviewShare,

            missingPolicyGapsBefore =
                missingPolicyGapsBefore,

            missingPolicyGapsAfter =
                missingPolicyGapsAfter,

            missingPolicyGapDelta =
                missingPolicyGapDelta,

            closedPolicyGapCount =
                closedPolicyGapCount,

            materializedEntriesBefore =
                materializedEntriesBefore,

            materializedEntriesAfter =
                materializedEntriesAfter,

            materializedEntryDelta =
                materializedEntryDelta,

            expandedCatalogEntriesBefore =
                expandedCatalogEntriesBefore,

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
                        catalogExpansionArithmeticValid &&
                        terminalClosureReached
        )
    }

    companion object {

        const val WAVE_FIVE_KEY =
            "generalized-semantic-policy-wave-005"

        const val WAVE_SIX_KEY =
            "generalized-semantic-policy-wave-006"

        const val WAVE_SIX_CURATION_ID_PREFIX =
            "curated-wave-006-"

        const val EXPECTED_WAVE_SIX_BATCH_COUNT =
            2

        const val EXPECTED_WAVE_SIX_POLICY_COUNT =
            14
    }
}