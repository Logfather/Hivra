package de.shopme.testing.system.tools.knowledge.catalog.semantic.combination

import com.google.gson.GsonBuilder
import java.io.File

class SemanticCombinationConstraintAuditReportWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        report: SemanticCombinationConstraintAuditReport,
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