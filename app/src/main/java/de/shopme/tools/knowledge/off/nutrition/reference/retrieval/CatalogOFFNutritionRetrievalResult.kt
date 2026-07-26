package de.shopme.tools.knowledge.off.nutrition.reference.retrieval

data class CatalogOFFNutritionRetrievalResult(
    val catalogItemCount: Int,
    val sourceCandidateCount: Int,
    val requestCount: Int,
    val requestWithCandidatesCount: Int,
    val requestWithoutCandidatesCount: Int,
    val exactTopCandidateCount: Int,
    val totalRetrievedCandidateCount: Int,
    val maximumCandidateCount: Int,
    val requests: List<CatalogOFFNutritionRetrievalRequest>
) {

    init {
        require(catalogItemCount >= 0)
        require(sourceCandidateCount >= 0)
        require(requestCount >= 0)
        require(requestWithCandidatesCount >= 0)
        require(requestWithoutCandidatesCount >= 0)
        require(exactTopCandidateCount >= 0)
        require(totalRetrievedCandidateCount >= 0)
        require(maximumCandidateCount >= 0)

        require(requestCount == catalogItemCount)
        require(requestCount == requests.size)

        require(
            requestCount ==
                    requestWithCandidatesCount +
                    requestWithoutCandidatesCount
        )

        require(
            requestWithCandidatesCount ==
                    requests.count { request ->
                        request.candidates.isNotEmpty()
                    }
        )

        require(
            requestWithoutCandidatesCount ==
                    requests.count { request ->
                        request.candidates.isEmpty()
                    }
        )

        require(
            exactTopCandidateCount ==
                    requests.count { request ->
                        request.candidates
                            .firstOrNull()
                            ?.exactMatch == true
                    }
        )

        require(
            totalRetrievedCandidateCount ==
                    requests.sumOf { request ->
                        request.candidates.size
                    }
        )

        require(
            requests ==
                    requests.sortedBy { request ->
                        request.catalogIndex
                    }
        ) {
            "Retrieval requests must be sorted by catalogIndex."
        }

        require(
            requests
                .map { request ->
                    request.catalogIndex
                }
                .distinct()
                .size ==
                    requests.size
        ) {
            "Retrieval requests must have unique catalog indexes."
        }

        require(
            requests.map { request ->
                request.catalogIndex
            } ==
                    requests.indices.toList()
        ) {
            "Retrieval request indexes must be contiguous."
        }
    }
}