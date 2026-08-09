package de.shopme.testing.system.tools.knowledge.catalog.report

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.testing.system.tools.knowledge.catalog.review.CatalogReviewBacklogEvaluationResult
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class CatalogReviewBacklogReportWriter(
    private val gson: Gson = createDefaultGson()
) {

    fun write(
        result: CatalogReviewBacklogEvaluationResult,
        outputFile: File
    ) {
        require(outputFile.name.isNotBlank()) {
            "Review backlog report must have a filename."
        }

        require(!outputFile.exists() || outputFile.isFile) {
            "Review backlog output path is not a file: " +
                    outputFile.absolutePath
        }

        val parentDirectory = requireNotNull(
            outputFile.absoluteFile.parentFile
        )

        if (!parentDirectory.exists()) {
            require(parentDirectory.mkdirs()) {
                "Could not create review backlog report directory: " +
                        parentDirectory.absolutePath
            }
        }

        require(parentDirectory.isDirectory)
        require(parentDirectory.canWrite())

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