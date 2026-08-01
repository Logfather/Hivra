package de.shopme.tools.knowledge.off.nutrition.reference.persistence

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.off.nutrition.reference.CanonicalOFFNutritionReferenceCandidate
import java.io.BufferedOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.zip.GZIPOutputStream

class OFFAcceptedNutritionReferenceWriter(
    private val gson: Gson =
        GsonBuilder()
            .disableHtmlEscaping()
            .create()
) {

    fun write(
        outputFile: File,
        produceReferences:
            (
            append:
                (CanonicalOFFNutritionReferenceCandidate) -> Unit
        ) -> Unit
    ): OFFAcceptedNutritionReferenceWriteResult {

        val canonicalOutputFile =
            outputFile.canonicalFile

        val outputDirectory =
            requireNotNull(
                canonicalOutputFile.parentFile
            ) {
                "Output file must have a parent directory."
            }

        Files.createDirectories(
            outputDirectory.toPath()
        )

        val temporaryFile =
            File(
                outputDirectory,
                canonicalOutputFile.name + ".tmp"
            )

        Files.deleteIfExists(
            temporaryFile.toPath()
        )

        val digest =
            MessageDigest.getInstance("SHA-256")

        var writtenReferenceCount =
            0L

        var uncompressedContentBytes =
            0L

        try {
            GZIPOutputStream(
                BufferedOutputStream(
                    Files.newOutputStream(
                        temporaryFile.toPath()
                    )
                )
            ).bufferedWriter(
                StandardCharsets.UTF_8
            ).use { writer ->

                produceReferences { reference ->

                    val line =
                        gson.toJson(reference) + "\n"

                    val lineBytes =
                        line.toByteArray(
                            StandardCharsets.UTF_8
                        )

                    digest.update(lineBytes)

                    writer.write(line)

                    writtenReferenceCount++
                    uncompressedContentBytes +=
                        lineBytes.size.toLong()
                }
            }

            moveAtomically(
                source =
                    temporaryFile,
                target =
                    canonicalOutputFile
            )
        } catch (throwable: Throwable) {
            Files.deleteIfExists(
                temporaryFile.toPath()
            )

            throw throwable
        }

        return OFFAcceptedNutritionReferenceWriteResult(
            outputFile =
                canonicalOutputFile,
            writtenReferenceCount =
                writtenReferenceCount,
            contentSha256 =
                digest.digest().toHex(),
            uncompressedContentBytes =
                uncompressedContentBytes
        )
    }

    private fun moveAtomically(
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

    private fun ByteArray.toHex(): String =
        joinToString(separator = "") { byte ->
            "%02x".format(
                byte.toInt() and 0xff
            )
        }
}