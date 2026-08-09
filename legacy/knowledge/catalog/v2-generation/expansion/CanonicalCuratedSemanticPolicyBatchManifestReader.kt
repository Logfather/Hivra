package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class CanonicalCuratedSemanticPolicyBatchManifestReader(
    private val gson: Gson =
        createDefaultGson()
) {

    fun read(
        inputFile: File
    ): CanonicalCuratedSemanticPolicyBatchManifest {
        require(inputFile.isFile) {
            "Curated semantic policy manifest does not exist: " +
                    inputFile.absolutePath
        }

        require(inputFile.length() > 0L) {
            "Curated semantic policy manifest is empty: " +
                    inputFile.absolutePath
        }

        val manifest =
            requireNotNull(
                inputFile.reader(Charsets.UTF_8)
                    .use { reader ->
                        gson.fromJson(
                            reader,
                            CanonicalCuratedSemanticPolicyBatchManifest::
                            class.java
                        )
                    }
            )

        require(manifest.valid) {
            "Curated semantic policy manifest is invalid."
        }

        return manifest
    }

    private companion object {

        fun createDefaultGson(): Gson =
            GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .create()
    }
}