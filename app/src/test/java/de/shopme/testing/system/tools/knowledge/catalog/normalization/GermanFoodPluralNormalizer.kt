package de.shopme.testing.system.tools.knowledge.catalog.normalization

import java.text.Normalizer
import java.util.Locale

class GermanFoodPluralNormalizer {

    fun evaluate(
        singular: String,
        existingPlural: String?
    ): GermanPluralEvaluation {
        require(singular.isNotBlank()) {
            "singular must not be blank."
        }

        val normalizedSingular = normalizeVisibleText(singular)

        if (existingPlural.isNullOrBlank()) {
            val generatedPlural =
                generateSafePlural(normalizedSingular)

            return if (generatedPlural == null) {
                GermanPluralEvaluation(
                    normalizedPlural = null,
                    reason =
                        "No existing plural was provided and no safe " +
                                "deterministic German plural could be generated."
                )
            } else {
                GermanPluralEvaluation(
                    normalizedPlural = generatedPlural,
                    reason =
                        "Generated a safe deterministic German plural " +
                                "from the canonical singular."
                )
            }
        }

        val normalizedExistingPlural =
            normalizeVisibleText(existingPlural)

        if (normalizedExistingPlural.isBlank()) {
            return GermanPluralEvaluation(
                normalizedPlural = null,
                reason =
                    "Removed plural because it was empty after normalization."
            )
        }

        if (containsPluralMetadata(normalizedExistingPlural)) {
            val strippedPlural =
                stripPluralMetadata(normalizedExistingPlural)

            return GermanPluralEvaluation(
                normalizedPlural = strippedPlural
                    .takeIf(String::isNotBlank),
                reason =
                    "Removed explanatory metadata from the plural value."
            )
        }

        if (containsPluralAlternatives(normalizedExistingPlural)) {
            return GermanPluralEvaluation(
                normalizedPlural = normalizedExistingPlural,
                reason =
                    "Preserved plural alternatives for manual review because " +
                            "no deterministic single form can be selected safely."
            )
        }

        if (
            comparisonKey(normalizedExistingPlural) ==
            comparisonKey(normalizedSingular)
        ) {
            return if (isInvariantPlural(normalizedSingular)) {
                GermanPluralEvaluation(
                    normalizedPlural = normalizedSingular,
                    reason =
                        "Preserved identical singular and plural because the " +
                                "food name uses an invariant grammatical form."
                )
            } else {
                val generatedPlural =
                    generateSafePlural(normalizedSingular)

                GermanPluralEvaluation(
                    normalizedPlural =
                        generatedPlural ?: normalizedExistingPlural,
                    reason =
                        if (generatedPlural != null) {
                            "Replaced an invalid singular-identical plural " +
                                    "with a safe deterministic German plural."
                        } else {
                            "Preserved singular-identical plural because no " +
                                    "safe deterministic replacement was available."
                        }
                )
            }
        }

        val normalizedCapitalization =
            alignCapitalization(
                singular = normalizedSingular,
                plural = normalizedExistingPlural
            )

        return GermanPluralEvaluation(
            normalizedPlural = normalizedCapitalization,
            reason =
                if (normalizedCapitalization == existingPlural) {
                    "Preserved the existing German plural."
                } else {
                    "Normalized whitespace, punctuation and capitalization " +
                            "of the existing German plural."
                }
        )
    }

    fun normalize(
        singular: String,
        existingPlural: String?
    ): String? =
        evaluate(
            singular = singular,
            existingPlural = existingPlural
        ).normalizedPlural

