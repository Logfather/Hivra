package de.shopme.tools.knowledge.mapping.catalog.rebuild

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import java.io.File

data class RebuiltCatalogServerMapping(
    val catalogKey: String,
    val serverArtifact: String,
    val serverKey: String
)

data class CatalogServerArtifactMappingReport(
    val serverArtifact: String,
    val serverEntryCount: Int,
    val exactMappingCount: Int,
    val unresolvedCatalogKeyCount: Int
)

data class CatalogServerMappingRebuildReport(
    val version: Int,
    val catalogEntryCount: Int,
    val serverArtifactCount: Int,
    val totalServerEntryCount: Long,
    val exactMappingCount: Int,
    val distinctMappedCatalogKeyCount: Int,
    val catalogKeysMappedInAtLeastOneArtifact: Int,
    val catalogKeysUnmappedInEveryArtifact: Int,
    val artifacts: List<CatalogServerArtifactMappingReport>,
    val mappingFile: String,
    val unresolvedFile: String
) {

    companion object {
        const val CURRENT_VERSION =
            1
    }
}

data class CatalogServerMappingRebuildResult(
    val mappings: List<RebuiltCatalogServerMapping>,
    val report: CatalogServerMappingRebuildReport
)

class RebuildCatalogServerMappings {

    fun rebuild(
        paths: KnowledgeBuildPaths =
            KnowledgeBuildPaths.default()
    ): CatalogServerMappingRebuildResult {

        require(
            paths.canonicalFoodCatalog.isFile
        ) {
            "Canonical food catalog does not exist: " +
                    paths.canonicalFoodCatalog.absolutePath
        }

        require(
            paths.serverRoot.isDirectory
        ) {
            "Server Knowledge directory does not exist: " +
                    paths.serverRoot.absolutePath
        }

        val catalogKeys =
            readCatalogKeys(
                file =
                    paths.canonicalFoodCatalog
            )

        require(
            catalogKeys.size ==
                    KnowledgeBuildPaths.CANONICAL_CATALOG_ENTRY_COUNT
        ) {
            "Canonical catalog key count changed. " +
                    "Expected " +
                    KnowledgeBuildPaths.CANONICAL_CATALOG_ENTRY_COUNT +
                    ", found ${catalogKeys.size}."
        }

        val serverArtifacts =
            paths.serverRoot
                .listFiles()
                .orEmpty()
                .asSequence()
                .filter {
                    it.isFile
                }
                .filter {
                    it.extension.equals(
                        other = "json",
                        ignoreCase = true
                    )
                }
                .sortedBy {
                    it.name
                }
                .toList()

        require(
            serverArtifacts.isNotEmpty()
        ) {
            "No server Knowledge artifacts found in: " +
                    paths.serverRoot.absolutePath
        }

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("REBUILD CATALOG → SERVER MAPPINGS")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Catalog entries  : " +
                    catalogKeys.size
        )
        println(
            "Server artifacts : " +
                    serverArtifacts.size
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()

        val mappings =
            mutableListOf<RebuiltCatalogServerMapping>()

        val artifactReports =
            mutableListOf<CatalogServerArtifactMappingReport>()

        val mappedCatalogKeys =
            linkedSetOf<String>()

        var totalServerEntryCount =
            0L

        serverArtifacts.forEach { artifactFile ->

            val scanResult =
                scanArtifact(
                    artifactFile =
                        artifactFile,
                    catalogKeys =
                        catalogKeys
                )

            totalServerEntryCount +=
                scanResult.serverEntryCount

            scanResult.matchedKeys
                .sorted()
                .forEach { catalogKey ->

                    mappings +=
                        RebuiltCatalogServerMapping(
                            catalogKey =
                                catalogKey,
                            serverArtifact =
                                artifactFile.name,
                            serverKey =
                                catalogKey
                        )

                    mappedCatalogKeys +=
                        catalogKey
                }

            artifactReports +=
                CatalogServerArtifactMappingReport(
                    serverArtifact =
                        artifactFile.name,
                    serverEntryCount =
                        scanResult.serverEntryCount,
                    exactMappingCount =
                        scanResult.matchedKeys.size,
                    unresolvedCatalogKeyCount =
                        catalogKeys.size -
                                scanResult.matchedKeys.size
                )

            println(
                artifactFile.name
                    .padEnd(30) +
                        " server=" +
                        scanResult.serverEntryCount
                            .toString()
                            .padStart(8) +
                        " exact=" +
                        scanResult.matchedKeys.size
                            .toString()
                            .padStart(5)
            )
        }

        val sortedMappings =
            mappings
                .distinct()
                .sortedWith(
                    compareBy<RebuiltCatalogServerMapping> {
                        it.catalogKey
                    }
                        .thenBy {
                            it.serverArtifact
                        }
                        .thenBy {
                            it.serverKey
                        }
                )

        validateMappings(
            mappings =
                sortedMappings
        )

        val unresolvedEverywhere =
            catalogKeys
                .minus(
                    mappedCatalogKeys
                )
                .sorted()

        writeMappings(
            mappings =
                sortedMappings,
            file =
                paths.catalogServerMappings
        )

        val unresolvedFile =
            paths.reportsRoot.resolve(
                "catalog-server-mapping-unresolved.json"
            )

        writeUnresolved(
            catalogKeys =
                unresolvedEverywhere,
            file =
                unresolvedFile
        )

        val report =
            CatalogServerMappingRebuildReport(
                version =
                    CatalogServerMappingRebuildReport
                        .CURRENT_VERSION,
                catalogEntryCount =
                    catalogKeys.size,
                serverArtifactCount =
                    serverArtifacts.size,
                totalServerEntryCount =
                    totalServerEntryCount,
                exactMappingCount =
                    sortedMappings.size,
                distinctMappedCatalogKeyCount =
                    mappedCatalogKeys.size,
                catalogKeysMappedInAtLeastOneArtifact =
                    mappedCatalogKeys.size,
                catalogKeysUnmappedInEveryArtifact =
                    unresolvedEverywhere.size,
                artifacts =
                    artifactReports.sortedBy {
                        it.serverArtifact
                    },
                mappingFile =
                    paths.catalogServerMappings.path,
                unresolvedFile =
                    unresolvedFile.path
            )

        val reportFile =
            paths.reportsRoot.resolve(
                "catalog-server-mapping-rebuild-report.json"
            )

        writeJson(
            value =
                report,
            file =
                reportFile
        )

        printReport(
            report =
                report,
            reportFile =
                reportFile
        )

        return CatalogServerMappingRebuildResult(
            mappings =
                sortedMappings,
            report =
                report
        )
    }

