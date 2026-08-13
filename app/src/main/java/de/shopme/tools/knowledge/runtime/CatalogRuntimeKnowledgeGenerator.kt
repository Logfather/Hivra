package de.shopme.tools.knowledge.runtime

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.io.File

class CatalogRuntimeKnowledgeGenerator(
    private val gson: Gson =
        GsonBuilder()
            .disableHtmlEscaping()
            .create(),
    private val printLine: (String) -> Unit =
        ::println
) {

    private companion object {

        const val MAX_DIAGNOSTIC_KEYS =
            25
    }

    fun generate(
        catalogFile: File,
        serverArtifactDirectory: File,
        runtimeArtifactDirectory: File,
        catalogServerMappingFile: File?
    ): CatalogRuntimeKnowledgeGenerationReport {

        require(catalogFile.isFile) {
            "Catalog file missing: ${catalogFile.absolutePath}"
        }

        require(serverArtifactDirectory.isDirectory) {
            "Server knowledge directory missing: " +
                    serverArtifactDirectory.absolutePath
        }

        ensureDirectoryExists(
            directory =
                runtimeArtifactDirectory
        )

        val catalogKeys =
            readCatalogKeys(
                file =
                    catalogFile
            )

        val mappingsByServerArtifact =
            readMappings(
                file =
                    catalogServerMappingFile
            )

        val mappingCount =
            mappingsByServerArtifact
                .values
                .sumOf {
                    it.size
                }

        val serverFiles =
            serverArtifactDirectory
                .listFiles { file ->
                    file.isFile &&
                            file.extension.equals(
                                other = "json",
                                ignoreCase = true
                            )
                }
                ?.sortedBy {
                    it.name
                }
                .orEmpty()

        require(serverFiles.isNotEmpty()) {
            "No server artifacts found in " +
                    serverArtifactDirectory.absolutePath
        }

        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        printLine("CATALOG RUNTIME KNOWLEDGE BUILD")
        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        printLine("Catalog keys=${catalogKeys.size}")
        printLine("Catalog-server mappings=$mappingCount")

        val artifactReports =
            serverFiles.map { serverFile ->

                generateArtifact(
                    serverFile =
                        serverFile,
                    runtimeDirectory =
                        runtimeArtifactDirectory,
                    catalogKeys =
                        catalogKeys,
                    mappings =
                        mappingsByServerArtifact[
                            serverFile.name
                        ].orEmpty()
                )
            }

        return CatalogRuntimeKnowledgeGenerationReport(
            catalogKeyCount =
                catalogKeys.size,
            mappingCount =
                mappingCount,
            artifacts =
                artifactReports
        )
    }


    private fun generateArtifact(
        serverFile: File,
        runtimeDirectory: File,
        catalogKeys: Set<String>,
        mappings: Map<String, String>
    ): CatalogRuntimeKnowledgeArtifactReport {

        val serverEntries =
            readEntries(
                file =
                    serverFile
            )

        val normalizedServerEntries =
            serverEntries.entries
                .associateBy(
                    keySelector = {
                        normalizeKey(
                            value =
                                it.key
                        )
                    },
                    valueTransform = {
                        it
                    }
                )

        val runtimeEntries =
            sortedMapOf<String, JsonElement>()

        var exactMatchCount =
            0

        var mappedMatchCount =
            0

        catalogKeys.forEach { catalogKey ->

            val exactEntry =
                normalizedServerEntries[
                    catalogKey
                ]

            if (exactEntry != null) {

                runtimeEntries[
                    catalogKey
                ] =
                    exactEntry.value.deepCopy()

                exactMatchCount++

                return@forEach
            }

            val mappedServerKey =
                mappings[
                    catalogKey
                ]
                    ?: return@forEach

            val mappedEntry =
                normalizedServerEntries[
                    normalizeKey(
                        value =
                            mappedServerKey
                    )
                ]
                    ?: return@forEach

            /*
             * The Runtime key remains the canonical Catalog key.
             *
             * Only the Knowledge value is resolved through the
             * artifact-specific Server mapping.
             */
            runtimeEntries[
                catalogKey
            ] =
                mappedEntry.value.deepCopy()

            mappedMatchCount++
        }

        val runtimeFile =
            runtimeDirectory.resolve(
                serverFile.name
            )

        val outputRoot =
            JsonObject().apply {

                addProperty(
                    "version",
                    1
                )

                add(
                    "entries",
                    JsonObject().apply {

                        runtimeEntries
                            .forEach { (key, value) ->

                                add(
                                    key,
                                    value
                                )
                            }
                    }
                )
            }

        runtimeFile.writeText(
            gson.toJson(
                outputRoot
            )
        )

        val missingCatalogKeys =
            catalogKeys
                .filterNot {
                    runtimeEntries.containsKey(
                        it
                    )
                }
                .sorted()

        printLine(
            "${serverFile.name}: " +
                    "runtime=${runtimeEntries.size}, " +
                    "exact=$exactMatchCount, " +
                    "mapped=$mappedMatchCount, " +
                    "missing=${missingCatalogKeys.size}"
        )

        if (missingCatalogKeys.isNotEmpty()) {

            printLine(
                "  missing sample: " +
                        missingCatalogKeys
                            .take(
                                MAX_DIAGNOSTIC_KEYS
                            )
                            .joinToString()
            )
        }

        return CatalogRuntimeKnowledgeArtifactReport(
            artifact =
                serverFile.name,
            serverEntryCount =
                serverEntries.size,
            exactMatchCount =
                exactMatchCount,
            mappedMatchCount =
                mappedMatchCount,
            runtimeEntryCount =
                runtimeEntries.size,
            missingCount =
                missingCatalogKeys.size,
            runtimeFile =
                runtimeFile.path
        )
    }


    private fun readCatalogKeys(
        file: File
    ): Set<String> {

        val type =
            object :
                TypeToken<List<JsonObject>>() {
            }.type

        val items =
            gson.fromJson<List<JsonObject>>(
                file.readText(),
                type
            )

        require(
            items.isNotEmpty()
        ) {
            "Canonical catalog contains no entries: " +
                    file.absolutePath
        }

        val catalogKeys =
            items.mapIndexed { index, item ->

                val canonicalIdentity =
                    item.requiredString(
                        key = "normalized"
                    )

                val catalogKey =
                    normalizeKey(
                        value =
                            canonicalIdentity
                    )

                require(
                    catalogKey.isNotBlank()
                ) {
                    "Canonical catalog entry at index $index " +
                            "has an empty normalized identity."
                }

                catalogKey
            }

        val duplicateCatalogKeys =
            catalogKeys
                .groupingBy {
                    it
                }
                .eachCount()
                .filterValues { count ->
                    count > 1
                }
                .keys
                .sorted()

        require(
            duplicateCatalogKeys.isEmpty()
        ) {
            "Canonical catalog contains duplicate normalized identities: " +
                    duplicateCatalogKeys.joinToString()
        }

        require(
            catalogKeys.size ==
                    items.size
        ) {
            "Canonical catalog identity count differs from entry count: " +
                    "entries=${items.size}, identities=${catalogKeys.size}"
        }

        return catalogKeys
            .toSortedSet()
    }


    private fun readMappings(
        file: File?
    ): Map<String, Map<String, String>> {

        if (
            file == null ||
            !file.isFile
        ) {
            return emptyMap()
        }

        val root =
            JsonParser.parseString(
                file.readText()
            )

        require(root.isJsonObject) {
            "Catalog-server mapping file must contain a JSON object: " +
                    file.absolutePath
        }

        val mappings =
            root.asJsonObject["mappings"]
                ?.takeIf {
                    it.isJsonArray
                }
                ?.asJsonArray
                ?: return emptyMap()

        data class ParsedMapping(
            val catalogKey: String,
            val serverArtifact: String,
            val serverKey: String
        )

        val parsedMappings =
            mappings.map { element ->

                val mapping =
                    element.asJsonObject

                ParsedMapping(
                    catalogKey =
                        normalizeKey(
                            value =
                                mapping.requiredString(
                                    key = "catalogKey"
                                )
                        ),
                    serverArtifact =
                        mapping.requiredString(
                            key = "serverArtifact"
                        ),
                    serverKey =
                        mapping.requiredString(
                            key = "serverKey"
                        )
                )
            }

        val duplicateMappings =
            parsedMappings
                .groupingBy { mapping ->
                    mapping.serverArtifact to
                            mapping.catalogKey
                }
                .eachCount()
                .filterValues { count ->
                    count > 1
                }
                .keys

        require(
            duplicateMappings.isEmpty()
        ) {
            "Duplicate catalog-server mappings: " +
                    duplicateMappings
                        .sortedWith(
                            compareBy<Pair<String, String>>(
                                { it.first },
                                { it.second }
                            )
                        )
                        .joinToString { duplicate ->
                            "${duplicate.first}:${duplicate.second}"
                        }
        }

        return parsedMappings
            .groupBy {
                it.serverArtifact
            }
            .mapValues { (_, artifactMappings) ->

                artifactMappings
                    .associate { mapping ->
                        mapping.catalogKey to
                                mapping.serverKey
                    }
                    .toSortedMap()
            }
            .toSortedMap()
    }


    private fun readEntries(
        file: File
    ): Map<String, JsonElement> {

        val root =
            JsonParser.parseString(
                file.readText()
            )

        require(root.isJsonObject) {
            "Server artifact must contain a JSON object: " +
                    file.absolutePath
        }

        val entries =
            root.asJsonObject["entries"]
                ?.takeIf {
                    it.isJsonObject
                }
                ?.asJsonObject
                ?: return emptyMap()

        return entries
            .entrySet()
            .associate { entry ->
                entry.key to entry.value
            }
    }


    private fun writeRuntimeArtifact(
        entries: Map<String, JsonElement>,
        file: File
    ) {

        ensureDirectoryExists(
            directory =
                requireNotNull(
                    file.parentFile
                ) {
                    "Runtime artifact has no parent directory: " +
                            file.absolutePath
                }
        )

        val root =
            JsonObject()

        val entryObject =
            JsonObject()

        entries
            .toSortedMap()
            .forEach { (key, value) ->
                entryObject.add(
                    key,
                    value
                )
            }

        root.add(
            "entries",
            entryObject
        )

        file.writeText(
            gson.toJson(root)
        )
    }


    private fun ensureDirectoryExists(
        directory: File
    ) {

        if (!directory.exists()) {
            check(directory.mkdirs()) {
                "Could not create directory: " +
                        directory.absolutePath
            }
        }

        require(directory.isDirectory) {
            "Path is not a directory: " +
                    directory.absolutePath
        }
    }


    private fun JsonObject.string(
        key: String
    ): String? =
        get(key)
            ?.takeIf {
                !it.isJsonNull &&
                        it.isJsonPrimitive
            }
            ?.asString
            ?.trim()
            ?.takeIf(String::isNotBlank)


    private fun JsonObject.requiredString(
        key: String
    ): String =
        string(key)
            ?: error(
                "Missing or blank string '$key'"
            )


    private fun normalizeKey(
        value: String
    ): String =
        value
            .trim()
            .lowercase()
            .replace("-", " ")
            .replace("_", " ")
            .collapseWhitespace()
            .trim()


    private fun String.collapseWhitespace():
            String {

        val builder =
            StringBuilder(length)

        var previousWasWhitespace =
            false

        for (char in this) {

            if (char.isWhitespace()) {

                if (!previousWasWhitespace) {
                    builder.append(' ')
                }

                previousWasWhitespace =
                    true

            } else {

                builder.append(char)

                previousWasWhitespace =
                    false
            }
        }

        return builder.toString()
    }
}

private data class RuntimeNutritionUnresolvedMapping(
    val catalogKey: String,
    val mappedServerKey: String,
    val normalizedMappedServerKey: String
) {

    init {
        require(catalogKey.isNotBlank()) {
            "catalogKey must not be blank."
        }

        require(mappedServerKey.isNotBlank()) {
            "mappedServerKey must not be blank."
        }

        require(normalizedMappedServerKey.isNotBlank()) {
            "normalizedMappedServerKey must not be blank."
        }
    }
}


data class CatalogRuntimeKnowledgeGenerationReport(
    val catalogKeyCount: Int,
    val mappingCount: Int,
    val artifacts: List<CatalogRuntimeKnowledgeArtifactReport>
)


data class CatalogRuntimeKnowledgeArtifactReport(
    val artifact: String,
    val serverEntryCount: Int,
    val exactMatchCount: Int,
    val mappedMatchCount: Int,
    val runtimeEntryCount: Int,
    val missingCount: Int,
    val runtimeFile: String
)