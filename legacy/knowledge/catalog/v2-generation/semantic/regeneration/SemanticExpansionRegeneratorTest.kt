package de.shopme.testing.system.tools.knowledge.catalog.semantic.regeneration

import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SemanticExpansionRegeneratorTest {

    @Test
    fun regeneratesOnlyDeterministicallyAcceptedSemanticExpansion() {

        val catalog =
            JsonParser
                .parseString(
                    """
                    [
                      {
                        "itemname": "Brot",
                        "category": "bakery",
                        "production": "Standard",
                        "normalized": "bread",
                        "plural": "Brote",
                        "colloquial": [],
                        "phoneticTokens": [],
                        "autocompleteTokens": ["bread"]
                      },
                      {
                        "itemname": "Müsli",
                        "category": "breakfast",
                        "production": "Standard",
                        "normalized": "muesli",
                        "plural": "Müsli",
                        "colloquial": [],
                        "phoneticTokens": [],
                        "autocompleteTokens": ["muesli"]
                      },
                      {
                        "itemname": "Brot – Buchweizen",
                        "category": "bakery",
                        "production": "Standard",
                        "normalized": "bread-buckwheat",
                        "plural": "Brot – Buchweizen",
                        "colloquial": ["Buchweizen"],
                        "phoneticTokens": [],
                        "autocompleteTokens": []
                      },
                      {
                        "itemname": "Brot – Fischbasiert",
                        "category": "bakery",
                        "production": "Standard",
                        "normalized": "bread-fish-based",
                        "plural": "Brot – Fischbasiert",
                        "colloquial": ["Fischbasiert"],
                        "phoneticTokens": [],
                        "autocompleteTokens": []
                      },
                      {
                        "itemname": "Müsli – Proteinreich",
                        "category": "breakfast",
                        "production": "Standard",
                        "normalized": "muesli-high-protein",
                        "plural": "Müsli – Proteinreich",
                        "colloquial": ["Proteinreich"],
                        "phoneticTokens": [],
                        "autocompleteTokens": []
                      }
                    ]
                    """.trimIndent()
                )
                .asJsonArray

        val result =
            SemanticExpansionRegenerator()
                .regenerate(catalog)

        assertEquals(
            expected = 2,
            actual = result.baselineEntryCount
        )

        assertEquals(
            expected = 3,
            actual = result.expansionInputEntryCount
        )

        assertEquals(
            expected = 1,
            actual = result.acceptedInputEntryCount
        )

        assertEquals(
            expected = 2,
            actual = result.rejectedInputEntryCount
        )

        assertEquals(
            expected = 0,
            actual = result.reviewInputEntryCount
        )

        assertEquals(
            expected = 1,
            actual = result.regeneratedExpansionEntryCount
        )

        assertEquals(
            expected = 3,
            actual = result.projectedCatalogEntryCount
        )

        assertTrue {
            result
                .regeneratedExpansion
                .any {
                    it
                        .get("normalized")
                        .asString ==
                            "bread-buckwheat"
                }
        }

        assertTrue {
            result
                .regeneratedExpansion
                .none {
                    it
                        .get("normalized")
                        .asString ==
                            "bread-fish-based"
                }
        }
    }

    @Test
    fun collapsesKnowledgeDecoratedAcceptedIdentitiesDeterministically() {

        val catalog =
            JsonParser
                .parseString(
                    """
                [
                  {
                    "itemname": "Brot",
                    "category": "bakery",
                    "production": "Standard",
                    "normalized": "bread",
                    "plural": "Brote",
                    "colloquial": [],
                    "phoneticTokens": [],
                    "autocompleteTokens": ["bread"]
                  },
                  {
                    "itemname": "Brot – Buchweizen",
                    "category": "bakery",
                    "production": "Standard",
                    "normalized": "bread-buckwheat",
                    "plural": "Brot – Buchweizen",
                    "colloquial": ["Buchweizen"],
                    "phoneticTokens": [],
                    "autocompleteTokens": []
                  },
                  {
                    "itemname": "Brot – Buchweizen – Glutenfrei",
                    "category": "bakery",
                    "production": "Standard",
                    "normalized": "bread-buckwheat-gluten-free",
                    "plural": "Brot – Buchweizen – Glutenfrei",
                    "colloquial": [
                      "Buchweizen",
                      "Glutenfrei"
                    ],
                    "phoneticTokens": [],
                    "autocompleteTokens": []
                  }
                ]
                """.trimIndent()
                )
                .asJsonArray

        val result =
            SemanticExpansionRegenerator()
                .regenerate(catalog)

        assertEquals(
            expected = 1,
            actual = result.regeneratedExpansionEntryCount
        )

        assertEquals(
            expected = 1,
            actual = result.collapsedDuplicateCount
        )

        assertEquals(
            expected = 1,
            actual = result.knowledgeAttributeProjectionCount
        )

        val item =
            result.regeneratedExpansion.single()

        assertEquals(
            expected = "Brot – Buchweizen",
            actual =
                item
                    .get("itemname")
                    .asString
        )

        assertEquals(
            expected = "bread-buckwheat",
            actual =
                item
                    .get("normalized")
                    .asString
        )
    }

    @Test
    fun regeneratesFamilyWithoutStandaloneBaselineEntry() {

        val catalog =
            JsonParser
                .parseString(
                    """
                [
                  {
                    "itemname": "Apfelsaft",
                    "category": "beverages",
                    "production": "Standard",
                    "normalized": "apfelsaft",
                    "plural": "Apfelsäfte",
                    "colloquial": [],
                    "phoneticTokens": [],
                    "autocompleteTokens": ["apfelsaft"]
                  },
                  {
                    "itemname": "Colagetränke – Bitter",
                    "category": "beverages",
                    "production": "Standard",
                    "normalized": "cola-bitter",
                    "plural": "Colagetränke – Bitter",
                    "colloquial": ["Bitter"],
                    "phoneticTokens": [],
                    "autocompleteTokens": []
                  }
                ]
                """.trimIndent()
                )
                .asJsonArray

        val result =
            SemanticExpansionRegenerator()
                .regenerate(catalog)

        assertEquals(
            expected = 1,
            actual =
                result.regeneratedExpansionEntryCount
        )

        val regenerated =
            result.regeneratedExpansion.single()

        assertEquals(
            expected = "Colagetränke – Bitter",
            actual =
                regenerated
                    .get("itemname")
                    .asString
        )

        assertEquals(
            expected = "cola-bitter",
            actual =
                regenerated
                    .get("normalized")
                    .asString
        )
    }
}