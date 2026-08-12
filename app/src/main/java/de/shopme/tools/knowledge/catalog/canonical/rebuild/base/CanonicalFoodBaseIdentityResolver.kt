package de.shopme.tools.knowledge.catalog.canonical.rebuild.base

data class CanonicalFoodBaseIdentityResolution(
    val itemname: String,
    val normalized: String,
    val extractedVariants: List<String>,
    val rejected: Boolean,
    val reason: String
)

class CanonicalFoodBaseIdentityResolver(
    private val compatibilityRegistry:
    CanonicalFoodVariantCompatibilityRegistry =
        CanonicalFoodVariantCompatibilityRegistry()
) {

    fun resolve(
        itemname: String,
        category: String
    ): CanonicalFoodBaseIdentityResolution {

        val original =
            itemname.trim()

        require(
            original.isNotBlank()
        )

        var base =
            original

        val variants =
            linkedSetOf<String>()

        /*
         * Claims / Meta-Attribute zuerst entfernen.
         *
         * Sie werden NICHT zu variants[].
         */
        base =
            removeMetaAttributes(
                base
            )

        VARIANT_RULES
            .forEach { rule ->

                if (
                    rule.pattern
                        .containsMatchIn(
                            base
                        )
                ) {

                    variants +=
                        rule.variant

                    base =
                        rule.pattern
                            .replace(
                                base,
                                " "
                            )
                }
            }

        base =
            cleanupBaseName(
                base
            )

        base =
            canonicalizeKnownBaseName(
                base
            )

        if (
            base.isBlank()
        ) {

            return CanonicalFoodBaseIdentityResolution(
                itemname =
                    original,

                normalized =
                    normalizeKey(
                        original
                    ),

                extractedVariants =
                    emptyList(),

                rejected =
                    true,

                reason =
                    "Removing non-identity attributes leaves no product identity."
            )
        }

        val incompatibleVariant =
            variants
                .firstOrNull { variant ->

                    !compatibilityRegistry
                        .isCompatible(
                            baseName =
                                base,
                            category =
                                category,
                            variant =
                                variant
                        )
                }

        if (
            incompatibleVariant !=
            null
        ) {

            return CanonicalFoodBaseIdentityResolution(
                itemname =
                    base,

                normalized =
                    normalizeKey(
                        base
                    ),

                extractedVariants =
                    variants.sorted(),

                rejected =
                    true,

                reason =
                    "Variant '$incompatibleVariant' is not market-plausible " +
                            "for canonical base identity '$base'."
            )
        }

        return CanonicalFoodBaseIdentityResolution(
            itemname =
                base,

            normalized =
                normalizeKey(
                    base
                ),

            extractedVariants =
                variants.sorted(),

            rejected =
                false,

            reason =
                if (
                    base ==
                    original &&
                    variants.isEmpty()
                ) {
                    "Identity already represents canonical base product."
                } else {
                    "Resolved product description to canonical base identity."
                }
        )
    }

    private fun removeMetaAttributes(
        value: String
    ): String {

        var current =
            value

        META_ATTRIBUTE_PATTERNS
            .forEach { pattern ->

                current =
                    pattern.replace(
                        current,
                        " "
                    )
            }

        return current
    }

    private fun cleanupBaseName(
        value: String
    ): String =
        value
            .replace(
                PUNCTUATION_PADDING_REGEX,
                " "
            )
            .replace(
                WHITESPACE_REGEX,
                " "
            )
            .trim(
                ' ',
                ',',
                '-',
                '–'
            )
            .trim()

    private fun canonicalizeKnownBaseName(
        value: String
    ): String {

        val trimmed =
            value.trim()

        val normalized =
            normalizeKey(
                trimmed
            )

        KNOWN_BASE_NAME_CORRECTIONS[
            normalized
        ]
            ?.let {
                return it
            }

        /*
         * Kleine grammatikalische Korrekturen,
         * die beim Entfernen eines vorangestellten
         * Adjektivs entstehen können.
         */
        return trimmed
            .replace(
                Regex(
                    """(?i)^grünen bohnen$"""
                ),
                "Grüne Bohnen"
            )
            .replace(
                Regex(
                    """(?i)^gruenen bohnen$"""
                ),
                "Grüne Bohnen"
            )
    }

    private fun normalizeKey(
        value: String
    ): String =
        value
            .lowercase()
            .replace(
                "ä",
                "ae"
            )
            .replace(
                "ö",
                "oe"
            )
            .replace(
                "ü",
                "ue"
            )
            .replace(
                "ß",
                "ss"
            )
            .replace(
                Regex(
                    """[^a-z0-9]+"""
                ),
                "-"
            )
            .trim(
                '-'
            )

    private data class VariantRule(
        val variant: String,
        val pattern: Regex
    )

    companion object {

        private val VARIANT_RULES =
            listOf(

                VariantRule(
                    variant =
                        CanonicalFoodVariantCompatibilityRegistry
                            .VARIANT_FROZEN,

                    pattern =
                        Regex(
                            """(?i)\b(""" +
                                    """gefro(?:ren|rene|rener|renes)|""" +
                                    """tiefgekühlt|tiefgekuehlt|""" +
                                    """tiefkühl|tiefkuehl|tk""" +
                                    """)\b"""
                        )
                ),

                VariantRule(
                    variant =
                        CanonicalFoodVariantCompatibilityRegistry
                            .VARIANT_FRESH,

                    pattern =
                        Regex(
                            """(?i)\b(frisch|frische|frischer|frisches)\b"""
                        )
                ),

                VariantRule(
                    variant =
                        CanonicalFoodVariantCompatibilityRegistry
                            .VARIANT_DRIED,

                    pattern =
                        Regex(
                            """(?i)\b(getrocknet|""" +
                                    """getrocknete|getrockneter|getrocknetes)\b"""
                        )
                ),

                VariantRule(
                    variant =
                        CanonicalFoodVariantCompatibilityRegistry
                            .VARIANT_SMOKED,

                    pattern =
                        Regex(
                            """(?i)\b(geräuchert|geraeuchert|""" +
                                    """geräucherte|geraeucherte)\b"""
                        )
                ),

                VariantRule(
                    variant =
                        CanonicalFoodVariantCompatibilityRegistry
                            .VARIANT_CANNED,

                    pattern =
                        Regex(
                            """(?i)\b(konserviert|dose|dosenware)\b"""
                        )
                ),

                VariantRule(
                    variant =
                        CanonicalFoodVariantCompatibilityRegistry
                            .VARIANT_SLICED,

                    pattern =
                        Regex(
                            """(?i)\b(geschnitten|geschnittene)\b"""
                        )
                ),

                VariantRule(
                    variant =
                        CanonicalFoodVariantCompatibilityRegistry
                            .VARIANT_DICED,

                    pattern =
                        Regex(
                            """(?i)\b(gewürfelt|gewuerfelt)\b"""
                        )
                ),

                VariantRule(
                    variant =
                        CanonicalFoodVariantCompatibilityRegistry
                            .VARIANT_GRATED,

                    pattern =
                        Regex(
                            """(?i)\b(geraspelt|geraspelte)\b"""
                        )
                ),

                VariantRule(
                    variant =
                        CanonicalFoodVariantCompatibilityRegistry
                            .VARIANT_GROUND,

                    pattern =
                        Regex(
                            """(?i)\b(gemahlen|gemahlene|gemahlener)\b"""
                        )
                )
            )

        /*
         * Diese Begriffe sind keine Produktvarianten
         * im Catalog-Modell.
         *
         * Bio → Production
         * Standard → kein semantischer Mehrwert
         */
        private val META_ATTRIBUTE_PATTERNS =
            listOf(
                Regex(
                    """(?i)\b(bio|biologisch|organic)\b"""
                ),

                Regex(
                    """(?i)\bstandard\b"""
                )
            )

        private val KNOWN_BASE_NAME_CORRECTIONS =
            mapOf(
                "tiefkuehlbeeren-gemischt" to
                        "Beerenmischung",

                "tiefkuehlbeeren-mix" to
                        "Beerenmischung",

                "gemischtes-beerenobst" to
                        "Beerenmischung",

                "rote-beeren-mix" to
                        "Rote Beerenmischung",

                "waldbeerenmix" to
                        "Waldbeerenmischung"
            )

        private val PUNCTUATION_PADDING_REGEX =
            Regex(
                """\s*[,;/]+\s*"""
            )

        private val WHITESPACE_REGEX =
            Regex(
                """\s+"""
            )
    }
}