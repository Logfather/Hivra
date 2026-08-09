package de.shopme.testing.system.tools.knowledge.catalog.review.classification

import de.shopme.testing.system.tools.knowledge.catalog.review.CatalogReviewBacklogEntry
import de.shopme.testing.system.tools.knowledge.catalog.review.CatalogReviewBacklogEvaluationResult
import de.shopme.testing.system.tools.knowledge.catalog.review.CatalogReviewBacklogStatus
import java.util.Locale

class CatalogReviewBacklogClassifier {

    fun classify(
        backlogResult: CatalogReviewBacklogEvaluationResult
    ): CatalogReviewBacklogClassificationResult {
        require(backlogResult.valid) {
            "Review backlog result must be valid before classification."
        }

        val unresolvedEntries = backlogResult.entries
            .filter {
                it.status ==
                        CatalogReviewBacklogStatus
                            .STILL_REVIEW_REQUIRED ||
                        it.status ==
                        CatalogReviewBacklogStatus
                            .STILL_SPLIT_REQUIRED
            }
            .sortedBy { it.sourceIndex }

        require(
            unresolvedEntries.size ==
                    backlogResult.unresolvedEntryCount
        ) {
            "Unresolved entries do not match unresolvedEntryCount."
        }

        val classifiedEntries = unresolvedEntries.map(
            ::classifyEntry
        )

        val countsByClassification =
            classifiedEntries
                .groupingBy {
                    it.primaryClassification
                }
                .eachCount()
                .toList()
                .sortedBy { it.first.name }
                .associate { it }

        val countsByAutomationAssessment =
            classifiedEntries
                .groupingBy {
                    it.automationAssessment
                }
                .eachCount()
                .toList()
                .sortedBy { it.first.name }
                .associate { it }

        val countsByCategory =
            classifiedEntries
                .groupingBy { entry ->
                    entry.category
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                        ?: UNCATEGORIZED_CATEGORY
                }
                .eachCount()
                .toSortedMap()

        return CatalogReviewBacklogClassificationResult(
            version =
                CatalogReviewBacklogClassificationResult
                    .CURRENT_VERSION,

            sourceBacklogEntryCount =
                unresolvedEntries.size,

            classifiedEntryCount =
                classifiedEntries.size,

            potentiallyDeterministicCount =
                classifiedEntries.count {
                    it.automationAssessment ==
                            CatalogReviewAutomationAssessment
                                .POTENTIALLY_DETERMINISTIC
                },

            manualReviewRequiredCount =
                classifiedEntries.count {
                    it.automationAssessment ==
                            CatalogReviewAutomationAssessment
                                .MANUAL_REVIEW_REQUIRED
                },

            conflictingEvidenceCount =
                classifiedEntries.count {
                    it.automationAssessment ==
                            CatalogReviewAutomationAssessment
                                .CONFLICTING_EVIDENCE
                },

            splitRequiredCount =
                classifiedEntries.count {
                    it.automationAssessment ==
                            CatalogReviewAutomationAssessment
                                .SPLIT_REQUIRED
                },

            entriesWithUniqueMergeTargetCount =
                classifiedEntries.count {
                    it.hasUniqueMergeTarget
                },

            entriesWithMultipleConflictReasonsCount =
                classifiedEntries.count {
                    it.hasMultipleConflictReasons
                },

            countsByClassification =
                countsByClassification,

            countsByAutomationAssessment =
                countsByAutomationAssessment,

            countsByCategory =
                countsByCategory,

            entries =
                classifiedEntries,

            valid = true
        )
    }

