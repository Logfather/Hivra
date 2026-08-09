package de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.resolution

object CanonicalGermanCompoundIdentityPolicy {

    data class Definition(
        val family: String,
        val variant: String,
        val canonicalItemName: String
    )

    private val definitions =
        listOf(

            Definition(
                family = "Fruchtsaft",
                variant = "Apfel",
                canonicalItemName = "Apfelsaft"
            ),

            Definition(
                family = "Fruchtsaft",
                variant = "Orange",
                canonicalItemName = "Orangensaft"
            )
        )

    private val byPair =
        definitions.associateBy {
            Pair(
                it.family,
                it.variant
            )
        }

    init {

        require(
            byPair.size ==
                    definitions.size
        ) {
            "Canonical German compound identity pairs must be unique."
        }
    }

    fun canonicalItemName(
        family: String,
        variant: String
    ): String? =
        byPair[
            Pair(
                family,
                variant
            )
        ]
            ?.canonicalItemName
}