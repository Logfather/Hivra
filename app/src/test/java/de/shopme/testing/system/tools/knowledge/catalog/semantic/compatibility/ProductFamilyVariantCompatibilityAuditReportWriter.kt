package de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility

import com.google.gson.GsonBuilder
import java.io.File

class ProductFamilyVariantCompatibilityAuditReportWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        report: ProductFamilyVariantCompatibilityAuditReport,
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