package de.shopme.testing.system.tools.knowledge.catalog.review.analysis

import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewAutomationAssessment
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassification
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.CatalogReviewBacklogClassificationResult
import de.shopme.testing.system.tools.knowledge.catalog.review.classification.ClassifiedCatalogReviewBacklogEntry

class CatalogReviewBacklogAnalyzer {

    fun analyze(
        classificationResult:
        CatalogReviewBacklogClassificationResult
    ): CatalogReviewBacklogAnalysisResult {
        require(classificationResult.valid) {
            "Classification result must be valid before analysis."
        }

        val entries = classificationResult.entries
            .sortedBy { it.sourceIndex }

        require(
            entries.size ==
                    classificationResult.classifiedEntryCount
        ) {
            "Classification entries are inconsistent."
        }

        val classificationAnalyses =
            entries
                .groupBy { it.primaryClassification }
                .map {
                        (classification, groupedEntries) ->

                    analyzeClassification(
                        classification = classification,
                        entries = groupedEntries,
                        totalEntryCount = entries.size
                    )
                }
                .sortedWith(
                    compareByDescending<CatalogReviewClassificationAnalysis> {
                        it.entryCount
                    }.thenBy {
                        it.classification.name
                    }
                )

        val categoryAnalyses =
            entries
                .groupBy {
                    it.category
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                        ?: UNCATEGORIZED_CATEGORY
                }
                .map { (category, groupedEntries) ->
                    analyzeCategory(
                        category = category,
                        entries = groupedEntries,
                        totalEntryCount = entries.size
                    )
                }
                .sortedWith(
                    compareByDescending<CatalogReviewCategoryAnalysis> {
                        it.entryCount
                    }.thenBy {
                        it.category
                    }
                )

        val prioritizedRecommendations =
            classificationAnalyses
                .sortedWith(
                    compareBy<CatalogReviewClassificationAnalysis> {
                        priorityRank(it.priority)
                    }.thenByDescending {
                        it.potentiallyDeterministicCount
                    }.thenByDescending {
                        it.entryCount
                    }.thenBy {
                        it.classification.name
                    }
                )
                .map { it.recommendation }
                .distinct()

        val classifiedEntryCount =
            classificationResult.classifiedEntryCount

        val manualOnlyCount =
            classificationResult.manualReviewRequiredCount +
                    classificationResult.conflictingEvidenceCount +
                    classificationResult.splitRequiredCount

        return CatalogReviewBacklogAnalysisResult(
            version =
                CatalogReviewBacklogAnalysisResult
                    .CURRENT_VERSION,

            classifiedEntryCount =
                classifiedEntryCount,

            potentiallyDeterministicCount =
                classificationResult
                    .potentiallyDeterministicCount,

            manualReviewRequiredCount =
                classificationResult
                    .manualReviewRequiredCount,

            conflictingEvidenceCount =
                classificationResult
                    .conflictingEvidenceCount,

            splitRequiredCount =
                classificationResult
                    .splitRequiredCount,

            potentiallyDeterministicShare =
                share(
                    count =
                        classificationResult
                            .potentiallyDeterministicCount,
                    total = classifiedEntryCount
                ),

            manualOnlyShare =
                share(
                    count = manualOnlyCount,
                    total = classifiedEntryCount
                ),

            classificationCount =
                classificationAnalyses.size,

            affectedCategoryCount =
                categoryAnalyses.size,

            countsByClassification =
                classificationResult
                    .countsByClassification
                    .toList()
                    .sortedBy { it.first.name }
                    .associate { it },

            countsByAutomationAssessment =
                classificationResult
                    .countsByAutomationAssessment
                    .toList()
                    .sortedBy { it.first.name }
                    .associate { it },

            classificationAnalyses =
                classificationAnalyses,

            categoryAnalyses =
                categoryAnalyses,

            prioritizedRecommendations =
                prioritizedRecommendations,

            nextRecommendedImplementation =
                prioritizedRecommendations.firstOrNull(),

            valid = true
        )
    }

