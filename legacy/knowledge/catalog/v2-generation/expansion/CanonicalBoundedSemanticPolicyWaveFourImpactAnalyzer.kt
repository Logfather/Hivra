package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.impact

import de.shopme.testing.system.tools.knowledge.catalog.expansion.approval
.CanonicalApprovedCatalogExpansionResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalCatalogExpansionSemanticValidationResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch
.CanonicalSemanticPolicyImplementationBatchResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure
.CanonicalBoundedSemanticPolicyClosurePlan
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure
.CanonicalBoundedSemanticPolicyWaveFourFactory
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation
.CanonicalCuratedSemanticPolicyBatchManifest
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy
.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy
.CanonicalFamilyAxisSemanticPolicySet
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy
.CanonicalFamilyAxisSemanticPolicyType

class CanonicalBoundedSemanticPolicyWaveFourImpactAnalyzer {

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
                    CanonicalBoundedSemanticPolicyWaveFourImpactBaselineFactory
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

        val wave =
            requireNotNull(
                closurePlan.waves.singleOrNull {
                    it.waveNumber ==
                            CanonicalBoundedSemanticPolicyWaveFourFactory
                                .WAVE_NUMBER
                }
            ) {
                "Bounded closure plan contains no Wave 4."
            }

        val expectedWavePolicyIdentities =
            wave.assignments
                .map {
                    it.identityKey
                }
                .toSet()

        require(
            expectedWavePolicyIdentities.size ==
                    wave.assignedGapCount
        ) {
            "Wave-4 closure plan contains duplicate policy identities."
        }

        val wavePolicies:
                List<CanonicalFamilyAxisSemanticPolicyEntry> =
            curatedManifest.batches
                .flatMap {
                    it.policies
                }
                .filter {
                    it.identityKey in
                            expectedWavePolicyIdentities
                }
                .sortedWith(
                    compareBy<
                            CanonicalFamilyAxisSemanticPolicyEntry
                            > {
                        it.familyKey
                    }.thenBy {
                        it.axis.name
                    }
                )

        require(
            wavePolicies.size ==
                    wave.assignedGapCount
        ) {
            "Expected ${wave.assignedGapCount} Wave-4 policies, " +
                    "but found ${wavePolicies.size}."
        }

        require(
            wavePolicies
                .map {
                    it.identityKey
                }
                .toSet() ==
                    expectedWavePolicyIdentities
        ) {
            "Curated Wave-4 policies do not exactly match the closure plan."
        }

        val currentPolicyByIdentity =
            currentPolicySet.entries
                .associateBy {
                    it.identityKey
                }

        wavePolicies.forEach { expectedPolicy ->
            require(
                currentPolicyByIdentity[
                    expectedPolicy.identityKey
                ] ==
                        expectedPolicy
            ) {
                "Applied Wave-4 policy is missing or divergent: " +
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
            CanonicalBoundedSemanticPolicyWaveFourImpactBaselineFactory
                .POLICY_COUNT_BEFORE

        val policyCountAfter =
            currentPolicySet.policyCount

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
                    wave.assignedGapCount

        val policyExpansionValid =
            policyCountDelta ==
                    wave.assignedGapCount &&
                    curatedPolicyCountDelta +
                    notApplicablePolicyCountDelta ==
                    wave.assignedGapCount

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
                        "Wave 4 did not reduce the semantic review backlog."
                    )
                }

                if (!expectedPolicyGapClosureReached) {
                    add(
                        "Expected ${wave.assignedGapCount} closed policy " +
                                "gaps, but observed $closedPolicyGapCount."
                    )
                }

                if (!policyExpansionValid) {
                    add(
                        "Wave-4 policy expansion is inconsistent: " +
                                "expected=${wave.assignedGapCount}, " +
                                "actualDelta=$policyCountDelta, " +
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
}