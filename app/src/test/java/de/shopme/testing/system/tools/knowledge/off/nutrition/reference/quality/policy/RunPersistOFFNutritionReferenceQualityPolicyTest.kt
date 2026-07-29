package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.quality.policy

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.off.nutrition.reference.quality.distribution.OFFNutritionReferenceQualityDistributionReport
import de.shopme.tools.knowledge.off.nutrition.reference.quality.pipeline.streaming.StreamingOFFNutritionReferenceQualityReport
import de.shopme.tools.knowledge.off.nutrition.reference.quality.policy.OFFNutritionReferenceQualityPolicyFactory
import de.shopme.tools.knowledge.off.nutrition.reference.quality.policy.OFFNutritionReferenceQualityPolicyReader
import de.shopme.tools.knowledge.off.nutrition.reference.quality.policy.OFFNutritionReferenceQualityPolicyStatus
import de.shopme.tools.knowledge.off.nutrition.reference.quality.policy.OFFNutritionReferenceQualityPolicyWriter
import de.shopme.tools.knowledge.off.nutrition.reference.quality.policy.OFFNutritionReferenceQualityThresholdDecision
import de.shopme.tools.knowledge.off.nutrition.reference.quality.policy.OFFNutritionReferenceQualityZeroOnlyDecision
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunPersistOFFNutritionReferenceQualityPolicyTest {

    private val gson =
        GsonBuilder()
            .disableHtmlEscaping()
            .create()

    @Test
    fun persistOFFNutritionReferenceQualityPolicy() {

        val repositoryRoot =
            resolveRepositoryRoot()

        val qualityReportFile =
            repositoryRoot.resolve(
                "data/generated/knowledge/reports/" +
                        "off-nutrition-reference-quality-full.json"
            )

        val distributionReportFile =
            repositoryRoot.resolve(
                "data/generated/knowledge/reports/" +
                        "off-nutrition-reference-quality-" +
                        "distribution-full.json"
            )

        val policyFile =
            repositoryRoot.resolve(
                "data/generated/knowledge/policies/" +
                        "off-nutrition-reference-quality-policy.json"
            )

        require(qualityReportFile.isFile) {
            "Missing quality report: " +
                    qualityReportFile.absolutePath
        }

        require(distributionReportFile.isFile) {
            "Missing distribution report: " +
                    distributionReportFile.absolutePath
        }

        val qualityReport =
            gson.fromJson(
                qualityReportFile.readText(),
                StreamingOFFNutritionReferenceQualityReport::class.java
            )

        val distributionReport =
            gson.fromJson(
                distributionReportFile.readText(),
                OFFNutritionReferenceQualityDistributionReport::class.java
            )

        val policy =
            OFFNutritionReferenceQualityPolicyFactory()
                .create(
                    qualityReport =
                        qualityReport,
                    distributionReport =
                        distributionReport
                )

        OFFNutritionReferenceQualityPolicyWriter()
            .write(
                policy =
                    policy,
                outputFile =
                    policyFile
            )

        val persistedPolicy =
            OFFNutritionReferenceQualityPolicyReader()
                .read(policyFile)

        assertEquals(
            OFFNutritionReferenceQualityPolicyStatus.APPROVED,
            persistedPolicy.policyStatus
        )

        assertEquals(
            OFFNutritionReferenceQualityThresholdDecision.UNCHANGED,
            persistedPolicy.thresholdDecision
        )

        assertEquals(
            OFFNutritionReferenceQualityZeroOnlyDecision
                .REJECT_WITH_DEFERRED_SELECTIVE_RECOVERY,
            persistedPolicy.zeroOnlyDecision
        )

        assertEquals(
            865_511L,
            persistedPolicy.evidence.generatedCandidateCount
        )

        assertEquals(
            818_631L,
            persistedPolicy.evidence.acceptedCandidateCount
        )

        assertEquals(
            46_880L,
            persistedPolicy.evidence.rejectedCandidateCount
        )

        assertEquals(
            23_178L,
            persistedPolicy.evidence
                .macronutrientSumRejectionCount
        )

        assertEquals(
            18_162L,
            persistedPolicy.evidence
                .macronutrientSumAbove150Count
        )

        assertEquals(
            20_472L,
            persistedPolicy.evidence.zeroOnlyRejectionCount
        )

        assertTrue(policyFile.isFile)

        println(
            "policyFile=" +
                    policyFile.absolutePath
        )

        println(
            "policyStatus=" +
                    persistedPolicy.policyStatus
        )

        println(
            "thresholdDecision=" +
                    persistedPolicy.thresholdDecision
        )

        println(
            "zeroOnlyDecision=" +
                    persistedPolicy.zeroOnlyDecision
        )

        println(
            "acceptedCandidateCount=" +
                    persistedPolicy.evidence.acceptedCandidateCount
        )
    }

    private fun resolveRepositoryRoot(): File {

        val workingDirectory =
            File(".").canonicalFile

        return if (workingDirectory.name == "app") {
            requireNotNull(
                workingDirectory.parentFile
            ).canonicalFile
        } else {
            workingDirectory
        }
    }
}