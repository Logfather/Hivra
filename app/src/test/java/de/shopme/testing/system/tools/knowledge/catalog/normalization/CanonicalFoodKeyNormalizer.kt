package de.shopme.testing.system.tools.knowledge.catalog.normalization

import java.text.Normalizer
import java.util.Locale

class CanonicalFoodKeyNormalizer {

    fun normalize(
        canonicalName: String
    ): String {
        require(canonicalName.isNotBlank()) {
            "canonicalName must not be blank."
        }

        val normalizedTokens = normalizeTokens(canonicalName)

        require(normalizedTokens.isNotEmpty()) {
            "canonicalName '$canonicalName' does not contain usable key tokens."
        }

        require(
            normalizedTokens.any { token ->
                token !in NON_SUBSTANTIVE_TOKENS
            }
        ) {
            "canonicalName '$canonicalName' contains only non-substantive tokens."
        }

        val normalizedKey = normalizedTokens.joinToString("-")

        require(isCanonicalKey(normalizedKey)) {
            "Generated normalized key '$normalizedKey' is not canonical."
        }

        return normalizedKey
    }

    fun isCanonicalKey(
        key: String
    ): Boolean =
        key.isNotBlank() &&
                key == key.trim() &&
                CANONICAL_KEY_REGEX.matches(key)

    fun normalizeTokens(
        text: String
    ): List<String> {
        if (text.isBlank()) {
            return emptyList()
        }

        val normalizedText = text
            .let(::normalizeGermanCharacters)
            .let(::normalizeFractions)
            .let(::normalizeApostrophes)
            .let(::normalizeUnicode)
            .let(::removeCombiningMarks)
            .lowercase(Locale.ROOT)
            .let(::normalizeSemanticSymbols)
            .let(::normalizeSeparators)
            .replace(NON_ALPHANUMERIC_REGEX, " ")
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")
            .trim()

        if (normalizedText.isBlank()) {
            return emptyList()
        }

        return normalizedText
            .split(MULTIPLE_WHITESPACE_REGEX)
            .asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .toList()
    }

    fun validate(
        key: String
    ): KeyValidationResult {
        if (key.isBlank()) {
            return KeyValidationResult(
                valid = false,
                normalizedKey = "",
                violations = listOf(KEY_IS_BLANK)
            )
        }

        val violations = mutableListOf<String>()

        if (key != key.trim()) {
            violations += SURROUNDING_WHITESPACE
        }

        if (key != key.lowercase(Locale.ROOT)) {
            violations += UPPERCASE_CHARACTERS
        }

        if ('_' in key) {
            violations += UNDERSCORE_USED
        }

        if (MULTIPLE_HYPHEN_REGEX.containsMatchIn(key)) {
            violations += MULTIPLE_HYPHENS
        }

        if (key.startsWith('-')) {
            violations += LEADING_HYPHEN
        }

        if (key.endsWith('-')) {
            violations += TRAILING_HYPHEN
        }

        if (key.any(Char::isWhitespace)) {
            violations += WHITESPACE_PRESENT
        }

        if (!CANONICAL_KEY_REGEX.matches(key)) {
            violations += INVALID_CHARACTER_SEQUENCE
        }

        val normalizedKey = normalizeExistingKeySafely(key)

        if (normalizedKey != key) {
            violations += KEY_DIFFERS_FROM_CANONICAL_NORMALIZATION
        }

        return KeyValidationResult(
            valid = violations.isEmpty(),
            normalizedKey = normalizedKey,
            violations = violations
                .distinct()
                .sorted()
        )
    }

    private fun normalizeExistingKeySafely(
        key: String
    ): String {
        if (key.isBlank()) {
            return ""
        }

        return normalizeTokens(key).joinToString("-")
    }

