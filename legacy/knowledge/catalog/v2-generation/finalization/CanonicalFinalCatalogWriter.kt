package de.shopme.testing.system.tools.knowledge.catalog.finalization

import com.google.gson.GsonBuilder
import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import java.io.File

class CanonicalFinalCatalogWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        items: List<CatalogFoodItem>,
        outputFile: File
    ) {
        require(items.isNotEmpty()) {
            "Final canonical food catalog must not be empty."
        }

        outputFile.parentFile?.mkdirs()

        outputFile.writeText(
            gson.toJson(items) +
                    System.lineSeparator(),

            Charsets.UTF_8
        )
    }
}