    private fun classifyEntry(
        entry: CatalogReviewBacklogEntry
    ): ClassifiedCatalogReviewBacklogEntry {
        val normalizedReasons = entry.originalReasons
            .map(::normalizeReason)

        val detected = detectClassifications(
            entry = entry,
            normalizedReasons = normalizedReasons
        )

        val primaryClassification =
            determinePrimaryClassification(
                entry = entry,
                detected = detected
            )

        val duplicateClassifications = detected
            .filter {
                it in DUPLICATE_CLASSIFICATIONS
            }

        val hasMultipleConflictReasons =
            duplicateClassifications.size > 1 ||
                    (
                            primaryClassification ==
                                    CatalogReviewBacklogClassification
                                        .SEMANTIC_CATEGORY_CONFLICT &&
                                    duplicateClassifications.isNotEmpty()
                            )

        val hasUniqueMergeTarget =
            entry.mergeTargetSourceIndex != null

        val automationAssessment =
            determineAutomationAssessment(
                entry = entry,
                primaryClassification =
                    primaryClassification,
                detectedClassifications = detected,
                hasUniqueMergeTarget =
                    hasUniqueMergeTarget,
                hasMultipleConflictReasons =
                    hasMultipleConflictReasons
            )

        val classificationReasons =
            buildClassificationReasons(
                primaryClassification =
                    primaryClassification,
                detectedClassifications = detected,
                automationAssessment =
                    automationAssessment,
                hasUniqueMergeTarget =
                    hasUniqueMergeTarget,
                hasMultipleConflictReasons =
                    hasMultipleConflictReasons
            )

        return ClassifiedCatalogReviewBacklogEntry(
            sourceIndex = entry.sourceIndex,

            itemName =
                entry.resultingItemName
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?: entry.originalItemName,

            category =
                entry.resultingCategory
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?: entry.originalCategory,

            normalizedKey =
                entry.resultingNormalizedKey
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?: entry.originalNormalizedKey,

            originalAction =
                entry.originalAction,

            backlogStatus =
                entry.status,

            mergeTargetSourceIndex =
                entry.mergeTargetSourceIndex,

            primaryClassification =
                primaryClassification,

            detectedClassifications =
                detected.sortedBy { it.name },

            automationAssessment =
                automationAssessment,

            hasUniqueMergeTarget =
                hasUniqueMergeTarget,

            hasMultipleConflictReasons =
                hasMultipleConflictReasons,

            originalReasons =
                entry.originalReasons
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .distinct()
                    .sorted(),

            classificationReasons =
                classificationReasons
        )
    }

    private fun detectClassifications(
        entry: CatalogReviewBacklogEntry,
        normalizedReasons: List<String>
    ): Set<CatalogReviewBacklogClassification> {
        if (
            entry.status ==
            CatalogReviewBacklogStatus
                .STILL_SPLIT_REQUIRED
        ) {
            return setOf(
                CatalogReviewBacklogClassification
                    .SPLIT_REQUIRED
            )
        }

        val classifications =
            linkedSetOf<CatalogReviewBacklogClassification>()

        if (
            normalizedReasons.any(
                ::containsSemanticCategoryConflict
            )
        ) {
            classifications +=
                CatalogReviewBacklogClassification
                    .SEMANTIC_CATEGORY_CONFLICT
        }

        DUPLICATE_MARKERS.forEach {
                (marker, classification) ->

            if (
                normalizedReasons.any {
                    marker in it
                }
            ) {
                classifications += classification
            }
        }

        if (
            classifications.isEmpty()
        ) {
            classifications +=
                CatalogReviewBacklogClassification
                    .UNCLASSIFIED_REVIEW
        }

        val duplicateClassifications =
            classifications.filter {
                it in DUPLICATE_CLASSIFICATIONS
            }

        if (duplicateClassifications.size > 1) {
            classifications +=
                CatalogReviewBacklogClassification
                    .MULTIPLE_DUPLICATE_REASONS
        }

        return classifications
    }

