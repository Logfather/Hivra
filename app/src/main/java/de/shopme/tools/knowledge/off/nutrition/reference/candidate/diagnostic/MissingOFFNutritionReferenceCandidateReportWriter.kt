package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic

import com.google.gson.GsonBuilder
import java.io.File

class MissingOFFNutritionReferenceCandidateReportWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        report: MissingOFFNutritionReferenceCandidateReport,
        outputFile: File
    ) {

        require(
            report.findings ==
                    report.findings.sortedWith(
                        compareBy<MissingOFFNutritionReferenceCandidateFinding>(
                            { it.catalogIndex },
                            { it.catalogKey }
                        )
                    )
        ) {
            "Report findings must be deterministically sorted."
        }

        outputFile.parentFile
            ?.mkdirs()

        require(
            outputFile.parentFile?.isDirectory == true
        ) {
            "Could not create output directory: " +
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