package de.shopme.testing.system.tools.knowledge.catalog.normalization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CanonicalFoodKeyNormalizerTest {

    private val normalizer = CanonicalFoodKeyNormalizer()

    @Test
    fun normalizeSimpleFoodName() {
        assertEquals(
            "apfel",
            normalizer.normalize("Apfel")
        )
    }

    @Test
    fun normalizeMultipleWords() {
        assertEquals(
            "paprika-rot",
            normalizer.normalize("Paprika rot")
        )

        assertEquals(
            "gouda-jung",
            normalizer.normalize("Gouda jung")
        )

        assertEquals(
            "haehnchen-brustfilet",
            normalizer.normalize("Hähnchen Brustfilet")
        )
    }

    @Test
    fun normalizeGermanUmlauts() {
        assertEquals(
            "oel",
            normalizer.normalize("Öl")
        )

        assertEquals(
            "aepfel",
            normalizer.normalize("Äpfel")
        )

        assertEquals(
            "gemuese",
            normalizer.normalize("Gemüse")
        )

        assertEquals(
            "suesskartoffel",
            normalizer.normalize("Süßkartoffel")
        )

        assertEquals(
            "haehnchen",
            normalizer.normalize("Hähnchen")
        )
    }

    @Test
    fun normalizeSharpS() {
        assertEquals(
            "weisskohl",
            normalizer.normalize("Weißkohl")
        )

        assertEquals(
            "suess",
            normalizer.normalize("süß")
        )

        assertEquals(
            "grossblattpetersilie",
            normalizer.normalize("Großblattpetersilie")
        )
    }

    @Test
    fun removeDiacritics() {
        assertEquals(
            "creme-fraiche",
            normalizer.normalize("Crème fraîche")
        )

        assertEquals(
            "jalapeno",
            normalizer.normalize("Jalapeño")
        )

        assertEquals(
            "puree",
            normalizer.normalize("Purée")
        )

        assertEquals(
            "cafe",
            normalizer.normalize("Café")
        )
    }

    @Test
    fun normalizeWhitespace() {
        assertEquals(
            "naturjoghurt",
            normalizer.normalize("  Naturjoghurt  ")
        )

        assertEquals(
            "paprika-rot",
            normalizer.normalize("Paprika   rot")
        )

        assertEquals(
            "reis-langkorn",
            normalizer.normalize("Reis\tLangkorn")
        )

        assertEquals(
            "brot-vollkorn",
            normalizer.normalize("Brot\nVollkorn")
        )

        assertEquals(
            "kaese-mild",
            normalizer.normalize("Käse\u00A0mild")
        )
    }

    @Test
    fun normalizeHyphenVariants() {
        assertEquals(
            "haehnchen-brustfilet",
            normalizer.normalize("Hähnchen-Brustfilet")
        )

        assertEquals(
            "haehnchen-brustfilet",
            normalizer.normalize("Hähnchen – Brustfilet")
        )

        assertEquals(
            "haehnchen-brustfilet",
            normalizer.normalize("Hähnchen—Brustfilet")
        )

        assertEquals(
            "haehnchen-brustfilet",
            normalizer.normalize("Hähnchen‐Brustfilet")
        )

        assertEquals(
            "haehnchen-brustfilet",
            normalizer.normalize("Hähnchen---Brustfilet")
        )
    }

    @Test
    fun normalizeUnderscores() {
        assertEquals(
            "paprika-rot",
            normalizer.normalize("Paprika_rot")
        )

        assertEquals(
            "naturjoghurt-3-5",
            normalizer.normalize("Naturjoghurt_3_5")
        )
    }

    @Test
    fun normalizeSlashesAndPathSeparators() {
        assertEquals(
            "reis-langkorn",
            normalizer.normalize("Reis / Langkorn")
        )

        assertEquals(
            "reis-langkorn",
            normalizer.normalize("Reis\\Langkorn")
        )

        assertEquals(
            "reis-langkorn",
            normalizer.normalize("Reis|Langkorn")
        )

        assertEquals(
            "reis-langkorn",
            normalizer.normalize("Reis;Langkorn")
        )

        assertEquals(
            "reis-langkorn",
            normalizer.normalize("Reis:Langkorn")
        )
    }

    @Test
    fun normalizeBrackets() {
        assertEquals(
            "naturjoghurt-3-5",
            normalizer.normalize("Naturjoghurt (3,5)")
        )

        assertEquals(
            "paprika-rot",
            normalizer.normalize("Paprika [rot]")
        )

        assertEquals(
            "reis-langkorn",
            normalizer.normalize("Reis {Langkorn}")
        )

        assertEquals(
            "kaese-mild",
            normalizer.normalize("Käse <mild>")
        )
    }

    @Test
    fun normalizeApostrophes() {
        assertEquals(
            "dangelo-pasta",
            normalizer.normalize("D'Angelo Pasta")
        )

        assertEquals(
            "dangelo-pasta",
            normalizer.normalize("D’Angelo Pasta")
        )

        assertEquals(
            "dangelo-pasta",
            normalizer.normalize("D`Angelo Pasta")
        )

        assertEquals(
            "dangelo-pasta",
            normalizer.normalize("D´Angelo Pasta")
        )
    }

    @Test
    fun normalizeAmpersand() {
        assertEquals(
            "oel-und-essig",
            normalizer.normalize("Öl & Essig")
        )

        assertEquals(
            "salz-und-pfeffer",
            normalizer.normalize("Salz&Pfeffer")
        )
    }

    @Test
    fun normalizePlusSign() {
        assertEquals(
            "apfel-plus-birne",
            normalizer.normalize("Apfel + Birne")
        )

        assertEquals(
            "vitamin-c-plus-zink",
            normalizer.normalize("Vitamin C+Zink")
        )
    }

    @Test
    fun normalizeAtSign() {
        assertEquals(
            "food-at-home",
            normalizer.normalize("Food@Home")
        )
    }

    @Test
    fun normalizePercentSign() {
        assertEquals(
            "naturjoghurt-3-5-prozent",
            normalizer.normalize("Naturjoghurt 3,5 %")
        )

        assertEquals(
            "milch-1-5-prozent-fett",
            normalizer.normalize("Milch 1,5% Fett")
        )

        assertEquals(
            "quark-20-prozent-fett",
            normalizer.normalize("Quark 20 % Fett")
        )
    }

    @Test
    fun normalizeDecimalSeparators() {
        assertEquals(
            "naturjoghurt-3-5",
            normalizer.normalize("Naturjoghurt 3,5")
        )

        assertEquals(
            "naturjoghurt-3-5",
            normalizer.normalize("Naturjoghurt 3.5")
        )

        assertEquals(
            "milch-1-5-prozent",
            normalizer.normalize("Milch 1,5 %")
        )
    }

    @Test
    fun normalizeFractions() {
        assertEquals(
            "milch-1-2-liter",
            normalizer.normalize("Milch ½ Liter")
        )

        assertEquals(
            "sahne-1-4-liter",
            normalizer.normalize("Sahne ¼ Liter")
        )

        assertEquals(
            "saft-3-4-liter",
            normalizer.normalize("Saft ¾ Liter")
        )
    }

    @Test
    fun normalizePunctuation() {
        assertEquals(
            "paprika-rot",
            normalizer.normalize("Paprika, rot")
        )

        assertEquals(
            "kaese-mild",
            normalizer.normalize("Käse: mild")
        )

        assertEquals(
            "apfel-suess",
            normalizer.normalize("Apfel! süß?")
        )

        assertEquals(
            "reis-langkorn",
            normalizer.normalize("Reis... Langkorn")
        )
    }

    @Test
    fun normalizeNumbers() {
        assertEquals(
            "milch-1-5-prozent",
            normalizer.normalize("Milch 1,5 %")
        )

        assertEquals(
            "penne-nr-73",
            normalizer.normalize("Penne Nr. 73")
        )

        assertEquals(
            "kaese-45-prozent-fett-i-tr",
            normalizer.normalize("Käse 45 % Fett i. Tr.")
        )

        assertEquals(
            "ei-groesse-m",
            normalizer.normalize("Ei Größe M")
        )
    }

    @Test
    fun normalizeMixedUnicodeFormsDeterministically() {
        val precomposed = "Crème fraîche"
        val decomposed = "Cre\u0300me frai\u0302che"

        assertEquals(
            normalizer.normalize(precomposed),
            normalizer.normalize(decomposed)
        )

        assertEquals(
            "creme-fraiche",
            normalizer.normalize(decomposed)
        )
    }

    @Test
    fun normalizeIsDeterministic() {
        val input = "  Naturjoghurt (3,5 %) – mild & cremig  "

        val first = normalizer.normalize(input)
        val second = normalizer.normalize(input)
        val third = normalizer.normalize(input)

        assertEquals(first, second)
        assertEquals(first, third)
        assertEquals(
            "naturjoghurt-3-5-prozent-mild-und-cremig",
            first
        )
    }

    @Test
    fun normalizeProducesCanonicalKey() {
        val inputs = listOf(
            "Apfel",
            "Paprika rot",
            "Crème fraîche",
            "Hähnchen-Brustfilet",
            "Naturjoghurt 3,5 %",
            "Öl & Essig",
            "Süßkartoffel",
            "Penne Nr. 73",
            "Reis / Langkorn",
            "Käse (mild)"
        )

        inputs.forEach { input ->
            val normalized = normalizer.normalize(input)

            assertTrue(
                normalizer.isCanonicalKey(normalized),
                "normalize('$input') produced invalid key '$normalized'."
            )

            assertTrue(
                CanonicalFoodKeyNormalizer.CANONICAL_KEY_REGEX.matches(
                    normalized
                ),
                "Generated key '$normalized' does not match canonical regex."
            )
        }
    }

    @Test
    fun normalizeCanonicalKeyIsIdempotent() {
        val keys = listOf(
            "apfel",
            "paprika-rot",
            "creme-fraiche",
            "haehnchen-brustfilet",
            "naturjoghurt-3-5-prozent",
            "oel-und-essig",
            "penne-nr-73"
        )

        keys.forEach { key ->
            assertEquals(
                key,
                normalizer.normalize(key),
                "Canonical key '$key' must remain unchanged."
            )
        }
    }

    @Test
    fun rejectBlankCanonicalName() {
        assertFailsWith<IllegalArgumentException> {
            normalizer.normalize("")
        }

        assertFailsWith<IllegalArgumentException> {
            normalizer.normalize(" ")
        }

        assertFailsWith<IllegalArgumentException> {
            normalizer.normalize("\t\n")
        }
    }

    @Test
    fun rejectNameWithoutUsableTokens() {
        assertFailsWith<IllegalArgumentException> {
            normalizer.normalize("---")
        }

        assertFailsWith<IllegalArgumentException> {
            normalizer.normalize("()[]{}")
        }

        assertFailsWith<IllegalArgumentException> {
            normalizer.normalize("!!!")
        }

        assertFailsWith<IllegalArgumentException> {
            normalizer.normalize("&")
        }
    }

    @Test
    fun normalizeTokensReturnsStableTokens() {
        assertEquals(
            listOf("naturjoghurt", "3", "5", "prozent"),
            normalizer.normalizeTokens("Naturjoghurt 3,5 %")
        )

        assertEquals(
            listOf("oel", "und", "essig"),
            normalizer.normalizeTokens("Öl & Essig")
        )

        assertEquals(
            listOf("creme", "fraiche"),
            normalizer.normalizeTokens("Crème fraîche")
        )

        assertEquals(
            listOf("haehnchen", "brustfilet"),
            normalizer.normalizeTokens("Hähnchen-Brustfilet")
        )
    }

    @Test
    fun normalizeTokensReturnsEmptyListForBlankText() {
        assertTrue(
            normalizer.normalizeTokens("").isEmpty()
        )

        assertTrue(
            normalizer.normalizeTokens("   ").isEmpty()
        )

        assertTrue(
            normalizer.normalizeTokens("\t\n").isEmpty()
        )
    }

    @Test
    fun normalizeTokensRemovesEmptyFragments() {
        assertEquals(
            listOf("apfel", "birne"),
            normalizer.normalizeTokens(
                "  Apfel  ///  Birne  "
            )
        )

        assertEquals(
            listOf("reis", "langkorn"),
            normalizer.normalizeTokens(
                "---Reis---Langkorn---"
            )
        )
    }

    @Test
    fun canonicalKeyValidationAcceptsValidKeys() {
        val validKeys = listOf(
            "apfel",
            "paprika-rot",
            "creme-fraiche",
            "naturjoghurt-3-5-prozent",
            "haehnchen-brustfilet",
            "penne-nr-73",
            "vitamin-b12",
            "100-prozent-apfelsaft"
        )

        validKeys.forEach { key ->
            assertTrue(
                normalizer.isCanonicalKey(key),
                "Expected canonical key: '$key'."
            )

            val result = normalizer.validate(key)

            assertTrue(
                result.valid,
                "Validation unexpectedly rejected '$key': " +
                        result.violations.joinToString()
            )

            assertEquals(
                key,
                result.normalizedKey
            )

            assertTrue(
                result.violations.isEmpty()
            )
        }
    }

    @Test
    fun canonicalKeyValidationRejectsBlankKey() {
        assertFalse(normalizer.isCanonicalKey(""))

        val result = normalizer.validate("")

        assertFalse(result.valid)
        assertEquals("", result.normalizedKey)
        assertEquals(
            listOf("KEY_IS_BLANK"),
            result.violations
        )
    }

    @Test
    fun canonicalKeyValidationRejectsWhitespace() {
        val result = normalizer.validate(" paprika-rot ")

        assertFalse(result.valid)
        assertEquals(
            "paprika-rot",
            result.normalizedKey
        )

        assertTrue(
            "SURROUNDING_WHITESPACE" in result.violations
        )

        assertTrue(
            "KEY_DIFFERS_FROM_CANONICAL_NORMALIZATION" in
                    result.violations
        )
    }

    @Test
    fun canonicalKeyValidationRejectsUppercaseCharacters() {
        val result = normalizer.validate("Paprika-Rot")

        assertFalse(result.valid)
        assertEquals(
            "paprika-rot",
            result.normalizedKey
        )

        assertTrue(
            "UPPERCASE_CHARACTERS" in result.violations
        )

        assertTrue(
            "INVALID_CHARACTER_SEQUENCE" in result.violations
        )

        assertTrue(
            "KEY_DIFFERS_FROM_CANONICAL_NORMALIZATION" in
                    result.violations
        )
    }

    @Test
    fun canonicalKeyValidationRejectsUnderscores() {
        val result = normalizer.validate("paprika_rot")

        assertFalse(result.valid)
        assertEquals(
            "paprika-rot",
            result.normalizedKey
        )

        assertTrue(
            "UNDERSCORE_USED" in result.violations
        )

        assertTrue(
            "INVALID_CHARACTER_SEQUENCE" in result.violations
        )

        assertTrue(
            "KEY_DIFFERS_FROM_CANONICAL_NORMALIZATION" in
                    result.violations
        )
    }

    @Test
    fun canonicalKeyValidationRejectsMultipleHyphens() {
        val result = normalizer.validate("paprika--rot")

        assertFalse(result.valid)
        assertEquals(
            "paprika-rot",
            result.normalizedKey
        )

        assertTrue(
            "MULTIPLE_HYPHENS" in result.violations
        )

        assertTrue(
            "INVALID_CHARACTER_SEQUENCE" in result.violations
        )

        assertTrue(
            "KEY_DIFFERS_FROM_CANONICAL_NORMALIZATION" in
                    result.violations
        )
    }

    @Test
    fun canonicalKeyValidationRejectsLeadingHyphen() {
        val result = normalizer.validate("-paprika-rot")

        assertFalse(result.valid)
        assertEquals(
            "paprika-rot",
            result.normalizedKey
        )

        assertTrue(
            "LEADING_HYPHEN" in result.violations
        )

        assertTrue(
            "INVALID_CHARACTER_SEQUENCE" in result.violations
        )
    }

    @Test
    fun canonicalKeyValidationRejectsTrailingHyphen() {
        val result = normalizer.validate("paprika-rot-")

        assertFalse(result.valid)
        assertEquals(
            "paprika-rot",
            result.normalizedKey
        )

        assertTrue(
            "TRAILING_HYPHEN" in result.violations
        )

        assertTrue(
            "INVALID_CHARACTER_SEQUENCE" in result.violations
        )
    }

    @Test
    fun canonicalKeyValidationRejectsEmbeddedWhitespace() {
        val result = normalizer.validate("paprika rot")

        assertFalse(result.valid)
        assertEquals(
            "paprika-rot",
            result.normalizedKey
        )

        assertTrue(
            "WHITESPACE_PRESENT" in result.violations
        )

        assertTrue(
            "INVALID_CHARACTER_SEQUENCE" in result.violations
        )
    }

    @Test
    fun canonicalKeyValidationRejectsUmlauts() {
        val result = normalizer.validate("hähnchen")

        assertFalse(result.valid)
        assertEquals(
            "haehnchen",
            result.normalizedKey
        )

        assertTrue(
            "INVALID_CHARACTER_SEQUENCE" in result.violations
        )

        assertTrue(
            "KEY_DIFFERS_FROM_CANONICAL_NORMALIZATION" in
                    result.violations
        )
    }

    @Test
    fun canonicalKeyValidationRejectsSpecialCharacters() {
        val result = normalizer.validate("öl&essig")

        assertFalse(result.valid)
        assertEquals(
            "oel-und-essig",
            result.normalizedKey
        )

        assertTrue(
            "INVALID_CHARACTER_SEQUENCE" in result.violations
        )
    }

    @Test
    fun validationViolationsAreUniqueAndSorted() {
        val result = normalizer.validate(
            " Paprika__Rot-- "
        )

        assertFalse(result.valid)

        assertEquals(
            result.violations.distinct(),
            result.violations,
            "Violations must not contain duplicates."
        )

        assertEquals(
            result.violations.sorted(),
            result.violations,
            "Violations must be deterministically sorted."
        )
    }

    @Test
    fun validationNormalizedKeyIsCanonicalWhenRecoverable() {
        val invalidKeys = listOf(
            "Paprika Rot",
            "paprika_rot",
            "-paprika--rot-",
            "Hähnchen",
            "Crème fraîche",
            "Öl & Essig",
            "naturjoghurt 3,5 %"
        )

        invalidKeys.forEach { key ->
            val result = normalizer.validate(key)

            assertFalse(
                result.valid,
                "Input '$key' should not already be canonical."
            )

            assertTrue(
                normalizer.isCanonicalKey(result.normalizedKey),
                "Recovered key '${result.normalizedKey}' from '$key' " +
                        "must be canonical."
            )
        }
    }

    @Test
    fun canonicalRegexAcceptsOnlyCanonicalGrammar() {
        val accepted = listOf(
            "a",
            "apfel",
            "apfel-2",
            "2-apfel",
            "naturjoghurt-3-5",
            "vitamin-b12"
        )

        accepted.forEach { key ->
            assertTrue(
                CanonicalFoodKeyNormalizer.CANONICAL_KEY_REGEX.matches(
                    key
                ),
                "Regex should accept '$key'."
            )
        }

        val rejected = listOf(
            "",
            "-apfel",
            "apfel-",
            "apfel--rot",
            "Apfel",
            "äpfel",
            "apfel_rot",
            "apfel rot",
            "apfel.",
            "apfel/"
        )

        rejected.forEach { key ->
            assertFalse(
                CanonicalFoodKeyNormalizer.CANONICAL_KEY_REGEX.matches(
                    key
                ),
                "Regex should reject '$key'."
            )
        }
    }

    @Test
    fun semanticallyEquivalentNamesProduceSameKey() {
        val groups = listOf(
            listOf(
                "Hähnchen-Brustfilet",
                "Hähnchen – Brustfilet",
                "Hähnchen  Brustfilet",
                "HÄHNCHEN_BRUSTFILET"
            ),
            listOf(
                "Crème fraîche",
                "Creme fraiche",
                "Crème-fraîche"
            ),
            listOf(
                "Öl & Essig",
                "Oel und Essig",
                "Öl und Essig"
            ),
            listOf(
                "Naturjoghurt 3,5 %",
                "Naturjoghurt 3.5%",
                "Naturjoghurt (3,5 %)"
            )
        )

        groups.forEach { equivalentNames ->
            val keys = equivalentNames
                .map(normalizer::normalize)
                .distinct()

            assertEquals(
                1,
                keys.size,
                "Equivalent names generated different keys: " +
                        equivalentNames.zip(
                            equivalentNames.map(normalizer::normalize)
                        )
            )
        }
    }
}