package de.shopme.tools.knowledge.mapping.catalog.training.model

import com.google.gson.GsonBuilder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.Locale

class NutritionMatcherModelComparator {

    fun compare(
        datasetFile: File,
        outputDirectory: File,
        reportFile: File,
        output: PrintStream = System.out,
    ): NutritionMatcherModelComparisonReport {

        require(datasetFile.isFile) {
            "Nutrition matcher training dataset does not exist: " +
                    datasetFile.absolutePath
        }

        ensureDirectory(
            directory =
                outputDirectory,
        )

        /*
         * Der vollständige Extractor bleibt die technische Quelle
         * sämtlicher extrahierbarer Featurewerte.
         *
         * Welche Features für das Training zulässig und aktiv sind,
         * bestimmt ausschließlich der Feature-Contract.
         */
        val completeExtractor =
            LocalNutritionMatcherFeatureExtractor()

        val baselineFeatureNames =
            LocalNutritionMatcherFeatureContract
                .BASE_FEATURE_NAMES

        val activeDomainFeatureNames =
            LocalNutritionMatcherFeatureContract
                .ACTIVE_DOMAIN_FEATURE_NAMES

        val activeFeatureNames =
            LocalNutritionMatcherFeatureContract
                .ACTIVE_FEATURE_NAMES

        require(
            baselineFeatureNames.size ==
                    LocalNutritionMatcherFeatureContract
                        .BASE_FEATURE_COUNT,
        ) {
            "Baseline feature count differs from its declared contract."
        }

        require(
            activeDomainFeatureNames.size ==
                    LocalNutritionMatcherFeatureContract
                        .ACTIVE_DOMAIN_FEATURE_COUNT,
        ) {
            "Active Domain-Mismatch feature count differs from its " +
                    "declared contract."
        }

        require(
            baselineFeatureNames +
                    activeDomainFeatureNames ==
                    activeFeatureNames,
        ) {
            "Active feature order does not equal base plus active " +
                    "Domain-Mismatch features."
        }

        require(
            activeFeatureNames.size ==
                    LocalNutritionMatcherFeatureContract
                        .ACTIVE_FEATURE_COUNT,
        ) {
            "Active feature count differs from its declared contract."
        }

        require(
            baselineFeatureNames.all { featureName ->
                featureName in completeExtractor.featureNames
            },
        ) {
            "The complete Nutrition matcher extractor does not expose " +
                    "every baseline feature."
        }

        require(
            activeDomainFeatureNames.all { featureName ->
                featureName in completeExtractor.featureNames
            },
        ) {
            "The complete Nutrition matcher extractor does not expose " +
                    "every active Domain-Mismatch feature."
        }

        require(
            activeFeatureNames.all { featureName ->
                featureName in completeExtractor.featureNames
            },
        ) {
            "The complete Nutrition matcher extractor does not expose " +
                    "the complete active production feature contract."
        }

        val baselineModel =
            train(
                datasetFile =
                    datasetFile,
                outputFile =
                    File(
                        outputDirectory,
                        BASELINE_MODEL_FILE_NAME,
                    ),
                featureNames =
                    baselineFeatureNames,
                completeExtractor =
                    completeExtractor,
            )

        val extendedModel =
            train(
                datasetFile =
                    datasetFile,
                outputFile =
                    File(
                        outputDirectory,
                        EXTENDED_MODEL_FILE_NAME,
                    ),
                featureNames =
                    activeFeatureNames,
                completeExtractor =
                    completeExtractor,
            )

        requireSameSplit(
            baseline =
                baselineModel,
            candidate =
                extendedModel,
        )

        val baselineSnapshot =
            baselineModel.toSnapshot()

        val extendedSnapshot =
            extendedModel.toSnapshot()

        val extendedDelta =
            delta(
                baseline =
                    baselineModel,
                candidate =
                    extendedModel,
            )

        val singleFeatureComparisons =
            activeDomainFeatureNames
                .map { activeDomainFeatureName ->

                    val candidateFeatureNames =
                        baselineFeatureNames +
                                activeDomainFeatureName

                    val candidateModel =
                        train(
                            datasetFile =
                                datasetFile,
                            outputFile =
                                File(
                                    outputDirectory,
                                    "nutrition.local-matcher-" +
                                            sanitize(
                                                value =
                                                    activeDomainFeatureName,
                                            ) +
                                            ".json",
                                ),
                            featureNames =
                                candidateFeatureNames,
                            completeExtractor =
                                completeExtractor,
                        )

                    requireSameSplit(
                        baseline =
                            baselineModel,
                        candidate =
                            candidateModel,
                    )

                    candidateModel.toFeatureComparison(
                        featureName =
                            activeDomainFeatureName,
                        baseline =
                            baselineModel,
                    )
                }

        val recommendation =
            recommend(
                baseline =
                    baselineModel,
                extended =
                    extendedModel,
            )

        val report =
            NutritionMatcherModelComparisonReport(
                version =
                    REPORT_VERSION,
                datasetFile =
                    datasetFile.name,
                baselineFeatureCount =
                    baselineModel.featureNames.size,
                extendedFeatureCount =
                    extendedModel.featureNames.size,
                baseline =
                    baselineSnapshot,
                extended =
                    extendedSnapshot,
                extendedDelta =
                    extendedDelta,
                singleFeatureComparisons =
                    singleFeatureComparisons,
                recommendation =
                    recommendation,
            )

        validateReport(
            report =
                report,
        )

        writeReport(
            report =
                report,
            reportFile =
                reportFile,
        )

        printReport(
            report =
                report,
            reportFile =
                reportFile,
            output =
                output,
        )

        return report
    }

