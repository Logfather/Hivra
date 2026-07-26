package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.retrieval

import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.CatalogOFFNutritionCandidateRetriever
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.CatalogOFFNutritionRetrievalItem
import de.shopme.tools.knowledge.off.nutrition.reference.training.OFFNutritionMatcherTrainingCandidate
import de.shopme.tools.knowledge.off.nutrition.reference.validation.OFFNutritionReferenceAggregateValidationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogOFFNutritionCandidateRetrieverTest {

    @Test
    fun retrieve_placesExactAliasFirst() {

        val result =
            CatalogOFFNutritionCandidateRetriever()
                .retrieve(
                    catalogItems =
                        listOf(
                            CatalogOFFNutritionRetrievalItem(
                                catalogIndex =
                                    0,
                                catalogKey =
                                    "naturjoghurt",
                                normalizedEnglish =
                                    "plain yogurt",
                                itemName =
                                    "Naturjoghurt",
                                category =
                                    "Milchprodukte",
                                production =
                                    null,
                                retrievalTerms =
                                    listOf(
                                        "Naturjoghurt",
                                        "naturjoghurt",
                                        "plain yogurt"
                                    ).sorted()
                            )
                        ),
                    sourceCandidates =
                        listOf(
                            candidate(
                                serverKey =
                                    "greek yogurt",
                                aliases =
                                    listOf("greek yogurt")
                            ),
                            candidate(
                                serverKey =
                                    "plain yogurt",
                                aliases =
                                    listOf(
                                        "natural yogurt",
                                        "plain yogurt"
                                    )
                            )
                        )
                )

        val topCandidate =
            result.requests
                .single()
                .candidates
                .first()

        assertEquals(
            "plain yogurt",
            topCandidate.serverKey
        )

        assertEquals(
            1.0,
            topCandidate.score
        )

        assertTrue(
            topCandidate.exactMatch
        )
    }

    @Test
    fun retrieve_isIndependentOfInputOrder() {

        val apple =
            candidate(
                serverKey =
                    "apple",
                aliases =
                    listOf("apple")
            )

        val appleJuice =
            candidate(
                serverKey =
                    "apple juice",
                aliases =
                    listOf("apple juice")
            )

        val catalogItems =
            listOf(
                CatalogOFFNutritionRetrievalItem(
                    catalogIndex =
                        0,
                    catalogKey =
                        "apfel",
                    normalizedEnglish =
                        "apple",
                    itemName =
                        "Apfel",
                    category =
                        "Obst",
                    production =
                        null,
                    retrievalTerms =
                        listOf(
                            "Apfel",
                            "apfel",
                            "apple"
                        ).sorted()
                )
            )

        val retriever =
            CatalogOFFNutritionCandidateRetriever()

        assertEquals(
            retriever.retrieve(
                catalogItems,
                listOf(
                    apple,
                    appleJuice
                )
            ),
            retriever.retrieve(
                catalogItems,
                listOf(
                    appleJuice,
                    apple
                )
            )
        )
    }

    private fun candidate(
        serverKey: String,
        aliases: List<String>
    ): OFFNutritionMatcherTrainingCandidate =
        OFFNutritionMatcherTrainingCandidate(
            serverArtifact =
                "nutrition.json",
            serverKey =
                serverKey,
            canonicalId =
                serverKey,
            retrievalAliases =
                aliases.distinct().sorted(),
            canonicalAliases =
                aliases.distinct().sorted(),
            matchAliases =
                emptyList(),
            singleIngredientNutritionAliases =
                emptyList(),
            nutrition =
                sortedMapOf(
                    "energyKcalPer100g" to 100.0
                ),
            profileCount =
                1,
            nutrientCount =
                1,
            validationStatus =
                OFFNutritionReferenceAggregateValidationStatus.ACCEPTED,
            warningCount =
                0,
            validationIssueTypes =
                emptyList(),
            representativeSourceId =
                "001",
            sourceIds =
                listOf("001"),
            source =
                "open_food_facts_aggregate",
            sourceVersion =
                "1",
            sourceConfidence =
                1.0
        )
}