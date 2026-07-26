package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionMatcherThresholdPolicyOptimizationReportWriter
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionMatcherThresholdPolicyOptimizer
import java.io.File
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunNutritionMatcherThresholdPolicyOptimizationTest {

    @Test
    fun runNutritionMatcherThresholdPolicyOptimization() {

        val projectRoot =
            File(
                "..",
            )

        val datasetFile =
            File(
                projectRoot,
                "data/generated/knowledge/training/" +
                        "nutrition.matcher-training-dataset.json",
            )

        val modelDirectory =
            File(
                projectRoot,
                "data/generated/knowledge/models/comparison/" +
                        "threshold-policy-optimization",
            )

        val reportFile =
            File(
                projectRoot,
                "data/generated/knowledge/reports/" +
                        "nutrition.local-matcher-threshold-" +
                        "optimization.json",
            )

        require(
            datasetFile.isFile,
        ) {
            "Nutrition matcher training dataset does not exist: " +
                    datasetFile.absolutePath
        }

        val report =
            NutritionMatcherThresholdPolicyOptimizer()
                .optimize(
                    datasetFile =
                        datasetFile,
                    workingDirectory =
                        modelDirectory,
                )

        NutritionMatcherThresholdPolicyOptimizationReportWriter()
            .write(
                report =
                    report,
                outputFile =
                    reportFile,
            )

        assertEquals(
            expected =
                4,
            actual =
                report.candidateCount,
        )

        assertTrue(
            actual =
                report.recommendedDecisionThreshold in
                        0.0..1.0,
        )

        assertTrue(
            actual =
                reportFile.isFile,
        )

        println()
        println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        )
        println(
            "NUTRITION THRESHOLD POLICY OPTIMIZATION",
        )
        println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        )

        report.entries
            .sortedBy { entry ->
                entry.conservatismRank
            }
            .forEach { entry ->
                println(
                    entry.name.padEnd(
                        length =
                            18,
                    ) +
                            " threshold=" +
                            format(
                                entry.decisionThreshold,
                            ) +
                            " testPrecision=" +
                            format(
                                entry.testPrecision,
                            ) +
                            " testRecall=" +
                            format(
                                entry.testRecall,
                            ) +
                            " testF1=" +
                            format(
                                entry.testF1,
                            ) +
                            " fp=" +
                            entry.testFalsePositiveCount +
                            " policy=" +
                            entry.calibrationPolicySatisfied,
                )
            }

        println()
        println(
            "Recommended candidate  : " +
                    report.recommendedCandidateName,
        )
        println(
            "Recommended threshold  : " +
                    format(
                        report.recommendedDecisionThreshold,
                    ),
        )
        println(
            "Report                 : " +
                    reportFile.absolutePath,
        )
        println(
            "Models                 : " +
                    modelDirectory.absolutePath,
        )
        println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        )
    }

    private fun format(
        value: Double,
    ): String {

        return "%.6f".format(
            locale =
                Locale.ROOT,
            value,
        )
    }
}