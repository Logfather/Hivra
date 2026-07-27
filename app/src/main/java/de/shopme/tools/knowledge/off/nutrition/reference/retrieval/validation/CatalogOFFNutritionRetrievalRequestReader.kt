package de.shopme.tools.knowledge.off.nutrition.reference.retrieval

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

class CatalogOFFNutritionRetrievalRequestReader {

    private val gson =
        GsonBuilder()
            .create()

    fun read(
        inputFile: File
    ): List<CatalogOFFNutritionRetrievalRequest> {

        require(inputFile.isFile) {
            "OFF nutrition retrieval request file not found: " +
                    inputFile.absolutePath
        }

        val listType =
            object :
                TypeToken<List<CatalogOFFNutritionRetrievalRequest>>() {
            }.type

        val requests =
            inputFile
                .reader(Charsets.UTF_8)
                .use { reader ->
                    gson.fromJson<List<CatalogOFFNutritionRetrievalRequest>>(
                        reader,
                        listType
                    )
                }
                ?: error(
                    "OFF nutrition retrieval request file contains null."
                )

        require(
            requests ==
                    requests.sortedBy { request ->
                        request.catalogIndex
                    }
        ) {
            "OFF nutrition retrieval requests must be sorted by catalogIndex."
        }

        require(
            requests.map { request ->
                request.catalogIndex
            } ==
                    requests.indices.toList()
        ) {
            "OFF nutrition retrieval request indexes must be contiguous."
        }

        return requests
    }
}