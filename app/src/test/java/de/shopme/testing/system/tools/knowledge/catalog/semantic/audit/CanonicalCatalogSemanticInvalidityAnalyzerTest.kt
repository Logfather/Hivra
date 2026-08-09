package de.shopme.testing.system.tools.knowledge.catalog.semantic.audit

import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalCatalogSemanticInvalidityAnalyzerTest {

    @Test
    fun detectsKnownSyntheticCatalogPatterns() {

        val catalog =
            JsonParser
                .parseString(
                    """
                    [
                      {
                        "itemname": "Baguette – Fischbasiert",
                        "category": "bakery",
                        "production": "Standard",
                        "normalized": "baguette-fish-based",
                        "plural": "Baguette – Fischbasiert",
                        "colloquial": ["Fischbasiert"],
                        "phoneticTokens": [],
                        "autocompleteTokens": []
                      },
                      {
                        "itemname": "Brot – Fermentiert – Fischbasiert",
                        "category": "bakery",
                        "production": "Standard",
                        "normalized": "bread-fermented-fish-based",
                        "plural": "Brot – Fermentiert – Fischbasiert",
                        "colloquial": [
                          "Fermentiert",
                          "Fischbasiert"
                        ],
                        "phoneticTokens": [],
                        "autocompleteTokens": []
                      },
                      {
                        "itemname": "Backschokolade – Rind",
                        "category": "baking-ingredients",
                        "production": "Standard",
                        "normalized": "baking-chocolate-beef",
                        "plural": "Backschokolade – Rind",
                        "colloquial": ["Rind"],
                        "phoneticTokens": [],
                        "autocompleteTokens": []
                      }
                    ]
                    """.trimIndent()
                )
                .asJsonArray

        val report =
            CanonicalCatalogSemanticInvalidityAnalyzer()
                .analyze(
                    catalog = catalog,
                    inputFile = "fixture.json"
                )

        assertEquals(
            expected = 3,
            actual = report.semanticInvalidEntryCount
        )

        assertEquals(
            expected = 3,
            actual = report.metadataReviewEntryCount
        )

        assertTrue {
            report.issues.any {
                it.normalized == "baguette-fish-based" &&
                        it.reason ==
                        CanonicalCatalogSemanticInvalidityReason
                            .GENERIC_SEMANTIC_PLACEHOLDER
            }
        }

        assertTrue {
            report.issues.any {
                it.normalized ==
                        "bread-fermented-fish-based" &&
                        it.reason ==
                        CanonicalCatalogSemanticInvalidityReason
                            .GENERIC_SEMANTIC_PLACEHOLDER
            }
        }

        assertTrue {
            report.issues.any {
                it.normalized ==
                        "baking-chocolate-beef" &&
                        it.reason ==
                        CanonicalCatalogSemanticInvalidityReason
                            .CROSS_DOMAIN_VARIANT
            }
        }
    }

    @Test
    fun doesNotFlagOrdinaryCanonicalFoodWithoutVariantSyntax() {

        val catalog =
            JsonParser
                .parseString(
                    """
                    [
                      {
                        "itemname": "Butter",
                        "category": "dairy",
                        "production": "Standard",
                        "normalized": "butter",
                        "plural": "Butter",
                        "colloquial": [],
                        "phoneticTokens": [],
                        "autocompleteTokens": ["butter"]
                      }
                    ]
                    """.trimIndent()
                )
                .asJsonArray

        val report =
            CanonicalCatalogSemanticInvalidityAnalyzer()
                .analyze(
                    catalog = catalog,
                    inputFile = "fixture.json"
                )

        assertEquals(
            expected = 0,
            actual = report.issueCount
        )

        assertEquals(
            expected = 0,
            actual = report.semanticInvalidEntryCount
        )

        assertEquals(
            expected = 0,
            actual = report.metadataReviewEntryCount
        )
    }
}