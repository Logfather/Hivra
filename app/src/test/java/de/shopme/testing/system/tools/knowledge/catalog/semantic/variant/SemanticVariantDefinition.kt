package de.shopme.testing.system.tools.knowledge.catalog.semantic.variant

data class SemanticVariantDefinition(
    val canonicalKey: String,
    val displayName: String,
    val type: SemanticVariantType,
    val aliases: Set<String> = emptySet()
)