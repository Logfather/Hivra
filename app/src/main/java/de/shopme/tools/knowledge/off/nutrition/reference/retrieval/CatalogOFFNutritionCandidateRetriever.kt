package de.shopme.tools.knowledge.off.nutrition.reference.retrieval

import de.shopme.tools.knowledge.off.nutrition.reference.training.OFFNutritionMatcherTrainingCandidate
import de.shopme.tools.knowledge.off.nutrition.reference.validation.OFFNutritionReferenceAggregateValidationStatus
import kotlin.math.max
import kotlin.math.min

class CatalogOFFNutritionCandidateRetriever(
    private val maximumCandidatesPerRequest: Int = 10,
    private val retrievalPoolSize: Int = 50,
    private val minimumScore: Double = 0.18,
    private val termExpander: OFFNutritionRetrievalTermExpander =
        OFFNutritionRetrievalTermExpander(),
    private val qualityReranker: OFFNutritionCandidateQualityReranker =
        OFFNutritionCandidateQualityReranker()
) {

    init {
        require(maximumCandidatesPerRequest > 0)

        require(retrievalPoolSize > 0)

        require(
            retrievalPoolSize >= maximumCandidatesPerRequest
        ) {
            "Retrieval pool size must be greater than or equal to " +
                    "maximum candidates per request: " +
                    "retrievalPoolSize=$retrievalPoolSize, " +
                    "maximumCandidatesPerRequest=$maximumCandidatesPerRequest."
        }

        require(
            minimumScore.isFinite() &&
                    minimumScore in 0.0..1.0
        )
    }

    fun retrieve(
        catalogItems: List<CatalogOFFNutritionRetrievalItem>,
        sourceCandidates: List<OFFNutritionMatcherTrainingCandidate>
    ): CatalogOFFNutritionRetrievalResult {

        val sortedCatalogItems =
            catalogItems.sortedBy { item ->
                item.catalogIndex
            }

        require(
            sortedCatalogItems
                .map { item ->
                    item.catalogIndex
                }
                .distinct()
                .size ==
                    sortedCatalogItems.size
        ) {
            "Catalog items contain duplicate catalog indexes."
        }

        require(
            sortedCatalogItems
                .map { item ->
                    item.catalogIndex
                } ==
                    sortedCatalogItems.indices.toList()
        ) {
            "Catalog indexes must be contiguous and start at zero."
        }

        val indexedCandidates =
            sourceCandidates
                .sortedBy { candidate ->
                    candidate.serverKey
                }
                .map(::indexCandidate)

        require(
            indexedCandidates
                .map { indexedCandidate ->
                    indexedCandidate.candidate.serverKey
                }
                .distinct()
                .size ==
                    indexedCandidates.size
        ) {
            "OFF matcher candidates contain duplicate server keys."
        }

        val invertedIndex =
            buildInvertedIndex(
                indexedCandidates = indexedCandidates
            )

        val requests =
            sortedCatalogItems.map { catalogItem ->
                retrieveForItem(
                    catalogItem = catalogItem,
                    indexedCandidates = indexedCandidates,
                    invertedIndex = invertedIndex
                )
            }

        return CatalogOFFNutritionRetrievalResult(
            catalogItemCount =
                sortedCatalogItems.size,
            sourceCandidateCount =
                indexedCandidates.size,
            requestCount =
                requests.size,
            requestWithCandidatesCount =
                requests.count { request ->
                    request.candidates.isNotEmpty()
                },
            requestWithoutCandidatesCount =
                requests.count { request ->
                    request.candidates.isEmpty()
                },
            exactTopCandidateCount =
                requests.count { request ->
                    request.candidates
                        .firstOrNull()
                        ?.exactMatch == true
                },
            totalRetrievedCandidateCount =
                requests.sumOf { request ->
                    request.candidates.size
                },
            maximumCandidateCount =
                requests.maxOfOrNull { request ->
                    request.candidates.size
                } ?: 0,
            requests =
                requests
        )
    }

    private fun retrieveForItem(
        catalogItem: CatalogOFFNutritionRetrievalItem,
        indexedCandidates: List<IndexedCandidate>,
        invertedIndex: Map<String, List<Int>>
    ): CatalogOFFNutritionRetrievalRequest {

        val baseCatalogTerms =
            buildSet {
                add(
                    OFFNutritionRetrievalTextNormalizer.normalize(
                        catalogItem.catalogKey
                    )
                )

                add(
                    OFFNutritionRetrievalTextNormalizer.normalize(
                        catalogItem.normalizedEnglish
                    )
                )

                catalogItem.retrievalTerms.forEach { term ->
                    add(
                        OFFNutritionRetrievalTextNormalizer.normalize(
                            term
                        )
                    )
                }
            }
                .filter(String::isNotBlank)
                .distinct()
                .sorted()

        val expandedCatalogTerms =
            termExpander.expand(
                terms = baseCatalogTerms
            )

        val catalogTokens =
            expandedCatalogTerms
                .flatMap(
                    OFFNutritionRetrievalTextNormalizer::tokenize
                )
                .distinct()
                .sorted()

        val candidateIndexes =
            catalogTokens
                .asSequence()
                .flatMap { token ->
                    invertedIndex[token]
                        .orEmpty()
                        .asSequence()
                }
                .distinct()
                .sorted()
                .toList()

        val rawScoredCandidates =
            candidateIndexes
                .asSequence()
                .map { candidateIndex ->
                    scoreCandidate(
                        catalogTerms = expandedCatalogTerms,
                        indexedCandidate =
                            indexedCandidates[candidateIndex]
                    )
                }
                .filter { scoredCandidate ->
                    scoredCandidate.score >= minimumScore
                }
                .sortedWith(
                    SCORED_CANDIDATE_COMPARATOR
                )
                .take(retrievalPoolSize)
                .toList()

        val rawRetrievedCandidates =
            rawScoredCandidates.mapIndexed { index, scoredCandidate ->
                scoredCandidate.toRetrievedCandidate(
                    rank = index + 1
                )
            }

        val provisionalRequest =
            CatalogOFFNutritionRetrievalRequest(
                catalogIndex =
                    catalogItem.catalogIndex,
                catalogKey =
                    catalogItem.catalogKey,
                normalizedEnglish =
                    catalogItem.normalizedEnglish,
                itemName =
                    catalogItem.itemName,
                category =
                    catalogItem.category,
                production =
                    catalogItem.production,
                catalogTerms =
                    expandedCatalogTerms,
                candidates =
                    rawRetrievedCandidates
            )

        val rerankedCandidates =
            qualityReranker.rerank(
                request = provisionalRequest,
                candidates = rawRetrievedCandidates,
                maximumCandidateCount =
                    maximumCandidatesPerRequest
            )

        require(
            rerankedCandidates.size <=
                    maximumCandidatesPerRequest
        ) {
            "Reranker returned too many candidates: " +
                    "catalogIndex=${catalogItem.catalogIndex}, " +
                    "candidateCount=${rerankedCandidates.size}, " +
                    "maximumCandidatesPerRequest=" +
                    "$maximumCandidatesPerRequest."
        }

        require(
            rerankedCandidates
                .map { candidate ->
                    candidate.rank
                } ==
                    (1..rerankedCandidates.size).toList()
        ) {
            "Reranked candidate ranks must be contiguous and start at one: " +
                    "catalogIndex=${catalogItem.catalogIndex}."
        }

        return provisionalRequest.copy(
            candidates =
                rerankedCandidates
        )
    }

    private fun scoreCandidate(
        catalogTerms: List<String>,
        indexedCandidate: IndexedCandidate
    ): ScoredCandidate {

        var best: ScoredCandidate? =
            null

        catalogTerms.forEach catalogTermLoop@ { catalogTerm ->

            val catalogTokens =
                OFFNutritionRetrievalTextNormalizer
                    .tokenize(catalogTerm)
                    .toSet()

            if (catalogTokens.isEmpty()) {
                return@catalogTermLoop
            }

            indexedCandidate.aliases.forEach candidateAliasLoop@ { candidateAlias ->

                val candidateTokens =
                    OFFNutritionRetrievalTextNormalizer
                        .tokenize(candidateAlias)
                        .toSet()

                if (candidateTokens.isEmpty()) {
                    return@candidateAliasLoop
                }

                val intersectionCount =
                    catalogTokens
                        .intersect(candidateTokens)
                        .size

                val unionCount =
                    catalogTokens
                        .union(candidateTokens)
                        .size

                val jaccard =
                    intersectionCount.toDouble() /
                            unionCount.toDouble()

                val containment =
                    intersectionCount.toDouble() /
                            min(
                                catalogTokens.size,
                                candidateTokens.size
                            ).toDouble()

                val exactMatch =
                    catalogTerm == candidateAlias

                val phraseContainment =
                    catalogTerm.contains(candidateAlias) ||
                            candidateAlias.contains(catalogTerm)

                val lengthRatio =
                    min(
                        catalogTerm.length,
                        candidateAlias.length
                    ).toDouble() /
                            max(
                                catalogTerm.length,
                                candidateAlias.length
                            ).toDouble()

                val validationFactor =
                    when (
                        indexedCandidate
                            .candidate
                            .validationStatus
                    ) {
                        OFFNutritionReferenceAggregateValidationStatus.ACCEPTED ->
                            1.0

                        OFFNutritionReferenceAggregateValidationStatus.WARNING ->
                            0.985

                        OFFNutritionReferenceAggregateValidationStatus.REJECTED ->
                            0.0
                    }

                val rawScore =
                    when {
                        exactMatch -> {
                            1.0
                        }

                        phraseContainment &&
                                containment == 1.0 -> {
                            0.82 +
                                    0.10 * lengthRatio +
                                    0.08 * jaccard
                        }

                        else -> {
                            0.58 * jaccard +
                                    0.32 * containment +
                                    0.10 * lengthRatio
                        }
                    }

                val finalScore =
                    (rawScore * validationFactor)
                        .coerceIn(
                            minimumValue = 0.0,
                            maximumValue = 1.0
                        )

                val scoredCandidate =
                    ScoredCandidate(
                        candidate =
                            indexedCandidate.candidate,
                        score =
                            finalScore,
                        exactMatch =
                            exactMatch,
                        matchedCatalogTerm =
                            catalogTerm,
                        matchedCandidateAlias =
                            candidateAlias,
                        tokenIntersectionCount =
                            intersectionCount,
                        tokenUnionCount =
                            unionCount,
                        tokenJaccard =
                            jaccard,
                        containmentScore =
                            containment
                    )

                if (
                    best == null ||
                    SCORED_CANDIDATE_COMPARATOR.compare(
                        scoredCandidate,
                        best
                    ) < 0
                ) {
                    best =
                        scoredCandidate
                }
            }
        }

        return requireNotNull(best) {
            "Candidate was indexed but could not be scored: " +
                    indexedCandidate.candidate.serverKey
        }
    }

    private fun indexCandidate(
        candidate: OFFNutritionMatcherTrainingCandidate
    ): IndexedCandidate {

        val aliases =
            candidate.retrievalAliases
                .asSequence()
                .map(
                    OFFNutritionRetrievalTextNormalizer::normalize
                )
                .filter(String::isNotBlank)
                .distinct()
                .sorted()
                .toList()

        val tokens =
            aliases
                .asSequence()
                .flatMap { alias ->
                    OFFNutritionRetrievalTextNormalizer
                        .tokenize(alias)
                        .asSequence()
                }
                .distinct()
                .sorted()
                .toList()

        return IndexedCandidate(
            candidate =
                candidate,
            aliases =
                aliases,
            tokens =
                tokens
        )
    }

    private fun buildInvertedIndex(
        indexedCandidates: List<IndexedCandidate>
    ): Map<String, List<Int>> {

        val mutableIndex =
            sortedMapOf<String, MutableList<Int>>()

        indexedCandidates.forEachIndexed { index, indexedCandidate ->
            indexedCandidate.tokens.forEach { token ->
                mutableIndex
                    .getOrPut(token) {
                        mutableListOf()
                    }
                    .add(index)
            }
        }

        return mutableIndex.mapValues { (_, indexes) ->
            indexes
                .distinct()
                .sorted()
        }
    }

    private fun ScoredCandidate.toRetrievedCandidate(
        rank: Int
    ): CatalogOFFNutritionRetrievedCandidate {

        return CatalogOFFNutritionRetrievedCandidate(
            rank =
                rank,
            serverArtifact =
                candidate.serverArtifact,
            serverKey =
                candidate.serverKey,
            score =
                score,
            exactMatch =
                exactMatch,
            matchedCatalogTerm =
                matchedCatalogTerm,
            matchedCandidateAlias =
                matchedCandidateAlias,
            tokenIntersectionCount =
                tokenIntersectionCount,
            tokenUnionCount =
                tokenUnionCount,
            tokenJaccard =
                tokenJaccard,
            containmentScore =
                containmentScore,
            profileCount =
                candidate.profileCount,
            validationStatus =
                candidate.validationStatus,
            warningCount =
                candidate.warningCount
        )
    }

    private data class IndexedCandidate(
        val candidate: OFFNutritionMatcherTrainingCandidate,
        val aliases: List<String>,
        val tokens: List<String>
    )

    private data class ScoredCandidate(
        val candidate: OFFNutritionMatcherTrainingCandidate,
        val score: Double,
        val exactMatch: Boolean,
        val matchedCatalogTerm: String,
        val matchedCandidateAlias: String,
        val tokenIntersectionCount: Int,
        val tokenUnionCount: Int,
        val tokenJaccard: Double,
        val containmentScore: Double
    )

    private companion object {

        val SCORED_CANDIDATE_COMPARATOR =
            compareByDescending<ScoredCandidate> { candidate ->
                candidate.score
            }
                .thenByDescending { candidate ->
                    candidate.exactMatch
                }
                .thenByDescending { candidate ->
                    candidate.tokenJaccard
                }
                .thenByDescending { candidate ->
                    candidate.containmentScore
                }
                .thenBy { candidate ->
                    candidate.candidate.serverKey
                }
    }
}