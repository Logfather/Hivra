package de.shopme.testing.system.tools.knowledge.catalog.baseline

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class CanonicalFoodCatalogBaselineReader(
    private val gson: Gson =
        createDefaultGson()
) {

    fun read(
        inputFile: File
    ): CanonicalFoodCatalogBaseline {
        require(inputFile.isFile) {
            "Catalog baseline file does not exist: " +
                    inputFile.absolutePath
        }

        require(inputFile.length() > 0L)

        val baseline =
            inputFile.reader(Charsets.UTF_8)
                .use { reader ->
                    gson.fromJson(
                        reader,
                        CanonicalFoodCatalogBaseline::class.java
                    )
                }

        return requireNotNull(baseline) {
            "Could not parse canonical food catalog baseline."
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