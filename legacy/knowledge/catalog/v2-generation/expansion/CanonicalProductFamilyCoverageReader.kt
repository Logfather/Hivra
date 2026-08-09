package de.shopme.testing.system.tools.knowledge.catalog.expansion.family

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class CanonicalProductFamilyCoverageReader(
    private val gson: Gson =
        createDefaultGson()
) {

    fun read(
        inputFile: File
    ): CanonicalProductFamilyCoverage {
        require(inputFile.isFile) {
            "Product-family coverage file does not exist: " +
                    inputFile.absolutePath
        }

        require(inputFile.length() > 0L)

        return requireNotNull(
            inputFile.reader(Charsets.UTF_8)
                .use { reader ->
                    gson.fromJson(
                        reader,
                        CanonicalProductFamilyCoverage::class.java
                    )
                }
        ) {
            "Could not parse canonical product-family coverage."
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