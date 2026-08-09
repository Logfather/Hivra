package de.shopme.testing.system.tools.knowledge.catalog.linguistic.repair

import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals

class CanonicalCatalogFamilyPluralGapAnalyzerTest {

    @Test
    fun aggregatesFallbacksByFamily() {

        val catalog =
            listOf(
                item(
                    """
                    {
                      "itemname": "Brot",
                      "category": "bakery",
                      "normalized": "bread",
                      "plural": "Brote"
                    }
                    """
                ),
                item(
                    """
                    {
                      "itemname": "Brot – Buchweizen",
                      "category": "bakery",
                      "normalized": "bread-buckwheat",
                      "plural": "Brote – Buchweizen"
                    }
                    """
                ),
                item(
                    """
                    {
                      "itemname": "Fruchtsaft – Apfel",
                      "category": "beverages",
                      "normalized": "fruit-juice-apple",
                      "plural": "Fruchtsaft – Apfel"
                    }
                    """
                ),
                item(
                    """
                    {
                      "itemname": "Fruchtsaft – Mandel",
                      "category": "beverages",
                      "normalized": "fruit-juice-almond",
                      "plural": "Fruchtsaft – Mandel"
                    }
                    """
                )
            )

        val report =
            CanonicalCatalogFamilyPluralGapAnalyzer()
                .analyze(catalog)

        assertEquals(
            expected = 3,
            actual = report.variantEntryCount
        )

        assertEquals(
            expected = 1,
            actual = report.familyPluralResolvedCount
        )

        assertEquals(
            expected = 2,
            actual = report.familyPluralFallbackCount
        )

        assertEquals(
            expected = 1,
            actual = report.distinctFallbackFamilyCount
        )

        assertEquals(
            expected = "Fruchtsaft",
            actual = report.gaps.single().family
        )

        assertEquals(
            expected = 2,
            actual = report.gaps.single().occurrenceCount
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