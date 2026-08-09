package de.shopme.testing.system.tools.knowledge.catalog.normalization

import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.model.IndexedCatalogFoodItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogFoodItemNormalizerTest {

    private val normalizer = CatalogFoodItemNormalizer(
        foodNameNormalizer = CanonicalFoodNameNormalizer(),
        keyNormalizer = CanonicalFoodKeyNormalizer(),
        tokenNormalizer = CatalogTokenNormalizer(),
        pluralNormalizer = GermanFoodPluralNormalizer()
    )

    @Test
    fun normalizeCompleteCatalogFoodItem() {
        val item = indexedItem(
            sourceIndex = 7,
            itemName = "  Hähnchen – Brustfilet  ",
            normalized = "Haehnchen_Brustfilet",
            plural = "Hähnchen-Brustfilets",
            colloquial = listOf(
                " Hähnchenbrust ",
                "Hähnchenbrust",
                "Brustfilet"
            ),
            phoneticTokens = listOf(
                "HAEHNCHEN",
                "Brustfilet"
            ),
            autocompleteTokens = listOf(
                "Hähnchen",
                "Brustfilet"
            )
        )

        val result = normalizer.normalize(item)

        assertEquals(7, result.sourceIndex)

        assertEquals(
            "  Hähnchen – Brustfilet  ",
            result.originalItemName
        )

        assertEquals(
            "Haehnchen_Brustfilet",
            result.originalNormalized
        )

        assertEquals(
            "Hähnchen-Brustfilets",
            result.originalPlural
        )

        assertEquals(
            listOf(
                " Hähnchenbrust ",
                "Hähnchenbrust",
                "Brustfilet"
            ),
            result.originalColloquial
        )

        assertEquals(
            listOf(
                "HAEHNCHEN",
                "Brustfilet"
            ),
            result.originalPhoneticTokens
        )

        assertEquals(
            listOf(
                "Hähnchen",
                "Brustfilet"
            ),
            result.originalAutocompleteTokens
        )

        assertEquals(
            "Hähnchen-Brustfilet",
            result.computedCanonicalName
        )

        assertEquals(
            "haehnchen-brustfilet",
            result.computedNormalizedKey
        )

        assertEquals(
            "Hähnchen-Brustfilets",
            result.computedPlural
        )

        assertEquals(
            listOf(
                "Brustfilet",
                "Hähnchenbrust"
            ),
            result.normalizedColloquial
        )

        assertEquals(
            listOf(
                "brustfilet",
                "haehnchen"
            ),
            result.normalizedPhoneticTokens
        )

        assertEquals(
            listOf(
                "brustfilet",
                "haehnchen",
                "haehnchen-brustfilet"
            ),
            result.normalizedAutocompleteTokens
        )

        assertTrue(
            result.changes.isNotEmpty(),
            "Changed catalog item must produce normalization changes."
        )
    }

    @Test
    fun preserveAllOriginalValuesInResult() {
        val originalColloquial = listOf(
            " Paradeiser ",
            "Tomate"
        )

        val originalPhoneticTokens = listOf(
            "TOMATE",
            "paradaiser"
        )

        val originalAutocompleteTokens = listOf(
            "Tomate",
            "rot"
        )

        val item = indexedItem(
            sourceIndex = 11,
            itemName = "Tomate rot",
            normalized = "tomate_rot",
            plural = "Tomaten rot",
            colloquial = originalColloquial,
            phoneticTokens = originalPhoneticTokens,
            autocompleteTokens = originalAutocompleteTokens
        )

        val result = normalizer.normalize(item)

        assertEquals(
            item.item.itemname,
            result.originalItemName
        )

        assertEquals(
            item.item.normalized,
            result.originalNormalized
        )

        assertEquals(
            item.item.plural,
            result.originalPlural
        )

        assertEquals(
            originalColloquial,
            result.originalColloquial
        )

        assertEquals(
            originalPhoneticTokens,
            result.originalPhoneticTokens
        )

        assertEquals(
            originalAutocompleteTokens,
            result.originalAutocompleteTokens
        )
    }

    @Test
    fun generateCanonicalNameAndNormalizedKey() {
        val cases = listOf(
            "Apfel" to "apfel",
            "Paprika rot" to "paprika-rot",
            "Crème fraîche" to "creme-fraiche",
            "Hähnchen-Brustfilet" to "haehnchen-brustfilet",
            "Naturjoghurt 3,5 %" to
                    "naturjoghurt-3-5-prozent",
            "Öl & Essig" to "oel-und-essig"
        )

        cases.forEachIndexed { index, (itemName, expectedKey) ->
            val result = normalizer.normalize(
                indexedItem(
                    sourceIndex = index,
                    itemName = itemName
                )
            )

            assertEquals(
                expectedKey,
                result.computedNormalizedKey,
                "Unexpected normalized key for '$itemName'."
            )

            assertTrue(
                CanonicalFoodKeyNormalizer
                    .CANONICAL_KEY_REGEX
                    .matches(result.computedNormalizedKey),
                "Generated key '${result.computedNormalizedKey}' " +
                        "must be canonical."
            )
        }
    }

    @Test
    fun normalizeWhitespaceAndPunctuationInItemName() {
        val item = indexedItem(
            sourceIndex = 3,
            itemName = "  Naturjoghurt   3,5 % – mild  "
        )

        val result = normalizer.normalize(item)

        assertEquals(
            "Naturjoghurt 3,5 % -mild",
            result.computedCanonicalName
        )

        assertEquals(
            "naturjoghurt-3-5-prozent-mild",
            result.computedNormalizedKey
        )

        assertTrue(
            result.changes.any {
                it.field == "itemname"
            }
        )
    }

    @Test
    fun retainExistingCanonicalValuesWithoutChanges() {
        val item = indexedItem(
            sourceIndex = 0,
            itemName = "Apfel",
            normalized = "apfel",
            plural = "Äpfel",
            colloquial = emptyList(),
            phoneticTokens = emptyList(),
            autocompleteTokens = listOf("apfel")
        )

        val result = normalizer.normalize(item)

        assertEquals(
            "Apfel",
            result.computedCanonicalName
        )

        assertEquals(
            "apfel",
            result.computedNormalizedKey
        )

        assertEquals(
            "Äpfel",
            result.computedPlural
        )

        assertEquals(
            emptyList(),
            result.normalizedColloquial
        )

        assertEquals(
            emptyList(),
            result.normalizedPhoneticTokens
        )

        assertEquals(
            listOf("apfel"),
            result.normalizedAutocompleteTokens
        )

        assertTrue(
            result.changes.isEmpty(),
            "Already canonical values should not produce changes."
        )
    }

    @Test
    fun generateMissingNormalizedKey() {
        val item = indexedItem(
            sourceIndex = 1,
            itemName = "Paprika rot",
            normalized = null
        )

        val result = normalizer.normalize(item)

        assertEquals(
            "paprika-rot",
            result.computedNormalizedKey
        )

        val keyChange = result.changes.singleOrNull {
            it.type ==
                    CatalogNormalizationChangeType
                        .RECOMPUTED_NORMALIZED_KEY
        }

        assertNotNull(
            keyChange,
            "Missing normalized key must produce a key change."
        )

        assertEquals(
            "normalized",
            keyChange.field
        )

        assertNull(keyChange.before)

        assertEquals(
            "paprika-rot",
            keyChange.after
        )
    }

    @Test
    fun repairNonCanonicalNormalizedKey() {
        val item = indexedItem(
            sourceIndex = 2,
            itemName = "Paprika rot",
            normalized = "Paprika_rot"
        )

        val result = normalizer.normalize(item)

        assertEquals(
            "paprika-rot",
            result.computedNormalizedKey
        )

        assertTrue(
            result.changes.any {
                it.type ==
                        CatalogNormalizationChangeType
                            .RECOMPUTED_NORMALIZED_KEY &&
                        it.before == "Paprika_rot" &&
                        it.after == "paprika-rot"
            }
        )
    }

    @Test
    fun normalizeAndDeduplicateColloquialAliases() {
        val item = indexedItem(
            sourceIndex = 4,
            itemName = "Tomate",
            colloquial = listOf(
                " Paradeiser ",
                "paradeiser",
                "Tomate",
                "Liebesapfel"
            )
        )

        val result = normalizer.normalize(item)

        assertEquals(
            listOf(
                "Liebesapfel",
                "Paradeiser"
            ),
            result.normalizedColloquial
        )

        assertEquals(
            result.normalizedColloquial.size,
            result.normalizedColloquial
                .map { it.lowercase() }
                .distinct()
                .size
        )

        assertFalse(
            result.normalizedColloquial.any {
                it.equals(
                    result.computedCanonicalName,
                    ignoreCase = true
                )
            },
            "Canonical name must not be duplicated as colloquial alias."
        )
    }

    @Test
    fun normalizePhoneticTokens() {
        val item = indexedItem(
            sourceIndex = 5,
            itemName = "Hähnchen",
            phoneticTokens = listOf(
                " HÄHNCHEN ",
                "haehnchen",
                "BRUST / FILET"
            )
        )

        val result = normalizer.normalize(item)

        assertEquals(
            listOf(
                "brust",
                "filet",
                "haehnchen"
            ),
            result.normalizedPhoneticTokens
        )

        assertEquals(
            result.normalizedPhoneticTokens.sorted(),
            result.normalizedPhoneticTokens
        )

        assertEquals(
            result.normalizedPhoneticTokens.distinct(),
            result.normalizedPhoneticTokens
        )
    }

    @Test
    fun normalizeAutocompleteTokensAndIncludeCanonicalName() {
        val item = indexedItem(
            sourceIndex = 6,
            itemName = "Paprika rot",
            autocompleteTokens = listOf(
                " Paprika ",
                "ROT",
                "paprika"
            )
        )

        val result = normalizer.normalize(item)

        assertEquals(
            listOf(
                "paprika",
                "paprika-rot",
                "rot"
            ),
            result.normalizedAutocompleteTokens
        )

        assertTrue(
            "paprika" in
                    result.normalizedAutocompleteTokens
        )

        assertTrue(
            "rot" in
                    result.normalizedAutocompleteTokens
        )

        assertTrue(
            "paprika-rot" in
                    result.normalizedAutocompleteTokens
        )
    }

    @Test
    fun generateKnownGermanPluralWhenMissing() {
        val cases = listOf(
            "Apfel" to "Äpfel",
            "Ei" to "Eier",
            "Nuss" to "Nüsse",
            "Wurst" to "Würste",
            "Pilz" to "Pilze"
        )

        cases.forEachIndexed {
                index,
                (singular, expectedPlural) ->

            val result = normalizer.normalize(
                indexedItem(
                    sourceIndex = index,
                    itemName = singular,
                    plural = null
                )
            )

            assertEquals(
                expectedPlural,
                result.computedPlural,
                "Unexpected generated plural for '$singular'."
            )

            assertTrue(
                result.changes.any {
                    it.type ==
                            CatalogNormalizationChangeType
                                .REPAIRED_PLURAL
                }
            )
        }
    }

    @Test
    fun preserveInvariantFoodPlural() {
        val invariantNames = listOf(
            "Brokkoli",
            "Fisch",
            "Fleisch",
            "Gemüse",
            "Käse",
            "Obst",
            "Reis",
            "Tofu"
        )

        invariantNames.forEachIndexed { index, itemName ->
            val result = normalizer.normalize(
                indexedItem(
                    sourceIndex = index,
                    itemName = itemName,
                    plural = itemName
                )
            )

            assertEquals(
                itemName,
                result.computedPlural,
                "Invariant plural should be preserved for '$itemName'."
            )
        }
    }

    @Test
    fun preserveExistingValidPlural() {
        val item = indexedItem(
            sourceIndex = 9,
            itemName = "Tomate",
            plural = "Tomaten"
        )

        val result = normalizer.normalize(item)

        assertEquals(
            "Tomaten",
            result.computedPlural
        )

        assertFalse(
            result.changes.any {
                it.type ==
                        CatalogNormalizationChangeType
                            .REPAIRED_PLURAL
            },
            "Unchanged valid plural must not produce a repair change."
        )
    }

    @Test
    fun normalizeExistingPluralWhitespace() {
        val item = indexedItem(
            sourceIndex = 10,
            itemName = "Tomate",
            plural = "  Tomaten  "
        )

        val result = normalizer.normalize(item)

        assertEquals(
            "Tomaten",
            result.computedPlural
        )

        assertTrue(
            result.changes.any {
                it.type ==
                        CatalogNormalizationChangeType
                            .REPAIRED_PLURAL &&
                        it.before == "  Tomaten  " &&
                        it.after == "Tomaten"
            }
        )
    }

    @Test
    fun changesAreUniqueAndDeterministicallySorted() {
        val item = indexedItem(
            sourceIndex = 12,
            itemName = "  paprika   ROT  ",
            normalized = "Paprika_ROT",
            plural = "  Paprikas  ",
            colloquial = listOf(
                " Paprika ",
                "paprika",
                "Rote Paprika"
            ),
            phoneticTokens = listOf(
                "PAPRIKA",
                "paprika"
            ),
            autocompleteTokens = listOf(
                "Paprika",
                "rot",
                "Paprika"
            )
        )

        val result = normalizer.normalize(item)

        assertEquals(
            result.changes.distinct(),
            result.changes,
            "Normalization changes must not contain duplicates."
        )

        assertEquals(
            result.changes.sortedWith(changeComparator()),
            result.changes,
            "Normalization changes must be deterministically sorted."
        )

        result.changes.forEach { change ->
            assertTrue(change.field.isNotBlank())
            assertTrue(change.reason.isNotBlank())
            assertFalse(
                change.before == change.after,
                "Change before and after must differ."
            )
        }
    }

    @Test
    fun normalizeIsDeterministic() {
        val item = indexedItem(
            sourceIndex = 13,
            itemName = "  Naturjoghurt 3,5 %  ",
            normalized = "Naturjoghurt_3_5",
            plural = "Naturjoghurts",
            colloquial = listOf(
                " Joghurt natur ",
                "Natur Joghurt"
            ),
            phoneticTokens = listOf(
                "NATURJOGHURT"
            ),
            autocompleteTokens = listOf(
                "Natur",
                "Joghurt"
            )
        )

        val first = normalizer.normalize(item)
        val second = normalizer.normalize(item)
        val third = normalizer.normalize(item)

        assertEquals(first, second)
        assertEquals(first, third)
    }

    @Test
    fun normalizeAllPreservesSourceIndexOrder() {
        val items = listOf(
            indexedItem(
                sourceIndex = 8,
                itemName = "Birne"
            ),
            indexedItem(
                sourceIndex = 2,
                itemName = "Apfel"
            ),
            indexedItem(
                sourceIndex = 5,
                itemName = "Banane"
            )
        )

        val results = normalizer.normalizeAll(items)

        assertEquals(
            listOf(2, 5, 8),
            results.map { it.sourceIndex }
        )

        assertEquals(
            listOf(
                "Apfel",
                "Banane",
                "Birne"
            ),
            results.map { it.originalItemName }
        )
    }

    @Test
    fun normalizeAllReturnsOneResultPerInputItem() {
        val items = listOf(
            indexedItem(
                sourceIndex = 0,
                itemName = "Apfel"
            ),
            indexedItem(
                sourceIndex = 1,
                itemName = "Birne"
            ),
            indexedItem(
                sourceIndex = 2,
                itemName = "Banane"
            )
        )

        val results = normalizer.normalizeAll(items)

        assertEquals(
            items.size,
            results.size
        )

        assertEquals(
            items.map { it.sourceIndex }.sorted(),
            results.map { it.sourceIndex }
        )
    }

    @Test
    fun normalizeAllRejectsDuplicateSourceIndices() {
        val items = listOf(
            indexedItem(
                sourceIndex = 4,
                itemName = "Apfel"
            ),
            indexedItem(
                sourceIndex = 4,
                itemName = "Birne"
            )
        )

        val exception =
            assertFailsWith<IllegalArgumentException> {
                normalizer.normalizeAll(items)
            }

        assertTrue(
            exception.message
                ?.contains("duplicate sourceIndex") == true
        )

        assertTrue(
            exception.message
                ?.contains("4") == true
        )
    }

    @Test
    fun rejectNegativeSourceIndex() {
        val item = indexedItem(
            sourceIndex = -1,
            itemName = "Apfel"
        )

        assertFailsWith<IllegalArgumentException> {
            normalizer.normalize(item)
        }
    }

    @Test
    fun rejectBlankItemName() {
        val item = indexedItem(
            sourceIndex = 0,
            itemName = " "
        )

        assertFailsWith<IllegalArgumentException> {
            normalizer.normalize(item)
        }
    }

    @Test
    fun rejectBlankColloquialAlias() {
        val item = indexedItem(
            sourceIndex = 0,
            itemName = "Apfel",
            colloquial = listOf(
                "Obst",
                " "
            )
        )

        assertFailsWith<IllegalArgumentException> {
            normalizer.normalize(item)
        }
    }

    @Test
    fun rejectBlankPhoneticToken() {
        val item = indexedItem(
            sourceIndex = 0,
            itemName = "Apfel",
            phoneticTokens = listOf(
                "apfel",
                ""
            )
        )

        assertFailsWith<IllegalArgumentException> {
            normalizer.normalize(item)
        }
    }

    @Test
    fun rejectBlankAutocompleteToken() {
        val item = indexedItem(
            sourceIndex = 0,
            itemName = "Apfel",
            autocompleteTokens = listOf(
                "apfel",
                "\t"
            )
        )

        assertFailsWith<IllegalArgumentException> {
            normalizer.normalize(item)
        }
    }

    @Test
    fun resultContainsOnlyCanonicalNormalizedKey() {
        val inputs = listOf(
            "Apfel",
            "Crème fraîche",
            "Hähnchen – Brustfilet",
            "Naturjoghurt 3,5 %",
            "Öl & Essig",
            "Paprika_rot"
        )

        inputs.forEachIndexed { sourceIndex, itemName ->
            val result = normalizer.normalize(
                indexedItem(
                    sourceIndex = sourceIndex,
                    itemName = itemName
                )
            )

            assertTrue(
                CanonicalFoodKeyNormalizer
                    .CANONICAL_KEY_REGEX
                    .matches(result.computedNormalizedKey),
                "Invalid computed key for '$itemName': " +
                        result.computedNormalizedKey
            )

            assertEquals(
                result.computedNormalizedKey
                    .lowercase(),
                result.computedNormalizedKey
            )

            assertFalse(
                result.computedNormalizedKey
                    .contains('_')
            )

            assertFalse(
                result.computedNormalizedKey
                    .contains(' ')
            )
        }
    }

    @Test
    fun normalizedListsContainNoBlankOrDuplicateValues() {
        val item = indexedItem(
            sourceIndex = 14,
            itemName = "Paprika rot",
            colloquial = listOf(
                "Rote Paprika",
                "rote paprika"
            ),
            phoneticTokens = listOf(
                "Paprika",
                "PAPRIKA"
            ),
            autocompleteTokens = listOf(
                "Paprika",
                "paprika",
                "Rot"
            )
        )

        val result = normalizer.normalize(item)

        assertListIsCanonical(
            values = result.normalizedColloquial,
            fieldName = "normalizedColloquial",
            caseInsensitive = true
        )

        assertListIsCanonical(
            values = result.normalizedPhoneticTokens,
            fieldName = "normalizedPhoneticTokens",
            caseInsensitive = false
        )

        assertListIsCanonical(
            values = result.normalizedAutocompleteTokens,
            fieldName = "normalizedAutocompleteTokens",
            caseInsensitive = false
        )
    }

    private fun assertListIsCanonical(
        values: List<String>,
        fieldName: String,
        caseInsensitive: Boolean
    ) {
        assertTrue(
            values.none(String::isBlank),
            "$fieldName must not contain blank values."
        )

        val comparisonValues = if (caseInsensitive) {
            values.map { it.lowercase() }
        } else {
            values
        }

        assertEquals(
            comparisonValues.distinct().size,
            comparisonValues.size,
            "$fieldName must not contain duplicates."
        )

        if (!caseInsensitive) {
            assertEquals(
                values.sorted(),
                values,
                "$fieldName must be sorted."
            )
        }
    }

    private fun indexedItem(
        sourceIndex: Int,
        itemName: String,
        normalized: String? = null,
        plural: String? = null,
        colloquial: List<String> = emptyList(),
        phoneticTokens: List<String> = emptyList(),
        autocompleteTokens: List<String> = emptyList()
    ): IndexedCatalogFoodItem =
        IndexedCatalogFoodItem(
            sourceIndex = sourceIndex,
            item = CatalogFoodItem(
                itemname = itemName,
                category = "fruit",
                production = null,
                normalized = normalized,
                plural = plural,
                colloquial = colloquial,
                phoneticTokens = phoneticTokens,
                autocompleteTokens = autocompleteTokens,
                normalizedEnglish = null
            )
        )

    private fun changeComparator():
            Comparator<CatalogNormalizationChange> =
        compareBy<CatalogNormalizationChange>(
            { it.type.name },
            { it.field },
            { it.before ?: "" },
            { it.after ?: "" },
            { it.reason }
        )
}