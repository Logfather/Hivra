package de.shopme.tools.knowledge.off.nutrition.reference.retrieval

import de.shopme.tools.knowledge.off.nutrition.reference.training.OFFNutritionMatcherTrainingCandidate
import de.shopme.tools.knowledge.off.nutrition.reference.validation.OFFNutritionReferenceAggregateValidationStatus
import kotlin.math.max
import kotlin.math.min

class CatalogOFFNutritionCandidateRetriever(
    private val maximumCandidatesPerRequest: Int = 10,
    private val minimumScore: Double = 0.18
) {

    init {
        require(maximumCandidatesPerRequest > 0)

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
                .sortedBy {
                    it.serverKey
                }
                .map(::indexCandidate)

        require(
            indexedCandidates.map { it.candidate.serverKey }.distinct().size ==
                    indexedCandidates.size
        ) {
            "OFF matcher candidates contain duplicate server keys."
        }

        val invertedIndex =
            buildInvertedIndex(
                indexedCandidates
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
                requests.count {
                    it.candidates.isNotEmpty()
                },
            requestWithoutCandidatesCount =
                requests.count {
                    it.candidates.isEmpty()
                },
            exactTopCandidateCount =
                requests.count {
                    it.candidates.firstOrNull()?.exactMatch == true
                },
            totalRetrievedCandidateCount =
                requests.sumOf {
                    it.candidates.size
                },
            maximumCandidateCount =
                requests.maxOfOrNull {
                    it.candidates.size
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

        val normalizedCatalogTerms =
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

        val catalogTokens =
            normalizedCatalogTerms
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

        val scoredCandidates =
            candidateIndexes
                .asSequence()
                .map { index ->
                    scoreCandidate(
                        catalogTerms =
                            normalizedCatalogTerms,
                        indexedCandidate =
                            indexedCandidates[index]
                    )
                }
                .filter { candidate ->
                    candidate.score >= minimumScore
                }
                .sortedWith(
                    compareByDescending<ScoredCandidate> {
                        it.score
                    }
                        .thenByDescending {
                            it.exactMatch
                        }
                        .thenByDescending {
                            it.tokenJaccard
                        }
                        .thenByDescending {
                            it.containmentScore
                        }
                        .thenBy {
                            it.candidate.serverKey
                        }
                )
                .take(maximumCandidatesPerRequest)
                .toList()

        val retrievedCandidates =
            scoredCandidates.mapIndexed { index, scored ->
                CatalogOFFNutritionRetrievedCandidate(
                    rank =
                        index + 1,
                    serverArtifact =
                        scored.candidate.serverArtifact,
                    serverKey =
                        scored.candidate.serverKey,
                    score =
                        scored.score,
                    exactMatch =
                        scored.exactMatch,
                    matchedCatalogTerm =
                        scored.matchedCatalogTerm,
                    matchedCandidateAlias =
                        scored.matchedCandidateAlias,
                    tokenIntersectionCount =
                        scored.tokenIntersectionCount,
                    tokenUnionCount =
                        scored.tokenUnionCount,
                    tokenJaccard =
                        scored.tokenJaccard,
                    containmentScore =
                        scored.containmentScore,
                    profileCount =
                        scored.candidate.profileCount,
                    validationStatus =
                        scored.candidate.validationStatus,
                    warningCount =
                        scored.candidate.warningCount
                )
            }

        return CatalogOFFNutritionRetrievalRequest(
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
                normalizedCatalogTerms,
            candidates =
                retrievedCandidates
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
                    when (indexedCandidate.candidate.validationStatus) {
                        OFFNutritionReferenceAggregateValidationStatus.ACCEPTED ->
                            1.0

                        OFFNutritionReferenceAggregateValidationStatus.WARNING ->
                            0.985

                        OFFNutritionReferenceAggregateValidationStatus.REJECTED ->
                            0.0
                    }

                val rawScore =
                    when {
                        exactMatch ->
                            1.0

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

                val scored =
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
                        scored,
                        best
                    ) < 0
                ) {
                    best =
                        scored
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

        indexedCandidates.forEachIndexed { index, candidate ->
            candidate.tokens.forEach { token ->
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
            compareByDescending<ScoredCandidate> {
                it.score
            }
                .thenByDescending {
                    it.exactMatch
                }
                .thenByDescending {
                    it.tokenJaccard
                }
                .thenByDescending {
                    it.containmentScore
                }
                .thenBy {
                    it.candidate.serverKey
                }
    }
}