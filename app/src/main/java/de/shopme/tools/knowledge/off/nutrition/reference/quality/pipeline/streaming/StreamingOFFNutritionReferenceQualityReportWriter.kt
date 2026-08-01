package de.shopme.tools.knowledge.off.nutrition.reference.quality.pipeline.streaming

import com.google.gson.GsonBuilder
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class StreamingOFFNutritionReferenceQualityReportWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        report: StreamingOFFNutritionReferenceQualityReport,
        outputFile: File
    ): StreamingOFFNutritionReferenceQualityReportWriteResult {

        val canonicalOutputFile =
            outputFile.canonicalFile

        val outputDirectory =
            canonicalOutputFile.parentFile

        if (outputDirectory != null) {
            Files.createDirectories(
                outputDirectory.toPath()
            )
        }

        val temporaryFile =
            if (outputDirectory != null) {
                File(
                    outputDirectory,
                    canonicalOutputFile.name + ".tmp"
                )
            } else {
                File(
                    canonicalOutputFile.name + ".tmp"
                )
            }

        val bytes =
            (
                    gson.toJson(report) +
                            System.lineSeparator()
                    )
                .toByteArray(StandardCharsets.UTF_8)

        Files.write(
            temporaryFile.toPath(),
            bytes
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

        return StreamingOFFNutritionReferenceQualityReportWriteResult(
            outputFile =
                canonicalOutputFile,
            writtenByteCount =
                canonicalOutputFile.length()
        )
    }
}