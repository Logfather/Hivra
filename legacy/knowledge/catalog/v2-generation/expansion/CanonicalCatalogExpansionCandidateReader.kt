package de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class CanonicalCatalogExpansionCandidateReader(
    private val gson: Gson =
        createDefaultGson()
) {

    fun read(
        inputFile: File
    ): CanonicalCatalogExpansionCandidateGenerationResult {
        require(inputFile.isFile) {
            "Expansion candidate file does not exist: " +
                    inputFile.absolutePath
        }

        require(inputFile.length() > 0L)

        return requireNotNull(
            inputFile.reader(Charsets.UTF_8)
                .use { reader ->
                    gson.fromJson(
                        reader,
                        CanonicalCatalogExpansionCandidateGenerationResult::
                        class.java
                    )
                }
        )
    }

    private companion object {
        fun createDefaultGson(): Gson =
            GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .create()
    }
}