    private fun train(
        datasetFile: File,
        outputFile: File,
        featureNames: List<String>,
        completeExtractor:
        LocalNutritionMatcherFeatureProvider,
    ): LocalNutritionMatcherModel {

        LocalNutritionMatcherFeatureContract
            .validateTrainingFeatureSubset(
                featureNames =
                    featureNames,
            )

        val extractor =
            LocalNutritionMatcherFeatureSubsetExtractor(
                delegate =
                    completeExtractor,
                selectedFeatureNames =
                    featureNames,
            )

        val suppressedOutput =
            PrintStream(
                ByteArrayOutputStream(),
            )

        return suppressedOutput.use { silentOutput ->

            val model =
                LocalNutritionMatcherModelTrainer(
                    featureExtractor =
                        extractor,
                    supportedFeatureNames =
                        featureNames,
                )
                    .train(
                        datasetFile =
                            datasetFile,
                        outputFile =
                            outputFile,
                        output =
                            silentOutput,
                    )
                    .model

            require(
                model.featureNames ==
                        featureNames,
            ) {
                "Trained Nutrition matcher feature contract differs " +
                        "from the requested feature subset. Expected: " +
                        featureNames.joinToString() +
                        "; actual: " +
                        model.featureNames.joinToString()
            }

            model
        }
    }

