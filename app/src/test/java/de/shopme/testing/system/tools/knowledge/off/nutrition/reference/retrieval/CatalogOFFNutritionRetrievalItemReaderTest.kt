package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.retrieval

import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.CatalogOFFNutritionRetrievalItemReader
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class CatalogOFFNutritionRetrievalItemReaderTest {

    @Test
    fun read_readsActualCatalogSchema() {

        val inputFile =
            Files
                .createTempFile(
                    "catalog",
                    ".json"
                )
                .toFile()

        inputFile.writeText(
            """
            [
              {
                "itemname": "Apfel",
                "category": "Obst",
                "production": "Bio",
                "normalized": "apfel",
                "plural": "aepfel",
                "colloquial": [
                  "apple fruit"
                ],
                "knowledge": {
                  "nutrition": {
                    "reference": "existing apple reference",
                    "source": "open_food_facts"
                  }
                },
                "phonetic_tokens": [
                  "apfel"
                ],
                "autocomplete_tokens": [
                  "ap"
                ],
                "normalizedEnglish": "apple"
              }
            ]
            """.trimIndent()
        )

        val result =
            CatalogOFFNutritionRetrievalItemReader()
                .read(
                    inputFile = inputFile
                )

        val item =
            result.single()

        assertEquals(
            0,
            item.catalogIndex
        )

        assertEquals(
            "apfel",
            item.catalogKey
        )

        assertEquals(
            "apple",
            item.normalizedEnglish
        )

        assertEquals(
            "Apfel",
            item.itemName
        )

        assertEquals(
            "Obst",
            item.category
        )

        assertEquals(
            "Bio",
            item.production
        )

        assertEquals(
            listOf(
                "Apfel",
                "aepfel",
                "apfel",
                "apple",
                "apple fruit"
            ),
            item.retrievalTerms
        )

        assertFalse(
            "ap" in item.retrievalTerms,
            "Autocomplete prefixes must not be retrieval terms."
        )

        assertFalse(
            "existing apple reference" in item.retrievalTerms,
            "Persisted nutrition references must not leak into retrieval."
        )
    }

    @Test
    fun read_preservesCatalogSourceOrderAndAssignsStableIndexes() {

        val inputFile =
            Files
                .createTempFile(
                    "catalog-order",
                    ".json"
                )
                .toFile()

        inputFile.writeText(
            """
            [
              {
                "itemname": "Banane",
                "category": "Obst",
                "production": "Konventionell",
                "normalized": "banane",
                "plural": "bananen",
                "colloquial": [],
                "phonetic_tokens": [
                  "banane"
                ],
                "autocomplete_tokens": [
                  "ba"
                ],
                "normalizedEnglish": "banana"
              },
              {
                "itemname": "Apfel",
                "category": "Obst",
                "production": "Bio",
                "normalized": "apfel",
                "plural": "aepfel",
                "colloquial": [],
                "phonetic_tokens": [
                  "apfel"
                ],
                "autocomplete_tokens": [
                  "ap"
                ],
                "normalizedEnglish": "apple"
              }
            ]
            """.trimIndent()
        )

        val result =
            CatalogOFFNutritionRetrievalItemReader()
                .read(
                    inputFile = inputFile
                )

        assertEquals(
            listOf(
                0,
                1
            ),
            result.map { item ->
                item.catalogIndex
            }
        )

        assertEquals(
            listOf(
                "banane",
                "apfel"
            ),
            result.map { item ->
                item.catalogKey
            }
        )

        assertEquals(
            listOf(
                "banana",
                "apple"
            ),
            result.map { item ->
                item.normalizedEnglish
            }
        )
    }

    @Test
    fun read_allowsDuplicateNormalizedEnglishValues() {

        val inputFile =
            Files
                .createTempFile(
                    "catalog-duplicate-english",
                    ".json"
                )
                .toFile()

        inputFile.writeText(
            """
            [
              {
                "itemname": "Banane Bio",
                "category": "Obst",
                "production": "Bio",
                "normalized": "banane bio",
                "plural": "bananen bio",
                "colloquial": [],
                "phonetic_tokens": [
                  "banane",
                  "bio"
                ],
                "autocomplete_tokens": [
                  "ba"
                ],
                "normalizedEnglish": "banana"
              },
              {
                "itemname": "Banane",
                "category": "Obst",
                "production": "Konventionell",
                "normalized": "banane",
                "plural": "bananen",
                "colloquial": [],
                "phonetic_tokens": [
                  "banane"
                ],
                "autocomplete_tokens": [
                  "ba"
                ],
                "normalizedEnglish": "banana"
              }
            ]
            """.trimIndent()
        )

        val result =
            CatalogOFFNutritionRetrievalItemReader()
                .read(
                    inputFile = inputFile
                )

        assertEquals(
            2,
            result.size
        )

        assertEquals(
            listOf(
                0,
                1
            ),
            result.map { item ->
                item.catalogIndex
            }
        )

        assertEquals(
            listOf(
                "banana",
                "banana"
            ),
            result.map { item ->
                item.normalizedEnglish
            }
        )

        assertEquals(
            listOf(
                "banane bio",
                "banane"
            ),
            result.map { item ->
                item.catalogKey
            }
        )
    }
}