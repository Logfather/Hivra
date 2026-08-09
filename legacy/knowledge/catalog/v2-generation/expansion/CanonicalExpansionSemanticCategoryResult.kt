package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic

data class CanonicalExpansionSemanticCategoryResult(
    val category: String,

    val evaluatedCandidateCount: Int,

    val acceptedCandidateCount: Int,
    val rejectedCandidateCount: Int,
    val reviewRequiredCandidateCount: Int,

    val decisionCounts:
    Map<CanonicalExpansionSemanticDecision, Int>,

    val entries:
    List<CanonicalExpansionCandidateSemanticValidationEntry>,

    val complete: Boolean
) {

    init {
        require(category.isNotBlank())

        require(evaluatedCandidateCount == entries.size)

        require(
            acceptedCandidateCount ==
                    entries.count {
                        it.accepted
                    }
        )

        require(
            rejectedCandidateCount ==
                    entries.count {
                        it.rejected
                    }
        )

        require(
            reviewRequiredCandidateCount ==
                    entries.count {
                        it.reviewRequired
                    }
        )

        require(
            evaluatedCandidateCount ==
                    acceptedCandidateCount +
                    rejectedCandidateCount +
                    reviewRequiredCandidateCount
        )

        require(
            decisionCounts.values.sum() ==
                    evaluatedCandidateCount
        )

        require(
            decisionCounts ==
                    decisionCounts
                        .toList()
                        .sortedBy {
                            it.first.name
                        }
                        .associate {
                            it
                        }
        )

        require(
            entries ==
                    entries.sortedBy {
                        it.candidateIndex
                    }
        )

        require(
            entries.all {
                it.category == category
            }
        )

        require(
            complete ==
                    (
                            evaluatedCandidateCount ==
                                    entries.size
                            )
        )
    }
}