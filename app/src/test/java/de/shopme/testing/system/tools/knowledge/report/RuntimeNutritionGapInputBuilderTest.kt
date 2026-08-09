package de.shopme.testing.system.tools.knowledge.report

import de.shopme.tools.knowledge.report.RuntimeKnowledgeGapDecisionOutcome
import de.shopme.tools.knowledge.report.RuntimeKnowledgeGapInputBuilder
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RuntimeNutritionGapInputBuilderTest {

    @Test
    fun buildNutritionGapInputsFromPersistedMatchArtifacts() {

        val directory =
            createTempDirectory(
                prefix =
                    "runtime-nutrition-gap-inputs-"
            )
                .toFile()

        try {
            val catalogFile =
                File(
                    directory,
                    "canonical-food-catalog.json"
                )
                    .apply {
                        writeText(
                            """
                            [
                              {
                                "normalizedEnglish": "Apple"
                              },
                              {
                                "normalizedEnglish": "Banana"
                              },
                              {
                                "normalizedEnglish": "Chocolate Drink"
                              },
                              {
                                "normalizedEnglish": "Cream Cheese"
                              },
                              {
                                "normalizedEnglish": "Oat Drink"
                              },
                              {
                                "normalizedEnglish": "Unknown Product"
                              }
                            ]
                            """.trimIndent()
                        )
                    }

            val serverArtifactFile =
                File(
                    directory,
                    "nutrition.json"
                )
                    .apply {
                        writeText(
                            """
                            {
                              "entries": {
                                "apple": {
                                  "energyKcalPer100g": 52.0
                                },
                                "banana raw": {
                                  "energyKcalPer100g": 89.0
                                },
                                "milk chocolate beverage": {
                                  "energyKcalPer100g": 80.0
                                },
                                "cream cheese spread": {
                                  "energyKcalPer100g": 342.0
                                }
                              }
                            }
                            """.trimIndent()
                        )
                    }

            val candidateRetrievalFile =
                File(
                    directory,
                    "nutrition-candidates.json"
                )
                    .apply {
                        writeText(
                            """
                            {
                              "results": [
                                {
                                  "catalogKey": "banana",
                                  "candidates": [
                                    {
                                      "serverKey": "banana raw"
                                    }
                                  ]
                                },
                                {
                                  "catalogKey": "chocolate drink",
                                  "candidates": [
                                    {
                                      "serverKey": "milk chocolate beverage"
                                    },
                                    {
                                      "serverKey": "chocolate milk"
                                    }
                                  ]
                                },
                                {
                                  "catalogKey": "cream cheese",
                                  "candidates": [
                                    {
                                      "serverKey": "cream cheese spread"
                                    }
                                  ]
                                },
                                {
                                  "catalogKey": "oat drink",
                                  "candidates": [
                                    {
                                      "serverKey": "oat beverage"
                                    }
                                  ]
                                },
                                {
                                  "catalogKey": "unknown product",
                                  "candidates": []
                                }
                              ]
                            }
                            """.trimIndent()
                        )
                    }

            val decisionFile =
                File(
                    directory,
                    "nutrition-decisions.json"
                )
                    .apply {
                        writeText(
                            """
                            {
                              "decisions": [
                                {
                                  "catalogKey": "banana",
                                  "outcome": "MATCH",
                                  "selectedServerKey": "banana raw",
                                  "confidence": 0.98
                                },
                                {
                                  "catalogKey": "chocolate drink",
                                  "outcome": "MATCH",
                                  "selectedServerKey": "milk chocolate beverage",
                                  "confidence": 0.62,
                                  "minimumConfidence": 0.80
                                },
                                {
                                  "catalogKey": "cream cheese",
                                  "outcome": "NO_MATCH"
                                },
                                {
                                  "catalogKey": "oat drink",
                                  "outcome": "MATCH",
                                  "selectedServerKey": "oat beverage",
                                  "confidence": 0.94
                                }
                              ]
                            }
                            """.trimIndent()
                        )
                    }

            val validationFile =
                File(
                    directory,
                    "nutrition-validations.json"
                )
                    .apply {
                        writeText(
                            """
                            {
                              "validations": [
                                {
                                  "catalogKey": "banana",
                                  "accepted": true
                                },
                                {
                                  "catalogKey": "chocolate drink",
                                  "accepted": false,
                                  "reason": "Confidence below threshold."
                                },
                                {
                                  "catalogKey": "cream cheese",
                                  "accepted": false,
                                  "reason": "AI returned NO_MATCH."
                                },
                                {
                                  "catalogKey": "oat drink",
                                  "accepted": true
                                }
                              ]
                            }
                            """.trimIndent()
                        )
                    }

            val mappingFile =
                File(
                    directory,
                    "catalog-server.mappings.json"
                )
                    .apply {
                        writeText(
                            """
                            {
                              "mappings": [
                                {
                                  "catalogKey": "banana",
                                  "serverKey": "banana raw"
                                }
                              ]
                            }
                            """.trimIndent()
                        )
                    }

            val inputs =
                RuntimeKnowledgeGapInputBuilder()
                    .build(
                        catalogFile = catalogFile,
                        serverArtifactFile =
                            serverArtifactFile,
                        candidateRetrievalFile =
                            candidateRetrievalFile,
                        decisionFile =
                            decisionFile,
                        validationFile =
                            validationFile,
                        catalogServerMappingFile =
                            mappingFile
                    )

            /*
             * Apple ist exakt abgedeckt.
             * Banana ist über das validierte Mapping abgedeckt.
             *
             * Nur die übrigen vier Keys sind echte Runtime-Lücken.
             */
            assertEquals(
                expected = 4,
                actual = inputs.size
            )

            assertEquals(
                expected =
                    listOf(
                        "chocolate drink",
                        "cream cheese",
                        "oat drink",
                        "unknown product"
                    ),
                actual =
                    inputs.map {
                        it.catalogKey
                    }
            )

            val chocolate =
                inputs.single {
                    it.catalogKey ==
                            "chocolate drink"
                }

            assertEquals(
                expected =
                    listOf(
                        "chocolate milk",
                        "milk chocolate beverage"
                    ),
                actual = chocolate.candidates
            )

            assertEquals(
                expected =
                    RuntimeKnowledgeGapDecisionOutcome.MATCH,
                actual =
                    chocolate.decision?.outcome
            )

            assertEquals(
                expected = 0.62,
                actual =
                    chocolate.decision?.confidence
            )

            assertEquals(
                expected = 0.80,
                actual =
                    chocolate.decision
                        ?.minimumConfidence
            )

            assertFalse(
                chocolate.validation
                    ?.accepted
                    ?: true
            )

            assertTrue(
                chocolate.serverEntryExists
                    ?: false
            )

            val creamCheese =
                inputs.single {
                    it.catalogKey ==
                            "cream cheese"
                }

            assertEquals(
                expected =
                    RuntimeKnowledgeGapDecisionOutcome.NO_MATCH,
                actual =
                    creamCheese.decision
                        ?.outcome
            )

            assertNull(
                creamCheese.serverEntryExists
            )

            val oatDrink =
                inputs.single {
                    it.catalogKey ==
                            "oat drink"
                }

            assertEquals(
                expected =
                    "oat beverage",
                actual =
                    oatDrink.decision
                        ?.selectedServerKey
            )

            assertTrue(
                oatDrink.validation
                    ?.accepted
                    ?: false
            )

            assertFalse(
                oatDrink.serverEntryExists
                    ?: true
            )

            val unknown =
                inputs.single {
                    it.catalogKey ==
                            "unknown product"
                }

            assertTrue(
                unknown.candidates.isEmpty()
            )

            assertNull(
                unknown.decision
            )

            assertNull(
                unknown.validation
            )

            assertNull(
                unknown.serverEntryExists
            )

        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun buildNutritionGapInputsDeterministically() {

        val directory =
            createTempDirectory(
                prefix =
                    "runtime-nutrition-gap-order-"
            )
                .toFile()

        try {
            val catalogFile =
                File(
                    directory,
                    "catalog.json"
                )
                    .apply {
                        writeText(
                            """
                            [
                              {
                                "normalizedEnglish": "Zucchini"
                              },
                              {
                                "normalizedEnglish": "Apple"
                              },
                              {
                                "normalizedEnglish": "Banana"
                              }
                            ]
                            """.trimIndent()
                        )
                    }

            val serverArtifactFile =
                File(
                    directory,
                    "nutrition.json"
                )
                    .apply {
                        writeText(
                            """
                            {
                              "entries": {}
                            }
                            """.trimIndent()
                        )
                    }

            val candidateFile =
                File(
                    directory,
                    "candidates.json"
                )
                    .apply {
                        writeText(
                            """
                            {
                              "results": [
                                {
                                  "catalogKey": "zucchini",
                                  "candidates": [
                                    "zucchini raw"
                                  ]
                                },
                                {
                                  "catalogKey": "apple",
                                  "candidates": [
                                    "apple raw"
                                  ]
                                },
                                {
                                  "catalogKey": "banana",
                                  "candidates": [
                                    "banana raw"
                                  ]
                                }
                              ]
                            }
                            """.trimIndent()
                        )
                    }

            val decisionFile =
                File(
                    directory,
                    "decisions.json"
                )
                    .apply {
                        writeText(
                            """
                            {
                              "decisions": []
                            }
                            """.trimIndent()
                        )
                    }

            val first =
                RuntimeKnowledgeGapInputBuilder()
                    .build(
                        catalogFile = catalogFile,
                        serverArtifactFile =
                            serverArtifactFile,
                        candidateRetrievalFile =
                            candidateFile,
                        decisionFile =
                            decisionFile,
                        validationFile = null,
                        catalogServerMappingFile = null
                    )

            val second =
                RuntimeKnowledgeGapInputBuilder()
                    .build(
                        catalogFile = catalogFile,
                        serverArtifactFile =
                            serverArtifactFile,
                        candidateRetrievalFile =
                            candidateFile,
                        decisionFile =
                            decisionFile,
                        validationFile = null,
                        catalogServerMappingFile = null
                    )

            assertEquals(
                expected = first,
                actual = second
            )

            assertEquals(
                expected =
                    listOf(
                        "apple",
                        "banana",
                        "zucchini"
                    ),
                actual =
                    first.map {
                        it.catalogKey
                    }
            )

        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun collapseDuplicateCanonicalCatalogKeys() {

        val directory =
            createTempDirectory(
                prefix =
                    "runtime-nutrition-duplicate-catalog-"
            )
                .toFile()

        try {
            val catalogFile =
                File(
                    directory,
                    "canonical-food-catalog.json"
                )
                    .apply {
                        writeText(
                            """
                        [
                          {
                            "normalizedEnglish": "Banana"
                          },
                          {
                            "normalizedEnglish": " banana "
                          },
                          {
                            "normalizedEnglish": "Apple"
                          }
                        ]
                        """.trimIndent()
                        )
                    }

            val serverArtifactFile =
                File(
                    directory,
                    "nutrition.json"
                )
                    .apply {
                        writeText(
                            """
                        {
                          "entries": {}
                        }
                        """.trimIndent()
                        )
                    }

            val candidateFile =
                File(
                    directory,
                    "nutrition.match-requests.json"
                )
                    .apply {
                        writeText(
                            """
                        {
                          "requests": [
                            {
                              "catalogKey": "apple",
                              "candidates": []
                            },
                            {
                              "catalogKey": "banana",
                              "candidates": []
                            }
                          ]
                        }
                        """.trimIndent()
                        )
                    }

            val decisionFile =
                File(
                    directory,
                    "nutrition.match-decisions.json"
                )
                    .apply {
                        writeText(
                            """
                        {
                          "decisions": []
                        }
                        """.trimIndent()
                        )
                    }

            val inputs =
                RuntimeKnowledgeGapInputBuilder()
                    .build(
                        catalogFile = catalogFile,
                        serverArtifactFile =
                            serverArtifactFile,
                        candidateRetrievalFile =
                            candidateFile,
                        decisionFile =
                            decisionFile,
                        validationFile = null,
                        catalogServerMappingFile = null
                    )

            assertEquals(
                expected =
                    listOf(
                        "apple",
                        "banana"
                    ),
                actual =
                    inputs.map {
                        it.catalogKey
                    }
            )

        } finally {
            directory.deleteRecursively()
        }
    }
}