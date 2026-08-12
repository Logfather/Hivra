package de.shopme.tools.knowledge.catalog.canonical.rebuild.io

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.catalog.canonical.rebuild.model.LegacyCanonicalFoodItem
import java.io.File

class LegacyCanonicalFoodCatalogReader {

    fun read(
        file: File
    ): List<LegacyCanonicalFoodItem> {

        require(file.isFile) {
            "Canonical food catalog not found: ${file.absolutePath}"
        }

        val root =
            JsonParser
                .parseString(
                    file.readText()
                )

        require(root.isJsonArray) {
            "Canonical food catalog root must be an array."
        }

        return root
            .asJsonArray
            .mapIndexed { index, element ->

                require(element.isJsonObject) {
                    "Catalog entry $index is not an object."
                }

                val json =
                    element.asJsonObject

                LegacyCanonicalFoodItem(
                    itemname =
                        json.requiredString(
                            "itemname",
                            index
                        ),

                    category =
                        json.requiredString(
                            "category",
                            index
                        ),

                    production =
                        json.optionalString(
                            "production"
                        ),

                    normalized =
                        json.requiredString(
                            "normalized",
                            index
                        ),

                    plural =
                        json.optionalString(
                            "plural"
                        ),

                    colloquial =
                        json.stringList(
                            "colloquial"
                        ),

                    phoneticTokens =
                        json.stringList(
                            "phoneticTokens"
                        ),

                    autocompleteTokens =
                        json.stringList(
                            "autocompleteTokens"
                        ),

                    normalizedEnglish =
                        json.optionalString(
                            "normalizedEnglish"
                        )
                )
            }
    }

    private fun JsonObject.requiredString(
        key: String,
        index: Int
    ): String =
        optionalString(key)
            ?: error(
                "Catalog entry $index has no '$key'."
            )

    private fun JsonObject.optionalString(
        key: String
    ): String? =
        get(key)
            ?.takeUnless {
                it.isJsonNull
            }
            ?.takeIf {
                it.isJsonPrimitive
            }
            ?.asString
            ?.trim()
            ?.takeIf {
                it.isNotBlank()
            }

    private fun JsonObject.stringList(
        key: String
    ): List<String> =
        get(key)
            ?.takeIf {
                it.isJsonArray
            }
            ?.asJsonArray
            ?.mapNotNull {
                it
                    .takeIf {
                            value ->
                        value.isJsonPrimitive
                    }
                    ?.asString
                    ?.trim()
                    ?.takeIf(
                        String::isNotBlank
                    )
            }
            .orEmpty()
}