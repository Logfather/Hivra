package de.shopme.testing.system.tools.knowledge.catalog.report

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.testing.system.tools.knowledge.catalog.review.analysis.updated.CatalogUpdatedReviewAnalysisResult
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class CatalogUpdatedReviewAnalysisReportWriter(
    private val gson: Gson = createDefaultGson()
) {

    fun write(
        result: CatalogUpdatedReviewAnalysisResult,
        outputFile: File
    ) {
        require(result.valid) {
            "Updated review analysis must be valid."
        }

        require(outputFile.name.isNotBlank()) {
            "Updated analysis output must have a filename."
        }

        val parentDirectory = requireNotNull(
            outputFile.absoluteFile.parentFile
        )

        if (!parentDirectory.exists()) {
            require(parentDirectory.mkdirs()) {
                "Could not create updated analysis directory: " +
                        parentDirectory.absolutePath
            }
        }

        require(parentDirectory.isDirectory)

        val content =
            gson.toJson(result).trimEnd() +
                    System.lineSeparator()

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