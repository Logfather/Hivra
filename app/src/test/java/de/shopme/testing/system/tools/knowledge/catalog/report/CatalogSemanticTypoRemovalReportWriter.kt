package de.shopme.testing.system.tools.knowledge.catalog.report

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.testing.system.tools.knowledge.catalog.removal.CatalogSemanticTypoRemovalResult
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class CatalogSemanticTypoRemovalReportWriter(
    private val gson: Gson = createDefaultGson()
) {

    fun write(
        result: CatalogSemanticTypoRemovalResult,
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
                "Could not create semantic typo removal directory: " +
                        parentDirectory.absolutePath
            }
        }

        require(parentDirectory.isDirectory)

        val content =
            gson.toJson(
                ReportPayload(
                    version = result.version,

                    inputEntryCount =
                        result.inputEntryCount,

                    candidateEntryCount =
                        result.candidateEntryCount,

                    removedEntryCount =
                        result.removedEntryCount,

                    outputEntryCount =
                        result.outputEntryCount,

                    removedSourceIndices =
                        result.removedSourceIndices,

                    countsByReason =
                        result.countsByReason,

                    decisions =
                        result.decisions,

                    valid =
                        result.valid
                )
            ).trimEnd() +
                    System.lineSeparator()

        val temporaryFile =
            File(
                parentDirectory,
                ".${outputFile.name}.tmp"
            )

        try {
            temporaryFile.writeText(
                text = content,
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
    }

    private data class ReportPayload(
        val version: Int,

        val inputEntryCount: Int,
        val candidateEntryCount: Int,
        val removedEntryCount: Int,
        val outputEntryCount: Int,

        val removedSourceIndices: List<Int>,

        val countsByReason:
        Map<
                de.shopme.testing.system.tools.knowledge.catalog.removal.CatalogSemanticTypoRemovalReason,
                Int
                >,

        val decisions:
        List<
                de.shopme.testing.system.tools.knowledge.catalog.removal.CatalogSemanticTypoRemovalDecision
                >,

        val valid: Boolean
    )

    private companion object {

        fun createDefaultGson(): Gson =
            GsonBuilder()
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .serializeNulls()
                .create()
    }
}