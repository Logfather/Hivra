package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureExtractor
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionMatcherModelComparator
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionMatcherModelComparisonReport
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionMatcherRecommendedModel
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunNutritionMatcherModelComparisonTest {

    @Test
    fun compareBaselineAndDomainMismatchModelsDeterministically() {

        val projectRoot =
            File("..")

        val datasetFile =
            File(
                projectRoot,
                "data/generated/knowledge/" +
                        "training/" +
                        "nutrition.matcher-training-dataset.json",
            )

        val outputDirectory =
            File(
                projectRoot,
                "data/generated/knowledge/" +
                        "models/comparison/",
            )

        val reportFile =
            File(
                projectRoot,
                "data/generated/knowledge/" +
                        "reports/" +
                        "nutrition.local-matcher-feature-comparison.json",
            )

        require(datasetFile.isFile) {
            "Nutrition matcher training dataset does not exist: " +
                    datasetFile.absolutePath
        }

        val report =
            NutritionMatcherModelComparator()
                .compare(
                    datasetFile =
                        datasetFile,
                    outputDirectory =
                        outputDirectory,
                    reportFile =
                        reportFile,
                )

        assertTrue(
            actual =
                reportFile.isFile,
            message =
                "Nutrition matcher comparison report was not written.",
        )

        assertTrue(
            actual =
                reportFile.length() > 0L,
            message =
                "Nutrition matcher comparison report is empty.",
        )

        assertEquals(
            expected =
                1,
            actual =
                report.version,
        )

        assertEquals(
            expected =
                datasetFile.name,
            actual =
                report.datasetFile,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureExtractor
                    .BASE_FEATURE_COUNT,
            actual =
                report.baselineFeatureCount,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureExtractor
                    .FEATURE_COUNT,
            actual =
                report.extendedFeatureCount,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureExtractor
                    .BASE_FEATURE_NAMES,
            actual =
                report.baseline.featureNames,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureExtractor
                    .BASE_FEATURE_NAMES +
                        LocalNutritionMatcherFeatureExtractor
                            .DOMAIN_MISMATCH_FEATURE_NAMES,
            actual =
                report.extended.featureNames,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureExtractor
                    .BASE_FEATURE_COUNT,
            actual =
                report.baseline.featureNames.size,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureExtractor
                    .FEATURE_COUNT,
            actual =
                report.extended.featureNames.size,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureExtractor
                    .DOMAIN_MISMATCH_FEATURE_COUNT,
            actual =
                report.singleFeatureComparisons.size,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureExtractor
                    .DOMAIN_MISMATCH_FEATURE_NAMES,
            actual =
                report.singleFeatureComparisons
                    .map {
                        it.featureName
                    },
        )

        assertEquals(
            expected =
                report.baseline.trainingExampleCount,
            actual =
                report.extended.trainingExampleCount,
        )

        assertEquals(
            expected =
                report.baseline.testExampleCount,
            actual =
                report.extended.testExampleCount,
        )

        report.singleFeatureComparisons
            .forEach { comparison ->

                assertEquals(
                    expected =
                        LocalNutritionMatcherFeatureExtractor
                            .BASE_FEATURE_COUNT + 1,
                    actual =
                        comparison.featureCount,
                    message =
                        "Unexpected feature count for " +
                                comparison.featureName,
                )

                assertTrue(
                    actual =
                        comparison.featureName in
                                LocalNutritionMatcherFeatureExtractor
                                    .DOMAIN_MISMATCH_FEATURE_NAMES,
                    message =
                        "Unknown Domain-Mismatch feature: " +
                                comparison.featureName,
                )

                assertTrue(
                    actual =
                        comparison.testPrecision.isFinite(),
                    message =
                        "Non-finite precision for " +
                                comparison.featureName,
                )

                assertTrue(
                    actual =
                        comparison.testRecall.isFinite(),
                    message =
                        "Non-finite recall for " +
                                comparison.featureName,
                )

                assertTrue(
                    actual =
                        comparison.testF1.isFinite(),
                    message =
                        "Non-finite F1 for " +
                                comparison.featureName,
                )

                assertTrue(
                    actual =
                        comparison.testBalancedAccuracy.isFinite(),
                    message =
                        "Non-finite balanced accuracy for " +
                                comparison.featureName,
                )

                assertTrue(
                    actual =
                        comparison.testAverageLogLoss.isFinite(),
                    message =
                        "Non-finite log loss for " +
                                comparison.featureName,
                )

                assertTrue(
                    actual =
                        comparison.delta.precision.isFinite(),
                    message =
                        "Non-finite precision delta for " +
                                comparison.featureName,
                )

                assertTrue(
                    actual =
                        comparison.delta.recall.isFinite(),
                    message =
                        "Non-finite recall delta for " +
                                comparison.featureName,
                )

                assertTrue(
                    actual =
                        comparison.delta.f1.isFinite(),
                    message =
                        "Non-finite F1 delta for " +
                                comparison.featureName,
                )

                assertTrue(
                    actual =
                        comparison.delta
                            .balancedAccuracy
                            .isFinite(),
                    message =
                        "Non-finite balanced accuracy delta for " +
                                comparison.featureName,
                )

                assertTrue(
                    actual =
                        comparison.delta
                            .averageLogLoss
                            .isFinite(),
                    message =
                        "Non-finite log-loss delta for " +
                                comparison.featureName,
                )
            }

        assertEquals(
            expected =
                NutritionMatcherRecommendedModel.BASELINE,
            actual =
                report.recommendation.recommendedModel,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureExtractor
                    .BASE_FEATURE_NAMES,
            actual =
                report.recommendation
                    .recommendedFeatureNames,
        )

        assertTrue(
            actual =
                report.recommendation.reason.isNotBlank(),
            message =
                "Nutrition matcher recommendation reason is blank.",
        )

        assertTrue(
            actual =
                !report.recommendation
                    .extendedDominatesPrimaryMetrics,
            message =
                "Extended model must not dominate both primary " +
                        "metrics for the current fixture.",
        )

        assertTrue(
            actual =
                report.extendedDelta.f1 <= 0.0,
            message =
                "Extended model unexpectedly improves test F1.",
        )

        assertTrue(
            actual =
                report.extendedDelta
                    .balancedAccuracy <= 0.0,
            message =
                "Extended model unexpectedly improves test " +
                        "balanced accuracy.",
        )

        val persistedReport =
            GsonBuilder()
                .create()
                .fromJson(
                    reportFile.readText(),
                    NutritionMatcherModelComparisonReport::class.java,
                )

        assertEquals(
            expected =
                report,
            actual =
                persistedReport,
            message =
                "Persisted Nutrition matcher comparison report " +
                        "differs from the returned report.",
        )

        assertEquals(
            expected =
                NutritionMatcherRecommendedModel.BASELINE,
            actual =
                persistedReport
                    .recommendation
                    .recommendedModel,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureExtractor
                    .BASE_FEATURE_NAMES,
            actual =
                persistedReport
                    .recommendation
                    .recommendedFeatureNames,
        )
    }
}