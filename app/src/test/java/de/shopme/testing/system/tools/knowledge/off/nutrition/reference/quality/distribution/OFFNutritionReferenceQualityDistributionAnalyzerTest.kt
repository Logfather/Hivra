package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.quality.distribution

import de.shopme.tools.knowledge.off.nutrition.reference.CanonicalOFFNutritionReferenceCandidate
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityFilter
import de.shopme.tools.knowledge.off.nutrition.reference.quality.distribution.OFFNutritionReferenceQualityDistributionAnalyzer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OFFNutritionReferenceQualityDistributionAnalyzerTest {

    @Test
    fun observe_analyzesRejectionDistributionsDeterministically() {

        val valid =
            candidate(
                sourceId = "1",
                nutrition =
                    mapOf(
                        "energyKcalPer100g" to 100.0,
                        "fatPer100g" to 10.0,
                        "carbohydratesPer100g" to 20.0,
                        "proteinsPer100g" to 5.0
                    )
            )

        val excessive =
            candidate(
                sourceId = "2",
                nutrition =
                    mapOf(
                        "energyKcalPer100g" to 1_200.0,
                        "fatPer100g" to 70.0,
                        "carbohydratesPer100g" to 60.0,
                        "proteinsPer100g" to 20.0
                    )
            )

        val zeroOnly =
            candidate(
                sourceId = "3",
                nutrition =
                    mapOf(
                        "energyKcalPer100g" to 0.0,
                        "fatPer100g" to 0.0,
                        "carbohydratesPer100g" to 0.0
                    )
            )

        val candidates =
            listOf(
                valid,
                excessive,
                zeroOnly
            )

        val qualityResult =
            OFFNutritionReferenceQualityFilter()
                .filter(candidates)

        val analyzer =
            OFFNutritionReferenceQualityDistributionAnalyzer(
                maximumExamplesPerReason = 2
            )

        analyzer.observe(
            candidates =
                candidates,
            qualityResult =
                qualityResult
        )

        val report =
            analyzer.createReport()

        assertEquals(
            3L,
            report.analyzedCandidateCount
        )

        assertEquals(
            1L,
            report.acceptedCandidateCount
        )

        assertEquals(
            2L,
            report.rejectedCandidateCount
        )

        assertEquals(
            1L,
            report.aboveMaximumCountsByNutritionKey[
                "energyKcalPer100g"
            ]
        )

        assertEquals(
            1L,
            report.macronutrientSumBuckets[
                "GT_120_TO_150"
            ]
        )

        assertEquals(
            1L,
            report.zeroOnlyCountsByPresentKeyCount[
                "3"
            ]
        )

        assertTrue(
            report.examplesByReason.isNotEmpty()
        )
    }

    private fun candidate(
        sourceId: String,
        nutrition: Map<String, Double>
    ): CanonicalOFFNutritionReferenceCandidate =
        CanonicalOFFNutritionReferenceCandidate(
            sourceId =
                sourceId,
            canonicalId =
                "product-$sourceId",
            aliases =
                sortedSetOf("product-$sourceId"),
            matchAliases =
                emptySet(),
            nutrition =
                nutrition.toSortedMap(),
            productName =
                "Product $sourceId",
            brand =
                null,
            categories =
                null,
            singleIngredientNutritionAliases =
                emptySet(),
            source =
                "open_food_facts",
            sourceVersion =
                "1",
            sourceConfidence =
                1.0
        )
}