package de.shopme.testing.system.tools.knowledge.catalog.semantic.audit

import com.google.gson.GsonBuilder
import java.io.File

class CanonicalCatalogSemanticInvalidityReportWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        report: CanonicalCatalogSemanticInvalidityReport,
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