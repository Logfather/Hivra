package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.deduplication

import de.shopme.tools.knowledge.off.nutrition.reference.CanonicalOFFNutritionReferenceCandidate
import de.shopme.tools.knowledge.off.nutrition.reference.deduplication.OFFNutritionReferenceCandidateDeduplicator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OFFNutritionReferenceCandidateDeduplicatorTest {

    @Test
    fun deduplicate_mergesEqualCanonicalIdAndNutrition() {

        val lessComplete =
            createCandidate(
                sourceId =
                    "222",
                canonicalId =
                    "plain yogurt",
                aliases =
                    setOf("yogurt"),
                matchAliases =
                    setOf("dairy"),
                productName =
                    null,
                brand =
                    null,
                categories =
                    null,
                singleIngredientAliases =
                    emptySet()
            )

        val moreComplete =
            createCandidate(
                sourceId =
                    "111",
                canonicalId =
                    "plain yogurt",
                aliases =
                    setOf("plain yogurt"),
                matchAliases =
                    setOf("fermented dairy"),
                productName =
                    "Natural Yogurt",
                brand =
                    "Example",
                categories =
                    "Dairies, Yogurts",
                singleIngredientAliases =
                    setOf("natural yogurt")
            )

        val result =
            OFFNutritionReferenceCandidateDeduplicator()
                .deduplicate(
                    listOf(
                        lessComplete,
                        moreComplete
                    )
                )

        assertEquals(
            2,
            result.inputCandidateCount
        )

        assertEquals(
            1,
            result.outputCandidateCount
        )

        assertEquals(
            1,
            result.removedDuplicateCount
        )

        assertEquals(
            1,
            result.duplicateGroupCount
        )

        val candidate =
            result.candidates.single()

        assertEquals(
            "111",
            candidate.sourceId
        )

        assertEquals(
            setOf(
                "Natural Yogurt",
                "plain yogurt",
                "yogurt"
            ),
            candidate.aliases
        )

        assertEquals(
            setOf(
                "dairy",
                "fermented dairy"
            ),
            candidate.matchAliases
        )

        assertEquals(
            setOf("natural yogurt"),
            candidate.singleIngredientNutritionAliases
        )

        assertEquals(
            listOf(
                "111",
                "222"
            ),
            result
                .duplicateGroups
                .single()
                .mergedSourceIds
        )
    }

    @Test
    fun deduplicate_keepsDifferentNutritionProfiles() {

        val lowFat =
            createCandidate(
                sourceId =
                    "111",
                canonicalId =
                    "plain yogurt",
                nutrition =
                    nutrition(
                        fat =
                            0.1,
                        carbohydrates =
                            4.0,
                        proteins =
                            4.5
                    )
            )

        val fullFat =
            createCandidate(
                sourceId =
                    "222",
                canonicalId =
                    "plain yogurt",
                nutrition =
                    nutrition(
                        fat =
                            10.0,
                        carbohydrates =
                            4.0,
                        proteins =
                            3.0
                    )
            )

        val result =
            OFFNutritionReferenceCandidateDeduplicator()
                .deduplicate(
                    listOf(
                        lowFat,
                        fullFat
                    )
                )

        assertEquals(
            2,
            result.outputCandidateCount
        )

        assertEquals(
            0,
            result.removedDuplicateCount
        )

        assertTrue(
            result.duplicateGroups.isEmpty()
        )
    }

    @Test
    fun deduplicate_keepsEqualNutritionForDifferentCanonicalIds() {

        val apple =
            createCandidate(
                sourceId =
                    "111",
                canonicalId =
                    "apple"
            )

        val pear =
            createCandidate(
                sourceId =
                    "222",
                canonicalId =
                    "pear"
            )

        val result =
            OFFNutritionReferenceCandidateDeduplicator()
                .deduplicate(
                    listOf(
                        apple,
                        pear
                    )
                )

        assertEquals(
            2,
            result.outputCandidateCount
        )

        assertEquals(
            0,
            result.removedDuplicateCount
        )
    }

    @Test
    fun deduplicate_isIndependentOfInputOrder() {

        val first =
            createCandidate(
                sourceId =
                    "111",
                canonicalId =
                    "apple",
                aliases =
                    setOf("apple")
            )

        val second =
            createCandidate(
                sourceId =
                    "222",
                canonicalId =
                    "apple",
                aliases =
                    setOf("fresh apple")
            )

        val deduplicator =
            OFFNutritionReferenceCandidateDeduplicator()

        val forward =
            deduplicator.deduplicate(
                listOf(
                    first,
                    second
                )
            )

        val reverse =
            deduplicator.deduplicate(
                listOf(
                    second,
                    first
                )
            )

        assertEquals(
            forward,
            reverse
        )
    }

    @Test
    fun nutritionFingerprint_isStableForMapOrderAndNumberFormatting() {

        val deduplicator =
            OFFNutritionReferenceCandidateDeduplicator()

        val first =
            linkedMapOf(
                "fatPer100g" to 1.0,
                "proteinsPer100g" to 2.50
            )

        val second =
            linkedMapOf(
                "proteinsPer100g" to 2.5,
                "fatPer100g" to 1.00
            )

        assertEquals(
            deduplicator.nutritionFingerprint(first),
            deduplicator.nutritionFingerprint(second)
        )
    }

    private fun createCandidate(
        sourceId: String,
        canonicalId: String,
        aliases: Set<String> =
            setOf(canonicalId),
        matchAliases: Set<String> =
            emptySet(),
        nutrition: Map<String, Double> =
            nutrition(
                fat =
                    3.3,
                carbohydrates =
                    4.7,
                proteins =
                    3.5
            ),
        productName: String? =
            canonicalId,
        brand: String? =
            "example",
        categories: String? =
            "example category",
        singleIngredientAliases: Set<String> =
            emptySet()
    ): CanonicalOFFNutritionReferenceCandidate =
        CanonicalOFFNutritionReferenceCandidate(
            sourceId =
                sourceId,
            canonicalId =
                canonicalId,
            aliases =
                aliases.toSortedSet(),
            matchAliases =
                matchAliases.toSortedSet(),
            nutrition =
                nutrition.toSortedMap(),
            productName =
                productName,
            brand =
                brand,
            categories =
                categories,
            singleIngredientNutritionAliases =
                singleIngredientAliases.toSortedSet(),
            source =
                "open_food_facts",
            sourceVersion =
                "1",
            sourceConfidence =
                1.0
        )

    private companion object {

        fun nutrition(
            fat: Double,
            carbohydrates: Double,
            proteins: Double
        ): Map<String, Double> =
            sortedMapOf(
                "energyKcalPer100g" to 61.0,
                "fatPer100g" to fat,
                "carbohydratesPer100g" to carbohydrates,
                "proteinsPer100g" to proteins,
                "saltPer100g" to 0.1
            )
    }
}