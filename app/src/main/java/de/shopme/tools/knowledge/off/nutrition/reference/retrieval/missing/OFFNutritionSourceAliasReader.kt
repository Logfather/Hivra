package de.shopme.tools.knowledge.off.nutrition.reference.retrieval.missing

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.OFFNutritionRetrievalTextNormalizer
import java.io.File

class OFFNutritionSourceAliasReader {

    fun read(
        inputFile: File
    ): Set<String> {

        require(inputFile.isFile) {
            "OFF nutrition source file not found: " +
                    inputFile.absolutePath
        }

        val root =
            inputFile
                .reader(Charsets.UTF_8)
                .use(JsonParser::parseReader)

        val aliases =
            linkedSetOf<String>()

        collectAliases(
            element = root,
            aliases = aliases
        )

        return aliases
            .asSequence()
            .map(
                OFFNutritionRetrievalTextNormalizer::normalize
            )
            .filter(String::isNotBlank)
            .toSortedSet()
    }

    private fun collectAliases(
        element: JsonElement,
        aliases: MutableSet<String>
    ) {

        when {
            element.isJsonArray -> {
                collectFromArray(
                    array = element.asJsonArray,
                    aliases = aliases
                )
            }

            element.isJsonObject -> {
                collectFromObject(
                    objectValue = element.asJsonObject,
                    aliases = aliases
                )
            }
        }
    }

    private fun collectFromArray(
        array: JsonArray,
        aliases: MutableSet<String>
    ) {

        array.forEach { child ->
            collectAliases(
                element = child,
                aliases = aliases
            )
        }
    }

    private fun collectFromObject(
        objectValue: JsonObject,
        aliases: MutableSet<String>
    ) {

        objectValue.entrySet()
            .forEach { entry ->

                when (entry.key) {
                    "serverKey",
                    "canonicalKey",
                    "alias",
                    "normalizedAlias",
                    "name",
                    "productName" -> {
                        addPrimitiveString(
                            element = entry.value,
                            aliases = aliases
                        )
                    }

                    "aliases",
                    "normalizedAliases",
                    "canonicalAliases",
                    "searchAliases" -> {
                        addStringArray(
                            element = entry.value,
                            aliases = aliases
                        )
                    }
                }

                collectAliases(
                    element = entry.value,
                    aliases = aliases
                )
            }
    }

    private fun addPrimitiveString(
        element: JsonElement,
        aliases: MutableSet<String>
    ) {

        if (
            element.isJsonPrimitive &&
            element.asJsonPrimitive.isString
        ) {
            aliases +=
                element.asString
        }
    }

    private fun addStringArray(
        element: JsonElement,
        aliases: MutableSet<String>
    ) {

        if (!element.isJsonArray) {
            return
        }

        element.asJsonArray
            .filter { child ->
                child.isJsonPrimitive &&
                        child.asJsonPrimitive.isString
            }
            .forEach { child ->
                aliases +=
                    child.asString
            }
    }
}