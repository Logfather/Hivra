package de.shopme.testing.system.tools.knowledge.catalog.expansion.refinement

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class CanonicalFoodCatalogRefinedTargetDistributionWriter(
    private val gson: Gson =
        createDefaultGson()
) {

    fun write(
        distribution:
        CanonicalFoodCatalogRefinedTargetDistribution,

        outputFile: File
    ) {
        require(distribution.valid) {
            "Cannot write invalid refined target distribution: " +
                    distribution.blockers.joinToString()
        }

        val outputDirectory =
            requireNotNull(
                outputFile.absoluteFile.parentFile
            )

        if (!outputDirectory.exists()) {
            require(outputDirectory.mkdirs())
        }

        require(outputDirectory.isDirectory)

        val content =
            gson.toJson(distribution)
                .trimEnd() +
                    System.lineSeparator()

        val temporaryFile =
            File(
                outputDirectory,
                ".${outputFile.name}.tmp"
            )

        try {
            temporaryFile.writeText(
                content,
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