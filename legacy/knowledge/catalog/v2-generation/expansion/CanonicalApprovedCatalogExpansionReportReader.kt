package de.shopme.testing.system.tools.knowledge.catalog.expansion.approval

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class CanonicalApprovedCatalogExpansionReportReader(
    private val gson: Gson =
        createDefaultGson()
) {

    fun read(
        inputFile: File
    ): CanonicalApprovedCatalogExpansionResult {
        require(inputFile.isFile)
        require(inputFile.length() > 0L)

        return requireNotNull(
            inputFile.reader(Charsets.UTF_8)
                .use { reader ->
                    gson.fromJson(
                        reader,
                        CanonicalApprovedCatalogExpansionResult::
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