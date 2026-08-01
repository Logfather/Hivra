package de.shopme.tools.knowledge.off.nutrition.reference.freeze

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class OFFNutritionSourceSnapshotWriter {

    private val gson: Gson =
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

    fun write(
        snapshot: OFFNutritionSourceSnapshot,
        outputFile: File
    ): Boolean {

        outputFile.parentFile?.let { parent ->
            require(
                parent.mkdirs() ||
                        parent.isDirectory
            ) {
                "Could not create OFF Nutrition snapshot directory: " +
                        parent.absolutePath
            }
        }

        val serializedSnapshot =
            gson.toJson(
                snapshot
            ) + "\n"

        if (
            outputFile.isFile &&
            outputFile.readText(
                StandardCharsets.UTF_8
            ) == serializedSnapshot
        ) {
            return false
        }

        val temporaryFile =
            outputFile.resolveSibling(
                outputFile.name +
                        ".tmp"
            )

        temporaryFile.writeText(
            serializedSnapshot,
            StandardCharsets.UTF_8
        )

        moveAtomically(
            sourceFile =
                temporaryFile,
            targetFile =
                outputFile
        )

        return true
    }

    private fun moveAtomically(
        sourceFile: File,
        targetFile: File
    ) {
        runCatching {
            Files.move(
                sourceFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        }
            .getOrElse {
                Files.move(
                    sourceFile.toPath(),
                    targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
    }
}