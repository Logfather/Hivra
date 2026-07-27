package de.shopme.tools.knowledge.off.nutrition.reference.retrieval.validation

import com.google.gson.GsonBuilder
import java.io.File

class OFFNutritionRetrievalQualityReportWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        report: OFFNutritionRetrievalQualityReport,
        outputFile: File
    ) {

        outputFile.parentFile?.mkdirs()

        require(
            outputFile.parentFile?.isDirectory == true
        ) {
            "Could not create retrieval quality report directory: " +
                    outputFile.parentFile?.absolutePath
        }

        val json =
            gson.toJson(report) + "\n"

        outputFile.writeText(
            text = json,
            charset = Charsets.UTF_8
        )

        require(outputFile.isFile) {
            "Retrieval quality report was not written: " +
                    outputFile.absolutePath
        }
    }
}