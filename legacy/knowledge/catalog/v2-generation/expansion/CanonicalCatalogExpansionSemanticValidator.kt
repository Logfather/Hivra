package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic

import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate
.CanonicalCatalogExpansionCandidateGenerationResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySet
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.rule
.CanonicalAnimalPlantConflictRule
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.rule
.CanonicalFamilyValueCompatibilityRule
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.rule
.CanonicalImpossibleDietaryCombinationRule
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.rule
.CanonicalRedundantFamilyValueRule

class CanonicalCatalogExpansionSemanticValidator(
    policySet:
    CanonicalFamilyAxisSemanticPolicySet,

    private val decisionResolver:
    CanonicalExpansionSemanticDecisionResolver =
        CanonicalExpansionSemanticDecisionResolver(),

    additionalRules:
    List<CanonicalExpansionSemanticRule> =
        emptyList()
) {

    private val rules:
            List<CanonicalExpansionSemanticRule> =
        defaultRules(
            policySet = policySet
        ) + additionalRules

    fun validate(
        candidates:
        CanonicalCatalogExpansionCandidateGenerationResult
    ): CanonicalCatalogExpansionSemanticValidationResult {
        require(candidates.valid) {
            "Expansion candidate generation result must be valid."
        }

        require(rules.isNotEmpty())

        val blockers =
            mutableListOf<String>()

        val entries =
            candidates.candidates
                .sortedBy {
                    it.candidateIndex
                }
                .map { candidate ->
                    val context =
                        CanonicalExpansionSemanticContext(
                            candidate =
                                candidate,

                            valuesByAxis =
                                candidate.variantValues
                                    .associate {
                                        it.axis to
                                                it.valueKey
                                    }
                        )

                    val findings =
                        rules
                            .flatMap { rule ->
                                rule.evaluate(context)
                            }
                            .distinct()
                            .sortedWith(
                                compareBy<
                                        CanonicalExpansionSemanticFinding
                                        > {
                                    it.decision.name
                                }.thenBy {
                                    it.ruleType.name
                                }.thenBy {
                                    it.ruleKey
                                }.thenBy {
                                    it.message
                                }
                            )

                    val decision =
                        decisionResolver.resolve(
                            findings
                        )

                    CanonicalExpansionCandidateSemanticValidationEntry(
                        candidateIndex =
                            candidate.candidateIndex,

                        candidateKey =
                            candidate.candidateKey,

                        category =
                            candidate.category,

                        familyKey =
                            candidate.familyKey,

                        proposedCanonicalName =
                            candidate.proposedCanonicalName,

                        proposedNormalizedKey =
                            candidate.proposedNormalizedKey,

                        decision =
                            decision,

                        findings =
                            findings,

                        findingCount =
                            findings.size,

                        accepted =
                            decision ==
                                    CanonicalExpansionSemanticDecision
                                        .ACCEPT,

                        rejected =
                            decision in
                                    REJECTION_DECISIONS,

                        reviewRequired =
                            decision ==
                                    CanonicalExpansionSemanticDecision
                                        .REVIEW_REQUIRED
                    )
                }

        val missingCandidateIndices =
            candidates.candidates
                .map {
                    it.candidateIndex
                }
                .toSet() -
                    entries
                        .map {
                            it.candidateIndex
                        }
                        .toSet()

        if (missingCandidateIndices.isNotEmpty()) {
            blockers +=
                "Semantic validation did not cover candidate indices: " +
                        missingCandidateIndices
                            .sorted()
                            .joinToString()
        }

        val duplicateEntryIndices =
            entries
                .groupingBy {
                    it.candidateIndex
                }
                .eachCount()
                .filterValues {
                    it > 1
                }
                .keys

        if (duplicateEntryIndices.isNotEmpty()) {
            blockers +=
                "Semantic validation produced duplicate candidate indices: " +
                        duplicateEntryIndices
                            .sorted()
                            .joinToString()
        }

        val categories =
            entries
                .groupBy {
                    it.category
                }
                .toList()
                .sortedBy {
                    it.first
                }
                .map { (category, categoryEntries) ->
                    val sortedEntries =
                        categoryEntries
                            .sortedBy {
                                it.candidateIndex
                            }

                    CanonicalExpansionSemanticCategoryResult(
                        category =
                            category,

                        evaluatedCandidateCount =
                            sortedEntries.size,

                        acceptedCandidateCount =
                            sortedEntries.count {
                                it.accepted
                            },

                        rejectedCandidateCount =
                            sortedEntries.count {
                                it.rejected
                            },

                        reviewRequiredCandidateCount =
                            sortedEntries.count {
                                it.reviewRequired
                            },

                        decisionCounts =
                            sortedEntries
                                .groupingBy {
                                    it.decision
                                }
                                .eachCount()
                                .toList()
                                .sortedBy {
                                    it.first.name
                                }
                                .associate {
                                    it
                                },

                        entries =
                            sortedEntries,

                        complete =
                            sortedEntries.isNotEmpty()
                    )
                }

        val evaluatedCount =
            entries.size

        val acceptedCount =
            entries.count {
                it.accepted
            }

        val rejectedCount =
            entries.count {
                it.rejected
            }

        val reviewCount =
            entries.count {
                it.reviewRequired
            }

        val sortedBlockers =
            blockers
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .sorted()

        val completeCoverage =
            evaluatedCount ==
                    candidates.generatedCandidateCount

        val deterministicOrder =
            entries ==
                    entries.sortedBy {
                        it.candidateIndex
                    }

        return CanonicalCatalogExpansionSemanticValidationResult(
            version =
                CanonicalCatalogExpansionSemanticValidationResult
                    .CURRENT_VERSION,

            sourceBaselineId =
                candidates.sourceBaselineId,

            sourceBaselineCatalogSha256 =
                candidates.sourceBaselineCatalogSha256,

            generatedCandidateCount =
                candidates.generatedCandidateCount,

            evaluatedCandidateCount =
                evaluatedCount,

            acceptedCandidateCount =
                acceptedCount,

            rejectedCandidateCount =
                rejectedCount,

            reviewRequiredCandidateCount =
                reviewCount,

            acceptanceShare =
                share(
                    numerator = acceptedCount,
                    denominator = evaluatedCount
                ),

            rejectionShare =
                share(
                    numerator = rejectedCount,
                    denominator = evaluatedCount
                ),

            reviewRequiredShare =
                share(
                    numerator = reviewCount,
                    denominator = evaluatedCount
                ),

            decisionCounts =
                entries
                    .groupingBy {
                        it.decision
                    }
                    .eachCount()
                    .toList()
                    .sortedBy {
                        it.first.name
                    }
                    .associate {
                        it
                    },

            findingCountsByRuleType =
                entries
                    .flatMap {
                        it.findings
                    }
                    .groupingBy {
                        it.ruleType
                    }
                    .eachCount()
                    .toList()
                    .sortedBy {
                        it.first.name
                    }
                    .associate {
                        it
                    },

            categoryCount =
                categories.size,

            categories =
                categories,

            entries =
                entries,

            completeCandidateCoverage =
                completeCoverage,

            deterministicOrderValid =
                deterministicOrder,

            blockers =
                sortedBlockers,

            valid =
                sortedBlockers.isEmpty() &&
                        completeCoverage &&
                        deterministicOrder &&
                        categories.all {
                            it.complete
                        }
        )
    }

    private fun share(
        numerator: Int,
        denominator: Int
    ): Double =
        if (denominator == 0) {
            0.0
        } else {
            numerator.toDouble() /
                    denominator.toDouble()
        }

    private companion object {

        val REJECTION_DECISIONS =
            setOf(
                CanonicalExpansionSemanticDecision
                    .REJECT_IMPOSSIBLE_COMBINATION,

                CanonicalExpansionSemanticDecision
                    .REJECT_WRONG_FAMILY_VALUE,

                CanonicalExpansionSemanticDecision
                    .REJECT_REDUNDANT_VARIANT
            )

        fun defaultRules(
            policySet:
            CanonicalFamilyAxisSemanticPolicySet
        ): List<CanonicalExpansionSemanticRule> {
            val familySemanticValuePolicy =
                CanonicalFamilySemanticValuePolicy(
                    policySet = policySet
                )

            return listOf(
                CanonicalAnimalPlantConflictRule(),
                CanonicalImpossibleDietaryCombinationRule(),
                CanonicalRedundantFamilyValueRule(),
                CanonicalFamilyValueCompatibilityRule(
                    familySemanticValuePolicy =
                        familySemanticValuePolicy
                )
            )
        }
    }
}