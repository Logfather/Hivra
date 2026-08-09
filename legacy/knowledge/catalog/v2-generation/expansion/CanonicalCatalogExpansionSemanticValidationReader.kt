package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class CanonicalCatalogExpansionSemanticValidationReader(
    private val gson: Gson =
        createDefaultGson()
) {

    fun read(
        inputFile: File
    ): CanonicalCatalogExpansionSemanticValidationResult {
        require(inputFile.isFile) {
            "Semantic validation file does not exist: " +
                    inputFile.absolutePath
        }

        require(inputFile.length() > 0L)

        return requireNotNull(
            inputFile.reader(Charsets.UTF_8)
                .use { reader ->
                    gson.fromJson(
                        reader,
                        CanonicalCatalogExpansionSemanticValidationResult::
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