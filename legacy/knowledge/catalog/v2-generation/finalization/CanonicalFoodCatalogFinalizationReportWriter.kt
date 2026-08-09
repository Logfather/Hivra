package de.shopme.testing.system.tools.knowledge.catalog.finalization

import com.google.gson.GsonBuilder
import java.io.File

class CanonicalFoodCatalogFinalizationReportWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        result: CanonicalFoodCatalogFinalizationResult,
        outputFile: File
    ) {
        require(result.valid) {
            "Only a valid canonical food catalog finalization result " +
                    "may be persisted."
        }

        outputFile.parentFile?.mkdirs()

        outputFile.writeText(
            gson.toJson(result) +
                    System.lineSeparator(),

            Charsets.UTF_8
        )
    }
}