package de.shopme.testing.system.tools.knowledge.catalog.expansion.analysis

data class CanonicalFoodCatalogCategoryGap(
    val category: String,

    val baselineEntryCount: Int,
    val targetEntryCount: Int,
    val expansionEntryCount: Int,

    /**
     * Anteil des bereits vorhandenen Bestands am Zielbestand.
     *
     * Beispiel:
     * 200 / 500 = 0.4
     */
    val baselineCoverage: Double,

    /**
     * Faktor, um den der Bestand wachsen muss.
     *
     * Beispiel:
     * 500 / 200 = 2.5
     *
     * null, wenn die Kategorie in der Baseline leer ist.
     */
    val requiredGrowthFactor: Double?,

    /**
     * Anteil des Kategorie-Gaps am gesamten Ausbau.
     */
    val shareOfRequiredExpansion: Double,

    /**
     * Anteil des fehlenden Bestands am Ziel dieser Kategorie.
     */
    val categoryGapShare: Double,

    val gapRank: Int,

    val priority:
    CanonicalFoodCatalogExpansionGapPriority
) {

    init {
        require(category.isNotBlank())
        require(category == category.trim())
        require(category == category.lowercase()) {
            "Category must be a canonical lowercase key: $category"
        }

        require(baselineEntryCount >= 0)
        require(targetEntryCount > 0)
        require(expansionEntryCount >= 0)

        require(
            expansionEntryCount ==
                    targetEntryCount - baselineEntryCount
        ) {
            "Expansion count is inconsistent for '$category'."
        }

        require(baselineCoverage in 0.0..1.0)
        require(shareOfRequiredExpansion in 0.0..1.0)
        require(categoryGapShare in 0.0..1.0)

        requiredGrowthFactor?.let {
            require(it >= 1.0)
        }

        require(gapRank > 0)
    }
}