package de.shopme.testing.system.tools.knowledge.catalog.linguistic.repair

data class CanonicalCatalogLinguisticRepairStats(
    val repairedEntryCount: Int,

    val pluralChangedCount: Int,
    val colloquialChangedCount: Int,
    val phoneticTokensChangedCount: Int,
    val autocompleteTokensChangedCount: Int,

    val removedVariantColloquialCount: Int,
    val preservedColloquialAliasCount: Int,

    val familyPluralResolvedCount: Int,
    val familyPluralFallbackCount: Int
)

data class CanonicalCatalogLinguisticRepairReport(
    val schemaVersion: Int,

    val inputEntryCount: Int,
    val outputEntryCount: Int,

    val uniqueNormalizedKeyCount: Int,
    val uniqueItemNameCount: Int,

    val blankPluralCount: Int,
    val variantLeakedIntoColloquialCount: Int,
    val duplicateColloquialValueCount: Int,

    val emptyAutocompleteEntryCount: Int,

    val deterministic: Boolean,
    val valid: Boolean,

    val stats: CanonicalCatalogLinguisticRepairStats
)