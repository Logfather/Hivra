package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.quality.policy

import de.shopme.tools.knowledge.off.nutrition.reference.quality.distribution.OFFNutritionReferenceQualityDistributionReport
import de.shopme.tools.knowledge.off.nutrition.reference.quality.distribution.OFFNutritionValueRange
import de.shopme.tools.knowledge.off.nutrition.reference.quality.pipeline.streaming.StreamingOFFNutritionReferenceQualityReport
import de.shopme.tools.knowledge.off.nutrition.reference.quality.policy.OFFNutritionReferenceQualityPolicyFactory
import de.shopme.tools.knowledge.off.nutrition.reference.quality.policy.OFFNutritionReferenceQualityThresholdDecision
import de.shopme.tools.knowledge.off.nutrition.reference.quality.policy.OFFNutritionReferenceQualityZeroOnlyDecision
import kotlin.test.Test
import kotlin.test.assertEquals

class OFFNutritionReferenceQualityPolicyFactoryTest {

    @Test
    fun create_persistsUnchangedThresholdDecisionFromMeasuredEvidence() {

        val qualityReport =
            StreamingOFFNutritionReferenceQualityReport(
                version = 1,
                inputFile = "/tmp/off.jsonl.gz",
                inputFileSizeBytes = 1_000L,
                batchSize = 1_000,
                maxCandidates = null,
                extractedCandidateCount = 120L,
                generatedCandidateCount = 100L,
                skippedWithoutNutritionCount = 15L,
                skippedInvalidIdentityCount = 5L,
                skippedInvalidNutritionPayloadCount = 0L,
                acceptedCandidateCount = 90L,
                rejectedCandidateCount = 10L,
                rejectionReasonOccurrenceCount = 13L,
                acceptanceRate = 0.9,
                countsByReason =
                    reasonCounts(
                        macronutrientSum = 5L,
                        aboveMaximum = 4L,
                        zeroOnly = 3L,
                        sugars = 1L,
                        saturatedFat = 0L,
                        negative = 0L
                    ),
                durationMillis = 100L
            )

        val distributionReport =
            OFFNutritionReferenceQualityDistributionReport(
                version = 1,
                analyzedCandidateCount = 100L,
                acceptedCandidateCount = 90L,
                rejectedCandidateCount = 10L,
                rejectionReasonOccurrenceCount = 13L,
                multipleReasonRejectionCount = 3L,
                countsByReason =
                    qualityReport.countsByReason,
                countsByReasonCombination =
                    mapOf(
                        "ZERO_ONLY_NUTRITION_PAYLOAD" to 3L,
                        "MACRONUTRIENT_SUM_EXCEEDS_100_GRAMS" to 3L,
                        "NUTRITION_VALUE_ABOVE_MAXIMUM" to 1L,
                        "MACRONUTRIENT_SUM_EXCEEDS_100_GRAMS+" +
                                "NUTRITION_VALUE_ABOVE_MAXIMUM" to 2L,
                        "SUGARS_EXCEED_CARBOHYDRATES" to 1L
                    ),
                countsByReasonCount =
                    mapOf(
                        "1" to 8L,
                        "2" to 2L
                    ),
                negativeValueCountsByNutritionKey =
                    emptyMap(),
                aboveMaximumCountsByNutritionKey =
                    mapOf(
                        "energyKcalPer100g" to 4L
                    ),
                aboveMaximumExcessBucketsByNutritionKey =
                    mapOf(
                        "energyKcalPer100g" to
                                mapOf(
                                    "GT_100" to 4L
                                )
                    ),
                macronutrientSumBuckets =
                    mapOf(
                        "GT_105_TO_110" to 1L,
                        "GT_150" to 4L
                    ),
                sugarExcessBuckets =
                    mapOf(
                        "GT_1_TO_5" to 1L
                    ),
                saturatedFatExcessBuckets =
                    emptyMap(),
                zeroOnlyCountsByPresentKeyCount =
                    mapOf(
                        "8_OR_MORE" to 3L
                    ),
                valueRangesByNutritionKey =
                    mapOf(
                        "energyKcalPer100g" to
                                OFFNutritionValueRange(
                                    observationCount = 100L,
                                    minimum = 0.0,
                                    maximum = 5_000.0
                                )
                    ),
                examplesByReason =
                    emptyMap()
            )

        val policy =
            OFFNutritionReferenceQualityPolicyFactory()
                .create(
                    qualityReport =
                        qualityReport,
                    distributionReport =
                        distributionReport
                )

        assertEquals(
            OFFNutritionReferenceQualityThresholdDecision.UNCHANGED,
            policy.thresholdDecision
        )

        assertEquals(
            OFFNutritionReferenceQualityZeroOnlyDecision
                .REJECT_WITH_DEFERRED_SELECTIVE_RECOVERY,
            policy.zeroOnlyDecision
        )

        assertEquals(
            4L,
            policy.evidence.macronutrientSumAbove150Count
        )

        assertEquals(
            3L,
            policy.evidence.zeroOnlyRejectionCount
        )
    }

    private fun reasonCounts(
        macronutrientSum: Long,
        aboveMaximum: Long,
        zeroOnly: Long,
        sugars: Long,
        saturatedFat: Long,
        negative: Long
    ): Map<String, Long> =
        sortedMapOf(
            "EMPTY_NUTRITION_PAYLOAD" to 0L,
            "MACRONUTRIENT_SUM_EXCEEDS_100_GRAMS" to
                    macronutrientSum,
            "NEGATIVE_NUTRITION_VALUE" to negative,
            "NON_FINITE_NUTRITION_VALUE" to 0L,
            "NUTRITION_VALUE_ABOVE_MAXIMUM" to aboveMaximum,
            "SATURATED_FAT_EXCEEDS_TOTAL_FAT" to
                    saturatedFat,
            "SUGARS_EXCEED_CARBOHYDRATES" to sugars,
            "UNSUPPORTED_NUTRITION_KEY" to 0L,
            "ZERO_ONLY_NUTRITION_PAYLOAD" to zeroOnly
        )
}