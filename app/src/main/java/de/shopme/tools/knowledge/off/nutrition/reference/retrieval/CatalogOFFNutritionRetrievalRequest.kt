package de.shopme.tools.knowledge.off.nutrition.reference.retrieval

data class CatalogOFFNutritionRetrievalRequest(
    val catalogIndex: Int,
    val catalogKey: String,
    val normalizedEnglish: String,
    val itemName: String,
    val category: String?,
    val production: String?,
    val catalogTerms: List<String>,
    val candidates: List<CatalogOFFNutritionRetrievedCandidate>
) {

    init {
        require(catalogIndex >= 0)
        require(catalogKey.isNotBlank())
        require(normalizedEnglish.isNotBlank())
        require(itemName.isNotBlank())

        require(
            catalogTerms ==
                    catalogTerms
                        .distinct()
                        .sorted()
        ) {
            "Catalog terms must be unique and sorted."
        }

        require(
            candidates ==
                    candidates.sortedWith(
                        CatalogOFFNutritionRetrievedCandidate.DETERMINISTIC_COMPARATOR
                    )
        ) {
            "Retrieved candidates must be deterministically sorted."
        }

        require(
            candidates
                .map { candidate ->
                    candidate.serverKey
                }
                .distinct()
                .size ==
                    candidates.size
        ) {
            "Retrieved candidates must have unique server keys."
        }

        require(
            candidates.map { candidate ->
                candidate.rank
            } ==
                    (1..candidates.size).toList()
        ) {
            "Retrieved candidate ranks must be contiguous."
        }
    }

    companion object {

        val CANDIDATE_COMPARATOR =
            compareByDescending<CatalogOFFNutritionRetrievedCandidate> {
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
                    it.serverKey
                }
    }
}