package de.shopme.testing.system.tools.knowledge.catalog.semantic.regeneration

import com.google.gson.JsonObject

data class SemanticExpansionRejectedEntry(
    val itemName: String,
    val normalized: String,
    val reason: String
)

data class SemanticExpansionReviewEntry(
    val itemName: String,
    val normalized: String,
    val reason: String
)

data class SemanticExpansionProjection(
    val sourceItemName: String,
    val sourceNormalized: String,
    val projectedItemName: String,
    val projectedNormalized: String,
    val removedKnowledgeVariantKeys: List<String>
)

data class SemanticExpansionRegenerationResult(
    val schemaVersion: Int,

    val inputCatalogEntryCount: Int,
    val baselineEntryCount: Int,
    val expansionInputEntryCount: Int,

    val acceptedInputEntryCount: Int,
    val rejectedInputEntryCount: Int,
    val reviewInputEntryCount: Int,

    val acceptedProjectedEntryCount: Int,
    val collapsedDuplicateCount: Int,
    val knowledgeAttributeProjectionCount: Int,

    val regeneratedExpansionEntryCount: Int,
    val projectedCatalogEntryCount: Int,

    val regeneratedExpansion: List<JsonObject>,
    val rejectedEntries: List<SemanticExpansionRejectedEntry>,
    val reviewEntries: List<SemanticExpansionReviewEntry>,
    val projections: List<SemanticExpansionProjection>
)