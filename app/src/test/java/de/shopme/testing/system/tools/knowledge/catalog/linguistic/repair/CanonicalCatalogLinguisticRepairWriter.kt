package de.shopme.testing.system.tools.knowledge.catalog.linguistic.repair

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.File

class CanonicalCatalogLinguisticRepairWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun writeCatalog(
        catalog: List<JsonObject>,
        outputFile: File
    ) {

        outputFile
            .parentFile
            ?.mkdirs()

        val array =
            JsonArray().apply {
                catalog.forEach(::add)
            }

        outputFile.writeText(
            gson.toJson(array) + "\n"
        )
    }

    fun writeReport(
        report: CanonicalCatalogLinguisticRepairReport,
        outputFile: File
    ) {

        outputFile
            .parentFile
            ?.mkdirs()

        outputFile.writeText(
            gson.toJson(report) + "\n"
        )
    }
}