package de.shopme.tools.knowledge.off.nutrition.reference.retrieval

import java.text.Normalizer
import java.util.Locale

object OFFNutritionRetrievalTextNormalizer {

    private val nonAlphaNumericRegex =
        Regex("[^a-z0-9]+")

    private val whitespaceRegex =
        Regex("\\s+")

    fun normalize(
        value: String
    ): String {

        val decomposed =
            Normalizer.normalize(
                value,
                Normalizer.Form.NFKD
            )

        return decomposed
            .lowercase(Locale.ROOT)
            .replace("&", " and ")
            .replace(nonAlphaNumericRegex, " ")
            .replace(whitespaceRegex, " ")
            .trim()
    }

    fun tokenize(
        value: String
    ): List<String> =
        normalize(value)
            .split(' ')
            .asSequence()
            .filter(String::isNotBlank)
            .filterNot { token ->
                token in STOP_WORDS
            }
            .distinct()
            .sorted()
            .toList()

    private val STOP_WORDS =
        setOf(
            "a",
            "an",
            "and",
            "de",
            "der",
            "die",
            "das",
            "des",
            "ein",
            "eine",
            "for",
            "in",
            "of",
            "the",
            "to",
            "und",
            "with"
        )
}