    private fun determinePrimaryClassification(
        entry: CatalogReviewBacklogEntry,
        detected: Set<CatalogReviewBacklogClassification>
    ): CatalogReviewBacklogClassification {
        if (
            entry.status ==
            CatalogReviewBacklogStatus
                .STILL_SPLIT_REQUIRED
        ) {
            return CatalogReviewBacklogClassification
                .SPLIT_REQUIRED
        }

        if (
            CatalogReviewBacklogClassification
                .MULTIPLE_DUPLICATE_REASONS in detected
        ) {
            return CatalogReviewBacklogClassification
                .MULTIPLE_DUPLICATE_REASONS
        }

        return CLASSIFICATION_PRIORITY
            .firstOrNull { it in detected }
            ?: CatalogReviewBacklogClassification
                .UNCLASSIFIED_REVIEW
    }

    private fun determineAutomationAssessment(
        entry: CatalogReviewBacklogEntry,
        primaryClassification:
        CatalogReviewBacklogClassification,
        detectedClassifications:
        Set<CatalogReviewBacklogClassification>,
        hasUniqueMergeTarget: Boolean,
        hasMultipleConflictReasons: Boolean
    ): CatalogReviewAutomationAssessment {
        if (
            primaryClassification ==
            CatalogReviewBacklogClassification
                .SPLIT_REQUIRED
        ) {
            return CatalogReviewAutomationAssessment
                .SPLIT_REQUIRED
        }

        if (hasMultipleConflictReasons) {
            return CatalogReviewAutomationAssessment
                .CONFLICTING_EVIDENCE
        }

        if (
            primaryClassification ==
            CatalogReviewBacklogClassification
                .SEMANTIC_CATEGORY_CONFLICT
        ) {
            return CatalogReviewAutomationAssessment
                .MANUAL_REVIEW_REQUIRED
        }

        if (
            detectedClassifications.size == 1 &&
            primaryClassification in
            POTENTIALLY_DETERMINISTIC_CLASSIFICATIONS &&
            hasUniqueMergeTarget &&
            entry.mergeTargetSourceIndex != entry.sourceIndex
        ) {
            return CatalogReviewAutomationAssessment
                .POTENTIALLY_DETERMINISTIC
        }

        return CatalogReviewAutomationAssessment
            .MANUAL_REVIEW_REQUIRED
    }

    private fun buildClassificationReasons(
        primaryClassification:
        CatalogReviewBacklogClassification,
        detectedClassifications:
        Set<CatalogReviewBacklogClassification>,
        automationAssessment:
        CatalogReviewAutomationAssessment,
        hasUniqueMergeTarget: Boolean,
        hasMultipleConflictReasons: Boolean
    ): List<String> {
        val reasons = mutableListOf<String>()

        reasons +=
            "Primary review classification is " +
                    "${primaryClassification.name}."

        if (detectedClassifications.size > 1) {
            reasons +=
                "Detected classifications: " +
                        detectedClassifications
                            .sortedBy { it.name }
                            .joinToString { it.name } +
                        "."
        }

        reasons += if (hasUniqueMergeTarget) {
            "The review entry contains one explicit merge target."
        } else {
            "The review entry contains no explicit merge target."
        }

        if (hasMultipleConflictReasons) {
            reasons +=
                "Multiple or mixed conflict reasons prevent automatic " +
                        "resolution."
        }

        reasons +=
            "Automation assessment is " +
                    "${automationAssessment.name}."

        return reasons
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()
    }

    private fun containsSemanticCategoryConflict(
        normalizedReason: String
    ): Boolean =
        SEMANTIC_CATEGORY_MARKERS.any {
            it in normalizedReason
        }

    private fun normalizeReason(
        value: String
    ): String =
        value
            .trim()
            .uppercase(Locale.ROOT)
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")

