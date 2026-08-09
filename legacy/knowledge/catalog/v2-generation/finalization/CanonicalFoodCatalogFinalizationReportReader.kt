package de.shopme.testing.system.tools.knowledge.catalog.finalization

import com.google.gson.GsonBuilder
import java.io.File

class CanonicalFoodCatalogFinalizationReportReader {

    private val gson =
        GsonBuilder()
            .create()

    fun read(
        file: File
    ): CanonicalFoodCatalogFinalizationResult {
        require(file.isFile) {
            "Canonical food catalog finalization report does not exist: " +
                    file.absolutePath
        }

        require(file.length() > 0L) {
            "Canonical food catalog finalization report is empty: " +
                    file.absolutePath
        }

        return file
            .reader(Charsets.UTF_8)
            .use { reader ->
                gson.fromJson(
                    reader,
                    CanonicalFoodCatalogFinalizationResult::class.java
                )
            }
    }
}