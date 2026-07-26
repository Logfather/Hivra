package de.shopme.tools.knowledge.mapping.catalog.training.model

import com.google.gson.GsonBuilder
import java.io.File

class NutritionFeatureSetOptimizationReportWriter {

    fun write(
        report: NutritionFeatureSetOptimizationReport,
        outputFile: File,
    ) {

        require(
            report.version ==
                    NutritionFeatureSetOptimizer
                        .REPORT_VERSION,
        ) {
            "Unsupported nutrition feature-set optimization report " +
                    "version: " +
                    report.version
        }

        require(
            report.entries.isNotEmpty(),
        ) {
            "Nutrition feature-set optimization report must contain " +
                    "at least one entry."
        }

        require(
            report.candidateCount ==
                    report.entries.size,
        ) {
            "Nutrition feature-set candidate count differs from entry " +
                    "count."
        }

        require(
            report.recommendedCandidateName in
                    report.entries.map { entry ->
                        entry.name
                    },
        ) {
            "Recommended nutrition feature-set candidate is missing " +
                    "from the report."
        }

        require(
            report.recommendedFeatureNames ==
                    report.entries
                        .single { entry ->
                            entry.name ==
                                    report.recommendedCandidateName
                        }
                        .featureNames,
        ) {
            "Recommended nutrition feature names differ from the " +
                    "recommended candidate."
        }

        outputFile.parentFile
            ?.let { directory ->
                if (
                    !directory.exists()
                ) {
                    require(
                        directory.mkdirs(),
                    ) {
                        "Could not create nutrition feature-set report " +
                                "directory: " +
                                directory.path
                    }
                }

                require(
                    directory.isDirectory,
                ) {
                    "Nutrition feature-set report parent path is not " +
                            "a directory: " +
                            directory.path
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