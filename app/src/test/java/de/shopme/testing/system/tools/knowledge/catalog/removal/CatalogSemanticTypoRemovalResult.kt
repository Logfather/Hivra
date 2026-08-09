package de.shopme.testing.system.tools.knowledge.catalog.removal

import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem

data class CatalogSemanticTypoRemovalResult(
    val version: Int,

    val inputEntryCount: Int,
    val candidateEntryCount: Int,
    val removedEntryCount: Int,
    val outputEntryCount: Int,

    val removedSourceIndices: List<Int>,

    val countsByReason:
    Map<CatalogSemanticTypoRemovalReason, Int>,

    val decisions:
    List<CatalogSemanticTypoRemovalDecision>,

    val outputItemsBySourceIndex:
    Map<Int, CatalogFoodItem>,

    val valid: Boolean
) {

    init {
        require(version > 0)

        require(inputEntryCount >= 0)
        require(candidateEntryCount >= 0)
        require(removedEntryCount >= 0)
        require(outputEntryCount >= 0)

        require(candidateEntryCount == removedEntryCount) {
            "Every selected semantic typo candidate must be removed."
        }

        require(removedEntryCount == decisions.size)

        require(
            outputEntryCount ==
                    inputEntryCount - removedEntryCount
        ) {
            "Output entry count is inconsistent."
        }

        require(
            outputEntryCount ==
                    outputItemsBySourceIndex.size
        )

        require(
            removedSourceIndices ==
                    removedSourceIndices
                        .distinct()
                        .sorted()
        )

        require(
            removedSourceIndices ==
                    decisions
                        .map { it.sourceIndex }
                        .sorted()
        )

        require(
            countsByReason.values.sum() ==
                    removedEntryCount
        )

        require(
            decisions ==
                    decisions.sortedBy {
                        it.sourceIndex
                    }
        )

        require(
            removedSourceIndices.none {
                it in outputItemsBySourceIndex
            }
        ) {
            "Removed sourceIndex is still present in output."
        }

        require(valid)
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}