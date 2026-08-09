package de.shopme.tools.knowledge.reader

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import de.shopme.domain.catalog.CatalogItem
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths

class ResourceCatalogReader : CatalogReader {

    override fun read(): List<CatalogItem> {

        val catalogFile =
            KnowledgeBuildPaths
                .default()
                .canonicalFoodCatalog

        require(catalogFile.isFile) {
            "Canonical food catalog not found: ${catalogFile.absolutePath}"
        }

        val json =
            catalogFile
                .bufferedReader()
                .use {
                    it.readText()
                }

        val type =
            object : TypeToken<List<CatalogItem>>() {}.type

        return Gson().fromJson(
            json,
            type
        )
    }
}