package de.shopme.tools.knowledge.off.nutrition.reference.deduplication

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class OFFNutritionReferenceDuplicateReportWriter(
    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
) {

    fun write(
        result: OFFNutritionReferenceDeduplicationResult,
        outputFile: File
    ): File {

        outputFile.parentFile?.mkdirs()

        require(
            outputFile.parentFile?.isDirectory == true
        ) {
            "Could not create duplicate report directory: " +
                    outputFile.parentFile?.absolutePath
        }

        val report =
            OFFNutritionReferenceDuplicateReport(
                version =
                    1,
                inputCandidateCount =
                    result.inputCandidateCount,
                outputCandidateCount =
                    result.outputCandidateCount,
                removedDuplicateCount =
                    result.removedDuplicateCount,
                duplicateGroupCount =
                    result.duplicateGroupCount,
                duplicateGroups =
                    result.duplicateGroups
            )

        val temporaryFile =
            File(
                outputFile.parentFile,
                "${outputFile.name}.tmp"
            )

        temporaryFile.writeText(
            gson.toJson(report) + "\n"
        )

        if (outputFile.exists()) {
            require(outputFile.delete()) {
                "Could not replace duplicate report: " +
                        outputFile.absolutePath
            }
        }

        require(
            temporaryFile.renameTo(outputFile)
        ) {
            "Could not move duplicate report into place: " +
                    outputFile.absolutePath
        }

        return outputFile
    }
}

data class OFFNutritionReferenceDuplicateReport(
    val version: Int,
    val inputCandidateCount: Int,
    val outputCandidateCount: Int,
    val removedDuplicateCount: Int,
    val duplicateGroupCount: Int,
    val duplicateGroups: List<OFFNutritionReferenceDuplicateGroup>
)