    private fun readCatalogKeys(
        file: File
    ): Set<String> {

        val root =
            JsonParser
                .parseString(
                    file.readText()
                )

        require(root.isJsonArray) {
            "Canonical catalog root must be an array: " +
                    file.absolutePath
        }

        val keys =
            root
                .asJsonArray
                .map { element ->

                    require(
                        element.isJsonObject
                    ) {
                        "Canonical catalog entry must be an object."
                    }

                    requireString(
                        json =
                            element.asJsonObject,
                        key =
                            "normalized"
                    )
                }

        require(
            keys.distinct().size ==
                    keys.size
        ) {
            "Canonical catalog contains duplicate normalized keys."
        }

        return keys.toSortedSet()
    }

    private fun scanArtifact(
        artifactFile: File,
        catalogKeys: Set<String>
    ): ArtifactScanResult {

        val matchedKeys =
            linkedSetOf<String>()

        var serverEntryCount =
            0

        artifactFile
            .bufferedReader()
            .use { bufferedReader ->

                JsonReader(
                    bufferedReader
                ).use { reader ->

                    reader.beginObject()

                    var entriesFound =
                        false

                    while (
                        reader.hasNext()
                    ) {

                        when (
                            reader.nextName()
                        ) {

                            "entries" -> {

                                entriesFound =
                                    true

                                reader.beginObject()

                                while (
                                    reader.hasNext()
                                ) {

                                    val serverKey =
                                        reader.nextName()

                                    serverEntryCount++

                                    if (
                                        serverKey in
                                        catalogKeys
                                    ) {
                                        matchedKeys +=
                                            serverKey
                                    }

                                    /*
                                     * Der Mapping-Rebuild benötigt
                                     * ausschließlich den Server-Key.
                                     *
                                     * Auch sehr große Knowledge-
                                     * Dimensionen werden deshalb
                                     * vollständig gestreamt und nie
                                     * als JSON-DOM materialisiert.
                                     */
                                    reader.skipValue()
                                }

                                reader.endObject()
                            }

                            else -> {
                                reader.skipValue()
                            }
                        }
                    }

                    reader.endObject()

                    require(
                        entriesFound
                    ) {
                        "Server Knowledge artifact does not contain " +
                                "'entries': " +
                                artifactFile.absolutePath
                    }
                }
            }

        return ArtifactScanResult(
            serverEntryCount =
                serverEntryCount,
            matchedKeys =
                matchedKeys
        )
    }

