package de.shopme.testing.system.tools.knowledge.catalog.normalization

import java.text.Normalizer
import java.util.Locale

class CatalogTokenNormalizer {

    fun normalizeAliases(
        values: List<String>,
        canonicalName: String
    ): List<String> {
        require(canonicalName.isNotBlank()) {
            "canonicalName must not be blank."
        }

        val canonicalComparisonKey =
            normalizeComparisonValue(canonicalName)

        return values
            .asSequence()
            .map(::normalizeVisibleText)
            .filter(String::isNotBlank)
            .filter {
                normalizeComparisonValue(it) !=
                        canonicalComparisonKey
            }
            .distinctBy(::normalizeComparisonValue)
            .sortedWith(
                compareBy<String>(
                    { normalizeComparisonValue(it) },
                    { it }
                )
            )
            .toList()
    }

    fun normalizePhoneticTokens(
        values: List<String>
    ): List<String> =
        values
            .asSequence()
            .flatMap { value ->
                splitTechnicalTokens(value).asSequence()
            }
            .map(::normalizeTechnicalToken)
            .filter(String::isNotBlank)
            .filter {
                it.length >= MINIMUM_PHONETIC_TOKEN_LENGTH
            }
            .distinct()
            .sorted()
            .toList()

    fun normalizeAutocompleteTokens(
        values: List<String>,
        canonicalName: String
    ): List<String> {
        require(canonicalName.isNotBlank()) {
            "canonicalName must not be blank."
        }

        val canonicalTokens = splitSearchTokens(canonicalName)

        val suppliedTokens = values
            .asSequence()
            .flatMap { value ->
                splitSearchTokens(value).asSequence()
            }

        return (canonicalTokens.asSequence() + suppliedTokens)
            .map(::normalizeTechnicalToken)
            .filter(String::isNotBlank)
            .filter {
                it.length >= MINIMUM_AUTOCOMPLETE_TOKEN_LENGTH ||
                        it.all(Char::isDigit)
            }
            .filterNot { it in AUTOCOMPLETE_STOP_TOKENS }
            .distinct()
            .sorted()
            .toList()
    }

    fun normalizeSearchTokens(
        value: String
    ): List<String> =
        splitSearchTokens(value)
            .map(::normalizeTechnicalToken)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()

    private fun normalizeVisibleText(
        value: String
    ): String =
        Normalizer.normalize(
            value,
            Normalizer.Form.NFKC
        )
            .replaceWhitespaceCharacters()
            .replaceApostropheVariants()
            .replaceHyphenVariants()
            .trim()
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")
            .replace(SPACE_AROUND_HYPHEN_REGEX, "-")
            .replace(MULTIPLE_HYPHENS_REGEX, "-")
            .trimAliasSeparators()

    private fun normalizeComparisonValue(
        value: String
    ): String =
        normalizeTechnicalToken(value)

    private fun normalizeTechnicalToken(
        value: String
    ): String =
        Normalizer.normalize(
            transliterateGermanCharacters(value),
            Normalizer.Form.NFKD
        )
            .lowercase(Locale.ROOT)
            .replace(COMBINING_MARKS_REGEX, "")
            .replace(AMPERSAND_REGEX, " und ")
            .replace(NON_ALPHANUMERIC_REGEX, " ")
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")
            .trim()
            .replace(' ', '-')
            .replace(MULTIPLE_HYPHENS_REGEX, "-")
            .trim('-')

    private fun splitTechnicalTokens(
        value: String
    ): List<String> =
        value
            .replace(TOKEN_SEPARATOR_REGEX, " ")
            .split(MULTIPLE_WHITESPACE_REGEX)
            .map(String::trim)
            .filter(String::isNotBlank)

    private fun splitSearchTokens(
        value: String
    ): List<String> {
        val normalized = Normalizer.normalize(
            transliterateGermanCharacters(value),
            Normalizer.Form.NFKD
        )
            .lowercase(Locale.ROOT)
            .replace(COMBINING_MARKS_REGEX, "")
            .replace(AMPERSAND_REGEX, " und ")
            .replace(NON_ALPHANUMERIC_REGEX, " ")
            .replace(MULTIPLE_WHITESPACE_REGEX, " ")
            .trim()

        if (normalized.isBlank()) {
            return emptyList()
        }

        val individualTokens = normalized
            .split(' ')
            .filter(String::isNotBlank)

        val compoundToken = individualTokens
            .takeIf { it.size > 1 }
            ?.joinToString("-")

        return buildList {
            addAll(individualTokens)

            if (!compoundToken.isNullOrBlank()) {
                add(compoundToken)
            }
        }
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
            .replace("ß", "ss")

    private fun String.replaceWhitespaceCharacters(): String =
        buildString(length) {
            this@replaceWhitespaceCharacters.forEach { character ->
                append(
                    if (character.isWhitespace()) {
                        ' '
                    } else {
                        character
                    }
                )
            }
        }

    private fun String.replaceApostropheVariants(): String {
        var result = this

        NON_CANONICAL_APOSTROPHES.forEach { apostrophe ->
            result = result.replace(apostrophe, '\'')
        }

        return result
    }

    private fun String.replaceHyphenVariants(): String {
        var result = this

        NON_CANONICAL_HYPHENS.forEach { hyphen ->
            result = result.replace(hyphen, '-')
        }

        return result
    }

    private fun String.trimAliasSeparators(): String =
        replace(LEADING_ALIAS_SEPARATOR_REGEX, "")
            .replace(TRAILING_ALIAS_SEPARATOR_REGEX, "")
            .trim()

    private companion object {

        const val MINIMUM_PHONETIC_TOKEN_LENGTH = 2
        const val MINIMUM_AUTOCOMPLETE_TOKEN_LENGTH = 2

        val MULTIPLE_WHITESPACE_REGEX =
            Regex("\\s+")

        val COMBINING_MARKS_REGEX =
            Regex("\\p{M}+")

        val NON_ALPHANUMERIC_REGEX =
            Regex("[^a-z0-9]+")

        val AMPERSAND_REGEX =
            Regex("&")

        val MULTIPLE_HYPHENS_REGEX =
            Regex("-{2,}")

        val SPACE_AROUND_HYPHEN_REGEX =
            Regex("\\s*-\\s*")

        val TOKEN_SEPARATOR_REGEX =
            Regex("[,;:/|\\\\]+")

        val LEADING_ALIAS_SEPARATOR_REGEX =
            Regex("^[,;:/|\\-]+\\s*")

        val TRAILING_ALIAS_SEPARATOR_REGEX =
            Regex("\\s*[,;:/|\\-]+$")

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

        val AUTOCOMPLETE_STOP_TOKENS = setOf(
            "art",
            "aus",
            "das",
            "der",
            "die",
            "ein",
            "eine",
            "einer",
            "eines",
            "fuer",
            "im",
            "in",
            "mit",
            "nach",
            "oder",
            "und",
            "von",
            "zum",
            "zur"
        )
    }
}