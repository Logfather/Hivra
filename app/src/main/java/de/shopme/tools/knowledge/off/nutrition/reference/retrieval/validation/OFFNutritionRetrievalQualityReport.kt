package de.shopme.tools.knowledge.off.nutrition.reference.retrieval.validation

data class OFFNutritionRetrievalQualityReport(
    val version: Int,
    val requestCount: Int,
    val requestWithCandidatesCount: Int,
    val requestWithoutCandidatesCount: Int,
    val exactAliasMatchCount: Int,
    val strongLexicalMatchCount: Int,
    val riskyFindingCount: Int,
    val countsByPrimaryType: Map<OFFNutritionRetrievalQualityType, Int>,
    val countsByQualityType: Map<OFFNutritionRetrievalQualityType, Int>,
    val findings: List<OFFNutritionRetrievalQualityFinding>
) {

    init {
        require(version == CURRENT_VERSION)
        require(requestCount >= 0)
        require(requestWithCandidatesCount >= 0)
        require(requestWithoutCandidatesCount >= 0)
        require(exactAliasMatchCount >= 0)
        require(strongLexicalMatchCount >= 0)
        require(riskyFindingCount >= 0)

        require(requestCount == findings.size)

        require(
            requestCount ==
                    requestWithCandidatesCount +
                    requestWithoutCandidatesCount
        )

        require(
            findings ==
                    findings.sortedBy { finding ->
                        finding.catalogIndex
                    }
        ) {
            "Retrieval quality findings must be sorted by catalogIndex."
        }

        require(
            findings
                .map { finding ->
                    finding.catalogIndex
                }
                .distinct()
                .size ==
                    findings.size
        ) {
            "Retrieval quality findings must have unique catalog indexes."
        }

        require(
            findings.map { finding ->
                finding.catalogIndex
            } ==
                    findings.indices.toList()
        ) {
            "Retrieval quality finding indexes must be contiguous."
        }

        require(
            countsByPrimaryType.values.sum() ==
                    requestCount
        ) {
            "Primary quality counts must cover every request exactly once."
        }
    }

    companion object {

        const val CURRENT_VERSION: Int = 1
    }
}