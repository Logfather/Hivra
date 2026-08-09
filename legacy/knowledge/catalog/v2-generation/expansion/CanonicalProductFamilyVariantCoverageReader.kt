package de.shopme.testing.system.tools.knowledge.catalog.expansion.variant

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class CanonicalProductFamilyVariantCoverageReader(
    private val gson: Gson =
        createDefaultGson()
) {

    fun read(
        inputFile: File
    ): CanonicalProductFamilyVariantCoverage {
        require(inputFile.isFile) {
            "Variant coverage file does not exist: " +
                    inputFile.absolutePath
        }

        require(inputFile.length() > 0L)

        return requireNotNull(
            inputFile.reader(Charsets.UTF_8)
                .use { reader ->
                    gson.fromJson(
                        reader,
                        CanonicalProductFamilyVariantCoverage::class.java
                    )
                }
        ) {
            "Could not parse canonical product-family variant coverage."
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