package de.shopme.testing.system.tools.knowledge.catalog.normalization

import java.text.Normalizer
import java.util.Locale

class CanonicalFoodNameNormalizer {

    fun normalize(
        itemName: String
    ): String {
        require(itemName.isNotBlank()) {
            "itemName must not be blank."
        }

        val normalized = itemName
            .let(::normalizeUnicode)
            .let(::normalizeWhitespaceCharacters)
            .let(::normalizeApostrophes)
            .let(::normalizeHyphens)
            .let(::normalizePunctuationSpacing)
            .let(::collapseWhitespace)
            .let(::removeInvalidBoundaryPunctuation)
            .let(::normalizeCapitalization)
            .trim()

        require(normalized.isNotBlank()) {
            "itemName '$itemName' does not contain a usable canonical name."
        }

        return normalized
    }

    fun isCanonicalName(
        value: String
    ): Boolean {
        if (value.isBlank()) {
            return false
        }

        return runCatching {
            normalize(value) == value
        }.getOrDefault(false)
    }

    private fun normalizeUnicode(
        value: String
    ): String =
        Normalizer.normalize(
            value,
            Normalizer.Form.NFKC
        )

    private fun normalizeWhitespaceCharacters(
        value: String
    ): String =
        buildString(value.length) {
            value.forEach { character ->
                append(
                    if (character.isWhitespace()) {
                        ' '
                    } else {
                        character
                    }
                )
            }
        }

    private fun normalizeApostrophes(
        value: String
    ): String {
        var result = value

        NON_CANONICAL_APOSTROPHES.forEach { apostrophe ->
            result = result.replace(apostrophe, '\'')
        }

        return result.replace(
            SPACE_AROUND_APOSTROPHE_REGEX,
            "'"
        )
    }

    private fun normalizeHyphens(
        value: String
    ): String {
        var result = value

        NON_CANONICAL_HYPHENS.forEach { hyphen ->
            result = result.replace(hyphen, '-')
        }

        return result
            .replace(SPACE_AROUND_HYPHEN_REGEX, "-")
            .replace(MULTIPLE_HYPHENS_REGEX, "-")
            .replace(PERCENT_DESCRIPTOR_BOUNDARY_REGEX, "% -")
    }

    private fun normalizePunctuationSpacing(
        value: String
    ): String =
        value
            .replace(SPACE_BEFORE_PUNCTUATION_REGEX, "$1")
            .replace(SPACE_AFTER_OPENING_BRACKET_REGEX, "$1")
            .replace(SPACE_BEFORE_CLOSING_BRACKET_REGEX, "$1")
            .replace(
                MISSING_SPACE_AFTER_COMMA_REGEX,
                ", "
            )
            .replace(
                MISSING_SPACE_AFTER_SEMICOLON_OR_COLON_REGEX,
                "$1 "
            )
            .replace(MULTIPLE_COMMAS_REGEX, ",")
            .replace(MULTIPLE_SEMICOLONS_REGEX, ";")

    private fun collapseWhitespace(
        value: String
    ): String =
        value
            .trim()
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")

    private fun removeInvalidBoundaryPunctuation(
        value: String
    ): String =
        value
            .replace(LEADING_SEPARATOR_REGEX, "")
            .replace(TRAILING_SEPARATOR_REGEX, "")
            .trim()

    private fun normalizeCapitalization(
        value: String
    ): String {
        if (value.isBlank()) {
            return value
        }

        val tokens = WORD_OR_SEPARATOR_REGEX
            .findAll(value)
            .map { it.value }
            .toList()

        if (tokens.isEmpty()) {
            return value
        }

        var lexicalWordIndex = 0
        var capitalizeNextLexicalToken = true

        return buildString(value.length) {
            tokens.forEach { token ->
                if (!token.any(Char::isLetter)) {
                    append(token)

                    if (token.contains('-')) {
                        capitalizeNextLexicalToken = true
                    }

                    return@forEach
                }

                val normalizedToken = normalizeWordCapitalization(
                    token = token,
                    lexicalWordIndex = lexicalWordIndex,
                    forceCapitalization =
                        capitalizeNextLexicalToken
                )

                append(normalizedToken)

                lexicalWordIndex++
                capitalizeNextLexicalToken = false
            }
        }
    }

    private fun normalizeWordCapitalization(
        token: String,
        lexicalWordIndex: Int,
        forceCapitalization: Boolean
    ): String {
        if (token in CASE_SENSITIVE_TERMS) {
            return token
        }

        val lowercaseToken = token.lowercase(Locale.GERMAN)

        CASE_SENSITIVE_TERMS_BY_LOWERCASE[
            lowercaseToken
        ]?.let {
            return it
        }

        if (isRomanNumeral(token)) {
            return token.uppercase(Locale.ROOT)
        }

        if (isKnownAbbreviation(token)) {
            return token.uppercase(Locale.ROOT)
        }

        if (
            !forceCapitalization &&
            lexicalWordIndex > 0 &&
            lowercaseToken in LOWERCASE_CONNECTORS
        ) {
            return lowercaseToken
        }

        if ('-' in lowercaseToken) {
            return lowercaseToken
                .split('-')
                .mapIndexed { partIndex, part ->
                    when {
                        part.isBlank() ->
                            part

                        partIndex > 0 &&
                                part in LOWERCASE_CONNECTORS ->
                            part

                        partIndex > 0 &&
                                part in LOWERCASE_DESCRIPTOR_TERMS ->
                            part

                        else ->
                            uppercaseFirstLetter(part)
                    }
                }
                .joinToString("-")
        }

        if (
            lexicalWordIndex > 0 &&
            lowercaseToken in LOWERCASE_DESCRIPTOR_TERMS
        ) {
            return lowercaseToken
        }

        return uppercaseFirstLetter(lowercaseToken)
    }

