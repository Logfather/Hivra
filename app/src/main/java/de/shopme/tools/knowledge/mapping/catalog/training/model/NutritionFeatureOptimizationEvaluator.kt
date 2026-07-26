package de.shopme.tools.knowledge.mapping.catalog.training.model

import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureContract
class NutritionFeatureOptimizationEvaluator {

    fun evaluate(
        report: NutritionFeatureOptimizationReport,
    ): NutritionFeatureOptimizationEvaluation {

        validateReport(
            report =
                report,
        )

        val evaluatedEntries =
            report.entries
                .map { entry ->
                    NutritionFeatureOptimizationFeatureEvaluation(
                        featureName =
                            entry.featureName,
                        classification =
                            entry.classification,
                        precisionDelta =
                            entry.precisionDelta,
                        recallDelta =
                            entry.recallDelta,
                        f1Delta =
                            entry.f1Delta,
                        balancedAccuracyDelta =
                            entry.balancedAccuracyDelta,
                    )
                }
                .sortedBy { entry ->
                    entry.featureName
                }

        val requiredFeatures =
            evaluatedEntries
                .filter { entry ->
                    entry.classification ==
                            NutritionFeatureOptimizationClassification
                                .REQUIRED
                }

        val neutralFeatures =
            evaluatedEntries
                .filter { entry ->
                    entry.classification ==
                            NutritionFeatureOptimizationClassification
                                .NEUTRAL
                }

        val harmfulFeatures =
            evaluatedEntries
                .filter { entry ->
                    entry.classification ==
                            NutritionFeatureOptimizationClassification
                                .HARMFUL
                }

        val removedFeatureNames =
            harmfulFeatures
                .map { entry ->
                    entry.featureName
                }
                .sorted()

        val recommendedFeatureNames =
            report.baselineFeatureNames
                .filterNot { featureName ->
                    featureName in removedFeatureNames
                }

        require(
            recommendedFeatureNames ==
                    report.recommendedFeatureNames,
        ) {
            "Evaluated nutrition matcher feature recommendation " +
                    "differs from the optimization report."
        }

        require(
            recommendedFeatureNames.take(
                LocalNutritionMatcherFeatureContract
                    .BASE_FEATURE_COUNT,
            ) ==
                    LocalNutritionMatcherFeatureContract
                        .BASE_FEATURE_NAMES,
        ) {
            "Recommended nutrition matcher feature contract must " +
                    "preserve every base feature."
        }

        return NutritionFeatureOptimizationEvaluation(
            version =
                EVALUATION_VERSION,
            baseline =
                NutritionFeatureOptimizationBaselineEvaluation(
                    featureNames =
                        report.baselineFeatureNames,
                    featureCount =
                        report.baselineFeatureCount,
                    testPrecision =
                        report.baselineTestPrecision,
                    testRecall =
                        report.baselineTestRecall,
                    testF1 =
                        report.baselineTestF1,
                    testBalancedAccuracy =
                        report.baselineTestBalancedAccuracy,
                ),
            classificationCounts =
                NutritionFeatureOptimizationClassificationCounts(
                    required =
                        requiredFeatures.size,
                    neutral =
                        neutralFeatures.size,
                    harmful =
                        harmfulFeatures.size,
                ),
            requiredFeatures =
                requiredFeatures,
            neutralFeatures =
                neutralFeatures,
            harmfulFeatures =
                harmfulFeatures,
            recommendedFeatureNames =
                recommendedFeatureNames,
            recommendedFeatureCount =
                recommendedFeatureNames.size,
            removedFeatureNames =
                removedFeatureNames,
            removedFeatureCount =
                removedFeatureNames.size,
        )
    }

    private fun validateReport(
        report: NutritionFeatureOptimizationReport,
    ) {
        require(
            report.baselineFeatureNames ==
                    LocalNutritionMatcherFeatureContract
                        .ACTIVE_FEATURE_NAMES,
        ) {
            "Nutrition feature optimization report does not use " +
                    "the current active feature contract."
        }

        require(
            report.baselineFeatureCount ==
                    report.baselineFeatureNames.size,
        ) {
            "Nutrition feature optimization baseline feature count " +
                    "does not match its feature names."
        }

        require(
            report.baselineFeatureCount ==
                    LocalNutritionMatcherFeatureContract
                        .ACTIVE_FEATURE_COUNT,
        ) {
            "Nutrition feature optimization report has an unexpected " +
                    "baseline feature count: " +
                    report.baselineFeatureCount
        }

        require(
            report.entries.size ==
                    LocalNutritionMatcherFeatureContract
                        .OPTIMIZABLE_FEATURE_NAMES
                        .size,
        ) {
            "Nutrition feature optimization report does not contain " +
                    "one entry per optimizable feature."
        }

        require(
            report.entries
                .map { entry ->
                    entry.featureName
                }
                .sorted() ==
                    LocalNutritionMatcherFeatureContract
                        .OPTIMIZABLE_FEATURE_NAMES
                        .sorted(),
        ) {
            "Nutrition feature optimization report contains an " +
                    "unexpected set of optimized features."
        }

        require(
            report.entries
                .map { entry ->
                    entry.featureName
                }
                .distinct()
                .size ==
                    report.entries.size,
        ) {
            "Nutrition feature optimization report contains duplicate " +
                    "feature evaluations."
        }

        require(
            report.entries.all { entry ->
                entry.featureName !in
                        LocalNutritionMatcherFeatureContract
                            .BASE_FEATURE_NAMES
            },
        ) {
            "Nutrition feature optimization report must not contain " +
                    "base-feature ablations."
        }

        require(
            report.entries.all { entry ->
                entry.precisionDelta.isFinite() &&
                        entry.recallDelta.isFinite() &&
                        entry.f1Delta.isFinite() &&
                        entry.balancedAccuracyDelta.isFinite()
            },
        ) {
            "Nutrition feature optimization report contains " +
                    "non-finite metric deltas."
        }

        require(
            report.baselineTestPrecision.isFinite() &&
                    report.baselineTestRecall.isFinite() &&
                    report.baselineTestF1.isFinite() &&
                    report.baselineTestBalancedAccuracy.isFinite(),
        ) {
            "Nutrition feature optimization baseline contains " +
                    "non-finite metrics."
        }
    }

    private companion object {

        const val EVALUATION_VERSION =
            1
    }
}