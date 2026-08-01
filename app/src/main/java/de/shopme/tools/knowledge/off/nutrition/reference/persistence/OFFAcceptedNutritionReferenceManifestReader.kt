package de.shopme.tools.knowledge.off.nutrition.reference.persistence

import com.google.gson.GsonBuilder
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class OFFAcceptedNutritionReferenceManifestReader {

    private val gson =
        GsonBuilder()
            .disableHtmlEscaping()
            .create()

    fun read(
        inputFile: File
    ): OFFAcceptedNutritionReferenceManifest {

        val canonicalInputFile =
            inputFile.canonicalFile

        require(canonicalInputFile.isFile) {
            "OFF nutrition reference manifest does not exist: " +
                    canonicalInputFile.absolutePath
        }

        val content =
            String(
                Files.readAllBytes(canonicalInputFile.toPath()),
                StandardCharsets.UTF_8
            )

        val manifest =
            requireNotNull(
                gson.fromJson(
                    content,
                    OFFAcceptedNutritionReferenceManifest::class.java
                )
            ) {
                "OFF nutrition reference manifest is empty."
            }

        require(
            manifest.version ==
                    OFFAcceptedNutritionReferenceManifest.CURRENT_VERSION
        ) {
            "Unsupported OFF nutrition reference manifest version: " +
                    manifest.version
        }

        return manifest
    }
}