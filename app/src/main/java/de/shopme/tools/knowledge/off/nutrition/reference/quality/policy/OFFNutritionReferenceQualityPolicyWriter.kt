package de.shopme.tools.knowledge.off.nutrition.reference.quality.policy

import com.google.gson.GsonBuilder
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class OFFNutritionReferenceQualityPolicyWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        policy: OFFNutritionReferenceQualityPolicy,
        outputFile: File
    ): File {

        val canonicalOutputFile =
            outputFile.canonicalFile

        val outputDirectory =
            requireNotNull(
                canonicalOutputFile.parentFile
            ) {
                "Policy output file must have a parent directory."
            }

        Files.createDirectories(
            outputDirectory.toPath()
        )

        val temporaryFile =
            File(
                outputDirectory,
                canonicalOutputFile.name + ".tmp"
            )

        val content =
            gson.toJson(policy) +
                    System.lineSeparator()

        Files.write(
            temporaryFile.toPath(),
            content.toByteArray(StandardCharsets.UTF_8)
        )

        try {
            Files.move(
                temporaryFile.toPath(),
                canonicalOutputFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                temporaryFile.toPath(),
                canonicalOutputFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }

        return canonicalOutputFile
    }
}