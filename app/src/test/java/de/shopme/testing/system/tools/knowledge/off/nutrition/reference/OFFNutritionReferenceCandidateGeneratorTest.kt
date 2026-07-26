package de.shopme.testing.system.tools.knowledge.off.nutrition.reference

import de.shopme.tools.knowledge.ki_candidates.CandidateMetadata
import de.shopme.tools.knowledge.ki_candidates.CanonicalKnowledgeCandidate
import de.shopme.tools.knowledge.ki_candidates.KnowledgeDimensionCandidate
import de.shopme.tools.knowledge.ki_candidates.KnowledgeDimensionCandidateType
import de.shopme.tools.knowledge.off.nutrition.reference.OFFNutritionReferenceCandidateGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OFFNutritionReferenceCandidateGeneratorTest {

    @Test
    fun generate_extractsTypedNutritionReferenceCandidates() {

        val candidate =
            CanonicalKnowledgeCandidate(
                canonicalId =
                    "plain yogurt",
                aliases =
                    setOf(
                        "naturjoghurt",
                        "plain yogurt"
                    ),
                matchAliases =
                    setOf(
                        "yogurt"
                    ),
                dimensions =
                    listOf(
                        KnowledgeDimensionCandidate(
                            dimension =
                                KnowledgeDimensionCandidateType.NUTRITION,
                            payload =
                                mapOf(
                                    "energyKcalPer100g" to 61.0,
                                    "fatPer100g" to 3.3,
                                    "saturatedFatPer100g" to 2.1,
                                    "carbohydratesPer100g" to 4.7,
                                    "sugarsPer100g" to 4.7,
                                    "proteinsPer100g" to 3.5,
                                    "saltPer100g" to 0.1
                                )
                        ),
                        KnowledgeDimensionCandidate(
                            dimension =
                                KnowledgeDimensionCandidateType.INGREDIENTS,
                            payload =
                                mapOf(
                                    "ingredientsText" to "milk"
                                )
                        )
                    ),
                metadata =
                    CandidateMetadata(
                        source =
                            "open_food_facts",
                        sourceId =
                            "1234567890123",
                        confidence =
                            1.0,
                        version =
                            "1",
                        attributes =
                            mapOf(
                                "productName" to "plain yogurt",
                                "brand" to "example brand",
                                "categories" to "dairies, yogurts",
                                "singleIngredientNutritionAliases" to
                                        "natural yogurt|yogurt"
                            )
                    )
            )

        val result =
            OFFNutritionReferenceCandidateGenerator()
                .generate(
                    candidates =
                        listOf(candidate)
                )

        assertEquals(
            1,
            result.inputCandidateCount
        )

        assertEquals(
            1,
            result.generatedCandidateCount
        )

        assertEquals(
            0,
            result.skippedWithoutNutritionCount
        )

        assertEquals(
            0,
            result.skippedInvalidIdentityCount
        )

        assertEquals(
            0,
            result.skippedInvalidNutritionPayloadCount
        )

        val generated =
            result.candidates.single()

        assertEquals(
            "1234567890123",
            generated.sourceId
        )

        assertEquals(
            "plain yogurt",
            generated.canonicalId
        )

        assertEquals(
            setOf(
                "naturjoghurt",
                "plain yogurt"
            ),
            generated.aliases
        )

        assertEquals(
            setOf("yogurt"),
            generated.matchAliases
        )

        assertEquals(
            61.0,
            generated.nutrition["energyKcalPer100g"]
        )

        assertEquals(
            "plain yogurt",
            generated.productName
        )

        assertEquals(
            "example brand",
            generated.brand
        )

        assertEquals(
            "dairies, yogurts",
            generated.categories
        )

        assertEquals(
            setOf(
                "natural yogurt",
                "yogurt"
            ),
            generated.singleIngredientNutritionAliases
        )

        assertEquals(
            "open_food_facts",
            generated.source
        )

        assertEquals(
            "1",
            generated.sourceVersion
        )

        assertEquals(
            1.0,
            generated.sourceConfidence
        )
    }

    @Test
    fun generate_skipsCandidatesWithoutNutrition() {

        val candidate =
            createCandidate(
                sourceId =
                    "without-nutrition",
                dimensions =
                    listOf(
                        KnowledgeDimensionCandidate(
                            dimension =
                                KnowledgeDimensionCandidateType.INGREDIENTS,
                            payload =
                                mapOf(
                                    "ingredientsText" to "water"
                                )
                        )
                    )
            )

        val result =
            OFFNutritionReferenceCandidateGenerator()
                .generate(
                    candidates =
                        listOf(candidate)
                )

        assertEquals(
            1,
            result.inputCandidateCount
        )

        assertEquals(
            0,
            result.generatedCandidateCount
        )

        assertEquals(
            1,
            result.skippedWithoutNutritionCount
        )

        assertEquals(
            0,
            result.skippedInvalidNutritionPayloadCount
        )

        assertTrue(
            result.candidates.isEmpty()
        )
    }

    @Test
    fun generate_skipsInvalidNutritionPayload() {

        val candidate =
            createCandidate(
                sourceId =
                    "invalid-payload",
                dimensions =
                    listOf(
                        KnowledgeDimensionCandidate(
                            dimension =
                                KnowledgeDimensionCandidateType.NUTRITION,
                            payload =
                                mapOf(
                                    "energyKcalPer100g" to "invalid"
                                )
                        )
                    )
            )

        val result =
            OFFNutritionReferenceCandidateGenerator()
                .generate(
                    candidates =
                        listOf(candidate)
                )

        assertEquals(
            1,
            result.inputCandidateCount
        )

        assertEquals(
            0,
            result.generatedCandidateCount
        )

        assertEquals(
            0,
            result.skippedWithoutNutritionCount
        )

        assertEquals(
            0,
            result.skippedInvalidIdentityCount
        )

        assertEquals(
            1,
            result.skippedInvalidNutritionPayloadCount
        )
    }

    @Test
    fun generate_sortsCandidatesDeterministicallyBySourceId() {

        val second =
            createNutritionCandidate(
                sourceId =
                    "200"
            )

        val first =
            createNutritionCandidate(
                sourceId =
                    "100"
            )

        val result =
            OFFNutritionReferenceCandidateGenerator()
                .generate(
                    candidates =
                        listOf(
                            second,
                            first
                        )
                )

        assertEquals(
            listOf(
                "100",
                "200"
            ),
            result.candidates.map { candidate ->
                candidate.sourceId
            }
        )
    }

    @Test
    fun generate_skipsCandidateWithMultipleNutritionDimensions() {

        val nutritionDimension =
            KnowledgeDimensionCandidate(
                dimension =
                    KnowledgeDimensionCandidateType.NUTRITION,
                payload =
                    mapOf(
                        "energyKcalPer100g" to 100.0
                    )
            )

        val candidate =
            createCandidate(
                sourceId =
                    "duplicate-nutrition",
                dimensions =
                    listOf(
                        nutritionDimension,
                        nutritionDimension
                    )
            )

        val result =
            OFFNutritionReferenceCandidateGenerator()
                .generate(
                    candidates =
                        listOf(candidate)
                )

        assertEquals(
            0,
            result.generatedCandidateCount
        )

        assertEquals(
            0,
            result.skippedWithoutNutritionCount
        )

        assertEquals(
            0,
            result.skippedInvalidIdentityCount
        )

        assertEquals(
            1,
            result.skippedInvalidNutritionPayloadCount
        )
    }

    @Test
    fun generate_skipsCandidateWithBlankCanonicalId() {

        val candidate =
            CanonicalKnowledgeCandidate(
                canonicalId =
                    "   ",
                aliases =
                    setOf("candidate"),
                matchAliases =
                    emptySet(),
                dimensions =
                    listOf(
                        KnowledgeDimensionCandidate(
                            dimension =
                                KnowledgeDimensionCandidateType.NUTRITION,
                            payload =
                                mapOf(
                                    "energyKcalPer100g" to 100.0
                                )
                        )
                    ),
                metadata =
                    CandidateMetadata(
                        source =
                            "open_food_facts",
                        sourceId =
                            "123",
                        confidence =
                            1.0,
                        version =
                            "1"
                    )
            )

        val result =
            OFFNutritionReferenceCandidateGenerator()
                .generate(
                    candidates =
                        listOf(candidate)
                )

        assertEquals(
            1,
            result.inputCandidateCount
        )

        assertEquals(
            0,
            result.generatedCandidateCount
        )

        assertEquals(
            1,
            result.skippedInvalidIdentityCount
        )

        assertTrue(
            result.candidates.isEmpty()
        )
    }

    @Test
    fun generate_skipsCandidateWithoutSourceId() {

        val candidate =
            CanonicalKnowledgeCandidate(
                canonicalId =
                    "plain yogurt",
                aliases =
                    setOf("plain yogurt"),
                matchAliases =
                    emptySet(),
                dimensions =
                    listOf(
                        KnowledgeDimensionCandidate(
                            dimension =
                                KnowledgeDimensionCandidateType.NUTRITION,
                            payload =
                                mapOf(
                                    "energyKcalPer100g" to 61.0
                                )
                        )
                    ),
                metadata =
                    CandidateMetadata(
                        source =
                            "open_food_facts",
                        sourceId =
                            null,
                        confidence =
                            1.0,
                        version =
                            "1"
                    )
            )

        val result =
            OFFNutritionReferenceCandidateGenerator()
                .generate(
                    candidates =
                        listOf(candidate)
                )

        assertEquals(
            0,
            result.generatedCandidateCount
        )

        assertEquals(
            1,
            result.skippedInvalidIdentityCount
        )
    }

    private fun createNutritionCandidate(
        sourceId: String
    ): CanonicalKnowledgeCandidate {
        return createCandidate(
            sourceId =
                sourceId,
            dimensions =
                listOf(
                    KnowledgeDimensionCandidate(
                        dimension =
                            KnowledgeDimensionCandidateType.NUTRITION,
                        payload =
                            mapOf(
                                "energyKcalPer100g" to 100.0,
                                "fatPer100g" to 1.0,
                                "proteinsPer100g" to 2.0
                            )
                    )
                )
        )
    }

    private fun createCandidate(
        sourceId: String,
        dimensions: List<KnowledgeDimensionCandidate>
    ): CanonicalKnowledgeCandidate {
        return CanonicalKnowledgeCandidate(
            canonicalId =
                "candidate-$sourceId",
            aliases =
                setOf(
                    "candidate-$sourceId"
                ),
            matchAliases =
                emptySet(),
            dimensions =
                dimensions,
            metadata =
                CandidateMetadata(
                    source =
                        "open_food_facts",
                    sourceId =
                        sourceId,
                    confidence =
                        1.0,
                    version =
                        "1",
                    attributes =
                        mapOf(
                            "productName" to
                                    "candidate-$sourceId"
                        )
                )
        )
    }
}