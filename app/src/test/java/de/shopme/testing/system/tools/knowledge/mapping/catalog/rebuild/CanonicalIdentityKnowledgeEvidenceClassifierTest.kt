package de.shopme.testing.system.tools.knowledge.mapping.catalog.rebuild

import de.shopme.tools.knowledge.mapping.catalog.rebuild.CanonicalIdentityKnowledgeComparison
import de.shopme.tools.knowledge.mapping.catalog.rebuild.CanonicalIdentityKnowledgeDecision
import de.shopme.tools.knowledge.mapping.catalog.rebuild.CanonicalIdentityKnowledgeEvidenceClassifier
import de.shopme.tools.knowledge.mapping.catalog.rebuild.CanonicalIdentityKnowledgeEvidenceDimension
import de.shopme.tools.knowledge.mapping.catalog.rebuild.CanonicalIdentityKnowledgeEvidenceRelationship
import org.junit.Assert.assertEquals
import org.junit.Test

class CanonicalIdentityKnowledgeEvidenceClassifierTest {

    private val classifier =
        CanonicalIdentityKnowledgeEvidenceClassifier()

    @Test
    fun identitySupportingDifferenceKeepsIdentity() {

        val result =
            classifier.classify(
                relationship(
                    dimensions =
                        mapOf(
                            "food_taxonomy.json" to
                                    dimension(
                                        CanonicalIdentityKnowledgeComparison.DIFFERENT
                                    ),
                            "seasonality.json" to
                                    dimension(
                                        CanonicalIdentityKnowledgeComparison.DIFFERENT
                                    )
                        )
                )
            )

        assertEquals(
            CanonicalIdentityKnowledgeDecision.KEEP_IDENTITY,
            result.decision
        )

        assertEquals(
            listOf(
                "food_taxonomy.json"
            ),
            result.identitySupportingDifferences
        )

        assertEquals(
            listOf(
                "seasonality.json"
            ),
            result.contextualDifferences
        )
    }

    @Test
    fun contextualDifferenceAloneRemainsReview() {

        val result =
            classifier.classify(
                relationship(
                    dimensions =
                        mapOf(
                            "food_miles.json" to
                                    dimension(
                                        CanonicalIdentityKnowledgeComparison.DIFFERENT
                                    ),
                            "locality.json" to
                                    dimension(
                                        CanonicalIdentityKnowledgeComparison.DIFFERENT
                                    )
                        )
                )
            )

        assertEquals(
            CanonicalIdentityKnowledgeDecision.REVIEW,
            result.decision
        )
    }

    @Test
    fun equalIdentityDimensionsRemainReview() {

        val result =
            classifier.classify(
                relationship(
                    dimensions =
                        mapOf(
                            "ingredients.json" to
                                    dimension(
                                        CanonicalIdentityKnowledgeComparison.EQUAL
                                    ),
                            "nutrition.json" to
                                    dimension(
                                        CanonicalIdentityKnowledgeComparison.EQUAL
                                    )
                        )
                )
            )

        assertEquals(
            CanonicalIdentityKnowledgeDecision.REVIEW,
            result.decision
        )
    }

    @Test
    fun noComparableDimensionsIsInsufficientEvidence() {

        val result =
            classifier.classify(
                relationship(
                    dimensions =
                        mapOf(
                            "ingredients.json" to
                                    dimension(
                                        CanonicalIdentityKnowledgeComparison.MISSING
                                    ),
                            "nutrition.json" to
                                    dimension(
                                        CanonicalIdentityKnowledgeComparison.MISSING
                                    )
                        )
                )
            )

        assertEquals(
            CanonicalIdentityKnowledgeDecision.INSUFFICIENT_EVIDENCE,
            result.decision
        )
    }

    private fun relationship(
        dimensions:
        Map<String, CanonicalIdentityKnowledgeEvidenceDimension>
    ) =
        CanonicalIdentityKnowledgeEvidenceRelationship(
            parent = "Bohnen",
            candidate = "Kidneybohnen",
            parentNormalized = "bohnen",
            candidateNormalized = "kidneybohnen",
            comparableDimensionCount =
                dimensions.values.count {
                    it.comparison !=
                            CanonicalIdentityKnowledgeComparison.MISSING
                },
            equalDimensionCount =
                dimensions.values.count {
                    it.comparison ==
                            CanonicalIdentityKnowledgeComparison.EQUAL
                },
            differentDimensionCount =
                dimensions.values.count {
                    it.comparison ==
                            CanonicalIdentityKnowledgeComparison.DIFFERENT
                },
            missingDimensionCount =
                dimensions.values.count {
                    it.comparison ==
                            CanonicalIdentityKnowledgeComparison.MISSING
                },
            evidenceState = "TEST",
            dimensions = dimensions
        )

    private fun dimension(
        comparison: CanonicalIdentityKnowledgeComparison
    ) =
        CanonicalIdentityKnowledgeEvidenceDimension(
            comparison = comparison
        )
}