    private fun generateSafePlural(
        singular: String
    ): String? {
        val normalized = normalizeVisibleText(singular)

        if (normalized.isBlank()) {
            return null
        }

        val exactPlural = IRREGULAR_PLURALS[
            comparisonKey(normalized)
        ]

        if (exactPlural != null) {
            return preservePrefixCapitalization(
                source = normalized,
                target = exactPlural
            )
        }

        if (isInvariantPlural(normalized)) {
            return normalized
        }

        val words = normalized.split(' ')

        if (words.isEmpty()) {
            return null
        }

        val finalWord = words.last()
        val generatedFinalWord =
            generateSafeFinalWordPlural(finalWord)
                ?: return null

        return (
                words.dropLast(1) +
                        generatedFinalWord
                ).joinToString(" ")
    }

    private fun generateSafeFinalWordPlural(
        word: String
    ): String? {
        val lowercaseWord = word.lowercase(Locale.GERMAN)

        IRREGULAR_FINAL_WORD_PLURALS[lowercaseWord]
            ?.let { plural ->
                return preservePrefixCapitalization(
                    source = word,
                    target = plural
                )
            }

        return when {
            lowercaseWord.endsWith("chen") ||
                    lowercaseWord.endsWith("lein") ->
                word

            lowercaseWord.endsWith("er") &&
                    lowercaseWord in INVARIANT_ER_WORDS ->
                word

            lowercaseWord.endsWith("e") ->
                "${word}n"

            lowercaseWord.endsWith("el") ||
                    lowercaseWord.endsWith("er") ||
                    lowercaseWord.endsWith("en") ->
                word

            lowercaseWord.endsWith("a") ||
                    lowercaseWord.endsWith("i") ||
                    lowercaseWord.endsWith("o") ||
                    lowercaseWord.endsWith("u") ||
                    lowercaseWord.endsWith("y") ->
                "${word}s"

            lowercaseWord.endsWith("nis") ->
                "${word}se"

            lowercaseWord.endsWith("sal") ->
                "${word}e"

            lowercaseWord.endsWith("ling") ->
                "${word}e"

            lowercaseWord.endsWith("ich") ->
                "${word}e"

            lowercaseWord.endsWith("ig") ->
                "${word}e"

            lowercaseWord.endsWith("eur") ->
                "${word}e"

            lowercaseWord.endsWith("or") ->
                "${word}en"

            lowercaseWord.endsWith("um") ->
                word.dropLast(2) + "en"

            lowercaseWord.endsWith("us") ->
                null

            lowercaseWord.length <= 3 ->
                null

            else ->
                "${word}e"
        }
    }

    private fun normalizeVisibleText(
        value: String
    ): String {
        var result = Normalizer.normalize(
            value,
            Normalizer.Form.NFKC
        )

        NON_CANONICAL_APOSTROPHES.forEach { apostrophe ->
            result = result.replace(apostrophe, '\'')
        }

        NON_CANONICAL_HYPHENS.forEach { hyphen ->
            result = result.replace(hyphen, '-')
        }

        return result
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")
            .replace(SPACE_AROUND_HYPHEN_REGEX, "-")
            .replace(SPACE_AROUND_APOSTROPHE_REGEX, "'")
            .trim()
    }

    private fun comparisonKey(
        value: String
    ): String =
        Normalizer.normalize(
            transliterateGermanCharacters(value),
            Normalizer.Form.NFKD
        )
            .replace(COMBINING_MARKS_REGEX, "")
            .lowercase(Locale.ROOT)
            .replace(NON_ALPHANUMERIC_REGEX, " ")
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")
            .trim()

    private fun containsPluralMetadata(
        value: String
    ): Boolean =
        PLURAL_METADATA_REGEX.containsMatchIn(value)

    private fun stripPluralMetadata(
        value: String
    ): String =
        value
            .replace(PLURAL_METADATA_REGEX, "")
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")
            .trim()

    private fun containsPluralAlternatives(
        value: String
    ): Boolean =
        PLURAL_ALTERNATIVE_REGEX.containsMatchIn(value)

    private fun isInvariantPlural(
        value: String
    ): Boolean {
        val key = comparisonKey(value)

        return key in INVARIANT_PLURALS ||
                INVARIANT_SUFFIXES.any(key::endsWith)
    }

