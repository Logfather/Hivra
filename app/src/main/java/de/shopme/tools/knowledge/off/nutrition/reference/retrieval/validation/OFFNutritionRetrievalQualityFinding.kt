package de.shopme.tools.knowledge.off.nutrition.reference.retrieval.validation

data class OFFNutritionRetrievalQualityFinding(
    val catalogIndex: Int,
    val catalogKey: String,
    val normalizedEnglish: String,
    val itemName: String,
    val category: String?,
    val production: String?,
    val candidateCount: Int,
    val topCandidateServerKey: String?,
    val topCandidateScore: Double?,
    val topCandidateExactMatch: Boolean?,
    val matchedCatalogTerm: String?,
    val matchedCandidateAlias: String?,
    val tokenIntersectionCount: Int?,
    val tokenUnionCount: Int?,
    val tokenJaccard: Double?,
    val containmentScore: Double?,
    val primaryType: OFFNutritionRetrievalQualityType,
    val qualityTypes: List<OFFNutritionRetrievalQualityType>,
    val reasons: List<String>
) {

    init {
        require(catalogIndex >= 0)
        require(catalogKey.isNotBlank())
        require(normalizedEnglish.isNotBlank())
        require(itemName.isNotBlank())
        require(candidateCount >= 0)

        require(
            qualityTypes.isNotEmpty()
        ) {
            "At least one retrieval quality type is required."
        }

        require(
            primaryType in qualityTypes
        ) {
            "Primary retrieval quality type must occur in qualityTypes."
        }

        require(
            qualityTypes ==
                    qualityTypes
                        .distinct()
                        .sortedBy { type ->
                            type.name
                        }
        ) {
            "Retrieval quality types must be unique and sorted."
        }

        require(
            reasons.isNotEmpty()
        ) {
            "At least one retrieval quality reason is required."
        }

        require(
            reasons ==
                    reasons
                        .distinct()
                        .sorted()
        ) {
            "Retrieval quality reasons must be unique and sorted."
        }

        if (candidateCount == 0) {
            require(topCandidateServerKey == null)
            require(topCandidateScore == null)
            require(topCandidateExactMatch == null)
        } else {
            require(!topCandidateServerKey.isNullOrBlank())
            require(topCandidateScore != null)
            require(topCandidateExactMatch != null)
        }
    }
}