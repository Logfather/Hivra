package de.shopme.testing.system.tools.knowledge.catalog.review.classification

import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationAction
import de.shopme.testing.system.tools.knowledge.catalog.review.CatalogReviewBacklogEntry
import de.shopme.testing.system.tools.knowledge.catalog.review.CatalogReviewBacklogEvaluationResult
import de.shopme.testing.system.tools.knowledge.catalog.review.CatalogReviewBacklogStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogReviewBacklogClassifierTest {

    private val classifier =
        CatalogReviewBacklogClassifier()

    @Test
    fun classifySemanticCategoryConflict() {
        val result = classifier.classify(
            backlogResult = backlogResult(
                entry(
                    sourceIndex = 10,
                    itemName = "Mozzarella",
                    category = "dairy",
                    action =
                        CatalogCanonicalizationAction.REVIEW,
                    status =
                        CatalogReviewBacklogStatus
                            .STILL_REVIEW_REQUIRED,
                    reasons = listOf(
                        "Catalog item 'Mozzarella' contains stronger " +
                                "lexical evidence for root category 'cheese'."
                    )
                )
            )
        )

        val classified = result.entries.single()

        assertEquals(
            CatalogReviewBacklogClassification
                .SEMANTIC_CATEGORY_CONFLICT,
            classified.primaryClassification
        )

        assertEquals(
            CatalogReviewAutomationAssessment
                .MANUAL_REVIEW_REQUIRED,
            classified.automationAssessment
        )
    }

    @Test
    fun classifyPotentiallyDeterministicTypoVariant() {
        val result = classifier.classify(
            backlogResult = backlogResult(
                entry(
                    sourceIndex = 20,
                    itemName = "Weis(s)kohl",
                    category = "vegetables",
                    action =
                        CatalogCanonicalizationAction.MERGE,
                    status =
                        CatalogReviewBacklogStatus
                            .STILL_REVIEW_REQUIRED,
                    mergeTargetSourceIndex = 5,
                    reasons = listOf(
                        "Duplicate reasons: TYPO_VARIANT.",
                        "Canonical duplicate target is sourceIndex 5."
                    )
                )
            )
        )

        val classified = result.entries.single()

        assertEquals(
            CatalogReviewBacklogClassification
                .TYPO_VARIANT,
            classified.primaryClassification
        )

        assertEquals(
            CatalogReviewAutomationAssessment
                .POTENTIALLY_DETERMINISTIC,
            classified.automationAssessment
        )

        assertTrue(classified.hasUniqueMergeTarget)
    }

    @Test
    fun classifyMultipleDuplicateReasonsAsConflict() {
        val result = classifier.classify(
            backlogResult = backlogResult(
                entry(
                    sourceIndex = 30,
                    itemName = "Produkt",
                    category = "snacks",
                    action =
                        CatalogCanonicalizationAction.MERGE,
                    status =
                        CatalogReviewBacklogStatus
                            .STILL_REVIEW_REQUIRED,
                    mergeTargetSourceIndex = 7,
                    reasons = listOf(
                        "Duplicate reasons: TYPO_VARIANT, " +
                                "POSSIBLE_SEMANTIC_DUPLICATE."
                    )
                )
            )
        )

        val classified = result.entries.single()

        assertEquals(
            CatalogReviewBacklogClassification
                .MULTIPLE_DUPLICATE_REASONS,
            classified.primaryClassification
        )

        assertEquals(
            CatalogReviewAutomationAssessment
                .CONFLICTING_EVIDENCE,
            classified.automationAssessment
        )

        assertTrue(
            classified.hasMultipleConflictReasons
        )
    }

    @Test
    fun classifySplitAction() {
        val result = classifier.classify(
            backlogResult = backlogResult(
                entry(
                    sourceIndex = 40,
                    itemName = "Obst und Gemüse Mix",
                    category = "vegetables",
                    action =
                        CatalogCanonicalizationAction.SPLIT,
                    status =
                        CatalogReviewBacklogStatus
                            .STILL_SPLIT_REQUIRED,
                    reasons = listOf(
                        "Entry contains multiple foods and requires split."
                    )
                )
            )
        )

        val classified = result.entries.single()

        assertEquals(
            CatalogReviewBacklogClassification
                .SPLIT_REQUIRED,
            classified.primaryClassification
        )

        assertEquals(
            CatalogReviewAutomationAssessment
                .SPLIT_REQUIRED,
            classified.automationAssessment
        )
    }

    @Test
    fun classifyEveryUnresolvedEntryDeterministically() {
        val backlog = backlogResult(
            entry(
                sourceIndex = 8,
                itemName = "Curry Sauce",
                category = "sauces",
                action =
                    CatalogCanonicalizationAction.MERGE,
                status =
                    CatalogReviewBacklogStatus
                        .STILL_REVIEW_REQUIRED,
                mergeTargetSourceIndex = 3,
                reasons = listOf(
                    "Duplicate reasons: PUNCTUATION_VARIANT."
                )
            ),
            entry(
                sourceIndex = 2,
                itemName = "Oregano",
                category = "spices",
                action =
                    CatalogCanonicalizationAction.REVIEW,
                status =
                    CatalogReviewBacklogStatus
                        .STILL_REVIEW_REQUIRED,
                reasons = listOf(
                    "Catalog item contains stronger lexical evidence " +
                            "for root category 'herbs'."
                )
            )
        )

        val first = classifier.classify(backlog)
        val second = classifier.classify(backlog)

        assertEquals(first, second)

        assertEquals(
            listOf(2, 8),
            first.entries.map { it.sourceIndex }
        )

        assertEquals(
            first.classifiedEntryCount,
            first.countsByClassification.values.sum()
        )
    }

    private fun entry(
        sourceIndex: Int,
        itemName: String,
        category: String,
        action: CatalogCanonicalizationAction,
        status: CatalogReviewBacklogStatus,
        mergeTargetSourceIndex: Int? = null,
        reasons: List<String>
    ): CatalogReviewBacklogEntry =
        CatalogReviewBacklogEntry(
            sourceIndex = sourceIndex,
            originalItemName = itemName,
            originalCategory = category,
            originalNormalizedKey =
                itemName.lowercase().replace(" ", "-"),
            originalAction = action,
            originalAutomatic = false,
            resultingItemName = itemName,
            resultingCategory = category,
            resultingNormalizedKey =
                itemName.lowercase().replace(" ", "-"),
            mergeTargetSourceIndex =
                mergeTargetSourceIndex,
            originalReasons = reasons
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .sorted(),
            status = status,
            resolutionReason =
                "Test unresolved review reason."
        )

    private fun backlogResult(
        vararg entries: CatalogReviewBacklogEntry
    ): CatalogReviewBacklogEvaluationResult {
        val sortedEntries = entries
            .sortedBy { it.sourceIndex }

        val stillReviewCount = sortedEntries.count {
            it.status ==
                    CatalogReviewBacklogStatus
                        .STILL_REVIEW_REQUIRED
        }

        val stillSplitCount = sortedEntries.count {
            it.status ==
                    CatalogReviewBacklogStatus
                        .STILL_SPLIT_REQUIRED
        }

        val statusCounts = sortedEntries
            .groupingBy { it.status }
            .eachCount()
            .toList()
            .sortedBy { it.first.name }
            .associate { it }

        return CatalogReviewBacklogEvaluationResult(
            version =
                CatalogReviewBacklogEvaluationResult
                    .CURRENT_VERSION,

            originalPlanEntryCount =
                sortedEntries.size,

            originalManualReviewCount =
                sortedEntries.size,

            evaluatedBacklogEntryCount =
                sortedEntries.size,

            resolvedEntryCount = 0,
            unresolvedEntryCount =
                sortedEntries.size,

            resolvedByNormalizationCount = 0,
            resolvedByCategoryMigrationCount = 0,
            resolvedByDuplicateMergeCount = 0,
            resolvedByRemovalCount = 0,

            stillReviewRequiredCount =
                stillReviewCount,

            stillSplitRequiredCount =
                stillSplitCount,

            statusCounts = statusCounts,
            entries = sortedEntries,
            valid = true
        )
    }
}