    private fun validateMappings(
        mappings:
        List<RebuiltCatalogServerMapping>
    ) {

        val duplicateTuples =
            mappings
                .groupBy {
                    Triple(
                        it.catalogKey,
                        it.serverArtifact,
                        it.serverKey
                    )
                }
                .filterValues {
                    it.size > 1
                }

        require(
            duplicateTuples.isEmpty()
        ) {
            "Duplicate Catalog→Server mapping tuples detected."
        }

        val conflictingMappings =
            mappings
                .groupBy {
                    it.catalogKey to
                            it.serverArtifact
                }
                .filterValues { values ->
                    values
                        .map {
                            it.serverKey
                        }
                        .distinct()
                        .size > 1
                }

        require(
            conflictingMappings.isEmpty()
        ) {
            buildString {
                appendLine(
                    "Conflicting exact Catalog→Server mappings detected."
                )

                conflictingMappings
                    .toSortedMap(
                        compareBy<Pair<String, String>> {
                            it.first
                        }
                            .thenBy {
                                it.second
                            }
                    )
                    .forEach { (key, values) ->

                        appendLine(
                            "${key.first} / ${key.second}: " +
                                    values
                                        .map {
                                            it.serverKey
                                        }
                                        .sorted()
                                        .joinToString()
                        )
                    }
            }
        }
    }

    private fun writeMappings(
        mappings:
        List<RebuiltCatalogServerMapping>,
        file: File
    ) {

        val payload =
            linkedMapOf(
                "version" to 1,
                "mappings" to mappings
            )

        writeJson(
            value =
                payload,
            file =
                file
        )
    }

    private fun writeUnresolved(
        catalogKeys: List<String>,
        file: File
    ) {

        val payload =
            linkedMapOf(
                "version" to 1,
                "catalogKeyCount" to
                        catalogKeys.size,
                "catalogKeys" to
                        catalogKeys
            )

        writeJson(
            value =
                payload,
            file =
                file
        )
    }

    private fun writeJson(
        value: Any,
        file: File
    ) {

        val parentDirectory =
            requireNotNull(
                file.parentFile
            ) {
                "Output file has no parent directory: " +
                        file.absolutePath
            }

        require(
            parentDirectory.exists() ||
                    parentDirectory.mkdirs()
        ) {
            "Could not create output directory: " +
                    parentDirectory.absolutePath
        }

        file.writeText(
            gson.toJson(
                value
            ) + "\n"
        )
    }

    private fun requireString(
        json: JsonObject,
        key: String
    ): String =
        requireNotNull(
            json
                .get(key)
                ?.takeIf {
                    it.isJsonPrimitive
                }
                ?.asString
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
        ) {
            "Missing or blank '$key' in canonical catalog."
        }

    private fun printReport(
        report:
        CatalogServerMappingRebuildReport,
        reportFile: File
    ) {

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("CATALOG → SERVER MAPPING REBUILD RESULT")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Catalog entries          : " +
                    report.catalogEntryCount
        )
        println(
            "Server artifacts         : " +
                    report.serverArtifactCount
        )
        println(
            "Server entries scanned   : " +
                    report.totalServerEntryCount
        )
        println(
            "Exact mappings           : " +
                    report.exactMappingCount
        )
        println(
            "Mapped catalog keys      : " +
                    report.distinctMappedCatalogKeyCount
        )
        println(
            "Globally unresolved      : " +
                    report.catalogKeysUnmappedInEveryArtifact
        )
        println(
            "Mapping file             : " +
                    report.mappingFile
        )
        println(
            "Report                   : " +
                    reportFile.path
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
    }

    private data class ArtifactScanResult(
        val serverEntryCount: Int,
        val matchedKeys: Set<String>
    )

    companion object {

        private val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
    }
}