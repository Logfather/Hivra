package de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.audit

import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalIdentityDuplicateAnalyzerTest {

    @Test
    fun detectsHardNormalizedKeyCollision() {

        val baseline =
            listOf(
                item(
                    """
                    {
                      "itemname": "Buttercroissant",
                      "category": "bakery",
                      "normalized": "butter-croissant"
                    }
                    """
                )
            )

        val expansion =
            listOf(
                item(
                    """
                    {
                      "itemname": "Croissant mit Butter",
                      "category": "bakery",
                      "normalized": "butter-croissant"
                    }
                    """
                )
            )

        val report =
            CanonicalIdentityDuplicateAnalyzer()
                .analyze(
                    baseline = baseline,
                    regeneratedExpansion = expansion
                )

        assertTrue {
            report.groups.any {
                it.reason ==
                        CanonicalIdentityDuplicateReason
                            .IDENTICAL_NORMALIZED_KEY &&
                        it.severity ==
                        CanonicalIdentityDuplicateSeverity.ERROR
            }
        }

        assertEquals(
            expected = 1,
            actual =
                report.errorGroupCount
        )
    }

    @Test
    fun detectsNormalizedNameCollision() {

        val baseline =
            listOf(
                item(
                    """
                    {
                      "itemname": "Crème fraîche",
                      "category": "dairy",
                      "normalized": "creme-fraiche"
                    }
                    """
                )
            )

        val expansion =
            listOf(
                item(
                    """
                    {
                      "itemname": "Creme Fraiche",
                      "category": "dairy",
                      "normalized": "creme-fraiche-variant"
                    }
                    """
                )
            )

        val report =
            CanonicalIdentityDuplicateAnalyzer()
                .analyze(
                    baseline = baseline,
                    regeneratedExpansion = expansion
                )

        assertTrue {
            report.groups.any {
                it.reason ==
                        CanonicalIdentityDuplicateReason
                            .IDENTICAL_NORMALIZED_NAME
            }
        }
    }

    @Test
    fun detectsWordOrderVariantAsReview() {

        val baseline =
            listOf(
                item(
                    """
                    {
                      "itemname": "Butter Croissant",
                      "category": "bakery",
                      "normalized": "butter-croissant"
                    }
                    """
                )
            )

        val expansion =
            listOf(
                item(
                    """
                    {
                      "itemname": "Croissant Butter",
                      "category": "bakery",
                      "normalized": "croissant-butter"
                    }
                    """
                )
            )

        val report =
            CanonicalIdentityDuplicateAnalyzer()
                .analyze(
                    baseline = baseline,
                    regeneratedExpansion = expansion
                )

        assertTrue {
            report.groups.any {
                it.reason ==
                        CanonicalIdentityDuplicateReason
                            .WORD_ORDER_VARIANT &&
                        it.severity ==
                        CanonicalIdentityDuplicateSeverity.REVIEW
            }
        }
    }

    @Test
    fun distinctFoodsRemainDistinct() {

        val baseline =
            listOf(
                item(
                    """
                    {
                      "itemname": "Butter",
                      "category": "dairy",
                      "normalized": "butter"
                    }
                    """
                ),
                item(
                    """
                    {
                      "itemname": "Buttermilch",
                      "category": "dairy",
                      "normalized": "buttermilk"
                    }
                    """
                )
            )

        val report =
            CanonicalIdentityDuplicateAnalyzer()
                .analyze(
                    baseline = baseline,
                    regeneratedExpansion = emptyList()
                )

        assertEquals(
            expected = 0,
            actual =
                report.duplicateGroupCount
        )
    }

    private fun item(
        rawJson: String
    ) =
        JsonParser
            .parseString(
                rawJson.trimIndent()
            )
            .asJsonObject
}