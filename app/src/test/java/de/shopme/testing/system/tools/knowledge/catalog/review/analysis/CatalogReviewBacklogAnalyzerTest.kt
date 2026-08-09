package de.shopme.testing.system.tools.knowledge.catalog.review.analysis

import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationAction
import de.shopme.testing.system.tools.knowledge.catalog.review.CatalogReviewBacklogStatus
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewAutomationAssessment
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassification
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassificationResult
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.ClassifiedCatalogReviewBacklogEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CatalogReviewBacklogAnalyzerTest {

    private val analyzer =
        CatalogReviewBacklogAnalyzer()

    @Test
    fun analyzeClassificationDistribution() {
        val result = analyzer.analyze(
            classificationResult = classificationResult(
                entry(
                    sourceIndex = 1,
                    classification =
                        CatalogReviewBacklogClassification
                            .TYPO_VARIANT,
                    assessment =
                        CatalogReviewAutomationAssessment
                            .POTENTIALLY_DETERMINISTIC,
                    category = "vegetables",
                    mergeTargetSourceIndex = 10
                ),
                entry(
                    sourceIndex = 2,
                    classification =
                        CatalogReviewBacklogClassification
                            .TYPO_VARIANT,
                    assessment =
                        CatalogReviewAutomationAssessment
                            .POTENTIALLY_DETERMINISTIC,
                    category = "vegetables",
                    mergeTargetSourceIndex = 11
                ),
                entry(
                    sourceIndex = 3,
                    classification =
                        CatalogReviewBacklogClassification
                            .SEMANTIC_CATEGORY_CONFLICT,
                    assessment =
                        CatalogReviewAutomationAssessment
                            .MANUAL_REVIEW_REQUIRED,
                    category = "dairy"
                )
            )
        )

        assertEquals(3, result.classifiedEntryCount)
        assertEquals(2, result.classificationCount)
        assertEquals(2, result.affectedCategoryCount)

        assertEquals(
            2,
            result.countsByClassification[
                CatalogReviewBacklogClassification
                    .TYPO_VARIANT
            ]
        )

        assertEquals(
            1,
            result.countsByClassification[
                CatalogReviewBacklogClassification
                    .SEMANTIC_CATEGORY_CONFLICT
            ]
        )
    }

    @Test
    fun prioritizePotentiallyDeterministicClassification() {
        val entries = (1..12).map { sourceIndex ->
            entry(
                sourceIndex = sourceIndex,
                classification =
                    CatalogReviewBacklogClassification
                        .TYPO_VARIANT,
                assessment =
                    CatalogReviewAutomationAssessment
                        .POTENTIALLY_DETERMINISTIC,
                category = "vegetables",
                mergeTargetSourceIndex =
                    sourceIndex + 100
            )
        }

        val result = analyzer.analyze(
            classificationResult =
                classificationResult(
                    *entries.toTypedArray()
                )
        )

        val analysis =
            result.classificationAnalyses.single()

        assertEquals(
            CatalogReviewResolutionPriority.HIGH,
            analysis.priority
        )

        assertEquals(
            CatalogReviewResolutionRecommendation
                .IMPLEMENT_TYPO_VARIANT_RESOLVER,
            result.nextRecommendedImplementation
        )
    }

    @Test
    fun preserveManualOnlySemanticConflicts() {
        val result = analyzer.analyze(
            classificationResult = classificationResult(
                entry(
                    sourceIndex = 20,
                    classification =
                        CatalogReviewBacklogClassification
                            .SEMANTIC_CATEGORY_CONFLICT,
                    assessment =
                        CatalogReviewAutomationAssessment
                            .MANUAL_REVIEW_REQUIRED,
                    category = "dairy"
                )
            )
        )

        val analysis =
            result.classificationAnalyses.single()

        assertEquals(
            CatalogReviewResolutionPriority.MANUAL_ONLY,
            analysis.priority
        )

        assertEquals(
            CatalogReviewResolutionRecommendation
                .REVIEW_SEMANTIC_CATEGORY_CONFLICTS,
            analysis.recommendation
        )
    }

    @Test
    fun analyzeDominantCategoryClassification() {
        val result = analyzer.analyze(
            classificationResult = classificationResult(
                entry(
                    sourceIndex = 1,
                    classification =
                        CatalogReviewBacklogClassification
                            .TYPO_VARIANT,
                    assessment =
                        CatalogReviewAutomationAssessment
                            .POTENTIALLY_DETERMINISTIC,
                    category = "vegetables",
                    mergeTargetSourceIndex = 20
                ),
                entry(
                    sourceIndex = 2,
                    classification =
                        CatalogReviewBacklogClassification
                            .TYPO_VARIANT,
                    assessment =
                        CatalogReviewAutomationAssessment
                            .POTENTIALLY_DETERMINISTIC,
                    category = "vegetables",
                    mergeTargetSourceIndex = 21
                ),
                entry(
                    sourceIndex = 3,
                    classification =
                        CatalogReviewBacklogClassification
                            .WORD_ORDER_VARIANT,
                    assessment =
                        CatalogReviewAutomationAssessment
                            .POTENTIALLY_DETERMINISTIC,
                    category = "vegetables",
                    mergeTargetSourceIndex = 22
                )
            )
        )

        val categoryAnalysis =
            result.categoryAnalyses.single()

        assertEquals(
            "vegetables",
            categoryAnalysis.category
        )

        assertEquals(
            CatalogReviewBacklogClassification
                .TYPO_VARIANT,
            categoryAnalysis.dominantClassification
        )
    }

    @Test
    fun analyzeEmptyClassificationResult() {
        val result = analyzer.analyze(
            classificationResult =
                classificationResult()
        )

        assertEquals(0, result.classifiedEntryCount)
        assertEquals(0, result.classificationCount)
        assertEquals(0, result.affectedCategoryCount)
        assertEquals(
            emptyList(),
            result.prioritizedRecommendations
        )
        assertEquals(
            null,
            result.nextRecommendedImplementation
        )
        assertTrue(result.valid)
    }

    @Test
    fun analyzeDeterministically() {
        val classificationResult =
            classificationResult(
                entry(
                    sourceIndex = 9,
                    classification =
                        CatalogReviewBacklogClassification
                            .PUNCTUATION_VARIANT,
                    assessment =
                        CatalogReviewAutomationAssessment
                            .POTENTIALLY_DETERMINISTIC,
                    category = "sauces",
                    mergeTargetSourceIndex = 3
                ),
                entry(
                    sourceIndex = 2,
                    classification =
                        CatalogReviewBacklogClassification
                            .SEMANTIC_CATEGORY_CONFLICT,
                    assessment =
                        CatalogReviewAutomationAssessment
                            .MANUAL_REVIEW_REQUIRED,
                    category = "dairy"
                )
            )

        val first =
            analyzer.analyze(classificationResult)

        val second =
            analyzer.analyze(classificationResult)

        assertEquals(first, second)
        assertNotNull(first.nextRecommendedImplementation)
    }

    private fun entry(
        sourceIndex: Int,
        classification:
        CatalogReviewBacklogClassification,
        assessment:
        CatalogReviewAutomationAssessment,
        category: String,
        mergeTargetSourceIndex: Int? = null
    ): ClassifiedCatalogReviewBacklogEntry =
        ClassifiedCatalogReviewBacklogEntry(
            sourceIndex = sourceIndex,
            itemName = "Item $sourceIndex",
            category = category,
            normalizedKey = "item-$sourceIndex",
            originalAction =
                if (mergeTargetSourceIndex == null) {
                    CatalogCanonicalizationAction.REVIEW
                } else {
                    CatalogCanonicalizationAction.MERGE
                },
            backlogStatus =
                CatalogReviewBacklogStatus
                    .STILL_REVIEW_REQUIRED,
            mergeTargetSourceIndex =
                mergeTargetSourceIndex,
            primaryClassification =
                classification,
            detectedClassifications =
                listOf(classification),
            automationAssessment =
                assessment,
            hasUniqueMergeTarget =
                mergeTargetSourceIndex != null,
            hasMultipleConflictReasons = false,
            originalReasons =
                listOf(
                    "Test reason for ${classification.name}."
                ),
            classificationReasons =
                listOf(
                    "Automation assessment is ${assessment.name}.",
                    "Primary review classification is " +
                            "${classification.name}.",
                    if (mergeTargetSourceIndex == null) {
                        "The review entry contains no explicit merge target."
                    } else {
                        "The review entry contains one explicit merge target."
                    }
                ).sorted()
        )

    private fun classificationResult(
        vararg entries:
        ClassifiedCatalogReviewBacklogEntry
    ): CatalogReviewBacklogClassificationResult {
        val sortedEntries =
            entries.sortedBy { it.sourceIndex }

        val countsByClassification =
            sortedEntries
                .groupingBy { it.primaryClassification }
                .eachCount()
                .toList()
                .sortedBy { it.first.name }
                .associate { it }

        val countsByAssessment =
            sortedEntries
                .groupingBy { it.automationAssessment }
                .eachCount()
                .toList()
                .sortedBy { it.first.name }
                .associate { it }

        val countsByCategory =
            sortedEntries
                .groupingBy {
                    requireNotNull(it.category)
                }
                .eachCount()
                .toSortedMap()

        return CatalogReviewBacklogClassificationResult(
            version =
                CatalogReviewBacklogClassificationResult
                    .CURRENT_VERSION,
            sourceBacklogEntryCount =
                sortedEntries.size,
            classifiedEntryCount =
                sortedEntries.size,
            potentiallyDeterministicCount =
                sortedEntries.count {
                    it.automationAssessment ==
                            CatalogReviewAutomationAssessment
                                .POTENTIALLY_DETERMINISTIC
                },
            manualReviewRequiredCount =
                sortedEntries.count {
                    it.automationAssessment ==
                            CatalogReviewAutomationAssessment
                                .MANUAL_REVIEW_REQUIRED
                },
            conflictingEvidenceCount =
                sortedEntries.count {
                    it.automationAssessment ==
                            CatalogReviewAutomationAssessment
                                .CONFLICTING_EVIDENCE
                },
            splitRequiredCount =
                sortedEntries.count {
                    it.automationAssessment ==
                            CatalogReviewAutomationAssessment
                                .SPLIT_REQUIRED
                },
            entriesWithUniqueMergeTargetCount =
                sortedEntries.count {
                    it.hasUniqueMergeTarget
                },
            entriesWithMultipleConflictReasonsCount =
                sortedEntries.count {
                    it.hasMultipleConflictReasons
                },
            countsByClassification =
                countsByClassification,
            countsByAutomationAssessment =
                countsByAssessment,
            countsByCategory =
                countsByCategory,
            entries = sortedEntries,
            valid = true
        )
    }
}