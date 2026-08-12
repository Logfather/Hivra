package de.shopme.tools.knowledge.mapping.catalog.rebuild

import kotlin.collections.map

enum class CanonicalIdentityKnowledgeEvidenceStrength {
    IDENTITY_SUPPORTING,
    CONTEXTUAL_ONLY,
    INSUFFICIENT
}

enum class CanonicalIdentityKnowledgeDecision {
    KEEP_IDENTITY,
    REVIEW,
    INSUFFICIENT_EVIDENCE
}

data class CanonicalIdentityKnowledgeClassification(
    val parent: String,
    val candidate: String,
    val parentNormalized: String,
    val candidateNormalized: String,
    val decision: CanonicalIdentityKnowledgeDecision,
    val identitySupportingDifferences: List<String>,
    val contextualDifferences: List<String>,
    val comparableIdentitySupportingDimensions: List<String>,
    val comparableContextualDimensions: List<String>
)

class CanonicalIdentityKnowledgeEvidenceClassifier {

    fun classify(
        relationship: CanonicalIdentityKnowledgeEvidenceRelationship
    ): CanonicalIdentityKnowledgeClassification {

        val comparable =
            relationship.dimensions
                .filterValues { dimension ->
                    dimension.comparison !=
                            CanonicalIdentityKnowledgeComparison.MISSING
                }

        val identitySupportingComparable =
            comparable
                .filterKeys(::isIdentitySupportingDimension)

        val contextualComparable =
            comparable
                .filterKeys(::isContextualDimension)

        val identitySupportingDifferences =
            identitySupportingComparable
                .filterValues { dimension ->
                    dimension.comparison ==
                            CanonicalIdentityKnowledgeComparison.DIFFERENT
                }
                .keys
                .sorted()

        val contextualDifferences =
            contextualComparable
                .filterValues { dimension ->
                    dimension.comparison ==
                            CanonicalIdentityKnowledgeComparison.DIFFERENT
                }
                .keys
                .sorted()

        val decision =
            when {

                identitySupportingDifferences.isNotEmpty() ->
                    CanonicalIdentityKnowledgeDecision.KEEP_IDENTITY

                comparable.isEmpty() ->
                    CanonicalIdentityKnowledgeDecision.INSUFFICIENT_EVIDENCE

                else ->
                    CanonicalIdentityKnowledgeDecision.REVIEW
            }

        return CanonicalIdentityKnowledgeClassification(
            parent =
                relationship.parent,
            candidate =
                relationship.candidate,
            parentNormalized =
                relationship.parentNormalized,
            candidateNormalized =
                relationship.candidateNormalized,
            decision =
                decision,
            identitySupportingDifferences =
                identitySupportingDifferences,
            contextualDifferences =
                contextualDifferences,
            comparableIdentitySupportingDimensions =
                identitySupportingComparable
                    .keys
                    .sorted(),
            comparableContextualDimensions =
                contextualComparable
                    .keys
                    .sorted()
        )
    }

    fun classify(
        relationships:
        Collection<CanonicalIdentityKnowledgeEvidenceRelationship>
    ): List<CanonicalIdentityKnowledgeClassification> =
        relationships
            .map(::classify)
            .sortedWith(
                compareBy(
                    CanonicalIdentityKnowledgeClassification::parentNormalized,
                    CanonicalIdentityKnowledgeClassification::candidateNormalized
                )
            )

    private fun isIdentitySupportingDimension(
        artifactName: String
    ): Boolean =
        artifactName in
                IDENTITY_SUPPORTING_DIMENSIONS

    private fun isContextualDimension(
        artifactName: String
    ): Boolean =
        artifactName in
                CONTEXTUAL_DIMENSIONS

    companion object {

        val IDENTITY_SUPPORTING_DIMENSIONS =
            setOf(
                "allergens.json",
                "food_taxonomy.json",
                "ingredients.json",
                "nutrition.json",
                "processing.json",
                "production.json"
            )

        val CONTEXTUAL_DIMENSIONS =
            setOf(
                "animal_welfare.json",
                "biodiversity.json",
                "diet_classification.json",
                "environmental_impact.json",
                "fairtrade.json",
                "food_miles.json",
                "ingredient_graph.json",
                "locality.json",
                "nutri_score.json",
                "packaging.json",
                "pesticides.json",
                "pollinator.json",
                "recipe_graph.json",
                "recipes.json",
                "seasonality.json",
                "water_footprint.json",
                "water_stress.json"
            )
    }
}