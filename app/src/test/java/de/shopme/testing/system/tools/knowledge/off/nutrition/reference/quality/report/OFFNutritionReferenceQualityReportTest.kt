package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.quality.report

import de.shopme.tools.knowledge.off.nutrition.reference.CanonicalOFFNutritionReferenceCandidate
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityFilter
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityRejectionReason
import de.shopme.tools.knowledge.off.nutrition.reference.quality.report.OFFNutritionReferenceQualityReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OFFNutritionReferenceQualityReportTest {

    @Test
    fun from_createsDeterministicCompleteQualityReport() {

        val candidates =
            listOf(
                createCandidate(
                    sourceId =
                        "accepted",
                    canonicalId =
                        "accepted",
                    nutrition =
                        mapOf(
                            "energyKcalPer100g" to 100.0
                        )
                ),
                createCandidate(
                    sourceId =
                        "negative",
                    canonicalId =
                        "negative",
                    nutrition =
                        mapOf(
                            "fatPer100g" to -1.0
                        )
                ),
                createCandidate(
                    sourceId =
                        "above-maximum",
                    canonicalId =
                        "above-maximum",
                    nutrition =
                        mapOf(
                            "energyKcalPer100g" to 950.0001
                        )
                )
            )

        val filterResult =
            OFFNutritionReferenceQualityFilter()
                .filter(
                    candidates =
                        candidates
                )

        val report =
            OFFNutritionReferenceQualityReport
                .from(
                    filterResult =
                        filterResult
                )

        assertEquals(
            1,
            report.version
        )

        assertEquals(
            3,
            report.inputCandidateCount
        )

        assertEquals(
            1,
            report.acceptedCandidateCount
        )

        assertEquals(
            2,
            report.rejectedCandidateCount
        )

        assertEquals(
            2,
            report.rejectionReasonOccurrenceCount
        )

        assertEquals(
            1.0 / 3.0,
            report.acceptanceRate,
            0.0
        )

        assertEquals(
            1,
            report.countsByReason[
                OFFNutritionReferenceQualityRejectionReason
                    .NEGATIVE_NUTRITION_VALUE
            ]
        )

        assertEquals(
            1,
            report.countsByReason[
                OFFNutritionReferenceQualityRejectionReason
                    .NUTRITION_VALUE_ABOVE_MAXIMUM
            ]
        )

        assertEquals(
            0,
            report.countsByReason[
                OFFNutritionReferenceQualityRejectionReason
                    .EMPTY_NUTRITION_PAYLOAD
            ]
        )

        assertEquals(
            OFFNutritionReferenceQualityRejectionReason
                .entries
                .size,
            report.countsByReason.size
        )

        assertTrue(
            report.countsByReason.keys.toList() ==
                    report.countsByReason.keys
                        .sortedBy { reason ->
                            reason.name
                        }
        )
    }

    @Test
    fun from_usesZeroAcceptanceRateForEmptyInput() {

        val filterResult =
            OFFNutritionReferenceQualityFilter()
                .filter(
                    candidates =
                        emptyList()
                )

        val report =
            OFFNutritionReferenceQualityReport
                .from(
                    filterResult =
                        filterResult
                )

        assertEquals(
            0,
            report.inputCandidateCount
        )

        assertEquals(
            0.0,
            report.acceptanceRate,
            0.0
        )

        assertEquals(
            0,
            report.rejectionReasonOccurrenceCount
        )
    }

    private fun createCandidate(
        sourceId: String,
        canonicalId: String,
        nutrition: Map<String, Double>
    ): CanonicalOFFNutritionReferenceCandidate {

        return CanonicalOFFNutritionReferenceCandidate(
            sourceId =
                sourceId,
            canonicalId =
                canonicalId,
            aliases =
                emptySet(),
            matchAliases =
                emptySet(),
            nutrition =
                nutrition,
            productName =
                null,
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
}