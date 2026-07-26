package de.shopme.tools.knowledge.off.nutrition.reference.retrieval

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

class CatalogOFFNutritionRetrievalItemReader {

    fun read(
        inputFile: File
    ): List<CatalogOFFNutritionRetrievalItem> {

        require(inputFile.isFile) {
            "Catalog file not found: ${inputFile.absolutePath}"
        }

        val root =
            inputFile
                .reader()
                .use(JsonParser::parseReader)

        require(root.isJsonArray) {
            "Expected catalog.json root to be an array: " +
                    inputFile.absolutePath
        }

        val result =
            root
                .asJsonArray
                .mapIndexed { index, element ->
                    require(element.isJsonObject) {
                        "Catalog entry at index $index is not an object."
                    }

                    readItem(
                        item = element.asJsonObject,
                        catalogIndex = index
                    )
                }

        require(
            result.map { item ->
                item.catalogIndex
            } ==
                    result.indices.toList()
        ) {
            "Catalog indexes must be contiguous and preserve source order."
        }

        return result
    }

    private fun readItem(
        item: JsonObject,
        catalogIndex: Int
    ): CatalogOFFNutritionRetrievalItem {

        val itemName =
            item.requiredString(
                fieldName = "itemname",
                catalogIndex = catalogIndex
            )

        val normalized =
            item.requiredString(
                fieldName = "normalized",
                catalogIndex = catalogIndex
            )

        val normalizedEnglish =
            item.requiredString(
                fieldName = "normalizedEnglish",
                catalogIndex = catalogIndex
            )

        val category =
            item.optionalString(
                fieldName = "category"
            )

        val production =
            item.optionalString(
                fieldName = "production"
            )

        val retrievalTerms =
            buildSet {
                add(normalizedEnglish)
                add(itemName)
                add(normalized)

                item.optionalString(
                    fieldName = "plural"
                )
                    ?.let(::add)

                item.stringArray(
                    fieldName = "colloquial",
                    catalogIndex = catalogIndex
                )
                    .forEach(::add)

                item.stringArray(
                    fieldName = "phonetic_tokens",
                    catalogIndex = catalogIndex
                )
                    .forEach(::add)
            }
                .asSequence()
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct()
                .sorted()
                .toList()

        return CatalogOFFNutritionRetrievalItem(
            catalogIndex =
                catalogIndex,
            catalogKey =
                normalized,
            normalizedEnglish =
                normalizedEnglish,
            itemName =
                itemName,
            category =
                category,
            production =
                production,
            retrievalTerms =
                retrievalTerms
        )
    }

    private fun JsonObject.requiredString(
        fieldName: String,
        catalogIndex: Int
    ): String {

        val value =
            get(fieldName)

        require(value != null) {
            "Catalog entry at index $catalogIndex is missing required " +
                    "field '$fieldName'. Actual fields=${keySet().sorted()}."
        }

        require(
            value.isJsonPrimitive &&
                    value.asJsonPrimitive.isString
        ) {
            "Catalog field '$fieldName' at index $catalogIndex " +
                    "must be a string."
        }

        return value
            .asString
            .trim()
            .also { stringValue ->
                require(stringValue.isNotBlank()) {
                    "Catalog field '$fieldName' at index $catalogIndex " +
                            "must not be blank."
                }
            }
    }

    private fun JsonObject.optionalString(
        fieldName: String
    ): String? {

        val value =
            get(fieldName)
                ?: return null

        require(
            value.isJsonPrimitive &&
                    value.asJsonPrimitive.isString
        ) {
            "Catalog field '$fieldName' must be a string when present."
        }

        return value
            .asString
            .trim()
            .takeIf(String::isNotBlank)
    }

    private fun JsonObject.stringArray(
        fieldName: String,
        catalogIndex: Int
    ): List<String> {

        val value =
            get(fieldName)
                ?: return emptyList()

        require(value.isJsonArray) {
            "Catalog field '$fieldName' at index $catalogIndex " +
                    "must be an array."
        }

        return value
            .asJsonArray
            .mapIndexedNotNull { elementIndex, element ->
                require(
                    element.isJsonPrimitive &&
                            element.asJsonPrimitive.isString
                ) {
                    "Catalog field '$fieldName' at catalog index " +
                            "$catalogIndex contains a non-string value at " +
                            "array index $elementIndex."
                }

                element
                    .asString
                    .trim()
                    .takeIf(String::isNotBlank)
            }
    }
}