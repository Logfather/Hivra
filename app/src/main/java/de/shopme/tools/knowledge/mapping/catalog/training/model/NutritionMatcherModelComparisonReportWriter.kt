package de.shopme.tools.knowledge.mapping.catalog.training.model

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class NutritionMatcherModelComparisonReportWriter(
    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create(),
) {

    fun write(
        report: NutritionMatcherModelComparisonReport,
        outputFile: File,
    ) {
        require(
            outputFile.name.isNotBlank(),
        ) {
            "Nutrition matcher model comparison report file name must not be blank."
        }

        val parentDirectory =
            outputFile.parentFile

        if (parentDirectory != null) {
            ensureDirectory(
                directory =
                    parentDirectory,
            )
        }

        outputFile.writeText(
            gson.toJson(report) + "\n",
        )

        check(outputFile.isFile) {
            "Nutrition matcher model comparison report was not written: " +
                    outputFile.absolutePath
        }

        check(outputFile.length() > 0L) {
            "Nutrition matcher model comparison report is empty: " +
                    outputFile.absolutePath
        }
    }

    private fun ensureDirectory(
        directory: File,
    ) {
        if (!directory.exists()) {
            check(directory.mkdirs()) {
                "Could not create Nutrition matcher model comparison " +
                        "report directory: ${directory.absolutePath}"
            }
        }

        require(directory.isDirectory) {
            "Nutrition matcher model comparison report parent path " +
                    "is not a directory: ${directory.absolutePath}"
        }
    }
}