    private fun alignCapitalization(
        singular: String,
        plural: String
    ): String {
        if (plural.isBlank()) {
            return plural
        }

        return if (
            singular.firstOrNull()?.isUpperCase() == true
        ) {
            uppercaseFirstLetter(plural)
        } else {
            plural
        }
    }

    private fun uppercaseFirstLetter(
        value: String
    ): String {
        val index = value.indexOfFirst(Char::isLetter)

        if (index < 0) {
            return value
        }

        return buildString(value.length) {
            append(value.substring(0, index))
            append(value[index].uppercaseChar())
            append(value.substring(index + 1))
        }
    }

    private fun preservePrefixCapitalization(
        source: String,
        target: String
    ): String =
        if (source.firstOrNull()?.isUpperCase() == true) {
            uppercaseFirstLetter(target)
        } else {
            target
        }

    private companion object {

        val MULTIPLE_WHITESPACE_REGEX =
            Regex("\\s+")

        val SPACE_AROUND_HYPHEN_REGEX =
            Regex("\\s*[-‐-‒–—−﹘﹣－]\\s*")

        val SPACE_AROUND_APOSTROPHE_REGEX =
            Regex("\\s*['’‘`´ʼ＇]\\s*")

        val COMBINING_MARKS_REGEX =
            Regex("\\p{M}+")

        val NON_ALPHANUMERIC_REGEX =
            Regex("[^a-z0-9]+")

        val PLURAL_METADATA_REGEX =
            Regex(
                "(?i)\\((?:plural|mehrzahl|pl\\.?|auch|selten)[^)]*\\)"
            )

        val PLURAL_ALTERNATIVE_REGEX =
            Regex("(?i)\\s+(?:oder|bzw\\.)\\s+|[/|;]")

        val NON_CANONICAL_APOSTROPHES = setOf(
            '’',
            '‘',
            '`',
            '´',
            'ʼ',
            '＇'
        )

        val NON_CANONICAL_HYPHENS = setOf(
            '‐',
            '-',
            '‒',
            '–',
            '—',
            '−',
            '﹘',
            '﹣',
            '－'
        )

        val INVARIANT_PLURALS = setOf(
            "ananas",
            "brokkoli",
            "couscous",
            "fisch",
            "fleisch",
            "gemuese",
            "hirse",
            "joghurt",
            "kaffee",
            "kaese",
            "mais",
            "obst",
            "reis",
            "salami",
            "sellerie",
            "spinat",
            "tofu"
        )

        val INVARIANT_SUFFIXES = setOf(
            "fleisch",
            "gemuese",
            "kaese",
            "obst",
            "reis"
        )

        val INVARIANT_ER_WORDS = setOf(
            "butter",
            "ingwer",
            "kefir",
            "zucker"
        )

        val IRREGULAR_PLURALS = mapOf(
            "apfel" to "Äpfel",
            "brot" to "Brote",
            "bruehe" to "Brühen",
            "ei" to "Eier",
            "glasnudel" to "Glasnudeln",
            "kraut" to "Kräuter",
            "nuss" to "Nüsse",
            "pilz" to "Pilze",
            "wurst" to "Würste"
        )

        val IRREGULAR_FINAL_WORD_PLURALS = mapOf(
            "apfel" to "Äpfel",
            "brot" to "Brote",
            "brühe" to "Brühen",
            "ei" to "Eier",
            "frucht" to "Früchte",
            "kraut" to "Kräuter",
            "nuss" to "Nüsse",
            "pilz" to "Pilze",
            "wurst" to "Würste"
        )
    }

    private fun transliterateGermanCharacters(
        value: String
    ): String =
        value
            .replace("Ä", "Ae")
            .replace("Ö", "Oe")
            .replace("Ü", "Ue")
            .replace("ä", "ae")
            .replace("ö", "oe")
            .replace("ü", "ue")
            .replace("ẞ", "SS")
            .replace("ß", "ss")
}