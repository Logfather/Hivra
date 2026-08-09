package de.shopme.testing.system.tools.knowledge.catalog.expansion.target

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class CanonicalCatalogTargetDerivationReader(
    private val gson: Gson =
        createDefaultGson()
) {

    fun read(
        inputFile: File
    ): CanonicalCatalogTargetDerivation {
        require(inputFile.isFile) {
            "Catalog target derivation file does not exist: " +
                    inputFile.absolutePath
        }

        require(inputFile.length() > 0L)

        return requireNotNull(
            inputFile.reader(Charsets.UTF_8)
                .use { reader ->
                    gson.fromJson(
                        reader,
                        CanonicalCatalogTargetDerivation::class.java
                    )
                }
        ) {
            "Could not parse canonical catalog target derivation."
        }
    }

    private companion object {
        fun createDefaultGson(): Gson =
            GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .create()
    }
}