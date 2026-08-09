package de.shopme.testing.system.tools.knowledge.catalog.expansion

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class CanonicalFoodCatalogTargetDistributionReader(
    private val gson: Gson =
        createDefaultGson()
) {

    fun read(
        inputFile: File
    ): CanonicalFoodCatalogTargetDistribution {
        require(inputFile.isFile) {
            "Target distribution file does not exist: " +
                    inputFile.absolutePath
        }

        require(inputFile.length() > 0L)

        val distribution =
            inputFile.reader(Charsets.UTF_8)
                .use { reader ->
                    gson.fromJson(
                        reader,
                        CanonicalFoodCatalogTargetDistribution::class.java
                    )
                }

        return requireNotNull(distribution) {
            "Could not parse canonical catalog target distribution."
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