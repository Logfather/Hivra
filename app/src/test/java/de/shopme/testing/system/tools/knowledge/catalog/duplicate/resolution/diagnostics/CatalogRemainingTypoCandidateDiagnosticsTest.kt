package de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution.diagnostics

import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationAction
import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationPlan
import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationPlanEntry
import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.model.IndexedCatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.review.CatalogReviewBacklogStatus
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewAutomationAssessment
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassification
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassificationResult
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.ClassifiedCatalogReviewBacklogEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogRemainingTypoCandidateDiagnosticsTest {

    private val diagnostics =
        CatalogRemainingTypoCandidateDiagnostics()

    @Test
    fun diagnoseCompoundSpacingVariant() {
        val result = diagnostics.diagnose(
            classificationResult =
                classificationResult(
                    classifiedEntry(
                        sourceIndex = 2,
                        itemName = "Basmatireis",
                        targetSourceIndex = 1
                    )
                ),

            sourceEntries = listOf(
                indexedItem(
                    sourceIndex = 1,
                    name = "Basmati Reis",
                    category = "rice"
                ),
                indexedItem(
                    sourceIndex = 2,
                    name = "Basmatireis",
                    category = "rice"
                )
            ),

            plan = plan(
                planEntry(
                    sourceIndex = 2,
                    itemName = "Basmatireis",
                    targetSourceIndex = 1
                )
            )
        )

        val entry = result.entries.single()

        assertEquals(
            CatalogTypoCandidateSubtype
                .COMPOUND_SPACING_VARIANT,
            entry.subtype
        )

        assertEquals(
            CatalogTypoCandidateRecommendation
                .IMPLEMENT_COMPOUND_SPACING_RESOLVER,
            entry.recommendation
        )
    }

    @Test
    fun diagnoseTypTypeVariant() {
        val result = diagnostics.diagnose(
            classificationResult =
                classificationResult(
                    classifiedEntry(
                        sourceIndex = 2,
                        itemName =
                            "Weizenmehl Type 405",
                        targetSourceIndex = 1
                    )
                ),

            sourceEntries = listOf(
                indexedItem(
                    sourceIndex = 1,
                    name =
                        "Weizenmehl Typ 405",
                    category = "flour"
                ),
                indexedItem(
                    sourceIndex = 2,
                    name =
                        "Weizenmehl Type 405",
                    category = "flour"
                )
            ),

            plan = plan(
                planEntry(
                    sourceIndex = 2,
                    itemName =
                        "Weizenmehl Type 405",
                    targetSourceIndex = 1
                )
            )
        )

        assertEquals(
            CatalogTypoCandidateSubtype
                .TYP_TYPE_VARIANT,
            result.entries.single().subtype
        )
    }

    @Test
    fun diagnoseNumericMismatch() {
        val result = diagnostics.diagnose(
            classificationResult =
                classificationResult(
                    classifiedEntry(
                        sourceIndex = 2,
                        itemName =
                            "Weizenmehl Typ 550",
                        targetSourceIndex = 1
                    )
                ),

            sourceEntries = listOf(
                indexedItem(
                    sourceIndex = 1,
                    name =
                        "Weizenmehl Typ 405",
                    category = "flour"
                ),
                indexedItem(
                    sourceIndex = 2,
                    name =
                        "Weizenmehl Typ 550",
                    category = "flour"
                )
            ),

            plan = plan(
                planEntry(
                    sourceIndex = 2,
                    itemName =
                        "Weizenmehl Typ 550",
                    targetSourceIndex = 1
                )
            )
        )

        val entry = result.entries.single()

        assertEquals(
            CatalogTypoCandidateSubtype
                .NUMERIC_VARIANT,
            entry.subtype
        )

        assertTrue(
            CatalogTypoCandidateDiagnosticReason
                .NUMERIC_TOKEN_MISMATCH in
                    entry.diagnosticReasons
        )
    }

    @Test
    fun diagnoseDeterministically() {
        val classification =
            classificationResult(
                classifiedEntry(
                    sourceIndex = 2,
                    itemName = "Basamti Reis",
                    targetSourceIndex = 1
                )
            )

        val sourceEntries = listOf(
            indexedItem(
                sourceIndex = 1,
                name = "Basmati Reis",
                category = "rice"
            ),
            indexedItem(
                sourceIndex = 2,
                name = "Basamti Reis",
                category = "rice"
            )
        )

        val plan = plan(
            planEntry(
                sourceIndex = 2,
                itemName = "Basamti Reis",
                targetSourceIndex = 1
            )
        )

        val first = diagnostics.diagnose(
            classification,
            sourceEntries,
            plan
        )

        val second = diagnostics.diagnose(
            classification,
            sourceEntries,
            plan
        )

        assertEquals(first, second)
    }

    @Test
    fun diagnoseHyphenationVariant() {
        val result = diagnostics.diagnose(
            classificationResult =
                classificationResult(
                    classifiedEntry(
                        sourceIndex = 2,
                        itemName = "Curry-Sauce",
                        targetSourceIndex = 1
                    )
                ),

            sourceEntries = listOf(
                indexedItem(
                    sourceIndex = 1,
                    name = "Curry Sauce",
                    category = "sauces"
                ),
                indexedItem(
                    sourceIndex = 2,
                    name = "Curry-Sauce",
                    category = "sauces"
                )
            ),

            plan = plan(
                planEntry(
                    sourceIndex = 2,
                    itemName = "Curry-Sauce",
                    targetSourceIndex = 1
                )
            )
        )

        val entry = result.entries.single()

        assertEquals(
            CatalogTypoCandidateSubtype
                .HYPHENATION_VARIANT,
            entry.subtype
        )

        assertEquals(
            CatalogTypoCandidateRecommendation
                .IMPLEMENT_HYPHENATION_RESOLVER,
            entry.recommendation
        )
    }

    private fun indexedItem(
        sourceIndex: Int,
        name: String,
        category: String
    ): IndexedCatalogFoodItem =
        IndexedCatalogFoodItem(
            sourceIndex = sourceIndex,
            item = CatalogFoodItem(
                itemname = name,
                category = category,
                production = "Standard",
                normalized =
                    name.lowercase()
                        .replace(" ", "-"),
                plural = name,
                colloquial = emptyList(),
                phoneticTokens = emptyList(),
                autocompleteTokens =
                    emptyList(),
                normalizedEnglish = null
            )
        )

    private fun classifiedEntry(
        sourceIndex: Int,
        itemName: String,
        targetSourceIndex: Int
    ): ClassifiedCatalogReviewBacklogEntry =
        ClassifiedCatalogReviewBacklogEntry(
            sourceIndex = sourceIndex,
            itemName = itemName,
            category = "rice",
            normalizedKey =
                itemName.lowercase()
                    .replace(" ", "-"),
            originalAction =
                CatalogCanonicalizationAction.MERGE,
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
                    .POTENTIALLY_DETERMINISTIC,
            hasUniqueMergeTarget = true,
            hasMultipleConflictReasons = false,
            originalReasons =
                typoReasons(targetSourceIndex),
            classificationReasons =
                listOf(
                    "Automation assessment is POTENTIALLY_DETERMINISTIC.",
                    "Primary review classification is TYPO_VARIANT.",
                    "The review entry contains one explicit merge target."
                ).sorted()
        )

    private fun classificationResult(
        vararg entries:
        ClassifiedCatalogReviewBacklogEntry
    ): CatalogReviewBacklogClassificationResult {
        val sorted =
            entries.sortedBy { it.sourceIndex }

        return CatalogReviewBacklogClassificationResult(
            version =
                CatalogReviewBacklogClassificationResult
                    .CURRENT_VERSION,
            sourceBacklogEntryCount =
                sorted.size,
            classifiedEntryCount =
                sorted.size,
            potentiallyDeterministicCount =
                sorted.size,
            manualReviewRequiredCount = 0,
            conflictingEvidenceCount = 0,
            splitRequiredCount = 0,
            entriesWithUniqueMergeTargetCount =
                sorted.size,
            entriesWithMultipleConflictReasonsCount =
                0,
            countsByClassification =
                if (sorted.isEmpty()) {
                    emptyMap()
                } else {
                    mapOf(
                        CatalogReviewBacklogClassification
                            .TYPO_VARIANT to
                                sorted.size
                    )
                },
            countsByAutomationAssessment =
                if (sorted.isEmpty()) {
                    emptyMap()
                } else {
                    mapOf(
                        CatalogReviewAutomationAssessment
                            .POTENTIALLY_DETERMINISTIC to
                                sorted.size
                    )
                },
            countsByCategory =
                if (sorted.isEmpty()) {
                    emptyMap()
                } else {
                    mapOf(
                        "rice" to sorted.size
                    )
                },
            entries = sorted,
            valid = true
        )
    }

    private fun planEntry(
        sourceIndex: Int,
        itemName: String,
        targetSourceIndex: Int
    ): CatalogCanonicalizationPlanEntry =
        CatalogCanonicalizationPlanEntry(
            sourceIndex = sourceIndex,
            originalItemName = itemName,
            action =
                CatalogCanonicalizationAction.MERGE,
            proposedCanonicalName = null,
            proposedNormalizedKey = null,
            proposedCategory = null,
            mergeTargetSourceIndex =
                targetSourceIndex,
            automatic = false,
            reasons =
                typoReasons(targetSourceIndex),
            confidence = 0.50
        )

    private fun plan(
        vararg entries:
        CatalogCanonicalizationPlanEntry
    ): CatalogCanonicalizationPlan {
        val sorted =
            entries.sortedBy { it.sourceIndex }

        return CatalogCanonicalizationPlan(
            version = 1,
            inputEntryCount = sorted.size,
            planEntryCount = sorted.size,
            automaticActionCount = 0,
            reviewActionCount =
                sorted.count {
                    it.action ==
                            CatalogCanonicalizationAction
                                .REVIEW
                },
            unchangedEntryCount = 0,
            actionCounts =
                sorted
                    .groupingBy { it.action }
                    .eachCount(),
            affectedSourceIndices =
                sorted.map { it.sourceIndex },
            entries = sorted,
            valid = true
        )
    }

    private fun typoReasons(
        targetSourceIndex: Int
    ): List<String> =
        listOf(
            "Canonical duplicate target is sourceIndex " +
                    "$targetSourceIndex.",
            "Duplicate reasons: TYPO_VARIANT.",
            "Duplicate recommendation: MERGE_AFTER_REVIEW.",
            "Entry belongs to duplicate group 'test'."
        ).sorted()
}