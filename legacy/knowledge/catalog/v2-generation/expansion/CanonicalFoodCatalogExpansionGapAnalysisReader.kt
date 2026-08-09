package de.shopme.testing.system.tools.knowledge.catalog.expansion.analysis

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class CanonicalFoodCatalogExpansionGapAnalysisReader(
    private val gson: Gson =
        createDefaultGson()
) {

    fun read(
        inputFile: File
    ): CanonicalFoodCatalogExpansionGapAnalysis {
        require(inputFile.isFile) {
            "Expansion gap analysis file does not exist: " +
                    inputFile.absolutePath
        }

        require(inputFile.length() > 0L)

        val analysis =
            inputFile.reader(Charsets.UTF_8)
                .use { reader ->
                    gson.fromJson(
                        reader,
                        CanonicalFoodCatalogExpansionGapAnalysis::class.java
                    )
                }

        return requireNotNull(analysis) {
            "Could not parse canonical catalog expansion gap analysis."
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