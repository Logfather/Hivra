package de.shopme.tools.knowledge.off.nutrition.reference.retrieval

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class CatalogOFFNutritionRetrievalDatasetWriter(
    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
) {

    fun write(
        requests: List<CatalogOFFNutritionRetrievalRequest>,
        outputFile: File
    ): File {

        require(
            requests ==
                    requests.sortedBy { request ->
                        request.catalogIndex
                    }
        ) {
            "Retrieval requests must be sorted by catalogIndex."
        }

        outputFile.parentFile?.mkdirs()

        require(outputFile.parentFile?.isDirectory == true) {
            "Could not create retrieval output directory."
        }

        val temporaryFile =
            File(
                outputFile.parentFile,
                "${outputFile.name}.tmp"
            )

        temporaryFile.writeText(
            gson.toJson(requests) +
                    System.lineSeparator()
        )

        if (outputFile.exists()) {
            require(outputFile.delete())
        }

        require(
            temporaryFile.renameTo(outputFile)
        )

        return outputFile
    }
}