package de.shopme.tools.knowledge.mapping.catalog.training.model

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.util.Locale

class NutritionFeatureOptimizationEvaluationWriter(
    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create(),
) {

    fun writeJson(
        evaluation: NutritionFeatureOptimizationEvaluation,
        outputFile: File,
    ) {
        ensureParentDirectoryExists(
            outputFile =
                outputFile,
        )

        outputFile.writeText(
            gson.toJson(
                evaluation,
            ) + "\n",
        )
    }

    fun writeMarkdown(
        evaluation: NutritionFeatureOptimizationEvaluation,
        outputFile: File,
    ) {
        ensureParentDirectoryExists(
            outputFile =
                outputFile,
        )

        outputFile.writeText(
            buildMarkdown(
                evaluation =
                    evaluation,
            ),
        )
    }

    private fun buildMarkdown(
        evaluation: NutritionFeatureOptimizationEvaluation,
    ): String {

        return buildString {
            appendLine(
                "# Nutrition Feature Optimization Evaluation",
            )
            appendLine()
            appendLine(
                "## Baseline",
            )
            appendLine()
            appendLine(
                "- Feature count: " +
                        evaluation.baseline.featureCount,
            )
            appendLine(
                "- Test precision: " +
                        formatMetric(
                            evaluation.baseline.testPrecision,
                        ),
            )
            appendLine(
                "- Test recall: " +
                        formatMetric(
                            evaluation.baseline.testRecall,
                        ),
            )
            appendLine(
                "- Test F1: " +
                        formatMetric(
                            evaluation.baseline.testF1,
                        ),
            )
            appendLine(
                "- Test balanced accuracy: " +
                        formatMetric(
                            evaluation
                                .baseline
                                .testBalancedAccuracy,
                        ),
            )
            appendLine()
            appendLine(
                "## Classification summary",
            )
            appendLine()
            appendLine(
                "- Required: " +
                        evaluation
                            .classificationCounts
                            .required,
            )
            appendLine(
                "- Neutral: " +
                        evaluation
                            .classificationCounts
                            .neutral,
            )
            appendLine(
                "- Harmful: " +
                        evaluation
                            .classificationCounts
                            .harmful,
            )
            appendLine()
            appendFeatureSection(
                title =
                    "Required features",
                features =
                    evaluation.requiredFeatures,
            )
            appendFeatureSection(
                title =
                    "Neutral features",
                features =
                    evaluation.neutralFeatures,
            )
            appendFeatureSection(
                title =
                    "Harmful features",
                features =
                    evaluation.harmfulFeatures,
            )
            appendLine(
                "## Recommended feature contract",
            )
            appendLine()
            appendLine(
                "- Recommended feature count: " +
                        evaluation.recommendedFeatureCount,
            )
            appendLine(
                "- Removed feature count: " +
                        evaluation.removedFeatureCount,
            )
            appendLine()
            evaluation.recommendedFeatureNames
                .forEach { featureName ->
                    appendLine(
                        "- `$featureName`",
                    )
                }

            if (evaluation.removedFeatureNames.isNotEmpty()) {
                appendLine()
                appendLine(
                    "## Removed features",
                )
                appendLine()

                evaluation.removedFeatureNames
                    .forEach { featureName ->
                        appendLine(
                            "- `$featureName`",
                        )
                    }
            }
        }
    }

    private fun StringBuilder.appendFeatureSection(
        title: String,
        features:
        List<NutritionFeatureOptimizationFeatureEvaluation>,
    ) {
        appendLine(
            "## $title",
        )
        appendLine()

        if (features.isEmpty()) {
            appendLine(
                "None.",
            )
            appendLine()

            return
        }

        appendLine(
            "| Feature | Δ Precision | Δ Recall | Δ F1 | " +
                    "Δ Balanced Accuracy |",
        )
        appendLine(
            "|---|---:|---:|---:|---:|",
        )

        features
            .sortedBy { feature ->
                feature.featureName
            }
            .forEach { feature ->
                appendLine(
                    "| `" +
                            feature.featureName +
                            "` | " +
                            formatSignedMetric(
                                feature.precisionDelta,
                            ) +
                            " | " +
                            formatSignedMetric(
                                feature.recallDelta,
                            ) +
                            " | " +
                            formatSignedMetric(
                                feature.f1Delta,
                            ) +
                            " | " +
                            formatSignedMetric(
                                feature.balancedAccuracyDelta,
                            ) +
                            " |",
                )
            }

        appendLine()
    }

    private fun ensureParentDirectoryExists(
        outputFile: File,
    ) {
        val parentDirectory =
            requireNotNull(
                outputFile.parentFile,
            ) {
                "Nutrition feature optimization evaluation output " +
                        "has no parent directory: " +
                        outputFile.path
            }

        if (!parentDirectory.exists()) {
            require(
                parentDirectory.mkdirs(),
            ) {
                "Could not create nutrition feature optimization " +
                        "evaluation output directory: " +
                        parentDirectory.path
            }
        }

        require(
            parentDirectory.isDirectory,
        ) {
            "Nutrition feature optimization evaluation parent path " +
                    "is not a directory: " +
                    parentDirectory.path
        }
    }

    private fun formatMetric(
        value: Double,
    ): String {
        return String.format(
            Locale.ROOT,
            METRIC_FORMAT,
            value,
        )
    }

    private fun formatSignedMetric(
        value: Double,
    ): String {
        return String.format(
            Locale.ROOT,
            SIGNED_METRIC_FORMAT,
            value,
        )
    }

    private companion object {

        const val METRIC_FORMAT =
            "%.6f"

        const val SIGNED_METRIC_FORMAT =
            "%+.6f"
    }
}