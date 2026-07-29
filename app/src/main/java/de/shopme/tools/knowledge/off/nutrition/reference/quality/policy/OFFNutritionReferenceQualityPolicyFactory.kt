package de.shopme.tools.knowledge.off.nutrition.reference.quality.policy

import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityRejectionReason
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityThresholds
import de.shopme.tools.knowledge.off.nutrition.reference.quality.distribution.OFFNutritionReferenceQualityDistributionReport
import de.shopme.tools.knowledge.off.nutrition.reference.quality.pipeline.streaming.StreamingOFFNutritionReferenceQualityReport

class OFFNutritionReferenceQualityPolicyFactory {

    fun create(
        qualityReport:
        StreamingOFFNutritionReferenceQualityReport,
        distributionReport:
        OFFNutritionReferenceQualityDistributionReport
    ): OFFNutritionReferenceQualityPolicy {

        validateReportConsistency(
            qualityReport =
                qualityReport,
            distributionReport =
                distributionReport
        )

        val countsByReason =
            distributionReport.countsByReason

        return OFFNutritionReferenceQualityPolicy(
            version =
                OFFNutritionReferenceQualityPolicy.CURRENT_VERSION,
            policyStatus =
                OFFNutritionReferenceQualityPolicyStatus.APPROVED,
            thresholdDecision =
                OFFNutritionReferenceQualityThresholdDecision
                    .UNCHANGED,
            zeroOnlyDecision =
                OFFNutritionReferenceQualityZeroOnlyDecision
                    .REJECT_WITH_DEFERRED_SELECTIVE_RECOVERY,
            input =
                OFFNutritionReferenceQualityPolicyInput(
                    inputFile =
                        qualityReport.inputFile,
                    inputFileSizeBytes =
                        qualityReport.inputFileSizeBytes,
                    qualityReportVersion =
                        qualityReport.version,
                    distributionReportVersion =
                        distributionReport.version
                ),
            thresholds =
                OFFNutritionReferenceQualityPolicyThresholds(
                    maximumEnergyKcalPer100g =
                        OFFNutritionReferenceQualityThresholds
                            .MAXIMUM_ENERGY_KCAL_PER_100G,
                    maximumComponentGramsPer100g =
                        OFFNutritionReferenceQualityThresholds
                            .MAXIMUM_COMPONENT_GRAMS_PER_100G,
                    OFFNutritionReferenceQualityThresholds
                        .MAXIMUM_MACRONUTRIENT_SUM_GRAMS,
                    relationshipToleranceGramsPer100g =
                        OFFNutritionReferenceQualityThresholds
                            .RELATIONSHIP_TOLERANCE_GRAMS
                ),
            evidence =
                OFFNutritionReferenceQualityPolicyEvidence(
                    generatedCandidateCount =
                        qualityReport.generatedCandidateCount,
                    acceptedCandidateCount =
                        qualityReport.acceptedCandidateCount,
                    rejectedCandidateCount =
                        qualityReport.rejectedCandidateCount,
                    acceptanceRate =
                        qualityReport.acceptanceRate,
                    rejectionReasonOccurrenceCount =
                        qualityReport.rejectionReasonOccurrenceCount,
                    multipleReasonRejectionCount =
                        distributionReport
                            .multipleReasonRejectionCount,
                    macronutrientSumRejectionCount =
                        countsByReason.count(
                            OFFNutritionReferenceQualityRejectionReason
                                .MACRONUTRIENT_SUM_EXCEEDS_100_GRAMS
                        ),
                    macronutrientSumAbove150Count =
                        distributionReport
                            .macronutrientSumBuckets
                            .getOrDefault(
                                MACRONUTRIENT_SUM_ABOVE_150_BUCKET,
                                0L
                            ),
                    aboveMaximumRejectionCount =
                        countsByReason.count(
                            OFFNutritionReferenceQualityRejectionReason
                                .NUTRITION_VALUE_ABOVE_MAXIMUM
                        ),
                    zeroOnlyRejectionCount =
                        countsByReason.count(
                            OFFNutritionReferenceQualityRejectionReason
                                .ZERO_ONLY_NUTRITION_PAYLOAD
                        ),
                    sugarsExceedCarbohydratesCount =
                        countsByReason.count(
                            OFFNutritionReferenceQualityRejectionReason
                                .SUGARS_EXCEED_CARBOHYDRATES
                        ),
                    saturatedFatExceedsTotalFatCount =
                        countsByReason.count(
                            OFFNutritionReferenceQualityRejectionReason
                                .SATURATED_FAT_EXCEEDS_TOTAL_FAT
                        ),
                    negativeNutritionValueCount =
                        countsByReason.count(
                            OFFNutritionReferenceQualityRejectionReason
                                .NEGATIVE_NUTRITION_VALUE
                        )
                ),
            rationale =
                createRationale()
        )
    }

