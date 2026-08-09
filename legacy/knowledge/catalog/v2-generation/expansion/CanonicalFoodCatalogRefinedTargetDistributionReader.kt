package de.shopme.testing.system.tools.knowledge.catalog.expansion.refinement

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

class CanonicalFoodCatalogRefinedTargetDistributionReader(
    private val gson: Gson =
        createDefaultGson()
) {

    fun read(
        inputFile: File
    ): CanonicalFoodCatalogRefinedTargetDistribution {
        require(inputFile.isFile)
        require(inputFile.length() > 0L)

        return requireNotNull(
            inputFile.reader(Charsets.UTF_8)
                .use { reader ->
                    gson.fromJson(
                        reader,
                        CanonicalFoodCatalogRefinedTargetDistribution::class.java
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