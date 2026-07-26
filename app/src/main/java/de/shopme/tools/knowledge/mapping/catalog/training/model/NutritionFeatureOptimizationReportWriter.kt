package de.shopme.tools.knowledge.mapping.catalog.training.model

import com.google.gson.GsonBuilder
import java.io.File

class NutritionFeatureOptimizationReportWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        report: NutritionFeatureOptimizationReport,
        outputFile: File,
    ) {
        outputFile.parentFile?.let { parentDirectory ->
            if (!parentDirectory.exists()) {
                require(parentDirectory.mkdirs()) {
                    "Could not create feature optimization report directory: " +
                            parentDirectory.path
                }
            }
        }

        outputFile.writeText(
            gson.toJson(report) + "\n",
        )
    }
}