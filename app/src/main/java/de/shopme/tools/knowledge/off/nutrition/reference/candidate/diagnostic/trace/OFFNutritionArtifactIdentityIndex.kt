package de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.trace

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.text.Normalizer

class OFFNutritionArtifactIdentityIndex private constructor(
    private val entries: List<IndexedEntry>,
    private val entriesByIdentity: Map<String, List<IndexedEntry>>
) {

    fun match(
        identities: Set<String>
    ): OFFNutritionArtifactIdentityMatch {

        val normalizedIdentities =
            identities
                .map(::normalizeIdentity)
                .filter(String::isNotBlank)
                .toSortedSet()

        val matchedEntries =
            normalizedIdentities
                .flatMap { identity ->
                    entriesByIdentity[identity].orEmpty()
                }
                .distinctBy(IndexedEntry::stableIdentity)
                .sortedWith(
                    compareBy(
                        IndexedEntry::sourceProductId,
                        IndexedEntry::productName,
                        IndexedEntry::stableIdentity
                    )
                )

        if (matchedEntries.isEmpty()) {
            return OFFNutritionArtifactIdentityMatch.EMPTY
        }

        val matchedIdentities =
            matchedEntries
                .flatMap { entry ->
                    entry.identities
                        .intersect(normalizedIdentities)
                }
                .distinct()
                .sorted()

        return OFFNutritionArtifactIdentityMatch(
            matchedEntryCount =
                matchedEntries.size,
            matchedIdentities =
                matchedIdentities,
            matchedSourceProductIds =
                matchedEntries
                    .mapNotNull(IndexedEntry::sourceProductId)
                    .distinct()
                    .sorted(),
            matchedProductNames =
                matchedEntries
                    .mapNotNull(IndexedEntry::productName)
                    .distinct()
                    .sorted()
        )
    }

    companion object {

        fun read(
            file: File
        ): OFFNutritionArtifactIdentityIndex {

            require(file.isFile) {
                "OFF nutrition artifact not found: ${file.absolutePath}"
            }

            val root =
                file.reader().use { reader ->
                    JsonParser.parseReader(reader)
                }

            val array =
                resolveEntryArray(root)

            val entries =
                array
                    .mapIndexedNotNull { index, element ->
                        parseEntry(
                            index =
                                index,
                            element =
                                element
                        )
                    }

            val entriesByIdentity =
                mutableMapOf<String, MutableList<IndexedEntry>>()

            entries.forEach { entry ->

                entry.identities.forEach { identity ->
                    entriesByIdentity
                        .getOrPut(identity) {
                            mutableListOf()
                        }
                        .add(entry)
                }
            }

            return OFFNutritionArtifactIdentityIndex(
                entries =
                    entries,
                entriesByIdentity =
                    entriesByIdentity
                        .mapValues { (_, indexedEntries) ->
                            indexedEntries
                                .distinctBy(IndexedEntry::stableIdentity)
                                .sortedBy(IndexedEntry::stableIdentity)
                        }
                        .toSortedMap()
            )
        }

        private fun parseEntry(
            index: Int,
            element: JsonElement
        ): IndexedEntry? {

            if (!element.isJsonObject) {
                return null
            }

            val json =
                element.asJsonObject

            val sourceProductId =
                firstString(
                    json =
                        json,
                    fieldNames =
                        SOURCE_ID_FIELD_NAMES
                )

            val productName =
                firstString(
                    json =
                        json,
                    fieldNames =
                        PRODUCT_NAME_FIELD_NAMES
                )

            val identities =
                collectIdentityStrings(json)
                    .map(::normalizeIdentity)
                    .filter(String::isNotBlank)
                    .toSortedSet()

            if (identities.isEmpty()) {
                return null
            }

            val stableIdentity =
                sourceProductId
                    ?.takeIf(String::isNotBlank)
                    ?: listOfNotNull(
                        productName,
                        identities.firstOrNull()
                    )
                        .joinToString("|")
                        .ifBlank {
                            "entry-$index"
                        }

            return IndexedEntry(
                stableIdentity =
                    stableIdentity,
                sourceProductId =
                    sourceProductId,
                productName =
                    productName,
                identities =
                    identities
            )
        }

        private fun collectIdentityStrings(
            json: JsonObject
        ): Set<String> {

            val values =
                mutableSetOf<String>()

            IDENTITY_FIELD_NAMES.forEach { fieldName ->

                val value =
                    json.get(fieldName)
                        ?: return@forEach

                collectStrings(
                    element =
                        value,
                    target =
                        values
                )
            }

            val metadata =
                json.getAsJsonObject("metadata")

            if (metadata != null) {

                SOURCE_ID_FIELD_NAMES.forEach { fieldName ->
                    metadata
                        .get(fieldName)
                        ?.takeUnless(JsonElement::isJsonNull)
                        ?.takeIf(JsonElement::isJsonPrimitive)
                        ?.asString
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                        ?.let(values::add)
                }

                PRODUCT_NAME_FIELD_NAMES.forEach { fieldName ->
                    metadata
                        .get(fieldName)
                        ?.takeUnless(JsonElement::isJsonNull)
                        ?.takeIf(JsonElement::isJsonPrimitive)
                        ?.asString
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                        ?.let(values::add)
                }

                metadata
                    .getAsJsonObject("attributes")
                    ?.let { attributes ->
                        PRODUCT_NAME_FIELD_NAMES.forEach { fieldName ->
                            attributes
                                .get(fieldName)
                                ?.takeUnless(JsonElement::isJsonNull)
                                ?.takeIf(JsonElement::isJsonPrimitive)
                                ?.asString
                                ?.trim()
                                ?.takeIf(String::isNotBlank)
                                ?.let(values::add)
                        }
                    }
            }

            return values
        }

        private fun collectStrings(
            element: JsonElement,
            target: MutableSet<String>
        ) {
            when {
                element.isJsonNull ->
                    Unit

                element.isJsonPrimitive -> {
                    if (element.asJsonPrimitive.isString) {
                        element.asString
                            .trim()
                            .takeIf(String::isNotBlank)
                            ?.let(target::add)
                    }
                }

                element.isJsonArray ->
                    element.asJsonArray.forEach { child ->
                        collectStrings(
                            element =
                                child,
                            target =
                                target
                        )
                    }

                element.isJsonObject ->
                    element.asJsonObject.entrySet()
                        .sortedBy(Map.Entry<String, JsonElement>::key)
                        .forEach { (_, child) ->
                            collectStrings(
                                element =
                                    child,
                                target =
                                    target
                            )
                        }
            }
        }

        private fun firstString(
            json: JsonObject,
            fieldNames: List<String>
        ): String? {

            fieldNames.forEach { fieldName ->

                val value =
                    json
                        .get(fieldName)
                        ?.takeUnless(JsonElement::isJsonNull)
                        ?.takeIf(JsonElement::isJsonPrimitive)
                        ?.asString
                        ?.trim()
                        ?.takeIf(String::isNotBlank)

                if (value != null) {
                    return value
                }
            }

            val metadata =
                json.getAsJsonObject("metadata")

            fieldNames.forEach { fieldName ->

                val value =
                    metadata
                        ?.get(fieldName)
                        ?.takeUnless(JsonElement::isJsonNull)
                        ?.takeIf(JsonElement::isJsonPrimitive)
                        ?.asString
                        ?.trim()
                        ?.takeIf(String::isNotBlank)

                if (value != null) {
                    return value
                }
            }

            val attributes =
                metadata
                    ?.getAsJsonObject("attributes")

            fieldNames.forEach { fieldName ->

                val value =
                    attributes
                        ?.get(fieldName)
                        ?.takeUnless(JsonElement::isJsonNull)
                        ?.takeIf(JsonElement::isJsonPrimitive)
                        ?.asString
                        ?.trim()
                        ?.takeIf(String::isNotBlank)

                if (value != null) {
                    return value
                }
            }

            return null
        }

        private fun resolveEntryArray(
            root: JsonElement
        ): JsonArray {

            if (root.isJsonArray) {
                return root.asJsonArray
            }

            require(root.isJsonObject) {
                "Artifact root must be an object or array."
            }

            val rootObject =
                root.asJsonObject

            ARRAY_FIELD_NAMES.forEach { fieldName ->

                val value =
                    rootObject.get(fieldName)

                if (
                    value != null &&
                    value.isJsonArray
                ) {
                    return value.asJsonArray
                }
            }

            error(
                "Artifact object contains none of the supported arrays: " +
                        ARRAY_FIELD_NAMES.joinToString()
            )
        }

        fun normalizeIdentity(
            value: String
        ): String {

            val decomposed =
                Normalizer.normalize(
                    value,
                    Normalizer.Form.NFD
                )

            return decomposed
                .replace(COMBINING_MARK_REGEX, "")
                .replace("&quot;", " ")
                .replace("&amp;", " and ")
                .lowercase()
                .replace(NON_ALPHANUMERIC_REGEX, " ")
                .replace(WHITESPACE_REGEX, " ")
                .trim()
        }

        private val ARRAY_FIELD_NAMES =
            listOf(
                "entries",
                "candidates",
                "traces",
                "findings",
                "items"
            )

        private val SOURCE_ID_FIELD_NAMES =
            listOf(
                "sourceProductId",
                "sourceId",
                "productId",
                "code",
                "barcode"
            )

        private val PRODUCT_NAME_FIELD_NAMES =
            listOf(
                "productName",
                "name",
                "canonicalName",
                "canonicalId"
            )

        private val IDENTITY_FIELD_NAMES =
            listOf(
                "productName",
                "name",
                "canonicalName",
                "canonicalId",
                "canonicalKey",
                "aliases",
                "matchAliases",
                "normalizedProductIdentities",
                "identities",
                "sourceProductId",
                "sourceId"
            )

        private val COMBINING_MARK_REGEX =
            Regex("\\p{M}+")

        private val NON_ALPHANUMERIC_REGEX =
            Regex("[^\\p{L}\\p{N}]+")

        private val WHITESPACE_REGEX =
            Regex("\\s+")
    }

    private data class IndexedEntry(
        val stableIdentity: String,
        val sourceProductId: String?,
        val productName: String?,
        val identities: Set<String>
    )
}