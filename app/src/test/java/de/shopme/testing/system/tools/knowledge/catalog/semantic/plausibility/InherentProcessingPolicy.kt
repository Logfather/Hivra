package de.shopme.testing.system.tools.knowledge.catalog.semantic.plausibility

import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantTaxonomyRegistry

object InherentProcessingPolicy {

    private val inherentVariantKeysByFamily =
        mapOf(

            key("Brot") to
                    setOf(
                        "baked"
                    ),

            key("Baguette") to
                    setOf(
                        "baked"
                    ),

            key("Brötchen") to
                    setOf(
                        "baked"
                    ),

            key("Toastbrot") to
                    setOf(
                        "baked"
                    ),

            key("Knäckebrot") to
                    setOf(
                        "baked"
                    ),

            key("Kuchen") to
                    setOf(
                        "baked"
                    )
        )

    fun isInherent(
        family: String,
        variantKey: String
    ): Boolean =
        variantKey in (
                inherentVariantKeysByFamily[
                    key(family)
                ]
                    ?: emptySet()
                )

    private fun key(
        value: String
    ): String =
        SemanticVariantTaxonomyRegistry
            .normalizeLookupKey(value)
}