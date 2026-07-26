package de.shopme.tools.knowledge.off.nutrition.reference.training

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class OFFNutritionMatcherTrainingCandidateDatasetWriter(
    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
) {

    fun write(
        candidates: List<OFFNutritionMatcherTrainingCandidate>,
        outputFile: File
    ): File {

        require(
            candidates ==
                    candidates.sortedBy { candidate ->
                        candidate.serverKey
                    }
        ) {
            "Matcher training candidates must be sorted before writing."
        }

        require(
            candidates
                .map { candidate ->
                    candidate.serverKey
                }
                .distinct()
                .size ==
                    candidates.size
        ) {
            "Matcher training candidate serverKeys must be unique."
        }

        outputFile.parentFile?.mkdirs()

        require(outputFile.parentFile?.isDirectory == true) {
            "Could not create matcher candidate output directory: " +
                    outputFile.parentFile?.absolutePath
        }

        val temporaryFile =
            File(
                outputFile.parentFile,
                "${outputFile.name}.tmp"
            )

        temporaryFile.writeText(
            gson.toJson(candidates) +
                    System.lineSeparator()
        )

        if (outputFile.exists()) {
            require(outputFile.delete()) {
                "Could not replace matcher candidate dataset: " +
                        outputFile.absolutePath
            }
        }

        require(temporaryFile.renameTo(outputFile)) {
            "Could not move matcher candidate dataset into place: " +
                    outputFile.absolutePath
        }

        return outputFile
    }
}