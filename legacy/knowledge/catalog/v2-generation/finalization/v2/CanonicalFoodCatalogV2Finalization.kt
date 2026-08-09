package de.shopme.testing.system.tools.knowledge.catalog.finalization.v2

data class CanonicalFoodCatalogV2FinalizationResult(
    val schemaVersion: Int,
    val releaseVersion: String,
    val finalizationId: String,

    val inputEntryCount: Int,
    val finalEntryCount: Int,
    val categoryCount: Int,

    val uniqueCanonicalNameCount: Int,
    val uniqueNormalizedKeyCount: Int,

    val deterministicOrder: Boolean,
    val jsonRoundtripValid: Boolean,

    val familyPluralFallbackCount: Int,
    val linguisticRepairValid: Boolean,

    val sha256: String,

    val valid: Boolean
)

data class CanonicalFoodCatalogV2ReleasePaths(
    val finalCatalogPath: String,
    val immutableSnapshotPath: String,
    val finalizationReportPath: String,
    val productiveCatalogPath: String
)