    private fun validateReportConsistency(
        qualityReport:
        StreamingOFFNutritionReferenceQualityReport,
        distributionReport:
        OFFNutritionReferenceQualityDistributionReport
    ) {
        require(
            qualityReport.generatedCandidateCount ==
                    distributionReport.analyzedCandidateCount
        ) {
            "Quality and distribution reports disagree on analyzed candidates."
        }

        require(
            qualityReport.acceptedCandidateCount ==
                    distributionReport.acceptedCandidateCount
        ) {
            "Quality and distribution reports disagree on accepted candidates."
        }

        require(
            qualityReport.rejectedCandidateCount ==
                    distributionReport.rejectedCandidateCount
        ) {
            "Quality and distribution reports disagree on rejected candidates."
        }

        require(
            qualityReport.rejectionReasonOccurrenceCount ==
                    distributionReport.rejectionReasonOccurrenceCount
        ) {
            "Quality and distribution reports disagree on reason occurrences."
        }

        require(
            qualityReport.countsByReason ==
                    distributionReport.countsByReason
        ) {
            "Quality and distribution reports disagree on reason counts."
        }
    }

    private fun createRationale():
            OFFNutritionReferenceQualityPolicyRationale =
        OFFNutritionReferenceQualityPolicyRationale(
            summary =
                "The full Open Food Facts nutrition distribution " +
                        "supports retaining the current quality thresholds. " +
                        "Most rejected values are materially invalid rather " +
                        "than minor rounding deviations.",
            thresholdRationale =
                listOf(
                    "The majority of macronutrient-sum violations " +
                            "are substantially above the permitted range.",

                    "Most nutrition values above their maximum " +
                            "exceed the maximum by large margins and are " +
                            "consistent with unit, serving-size, scaling, " +
                            "or source-data errors.",

                    "Only a small minority of relationship violations " +
                            "fall immediately above the configured tolerance.",

                    "Relaxing the thresholds would recover few " +
                            "candidates while increasing the risk of " +
                            "admitting physically implausible references."
                ),
            zeroOnlyRationale =
                listOf(
                    "Zero-only payloads contain no discriminative " +
                            "nutrition evidence for general reference matching.",

                    "Some zero-only products may be legitimate, but " +
                            "the full distribution also contains clearly " +
                            "incorrect zero-only product records.",

                    "Zero-only payloads therefore remain rejected " +
                            "unless a separate deterministic recovery policy " +
                            "can establish a sufficiently narrow safe class."
                ),
            deferredActions =
                listOf(
                    "Evaluate a separate category-constrained recovery " +
                            "policy for legitimate zero-only products.",

                    "Do not change the general OFF nutrition quality " +
                            "thresholds without new measured evidence."
                )
        )

    private fun Map<String, Long>.count(
        reason: OFFNutritionReferenceQualityRejectionReason
    ): Long =
        getOrDefault(
            reason.name,
            0L
        )

    private companion object {

        const val MACRONUTRIENT_SUM_ABOVE_150_BUCKET =
            "GT_150"
    }
}