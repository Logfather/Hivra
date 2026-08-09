package de.shopme.testing.system.tools.knowledge.catalog.removal

import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassification
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassificationResult
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.ClassifiedCatalogReviewBacklogEntry
import java.util.Locale

class CatalogUnresolvedSemanticTypoRemover {

    fun remove(
        itemsBySourceIndex: Map<Int, CatalogFoodItem>,

        effectiveClassificationResult:
        CatalogReviewBacklogClassificationResult
    ): CatalogSemanticTypoRemovalResult {
        require(effectiveClassificationResult.valid) {
            "Effective review backlog classification must be valid."
        }

        require(
            itemsBySourceIndex.keys.all { it >= 0 }
        ) {
            "itemsBySourceIndex contains a negative sourceIndex."
        }

        val candidates =
            effectiveClassificationResult.entries
                .asSequence()
                .filter(::isUnresolvedSemanticTypoCandidate)
                .sortedBy { it.sourceIndex }
                .toList()

        val effectiveTypoCandidateCount =
            effectiveClassificationResult
                .countsByClassification[
                CatalogReviewBacklogClassification
                    .TYPO_VARIANT
            ] ?: 0

        require(
            candidates.size ==
                    effectiveTypoCandidateCount
        ) {
            "Semantic typo removal candidate selection is inconsistent. " +
                    "effectiveTypo=$effectiveTypoCandidateCount, " +
                    "selectedCandidates=${candidates.size}."
        }

        val decisions =
            candidates.map { candidate ->
                createDecision(
                    candidate = candidate,
                    itemsBySourceIndex =
                        itemsBySourceIndex
                )
            }

        val removedSourceIndices =
            decisions
                .map { it.sourceIndex }
                .distinct()
                .sorted()

        require(
            removedSourceIndices.size ==
                    decisions.size
        ) {
            "Removal decisions contain duplicate sourceIndex values."
        }

        val missingCandidates =
            removedSourceIndices.filterNot {
                it in itemsBySourceIndex
            }

        require(missingCandidates.isEmpty()) {
            "Semantic typo removal references missing catalog entries: " +
                    missingCandidates.joinToString()
        }

        /*
         * Keine Mutation der Eingabemap. Der Zielbestand wird aus einer
         * deterministisch nach sourceIndex sortierten Map aufgebaut.
         */
        val outputItemsBySourceIndex =
            itemsBySourceIndex
                .asSequence()
                .filterNot { (sourceIndex, _) ->
                    sourceIndex in removedSourceIndices
                }
                .sortedBy { (sourceIndex, _) ->
                    sourceIndex
                }
                .associate { it.toPair() }

        val countsByReason =
            decisions
                .groupingBy { it.reason }
                .eachCount()
                .toList()
                .sortedBy { it.first.name }
                .associate { it }

        return CatalogSemanticTypoRemovalResult(
            version =
                CatalogSemanticTypoRemovalResult
                    .CURRENT_VERSION,

            inputEntryCount =
                itemsBySourceIndex.size,

            candidateEntryCount =
                candidates.size,

            removedEntryCount =
                decisions.size,

            outputEntryCount =
                outputItemsBySourceIndex.size,

            removedSourceIndices =
                removedSourceIndices,

            countsByReason =
                countsByReason,

            decisions =
                decisions,

            outputItemsBySourceIndex =
                outputItemsBySourceIndex,

            valid = true
        )
    }

    private fun isUnresolvedSemanticTypoCandidate(
        entry: ClassifiedCatalogReviewBacklogEntry
    ): Boolean =
        entry.primaryClassification ==
                CatalogReviewBacklogClassification
                    .TYPO_VARIANT

    private fun createDecision(
        candidate: ClassifiedCatalogReviewBacklogEntry,

        itemsBySourceIndex:
        Map<Int, CatalogFoodItem>
    ): CatalogSemanticTypoRemovalDecision {
        val sourceItem =
            requireNotNull(
                itemsBySourceIndex[
                    candidate.sourceIndex
                ]
            ) {
                "Semantic typo candidate ${candidate.sourceIndex} " +
                        "does not exist in normalized catalog."
            }

        val targetSourceIndex =
            candidate.mergeTargetSourceIndex

        val targetItem =
            targetSourceIndex
                ?.let(itemsBySourceIndex::get)

        val reason =
            classifyReason(
                sourceItem = sourceItem,
                targetItem = targetItem,
                candidate = candidate
            )

        val evidence =
            buildEvidence(
                sourceItem = sourceItem,
                targetItem = targetItem,
                candidate = candidate,
                reason = reason
            )

        return CatalogSemanticTypoRemovalDecision(
            sourceIndex =
                candidate.sourceIndex,

            itemName =
                sourceItem.itemname,

            category =
                sourceItem.category,

            normalizedKey =
                sourceItem.normalized,

            previousMergeTargetSourceIndex =
                targetSourceIndex,

            previousMergeTargetName =
                targetItem?.itemname,

            reason =
                reason,

            evidence =
                evidence
        )
    }