    /**
     * Must run before Unicode decomposition. Otherwise ä/ö/ü are already
     * split into base character plus combining mark and can no longer be
     * transliterated to ae/oe/ue.
     */
    private fun normalizeGermanCharacters(
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

    private fun normalizeFractions(
        value: String
    ): String =
        value
            .replace("½", " 1 2 ")
            .replace("¼", " 1 4 ")
            .replace("¾", " 3 4 ")

    private fun normalizeUnicode(
        value: String
    ): String =
        Normalizer.normalize(
            value,
            Normalizer.Form.NFKD
        )

    private fun removeCombiningMarks(
        value: String
    ): String =
        value.replace(COMBINING_MARKS_REGEX, "")

    private fun normalizeSemanticSymbols(
        value: String
    ): String =
        value
            .replace("%", " prozent ")
            .replace("&", " und ")
            .replace("+", " plus ")
            .replace("@", " at ")

    private fun normalizeApostrophes(
        value: String
    ): String =
        value.replace(APOSTROPHE_REGEX, "")

    private fun normalizeSeparators(
        value: String
    ): String =
        value
            .replace(DECIMAL_SEPARATOR_REGEX, " ")
            .replace(HYPHEN_VARIANT_REGEX, " ")
            .replace(PATH_SEPARATOR_REGEX, " ")
            .replace(BRACKET_REGEX, " ")
            .replace(UNDERSCORE_REGEX, " ")

    data class KeyValidationResult(
        val valid: Boolean,
        val normalizedKey: String,
        val violations: List<String>
    ) {

        init {
            require(violations.none(String::isBlank)) {
                "violations must not contain blank values."
            }

            require(violations.distinct().size == violations.size) {
                "violations must not contain duplicates."
            }

            require(violations == violations.sorted()) {
                "violations must be sorted."
            }

            require(valid == violations.isEmpty()) {
                "valid must be true exactly when violations is empty."
            }
        }
    }

    companion object {

        private val NON_SUBSTANTIVE_TOKENS = setOf(
            "at",
            "plus",
            "prozent",
            "und"
        )

        val CANONICAL_KEY_REGEX: Regex =
            Regex("^[a-z0-9]+(?:-[a-z0-9]+)*$")

        private const val KEY_IS_BLANK =
            "KEY_IS_BLANK"

        private const val SURROUNDING_WHITESPACE =
            "SURROUNDING_WHITESPACE"

        private const val UPPERCASE_CHARACTERS =
            "UPPERCASE_CHARACTERS"

        private const val UNDERSCORE_USED =
            "UNDERSCORE_USED"

        private const val MULTIPLE_HYPHENS =
            "MULTIPLE_HYPHENS"

        private const val LEADING_HYPHEN =
            "LEADING_HYPHEN"

        private const val TRAILING_HYPHEN =
            "TRAILING_HYPHEN"

        private const val WHITESPACE_PRESENT =
            "WHITESPACE_PRESENT"

        private const val INVALID_CHARACTER_SEQUENCE =
            "INVALID_CHARACTER_SEQUENCE"

        private const val KEY_DIFFERS_FROM_CANONICAL_NORMALIZATION =
            "KEY_DIFFERS_FROM_CANONICAL_NORMALIZATION"

        private val COMBINING_MARKS_REGEX =
            Regex("\\p{M}+")

        private val NON_ALPHANUMERIC_REGEX =
            Regex("[^a-z0-9]+")

        private val MULTIPLE_WHITESPACE_REGEX =
            Regex("\\s+")

        private val MULTIPLE_HYPHEN_REGEX =
            Regex("-{2,}")

        private val DECIMAL_SEPARATOR_REGEX =
            Regex("(?<=\\d)[,.](?=\\d)")

        private val PATH_SEPARATOR_REGEX =
            Regex("[/\\\\|;:]+")

        private val BRACKET_REGEX =
            Regex("[()\\[\\]{}<>]")

        private val UNDERSCORE_REGEX =
            Regex("_+")

        private val APOSTROPHE_REGEX =
            Regex("['’‘`´ʼ＇]")

        private val HYPHEN_VARIANT_REGEX =
            Regex("[-‐-‒–—−﹘﹣－]+")
    }
}