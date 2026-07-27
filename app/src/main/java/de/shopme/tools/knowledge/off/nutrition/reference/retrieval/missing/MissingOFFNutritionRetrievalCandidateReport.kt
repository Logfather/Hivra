package de.shopme.tools.knowledge.off.nutrition.reference.retrieval.missing

data class MissingOFFNutritionRetrievalCandidateReport(
    val version: Int,
    val requestCount: Int,
    val missingRequestCount: Int,
    val countsByType: Map<MissingOFFNutritionRetrievalCandidateType, Int>,
    val findings: List<MissingOFFNutritionRetrievalCandidateFinding>
) {

    init {
        require(version == CURRENT_VERSION)
        require(requestCount >= 0)
        require(missingRequestCount >= 0)
        require(missingRequestCount == findings.size)

        require(
            countsByType.values.sum() ==
                    missingRequestCount
        )

        require(
            findings ==
                    findings.sortedBy { finding ->
                        finding.catalogIndex
                    }
        )
    }

    companion object {

        const val CURRENT_VERSION =
            1
    }
}