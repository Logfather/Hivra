package de.shopme.testing.system.tools.knowledge.catalog.review

import de.shopme.testing.system.tools.knowledge.catalog.application.CatalogCanonicalizationApplicationEntry
import de.shopme.testing.system.tools.knowledge.catalog.application.CatalogCanonicalizationApplicationResult
import de.shopme.testing.system.tools.knowledge.catalog.application.CatalogCanonicalizationApplicationStatus
import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationAction
import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationPlan
import de.shopme.testing.system.tools.knowledge.catalog.canonicalization.CatalogCanonicalizationPlanEntry
import de.shopme.testing.system.tools.knowledge.catalog.category.CanonicalFoodCategoryRegistry
import de.shopme.testing.system.tools.knowledge.catalog.model.IndexedCatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CanonicalFoodKeyNormalizer
import de.shopme.testing.system.tools.knowledge.catalog.normalization.CatalogNormalizationResult
import java.util.Locale

class CatalogReviewBacklogEvaluator(
    private val keyNormalizer: CanonicalFoodKeyNormalizer
) {

    fun evaluate(
        sourceEntries: List<IndexedCatalogFoodItem>,
        normalizations: List<CatalogNormalizationResult>,
        plan: CatalogCanonicalizationPlan,
        applicationResult: CatalogCanonicalizationApplicationResult,
        categoryRegistry: CanonicalFoodCategoryRegistry
    ): CatalogReviewBacklogEvaluationResult {
        validateInputs(
            sourceEntries = sourceEntries,
            normalizations = normalizations,
            plan = plan,
            applicationResult = applicationResult
        )

        val sourceEntriesByIndex =
            sourceEntries.associateBy { it.sourceIndex }

        val normalizationsByIndex =
            normalizations.associateBy { it.sourceIndex }

        val applicationEntriesByIndex =
            applicationResult.entries.associateBy {
                it.sourceIndex
            }

        val reviewPlanEntries = plan.entries
            .filter(::requiresBacklogEvaluation)
            .sortedBy { it.sourceIndex }

        val evaluatedEntries = reviewPlanEntries.map { planEntry ->
            val sourceEntry =
                sourceEntriesByIndex.getValue(
                    planEntry.sourceIndex
                )

            val normalization =
                normalizationsByIndex.getValue(
                    planEntry.sourceIndex
                )

            val applicationEntry =
                applicationEntriesByIndex.getValue(
                    planEntry.sourceIndex
                )

            evaluateEntry(
                sourceEntry = sourceEntry,
                normalization = normalization,
                planEntry = planEntry,
                applicationEntry = applicationEntry,
                categoryRegistry = categoryRegistry
            )
        }

        val statusCounts = evaluatedEntries
            .groupingBy { it.status }
            .eachCount()
            .toList()
            .sortedBy { it.first.name }
            .associate { it }

        val resolvedStatuses = setOf(
            CatalogReviewBacklogStatus
                .RESOLVED_BY_NORMALIZATION,
            CatalogReviewBacklogStatus
                .RESOLVED_BY_CATEGORY_MIGRATION,
            CatalogReviewBacklogStatus
                .RESOLVED_BY_DUPLICATE_MERGE,
            CatalogReviewBacklogStatus
                .RESOLVED_BY_REMOVAL
        )

        val resolvedEntryCount = evaluatedEntries.count {
            it.status in resolvedStatuses
        }

        val unresolvedEntryCount =
            evaluatedEntries.size -
                    resolvedEntryCount

        return CatalogReviewBacklogEvaluationResult(
            version =
                CatalogReviewBacklogEvaluationResult
                    .CURRENT_VERSION,

            originalPlanEntryCount =
                plan.planEntryCount,

            originalManualReviewCount =
                plan.entries.count {
                    !it.automatic ||
                            it.action ==
                            CatalogCanonicalizationAction.REVIEW ||
                            it.action ==
                            CatalogCanonicalizationAction.SPLIT
                },

            evaluatedBacklogEntryCount =
                evaluatedEntries.size,

            resolvedEntryCount =
                resolvedEntryCount,

            unresolvedEntryCount =
                unresolvedEntryCount,

            resolvedByNormalizationCount =
                evaluatedEntries.count {
                    it.status ==
                            CatalogReviewBacklogStatus
                                .RESOLVED_BY_NORMALIZATION
                },

            resolvedByCategoryMigrationCount =
                evaluatedEntries.count {
                    it.status ==
                            CatalogReviewBacklogStatus
                                .RESOLVED_BY_CATEGORY_MIGRATION
                },

            resolvedByDuplicateMergeCount =
                evaluatedEntries.count {
                    it.status ==
                            CatalogReviewBacklogStatus
                                .RESOLVED_BY_DUPLICATE_MERGE
                },

            resolvedByRemovalCount =
                evaluatedEntries.count {
                    it.status ==
                            CatalogReviewBacklogStatus
                                .RESOLVED_BY_REMOVAL
                },

            stillReviewRequiredCount =
                evaluatedEntries.count {
                    it.status ==
                            CatalogReviewBacklogStatus
                                .STILL_REVIEW_REQUIRED
                },

            stillSplitRequiredCount =
                evaluatedEntries.count {
                    it.status ==
                            CatalogReviewBacklogStatus
                                .STILL_SPLIT_REQUIRED
                },

            statusCounts = statusCounts,
            entries = evaluatedEntries,
            valid = true
        )
    }

    private fun requiresBacklogEvaluation(
        entry: CatalogCanonicalizationPlanEntry
    ): Boolean =
        !entry.automatic ||
                entry.action ==
                CatalogCanonicalizationAction.REVIEW ||
                entry.action ==
                CatalogCanonicalizationAction.SPLIT

    private fun evaluateEntry(
        sourceEntry: IndexedCatalogFoodItem,
        normalization: CatalogNormalizationResult,
        planEntry: CatalogCanonicalizationPlanEntry,
        applicationEntry: CatalogCanonicalizationApplicationEntry,
        categoryRegistry: CanonicalFoodCategoryRegistry
    ): CatalogReviewBacklogEntry {
        val originalItem = sourceEntry.item

        val normalizedReasons = planEntry.reasons
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()

        val statusAndReason = determineStatus(
            originalItemName = originalItem.itemname,
            originalCategory = originalItem.category,
            originalNormalizedKey = originalItem.normalized,
            normalization = normalization,
            planEntry = planEntry,
            applicationEntry = applicationEntry,
            categoryRegistry = categoryRegistry,
            normalizedReasons = normalizedReasons
        )

        return CatalogReviewBacklogEntry(
            sourceIndex = sourceEntry.sourceIndex,

            originalItemName =
                originalItem.itemname,

            originalCategory =
                originalItem.category,

            originalNormalizedKey =
                originalItem.normalized,

            originalAction =
                planEntry.action,

            originalAutomatic =
                planEntry.automatic,

            resultingItemName =
                normalization.computedCanonicalName,

            resultingCategory =
                applicationEntry.resultingCategory,

            resultingNormalizedKey =
                applicationEntry.resultingNormalizedKey,

            mergeTargetSourceIndex =
                applicationEntry.mergeTargetSourceIndex,

            originalReasons =
                normalizedReasons,

            status =
                statusAndReason.first,

            resolutionReason =
                statusAndReason.second
        )
    }

    private fun determineStatus(
        originalItemName: String,
        originalCategory: String?,
        originalNormalizedKey: String?,
        normalization: CatalogNormalizationResult,
        planEntry: CatalogCanonicalizationPlanEntry,
        applicationEntry: CatalogCanonicalizationApplicationEntry,
        categoryRegistry: CanonicalFoodCategoryRegistry,
        normalizedReasons: List<String>
    ): Pair<CatalogReviewBacklogStatus, String> {
        if (
            applicationEntry.status ==
            CatalogCanonicalizationApplicationStatus
                .MERGED_INTO_TARGET
        ) {
            return CatalogReviewBacklogStatus
                .RESOLVED_BY_DUPLICATE_MERGE to
                    (
                            "Entry was deterministically merged into " +
                                    "sourceIndex " +
                                    applicationEntry.mergeTargetSourceIndex +
                                    "."
                            )
        }

        if (
            applicationEntry.status ==
            CatalogCanonicalizationApplicationStatus.REMOVED
        ) {
            return CatalogReviewBacklogStatus
                .RESOLVED_BY_REMOVAL to
                    "Entry was removed by an approved automatic action."
        }

        if (
            planEntry.action ==
            CatalogCanonicalizationAction.SPLIT
        ) {
            return CatalogReviewBacklogStatus
                .STILL_SPLIT_REQUIRED to
                    (
                            "The entry still represents multiple semantic " +
                                    "identities and requires a deterministic split."
                            )
        }

        val resultingCategory =
            applicationEntry.resultingCategory
                ?.trim()
                ?.takeIf(String::isNotBlank)

        val categoryChanged =
            normalizeOptionalText(originalCategory) !=
                    normalizeOptionalText(resultingCategory)

        val categoryIsCanonical =
            resultingCategory != null &&
                    categoryRegistry.contains(resultingCategory)

        if (
            categoryChanged &&
            categoryIsCanonical &&
            reasonsAreCategoryOnly(normalizedReasons)
        ) {
            return CatalogReviewBacklogStatus
                .RESOLVED_BY_CATEGORY_MIGRATION to
                    (
                            "Legacy category was deterministically migrated from " +
                                    "'${originalCategory.orEmpty()}' to " +
                                    "'$resultingCategory'."
                            )
        }

        val resultingNormalizedKey =
            applicationEntry.resultingNormalizedKey
                ?.trim()
                ?.takeIf(String::isNotBlank)

        val keyIsCanonical =
            resultingNormalizedKey != null &&
                    keyNormalizer.isCanonicalKey(
                        resultingNormalizedKey
                    )

        val technicalNormalizationApplied =
            originalItemName !=
                    normalization.computedCanonicalName ||
                    normalizeOptionalText(originalNormalizedKey) !=
                    resultingNormalizedKey ||
                    normalization.changes.isNotEmpty()

        if (
            technicalNormalizationApplied &&
            keyIsCanonical &&
            categoryIsCanonical &&
            reasonsAreTechnicalOnly(normalizedReasons)
        ) {
            return CatalogReviewBacklogStatus
                .RESOLVED_BY_NORMALIZATION to
                    (
                            "All original review reasons were technical and were " +
                                    "resolved by safe baseline normalization."
                            )
        }

        return CatalogReviewBacklogStatus
            .STILL_REVIEW_REQUIRED to
                buildStillReviewReason(
                    planEntry = planEntry,
                    normalizedReasons = normalizedReasons
                )
    }

    private fun reasonsAreCategoryOnly(
        reasons: List<String>
    ): Boolean {
        if (reasons.isEmpty()) {
            return false
        }

        /*
         * Eine Kategorie-Migration gilt nur dann als vollständig erledigt,
         * wenn alle ursprünglichen Gründe ausschließlich die technische
         * Legacy-Darstellung der Kategorie betreffen.
         *
         * Fachliche Hinweise wie:
         *
         * - stärkere lexikalische Evidenz für eine andere Kategorie,
         * - mögliche spezifischere Unterkategorie,
         * - direkte Zuordnung zu einer Root-Kategorie,
         *
         * bleiben ausdrücklich reviewpflichtig.
         */
        return reasons.all { reason ->
            val normalizedReason =
                normalizeReason(reason)

            isTechnicalCategoryMigrationReason(
                normalizedReason
            ) &&
                    !containsSemanticCategoryConflict(
                        normalizedReason
                    )
        }
    }

    private fun isTechnicalCategoryMigrationReason(
        normalizedReason: String
    ): Boolean =
        TECHNICAL_CATEGORY_REASON_MARKERS.any { marker ->
            marker in normalizedReason
        }

    private fun containsSemanticCategoryConflict(
        normalizedReason: String
    ): Boolean =
        SEMANTIC_CATEGORY_CONFLICT_MARKERS.any { marker ->
            marker in normalizedReason
        }

    private fun reasonsAreTechnicalOnly(
        reasons: List<String>
    ): Boolean {
        if (reasons.isEmpty()) {
            return false
        }

        return reasons.all { reason ->
            val normalizedReason =
                normalizeReason(reason)

            TECHNICAL_REASON_MARKERS.any {
                it in normalizedReason
            } &&
                    SEMANTIC_REASON_MARKERS.none {
                        it in normalizedReason
                    }
        }
    }

    private fun buildStillReviewReason(
        planEntry: CatalogCanonicalizationPlanEntry,
        normalizedReasons: List<String>
    ): String {
        if (normalizedReasons.isEmpty()) {
            return (
                    "The original ${planEntry.action.name} action has no " +
                            "machine-verifiable technical resolution."
                    )
        }

        return buildString {
            append(
                "Semantic review remains required for action "
            )
            append(planEntry.action.name)
            append(": ")
            append(
                normalizedReasons.joinToString(
                    separator = "; "
                )
            )
        }
    }

    private fun normalizeOptionalText(
        value: String?
    ): String? =
        value
            ?.trim()
            ?.replace(MULTIPLE_WHITESPACE_REGEX, " ")
            ?.takeIf(String::isNotBlank)
            ?.lowercase(Locale.GERMAN)

    private fun normalizeReason(
        value: String
    ): String =
        value
            .trim()
            .lowercase(Locale.ROOT)
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")

    private fun validateInputs(
        sourceEntries: List<IndexedCatalogFoodItem>,
        normalizations: List<CatalogNormalizationResult>,
        plan: CatalogCanonicalizationPlan,
        applicationResult: CatalogCanonicalizationApplicationResult
    ) {
        val sourceIndices =
            sourceEntries.mapTo(sortedSetOf()) {
                it.sourceIndex
            }

        require(
            sourceIndices.size ==
                    sourceEntries.size
        ) {
            "Source entries contain duplicate sourceIndex values."
        }

        val normalizationIndices =
            normalizations.mapTo(sortedSetOf()) {
                it.sourceIndex
            }

        val planIndices =
            plan.entries.mapTo(sortedSetOf()) {
                it.sourceIndex
            }

        val applicationIndices =
            applicationResult.entries
                .mapTo(sortedSetOf()) {
                    it.sourceIndex
                }

        require(sourceIndices == normalizationIndices) {
            "Source entries and normalizations must cover identical indices."
        }

        require(sourceIndices == planIndices) {
            "Source entries and plan must cover identical indices."
        }

        require(sourceIndices == applicationIndices) {
            "Source entries and application result must cover identical indices."
        }

        require(plan.planEntryCount == plan.entries.size) {
            "Canonicalization plan planEntryCount is inconsistent."
        }

        require(applicationResult.valid) {
            "Application result must be valid before backlog evaluation."
        }
    }

    private companion object {

        val MULTIPLE_WHITESPACE_REGEX =
            Regex("\\s+")

        val TECHNICAL_REASON_MARKERS = setOf(
            "normalized key",
            "normalised key",
            "normalization",
            "normalisation",
            "whitespace",
            "capitalization",
            "capitalisation",
            "punctuation",
            "apostrophe",
            "hyphen",
            "umlaut",
            "plural",
            "token",
            "autocomplete",
            "phonetic",
            "spelling",
            "typo",
            "abbreviation",
            "language",
            "case",
            "format"
        )

        val TECHNICAL_CATEGORY_REASON_MARKERS = setOf(
            /*
             * Generische technische Migration.
             */
            "legacy category requires category mapping",
            "legacy category",
            "category mapping",
            "move category",

            /*
             * Ungültige oder historische Key-Darstellung.
             */
            "contains characters that are not valid in a canonical category key",
            "does not use canonical lowercase key formatting",
            "is not part of the canonical food category registry",
            "normalizes to",

            /*
             * Displayname statt kanonischem Registry-Key.
             */
            "uses the display name",
            "instead of canonical key",

            /*
             * Technische Registry- und Taxonomiehinweise.
             */
            "unknown category",
            "category key",
            "canonical category registry"
        )

        val SEMANTIC_CATEGORY_CONFLICT_MARKERS = setOf(
            /*
             * Der Eintrag liegt möglicherweise auf einer zu groben Taxonomiestufe.
             */
            "assigned directly to root category",
            "more specific child category may be available",
            "more specific category may be available",

            /*
             * Der Produktname deutet fachlich auf eine andere Kategorie.
             */
            "stronger lexical evidence",
            "contains stronger lexical evidence",
            "but its name contains",
            "lexical evidence for root category",

            /*
             * Allgemeine fachliche Konfliktbegriffe.
             */
            "semantic category conflict",
            "ambiguous category",
            "category conflict",
            "requires semantic category review"
        )

        val SEMANTIC_REASON_MARKERS = setOf(
            "brand",
            "retailer",
            "package",
            "pack size",
            "product number",
            "non-food",
            "non food",
            "ambiguous",
            "semantic",
            "preparation",
            "sales form",
            "cut",
            "split",
            "multiple foods",
            "canonical candidate",
            "manual review"
        )

        val SEMANTIC_NON_CATEGORY_MARKERS =
            SEMANTIC_REASON_MARKERS -
                    setOf("semantic")
    }
}