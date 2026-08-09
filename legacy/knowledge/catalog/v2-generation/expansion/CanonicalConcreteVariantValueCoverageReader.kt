package de.shopme.testing.system.tools.knowledge.catalog.expansion.value

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class CanonicalConcreteVariantValueCoverageReader(
    private val gson: Gson =
        createDefaultGson()
) {

    fun read(
        inputFile: File
    ): CanonicalConcreteVariantValueCoverage {
        require(inputFile.isFile) {
            "Concrete variant value coverage file does not exist: " +
                    inputFile.absolutePath
        }

        require(inputFile.length() > 0L)

        return requireNotNull(
            inputFile.reader(Charsets.UTF_8)
                .use { reader ->
                    gson.fromJson(
                        reader,
                        CanonicalConcreteVariantValueCoverage::class.java
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