    private fun analyzeClassification(
        classification:
        CatalogReviewBacklogClassification,
        entries: List<ClassifiedCatalogReviewBacklogEntry>,
        totalEntryCount: Int
    ): CatalogReviewClassificationAnalysis {
        val potentiallyDeterministicCount =
            entries.count {
                it.automationAssessment ==
                        CatalogReviewAutomationAssessment
                            .POTENTIALLY_DETERMINISTIC
            }

        val manualReviewRequiredCount =
            entries.count {
                it.automationAssessment ==
                        CatalogReviewAutomationAssessment
                            .MANUAL_REVIEW_REQUIRED
            }

        val conflictingEvidenceCount =
            entries.count {
                it.automationAssessment ==
                        CatalogReviewAutomationAssessment
                            .CONFLICTING_EVIDENCE
            }

        val splitRequiredCount =
            entries.count {
                it.automationAssessment ==
                        CatalogReviewAutomationAssessment
                            .SPLIT_REQUIRED
            }

        val priority = determinePriority(
            classification = classification,
            entryCount = entries.size,
            potentiallyDeterministicCount =
                potentiallyDeterministicCount
        )

        val recommendation =
            recommendationFor(classification)

        return CatalogReviewClassificationAnalysis(
            classification = classification,
            entryCount = entries.size,
            shareOfBacklog =
                share(entries.size, totalEntryCount),

            potentiallyDeterministicCount =
                potentiallyDeterministicCount,

            manualReviewRequiredCount =
                manualReviewRequiredCount,

            conflictingEvidenceCount =
                conflictingEvidenceCount,

            splitRequiredCount =
                splitRequiredCount,

            entriesWithUniqueMergeTargetCount =
                entries.count {
                    it.hasUniqueMergeTarget
                },

            priority = priority,
            recommendation = recommendation,

            rationale = buildRationale(
                classification = classification,
                entryCount = entries.size,
                potentiallyDeterministicCount =
                    potentiallyDeterministicCount,
                conflictingEvidenceCount =
                    conflictingEvidenceCount,
                splitRequiredCount =
                    splitRequiredCount
            ),

            exampleSourceIndices =
                entries
                    .map { it.sourceIndex }
                    .distinct()
                    .sorted()
                    .take(MAXIMUM_EXAMPLES)
        )
    }

    private fun analyzeCategory(
        category: String,
        entries: List<ClassifiedCatalogReviewBacklogEntry>,
        totalEntryCount: Int
    ): CatalogReviewCategoryAnalysis {
        val countsByClassification =
            entries
                .groupingBy { it.primaryClassification }
                .eachCount()
                .toList()
                .sortedBy { it.first.name }
                .associate { it }

        val dominantClassification =
            countsByClassification
                .entries
                .sortedWith(
                    compareByDescending<
                            Map.Entry<
                                    CatalogReviewBacklogClassification,
                                    Int
                                    >
                            > {
                        it.value
                    }.thenBy {
                        it.key.name
                    }
                )
                .first()
                .key

        return CatalogReviewCategoryAnalysis(
            category = category,
            entryCount = entries.size,
            shareOfBacklog =
                share(entries.size, totalEntryCount),

            potentiallyDeterministicCount =
                entries.count {
                    it.automationAssessment ==
                            CatalogReviewAutomationAssessment
                                .POTENTIALLY_DETERMINISTIC
                },

            manualReviewRequiredCount =
                entries.count {
                    it.automationAssessment ==
                            CatalogReviewAutomationAssessment
                                .MANUAL_REVIEW_REQUIRED
                },

            conflictingEvidenceCount =
                entries.count {
                    it.automationAssessment ==
                            CatalogReviewAutomationAssessment
                                .CONFLICTING_EVIDENCE
                },

            splitRequiredCount =
                entries.count {
                    it.automationAssessment ==
                            CatalogReviewAutomationAssessment
                                .SPLIT_REQUIRED
                },

            countsByClassification =
                countsByClassification,

            dominantClassification =
                dominantClassification,

            exampleSourceIndices =
                entries
                    .map { it.sourceIndex }
                    .distinct()
                    .sorted()
                    .take(MAXIMUM_EXAMPLES)
        )
    }

    private fun determinePriority(
        classification:
        CatalogReviewBacklogClassification,
        entryCount: Int,
        potentiallyDeterministicCount: Int
    ): CatalogReviewResolutionPriority {
        if (
            classification in
            MANUAL_ONLY_CLASSIFICATIONS
        ) {
            return CatalogReviewResolutionPriority
                .MANUAL_ONLY
        }

        if (
            potentiallyDeterministicCount == 0
        ) {
            return CatalogReviewResolutionPriority.LOW
        }

        val deterministicShare =
            potentiallyDeterministicCount
                .toDouble() /
                    entryCount.toDouble()

        return when {
            potentiallyDeterministicCount >=
                    HIGH_PRIORITY_MINIMUM_COUNT &&
                    deterministicShare >=
                    HIGH_PRIORITY_MINIMUM_SHARE ->
                CatalogReviewResolutionPriority.HIGH

            potentiallyDeterministicCount >=
                    MEDIUM_PRIORITY_MINIMUM_COUNT ->
                CatalogReviewResolutionPriority.MEDIUM

            else ->
                CatalogReviewResolutionPriority.LOW
        }
    }

