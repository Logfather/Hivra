package de.shopme.tools.knowledge.catalog.canonical.rebuild.base.model

enum class CanonicalFoodBaseIdentityResolutionAction {

    KEEP,

    CONSOLIDATE,

    REJECT_SEMANTICALLY_IMPLAUSIBLE
}

data class CanonicalFoodBaseIdentityResolutionDecision(
    val sourceItemname: String,
    val sourceNormalized: String,
    val action: CanonicalFoodBaseIdentityResolutionAction,
    val resolvedItemname: String?,
    val resolvedNormalized: String?,
    val extractedVariants: List<String>,
    val reason: String
)

data class CanonicalFoodBaseIdentityResolutionReport(
    val version: Int,
    val inputEntryCount: Int,
    val outputEntryCount: Int,
    val consolidatedEntryCount: Int,
    val rejectedSemanticallyImplausibleCount: Int,
    val outputVariantCount: Int,
    val outputSourceVariantCount: Int,
    val outputFile: String,
    val decisionsFile: String
)