package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class CanonicalCuratedSemanticPolicyBatchManifestWriter(
    private val gson: Gson =
        createDefaultGson()
) {

    fun write(
        manifest:
        CanonicalCuratedSemanticPolicyBatchManifest,

        outputFile: File
    ) {
        require(manifest.valid)

        val outputDirectory =
            requireNotNull(
                outputFile.absoluteFile.parentFile
            )

        if (!outputDirectory.exists()) {
            require(outputDirectory.mkdirs()) {
                "Could not create curated-policy directory: " +
                        outputDirectory.absolutePath
            }
        }

        require(outputDirectory.isDirectory)

        val content =
            gson.toJson(manifest)
                .trimEnd() +
                    System.lineSeparator()

        val temporaryFile =
            File(
                outputDirectory,
                ".${outputFile.name}.tmp"
            )

        try {
            temporaryFile.writeText(
                text =
                    content,

                charset =
                    StandardCharsets.UTF_8
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