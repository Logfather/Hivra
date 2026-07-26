package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureContract
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionFeatureOptimizationClassification
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionFeatureOptimizationEntry
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionFeatureOptimizationEvaluator
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionFeatureOptimizationReport
import kotlin.test.Test
import kotlin.test.assertEquals

class NutritionFeatureOptimizationEvaluatorTest {

    @Test
    fun evaluateFeatureOptimizationReportDeterministically() {

        val optimizableFeatureNames =
            LocalNutritionMatcherFeatureContract
                .OPTIMIZABLE_FEATURE_NAMES

        val harmfulFeatureName =
            optimizableFeatureNames.first()

        val entries =
            optimizableFeatureNames
                .map { featureName ->
                    val harmful =
                        featureName ==
                                harmfulFeatureName

                    NutritionFeatureOptimizationEntry(
                        featureName =
                            featureName,
                        baselineFeatureCount =
                            LocalNutritionMatcherFeatureContract
                                .ACTIVE_FEATURE_COUNT,
                        ablatedFeatureCount =
                            LocalNutritionMatcherFeatureContract
                                .ACTIVE_FEATURE_COUNT - 1,
                        baselineTestPrecision =
                            0.40,
                        ablatedTestPrecision =
                            if (harmful) {
                                0.42
                            } else {
                                0.40
                            },
                        precisionDelta =
                            if (harmful) {
                                0.02
                            } else {
                                0.0
                            },
                        baselineTestRecall =
                            0.87,
                        ablatedTestRecall =
                            0.87,
                        recallDelta =
                            0.0,
                        baselineTestF1 =
                            0.55,
                        ablatedTestF1 =
                            if (harmful) {
                                0.57
                            } else {
                                0.55
                            },
                        f1Delta =
                            if (harmful) {
                                0.02
                            } else {
                                0.0
                            },
                        baselineTestBalancedAccuracy =
                            0.86,
                        ablatedTestBalancedAccuracy =
                            0.86,
                        balancedAccuracyDelta =
                            0.0,
                        classification =
                            if (harmful) {
                                NutritionFeatureOptimizationClassification
                                    .HARMFUL
                            } else {
                                NutritionFeatureOptimizationClassification
                                    .NEUTRAL
                            },
                    )
                }
                .sortedBy { entry ->
                    entry.featureName
                }

        val expectedRecommendedFeatureNames =
            LocalNutritionMatcherFeatureContract
                .ACTIVE_FEATURE_NAMES
                .filterNot { featureName ->
                    featureName ==
                            harmfulFeatureName
                }

        val report =
            NutritionFeatureOptimizationReport(
                version =
                    1,
                baselineFeatureNames =
                    LocalNutritionMatcherFeatureContract
                        .ACTIVE_FEATURE_NAMES,
                baselineFeatureCount =
                    LocalNutritionMatcherFeatureContract
                        .ACTIVE_FEATURE_COUNT,
                baselineTestPrecision =
                    0.40,
                baselineTestRecall =
                    0.87,
                baselineTestF1 =
                    0.55,
                baselineTestBalancedAccuracy =
                    0.86,
                requiredFeatureNames =
                    emptyList(),
                neutralFeatureNames =
                    optimizableFeatureNames
                        .filterNot { featureName ->
                            featureName ==
                                    harmfulFeatureName
                        }
                        .sorted(),
                harmfulFeatureNames =
                    listOf(
                        harmfulFeatureName,
                    ),
                recommendedFeatureNames =
                    expectedRecommendedFeatureNames,
                entries =
                    entries,
            )

        val evaluation =
            NutritionFeatureOptimizationEvaluator()
                .evaluate(
                    report =
                        report,
                )

        assertEquals(
            expected =
                1,
            actual =
                evaluation.removedFeatureCount,
        )

        assertEquals(
            expected =
                listOf(
                    harmfulFeatureName,
                ),
            actual =
                evaluation.removedFeatureNames,
        )

        assertEquals(
            expected =
                expectedRecommendedFeatureNames,
            actual =
                evaluation.recommendedFeatureNames,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_FEATURE_COUNT - 1,
            actual =
                evaluation.recommendedFeatureCount,
        )

        assertEquals(
            expected =
                1,
            actual =
                evaluation
                    .classificationCounts
                    .harmful,
        )

        assertEquals(
            expected =
                optimizableFeatureNames.size - 1,
            actual =
                evaluation
                    .classificationCounts
                    .neutral,
        )

        assertEquals(
            expected =
                0,
            actual =
                evaluation
                    .classificationCounts
                    .required,
        )
    }
}