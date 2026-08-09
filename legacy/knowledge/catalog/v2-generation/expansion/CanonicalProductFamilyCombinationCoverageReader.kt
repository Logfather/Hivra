package de.shopme.testing.system.tools.knowledge.catalog.expansion.combination

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class CanonicalProductFamilyCombinationCoverageReader(
    private val gson: Gson =
        createDefaultGson()
) {

    fun read(
        inputFile: File
    ): CanonicalProductFamilyCombinationCoverage {
        require(inputFile.isFile) {
            "Combination coverage file does not exist: " +
                    inputFile.absolutePath
        }

        require(inputFile.length() > 0L)

        return requireNotNull(
            inputFile.reader(Charsets.UTF_8)
                .use { reader ->
                    gson.fromJson(
                        reader,
                        CanonicalProductFamilyCombinationCoverage::class.java
                    )
                }
        ) {
            "Could not parse canonical combination coverage."
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