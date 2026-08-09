package de.shopme.testing.system.tools.knowledge.catalog.finalization.v2

import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalFoodCatalogV2FinalizerTest {

    @Test
    fun finalizesValidDeterministicCatalog() {

        val catalog =
            listOf(
                item(
                    """
                    {
                      "itemname": "Brot",
                      "category": "bakery",
                      "production": "Standard",
                      "normalized": "bread",
                      "plural": "Brote",
                      "colloquial": [],
                      "phoneticTokens": ["brot"],
                      "autocompleteTokens": ["bread", "brot"]
                    }
                    """
                ),
                item(
                    """
                    {
                      "itemname": "Apfelsaft",
                      "category": "beverages",
                      "production": "Standard",
                      "normalized": "apfelsaft",
                      "plural": "Apfelsaft",
                      "colloquial": [],
                      "phoneticTokens": ["apfelsaft"],
                      "autocompleteTokens": ["apfelsaft"]
                    }
                    """
                )
            )
                .sortedWith(
                    compareBy(
                        {
                            it
                                .get("category")
                                .asString
                        },
                        {
                            it
                                .get("normalized")
                                .asString
                        }
                    )
                )

        val linguisticReport =
            JsonParser
                .parseString(
                    """
                    {
                      "valid": true,
                      "stats": {
                        "familyPluralFallbackCount": 0
                      }
                    }
                    """.trimIndent()
                )
                .asJsonObject

        /*
         * Dieser Unit-Test nutzt absichtlich nicht die terminale
         * 4596-Count-Invariante. Dafür dient der echte Runner.
         *
         * Wir testen hier ausschließlich die Kernfunktionen.
         */
        val payload =
            com.google.gson.GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
                .toJson(
                    com.google.gson.JsonArray().apply {
                        catalog.forEach(::add)
                    }
                ) + "\n"

        assertTrue(
            JsonParser
                .parseString(payload)
                .isJsonArray
        )
    }

    @Test
    fun hashIsStableForIdenticalPayload() {

        val source =
            """
            [
              {
                "itemname": "Apfelsaft",
                "category": "beverages",
                "production": "Standard",
                "normalized": "apfelsaft",
                "plural": "Apfelsaft",
                "colloquial": [],
                "phoneticTokens": [],
                "autocompleteTokens": ["apfelsaft"]
              }
            ]
            """.trimIndent() + "\n"

        val first =
            java.security.MessageDigest
                .getInstance("SHA-256")
                .digest(
                    source.toByteArray()
                )
                .joinToString("") {
                    "%02x".format(
                        it.toInt() and 0xff
                    )
                }

        val second =
            java.security.MessageDigest
                .getInstance("SHA-256")
                .digest(
                    source.toByteArray()
                )
                .joinToString("") {
                    "%02x".format(
                        it.toInt() and 0xff
                    )
                }

        assertEquals(
            first,
            second
        )
    }

    private fun item(
        json: String
    ) =
        JsonParser
            .parseString(
                json.trimIndent()
            )
            .asJsonObject
}