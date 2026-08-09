package de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution

import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem

data class CatalogDuplicateResolutionResult(
    val version: Int,
    val inputEntryCount: Int,
    val outputEntryCount: Int,
    val mergedEntryCount: Int,
    val itemsBySourceIndex: Map<Int, CatalogFoodItem>,
    val decisions: List<CatalogDuplicateResolutionDecision>,
    val valid: Boolean
) {

    init {
        require(version > 0)
        require(inputEntryCount >= 0)
        require(outputEntryCount >= 0)
        require(mergedEntryCount >= 0)

        require(outputEntryCount == itemsBySourceIndex.size) {
            "outputEntryCount must equal itemsBySourceIndex size."
        }

        require(mergedEntryCount == decisions.size) {
            "mergedEntryCount must equal decisions size."
        }

        require(
            inputEntryCount - mergedEntryCount ==
                    outputEntryCount
        ) {
            "Output count must equal input count minus merged entries."
        }

        require(
            decisions.map { it.sourceIndex }.distinct().size ==
                    decisions.size
        ) {
            "Every duplicate source may occur only once."
        }

        require(
            decisions == decisions.sortedBy { it.sourceIndex }
        ) {
            "Duplicate decisions must be sorted by sourceIndex."
        }

        require(
            decisions.none {
                it.sourceIndex in itemsBySourceIndex
            }
        ) {
            "Merged source entries must not remain in output."
        }

        require(
            decisions.all {
                it.targetSourceIndex in itemsBySourceIndex
            }
        ) {
            "Every merge target must remain in output."
        }

        require(valid) {
            "Duplicate resolution result must be valid."
        }
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}