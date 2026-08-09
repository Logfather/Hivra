package de.shopme.testing.system.tools.knowledge.catalog.baseline

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class CanonicalFoodCatalogBaselineWriter(
    private val gson: Gson =
        createDefaultGson()
) {

    fun write(
        baseline: CanonicalFoodCatalogBaseline,
        outputFile: File
    ) {
        require(baseline.valid)
        require(outputFile.name.isNotBlank())

        val parentDirectory =
            requireNotNull(
                outputFile.absoluteFile.parentFile
            )

        if (!parentDirectory.exists()) {
            require(parentDirectory.mkdirs()) {
                "Could not create catalog baseline directory: " +
                        parentDirectory.absolutePath
            }
        }

        require(parentDirectory.isDirectory)

        val json =
            gson.toJson(baseline)
                .trimEnd() +
                    System.lineSeparator()

        val temporaryFile =
            File(
                parentDirectory,
                ".${outputFile.name}.tmp"
            )

        try {
            temporaryFile.writeText(
                text = json,
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