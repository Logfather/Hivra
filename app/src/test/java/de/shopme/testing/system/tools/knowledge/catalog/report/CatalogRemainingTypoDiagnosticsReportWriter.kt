package de.shopme.testing.system.tools.knowledge.catalog.report

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.testing.system.tools.knowledge.catalog.duplicate.resolution.diagnostics.CatalogRemainingTypoDiagnosticsResult
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class CatalogRemainingTypoDiagnosticsReportWriter(
    private val gson: Gson = createDefaultGson()
) {

    fun write(
        result: CatalogRemainingTypoDiagnosticsResult,
        outputFile: File
    ) {
        require(result.valid)

        require(outputFile.name.isNotBlank())

        val parentDirectory =
            requireNotNull(
                outputFile.absoluteFile.parentFile
            )

        if (!parentDirectory.exists()) {
            require(parentDirectory.mkdirs()) {
                "Could not create typo diagnostics directory: " +
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