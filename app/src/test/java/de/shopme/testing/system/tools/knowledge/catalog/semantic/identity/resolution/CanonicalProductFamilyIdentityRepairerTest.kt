package de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.resolution

import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals

class CanonicalProductFamilyIdentityRepairerTest {

    @Test
    fun canonicalizesColaFamilyIdentity() {

        val source =
            listOf(
                JsonParser
                    .parseString(
                        """
                        {
                          "itemname": "Colagetränke – Bitter",
                          "category": "beverages",
                          "production": "Standard",
                          "normalized": "cola-bitter",
                          "plural": "Colagetränke – Bitter",
                          "colloquial": [],
                          "phoneticTokens": [],
                          "autocompleteTokens": []
                        }
                        """.trimIndent()
                    )
                    .asJsonObject
            )

        val result =
            CanonicalProductFamilyIdentityRepairer()
                .repair(source)

        assertEquals(
            expected = 1,
            actual = result.changedEntryCount
        )

        val entry =
            result.entries.single()

        assertEquals(
            expected = "Cola – Bitter",
            actual =
                entry
                    .get("itemname")
                    .asString
        )

        assertEquals(
            expected = "cola-bitter",
            actual =
                entry
                    .get("normalized")
                    .asString
        )
    }

    @Test
    fun doesNotChangeUnrelatedFamilies() {

        val source =
            listOf(
                JsonParser
                    .parseString(
                        """
                        {
                          "itemname": "Brot – Buchweizen",
                          "category": "bakery",
                          "production": "Standard",
                          "normalized": "bread-buckwheat",
                          "plural": "Brote – Buchweizen",
                          "colloquial": [],
                          "phoneticTokens": [],
                          "autocompleteTokens": []
                        }
                        """.trimIndent()
                    )
                    .asJsonObject
            )

        val result =
            CanonicalProductFamilyIdentityRepairer()
                .repair(source)

        assertEquals(
            expected = 0,
            actual = result.changedEntryCount
        )

        assertEquals(
            expected = "Brot – Buchweizen",
            actual =
                result.entries
                    .single()
                    .get("itemname")
                    .asString
        )

        assertEquals(
            expected = "bread-buckwheat",
            actual =
                result.entries
                    .single()
                    .get("normalized")
                    .asString
        )
    }

    @Test
    fun canonicalizesFruitJuiceCompoundIdentity() {

        val source =
            listOf(
                JsonParser
                    .parseString(
                        """
                        {
                          "itemname": "Fruchtsaft – Apfel",
                          "category": "beverages",
                          "production": "Standard",
                          "normalized": "fruit-juice-apple",
                          "plural": "Fruchtsaft – Apfel",
                          "colloquial": [],
                          "phoneticTokens": [],
                          "autocompleteTokens": []
                        }
                        """.trimIndent()
                    )
                    .asJsonObject
            )

        val result =
            CanonicalProductFamilyIdentityRepairer()
                .repair(source)

        assertEquals(
            expected = 1,
            actual = result.changedEntryCount
        )

        assertEquals(
            expected = "Apfelsaft",
            actual =
                result.entries
                    .single()
                    .get("itemname")
                    .asString
        )

        /*
         * Der technische Key bleibt in diesem Commit stabil.
         */
        assertEquals(
            expected = "fruit-juice-apple",
            actual =
                result.entries
                    .single()
                    .get("normalized")
                    .asString
        )
    }

    @Test
    fun canonicalizesOrangeJuiceCompoundIdentity() {

        val source =
            listOf(
                JsonParser
                    .parseString(
                        """
                        {
                          "itemname": "Fruchtsaft – Orange",
                          "category": "beverages",
                          "production": "Standard",
                          "normalized": "fruit-juice-orange",
                          "plural": "Fruchtsaft – Orange",
                          "colloquial": [],
                          "phoneticTokens": [],
                          "autocompleteTokens": []
                        }
                        """.trimIndent()
                    )
                    .asJsonObject
            )

        val result =
            CanonicalProductFamilyIdentityRepairer()
                .repair(source)

        assertEquals(
            expected = 1,
            actual = result.changedEntryCount
        )

        assertEquals(
            expected = "Orangensaft",
            actual =
                result.entries
                    .single()
                    .get("itemname")
                    .asString
        )

        assertEquals(
            expected = "fruit-juice-orange",
            actual =
                result.entries
                    .single()
                    .get("normalized")
                    .asString
        )
    }
}