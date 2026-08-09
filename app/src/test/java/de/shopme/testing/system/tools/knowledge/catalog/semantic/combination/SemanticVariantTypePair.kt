package de.shopme.testing.system.tools.knowledge.catalog.semantic.combination

import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantType

data class SemanticVariantTypePair private constructor(
    val first: SemanticVariantType,
    val second: SemanticVariantType
) {

    companion object {

        fun of(
            first: SemanticVariantType,
            second: SemanticVariantType
        ): SemanticVariantTypePair {

            return if (first.name <= second.name) {
                SemanticVariantTypePair(
                    first = first,
                    second = second
                )
            } else {
                SemanticVariantTypePair(
                    first = second,
                    second = first
                )
            }
        }
    }
}