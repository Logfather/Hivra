package de.shopme.tools.knowledge.rebuild.nutrition.coverage

import com.google.gson.GsonBuilder
import java.io.File

class NutritionCoverageGapAnalysisWriter(
    private val outputFile: File
) {

    fun write(
        analysis: NutritionCoverageGapAnalysis
    ) {
        outputFile.parentFile
            ?.mkdirs()

        val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()

        outputFile.writeText(
            gson.toJson(
                analysis
            ) + "\n"
        )

        require(outputFile.isFile) {
            "Nutrition coverage-gap analysis was not written: " +
                    outputFile.absolutePath
        }

        require(outputFile.length() > 0L) {
            "Nutrition coverage-gap analysis is empty: " +
                    outputFile.absolutePath
        }
    }
}