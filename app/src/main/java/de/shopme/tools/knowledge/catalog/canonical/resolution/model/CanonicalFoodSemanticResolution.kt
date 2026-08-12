package de.shopme.tools.knowledge.catalog.canonical.resolution.model

enum class CanonicalFoodSemanticResolutionAction {

    KEEP,

    MERGE,

    REJECT_INVALID_IDENTITY,

    CORRECT_CATEGORY
}

data class CanonicalFoodSemanticResolutionDecision(
    val sourceItemname: String,
    val sourceNormalized: String,
    val action: CanonicalFoodSemanticResolutionAction,
    val targetItemname: String?,
    val targetNormalized: String?,
    val reason: String
)

data class CanonicalFoodSemanticResolutionReport(
    val version: Int,
    val inputEntryCount: Int,
    val outputEntryCount: Int,
    val mergedEntryCount: Int,
    val rejectedEntryCount: Int,
    val categoryCorrectionCount: Int,
    val outputFile: String,
    val decisionsFile: String
)