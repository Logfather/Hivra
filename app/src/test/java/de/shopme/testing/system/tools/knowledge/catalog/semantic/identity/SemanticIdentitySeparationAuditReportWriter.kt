package de.shopme.testing.system.tools.knowledge.catalog.semantic.identity

import com.google.gson.GsonBuilder
import java.io.File

class SemanticIdentitySeparationAuditReportWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        report: SemanticIdentitySeparationAuditReport,
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