package de.shopme.tools.knowledge.catalog.truecanonical

import java.text.Normalizer

class TrueCanonicalFoodNameNormalizer {

    fun normalize(
        value: String
    ): String {

        val germanNormalized =
            value
                .trim()
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

        val decomposed =
            Normalizer.normalize(
                germanNormalized,
                Normalizer.Form.NFD
            )

        return decomposed
            .replace(
                DIACRITIC_REGEX,
                ""
            )
            .replace(
                NON_ALPHANUMERIC_REGEX,
                "-"
            )
            .replace(
                MULTIPLE_HYPHEN_REGEX,
                "-"
            )
            .trim(
                '-'
            )
    }

    companion object {

        private val DIACRITIC_REGEX =
            Regex(
                """\p{M}+"""
            )

        private val NON_ALPHANUMERIC_REGEX =
            Regex(
                """[^a-z0-9]+"""
            )

        private val MULTIPLE_HYPHEN_REGEX =
            Regex(
                """-+"""
            )
    }
}