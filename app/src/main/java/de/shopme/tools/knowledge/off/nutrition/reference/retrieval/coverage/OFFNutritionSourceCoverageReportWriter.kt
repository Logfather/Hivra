package de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage

import com.google.gson.GsonBuilder
import java.io.File

class OFFNutritionSourceCoverageReportWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        report: OFFNutritionSourceCoverageReport,
        outputFile: File
    ) {

        outputFile.parentFile?.mkdirs()

        require(
            outputFile.parentFile?.isDirectory == true
        ) {
            "Could not create report directory: " +
                    outputFile.parentFile?.absolutePath
        }

        outputFile.writeText(
            text =
                gson.toJson(report) + "\n",
            charset =
                Charsets.UTF_8
        )
    }
}