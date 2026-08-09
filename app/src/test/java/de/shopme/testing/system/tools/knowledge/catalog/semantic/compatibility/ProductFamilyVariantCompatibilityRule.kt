package de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility

import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantType

data class ProductFamilyVariantCompatibilityRule(
    val familyKey: String,

    val allowedTypes: Set<SemanticVariantType> =
        emptySet(),

    val rejectedTypes: Set<SemanticVariantType> =
        emptySet(),

    val allowedVariantKeys: Set<String> =
        emptySet(),

    val rejectedVariantKeys: Set<String> =
        emptySet()
)