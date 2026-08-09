package de.shopme.testing.system.tools.knowledge.catalog.linguistic.repair

import com.google.gson.GsonBuilder
import java.io.File

class CanonicalCatalogFamilyPluralGapAuditWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        report: CanonicalCatalogFamilyPluralGapAuditReport,
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