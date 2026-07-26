package de.shopme.tools.knowledge.off.nutrition.reference.training

data class OFFNutritionMatcherTrainingCandidateExportResult(
    val inputAggregateCount: Int,
    val exportedCandidateCount: Int,
    val acceptedCandidateCount: Int,
    val warningCandidateCount: Int,
    val uniqueRetrievalAliasCount: Int,
    val candidates: List<OFFNutritionMatcherTrainingCandidate>
) {

    init {
        require(inputAggregateCount >= 0) {
            "inputAggregateCount must not be negative."
        }

        require(exportedCandidateCount >= 0) {
            "exportedCandidateCount must not be negative."
        }

        require(acceptedCandidateCount >= 0) {
            "acceptedCandidateCount must not be negative."
        }

        require(warningCandidateCount >= 0) {
            "warningCandidateCount must not be negative."
        }

        require(uniqueRetrievalAliasCount >= 0) {
            "uniqueRetrievalAliasCount must not be negative."
        }

        require(exportedCandidateCount == candidates.size) {
            "exportedCandidateCount must equal candidates.size."
        }

        require(inputAggregateCount == exportedCandidateCount) {
            "Every validated aggregate must produce exactly one candidate."
        }

        require(
            exportedCandidateCount ==
                    acceptedCandidateCount +
                    warningCandidateCount
        ) {
            "Candidate status counts do not cover all candidates."
        }

        require(
            candidates ==
                    candidates.sortedBy { candidate ->
                        candidate.serverKey
                    }
        ) {
            "Matcher training candidates must be sorted by serverKey."
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
            "Matcher training candidate serverKeys must be unique."
        }

        val actualUniqueAliasCount =
            candidates
                .asSequence()
                .flatMap { candidate ->
                    candidate.retrievalAliases.asSequence()
                }
                .distinct()
                .count()

        require(
            uniqueRetrievalAliasCount ==
                    actualUniqueAliasCount
        ) {
            "uniqueRetrievalAliasCount does not match candidate aliases."
        }
    }
}