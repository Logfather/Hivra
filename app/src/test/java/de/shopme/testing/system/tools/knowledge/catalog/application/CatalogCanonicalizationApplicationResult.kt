package de.shopme.testing.system.tools.knowledge.catalog.application

import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem

data class CatalogCanonicalizationApplicationResult(
    val version: Int,

    val inputEntryCount: Int,
    val outputEntryCount: Int,

    val appliedEntryCount: Int,
    val skippedReviewEntryCount: Int,
    val removedEntryCount: Int,
    val mergedEntryCount: Int,
    val failedEntryCount: Int,

    /**
     * Finaler Katalogbestand in stabiler fachlicher Sortierung.
     *
     * Diese Liste ist für Validierung und persistierte JSON-Ausgabe
     * vorgesehen.
     */
    val outputItems: List<CatalogFoodItem>,

    /**
     * Derselbe finale Katalogbestand unter Beibehaltung der ursprünglichen
     * sourceIndex-Identitäten.
     *
     * Diese Map ist für nachgelagerte deterministische Pipeline-Schritte
     * erforderlich, die Entscheidungen anhand eines sourceIndex anwenden.
     *
     * Nach einem Merge enthält die Map ausschließlich das erhaltene
     * kanonische Ziel. Der entfernte Quellindex ist nicht mehr vorhanden.
     */
    val outputItemsBySourceIndex:
    Map<Int, CatalogFoodItem>,

    val entries:
    List<CatalogCanonicalizationApplicationEntry>,

    val valid: Boolean
) {

    init {
        require(version > 0)

        require(inputEntryCount >= 0)
        require(outputEntryCount >= 0)
        require(appliedEntryCount >= 0)
        require(skippedReviewEntryCount >= 0)
        require(removedEntryCount >= 0)
        require(mergedEntryCount >= 0)
        require(failedEntryCount >= 0)

        require(
            outputEntryCount ==
                    outputItems.size
        ) {
            "outputEntryCount must equal outputItems size."
        }

        require(
            outputEntryCount ==
                    outputItemsBySourceIndex.size
        ) {
            "outputEntryCount must equal outputItemsBySourceIndex size."
        }

        require(
            outputItemsBySourceIndex.keys.all {
                it >= 0
            }
        ) {
            "outputItemsBySourceIndex contains a negative sourceIndex."
        }

        require(
            outputItemsBySourceIndex.keys.toList() ==
                    outputItemsBySourceIndex.keys.sorted()
        ) {
            "outputItemsBySourceIndex must be sorted by sourceIndex."
        }

        /*
         * Liste und Map müssen exakt denselben finalen Artikelbestand
         * repräsentieren. Die Reihenfolge darf unterschiedlich sein:
         *
         * - outputItems: fachliche Katalogsortierung
         * - outputItemsBySourceIndex: ursprüngliche Source-Identität
         */
        require(
            outputItems.groupingBy { it }.eachCount() ==
                    outputItemsBySourceIndex
                        .values
                        .groupingBy { it }
                        .eachCount()
        ) {
            "outputItems and outputItemsBySourceIndex must contain " +
                    "identical catalog items."
        }

        require(
            inputEntryCount ==
                    entries.size
        ) {
            "Every input entry must have one application entry."
        }

        require(
            entries.map { it.sourceIndex }
                .distinct()
                .size ==
                    entries.size
        ) {
            "Application entries must have unique sourceIndex values."
        }

        require(
            entries ==
                    entries.sortedBy {
                        it.sourceIndex
                    }
        ) {
            "Application entries must be sorted by sourceIndex."
        }

        require(
            appliedEntryCount ==
                    entries.count {
                        it.status ==
                                CatalogCanonicalizationApplicationStatus
                                    .APPLIED
                    }
        ) {
            "appliedEntryCount is inconsistent."
        }

        require(
            skippedReviewEntryCount ==
                    entries.count {
                        it.status ==
                                CatalogCanonicalizationApplicationStatus
                                    .SKIPPED_REVIEW_REQUIRED
                    }
        ) {
            "skippedReviewEntryCount is inconsistent."
        }

        require(
            removedEntryCount ==
                    entries.count {
                        it.status ==
                                CatalogCanonicalizationApplicationStatus
                                    .REMOVED
                    }
        ) {
            "removedEntryCount is inconsistent."
        }

        require(
            mergedEntryCount ==
                    entries.count {
                        it.status ==
                                CatalogCanonicalizationApplicationStatus
                                    .MERGED_INTO_TARGET
                    }
        ) {
            "mergedEntryCount is inconsistent."
        }

        require(
            failedEntryCount ==
                    entries.count {
                        it.status ==
                                CatalogCanonicalizationApplicationStatus
                                    .FAILED
                    }
        ) {
            "failedEntryCount is inconsistent."
        }

        require(
            valid ==
                    (failedEntryCount == 0)
        ) {
            "valid must be true exactly when no application failed."
        }
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}