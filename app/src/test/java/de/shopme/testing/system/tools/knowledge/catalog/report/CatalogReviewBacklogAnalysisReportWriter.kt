package de.shopme.testing.system.tools.knowledge.catalog.report

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.testing.system.tools.knowledge.catalog.review.analysis.CatalogReviewBacklogAnalysisResult
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class CatalogReviewBacklogAnalysisReportWriter(
    private val gson: Gson = createDefaultGson()
) {

    fun write(
        result: CatalogReviewBacklogAnalysisResult,
        outputFile: File
    ) {
        require(result.valid) {
            "Review backlog analysis result must be valid."
        }

        require(outputFile.name.isNotBlank()) {
            "Analysis output file must have a filename."
        }

        require(!outputFile.exists() || outputFile.isFile) {
            "Analysis output path is not a file: " +
                    outputFile.absolutePath
        }

        val parentDirectory = requireNotNull(
            outputFile.absoluteFile.parentFile
        )

        if (!parentDirectory.exists()) {
            require(parentDirectory.mkdirs()) {
                "Could not create analysis report directory: " +
                        parentDirectory.absolutePath
            }
        }

        require(parentDirectory.isDirectory) {
            "Analysis report parent is not a directory: " +
                    parentDirectory.absolutePath
        }

        val content =
            gson.toJson(result).trimEnd() +
                    System.lineSeparator()

        writeAtomically(
            outputFile = outputFile,
            content = content
        )
    }

    private fun writeAtomically(
        outputFile: File,
        content: String
    ) {
        val parentDirectory = requireNotNull(
            outputFile.absoluteFile.parentFile
        )

        val temporaryFile = File(
            parentDirectory,
            ".${outputFile.name}.tmp"
        )

        try {
            temporaryFile.writeText(
                text = content,
                charset = StandardCharsets.UTF_8
            )

            try {
                Files.move(
                    temporaryFile.toPath(),
                    outputFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(
                    temporaryFile.toPath(),
                    outputFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        } finally {
            if (temporaryFile.exists()) {
                temporaryFile.delete()
            }
        }
    }

    private companion object {

        fun createDefaultGson(): Gson =
            GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .serializeNulls()
                .create()
    }
}