    private companion object {

        const val UNCATEGORIZED_CATEGORY =
            "<uncategorized>"

        val MULTIPLE_WHITESPACE_REGEX =
            Regex("\\s+")

        val DUPLICATE_MARKERS = mapOf(
            "SINGULAR_PLURAL_VARIANT" to
                    CatalogReviewBacklogClassification
                        .SINGULAR_PLURAL_VARIANT,

            "TYPO_VARIANT" to
                    CatalogReviewBacklogClassification
                        .TYPO_VARIANT,

            "PUNCTUATION_VARIANT" to
                    CatalogReviewBacklogClassification
                        .PUNCTUATION_VARIANT,

            "WORD_ORDER_VARIANT" to
                    CatalogReviewBacklogClassification
                        .WORD_ORDER_VARIANT,

            "SALES_FORM_VARIANT" to
                    CatalogReviewBacklogClassification
                        .SALES_FORM_VARIANT,

            "PREPARATION_VARIANT" to
                    CatalogReviewBacklogClassification
                        .PREPARATION_VARIANT,

            "BRAND_VARIANT" to
                    CatalogReviewBacklogClassification
                        .BRAND_VARIANT,

            "POSSIBLE_SEMANTIC_DUPLICATE" to
                    CatalogReviewBacklogClassification
                        .POSSIBLE_SEMANTIC_DUPLICATE
        )

        val DUPLICATE_CLASSIFICATIONS = setOf(
            CatalogReviewBacklogClassification
                .SINGULAR_PLURAL_VARIANT,
            CatalogReviewBacklogClassification
                .TYPO_VARIANT,
            CatalogReviewBacklogClassification
                .PUNCTUATION_VARIANT,
            CatalogReviewBacklogClassification
                .WORD_ORDER_VARIANT,
            CatalogReviewBacklogClassification
                .SALES_FORM_VARIANT,
            CatalogReviewBacklogClassification
                .PREPARATION_VARIANT,
            CatalogReviewBacklogClassification
                .BRAND_VARIANT,
            CatalogReviewBacklogClassification
                .POSSIBLE_SEMANTIC_DUPLICATE
        )

        val POTENTIALLY_DETERMINISTIC_CLASSIFICATIONS = setOf(
            CatalogReviewBacklogClassification
                .SINGULAR_PLURAL_VARIANT,
            CatalogReviewBacklogClassification
                .TYPO_VARIANT,
            CatalogReviewBacklogClassification
                .PUNCTUATION_VARIANT,
            CatalogReviewBacklogClassification
                .WORD_ORDER_VARIANT
        )

        val SEMANTIC_CATEGORY_MARKERS = setOf(
            "STRONGER LEXICAL EVIDENCE",
            "CONTAINS STRONGER LEXICAL EVIDENCE",
            "ASSIGNED DIRECTLY TO ROOT CATEGORY",
            "MORE SPECIFIC CHILD CATEGORY MAY BE AVAILABLE",
            "MORE SPECIFIC CATEGORY MAY BE AVAILABLE",
            "SEMANTIC CATEGORY CONFLICT",
            "AMBIGUOUS CATEGORY",
            "CATEGORY CONFLICT",
            "REQUIRES SEMANTIC CATEGORY REVIEW"
        )

        val CLASSIFICATION_PRIORITY = listOf(
            CatalogReviewBacklogClassification
                .SEMANTIC_CATEGORY_CONFLICT,

            CatalogReviewBacklogClassification
                .SINGULAR_PLURAL_VARIANT,

            CatalogReviewBacklogClassification
                .TYPO_VARIANT,

            CatalogReviewBacklogClassification
                .PUNCTUATION_VARIANT,

            CatalogReviewBacklogClassification
                .WORD_ORDER_VARIANT,

            CatalogReviewBacklogClassification
                .SALES_FORM_VARIANT,

            CatalogReviewBacklogClassification
                .PREPARATION_VARIANT,

            CatalogReviewBacklogClassification
                .BRAND_VARIANT,

            CatalogReviewBacklogClassification
                .POSSIBLE_SEMANTIC_DUPLICATE,

            CatalogReviewBacklogClassification
                .UNCLASSIFIED_REVIEW
        )
    }
}