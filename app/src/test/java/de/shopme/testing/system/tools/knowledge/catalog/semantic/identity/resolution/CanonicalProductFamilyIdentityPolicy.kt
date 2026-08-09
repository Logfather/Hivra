package de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.resolution

import java.text.Normalizer

object CanonicalProductFamilyIdentityPolicy {

    /*
     * IMPORTANT:
     *
     * Diese Regex-Instanzen müssen vor identitiesByLegacyName
     * initialisiert werden, weil dessen Initialisierung bereits
     * key(...) aufruft.
     */
    private val COMBINING_MARKS_REGEX =
        Regex("\\p{M}+")

    private val NON_ALPHANUMERIC_REGEX =
        Regex("[^a-z0-9]+")

    private val WHITESPACE_REGEX =
        Regex("\\s+")

    data class CanonicalFamilyIdentity(
        val canonicalName: String,
        val plural: String
    )

    /*
     * Explizite Korrekturen historischer bzw. künstlicher
     * Product-Family-Bezeichnungen.
     *
     * Diese Policy beschreibt kanonische Produktidentitäten,
     * keine Marken.
     */
    private val identitiesByLegacyName =
        mapOf(

            key("Colagetränke") to
                    CanonicalFamilyIdentity(
                        canonicalName = "Cola",
                        plural = "Cola"
                    )
        )

    fun canonicalIdentityFor(
        family: String
    ): CanonicalFamilyIdentity? =
        identitiesByLegacyName[
            key(family)
        ]

    fun canonicalFamilyName(
        family: String
    ): String =
        canonicalIdentityFor(family)
            ?.canonicalName
            ?: family

    fun canonicalPlural(
        family: String
    ): String? =
        canonicalIdentityFor(family)
            ?.plural

    private fun key(
        value: String
    ): String {

        val decomposed =
            Normalizer.normalize(
                value
                    .trim()
                    .lowercase(),
                Normalizer.Form.NFD
            )

        return decomposed
            .replace(
                COMBINING_MARKS_REGEX,
                ""
            )
            .replace(
                "ß",
                "ss"
            )
            .replace(
                NON_ALPHANUMERIC_REGEX,
                " "
            )
            .replace(
                WHITESPACE_REGEX,
                " "
            )
            .trim()
    }
}