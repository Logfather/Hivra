package de.shopme.tools.knowledge.off.nutrition.reference.retrieval.missing

import com.google.gson.GsonBuilder
import java.io.File

class MissingOFFNutritionRetrievalCandidateReportWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        report: MissingOFFNutritionRetrievalCandidateReport,
        outputFile: File
    ) {

        outputFile.parentFile?.mkdirs()

        require(
            outputFile.parentFile?.isDirectory == true
        )

        outputFile.writeText(
            text =
                gson.toJson(report) + "\n",
            charset =
                Charsets.UTF_8
        )
    }
}