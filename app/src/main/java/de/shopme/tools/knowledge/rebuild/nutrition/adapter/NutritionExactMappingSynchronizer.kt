package de.shopme.tools.knowledge.rebuild.nutrition.adapter

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

class NutritionExactMappingSynchronizer(
    private val exactMatchReportFile: File,
    private val exactMappingFile: File,
    private val serverNutritionFile: File
) {

    fun synchronize():
            NutritionExactMappingSynchronizationResult {

        require(exactMatchReportFile.isFile) {
            "Nutrition exact-match report does not exist: " +
                    exactMatchReportFile.absolutePath
        }

        require(serverNutritionFile.isFile) {
            "Nutrition server artifact does not exist: " +
                    serverNutritionFile.absolutePath
        }

        val reportedExactKeys =
            readReportedExactKeys()

        val serverKeys =
            readServerKeys()

        val existingMappings =
            readExistingMappings()

        val existingByCatalogKey =
            existingMappings.associateBy {
                normalizeKey(
                    value =
                        it.catalogKey
                )
            }

        val synchronizedMappings =
            buildList {

                addAll(
                    existingMappings
                )

                reportedExactKeys
                    .asSequence()
                    .filter {
                        it !in existingByCatalogKey
                    }
                    .forEach { catalogKey ->

                        require(
                            catalogKey in serverKeys
                        ) {
                            "Reported exact nutrition match '$catalogKey' " +
                                    "does not exist in the server nutrition " +
                                    "artifact."
                        }

                        add(
                            ExactNutritionMapping(
                                catalogKey =
                                    catalogKey,
                                serverArtifact =
                                    NUTRITION_ARTIFACT,
                                serverKey =
                                    catalogKey
                            )
                        )
                    }
            }
                .distinctBy {
                    normalizeKey(
                        value =
                            it.catalogKey
                    )
                }
                .sortedWith(
                    compareBy<ExactNutritionMapping>(
                        { it.serverArtifact },
                        { it.catalogKey },
                        { it.serverKey }
                    )
                )

        writeMappings(
            mappings =
                synchronizedMappings
        )

        val addedCatalogKeys =
            synchronizedMappings
                .map {
                    normalizeKey(
                        value =
                            it.catalogKey
                    )
                }
                .toSet()
                .minus(
                    existingByCatalogKey.keys
                )
                .sorted()

        return NutritionExactMappingSynchronizationResult(
            existingMappingCount =
                existingMappings.size,
            reportedExactMatchCount =
                reportedExactKeys.size,
            addedMappingCount =
                addedCatalogKeys.size,
            addedCatalogKeys =
                addedCatalogKeys,
            finalMappingCount =
                synchronizedMappings.size
        )
    }

    private fun readReportedExactKeys():
            Set<String> {

        val root =
            JsonParser.parseString(
                exactMatchReportFile.readText()
            )

        require(root.isJsonObject) {
            "Nutrition exact-match report must contain a JSON object."
        }

        val exactMatches =
            root.asJsonObject[
                "exactMatches"
            ]
                ?.takeIf {
                    it.isJsonArray
                }
                ?.asJsonArray
                ?: error(
                    "Nutrition exact-match report contains no " +
                            "'exactMatches' array."
                )

        return exactMatches
            .map { element ->

                require(
                    element.isJsonPrimitive &&
                            element
                                .asJsonPrimitive
                                .isString
                ) {
                    "Nutrition exactMatches must contain strings."
                }

                normalizeKey(
                    value =
                        element.asString
                )
            }
            .filter(
                String::isNotBlank
            )
            .toSortedSet()
    }

    private fun readExistingMappings():
            List<ExactNutritionMapping> {

        if (!exactMappingFile.isFile) {
            return emptyList()
        }

        val root =
            JsonParser.parseString(
                exactMappingFile.readText()
            )

        require(root.isJsonObject) {
            "Exact nutrition mapping file must contain a JSON object."
        }

        val mappings =
            root.asJsonObject[
                "mappings"
            ]
                ?.takeIf {
                    it.isJsonArray
                }
                ?.asJsonArray
                ?: error(
                    "Exact nutrition mapping file contains no mappings array."
                )

        return mappings
            .map { element ->

                require(element.isJsonObject) {
                    "Exact nutrition mapping must be a JSON object."
                }

                val objectValue =
                    element.asJsonObject

                ExactNutritionMapping(
                    catalogKey =
                        objectValue.requiredString(
                            key =
                                "catalogKey"
                        ),
                    serverArtifact =
                        objectValue.optionalString(
                            key =
                                "serverArtifact"
                        )
                            ?: NUTRITION_ARTIFACT,
                    serverKey =
                        objectValue.requiredString(
                            key =
                                "serverKey"
                        )
                )
            }
    }

    private fun readServerKeys():
            Set<String> {

        val root =
            JsonParser.parseString(
                serverNutritionFile.readText()
            )

        return when {

            root.isJsonObject -> {

                val objectValue =
                    root.asJsonObject

                when {

                    objectValue[
                        "entries"
                    ]?.isJsonObject ==
                            true ->
                        objectValue[
                            "entries"
                        ]
                            .asJsonObject
                            .keySet()

                    objectValue[
                        "entries"
                    ]?.isJsonArray ==
                            true ->
                        objectValue[
                            "entries"
                        ]
                            .asJsonArray
                            .mapNotNull { element ->

                                if (!element.isJsonObject) {
                                    return@mapNotNull null
                                }

                                val entry =
                                    element.asJsonObject

                                entry.optionalString(
                                    key =
                                        "key"
                                )
                                    ?: entry.optionalString(
                                        key =
                                            "canonicalKey"
                                    )
                                    ?: entry.optionalString(
                                        key =
                                            "id"
                                    )
                                    ?: entry.optionalString(
                                        key =
                                            "name"
                                    )
                            }
                            .toSet()

                    else ->
                        objectValue.keySet()
                }
            }

            root.isJsonArray ->
                root.asJsonArray
                    .mapNotNull { element ->

                        if (!element.isJsonObject) {
                            return@mapNotNull null
                        }

                        val entry =
                            element.asJsonObject

                        entry.optionalString(
                            key =
                                "key"
                        )
                            ?: entry.optionalString(
                                key =
                                    "canonicalKey"
                            )
                            ?: entry.optionalString(
                                key =
                                    "id"
                            )
                            ?: entry.optionalString(
                                key =
                                    "name"
                            )
                    }
                    .toSet()

            else ->
                error(
                    "Unsupported server nutrition artifact structure."
                )
        }
            .map(
                ::normalizeKey
            )
            .filter(
                String::isNotBlank
            )
            .toSortedSet()
    }

    private fun writeMappings(
        mappings: List<ExactNutritionMapping>
    ) {
        exactMappingFile.parentFile
            ?.let { parentDirectory ->

                if (!parentDirectory.exists()) {
                    check(parentDirectory.mkdirs()) {
                        "Could not create exact mapping directory: " +
                                parentDirectory.absolutePath
                    }
                }
            }

        val output =
            ExactNutritionMappings(
                version =
                    CURRENT_VERSION,
                mappings =
                    mappings
            )

        exactMappingFile.writeText(
            GSON.toJson(
                output
            ) + "\n"
        )
    }

    private fun normalizeKey(
        value: String
    ): String {

        return value
            .trim()
            .lowercase()
            .replace(
                "-",
                " "
            )
            .replace(
                "_",
                " "
            )
            .replace(
                WHITESPACE_REGEX,
                " "
            )
            .trim()
    }

    private fun JsonObject.requiredString(
        key: String
    ): String {

        return optionalString(
            key =
                key
        )
            ?: error(
                "Missing or blank string '$key'."
            )
    }

    private fun JsonObject.optionalString(
        key: String
    ): String? {

        return get(key)
            ?.takeIf {
                !it.isJsonNull &&
                        it.isJsonPrimitive &&
                        it.asJsonPrimitive.isString
            }
            ?.asString
            ?.trim()
            ?.takeIf(
                String::isNotBlank
            )
    }

    private data class ExactNutritionMappings(
        val version: Int,
        val mappings: List<ExactNutritionMapping>
    )

    private data class ExactNutritionMapping(
        val catalogKey: String,
        val serverArtifact: String,
        val serverKey: String
    )

    private companion object {

        const val CURRENT_VERSION =
            1

        const val NUTRITION_ARTIFACT =
            "nutrition.json"

        val WHITESPACE_REGEX =
            Regex("\\s+")

        val GSON =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
    }
}

data class NutritionExactMappingSynchronizationResult(
    val existingMappingCount: Int,
    val reportedExactMatchCount: Int,
    val addedMappingCount: Int,
    val addedCatalogKeys: List<String>,
    val finalMappingCount: Int
)