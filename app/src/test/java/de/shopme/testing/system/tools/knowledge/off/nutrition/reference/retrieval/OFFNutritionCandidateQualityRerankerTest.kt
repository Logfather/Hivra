package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.retrieval

import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.CatalogOFFNutritionRetrievalRequest
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.CatalogOFFNutritionRetrievedCandidate
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.OFFNutritionCandidateQualityReranker
import de.shopme.tools.knowledge.off.nutrition.reference.validation.OFFNutritionReferenceAggregateValidationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OFFNutritionCandidateQualityRerankerTest {

    private val reranker =
        OFFNutritionCandidateQualityReranker()

    @Test
    fun rerank_prefersRawPearOverPearJuice() {

        val request =
            request(
                normalizedEnglish = "pear",
                terms =
                    listOf(
                        "pear"
                    )
            )

        val result =
            reranker.rerank(
                request = request,
                candidates =
                    listOf(
                        candidate(
                            serverKey = "pear juice",
                            score = 0.90,
                            alias = "pear juice"
                        ),
                        candidate(
                            serverKey = "fresh pears",
                            score = 0.86,
                            alias = "pear"
                        )
                    ),
                maximumCandidateCount = 10
            )

        assertEquals(
            "fresh pears",
            result.first().serverKey
        )
    }

    @Test
    fun rerank_penalizesAnimalSpeciesMismatch() {

        val request =
            request(
                normalizedEnglish = "duck breast",
                terms =
                    listOf(
                        "duck breast"
                    )
            )

        val result =
            reranker.rerank(
                request = request,
                candidates =
                    listOf(
                        candidate(
                            serverKey = "turkey breast",
                            score = 0.80,
                            alias = "turkey breast"
                        ),
                        candidate(
                            serverKey = "duck breast fillet",
                            score = 0.65,
                            alias = "duck breast"
                        )
                    ),
                maximumCandidateCount = 10
            )

        assertEquals(
            "duck breast fillet",
            result.first().serverKey
        )
    }

    @Test
    fun rerank_keepsExactMatchAtScoreOne() {

        val request =
            request(
                normalizedEnglish = "apple",
                terms =
                    listOf(
                        "apple"
                    )
            )

        val result =
            reranker.rerank(
                request = request,
                candidates =
                    listOf(
                        candidate(
                            serverKey = "apple",
                            score = 0.95,
                            alias = "apple",
                            exactMatch = true
                        )
                    ),
                maximumCandidateCount = 10
            )

        assertEquals(
            1.0,
            result.single().score
        )

        assertTrue(
            result.single().exactMatch
        )
    }

    private fun request(
        normalizedEnglish: String,
        terms: List<String>
    ): CatalogOFFNutritionRetrievalRequest {

        return CatalogOFFNutritionRetrievalRequest(
            catalogIndex =
                0,
            catalogKey =
                normalizedEnglish,
            normalizedEnglish =
                normalizedEnglish,
            itemName =
                normalizedEnglish,
            category =
                null,
            production =
                null,
            catalogTerms =
                terms.sorted(),
            candidates =
                emptyList()
        )
    }

    private fun candidate(
        serverKey: String,
        score: Double,
        alias: String,
        exactMatch: Boolean = false
    ): CatalogOFFNutritionRetrievedCandidate {

        val candidateTokens =
            alias.split(' ')

        return CatalogOFFNutritionRetrievedCandidate(
            rank =
                1,
            serverArtifact =
                "nutrition.json",
            serverKey =
                serverKey,
            score =
                score,
            exactMatch =
                exactMatch,
            matchedCatalogTerm =
                alias.substringBefore(' '),
            matchedCandidateAlias =
                alias,
            tokenIntersectionCount =
                1,
            tokenUnionCount =
                candidateTokens.size,
            tokenJaccard =
                1.0 / candidateTokens.size,
            containmentScore =
                1.0,
            profileCount =
                1,
            validationStatus =
                OFFNutritionReferenceAggregateValidationStatus.ACCEPTED,
            warningCount =
                0
        )
    }
}