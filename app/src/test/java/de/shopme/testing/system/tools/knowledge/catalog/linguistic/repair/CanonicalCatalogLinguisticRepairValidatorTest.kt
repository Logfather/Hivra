package de.shopme.testing.system.tools.knowledge.catalog.linguistic.repair

import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertTrue

class CanonicalCatalogLinguisticRepairValidatorTest {

    @Test
    fun validatesCleanRepairedCatalog() {

        val source =
            listOf(
                JsonParser
                    .parseString(
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
                        """.trimIndent()
                    )
                    .asJsonObject,

                JsonParser
                    .parseString(
                        """
                        {
                          "itemname": "Brot – Buchweizen",
                          "category": "bakery",
                          "production": "Standard",
                          "normalized": "bread-buckwheat",
                          "plural": "Brot – Buchweizen",
                          "colloquial": [
                            "Buchweizen"
                          ],
                          "phoneticTokens": [],
                          "autocompleteTokens": []
                        }
                        """.trimIndent()
                    )
                    .asJsonObject
            )

        val repair =
            CanonicalCatalogLinguisticMetadataRepairer()
                .repair(source)

        val report =
            CanonicalCatalogLinguisticRepairValidator()
                .validate(
                    catalog =
                        repair.first,
                    stats =
                        repair.second,
                    deterministic =
                        true
                )

        assertTrue(
            report.valid
        )
    }
}