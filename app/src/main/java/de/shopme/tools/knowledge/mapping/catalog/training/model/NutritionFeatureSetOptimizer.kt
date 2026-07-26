package de.shopme.tools.knowledge.mapping.catalog.training.model

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class NutritionFeatureSetOptimizer(
    private val fullFeatureExtractor:
    LocalNutritionMatcherFeatureExtractor =
        LocalNutritionMatcherFeatureExtractor(),
) {

    fun optimize(
        datasetFile: File,
        workingDirectory: File,
    ): NutritionFeatureSetOptimizationReport {

        require(
            datasetFile.isFile,
        ) {
            "Nutrition matcher training dataset does not exist: " +
                    datasetFile.path
        }

        if (
            !workingDirectory.exists()
        ) {
            require(
                workingDirectory.mkdirs(),
            ) {
                "Could not create nutrition feature-set optimization " +
                        "directory: " +
                        workingDirectory.path
            }
        }

        require(
            workingDirectory.isDirectory,
        ) {
            "Nutrition feature-set optimization working path is not " +
                    "a directory: " +
                    workingDirectory.path
        }

        val candidates =
            featureSetCandidates()

        require(
            candidates.isNotEmpty(),
        ) {
            "Nutrition feature-set optimization requires at least " +
                    "one candidate."
        }

        require(
            candidates.map { candidate ->
                candidate.name
            }
                .distinct()
                .size ==
                    candidates.size,
        ) {
            "Nutrition feature-set candidate names must be unique."
        }

        val entries =
            candidates
                .map { candidate ->
                    trainCandidate(
                        candidate =
                            candidate,
                        datasetFile =
                            datasetFile,
                        outputFile =
                            File(
                                workingDirectory,
                                candidate.fileName,
                            ),
                    )
                }
                .sortedWith(
                    compareBy<NutritionFeatureSetOptimizationEntry>(
                        { entry ->
                            entry.featureCount
                        },
                        { entry ->
                            entry.name
                        },
                    ),
                )

        val recommendedEntry =
            entries
                .sortedWith(
                    recommendedEntryComparator(),
                )
                .first()

        return NutritionFeatureSetOptimizationReport(
            version =
                REPORT_VERSION,
            candidateCount =
                entries.size,
            recommendedCandidateName =
                recommendedEntry.name,
            recommendedFeatureNames =
                recommendedEntry.featureNames,
            entries =
                entries,
        )
    }

    private fun featureSetCandidates():
            List<NutritionFeatureSetCandidate> {

        val baseFeatureNames =
            LocalNutritionMatcherFeatureContract
                .BASE_FEATURE_NAMES

        val optimizedFeatureNames =
            LocalNutritionMatcherFeatureContract
                .OPTIMIZED_FEATURE_NAMES

        val optimizationBaselineFeatureNames =
            baseFeatureNames +
                    LocalNutritionMatcherFeatureContract
                        .OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES

        require(
            baseFeatureNames.size ==
                    11,
        ) {
            "Unexpected nutrition base-feature count: " +
                    baseFeatureNames.size
        }

        require(
            optimizedFeatureNames.size ==
                    15,
        ) {
            "Unexpected optimized nutrition feature count: " +
                    optimizedFeatureNames.size
        }

        require(
            optimizationBaselineFeatureNames.size ==
                    18,
        ) {
            "Unexpected nutrition optimization-baseline feature count: " +
                    optimizationBaselineFeatureNames.size
        }

        require(
            optimizedFeatureNames.take(
                LocalNutritionMatcherFeatureContract
                    .BASE_FEATURE_COUNT,
            ) ==
                    baseFeatureNames,
        ) {
            "Optimized nutrition feature contract must preserve the " +
                    "base-feature prefix."
        }

        require(
            optimizationBaselineFeatureNames.take(
                LocalNutritionMatcherFeatureContract
                    .BASE_FEATURE_COUNT,
            ) ==
                    baseFeatureNames,
        ) {
            "Optimization baseline must preserve the nutrition " +
                    "base-feature prefix."
        }

        require(
            optimizedFeatureNames.toSet()
                .subtract(
                    baseFeatureNames.toSet(),
                ) ==
                    LocalNutritionMatcherFeatureContract
                        .OPTIMIZED_DOMAIN_FEATURE_NAMES
                        .toSet(),
        ) {
            "Optimized nutrition feature contract contains unexpected " +
                    "domain features."
        }

        return listOf(
            NutritionFeatureSetCandidate(
                name =
                    BASE_CANDIDATE_NAME,
                fileName =
                    "base-11-model.json",
                featureNames =
                    baseFeatureNames,
            ),
            NutritionFeatureSetCandidate(
                name =
                    OPTIMIZED_CANDIDATE_NAME,
                fileName =
                    "optimized-15-model.json",
                featureNames =
                    optimizedFeatureNames,
            ),
            NutritionFeatureSetCandidate(
                name =
                    OPTIMIZATION_BASELINE_CANDIDATE_NAME,
                fileName =
                    "optimization-baseline-18-model.json",
                featureNames =
                    optimizationBaselineFeatureNames,
            ),
        )
    }

    private fun trainCandidate(
        candidate: NutritionFeatureSetCandidate,
        datasetFile: File,
        outputFile: File,
    ): NutritionFeatureSetOptimizationEntry {

        require(
            candidate.featureNames.isNotEmpty(),
        ) {
            "Nutrition feature-set candidate must not be empty: " +
                    candidate.name
        }

        require(
            candidate.featureNames.distinct().size ==
                    candidate.featureNames.size,
        ) {
            "Nutrition feature-set candidate contains duplicate " +
                    "features: " +
                    candidate.name
        }

        require(
            candidate.featureNames.take(
                LocalNutritionMatcherFeatureContract
                    .BASE_FEATURE_COUNT,
            ) ==
                    LocalNutritionMatcherFeatureContract
                        .BASE_FEATURE_NAMES,
        ) {
            "Nutrition feature-set candidate must preserve every " +
                    "base feature: " +
                    candidate.name
        }

        val featureExtractor =
            LocalNutritionMatcherFeatureSubsetExtractor(
                delegate =
                    fullFeatureExtractor,
                selectedFeatureNames =
                    candidate.featureNames,
            )

        val trainer =
            LocalNutritionMatcherModelTrainer(
                featureExtractor =
                    featureExtractor,
                supportedFeatureNames =
                    candidate.featureNames,
            )

        val model =
            PrintStream(
                ByteArrayOutputStream(),
            )
                .use { output ->
                    trainer
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
                    candidate.featureNames,
        ) {
            "Trained nutrition matcher feature contract differs " +
                    "from candidate: " +
                    candidate.name
        }

        val testMetrics =
            model.metrics.test

        val thresholdOptimization =
            model.decisionThresholdOptimization

        return NutritionFeatureSetOptimizationEntry(
            name =
                candidate.name,
            featureNames =
                model.featureNames,
            featureCount =
                model.featureNames.size,
            decisionThreshold =
                model.decisionThreshold,
            thresholdPolicySatisfied =
                thresholdOptimization.policySatisfied,
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

    private fun recommendedEntryComparator():
            Comparator<NutritionFeatureSetOptimizationEntry> {

        return compareByDescending<NutritionFeatureSetOptimizationEntry> {
                entry ->
            entry.thresholdPolicySatisfied
        }
            .thenByDescending { entry ->
                entry.testPrecision
            }
            .thenBy { entry ->
                entry.testFalsePositiveCount
            }
            .thenByDescending { entry ->
                entry.testF1
            }
            .thenByDescending { entry ->
                entry.testBalancedAccuracy
            }
            .thenByDescending { entry ->
                entry.testRecall
            }
            .thenBy { entry ->
                entry.featureCount
            }
            .thenBy { entry ->
                entry.name
            }
    }

    private data class NutritionFeatureSetCandidate(
        val name: String,
        val fileName: String,
        val featureNames: List<String>,
    )

    companion object {

        const val REPORT_VERSION =
            1

        const val BASE_CANDIDATE_NAME =
            "BASE_11"

        const val OPTIMIZED_CANDIDATE_NAME =
            "OPTIMIZED_15"

        const val OPTIMIZATION_BASELINE_CANDIDATE_NAME =
            "OPTIMIZATION_BASELINE_18"
    }
}

data class NutritionFeatureSetOptimizationReport(
    val version: Int,
    val candidateCount: Int,
    val recommendedCandidateName: String,
    val recommendedFeatureNames: List<String>,
    val entries: List<NutritionFeatureSetOptimizationEntry>,
)

data class NutritionFeatureSetOptimizationEntry(
    val name: String,
    val featureNames: List<String>,
    val featureCount: Int,
    val decisionThreshold: Double,
    val thresholdPolicySatisfied: Boolean,
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