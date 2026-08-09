package de.shopme.testing.system.tools.knowledge.catalog.reader

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.catalog.model.CatalogFoodItem
import de.shopme.testing.system.tools.knowledge.catalog.model.IndexedCatalogFoodItem
import java.io.File
import java.nio.charset.StandardCharsets

class CatalogFoodItemReader(
    private val gson: Gson = createDefaultGson()
) {

    fun read(
        file: File
    ): List<IndexedCatalogFoodItem> {
        validateInputFile(file)

        val rootElement = parseJson(file)

        require(rootElement.isJsonArray) {
            "Catalog JSON root must be an array: ${file.absolutePath}"
        }

        val jsonArray = rootElement.asJsonArray

        return jsonArray.mapIndexed { sourceIndex, element ->
            readEntry(
                sourceIndex = sourceIndex,
                element = element,
                sourceFile = file
            )
        }
    }

    private fun readEntry(
        sourceIndex: Int,
        element: JsonElement,
        sourceFile: File
    ): IndexedCatalogFoodItem {
        require(element.isJsonObject) {
            "Catalog entry at sourceIndex $sourceIndex must be a JSON " +
                    "object in ${sourceFile.absolutePath}."
        }

        val jsonObject = element.asJsonObject

        validateRequiredJsonFields(
            sourceIndex = sourceIndex,
            jsonObject = jsonObject,
            sourceFile = sourceFile
        )

        val parsedItem = try {
            gson.fromJson(
                jsonObject,
                CatalogFoodItem::class.java
            )
        } catch (exception: JsonParseException) {
            throw IllegalArgumentException(
                "Could not deserialize catalog entry at sourceIndex " +
                        "$sourceIndex in ${sourceFile.absolutePath}: " +
                        exception.message,
                exception
            )
        } catch (exception: RuntimeException) {
            throw IllegalArgumentException(
                "Could not read catalog entry at sourceIndex " +
                        "$sourceIndex in ${sourceFile.absolutePath}: " +
                        exception.message,
                exception
            )
        }

        requireNotNull(parsedItem) {
            "Catalog entry at sourceIndex $sourceIndex deserialized to null."
        }

        val sanitizedItem = sanitizeItem(
            sourceIndex = sourceIndex,
            item = parsedItem
        )

        return IndexedCatalogFoodItem(
            sourceIndex = sourceIndex,
            item = sanitizedItem
        )
    }

    private fun sanitizeItem(
        sourceIndex: Int,
        item: CatalogFoodItem
    ): CatalogFoodItem {
        val itemName = requireNotNullSafely(
            valueProvider = { item.itemname },
            message =
                "Catalog itemname must not be null at sourceIndex " +
                        "$sourceIndex."
        )

        /*
         * Gson may assign null to Kotlin non-null properties when the JSON
         * explicitly contains null or omits a field. Access each list through
         * a guarded provider and convert absent lists to empty lists.
         */
        val colloquial = nullableProperty {
            item.colloquial
        }.orEmpty()

        val phoneticTokens = nullableProperty {
            item.phoneticTokens
        }.orEmpty()

        val autocompleteTokens = nullableProperty {
            item.autocompleteTokens
        }.orEmpty()

        return item.copy(
            itemname = itemName,
            colloquial = colloquial.toList(),
            phoneticTokens = phoneticTokens.toList(),
            autocompleteTokens = autocompleteTokens.toList()
        )
    }

    private fun validateRequiredJsonFields(
        sourceIndex: Int,
        jsonObject: JsonObject,
        sourceFile: File
    ) {
        require(jsonObject.has(JSON_FIELD_ITEM_NAME)) {
            "Catalog entry at sourceIndex $sourceIndex is missing required " +
                    "field '$JSON_FIELD_ITEM_NAME' in " +
                    sourceFile.absolutePath
        }

        val itemNameElement = jsonObject.get(JSON_FIELD_ITEM_NAME)

        require(
            itemNameElement != null &&
                    !itemNameElement.isJsonNull &&
                    itemNameElement.isJsonPrimitive &&
                    itemNameElement.asJsonPrimitive.isString
        ) {
            "Catalog field '$JSON_FIELD_ITEM_NAME' must be a string at " +
                    "sourceIndex $sourceIndex in ${sourceFile.absolutePath}."
        }

        validateOptionalStringField(
            sourceIndex = sourceIndex,
            jsonObject = jsonObject,
            fieldNames = listOf(
                JSON_FIELD_CATEGORY
            ),
            sourceFile = sourceFile
        )

        validateOptionalStringField(
            sourceIndex = sourceIndex,
            jsonObject = jsonObject,
            fieldNames = listOf(
                JSON_FIELD_NORMALIZED
            ),
            sourceFile = sourceFile
        )

        validateOptionalStringField(
            sourceIndex = sourceIndex,
            jsonObject = jsonObject,
            fieldNames = listOf(
                JSON_FIELD_PLURAL
            ),
            sourceFile = sourceFile
        )

        validateOptionalStringField(
            sourceIndex = sourceIndex,
            jsonObject = jsonObject,
            fieldNames = listOf(
                JSON_FIELD_NORMALIZED_ENGLISH,
                JSON_FIELD_NORMALIZED_ENGLISH_CAMEL_CASE
            ),
            sourceFile = sourceFile
        )

        validateOptionalStringArrayField(
            sourceIndex = sourceIndex,
            jsonObject = jsonObject,
            fieldNames = listOf(
                JSON_FIELD_COLLOQUIAL
            ),
            sourceFile = sourceFile
        )

        validateOptionalStringArrayField(
            sourceIndex = sourceIndex,
            jsonObject = jsonObject,
            fieldNames = listOf(
                JSON_FIELD_PHONETIC_TOKENS,
                JSON_FIELD_PHONETIC_TOKENS_CAMEL_CASE
            ),
            sourceFile = sourceFile
        )

        validateOptionalStringArrayField(
            sourceIndex = sourceIndex,
            jsonObject = jsonObject,
            fieldNames = listOf(
                JSON_FIELD_AUTOCOMPLETE_TOKENS,
                JSON_FIELD_AUTOCOMPLETE_TOKENS_CAMEL_CASE
            ),
            sourceFile = sourceFile
        )
    }

    private fun validateOptionalStringField(
        sourceIndex: Int,
        jsonObject: JsonObject,
        fieldNames: List<String>,
        sourceFile: File
    ) {
        val presentField = fieldNames.firstOrNull(jsonObject::has)
            ?: return

        val element = jsonObject.get(presentField)

        require(
            element == null ||
                    element.isJsonNull ||
                    (
                            element.isJsonPrimitive &&
                                    element.asJsonPrimitive.isString
                            )
        ) {
            "Catalog field '$presentField' must be a string or null at " +
                    "sourceIndex $sourceIndex in ${sourceFile.absolutePath}."
        }
    }

    private fun validateOptionalStringArrayField(
        sourceIndex: Int,
        jsonObject: JsonObject,
        fieldNames: List<String>,
        sourceFile: File
    ) {
        val presentField = fieldNames.firstOrNull(jsonObject::has)
            ?: return

        val element = jsonObject.get(presentField)

        if (element == null || element.isJsonNull) {
            return
        }

        require(element.isJsonArray) {
            "Catalog field '$presentField' must be an array or null at " +
                    "sourceIndex $sourceIndex in ${sourceFile.absolutePath}."
        }

        element.asJsonArray.forEachIndexed { elementIndex, arrayElement ->
            require(
                !arrayElement.isJsonNull &&
                        arrayElement.isJsonPrimitive &&
                        arrayElement.asJsonPrimitive.isString
            ) {
                "Catalog field '$presentField' must contain only strings. " +
                        "Invalid element at sourceIndex $sourceIndex, array " +
                        "position $elementIndex in ${sourceFile.absolutePath}."
            }
        }
    }

    private fun validateInputFile(
        file: File
    ) {
        require(file.path.isNotBlank()) {
            "Catalog file path must not be blank."
        }

        require(file.exists()) {
            "Catalog file does not exist: ${file.absolutePath}"
        }

        require(file.isFile) {
            "Catalog input path is not a file: ${file.absolutePath}"
        }

        require(file.canRead()) {
            "Catalog file is not readable: ${file.absolutePath}"
        }

        require(file.length() > 0L) {
            "Catalog file is empty: ${file.absolutePath}"
        }
    }

    private fun parseJson(
        file: File
    ): JsonElement {
        val json = try {
            file.readText(StandardCharsets.UTF_8)
        } catch (exception: RuntimeException) {
            throw IllegalArgumentException(
                "Could not read catalog file ${file.absolutePath}: " +
                        exception.message,
                exception
            )
        }

        return try {
            JsonParser.parseString(json)
        } catch (exception: JsonParseException) {
            throw IllegalArgumentException(
                "Catalog file contains invalid JSON: " +
                        "${file.absolutePath}: ${exception.message}",
                exception
            )
        } catch (exception: RuntimeException) {
            throw IllegalArgumentException(
                "Could not parse catalog JSON ${file.absolutePath}: " +
                        exception.message,
                exception
            )
        }
    }

    private fun <T : Any> requireNotNullSafely(
        valueProvider: () -> T,
        message: String
    ): T {
        val value = nullableProperty(valueProvider)

        return requireNotNull(value) {
            message
        }
    }

    private fun <T> nullableProperty(
        valueProvider: () -> T
    ): T? =
        try {
            valueProvider()
        } catch (_: NullPointerException) {
            null
        }

    private companion object {

        const val JSON_FIELD_ITEM_NAME = "itemname"
        const val JSON_FIELD_CATEGORY = "category"
        const val JSON_FIELD_NORMALIZED = "normalized"
        const val JSON_FIELD_PLURAL = "plural"
        const val JSON_FIELD_COLLOQUIAL = "colloquial"

        const val JSON_FIELD_PHONETIC_TOKENS =
            "phonetic_tokens"

        const val JSON_FIELD_PHONETIC_TOKENS_CAMEL_CASE =
            "phoneticTokens"

        const val JSON_FIELD_AUTOCOMPLETE_TOKENS =
            "autocomplete_tokens"

        const val JSON_FIELD_AUTOCOMPLETE_TOKENS_CAMEL_CASE =
            "autocompleteTokens"

        const val JSON_FIELD_NORMALIZED_ENGLISH =
            "normalizedEnglish"

        const val JSON_FIELD_NORMALIZED_ENGLISH_CAMEL_CASE =
            "normalized_english"

        fun createDefaultGson(): Gson =
            GsonBuilder()
                .disableHtmlEscaping()
                .create()
    }
}