package de.shopme.testing.system.tools.knowledge.catalog.semantic.plausibility

import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals

class MarketPlausibilityAnalyzerTest {

    @Test
    fun separatesMarketAcceptedRejectedAndReviewItems() {

        val catalog =
            JsonParser
                .parseString(
                    """
                    [
                      {
                        "itemname": "Butter",
                        "category": "dairy",
                        "normalized": "butter"
                      },
                      {
                        "itemname": "Brot – Buchweizen",
                        "category": "bakery",
                        "normalized": "bread-buckwheat"
                      },
                      {
                        "itemname": "Müsli – Proteinreich",
                        "category": "cereals",
                        "normalized": "muesli-high-protein"
                      },
                      {
                        "itemname": "Butter – Höhlengereift",
                        "category": "dairy",
                        "normalized": "butter-cave-aged"
                      }
                    ]
                    """.trimIndent()
                )
                .asJsonArray

        val report =
            MarketPlausibilityAnalyzer()
                .analyze(
                    catalog = catalog,
                    inputFile = "fixture.json"
                )

        assertEquals(
            expected = 4,
            actual = report.catalogEntryCount
        )

        assertEquals(
            expected = 2,
            actual = report.acceptedEntryCount
        )

        assertEquals(
            expected = 2,
            actual = report.rejectedEntryCount
        )

        assertEquals(
            expected = 0,
            actual = report.reviewEntryCount
        )
    }
}