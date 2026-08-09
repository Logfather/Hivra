package de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility

import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductFamilyVariantCompatibilityAnalyzerTest {

    @Test
    fun separatesAllowedRejectedAndReviewOccurrences() {

        val catalog =
            JsonParser
                .parseString(
                    """
                    [
                      {
                        "itemname": "Brot – Gerste",
                        "category": "bakery",
                        "normalized": "bread-barley"
                      },
                      {
                        "itemname": "Brot – Gebacken",
                        "category": "bakery",
                        "normalized": "bread-baked"
                      },
                      {
                        "itemname": "Butter – Höhlengereift",
                        "category": "dairy",
                        "normalized": "butter-cave-aged"
                      },
                      {
                        "itemname": "Butter – Fermentiert",
                        "category": "dairy",
                        "normalized": "butter-fermented"
                      },
                      {
                        "itemname": "Hartkäse – Lang gereift",
                        "category": "dairy",
                        "normalized": "hard-cheese-aged"
                      }
                    ]
                    """.trimIndent()
                )
                .asJsonArray

        val report =
            ProductFamilyVariantCompatibilityAnalyzer()
                .analyze(
                    catalog = catalog,
                    inputFile = "fixture.json"
                )

        assertEquals(
            expected = 5,
            actual = report.variantOccurrenceCount
        )

        assertEquals(
            expected = 2,
            actual = report.allowOccurrenceCount
        )

        assertEquals(
            expected = 3,
            actual = report.rejectOccurrenceCount
        )

        assertEquals(
            expected = 0,
            actual = report.reviewOccurrenceCount
        )

        assertTrue {
            report.occurrences.any {
                it.normalizedItem ==
                        "butter-cave-aged" &&
                        it.decision ==
                        ProductFamilyVariantCompatibilityDecision.REJECT
            }
        }

        assertTrue {
            report.occurrences.any {
                it.normalizedItem ==
                        "butter-fermented" &&
                        it.decision ==
                        ProductFamilyVariantCompatibilityDecision.REJECT &&
                        it.reason ==
                        ProductFamilyVariantCompatibilityReason
                            .SEMANTIC_PROFILE_TYPE_REJECT
            }
        }
    }
}