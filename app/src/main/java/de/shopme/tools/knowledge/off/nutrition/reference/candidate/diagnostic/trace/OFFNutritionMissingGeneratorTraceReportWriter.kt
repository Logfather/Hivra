package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.trace

import com.google.gson.GsonBuilder
import java.io.File

class OFFNutritionMissingGeneratorTraceReportWriter {

    fun write(
        report: OFFNutritionMissingGeneratorTraceReport,
        outputFile: File
    ): File {

        val canonicalOutputFile =
            outputFile.canonicalFile

        canonicalOutputFile.parentFile?.mkdirs()

        require(
            canonicalOutputFile.parentFile?.isDirectory == true
        ) {
            "Output directory could not be created: " +
                    canonicalOutputFile.parentFile?.absolutePath
        }

        val json =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
                .toJson(report)
                .trimEnd() +
                    System.lineSeparator()

        canonicalOutputFile.writeText(json)

        require(canonicalOutputFile.isFile) {
            "Missing-generator-trace report was not written: " +
                    canonicalOutputFile.absolutePath
        }

        require(canonicalOutputFile.length() > 0L) {
            "Missing-generator-trace report is empty: " +
                    canonicalOutputFile.absolutePath
        }

        return canonicalOutputFile
    }
}