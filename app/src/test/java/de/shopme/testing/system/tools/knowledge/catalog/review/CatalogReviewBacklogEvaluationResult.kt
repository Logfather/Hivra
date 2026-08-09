package de.shopme.testing.system.tools.knowledge.catalog.review

data class CatalogReviewBacklogEvaluationResult(
    val version: Int,

    val originalPlanEntryCount: Int,
    val originalManualReviewCount: Int,

    val evaluatedBacklogEntryCount: Int,

    val resolvedEntryCount: Int,
    val unresolvedEntryCount: Int,

    val resolvedByNormalizationCount: Int,
    val resolvedByCategoryMigrationCount: Int,
    val resolvedByDuplicateMergeCount: Int,
    val resolvedByRemovalCount: Int,

    val stillReviewRequiredCount: Int,
    val stillSplitRequiredCount: Int,

    val statusCounts: Map<CatalogReviewBacklogStatus, Int>,

    val entries: List<CatalogReviewBacklogEntry>,

    val valid: Boolean
) {

    init {
        require(version > 0)

        require(originalPlanEntryCount >= 0)
        require(originalManualReviewCount >= 0)
        require(evaluatedBacklogEntryCount >= 0)

        require(resolvedEntryCount >= 0)
        require(unresolvedEntryCount >= 0)

        require(resolvedByNormalizationCount >= 0)
        require(resolvedByCategoryMigrationCount >= 0)
        require(resolvedByDuplicateMergeCount >= 0)
        require(resolvedByRemovalCount >= 0)

        require(stillReviewRequiredCount >= 0)
        require(stillSplitRequiredCount >= 0)

        require(evaluatedBacklogEntryCount == entries.size) {
            "evaluatedBacklogEntryCount must equal entries size."
        }

        require(
            resolvedEntryCount + unresolvedEntryCount ==
                    evaluatedBacklogEntryCount
        ) {
            "Resolved and unresolved counts must cover the backlog."
        }

        require(
            resolvedEntryCount ==
                    resolvedByNormalizationCount +
                    resolvedByCategoryMigrationCount +
                    resolvedByDuplicateMergeCount +
                    resolvedByRemovalCount
        ) {
            "Resolved detail counts must sum to resolvedEntryCount."
        }

        require(
            unresolvedEntryCount ==
                    stillReviewRequiredCount +
                    stillSplitRequiredCount
        ) {
            "Unresolved detail counts must sum to unresolvedEntryCount."
        }

        require(
            statusCounts.values.sum() ==
                    evaluatedBacklogEntryCount
        ) {
            "statusCounts must cover all evaluated entries."
        }

        require(
            entries.map { it.sourceIndex }
                .distinct()
                .size ==
                    entries.size
        ) {
            "Backlog entries must have unique sourceIndex values."
        }

        require(
            entries ==
                    entries.sortedBy { it.sourceIndex }
        ) {
            "Backlog entries must be sorted by sourceIndex."
        }

        require(valid) {
            "Review backlog evaluation result must be valid."
        }
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}