    private fun recommend(
        baseline: LocalNutritionMatcherModel,
        extended: LocalNutritionMatcherModel,
    ): NutritionMatcherModelRecommendation {

        val baselineMetrics =
            baseline.metrics.test

        val extendedMetrics =
            extended.metrics.test

        val baselineF1AtLeastAsGood =
            baselineMetrics.f1 >=
                    extendedMetrics.f1 - EPSILON

        val baselineBalancedAccuracyAtLeastAsGood =
            baselineMetrics.balancedAccuracy >=
                    extendedMetrics.balancedAccuracy - EPSILON

        val extendedF1StrictlyBetter =
            extendedMetrics.f1 >
                    baselineMetrics.f1 + EPSILON

        val extendedBalancedAccuracyStrictlyBetter =
            extendedMetrics.balancedAccuracy >
                    baselineMetrics.balancedAccuracy + EPSILON

        val baselineDominatesPrimaryMetrics =
            baselineF1AtLeastAsGood &&
                    baselineBalancedAccuracyAtLeastAsGood

        val extendedDominatesPrimaryMetrics =
            extendedF1StrictlyBetter &&
                    extendedBalancedAccuracyStrictlyBetter

        return if (extendedDominatesPrimaryMetrics) {

            NutritionMatcherModelRecommendation(
                recommendedModel =
                    NutritionMatcherRecommendedModel.EXTENDED,
                recommendedFeatureNames =
                    extended.featureNames,
                reason =
                    "The active production model improves both test F1 " +
                            "and test balanced accuracy compared with the " +
                            "base-feature model.",
                baselineDominatesPrimaryMetrics =
                    false,
                extendedDominatesPrimaryMetrics =
                    true,
            )

        } else {

            val reason =
                when {
                    baselineDominatesPrimaryMetrics -> {
                        "The active production model does not improve " +
                                "test F1 or test balanced accuracy. The " +
                                "smaller base-feature model is retained."
                    }

                    extendedF1StrictlyBetter -> {
                        "The active production model improves test F1 " +
                                "but not test balanced accuracy. The smaller " +
                                "base-feature model is retained."
                    }

                    extendedBalancedAccuracyStrictlyBetter -> {
                        "The active production model improves test " +
                                "balanced accuracy but not test F1. The " +
                                "smaller base-feature model is retained."
                    }

                    else -> {
                        "The active production model provides no " +
                                "unambiguous improvement in both primary " +
                                "metrics. The smaller base-feature model " +
                                "is retained."
                    }
                }

            NutritionMatcherModelRecommendation(
                recommendedModel =
                    NutritionMatcherRecommendedModel.BASELINE,
                recommendedFeatureNames =
                    baseline.featureNames,
                reason =
                    reason,
                baselineDominatesPrimaryMetrics =
                    baselineDominatesPrimaryMetrics,
                extendedDominatesPrimaryMetrics =
                    false,
            )
        }
    }

