package de.shopme.testing.system.tools.knowledge.catalog.canonicalization

data class CatalogCanonicalizationPlan(
    val version: Int,
    val inputEntryCount: Int,
    val planEntryCount: Int,
    val automaticActionCount: Int,
    val reviewActionCount: Int,
    val unchangedEntryCount: Int,
    val actionCounts: Map<CatalogCanonicalizationAction, Int>,
    val affectedSourceIndices: List<Int>,
    val entries: List<CatalogCanonicalizationPlanEntry>,
    val valid: Boolean
) {

    init {
        require(version > 0) {
            "version must be greater than zero."
        }

        require(inputEntryCount >= 0) {
            "inputEntryCount must not be negative."
        }

        require(planEntryCount >= 0) {
            "planEntryCount must not be negative."
        }

        require(automaticActionCount >= 0) {
            "automaticActionCount must not be negative."
        }

        require(reviewActionCount >= 0) {
            "reviewActionCount must not be negative."
        }

        require(unchangedEntryCount >= 0) {
            "unchangedEntryCount must not be negative."
        }

        require(planEntryCount == entries.size) {
            "planEntryCount must equal entries size."
        }

        require(planEntryCount == inputEntryCount) {
            "Every input entry must have exactly one canonicalization plan entry."
        }

        require(
            entries.map { it.sourceIndex }.distinct().size ==
                    entries.size
        ) {
            "Canonicalization plan entries must have unique sourceIndex values."
        }

        require(
            entries.map { it.sourceIndex } ==
                    entries.map { it.sourceIndex }.sorted()
        ) {
            "Canonicalization plan entries must be sorted by sourceIndex."
        }

        require(
            actionCounts.keys.toList() ==
                    actionCounts.keys.sortedBy { it.name }
        ) {
            "actionCounts must be sorted by action name."
        }

        require(actionCounts.values.all { it >= 0 }) {
            "actionCounts must not contain negative values."
        }

        require(actionCounts.values.sum() == planEntryCount) {
            "Sum of actionCounts must equal planEntryCount."
        }

        require(
            automaticActionCount ==
                    entries.count { it.automatic }
        ) {
            "automaticActionCount must equal the number of automatic entries."
        }

        require(
            reviewActionCount ==
                    entries.count {
                        it.action == CatalogCanonicalizationAction.REVIEW
                    }
        ) {
            "reviewActionCount must equal the number of REVIEW entries."
        }

        require(
            unchangedEntryCount ==
                    entries.count {
                        it.action == CatalogCanonicalizationAction.KEEP
                    }
        ) {
            "unchangedEntryCount must equal the number of KEEP entries."
        }

        require(
            affectedSourceIndices ==
                    affectedSourceIndices.sorted()
        ) {
            "affectedSourceIndices must be sorted."
        }

        require(
            affectedSourceIndices.distinct().size ==
                    affectedSourceIndices.size
        ) {
            "affectedSourceIndices must not contain duplicates."
        }

        require(
            affectedSourceIndices ==
                    entries
                        .filter {
                            it.action != CatalogCanonicalizationAction.KEEP
                        }
                        .map { it.sourceIndex }
                        .sorted()
        ) {
            "affectedSourceIndices must contain all non-KEEP entries."
        }

        require(
            valid ==
                    (
                            planEntryCount == inputEntryCount &&
                                    entries.map { it.sourceIndex }.distinct().size ==
                                    inputEntryCount
                            )
        ) {
            "valid must reflect the structural completeness of the plan."
        }
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}