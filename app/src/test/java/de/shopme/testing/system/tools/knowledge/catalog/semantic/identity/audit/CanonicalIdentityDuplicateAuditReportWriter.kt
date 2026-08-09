package de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.audit

import com.google.gson.GsonBuilder
import java.io.File

class CanonicalIdentityDuplicateAuditReportWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        report: CanonicalIdentityDuplicateAuditReport,
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