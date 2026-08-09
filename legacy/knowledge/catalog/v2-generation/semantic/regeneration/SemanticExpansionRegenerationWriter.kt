package de.shopme.testing.system.tools.knowledge.catalog.semantic.regeneration

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import java.io.File

class SemanticExpansionRegenerationWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        result: SemanticExpansionRegenerationResult,
        outputDirectory: File
    ) {

        outputDirectory.mkdirs()

        writeExpansion(
            result = result,
            outputDirectory = outputDirectory
        )

        writeReport(
            result = result,
            outputDirectory = outputDirectory
        )
    }

    private fun writeExpansion(
        result: SemanticExpansionRegenerationResult,
        outputDirectory: File
    ) {

        val json =
            JsonArray()

        result
            .regeneratedExpansion
            .forEach {
                json.add(it)
            }

        File(
            outputDirectory,
            "canonical-food-catalog-semantic-expansion-regenerated.json"
        )
            .writeText(
                gson.toJson(json) + "\n"
            )
    }

    private fun writeReport(
        result: SemanticExpansionRegenerationResult,
        outputDirectory: File
    ) {

        File(
            outputDirectory,
            "canonical-food-catalog-semantic-expansion-regeneration-report.json"
        )
            .writeText(
                gson.toJson(
                    result.copy(
                        regeneratedExpansion =
                            emptyList()
                    )
                ) + "\n"
            )
    }
}