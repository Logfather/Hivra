package de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.OFFNutritionRetrievalTextNormalizer
import java.io.File

class OFFNutritionCoverageAliasDatasetReader {

    fun read(
        inputFile: File
    ): List<OFFNutritionCoverageAliasEntry> {

        require(inputFile.isFile) {
            "Coverage alias dataset not found: " +
                    inputFile.absolutePath
        }

        val root =
            inputFile
                .reader(Charsets.UTF_8)
                .use(JsonParser::parseReader)

        val objects =
            rootObjects(root)

        return objects
            .mapIndexedNotNull { index, objectValue ->
                parseEntry(
                    objectValue = objectValue,
                    fallbackIdentity =
                        "${inputFile.name}:$index"
                )
            }
            .sortedWith(
                compareBy<OFFNutritionCoverageAliasEntry>(
                    { it.identity },
                    { it.aliases.joinToString("\u0000") }
                )
            )
    }

    private fun rootObjects(
        root: JsonElement
    ): List<JsonObject> {

        if (root.isJsonArray) {
            return root
                .asJsonArray
                .mapNotNull { element ->
                    element
                        .takeIf(JsonElement::isJsonObject)
                        ?.asJsonObject
                }
        }

        require(root.isJsonObject) {
            "Expected JSON array or object root."
        }

        val rootObject =
            root.asJsonObject

        val array =
            ROOT_ARRAY_FIELDS
                .asSequence()
                .mapNotNull(rootObject::get)
                .firstOrNull(JsonElement::isJsonArray)
                ?.asJsonArray
                ?: rootObject
                    .entrySet()
                    .asSequence()
                    .map { entry ->
                        entry.value
                    }
                    .firstOrNull(JsonElement::isJsonArray)
                    ?.asJsonArray
                ?: JsonArray()

        return array.mapNotNull { element ->
            element
                .takeIf(JsonElement::isJsonObject)
                ?.asJsonObject
        }
    }

    private fun parseEntry(
        objectValue: JsonObject,
        fallbackIdentity: String
    ): OFFNutritionCoverageAliasEntry? {

        val identity =
            IDENTITY_FIELDS
                .asSequence()
                .mapNotNull { fieldName ->
                    objectValue.string(fieldName)
                }
                .firstOrNull()
                ?: fallbackIdentity

        val aliases =
            buildSet {

                IDENTITY_FIELDS.forEach { fieldName ->
                    objectValue
                        .string(fieldName)
                        ?.let(::add)
                }

                SINGLE_ALIAS_FIELDS.forEach { fieldName ->
                    objectValue
                        .string(fieldName)
                        ?.let(::add)
                }

                ARRAY_ALIAS_FIELDS.forEach { fieldName ->
                    addAll(
                        objectValue.stringArray(
                            fieldName = fieldName
                        )
                    )
                }
            }
                .asSequence()
                .map(
                    OFFNutritionRetrievalTextNormalizer::normalize
                )
                .filter(String::isNotBlank)
                .toSortedSet()

        if (aliases.isEmpty()) {
            return null
        }

        return OFFNutritionCoverageAliasEntry(
            identity =
                identity.trim(),
            aliases =
                aliases
        )
    }

    private fun JsonObject.string(
        fieldName: String
    ): String? {

        val element =
            get(fieldName)
                ?: return null

        if (
            !element.isJsonPrimitive ||
            !element.asJsonPrimitive.isString
        ) {
            return null
        }

        return element
            .asString
            .trim()
            .takeIf(String::isNotBlank)
    }

    private fun JsonObject.stringArray(
        fieldName: String
    ): Set<String> {

        val element =
            get(fieldName)
                ?: return emptySet()

        if (!element.isJsonArray) {
            return emptySet()
        }

        return element
            .asJsonArray
            .asSequence()
            .filter(JsonElement::isJsonPrimitive)
            .map { value ->
                value.asJsonPrimitive
            }
            .filter { primitive ->
                primitive.isString
            }
            .map { primitive ->
                primitive.asString.trim()
            }
            .filter(String::isNotBlank)
            .toSortedSet()
    }

    companion object {

        private val ROOT_ARRAY_FIELDS =
            listOf(
                "candidates",
                "entries",
                "aggregates",
                "items",
                "results"
            )

        private val IDENTITY_FIELDS =
            listOf(
                "serverKey",
                "canonicalId",
                "sourceId"
            )

        private val SINGLE_ALIAS_FIELDS =
            listOf(
                "productName",
                "normalizedAlias",
                "alias"
            )

        private val ARRAY_ALIAS_FIELDS =
            listOf(
                "aliases",
                "canonicalAliases",
                "matchAliases",
                "retrievalAliases",
                "singleIngredientNutritionAliases"
            )
    }
}