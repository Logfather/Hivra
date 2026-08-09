package de.shopme.testing.system.tools.knowledge.catalog.semantic.variant

import com.google.gson.GsonBuilder
import java.io.File

class SemanticVariantTaxonomyAuditReportWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        report: SemanticVariantTaxonomyAuditReport,
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