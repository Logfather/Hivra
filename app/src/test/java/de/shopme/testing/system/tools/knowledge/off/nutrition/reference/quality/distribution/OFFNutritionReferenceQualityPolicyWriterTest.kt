package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.quality.policy

import de.shopme.tools.knowledge.off.nutrition.reference.quality.policy.OFFNutritionReferenceQualityPolicy
import de.shopme.tools.knowledge.off.nutrition.reference.quality.policy.OFFNutritionReferenceQualityPolicyEvidence
import de.shopme.tools.knowledge.off.nutrition.reference.quality.policy.OFFNutritionReferenceQualityPolicyInput
import de.shopme.tools.knowledge.off.nutrition.reference.quality.policy.OFFNutritionReferenceQualityPolicyRationale
import de.shopme.tools.knowledge.off.nutrition.reference.quality.policy.OFFNutritionReferenceQualityPolicyReader
import de.shopme.tools.knowledge.off.nutrition.reference.quality.policy.OFFNutritionReferenceQualityPolicyStatus
import de.shopme.tools.knowledge.off.nutrition.reference.quality.policy.OFFNutritionReferenceQualityPolicyThresholds
import de.shopme.tools.knowledge.off.nutrition.reference.quality.policy.OFFNutritionReferenceQualityPolicyWriter
import de.shopme.tools.knowledge.off.nutrition.reference.quality.policy.OFFNutritionReferenceQualityThresholdDecision
import de.shopme.tools.knowledge.off.nutrition.reference.quality.policy.OFFNutritionReferenceQualityZeroOnlyDecision
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class OFFNutritionReferenceQualityPolicyWriterTest {

    @Test
    fun write_thenRead_preservesPolicy() {

        val outputDirectory =
            Files.createTempDirectory(
                "off-quality-policy"
            ).toFile()

        val outputFile =
            outputDirectory.resolve(
                "policy.json"
            )

        val policy =
            OFFNutritionReferenceQualityPolicy(
                version = 1,
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
                        inputFile = "/tmp/off.jsonl.gz",
                        inputFileSizeBytes = 1_000L,
                        qualityReportVersion = 1,
                        distributionReportVersion = 1
                    ),
                thresholds =
                    OFFNutritionReferenceQualityPolicyThresholds(
                        maximumEnergyKcalPer100g = 950.0,
                        maximumComponentGramsPer100g = 100.0,
                        maximumMacronutrientSumGramsPer100g = 105.0,
                        relationshipToleranceGramsPer100g = 0.5
                    ),
                evidence =
                    OFFNutritionReferenceQualityPolicyEvidence(
                        generatedCandidateCount = 100L,
                        acceptedCandidateCount = 90L,
                        rejectedCandidateCount = 10L,
                        acceptanceRate = 0.9,
                        rejectionReasonOccurrenceCount = 13L,
                        multipleReasonRejectionCount = 3L,
                        macronutrientSumRejectionCount = 5L,
                        macronutrientSumAbove150Count = 4L,
                        aboveMaximumRejectionCount = 4L,
                        zeroOnlyRejectionCount = 3L,
                        sugarsExceedCarbohydratesCount = 1L,
                        saturatedFatExceedsTotalFatCount = 0L,
                        negativeNutritionValueCount = 0L
                    ),
                rationale =
                    OFFNutritionReferenceQualityPolicyRationale(
                        summary = "Thresholds remain unchanged.",
                        thresholdRationale =
                            listOf(
                                "Measured violations are material."
                            ),
                        zeroOnlyRationale =
                            listOf(
                                "Zero-only payloads are not generally safe."
                            ),
                        deferredActions =
                            listOf(
                                "Evaluate selective recovery separately."
                            )
                    )
            )

        OFFNutritionReferenceQualityPolicyWriter()
            .write(
                policy =
                    policy,
                outputFile =
                    outputFile
            )

        val restored =
            OFFNutritionReferenceQualityPolicyReader()
                .read(outputFile)

        assertEquals(
            policy,
            restored
        )
    }
}