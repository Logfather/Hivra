package de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility

import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantType

enum class ProductFamilyVariantCompatibilityDecision {
    ALLOW,
    REJECT,
    REVIEW
}

enum class ProductFamilyVariantCompatibilityReason {
    EXPLICIT_FAMILY_VARIANT_ALLOW,
    EXPLICIT_FAMILY_TYPE_ALLOW,
    EXPLICIT_FAMILY_VARIANT_REJECT,
    EXPLICIT_FAMILY_TYPE_REJECT,
    GLOBAL_VARIANT_TYPE_REJECT,
    CATEGORY_TYPE_ALLOW,

    SEMANTIC_PROFILE_TYPE_ALLOW,

    SEMANTIC_PROFILE_TYPE_REJECT,
    NO_EXPLICIT_COMPATIBILITY_RULE
}

data class ProductFamilyVariantCompatibilityResult(
    val family: String,
    val normalizedFamily: String,
    val category: String,
    val variantRawValue: String,
    val variantCanonicalKey: String,
    val variantType: SemanticVariantType,
    val decision: ProductFamilyVariantCompatibilityDecision,
    val reason: ProductFamilyVariantCompatibilityReason
)