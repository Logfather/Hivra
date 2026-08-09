package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic

fun interface CanonicalExpansionSemanticRule {

    fun evaluate(
        context: CanonicalExpansionSemanticContext
    ): List<CanonicalExpansionSemanticFinding>
}