    private fun validateReport(
        report: NutritionMatcherModelComparisonReport,
    ) {
        require(
            report.version ==
                    REPORT_VERSION,
        ) {
            "Unexpected Nutrition matcher comparison report version: " +
                    report.version
        }

        require(
            report.datasetFile.isNotBlank(),
        ) {
            "Nutrition matcher comparison dataset file must not be blank."
        }

        require(
            report.baselineFeatureCount ==
                    report.baseline.featureNames.size,
        ) {
            "Baseline feature count does not match the persisted " +
                    "baseline feature list."
        }

        require(
            report.extendedFeatureCount ==
                    report.extended.featureNames.size,
        ) {
            "Extended feature count does not match the persisted " +
                    "extended feature list."
        }

        require(
            report.baselineFeatureCount ==
                    LocalNutritionMatcherFeatureContract
                        .BASE_FEATURE_COUNT,
        ) {
            "Unexpected baseline Nutrition matcher feature count."
        }

        require(
            report.extendedFeatureCount ==
                    LocalNutritionMatcherFeatureContract
                        .ACTIVE_FEATURE_COUNT,
        ) {
            "Unexpected extended Nutrition matcher feature count."
        }

        require(
            report.baseline.featureNames ==
                    LocalNutritionMatcherFeatureContract
                        .BASE_FEATURE_NAMES,
        ) {
            "Baseline comparison model does not use the declared " +
                    "base-feature contract."
        }

        require(
            report.extended.featureNames ==
                    LocalNutritionMatcherFeatureContract
                        .ACTIVE_FEATURE_NAMES,
        ) {
            "Extended comparison model does not use the active " +
                    "production feature contract."
        }

        require(
            report.singleFeatureComparisons.size ==
                    LocalNutritionMatcherFeatureContract
                        .ACTIVE_DOMAIN_FEATURE_COUNT,
        ) {
            "Unexpected number of active single Domain-Mismatch " +
                    "feature comparisons."
        }

        require(
            report.singleFeatureComparisons
                .map { comparison ->
                    comparison.featureName
                } ==
                    LocalNutritionMatcherFeatureContract
                        .ACTIVE_DOMAIN_FEATURE_NAMES,
        ) {
            "Single Domain-Mismatch feature comparisons do not " +
                    "follow the active deterministic feature order."
        }

        require(
            report.singleFeatureComparisons.none { comparison ->
                comparison.featureName in
                        LocalNutritionMatcherFeatureContract
                            .HARMFUL_DOMAIN_FEATURE_NAMES
            },
        ) {
            "Single-feature comparisons contain a harmful " +
                    "Domain-Mismatch feature."
        }

        require(
            report.baseline.trainingExampleCount ==
                    report.extended.trainingExampleCount,
        ) {
            "Baseline and extended comparison models use different " +
                    "training example counts."
        }

        require(
            report.baseline.testExampleCount ==
                    report.extended.testExampleCount,
        ) {
            "Baseline and extended comparison models use different " +
                    "test example counts."
        }

        require(
            report.recommendation.recommendedFeatureNames ==
                    when (
                        report.recommendation.recommendedModel
                    ) {
                        NutritionMatcherRecommendedModel.BASELINE -> {
                            report.baseline.featureNames
                        }

                        NutritionMatcherRecommendedModel.EXTENDED -> {
                            report.extended.featureNames
                        }
                    },
        ) {
            "Recommended feature names do not match the recommended model."
        }

        require(
            report.recommendation.reason.isNotBlank(),
        ) {
            "Nutrition matcher model recommendation reason is blank."
        }

        validateSnapshot(
            name =
                "baseline",
            snapshot =
                report.baseline,
        )

        validateSnapshot(
            name =
                "extended",
            snapshot =
                report.extended,
        )

        validateDelta(
            name =
                "extended",
            delta =
                report.extendedDelta,
        )

        report.singleFeatureComparisons
            .forEach { comparison ->

                require(
                    comparison.featureName in
                            LocalNutritionMatcherFeatureContract
                                .ACTIVE_DOMAIN_FEATURE_NAMES,
                ) {
                    "Unknown active Domain-Mismatch comparison " +
                            "feature: ${comparison.featureName}"
                }

                require(
                    comparison.featureName !in
                            LocalNutritionMatcherFeatureContract
                                .HARMFUL_DOMAIN_FEATURE_NAMES,
                ) {
                    "Harmful Domain-Mismatch feature must not be " +
                            "trained: ${comparison.featureName}"
                }

                require(
                    comparison.featureCount ==
                            LocalNutritionMatcherFeatureContract
                                .BASE_FEATURE_COUNT + 1,
                ) {
                    "Unexpected feature count for " +
                            comparison.featureName +
                            ": ${comparison.featureCount}"
                }

                require(
                    comparison.testPrecision.isFinite() &&
                            comparison.testRecall.isFinite() &&
                            comparison.testF1.isFinite() &&
                            comparison.testBalancedAccuracy.isFinite() &&
                            comparison.testAverageLogLoss.isFinite(),
                ) {
                    "Non-finite test metric for " +
                            comparison.featureName
                }

                validateDelta(
                    name =
                        comparison.featureName,
                    delta =
                        comparison.delta,
                )
            }
    }

    private fun validateSnapshot(
        name: String,
        snapshot:
        NutritionMatcherModelComparisonSnapshot,
    ) {
        require(
            snapshot.trainingExampleCount > 0,
        ) {
            "$name model has no training examples."
        }

        require(
            snapshot.testExampleCount > 0,
        ) {
            "$name model has no test examples."
        }

        require(
            snapshot.testPrecision.isFinite() &&
                    snapshot.testRecall.isFinite() &&
                    snapshot.testF1.isFinite() &&
                    snapshot.testBalancedAccuracy.isFinite() &&
                    snapshot.testAverageLogLoss.isFinite(),
        ) {
            "$name model contains non-finite test metrics."
        }
    }

    private fun validateDelta(
        name: String,
        delta: NutritionMatcherModelMetricDelta,
    ) {
        require(
            delta.precision.isFinite() &&
                    delta.recall.isFinite() &&
                    delta.f1.isFinite() &&
                    delta.balancedAccuracy.isFinite() &&
                    delta.averageLogLoss.isFinite(),
        ) {
            "$name comparison contains non-finite metric deltas."
        }
    }

