package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import com.google.gson.Gson
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureExtractor
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionMatcherModelComparisonReport
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionMatcherModelComparisonReportWriter
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionMatcherModelComparisonSnapshot
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionMatcherModelMetricDelta
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionMatcherModelRecommendation
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionMatcherRecommendedModel
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NutritionMatcherModelComparisonReportWriterTest {

    @Test
    fun writeComparisonReportDeterministically() {

        val temporaryDirectory =
            Files
                .createTempDirectory(
                    "nutrition-matcher-comparison-report-",
                )
                .toFile()

        try {
            val outputFile =
                File(
                    temporaryDirectory,
                    "nested/" +
                            "nutrition.local-matcher-feature-comparison.json",
                )

            val baselineSnapshot =
                snapshot(
                    featureNames =
                        LocalNutritionMatcherFeatureExtractor
                            .BASE_FEATURE_NAMES,
                    f1 =
                        0.57,
                    balancedAccuracy =
                        0.86,
                )

            val extendedSnapshot =
                snapshot(
                    featureNames =
                        LocalNutritionMatcherFeatureExtractor
                            .BASE_FEATURE_NAMES +
                                LocalNutritionMatcherFeatureExtractor
                                    .DOMAIN_MISMATCH_FEATURE_NAMES,
                    f1 =
                        0.54,
                    balancedAccuracy =
                        0.85,
                )

            val report =
                NutritionMatcherModelComparisonReport(
                    version =
                        1,
                    datasetFile =
                        "nutrition.matcher-training-dataset.json",
                    baselineFeatureCount =
                        baselineSnapshot.featureNames.size,
                    extendedFeatureCount =
                        extendedSnapshot.featureNames.size,
                    baseline =
                        baselineSnapshot,
                    extended =
                        extendedSnapshot,
                    extendedDelta =
                        NutritionMatcherModelMetricDelta(
                            precision =
                                -0.02,
                            recall =
                                0.0,
                            f1 =
                                -0.03,
                            balancedAccuracy =
                                -0.01,
                            averageLogLoss =
                                0.01,
                            falsePositiveCount =
                                3,
                            falseNegativeCount =
                                0,
                        ),
                    singleFeatureComparisons =
                        emptyList(),
                    recommendation =
                        NutritionMatcherModelRecommendation(
                            recommendedModel =
                                NutritionMatcherRecommendedModel.BASELINE,
                            recommendedFeatureNames =
                                LocalNutritionMatcherFeatureExtractor
                                    .BASE_FEATURE_NAMES,
                            reason =
                                "Baseline retained.",
                            baselineDominatesPrimaryMetrics =
                                true,
                            extendedDominatesPrimaryMetrics =
                                false,
                        ),
                )

            NutritionMatcherModelComparisonReportWriter()
                .write(
                    report =
                        report,
                    outputFile =
                        outputFile,
                )

            assertTrue(
                actual =
                    outputFile.isFile,
                message =
                    "Comparison report was not written.",
            )

            assertTrue(
                actual =
                    outputFile.readText().endsWith("\n"),
                message =
                    "Comparison report must end with a newline.",
            )

            val persisted =
                Gson()
                    .fromJson(
                        outputFile.readText(),
                        NutritionMatcherModelComparisonReport::class.java,
                    )

            assertEquals(
                expected =
                    report,
                actual =
                    persisted,
            )
        } finally {
            temporaryDirectory.deleteRecursively()
        }
    }

    private fun snapshot(
        featureNames: List<String>,
        f1: Double,
        balancedAccuracy: Double,
    ): NutritionMatcherModelComparisonSnapshot {

        return NutritionMatcherModelComparisonSnapshot(
            featureNames =
                featureNames,
            trainingExampleCount =
                100,
            testExampleCount =
                20,
            testPrecision =
                0.5,
            testRecall =
                0.8,
            testF1 =
                f1,
            testBalancedAccuracy =
                balancedAccuracy,
            testAverageLogLoss =
                0.4,
            testFalsePositiveCount =
                4,
            testFalseNegativeCount =
                2,
            testByRole =
                emptyList(),
        )
    }
}