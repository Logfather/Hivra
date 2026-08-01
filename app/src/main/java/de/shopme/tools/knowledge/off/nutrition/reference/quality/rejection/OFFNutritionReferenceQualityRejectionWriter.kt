package de.shopme.tools.knowledge.off.nutrition.reference.quality.rejection

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityRejection
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class OFFNutritionReferenceQualityRejectionWriter {

    private val gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        rejections: List<OFFNutritionReferenceQualityRejection>,
        outputFile: File
    ): OFFNutritionReferenceQualityRejectionWriteResult {

        val canonicalOutputFile =
            outputFile.canonicalFile

        canonicalOutputFile.parentFile?.mkdirs()

        val outputDirectory =
            canonicalOutputFile.parentFile

        if (outputDirectory != null) {
            outputDirectory.mkdirs()

            require(outputDirectory.isDirectory) {
                "Unable to create output directory for: " +
                        canonicalOutputFile.absolutePath
            }
        }

        val deterministicRejections =
            rejections
                .sortedWith(
                    compareBy<OFFNutritionReferenceQualityRejection>(
                        { it.canonicalId },
                        { it.sourceId }
                    )
                )

        require(
            deterministicRejections
                .map { rejection ->
                    rejection.sourceId to rejection.canonicalId
                }
                .toSet()
                .size == deterministicRejections.size
        ) {
            "Duplicate OFF nutrition quality rejection identities detected."
        }

        val document =
            RejectionDocument(
                version = VERSION,
                rejectionCount = deterministicRejections.size,
                rejections = deterministicRejections
            )

        val json =
            gson.toJson(document) + System.lineSeparator()

        val bytes =
            json.toByteArray(StandardCharsets.UTF_8)

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

        Files.write(
            temporaryFile.toPath(),
            bytes
        )

        replace(
            source = temporaryFile,
            target = canonicalOutputFile
        )

        return OFFNutritionReferenceQualityRejectionWriteResult(
            outputFile = canonicalOutputFile,
            rejectionCount = deterministicRejections.size,
            writtenByteCount = canonicalOutputFile.length()
        )
    }

    private fun replace(
        source: File,
        target: File
    ) {
        try {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    private data class RejectionDocument(
        val version: Int,
        val rejectionCount: Int,
        val rejections: List<OFFNutritionReferenceQualityRejection>
    )

    private companion object {

        const val VERSION =
            1
    }
}