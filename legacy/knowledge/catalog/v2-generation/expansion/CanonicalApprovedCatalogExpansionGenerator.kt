package de.shopme.testing.system.tools.knowledge.catalog.expansion.approval

import de.shopme.testing.system.tools.knowledge.catalog.baseline
.CanonicalFoodCatalogBaseline
import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate
.CanonicalCatalogExpansionCandidate
import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate
.CanonicalCatalogExpansionCandidateGenerationResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalCatalogExpansionSemanticValidationResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalExpansionSemanticDecision
import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem

class CanonicalApprovedCatalogExpansionGenerator {

    fun generate(
        baseline:
        CanonicalFoodCatalogBaseline,

        baselineItems:
        List<CatalogFoodItem>,

        candidates:
        CanonicalCatalogExpansionCandidateGenerationResult,

        semanticValidation:
        CanonicalCatalogExpansionSemanticValidationResult
    ): CanonicalApprovedCatalogExpansionResult {
        require(baseline.valid)
        require(candidates.valid)
        require(semanticValidation.valid)

        require(
            baseline.finalOutputEntryCount ==
                    baselineItems.size
        ) {
            "Baseline metadata count does not match normalized catalog."
        }

        require(
            baseline.baselineId ==
                    candidates.sourceBaselineId
        )

        require(
            baseline.baselineId ==
                    semanticValidation.sourceBaselineId
        )

        require(
            baseline.catalogArtifact.sha256 ==
                    candidates.sourceBaselineCatalogSha256
        )

        require(
            baseline.catalogArtifact.sha256 ==
                    semanticValidation.sourceBaselineCatalogSha256
        )

        require(
            candidates.generatedCandidateCount ==
                    semanticValidation.evaluatedCandidateCount
        )

        val candidateByIndex =
            candidates.candidates
                .associateBy {
                    it.candidateIndex
                }

        require(
            candidateByIndex.size ==
                    candidates.generatedCandidateCount
        )

        val baselineKeys =
            baselineItems
                .map {
                    requireNotNull(it.normalized)
                }
                .toSet()

        val expansionKeys =
            mutableSetOf<String>()

        val materializedItems =
            mutableListOf<CatalogFoodItem>()

        val blockers =
            mutableListOf<String>()

        val materializationEntries =
            semanticValidation.entries
                .sortedBy {
                    it.candidateIndex
                }
                .map { validationEntry ->
                    val candidate =
                        requireNotNull(
                            candidateByIndex[
                                validationEntry.candidateIndex
                            ]
                        )

                    require(
                        candidate.candidateKey ==
                                validationEntry.candidateKey
                    )

                    when (validationEntry.decision) {
                        CanonicalExpansionSemanticDecision.ACCEPT ->
                            materializeAcceptedCandidate(
                                candidate =
                                    candidate,

                                baselineKeys =
                                    baselineKeys,

                                expansionKeys =
                                    expansionKeys,

                                materializedItems =
                                    materializedItems,

                                blockers =
                                    blockers
                            )

                        CanonicalExpansionSemanticDecision
                            .REVIEW_REQUIRED ->
                            nonMaterializedEntry(
                                candidate = candidate,
                                semanticDecision =
                                    validationEntry.decision,
                                status =
                                    CanonicalExpansionMaterializationStatus
                                        .REVIEW_REQUIRED,
                                reason =
                                    "Candidate remains subject to semantic review."
                            )

                        CanonicalExpansionSemanticDecision
                            .REJECT_IMPOSSIBLE_COMBINATION,

                        CanonicalExpansionSemanticDecision
                            .REJECT_WRONG_FAMILY_VALUE,

                        CanonicalExpansionSemanticDecision
                            .REJECT_REDUNDANT_VARIANT ->
                            nonMaterializedEntry(
                                candidate = candidate,
                                semanticDecision =
                                    validationEntry.decision,
                                status =
                                    CanonicalExpansionMaterializationStatus
                                        .REJECTED_SEMANTICALLY,
                                reason =
                                    "Candidate was rejected by semantic validation: " +
                                            validationEntry.decision.name
                            )
                    }
                }

        val sortedMaterializedItems =
            CanonicalExpandedCatalogItemOrder
                .sort(materializedItems)

        val expandedCatalogItems =
            CanonicalExpandedCatalogItemOrder
                .sort(
                    baselineItems +
                            sortedMaterializedItems
                )

        val duplicateExpandedKeys =
            expandedCatalogItems
                .groupingBy {
                    requireNotNull(it.normalized)
                }
                .eachCount()
                .filterValues {
                    it > 1
                }
                .keys
                .sorted()

        duplicateExpandedKeys.forEach { key ->
            blockers +=
                "Expanded catalog contains duplicate normalized key '$key'."
        }

        val baselineCollisionCount =
            materializationEntries.count {
                it.status ==
                        CanonicalExpansionMaterializationStatus
                            .BLOCKED_BASELINE_KEY_COLLISION
            }

        val expansionCollisionCount =
            materializationEntries.count {
                it.status ==
                        CanonicalExpansionMaterializationStatus
                            .BLOCKED_EXPANSION_KEY_COLLISION
            }

        val sortedBlockers =
            blockers
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .sorted()

        val uniqueNormalizedKeys =
            expandedCatalogItems
                .map {
                    requireNotNull(it.normalized)
                }
                .distinct()
                .size ==
                    expandedCatalogItems.size

        val exactCatalogArithmeticValid =
            expandedCatalogItems.size ==
                    baselineItems.size +
                    sortedMaterializedItems.size

        val deterministicOrderValid =
            CanonicalExpandedCatalogItemOrder
                .isSorted(expandedCatalogItems)

        return CanonicalApprovedCatalogExpansionResult(
            version =
                CanonicalApprovedCatalogExpansionResult
                    .CURRENT_VERSION,

            sourceBaselineId =
                baseline.baselineId,

            sourceBaselineCatalogSha256 =
                baseline.catalogArtifact.sha256,

            baselineEntryCount =
                baselineItems.size,

            generatedCandidateCount =
                candidates.generatedCandidateCount,

            semanticallyAcceptedCandidateCount =
                semanticValidation.acceptedCandidateCount,

            semanticallyRejectedCandidateCount =
                semanticValidation.rejectedCandidateCount,

            reviewRequiredCandidateCount =
                semanticValidation.reviewRequiredCandidateCount,

            materializedEntryCount =
                sortedMaterializedItems.size,

            baselineKeyCollisionCount =
                baselineCollisionCount,

            expansionKeyCollisionCount =
                expansionCollisionCount,

            expandedCatalogEntryCount =
                expandedCatalogItems.size,

            materializedItems =
                sortedMaterializedItems,

            expandedCatalogItems =
                expandedCatalogItems,

            entries =
                materializationEntries,

            completeCandidateCoverage =
                materializationEntries.size ==
                        candidates.generatedCandidateCount,

            exactCatalogArithmeticValid =
                exactCatalogArithmeticValid,

            uniqueNormalizedKeys =
                uniqueNormalizedKeys,

            deterministicOrderValid =
                deterministicOrderValid,

            blockers =
                sortedBlockers,

            valid =
                sortedBlockers.isEmpty() &&
                        materializationEntries.size ==
                        candidates.generatedCandidateCount &&
                        exactCatalogArithmeticValid &&
                        uniqueNormalizedKeys &&
                        deterministicOrderValid &&
                        baselineCollisionCount == 0 &&
                        expansionCollisionCount == 0
        )
    }

