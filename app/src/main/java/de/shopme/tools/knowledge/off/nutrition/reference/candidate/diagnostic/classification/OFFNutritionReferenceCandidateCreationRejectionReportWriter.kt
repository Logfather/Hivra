package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.classification

import com.google.gson.GsonBuilder
import java.io.File

class OFFNutritionReferenceCandidateCreationRejectionReportWriter {

    fun write(
        report:
        OFFNutritionReferenceCandidateCreationRejectionReport,
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
            "Rejection classification report was not written: " +
                    canonicalOutputFile.absolutePath
        }

        require(canonicalOutputFile.length() > 0L) {
            "Rejection classification report is empty: " +
                    canonicalOutputFile.absolutePath
        }

        return canonicalOutputFile
    }
}