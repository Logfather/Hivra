package de.shopme.testing.system.tools.knowledge.catalog.expansion.approval

import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import java.util.Locale

object CanonicalExpandedCatalogItemOrder {

    /**
     * Verbindliche Reihenfolge des normalisierten kanonischen Katalogs.
     *
     * Diese Reihenfolge muss exakt dem Sortiervertrag des
     * NormalizedCatalogValidator entsprechen:
     *
     * 1. category, case-insensitive mit Locale.ROOT
     * 2. itemname, case-insensitive mit Locale.GERMAN
     * 3. normalized, case-insensitive mit Locale.ROOT
     */
    val comparator:
            Comparator<CatalogFoodItem> =
        compareBy<CatalogFoodItem>(
            {
                it.category
                    ?.lowercase(Locale.ROOT)
                    ?: ""
            },
            {
                it.itemname.lowercase(
                    Locale.GERMAN
                )
            },
            {
                it.normalized
                    ?.lowercase(Locale.ROOT)
                    ?: ""
            }
        )

    fun sort(
        items: List<CatalogFoodItem>
    ): List<CatalogFoodItem> =
        items.sortedWith(comparator)

    fun isSorted(
        items: List<CatalogFoodItem>
    ): Boolean =
        items == sort(items)
}