    private fun requireSameSplit(
        baseline: LocalNutritionMatcherModel,
        candidate: LocalNutritionMatcherModel,
    ) {
        require(
            baseline.training.exampleCount ==
                    candidate.training.exampleCount,
        ) {
            "Compared models use different total example counts."
        }

        require(
            baseline.training.trainingExampleCount ==
                    candidate.training.trainingExampleCount,
        ) {
            "Compared models use different training example counts."
        }

        require(
            baseline.training.testExampleCount ==
                    candidate.training.testExampleCount,
        ) {
            "Compared models use different test example counts."
        }

        require(
            baseline.training.trainingCatalogKeyCount ==
                    candidate.training.trainingCatalogKeyCount,
        ) {
            "Compared models use different training catalog-key counts."
        }

        require(
            baseline.training.testCatalogKeyCount ==
                    candidate.training.testCatalogKeyCount,
        ) {
            "Compared models use different test catalog-key counts."
        }

        require(
            baseline.training.splitModulo ==
                    candidate.training.splitModulo,
        ) {
            "Compared models use different split moduli."
        }

        require(
            baseline.training.testBuckets ==
                    candidate.training.testBuckets,
        ) {
            "Compared models use different test buckets."
        }

        require(
            baseline.training.positiveClassWeight ==
                    candidate.training.positiveClassWeight,
        ) {
            "Compared models use different positive class weights."
        }

        require(
            baseline.training.negativeClassWeight ==
                    candidate.training.negativeClassWeight,
        ) {
            "Compared models use different negative class weights."
        }

        require(
            baseline.training.learningRate ==
                    candidate.training.learningRate,
        ) {
            "Compared models use different learning rates."
        }

        require(
            baseline.training.iterationCount ==
                    candidate.training.iterationCount,
        ) {
            "Compared models use different iteration counts."
        }

        require(
            baseline.training.l2Regularization ==
                    candidate.training.l2Regularization,
        ) {
            "Compared models use different L2 regularization."
        }
    }

    private fun LocalNutritionMatcherModel.toSnapshot():
            NutritionMatcherModelComparisonSnapshot {

        val test =
            metrics.test

        return NutritionMatcherModelComparisonSnapshot(
            featureNames =
                featureNames,
            trainingExampleCount =
                training.trainingExampleCount,
            testExampleCount =
                training.testExampleCount,
            testPrecision =
                test.precision,
            testRecall =
                test.recall,
            testF1 =
                test.f1,
            testBalancedAccuracy =
                test.balancedAccuracy,
            testAverageLogLoss =
                test.averageLogLoss,
            testFalsePositiveCount =
                test.falsePositive,
            testFalseNegativeCount =
                test.falseNegative,
            testByRole =
                metrics.testByRole,
        )
    }

    private fun LocalNutritionMatcherModel.toFeatureComparison(
        featureName: String,
        baseline: LocalNutritionMatcherModel,
    ): NutritionDomainFeatureComparison {

        val metricDelta =
            delta(
                baseline =
                    baseline,
                candidate =
                    this,
            )

        return NutritionDomainFeatureComparison(
            featureName =
                featureName,
            featureCount =
                featureNames.size,
            testPrecision =
                metrics.test.precision,
            testRecall =
                metrics.test.recall,
            testF1 =
                metrics.test.f1,
            testBalancedAccuracy =
                metrics.test.balancedAccuracy,
            testAverageLogLoss =
                metrics.test.averageLogLoss,
            delta =
                metricDelta,
            improvesF1 =
                metricDelta.f1 > EPSILON,
            improvesBalancedAccuracy =
                metricDelta.balancedAccuracy > EPSILON,
            improvesBothPrimaryMetrics =
                metricDelta.f1 > EPSILON &&
                        metricDelta.balancedAccuracy > EPSILON,
            roleDeltas =
                roleDeltas(
                    baseline =
                        baseline,
                    candidate =
                        this,
                ),
        )
    }

