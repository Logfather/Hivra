package de.shopme.tools.knowledge.off.nutrition.reference.retrieval

import de.shopme.tools.knowledge.off.nutrition.reference.validation.OFFNutritionReferenceAggregateValidationStatus

data class CatalogOFFNutritionRetrievedCandidate(
    val rank: Int,
    val serverArtifact: String,
    val serverKey: String,
    val score: Double,
    val exactMatch: Boolean,
    val matchedCatalogTerm: String,
    val matchedCandidateAlias: String,
    val tokenIntersectionCount: Int,
    val tokenUnionCount: Int,
    val tokenJaccard: Double,
    val containmentScore: Double,
    val profileCount: Int,
    val validationStatus: OFFNutritionReferenceAggregateValidationStatus,
    val warningCount: Int
) {

    init {
        require(rank > 0)
        require(serverArtifact == "nutrition.json")
        require(serverKey.isNotBlank())

        require(score.isFinite() && score in 0.0..1.0)
        require(matchedCatalogTerm.isNotBlank())
        require(matchedCandidateAlias.isNotBlank())

        require(tokenIntersectionCount >= 0)
        require(tokenUnionCount > 0)

        require(tokenJaccard.isFinite() && tokenJaccard in 0.0..1.0)
        require(containmentScore.isFinite() && containmentScore in 0.0..1.0)

        require(profileCount > 0)
        require(warningCount >= 0)
    }
}