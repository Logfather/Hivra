package de.shopme.testing.system.tools.knowledge.catalog.expansion.approval

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import java.io.File

class CanonicalNormalizedCatalogReader(
    private val gson: Gson =
        createDefaultGson()
) {

    fun read(
        inputFile: File
    ): List<CatalogFoodItem> {
        require(inputFile.isFile) {
            "Normalized catalog file does not exist: " +
                    inputFile.absolutePath
        }

        require(inputFile.length() > 0L)

        val type =
            object :
                TypeToken<List<CatalogFoodItem>>() {
            }.type

        val items:
                List<CatalogFoodItem> =
            inputFile
                .reader(Charsets.UTF_8)
                .use { reader ->
                    requireNotNull(
                        gson.fromJson(
                            reader,
                            type
                        )
                    )
                }

        require(items.isNotEmpty())

        require(
            items.all {
                !it.normalized.isNullOrBlank()
            }
        ) {
            "Normalized catalog contains an item without normalized key."
        }

        require(
            items
                .map {
                    requireNotNull(it.normalized)
                }
                .distinct()
                .size ==
                    items.size
        ) {
            "Normalized baseline catalog contains duplicate keys."
        }

        return items
    }

    private companion object {
        fun createDefaultGson(): Gson =
            GsonBuilder()
                .disableHtmlEscaping()
                .serializeNulls()
                .create()
    }
}