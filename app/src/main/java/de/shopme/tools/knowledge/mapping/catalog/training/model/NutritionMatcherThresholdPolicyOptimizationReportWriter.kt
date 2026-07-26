package de.shopme.tools.knowledge.mapping.catalog.training.model

import com.google.gson.GsonBuilder
import java.io.File

class NutritionMatcherThresholdPolicyOptimizationReportWriter {

    fun write(
        report: NutritionMatcherThresholdPolicyOptimizationReport,
        outputFile: File,
    ) {

        require(
            report.version ==
                    NutritionMatcherThresholdPolicyOptimizer
                        .REPORT_VERSION,
        ) {
            "Unsupported nutrition threshold optimization report version."
        }

        require(
            report.featureNames ==
                    LocalNutritionMatcherFeatureContract
                        .ACTIVE_FEATURE_NAMES,
        ) {
            "Threshold optimization report does not use the active " +
                    "feature contract."
        }

        require(
            report.featureCount ==
                    report.featureNames.size,
        ) {
            "Threshold report feature count is inconsistent."
        }

        require(
            report.candidateCount ==
                    report.entries.size,
        ) {
            "Threshold report candidate count is inconsistent."
        }

        val recommendedEntry =
            report.entries
                .singleOrNull { entry ->
                    entry.name ==
                            report.recommendedCandidateName
                }

        requireNotNull(
            recommendedEntry,
        ) {
            "Recommended threshold candidate is missing."
        }

        require(
            recommendedEntry.decisionThreshold ==
                    report.recommendedDecisionThreshold,
        ) {
            "Recommended decision threshold differs from its entry."
        }

        require(
            recommendedEntry.policy ==
                    report.recommendedPolicy,
        ) {
            "Recommended threshold policy differs from its entry."
        }

        outputFile.parentFile
            ?.let { directory ->
                if (
                    !directory.exists()
                ) {
                    require(
                        directory.mkdirs(),
                    ) {
                        "Could not create threshold report directory: " +
                                directory.absolutePath
                    }
                }
            }

        val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()

        outputFile.writeText(
            text =
                gson.toJson(
                    report,
                ) +
                        System.lineSeparator(),
        )
    }
}