package de.shopme.testing.system.tools.knowledge.catalog.semantic.plausibility

import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantTaxonomyRegistry

object MarketPlausibilityPolicy {

    private val explicitRejectedVariantsByFamily =
        mapOf(

            key("Butter") to
                    setOf(
                        "aged",
                        "extra-aged",
                        "cave-aged",
                        "brine-ripened",
                        "baked",
                        "boiled",
                        "deep-fried",
                        "sliced"
                    ),

            key("Frischkäse") to
                    setOf(
                        "baked",
                        "boiled",
                        "deep-fried",
                        "cave-aged",
                        "brine-ripened"
                    ),

            key("Mineralwasser") to
                    setOf(
                        "beef",
                        "barley",
                        "bean",
                        "carrot",
                        "almond"
                    ),

            key("Fruchtsaft") to
                    setOf(
                        "beef",
                        "barley",
                        "bean",
                        "carrot"
                    ),

            key("Backschokolade") to
                    setOf(
                        "beef",
                        "barley",
                        "bean",
                        "diced",
                        "grated"
                    )
        )

    /*
     * Nur wirklich offensichtliche positive Marktidentitäten.
     *
     * Dieser Satz bleibt bewusst klein.
     */
    private val explicitAcceptedVariantsByFamily =
        mapOf(

            key("Brot") to
                    setOf(
                        "barley",
                        "buckwheat"
                    ),

            key("Hartkäse") to
                    setOf(
                        "aged",
                        "extra-aged"
                    ),

            key("Fruchtsaft") to
                    setOf(
                        "apple"
                    ),

            key("Hähnchenfleisch") to
                    setOf(
                        "fillet",
                        "schnitzel-cut"
                    ),

            key("Kalbfleisch") to
                    setOf(
                        "fillet",
                        "chop",
                        "schnitzel-cut"
                    )
        )

    fun isExplicitlyRejected(
        family: String,
        variantKey: String
    ): Boolean =
        variantKey in (
                explicitRejectedVariantsByFamily[
                    key(family)
                ]
                    ?: emptySet()
                )

    fun isExplicitlyAccepted(
        family: String,
        variantKey: String
    ): Boolean =
        variantKey in (
                explicitAcceptedVariantsByFamily[
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