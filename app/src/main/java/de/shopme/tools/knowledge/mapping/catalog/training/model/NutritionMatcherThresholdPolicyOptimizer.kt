package de.shopme.tools.knowledge.mapping.catalog.training.model

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class NutritionMatcherThresholdPolicyOptimizer(
    private val featureExtractor:
    LocalNutritionMatcherFeatureProvider =
        LocalNutritionMatcherFeatureSubsetExtractor(
            delegate =
                LocalNutritionMatcherFeatureExtractor(),
            selectedFeatureNames =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_FEATURE_NAMES,
        ),
    private val candidates:
    List<NutritionMatcherThresholdPolicyCandidate> =
        NutritionMatcherConservativeThresholdPolicyContract
            .CANDIDATES,
) {

    init {
        require(
            featureExtractor.featureNames ==
                    LocalNutritionMatcherFeatureContract
                        .ACTIVE_FEATURE_NAMES,
        ) {
            "Threshold optimization must use the active nutrition " +
                    "feature contract."
        }

        require(
            candidates.isNotEmpty(),
        ) {
            "Threshold optimization requires policy candidates."
        }

        require(
            candidates.map { candidate ->
                candidate.name
            }
                .distinct()
                .size ==
                    candidates.size,
        ) {
            "Threshold policy candidate names must be unique."
        }
    }

    fun optimize(
        datasetFile: File,
        workingDirectory: File,
    ): NutritionMatcherThresholdPolicyOptimizationReport {

        require(
            datasetFile.isFile,
        ) {
            "Nutrition matcher training dataset does not exist: " +
                    datasetFile.absolutePath
        }

        if (
            !workingDirectory.exists()
        ) {
            require(
                workingDirectory.mkdirs(),
            ) {
                "Could not create threshold optimization directory: " +
                        workingDirectory.absolutePath
            }
        }

        require(
            workingDirectory.isDirectory,
        ) {
            "Threshold optimization working path is not a directory: " +
                    workingDirectory.absolutePath
        }

        val entries =
            candidates
                .sortedBy { candidate ->
                    candidate.conservatismRank
                }
                .map { candidate ->
                    trainCandidate(
                        candidate =
                            candidate,
                        datasetFile =
                            datasetFile,
                        outputFile =
                            File(
                                workingDirectory,
                                candidate.name
                                    .lowercase()
                                    .replace(
                                        oldChar =
                                            '_',
                                        newChar =
                                            '-',
                                    ) +
                                        "-model.json",
                            ),
                    )
                }

        val recommendedEntry =
            entries
                .sortedWith(
                    recommendedComparator(),
                )
                .first()

        return NutritionMatcherThresholdPolicyOptimizationReport(
            version =
                REPORT_VERSION,
            featureNames =
                featureExtractor.featureNames,
            featureCount =
                featureExtractor.featureNames.size,
            candidateCount =
                entries.size,
            recommendedCandidateName =
                recommendedEntry.name,
            recommendedDecisionThreshold =
                recommendedEntry.decisionThreshold,
            recommendedPolicy =
                recommendedEntry.policy,
            entries =
                entries,
        )
    }

    private fun trainCandidate(
        candidate: NutritionMatcherThresholdPolicyCandidate,
        datasetFile: File,
        outputFile: File,
    ): NutritionMatcherThresholdPolicyOptimizationEntry {

        val model =
            PrintStream(
                ByteArrayOutputStream(),
            )
                .use { output ->
                    LocalNutritionMatcherModelTrainer(
                        featureExtractor =
                            featureExtractor,
                        supportedFeatureNames =
                            featureExtractor.featureNames,
                        thresholdOptimizationPolicy =
                            candidate.policy,
                    )
                        .train(
                            datasetFile =
                                datasetFile,
                            outputFile =
                                outputFile,
                            output =
                                output,
                        )
                        .model
                }

        require(
            model.featureNames ==
                    featureExtractor.featureNames,
        ) {
            "Threshold policy model differs from active feature contract: " +
                    candidate.name
        }

        val optimization =
            model.decisionThresholdOptimization

        require(
            optimization.minimumPrecision ==
                    candidate.policy.minimumPrecision,
        ) {
            "Persisted minimum precision differs from candidate policy."
        }

        require(
            optimization.maximumFalsePositiveRate ==
                    candidate.policy.maximumFalsePositiveRate,
        ) {
            "Persisted maximum false-positive rate differs from " +
                    "candidate policy."
        }

        require(
            optimization.minimumPredictedPositiveCount ==
                    candidate.policy.minimumPredictedPositiveCount,
        ) {
            "Persisted minimum predicted-positive count differs from " +
                    "candidate policy."
        }

        val testMetrics =
            model.metrics.test

        return NutritionMatcherThresholdPolicyOptimizationEntry(
            name =
                candidate.name,
            conservatismRank =
                candidate.conservatismRank,
            policy =
                candidate.policy,
            decisionThreshold =
                model.decisionThreshold,
            calibrationPolicySatisfied =
                optimization.policySatisfied,
            calibrationPrecision =
                optimization.selectedPrecision,
            calibrationRecall =
                optimization.selectedRecall,
            calibrationFalsePositiveRate =
                optimization.selectedFalsePositiveRate,
            calibrationPredictedPositiveCount =
                optimization.selectedTruePositiveCount +
                        optimization.selectedFalsePositiveCount,
            testExampleCount =
                testMetrics.exampleCount,
            testPrecision =
                testMetrics.precision,
            testRecall =
                testMetrics.recall,
            testF1 =
                testMetrics.f1,
            testBalancedAccuracy =
                testMetrics.balancedAccuracy,
            testTruePositiveCount =
                testMetrics.truePositive,
            testFalsePositiveCount =
                testMetrics.falsePositive,
            testTrueNegativeCount =
                testMetrics.trueNegative,
            testFalseNegativeCount =
                testMetrics.falseNegative,
        )
    }

    private fun recommendedComparator():
            Comparator<NutritionMatcherThresholdPolicyOptimizationEntry> {

        return compareByDescending<
                NutritionMatcherThresholdPolicyOptimizationEntry
                > { entry ->
            entry.calibrationPolicySatisfied
        }
            .thenByDescending { entry ->
                entry.testPrecision
            }
            .thenBy { entry ->
                entry.testFalsePositiveCount
            }
            .thenByDescending { entry ->
                entry.testRecall
            }
            .thenByDescending { entry ->
                entry.testF1
            }
            .thenByDescending { entry ->
                entry.testBalancedAccuracy
            }
            .thenBy { entry ->
                entry.conservatismRank
            }
            .thenBy { entry ->
                entry.name
            }
    }

    companion object {

        const val REPORT_VERSION =
            1
    }
}

data class NutritionMatcherThresholdPolicyOptimizationReport(
    val version: Int,
    val featureNames: List<String>,
    val featureCount: Int,
    val candidateCount: Int,
    val recommendedCandidateName: String,
    val recommendedDecisionThreshold: Double,
    val recommendedPolicy: NutritionMatcherThresholdOptimizationPolicy,
    val entries: List<NutritionMatcherThresholdPolicyOptimizationEntry>,
)

data class NutritionMatcherThresholdPolicyOptimizationEntry(
    val name: String,
    val conservatismRank: Int,
    val policy: NutritionMatcherThresholdOptimizationPolicy,
    val decisionThreshold: Double,
    val calibrationPolicySatisfied: Boolean,
    val calibrationPrecision: Double,
    val calibrationRecall: Double,
    val calibrationFalsePositiveRate: Double,
    val calibrationPredictedPositiveCount: Int,
    val testExampleCount: Int,
    val testPrecision: Double,
    val testRecall: Double,
    val testF1: Double,
    val testBalancedAccuracy: Double,
    val testTruePositiveCount: Int,
    val testFalsePositiveCount: Int,
    val testTrueNegativeCount: Int,
    val testFalseNegativeCount: Int,
)