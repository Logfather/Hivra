package de.shopme.testing.system.tools.knowledge.catalog.linguistic.repair

import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CanonicalCatalogLinguisticMetadataRepairerTest {

    @Test
    fun removesVariantValueFromColloquial() {

        val catalog =
            listOf(
                item(
                    """
                    {
                      "itemname": "Butter",
                      "category": "dairy",
                      "production": "Standard",
                      "normalized": "butter",
                      "plural": "Butter",
                      "colloquial": [],
                      "phoneticTokens": [],
                      "autocompleteTokens": []
                    }
                    """
                ),
                item(
                    """
                    {
                      "itemname": "Butter – Fermentiert",
                      "category": "dairy",
                      "production": "Standard",
                      "normalized": "butter-fermented",
                      "plural": "Butter – Fermentiert",
                      "colloquial": [
                        "Fermentiert"
                      ],
                      "phoneticTokens": [],
                      "autocompleteTokens": []
                    }
                    """
                )
            )

        val repaired =
            CanonicalCatalogLinguisticMetadataRepairer()
                .repair(catalog)
                .first

        val butterFermented =
            repaired.first {
                it
                    .get("normalized")
                    .asString ==
                        "butter-fermented"
            }

        assertEquals(
            expected = 0,
            actual =
                butterFermented
                    .getAsJsonArray(
                        "colloquial"
                    )
                    .size()
        )
    }

    @Test
    fun usesFamilyPluralForVariantIdentity() {

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
                      "phoneticTokens": [],
                      "autocompleteTokens": []
                    }
                    """
                ),
                item(
                    """
                    {
                      "itemname": "Brot – Buchweizen",
                      "category": "bakery",
                      "production": "Standard",
                      "normalized": "bread-buckwheat",
                      "plural": "Brot – Buchweizen",
                      "colloquial": [],
                      "phoneticTokens": [],
                      "autocompleteTokens": []
                    }
                    """
                )
            )

        val repaired =
            CanonicalCatalogLinguisticMetadataRepairer()
                .repair(catalog)
                .first

        val entry =
            repaired.first {
                it
                    .get("normalized")
                    .asString ==
                        "bread-buckwheat"
            }

        assertEquals(
            expected =
                "Brote – Buchweizen",
            actual =
                entry
                    .get("plural")
                    .asString
        )
    }

    @Test
    fun preservesValidColloquialAliases() {

        val catalog =
            listOf(
                item(
                    """
                    {
                      "itemname": "Kartoffel",
                      "category": "vegetables",
                      "production": "Standard",
                      "normalized": "kartoffel",
                      "plural": "Kartoffeln",
                      "colloquial": [
                        "Erdapfel"
                      ],
                      "phoneticTokens": [],
                      "autocompleteTokens": []
                    }
                    """
                )
            )

        val repaired =
            CanonicalCatalogLinguisticMetadataRepairer()
                .repair(catalog)
                .first
                .single()

        assertEquals(
            expected =
                listOf(
                    "Erdapfel"
                ),
            actual =
                repaired
                    .getAsJsonArray(
                        "colloquial"
                    )
                    .map {
                        it.asString
                    }
        )
    }

    @Test
    fun generatesDeterministicAutocompleteTokens() {

        val catalog =
            listOf(
                item(
                    """
                    {
                      "itemname": "Balsamicoessig",
                      "category": "sauces",
                      "production": "Standard",
                      "normalized": "balsamicoessig",
                      "plural": "Balsamicoessige",
                      "colloquial": [],
                      "phoneticTokens": [],
                      "autocompleteTokens": []
                    }
                    """
                )
            )

        val first =
            CanonicalCatalogLinguisticMetadataRepairer()
                .repair(catalog)
                .first
                .single()

        val second =
            CanonicalCatalogLinguisticMetadataRepairer()
                .repair(catalog)
                .first
                .single()

        assertEquals(
            expected =
                first
                    .getAsJsonArray(
                        "autocompleteTokens"
                    ),
            actual =
                second
                    .getAsJsonArray(
                        "autocompleteTokens"
                    )
        )

        assertTrue(
            first
                .getAsJsonArray(
                    "autocompleteTokens"
                )
                .map {
                    it.asString
                }
                .contains(
                    "balsamicoessig"
                )
        )
    }

    @Test
    fun repairDoesNotChangeCanonicalIdentityFields() {

        val source =
            item(
                """
                {
                  "itemname": "Chiasamen",
                  "category": "spices",
                  "production": "Standard",
                  "normalized": "chiasamen",
                  "plural": "Chiasamen",
                  "colloquial": [],
                  "phoneticTokens": [],
                  "autocompleteTokens": []
                }
                """
            )

        val repaired =
            CanonicalCatalogLinguisticMetadataRepairer()
                .repair(
                    listOf(source)
                )
                .first
                .single()

        assertEquals(
            "Chiasamen",
            repaired
                .get("itemname")
                .asString
        )

        assertEquals(
            "spices",
            repaired
                .get("category")
                .asString
        )

        assertEquals(
            "Standard",
            repaired
                .get("production")
                .asString
        )

        assertEquals(
            "chiasamen",
            repaired
                .get("normalized")
                .asString
        )

        assertFalse(
            repaired
                .getAsJsonArray(
                    "autocompleteTokens"
                )
                .isEmpty
        )
    }

    @Test
    fun usesCanonicalColaFamilyIdentityAndPlural() {

        val catalog =
            listOf(
                item(
                    """
                {
                  "itemname": "Cola – Bitter",
                  "category": "beverages",
                  "production": "Standard",
                  "normalized": "cola-bitter",
                  "plural": "Colagetränke – Bitter",
                  "colloquial": [],
                  "phoneticTokens": [],
                  "autocompleteTokens": []
                }
                """
                )
            )

        val repaired =
            CanonicalCatalogLinguisticMetadataRepairer()
                .repair(catalog)
                .first
                .single()

        assertEquals(
            expected = "Cola – Bitter",
            actual =
                repaired
                    .get("plural")
                    .asString
        )

        assertTrue(
            repaired
                .getAsJsonArray(
                    "autocompleteTokens"
                )
                .map {
                    it.asString
                }
                .contains(
                    "cola"
                )
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