    private fun materializeAcceptedCandidate(
        candidate:
        CanonicalCatalogExpansionCandidate,

        baselineKeys: Set<String>,

        expansionKeys:
        MutableSet<String>,

        materializedItems:
        MutableList<CatalogFoodItem>,

        blockers:
        MutableList<String>
    ): CanonicalExpansionMaterializationEntry {
        val normalizedKey =
            candidate.proposedNormalizedKey

        if (normalizedKey in baselineKeys) {
            blockers +=
                "Accepted candidate ${candidate.candidateIndex} collides " +
                        "with baseline key '$normalizedKey'."

            return nonMaterializedEntry(
                candidate = candidate,
                semanticDecision =
                    CanonicalExpansionSemanticDecision.ACCEPT,
                status =
                    CanonicalExpansionMaterializationStatus
                        .BLOCKED_BASELINE_KEY_COLLISION,
                reason =
                    "Normalized key '$normalizedKey' already exists " +
                            "in the canonical baseline."
            )
        }

        if (!expansionKeys.add(normalizedKey)) {
            blockers +=
                "Accepted candidate ${candidate.candidateIndex} collides " +
                        "with another expansion key '$normalizedKey'."

            return nonMaterializedEntry(
                candidate = candidate,
                semanticDecision =
                    CanonicalExpansionSemanticDecision.ACCEPT,
                status =
                    CanonicalExpansionMaterializationStatus
                        .BLOCKED_EXPANSION_KEY_COLLISION,
                reason =
                    "Normalized key '$normalizedKey' already exists " +
                            "in the approved expansion."
            )
        }

        val item =
            createCatalogFoodItem(
                candidate = candidate
            )

        materializedItems += item

        return CanonicalExpansionMaterializationEntry(
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

            semanticDecision =
                CanonicalExpansionSemanticDecision.ACCEPT,

            status =
                CanonicalExpansionMaterializationStatus
                    .MATERIALIZED,

            resultingCanonicalName =
                item.itemname,

            resultingNormalizedKey =
                item.normalized,

            reasons =
                listOf(
                    "Candidate passed deterministic semantic validation.",
                    "Candidate normalized key is unique against baseline and expansion."
                ).sorted()
        )
    }

    private fun createCatalogFoodItem(
        candidate:
        CanonicalCatalogExpansionCandidate
    ): CatalogFoodItem {
        val variantDisplayNames =
            candidate.variantValues
                .map {
                    it.displayName
                }
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()

        val autocompleteTokens =
            buildList {
                add(candidate.proposedNormalizedKey)
                add(candidate.familyKey)

                candidate.variantValues
                    .forEach {
                        add(it.valueKey)
                    }
            }
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .sorted()

        return CatalogFoodItem(
            itemname =
                candidate.proposedCanonicalName,

            category =
                candidate.category,

            production =
                "Standard",

            normalized =
                candidate.proposedNormalizedKey,

            plural =
                candidate.proposedCanonicalName,

            colloquial =
                variantDisplayNames,

            phoneticTokens =
                emptyList(),

            autocompleteTokens =
                autocompleteTokens,

            normalizedEnglish =
                null
        )
    }

    private fun nonMaterializedEntry(
        candidate:
        CanonicalCatalogExpansionCandidate,

        semanticDecision:
        CanonicalExpansionSemanticDecision,

        status:
        CanonicalExpansionMaterializationStatus,

        reason: String
    ): CanonicalExpansionMaterializationEntry =
        CanonicalExpansionMaterializationEntry(
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

            semanticDecision =
                semanticDecision,

            status =
                status,

            resultingCanonicalName =
                null,

            resultingNormalizedKey =
                null,

            reasons =
                listOf(reason.trim())
        )
}