    private fun classifyReason(
        sourceItem: CatalogFoodItem,
        targetItem: CatalogFoodItem?,
        candidate: ClassifiedCatalogReviewBacklogEntry
    ): CatalogSemanticTypoRemovalReason {
        if (targetItem == null) {
            return CatalogSemanticTypoRemovalReason
                .UNSAFE_CANONICAL_IDENTITY
        }

        val sourceName =
            normalize(sourceItem.itemname)

        val targetName =
            normalize(targetItem.itemname)

        if (
            isKnownSemanticOpposition(
                sourceName = sourceName,
                targetName = targetName
            )
        ) {
            return CatalogSemanticTypoRemovalReason
                .SEMANTICALLY_DISTINCT_PRODUCT_PAIR
        }

        if (
            isObviouslyIncorrectTarget(
                sourceName = sourceName,
                targetName = targetName
            )
        ) {
            return CatalogSemanticTypoRemovalReason
                .INCORRECT_MERGE_TARGET
        }

        if (
            isAmbiguousGenericPair(
                sourceName = sourceName,
                targetName = targetName
            )
        ) {
            return CatalogSemanticTypoRemovalReason
                .AMBIGUOUS_GENERIC_PRODUCT
        }

        if (
            candidate.hasMultipleConflictReasons
        ) {
            return CatalogSemanticTypoRemovalReason
                .UNSAFE_CANONICAL_IDENTITY
        }

        return CatalogSemanticTypoRemovalReason
            .REDUNDANT_LEGACY_ENTRY
    }

    private fun isKnownSemanticOpposition(
        sourceName: String,
        targetName: String
    ): Boolean {
        val pair =
            setOf(sourceName, targetName)

        if (
            pair.any { "sin carne" in it } &&
            pair.any { "con carne" in it }
        ) {
            return true
        }

        if (
            pair.any { "sahnesauce" in it } &&
            pair.any { "senfsauce" in it }
        ) {
            return true
        }

        if (
            pair.any { "in dose" in it } &&
            pair.any { "in oel" in it }
        ) {
            return true
        }

        return false
    }

    private fun isObviouslyIncorrectTarget(
        sourceName: String,
        targetName: String
    ): Boolean {
        val sourceTokens =
            meaningfulTokens(sourceName)

        val targetTokens =
            meaningfulTokens(targetName)

        if (
            sourceTokens.isEmpty() ||
            targetTokens.isEmpty()
        ) {
            return true
        }

        val sharedTokens =
            sourceTokens intersect targetTokens

        val smallerTokenCount =
            minOf(
                sourceTokens.size,
                targetTokens.size
            )

        /*
         * Kein gemeinsames bedeutungstragendes Token oder nur ein sehr
         * schwacher Resttreffer bei mehreren Tokens deutet auf ein falsches
         * Candidate-Retrieval-Ziel hin.
         */
        return sharedTokens.isEmpty() ||
                (
                        smallerTokenCount >= 2 &&
                                sharedTokens.size == 1 &&
                                sharedTokens.single() in
                                NON_IDENTITY_TOKENS
                        )
    }

    private fun isAmbiguousGenericPair(
        sourceName: String,
        targetName: String
    ): Boolean {
        val sourceTokens =
            meaningfulTokens(sourceName)

        val targetTokens =
            meaningfulTokens(targetName)

        val sourceContainsTarget =
            sourceTokens.containsAll(
                targetTokens
            )

        val targetContainsSource =
            targetTokens.containsAll(
                sourceTokens
            )

        return (
                sourceContainsTarget ||
                        targetContainsSource
                ) &&
                sourceTokens != targetTokens
    }

    private fun buildEvidence(
        sourceItem: CatalogFoodItem,
        targetItem: CatalogFoodItem?,
        candidate: ClassifiedCatalogReviewBacklogEntry,
        reason: CatalogSemanticTypoRemovalReason
    ): List<String> =
        buildList {
            add(
                "Entry remained classified as TYPO_VARIANT after all " +
                        "deterministic typo rules were applied."
            )

            add(
                "Effective automation assessment is " +
                        "${candidate.automationAssessment.name}."
            )

            add(
                "Removal reason is ${reason.name}."
            )

            targetItem?.let {
                add(
                    "Previous merge target was sourceIndex " +
                            "${candidate.mergeTargetSourceIndex} " +
                            "('${it.itemname}')."
                )

                if (
                    !sourceItem.category
                        .orEmpty()
                        .trim()
                        .equals(
                            it.category
                                .orEmpty()
                                .trim(),
                            ignoreCase = true
                        )
                ) {
                    add(
                        "Source and target categories differ: " +
                                "'${sourceItem.category.orEmpty()}' versus " +
                                "'${it.category.orEmpty()}'."
                    )
                }
            }

            addAll(
                candidate.classificationReasons
            )

            addAll(
                candidate.originalReasons
            )
        }
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()

    private fun meaningfulTokens(
        value: String
    ): Set<String> =
        normalize(value)
            .split(' ')
            .map(String::trim)
            .filter(String::isNotBlank)
            .filterNot {
                it in NON_IDENTITY_TOKENS
            }
            .toSortedSet()

    private fun normalize(
        value: String
    ): String =
        value
            .trim()
            .lowercase(Locale.GERMAN)
            .replace("ä", "ae")
            .replace("ö", "oe")
            .replace("ü", "ue")
            .replace("ß", "ss")
            .replace(
                NON_LEXICAL_REGEX,
                " "
            )
            .replace(
                MULTIPLE_WHITESPACE_REGEX,
                " "
            )
            .trim()

    private companion object {

        val NON_LEXICAL_REGEX =
            Regex("[^\\p{L}\\p{M}\\p{N}]+")

        val MULTIPLE_WHITESPACE_REGEX =
            Regex("\\s+")

        val NON_IDENTITY_TOKENS = setOf(
            "in",
            "mit",
            "ohne",
            "und",
            "der",
            "die",
            "das",
            "fertig",
            "fertige",
            "fertiges",
            "fertiggericht"
        )
    }
}