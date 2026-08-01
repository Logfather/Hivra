package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.coverage

import java.util.Locale

class ResultingNutritionCoverageCanonicalIdNormalizer {

    fun normalize(
        canonicalId: String
    ): String =
        canonicalId
            .trim()
            .lowercase(
                Locale.ROOT
            )
            .replace(
                HYPHEN_REGEX,
                " "
            )
            .replace(
                WHITESPACE_REGEX,
                " "
            )
            .trim()

    private companion object {

        val HYPHEN_REGEX =
            Regex(
                "[-‐-‒–—―]+"
            )

        val WHITESPACE_REGEX =
            Regex(
                "\\s+"
            )
    }
}