    private fun uppercaseFirstLetter(
        value: String
    ): String {
        val firstLetterIndex = value.indexOfFirst(Char::isLetter)

        if (firstLetterIndex < 0) {
            return value
        }

        return buildString(value.length) {
            append(value.substring(0, firstLetterIndex))
            append(
                value[firstLetterIndex]
                    .uppercaseChar()
            )
            append(value.substring(firstLetterIndex + 1))
        }
    }

    private fun isRomanNumeral(
        value: String
    ): Boolean =
        value == value.uppercase(Locale.ROOT) &&
                ROMAN_NUMERAL_REGEX.matches(value)

    private fun isKnownAbbreviation(
        value: String
    ): Boolean =
        value.lowercase(Locale.ROOT) in KNOWN_ABBREVIATIONS

    private companion object {

        val PERCENT_DESCRIPTOR_BOUNDARY_REGEX =
            Regex("%-(?=\\p{L})")

        val LOWERCASE_DESCRIPTOR_TERMS = setOf(
            "cremig",
            "fein",
            "frisch",
            "geraeuchert",
            "geröstet",
            "gekocht",
            "getrocknet",
            "herb",
            "mild",
            "natur",
            "pikant",
            "roh",
            "scharf",
            "suess",
            "süß",
            "ungesuesst",
            "ungesüßt",
            "wuerzig",
            "würzig"
        )

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

        val MULTIPLE_WHITESPACE_REGEX =
            Regex("\\s+")

        val SPACE_AROUND_APOSTROPHE_REGEX =
            Regex("\\s*['’‘`´ʼ＇]\\s*")

        val SPACE_AROUND_HYPHEN_REGEX =
            Regex("\\s*[-‐-‒–—−﹘﹣－]\\s*")

        val MULTIPLE_HYPHENS_REGEX =
            Regex("-{2,}")

        val SPACE_BEFORE_PUNCTUATION_REGEX =
            Regex("\\s+([,;:.!?])")

        val MISSING_SPACE_AFTER_COMMA_REGEX =
            Regex(",(?=\\p{L})")

        val MISSING_SPACE_AFTER_SEMICOLON_OR_COLON_REGEX =
            Regex("([;:])(?=[\\p{L}\\p{N}])")

        val SPACE_AFTER_OPENING_BRACKET_REGEX =
            Regex("([\\[(])\\s+")

        val SPACE_BEFORE_CLOSING_BRACKET_REGEX =
            Regex("\\s+([\\])])")

        val MULTIPLE_COMMAS_REGEX =
            Regex(",{2,}")

        val MULTIPLE_SEMICOLONS_REGEX =
            Regex(";{2,}")

        val LEADING_SEPARATOR_REGEX =
            Regex("^[,;:/|\\-]+\\s*")

        val TRAILING_SEPARATOR_REGEX =
            Regex("\\s*[,;:/|\\-]+$")

        val WORD_OR_SEPARATOR_REGEX =
            Regex(
                "[\\p{L}\\p{M}\\p{N}]+(?:['-][\\p{L}\\p{M}\\p{N}]+)*|" +
                        "[^\\p{L}\\p{M}\\p{N}]+"
            )

        val ROMAN_NUMERAL_REGEX =
            Regex(
                "^(?=.)M{0,4}(CM|CD|D?C{0,3})" +
                        "(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$"
            )

        val LOWERCASE_CONNECTORS = setOf(
            "a",
            "alla",
            "au",
            "aux",
            "de",
            "del",
            "della",
            "di",
            "du",
            "et",
            "la",
            "mit",
            "nach",
            "oder",
            "und",
            "van",
            "von"
        )

        val KNOWN_ABBREVIATIONS = setOf(
            "bbq",
            "g.g.a.",
            "g.g.u.",
            "g.t.s.",
            "i.g.p.",
            "tk"
        )

        val CASE_SENSITIVE_TERMS = setOf(
            "BBQ",
            "Crème",
            "Crème fraîche",
            "Gnocchi",
            "Gouda",
            "Kefir",
            "Kimchi",
            "Mozzarella",
            "Parmesan",
            "Pesto",
            "Quinoa",
            "Skyr",
            "Tofu"
        )

        val CASE_SENSITIVE_TERMS_BY_LOWERCASE =
            CASE_SENSITIVE_TERMS.associateBy {
                it.lowercase(Locale.GERMAN)
            }
    }
}