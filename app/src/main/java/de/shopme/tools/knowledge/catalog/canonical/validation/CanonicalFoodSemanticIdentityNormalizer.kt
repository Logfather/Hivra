package de.shopme.tools.knowledge.catalog.canonical.validation

class CanonicalFoodSemanticIdentityNormalizer {

    fun normalize(
        value: String
    ): String {

        var normalized =
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
                    NON_ALPHANUMERIC_REGEX,
                    " "
                )
                .replace(
                    WHITESPACE_REGEX,
                    " "
                )
                .trim()

        normalized =
            COMPOUND_EQUIVALENTS[
                normalized
            ]
                ?: normalized

        normalized =
            singularizeKnownIdentity(
                normalized
            )

        /*
         * Für Duplicate-Vergleich werden Worttrenner
         * anschließend entfernt.
         */
        return normalized
            .replace(
                " ",
                ""
            )
    }

    private fun singularizeKnownIdentity(
        value: String
    ): String =
        SINGULAR_PLURAL_EQUIVALENTS[
            value
        ]
            ?: value

    companion object {

        /*
         * Nur eindeutig bekannte deutsche Komposita.
         *
         * Kein generisches Zusammenkleben aller Wörter.
         */
        private val COMPOUND_EQUIVALENTS =
            mapOf(
                "basmati reis" to
                        "basmatireis",

                "apfelsaft schorle" to
                        "apfelschorle",

                "gemuese lasagne" to
                        "gemueselasagne",

                "bohnen mix" to
                        "bohnenmix",

                "ciabatta brot" to
                        "ciabatta",

                "pita brot" to
                        "pitabrot"
            )

        /*
         * Nur sichere Singular-/Pluralpaare.
         */
        private val SINGULAR_PLURAL_EQUIVALENTS =
            mapOf(
                "croissants" to
                        "croissant",

                "erdbeere" to
                        "erdbeeren",

                "himbeere" to
                        "himbeeren",

                "heidelbeere" to
                        "heidelbeeren",

                "cranberry" to
                        "cranberries",

                "dattel" to
                        "datteln",

                "feige" to
                        "feigen",

                "kirsche" to
                        "kirschen",

                "bratwuerste" to
                        "bratwurst"
            )

        private val NON_ALPHANUMERIC_REGEX =
            Regex(
                """[^a-z0-9]+"""
            )

        private val WHITESPACE_REGEX =
            Regex(
                """\s+"""
            )
    }
}