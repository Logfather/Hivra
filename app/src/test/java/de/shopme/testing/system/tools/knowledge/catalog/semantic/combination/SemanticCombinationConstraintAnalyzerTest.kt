package de.shopme.testing.system.tools.knowledge.catalog.semantic.combination

import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SemanticCombinationConstraintAnalyzerTest {

    @Test
    fun analyzesOnlyMultiVariantCatalogEntries() {

        val catalog =
            JsonParser
                .parseString(
                    """
                    [
                      {
                        "itemname": "Brot – Buchweizen",
                        "category": "bakery",
                        "normalized": "bread-buckwheat"
                      },
                      {
                        "itemname": "Brot – Buchweizen – Fermentiert",
                        "category": "bakery",
                        "normalized": "bread-buckwheat-fermented"
                      },
                      {
                        "itemname": "Brot – Fermentiert – Fischbasiert",
                        "category": "bakery",
                        "normalized": "bread-fermented-fish-based"
                      },
                      {
                        "itemname": "Hartkäse – Lang gereift – Höhlengereift",
                        "category": "dairy",
                        "normalized": "hard-cheese-aged-cave-aged"
                      }
                    ]
                    """.trimIndent()
                )
                .asJsonArray

        val report =
            SemanticCombinationConstraintAnalyzer()
                .analyze(
                    catalog = catalog,
                    inputFile = "fixture.json"
                )

        assertEquals(
            expected = 3,
            actual = report.combinationEntryCount
        )

        assertEquals(
            expected = 1,
            actual = report.allowEntryCount
        )

        assertEquals(
            expected = 2,
            actual = report.rejectEntryCount
        )

        assertEquals(
            expected = 0,
            actual = report.reviewEntryCount
        )

        assertTrue {
            report.entries.none {
                it.normalizedItem ==
                        "bread-buckwheat"
            }
        }
    }
}