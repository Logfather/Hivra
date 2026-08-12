package de.shopme.tools.knowledge.catalog.canonical.validation.model

enum class CanonicalFoodSemanticSeverity {
    ERROR,
    REVIEW
}

enum class CanonicalFoodSemanticIssueType {

    INVALID_PRODUCT_IDENTITY,

    TOO_SPECIFIC_PRODUCT_IDENTITY,

    KNOWLEDGE_ATTRIBUTE_IN_IDENTITY,

    SEMANTICALLY_IMPLAUSIBLE_IDENTITY,

    INVALID_CATEGORY,

    DUPLICATE_BASE_IDENTITY,

    INVALID_VARIANT,

    DUPLICATE_SOURCE_VARIANT,

    SOURCE_VARIANT_EQUALS_CANONICAL_NAME
}

data class CanonicalFoodSemanticIssue(
    val catalogKey: String,
    val itemname: String,
    val severity: CanonicalFoodSemanticSeverity,
    val type: CanonicalFoodSemanticIssueType,
    val message: String
)

data class CanonicalFoodSemanticEntryValidation(
    val catalogKey: String,
    val itemname: String,
    val category: String,
    val valid: Boolean,
    val issues: List<CanonicalFoodSemanticIssue>
)

data class CanonicalFoodSemanticValidationResult(
    val version: Int,
    val catalogEntryCount: Int,
    val validEntryCount: Int,
    val invalidEntryCount: Int,
    val reviewEntryCount: Int,
    val errorIssueCount: Int,
    val reviewIssueCount: Int,
    val issueCounts: Map<String, Int>,
    val entries: List<CanonicalFoodSemanticEntryValidation>
)