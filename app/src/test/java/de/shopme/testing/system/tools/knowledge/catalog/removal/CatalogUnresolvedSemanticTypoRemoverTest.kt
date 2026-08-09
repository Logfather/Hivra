package de.shopme.testing.system.tools.knowledge.catalog.removal

import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationAction
import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.review.CatalogReviewBacklogStatus
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewAutomationAssessment
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassification
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassificationResult
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.ClassifiedCatalogReviewBacklogEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CatalogUnresolvedSemanticTypoRemoverTest {

    private val remover =
        CatalogUnresolvedSemanticTypoRemover()

    @Test
    fun removeManualSemanticTypoCandidate() {
        val result =
            remover.remove(
                itemsBySourceIndex =
                    mapOf(
                        10 to item(
                            "Chili con Carne",
                            "ready-meals"
                        ),
                        20 to item(
                            "Chili sin Carne",
                            "ready-meals"
                        )
                    ),

                effectiveClassificationResult =
                    classificationResult(
                        semanticTypoEntry(
                            sourceIndex = 20,
                            itemName =
                                "Chili sin Carne",
                            category =
                                "ready-meals",
                            targetSourceIndex = 10
                        )
                    )
            )

        assertEquals(1, result.removedEntryCount)
        assertEquals(1, result.outputEntryCount)

        assertTrue(10 in result.outputItemsBySourceIndex)
        assertFalse(20 in result.outputItemsBySourceIndex)

        assertEquals(
            CatalogSemanticTypoRemovalReason
                .SEMANTICALLY_DISTINCT_PRODUCT_PAIR,
            result.decisions.single().reason
        )
    }

    @Test
    fun preserveNonTypoReviewEntry() {
        val result =
            remover.remove(
                itemsBySourceIndex =
                    mapOf(
                        10 to item(
                            "Produkt A",
                            "ready-meals"
                        )
                    ),

                effectiveClassificationResult =
                    classificationResult(
                        semanticTypoEntry(
                            sourceIndex = 10,
                            itemName = "Produkt A",
                            category = "ready-meals",
                            targetSourceIndex = 20
                        ).copy(
                            primaryClassification =
                                CatalogReviewBacklogClassification
                                    .POSSIBLE_SEMANTIC_DUPLICATE,

                            detectedClassifications =
                                listOf(
                                    CatalogReviewBacklogClassification
                                        .POSSIBLE_SEMANTIC_DUPLICATE
                                )
                        )
                    )
            )

        assertEquals(0, result.removedEntryCount)
        assertEquals(1, result.outputEntryCount)
        assertTrue(10 in result.outputItemsBySourceIndex)
    }

    @Test
    fun removeEffectiveTypoCandidateRegardlessOfAutomationAssessment() {
        val sourceIndex = 10
        val targetSourceIndex = 20

        val result =
            remover.remove(
                itemsBySourceIndex =
                    mapOf(
                        sourceIndex to
                                item(
                                    "Chili sin Carne",
                                    "ready-meals"
                                ),
                        targetSourceIndex to
                                item(
                                    "Chili con Carne",
                                    "ready-meals"
                                )
                    ),

                effectiveClassificationResult =
                    classificationResult(
                        semanticTypoEntry(
                            sourceIndex = sourceIndex,
                            itemName =
                                "Chili sin Carne",
                            category =
                                "ready-meals",
                            targetSourceIndex =
                                targetSourceIndex
                        ).copy(
                            automationAssessment =
                                CatalogReviewAutomationAssessment
                                    .POTENTIALLY_DETERMINISTIC
                        )
                    )
            )

        assertEquals(
            1,
            result.candidateEntryCount
        )

        assertEquals(
            1,
            result.removedEntryCount
        )

        assertFalse(
            sourceIndex in
                    result.outputItemsBySourceIndex
        )

        assertTrue(
            targetSourceIndex in
                    result.outputItemsBySourceIndex
        )
    }

    @Test
    fun removeDeterministically() {
        val items =
            mapOf(
                30 to item(
                    "Mangos in Dose",
                    "canned-food"
                ),
                10 to item(
                    "Mais in Dosen",
                    "canned-food"
                ),
                20 to item(
                    "Chili sin Carne",
                    "ready-meals"
                ),
                40 to item(
                    "Chili con Carne",
                    "ready-meals"
                )
            )

        val classification =
            classificationResult(
                semanticTypoEntry(
                    sourceIndex = 30,
                    itemName = "Mangos in Dose",
                    category = "canned-food",
                    targetSourceIndex = 10
                ),
                semanticTypoEntry(
                    sourceIndex = 20,
                    itemName = "Chili sin Carne",
                    category = "ready-meals",
                    targetSourceIndex = 40
                )
            )

        val first =
            remover.remove(
                itemsBySourceIndex = items,
                effectiveClassificationResult =
                    classification
            )

        val second =
            remover.remove(
                itemsBySourceIndex = items,
                effectiveClassificationResult =
                    classification
            )

        assertEquals(first, second)

        assertEquals(
            listOf(20, 30),
            first.removedSourceIndices
        )
    }

    private fun item(
        name: String,
        category: String
    ): CatalogFoodItem =
        CatalogFoodItem(
            itemname = name,
            category = category,
            production = "Standard",
            normalized =
                name.lowercase()
                    .replace(" ", "-"),
            plural = name,
            colloquial = emptyList(),
            phoneticTokens = emptyList(),
            autocompleteTokens = emptyList(),
            normalizedEnglish = null
        )

    private fun semanticTypoEntry(
        sourceIndex: Int,
        itemName: String,
        category: String,
        targetSourceIndex: Int
    ): ClassifiedCatalogReviewBacklogEntry =
        ClassifiedCatalogReviewBacklogEntry(
            sourceIndex = sourceIndex,
            itemName = itemName,
            category = category,
            normalizedKey =
                itemName.lowercase(),
            originalAction =
                CatalogCanonicalizationAction.REVIEW,
            backlogStatus =
                CatalogReviewBacklogStatus
                    .STILL_REVIEW_REQUIRED,
            mergeTargetSourceIndex =
                targetSourceIndex,
            primaryClassification =
                CatalogReviewBacklogClassification
                    .TYPO_VARIANT,
            detectedClassifications =
                listOf(
                    CatalogReviewBacklogClassification
                        .TYPO_VARIANT
                ),
            automationAssessment =
                CatalogReviewAutomationAssessment
                    .MANUAL_REVIEW_REQUIRED,
            hasUniqueMergeTarget = true,
            hasMultipleConflictReasons = false,
            originalReasons =
                listOf(
                    "Duplicate reasons: TYPO_VARIANT."
                ),
            classificationReasons =
                listOf(
                    "Semantic typo candidate remained unresolved."
                )
        )

    private fun classificationResult(
        vararg entries:
        ClassifiedCatalogReviewBacklogEntry
    ): CatalogReviewBacklogClassificationResult {
        val sorted =
            entries.sortedBy {
                it.sourceIndex
            }

        return CatalogReviewBacklogClassificationResult(
            version =
                CatalogReviewBacklogClassificationResult
                    .CURRENT_VERSION,

            sourceBacklogEntryCount =
                sorted.size,

            classifiedEntryCount =
                sorted.size,

            potentiallyDeterministicCount =
                sorted.count {
                    it.automationAssessment ==
                            CatalogReviewAutomationAssessment
                                .POTENTIALLY_DETERMINISTIC
                },

            manualReviewRequiredCount =
                sorted.count {
                    it.automationAssessment ==
                            CatalogReviewAutomationAssessment
                                .MANUAL_REVIEW_REQUIRED
                },

            conflictingEvidenceCount =
                sorted.count {
                    it.automationAssessment ==
                            CatalogReviewAutomationAssessment
                                .CONFLICTING_EVIDENCE
                },

            splitRequiredCount =
                sorted.count {
                    it.automationAssessment ==
                            CatalogReviewAutomationAssessment
                                .SPLIT_REQUIRED
                },

            entriesWithUniqueMergeTargetCount =
                sorted.count {
                    it.hasUniqueMergeTarget
                },

            entriesWithMultipleConflictReasonsCount =
                sorted.count {
                    it.hasMultipleConflictReasons
                },

            countsByClassification =
                sorted
                    .groupingBy {
                        it.primaryClassification
                    }
                    .eachCount(),

            countsByAutomationAssessment =
                sorted
                    .groupingBy {
                        it.automationAssessment
                    }
                    .eachCount(),

            countsByCategory =
                sorted
                    .groupingBy {
                        it.category
                            ?: "<uncategorized>"
                    }
                    .eachCount()
                    .toSortedMap(),

            entries =
                sorted,

            valid = true
        )
    }
}