package de.shopme.testing.system.tools.knowledge.catalog.expansion.target

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class CanonicalCatalogTargetDerivationWriter(
    private val gson: Gson =
        createDefaultGson()
) {

    fun write(
        derivation:
        CanonicalCatalogTargetDerivation,

        outputFile: File
    ) {
        require(derivation.valid) {
            "Cannot persist invalid catalog target derivation: " +
                    derivation.blockers.joinToString()
        }

        val outputDirectory =
            requireNotNull(
                outputFile.absoluteFile.parentFile
            )

        if (!outputDirectory.exists()) {
            require(outputDirectory.mkdirs()) {
                "Could not create target derivation directory: " +
                        outputDirectory.absolutePath
            }
        }

        require(outputDirectory.isDirectory)

        val content =
            gson.toJson(derivation)
                .trimEnd() +
                    System.lineSeparator()

        val temporaryFile =
            File(
                outputDirectory,
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
            } catch (
                _: AtomicMoveNotSupportedException
            ) {
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

        require(outputFile.isFile)
        require(outputFile.length() > 0L)
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