    private fun delta(
        baseline: LocalNutritionMatcherModel,
        candidate: LocalNutritionMatcherModel,
    ): NutritionMatcherModelMetricDelta {

        return NutritionMatcherModelMetricDelta(
            precision =
                candidate.metrics.test.precision -
                        baseline.metrics.test.precision,
            recall =
                candidate.metrics.test.recall -
                        baseline.metrics.test.recall,
            f1 =
                candidate.metrics.test.f1 -
                        baseline.metrics.test.f1,
            balancedAccuracy =
                candidate.metrics.test.balancedAccuracy -
                        baseline.metrics.test.balancedAccuracy,
            averageLogLoss =
                candidate.metrics.test.averageLogLoss -
                        baseline.metrics.test.averageLogLoss,
            falsePositiveCount =
                candidate.metrics.test.falsePositive -
                        baseline.metrics.test.falsePositive,
            falseNegativeCount =
                candidate.metrics.test.falseNegative -
                        baseline.metrics.test.falseNegative,
        )
    }

    private fun roleDeltas(
        baseline: LocalNutritionMatcherModel,
        candidate: LocalNutritionMatcherModel,
    ): List<NutritionMatcherRoleMetricDelta> {

        val baselineByRole =
            baseline.metrics.testByRole
                .associateBy { roleMetrics ->
                    roleMetrics.role
                }

        val candidateByRole =
            candidate.metrics.testByRole
                .associateBy { roleMetrics ->
                    roleMetrics.role
                }

        require(
            baselineByRole.keys ==
                    candidateByRole.keys,
        ) {
            "Role sets differ between compared models."
        }

        return baselineByRole.keys
            .sorted()
            .map { role ->

                val baselineMetrics =
                    requireNotNull(
                        baselineByRole[role],
                    )

                val candidateMetrics =
                    requireNotNull(
                        candidateByRole[role],
                    )

                NutritionMatcherRoleMetricDelta(
                    role =
                        role,
                    baselineFalsePositiveRate =
                        baselineMetrics.falsePositiveRate,
                    candidateFalsePositiveRate =
                        candidateMetrics.falsePositiveRate,
                    falsePositiveRateDelta =
                        candidateMetrics.falsePositiveRate -
                                baselineMetrics.falsePositiveRate,
                    baselineRecall =
                        baselineMetrics.recall,
                    candidateRecall =
                        candidateMetrics.recall,
                    recallDelta =
                        candidateMetrics.recall -
                                baselineMetrics.recall,
                )
            }
    }

    private fun writeReport(
        report: NutritionMatcherModelComparisonReport,
        reportFile: File,
    ) {
        reportFile.parentFile
            ?.let { directory ->

                ensureDirectory(
                    directory =
                        directory,
                )
            }

        val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()

        reportFile.writeText(
            gson.toJson(
                report,
            ) + "\n",
        )

        check(reportFile.isFile) {
            "Nutrition matcher comparison report was not written: " +
                    reportFile.absolutePath
        }

        check(reportFile.length() > 0L) {
            "Nutrition matcher comparison report is empty: " +
                    reportFile.absolutePath
        }
    }

