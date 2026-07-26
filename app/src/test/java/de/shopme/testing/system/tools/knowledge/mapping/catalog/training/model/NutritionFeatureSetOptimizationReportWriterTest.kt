package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionFeatureSetOptimizationEntry
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionFeatureSetOptimizationReport
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionFeatureSetOptimizationReportWriter
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionFeatureSetOptimizer
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NutritionFeatureSetOptimizationReportWriterTest {

    @Test
    fun writeReportDeterministically() {

        val directory =
            createTempDirectory(
                prefix =
                    "nutrition-feature-set-report-",
            )
                .toFile()

        try {
            val outputFile =
                File(
                    directory,
                    "nutrition.local-matcher-feature-set-optimization.json",
                )

            val entry =
                NutritionFeatureSetOptimizationEntry(
                    name =
                        NutritionFeatureSetOptimizer
                            .BASE_CANDIDATE_NAME,
                    featureNames =
                        listOf(
                            "diagnostic_score",
                        ),
                    featureCount =
                        1,
                    decisionThreshold =
                        0.9,
                    thresholdPolicySatisfied =
                        true,
                    testExampleCount =
                        10,
                    testPrecision =
                        1.0,
                    testRecall =
                        0.8,
                    testF1 =
                        0.8888888888888888,
                    testBalancedAccuracy =
                        0.9,
                    testTruePositiveCount =
                        4,
                    testFalsePositiveCount =
                        0,
                    testTrueNegativeCount =
                        5,
                    testFalseNegativeCount =
                        1,
                )

            val report =
                NutritionFeatureSetOptimizationReport(
                    version =
                        NutritionFeatureSetOptimizer
                            .REPORT_VERSION,
                    candidateCount =
                        1,
                    recommendedCandidateName =
                        entry.name,
                    recommendedFeatureNames =
                        entry.featureNames,
                    entries =
                        listOf(
                            entry,
                        ),
                )

            val writer =
                NutritionFeatureSetOptimizationReportWriter()

            writer.write(
                report =
                    report,
                outputFile =
                    outputFile,
            )

            val firstContent =
                outputFile.readText()

            writer.write(
                report =
                    report,
                outputFile =
                    outputFile,
            )

            assertEquals(
                expected =
                    firstContent,
                actual =
                    outputFile.readText(),
            )

            assertTrue(
                actual =
                    outputFile.isFile,
            )

            val persisted =
                GsonBuilder()
                    .create()
                    .fromJson(
                        outputFile.readText(),
                        NutritionFeatureSetOptimizationReport::class.java,
                    )

            assertEquals(
                expected =
                    report,
                actual =
                    persisted,
            )
        } finally {
            directory.deleteRecursively()
        }
    }
}