    private fun recommendationFor(
        classification:
        CatalogReviewBacklogClassification
    ): CatalogReviewResolutionRecommendation =
        when (classification) {
            CatalogReviewBacklogClassification
                .SINGULAR_PLURAL_VARIANT ->
                CatalogReviewResolutionRecommendation
                    .IMPLEMENT_SINGULAR_PLURAL_RESOLVER

            CatalogReviewBacklogClassification
                .TYPO_VARIANT ->
                CatalogReviewResolutionRecommendation
                    .IMPLEMENT_TYPO_VARIANT_RESOLVER

            CatalogReviewBacklogClassification
                .PUNCTUATION_VARIANT ->
                CatalogReviewResolutionRecommendation
                    .IMPLEMENT_PUNCTUATION_VARIANT_RESOLVER

            CatalogReviewBacklogClassification
                .WORD_ORDER_VARIANT ->
                CatalogReviewResolutionRecommendation
                    .IMPLEMENT_WORD_ORDER_VARIANT_RESOLVER

            CatalogReviewBacklogClassification
                .SALES_FORM_VARIANT ->
                CatalogReviewResolutionRecommendation
                    .IMPLEMENT_SALES_FORM_VARIANT_CLASSIFIER

            CatalogReviewBacklogClassification
                .PREPARATION_VARIANT ->
                CatalogReviewResolutionRecommendation
                    .IMPLEMENT_PREPARATION_VARIANT_CLASSIFIER

            CatalogReviewBacklogClassification
                .BRAND_VARIANT ->
                CatalogReviewResolutionRecommendation
                    .IMPLEMENT_BRAND_VARIANT_POLICY

            CatalogReviewBacklogClassification
                .POSSIBLE_SEMANTIC_DUPLICATE ->
                CatalogReviewResolutionRecommendation
                    .REVIEW_POSSIBLE_SEMANTIC_DUPLICATES

            CatalogReviewBacklogClassification
                .SEMANTIC_CATEGORY_CONFLICT ->
                CatalogReviewResolutionRecommendation
                    .REVIEW_SEMANTIC_CATEGORY_CONFLICTS

            CatalogReviewBacklogClassification
                .SPLIT_REQUIRED ->
                CatalogReviewResolutionRecommendation
                    .RESOLVE_SPLIT_ACTIONS

            CatalogReviewBacklogClassification
                .UNCLASSIFIED_REVIEW ->
                CatalogReviewResolutionRecommendation
                    .REVIEW_UNCLASSIFIED_ENTRIES

            CatalogReviewBacklogClassification
                .MULTIPLE_DUPLICATE_REASONS ->
                CatalogReviewResolutionRecommendation
                    .REVIEW_CONFLICTING_EVIDENCE

            CatalogReviewBacklogClassification
                .FROZEN_FORM_VARIANT,

            CatalogReviewBacklogClassification
                .CANNED_FORM_VARIANT,

            CatalogReviewBacklogClassification
                .BIO_ATTRIBUTE_VARIANT ->
                CatalogReviewResolutionRecommendation
                    .PRESERVE_DISTINCT_PRODUCT_FORMS
        }

    private fun buildRationale(
        classification:
        CatalogReviewBacklogClassification,
        entryCount: Int,
        potentiallyDeterministicCount: Int,
        conflictingEvidenceCount: Int,
        splitRequiredCount: Int
    ): String =
        buildString {
            append("Classification ")
            append(classification.name)
            append(" contains ")
            append(entryCount)
            append(" unresolved entries")

            append(", including ")
            append(potentiallyDeterministicCount)
            append(" potentially deterministic entries")

            append(", ")
            append(conflictingEvidenceCount)
            append(" entries with conflicting evidence")

            append(", and ")
            append(splitRequiredCount)
            append(" split-required entries.")
        }

    private fun share(
        count: Int,
        total: Int
    ): Double =
        if (total == 0) {
            0.0
        } else {
            count.toDouble() /
                    total.toDouble()
        }

    private fun priorityRank(
        priority: CatalogReviewResolutionPriority
    ): Int =
        when (priority) {
            CatalogReviewResolutionPriority.HIGH -> 0
            CatalogReviewResolutionPriority.MEDIUM -> 1
            CatalogReviewResolutionPriority.LOW -> 2
            CatalogReviewResolutionPriority.MANUAL_ONLY -> 3
        }

    private companion object {

        const val UNCATEGORIZED_CATEGORY =
            "<uncategorized>"

        const val MAXIMUM_EXAMPLES = 10

        const val HIGH_PRIORITY_MINIMUM_COUNT = 10

        const val HIGH_PRIORITY_MINIMUM_SHARE = 0.50

        const val MEDIUM_PRIORITY_MINIMUM_COUNT = 3

        val MANUAL_ONLY_CLASSIFICATIONS = setOf(
            CatalogReviewBacklogClassification
                .SEMANTIC_CATEGORY_CONFLICT,

            CatalogReviewBacklogClassification
                .POSSIBLE_SEMANTIC_DUPLICATE,

            CatalogReviewBacklogClassification
                .MULTIPLE_DUPLICATE_REASONS,

            CatalogReviewBacklogClassification
                .SPLIT_REQUIRED,

            CatalogReviewBacklogClassification
                .UNCLASSIFIED_REVIEW
        )
    }
}