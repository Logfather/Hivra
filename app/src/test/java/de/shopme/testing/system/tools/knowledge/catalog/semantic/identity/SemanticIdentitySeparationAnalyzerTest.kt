package de.shopme.testing.system.tools.knowledge.catalog.semantic.identity

import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SemanticIdentitySeparationAnalyzerTest {

    @Test
    fun projectsKnowledgeClaimsOutOfCatalogIdentity() {

        val catalog =
            JsonParser
                .parseString(
                    """
                    [
                      {
                        "itemname": "Müsli – Proteinreich",
                        "category": "cereals",
                        "normalized": "muesli-high-protein"
                      },
                      {
                        "itemname": "Müsli – Glutenfrei",
                        "category": "cereals",
                        "normalized": "muesli-gluten-free"
                      },
                      {
                        "itemname": "Müsli – Mandel",
                        "category": "cereals",
                        "normalized": "muesli-almond"
                      },
                      {
                        "itemname": "Müsli – Mandel – Proteinreich",
                        "category": "cereals",
                        "normalized": "muesli-almond-high-protein"
                      }
                    ]
                    """.trimIndent()
                )
                .asJsonArray

        val report =
            SemanticIdentitySeparationAnalyzer()
                .analyze(
                    catalog = catalog,
                    inputFile = "fixture.json"
                )

        assertEquals(
            expected = 4,
            actual = report.variantBearingEntryCount
        )

        assertEquals(
            expected = 3,
            actual = report.knowledgeOnlyOccurrenceCount
        )

        assertEquals(
            expected = 3,
            actual = report.entriesWithKnowledgeOnlyVariants
        )

        assertEquals(
            expected = 2,
            actual = report.pureKnowledgeAttributeEntryCount
        )

        assertEquals(
            expected = 1,
            actual = report.mixedIdentityAndKnowledgeEntryCount
        )

        assertTrue {
            report.entries.any {
                it.normalizedItem ==
                        "muesli-high-protein" &&
                        it.projectedBaseIdentity ==
                        "musli"
            }
        }

        assertTrue {
            report.entries.any {
                it.normalizedItem ==
                        "muesli-almond-high-protein" &&
                        it.projectedBaseIdentity ==
                        "musli::almond"
            }
        }

        assertTrue {
            report.projectedBaseIdentityCollisions
                .isNotEmpty()
        }
    }
}