    private fun printReport(
        report: NutritionMatcherModelComparisonReport,
        reportFile: File,
        output: PrintStream,
    ) {
        output.println()
        output.println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        )
        output.println(
            "NUTRITION MATCHER ACTIVE FEATURE COMPARISON",
        )
        output.println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        )

        output.println(
            "Baseline features        : " +
                    report.baselineFeatureCount,
        )

        output.println(
            "Active features          : " +
                    report.extendedFeatureCount,
        )

        output.println()

        output.println(
            "BASELINE test precision  : " +
                    format(
                        value =
                            report.baseline.testPrecision,
                    ),
        )

        output.println(
            "ACTIVE test precision    : " +
                    format(
                        value =
                            report.extended.testPrecision,
                    ),
        )

        output.println(
            "Precision delta          : " +
                    format(
                        value =
                            report.extendedDelta.precision,
                    ),
        )

        output.println()

        output.println(
            "BASELINE test recall     : " +
                    format(
                        value =
                            report.baseline.testRecall,
                    ),
        )

        output.println(
            "ACTIVE test recall       : " +
                    format(
                        value =
                            report.extended.testRecall,
                    ),
        )

        output.println(
            "Recall delta             : " +
                    format(
                        value =
                            report.extendedDelta.recall,
                    ),
        )

        output.println()

        output.println(
            "BASELINE test F1         : " +
                    format(
                        value =
                            report.baseline.testF1,
                    ),
        )

        output.println(
            "ACTIVE test F1           : " +
                    format(
                        value =
                            report.extended.testF1,
                    ),
        )

        output.println(
            "F1 delta                 : " +
                    format(
                        value =
                            report.extendedDelta.f1,
                    ),
        )

        output.println()

        output.println(
            "BASELINE balanced acc.   : " +
                    format(
                        value =
                            report.baseline
                                .testBalancedAccuracy,
                    ),
        )

        output.println(
            "ACTIVE balanced acc.     : " +
                    format(
                        value =
                            report.extended
                                .testBalancedAccuracy,
                    ),
        )

        output.println(
            "Balanced accuracy delta  : " +
                    format(
                        value =
                            report.extendedDelta
                                .balancedAccuracy,
                    ),
        )

        output.println()

        output.println(
            "Recommended model        : " +
                    report.recommendation
                        .recommendedModel,
        )

        output.println(
            "Recommended features     : " +
                    report.recommendation
                        .recommendedFeatureNames
                        .size,
        )

        output.println(
            "Recommendation reason    : " +
                    report.recommendation.reason,
        )

        output.println()

        output.println(
            "SINGLE ACTIVE DOMAIN FEATURE ADDITIONS",
        )

        report.singleFeatureComparisons
            .sortedWith(
                compareByDescending<NutritionDomainFeatureComparison> {
                    it.delta.balancedAccuracy
                }
                    .thenByDescending {
                        it.delta.f1
                    }
                    .thenBy {
                        it.featureName
                    },
            )
            .forEach { comparison ->

                output.println(
                    comparison.featureName.padEnd(42) +
                            " ΔF1=" +
                            format(
                                value =
                                    comparison.delta.f1,
                            ) +
                            " ΔBA=" +
                            format(
                                value =
                                    comparison.delta
                                        .balancedAccuracy,
                            ) +
                            " ΔFP=" +
                            comparison.delta
                                .falsePositiveCount +
                            " ΔFN=" +
                            comparison.delta
                                .falseNegativeCount,
                )
            }

        output.println()

        output.println(
            "Improves both F1 and balanced accuracy:",
        )

        val improving =
            report.singleFeatureComparisons
                .filter { comparison ->
                    comparison.improvesBothPrimaryMetrics
                }
                .sortedByDescending { comparison ->
                    comparison.delta.balancedAccuracy
                }

        if (improving.isEmpty()) {

            output.println(
                "  none",
            )

        } else {

            improving.forEach { comparison ->

                output.println(
                    "  ${comparison.featureName}",
                )
            }
        }

        output.println()

        output.println(
            "Report                   : " +
                    reportFile.absolutePath,
        )

        output.println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        )
    }

    private fun ensureDirectory(
        directory: File,
    ) {
        if (!directory.exists()) {

            check(directory.mkdirs()) {
                "Could not create directory: " +
                        directory.absolutePath
            }
        }

        require(directory.isDirectory) {
            "Path is not a directory: " +
                    directory.absolutePath
        }
    }

    private fun sanitize(
        value: String,
    ): String {

        return value
            .lowercase(Locale.ROOT)
            .replace(
                Regex("[^a-z0-9]+"),
                "-",
            )
            .trim('-')
    }

    private fun format(
        value: Double,
    ): String {

        return "%+.4f".format(
            Locale.ROOT,
            value,
        )
    }

    private companion object {

        const val REPORT_VERSION =
            1

        const val BASELINE_MODEL_FILE_NAME =
            "nutrition.local-matcher-baseline.json"

        const val EXTENDED_MODEL_FILE_NAME =
            "nutrition.local-matcher-active-features.json"

        const val EPSILON =
            1e-12
    }
}