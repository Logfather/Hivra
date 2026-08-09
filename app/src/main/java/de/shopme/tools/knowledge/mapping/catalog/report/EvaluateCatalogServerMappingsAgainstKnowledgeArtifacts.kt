package de.shopme.tools.knowledge.mapping.catalog.runner

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.mapping.catalog.report.CatalogServerMappingArtifactCoverageEvaluator
import de.shopme.tools.knowledge.mapping.catalog.report.CatalogServerMappingCoverageReport
import de.shopme.tools.knowledge.mapping.catalog.report.CatalogServerMappingCoverageReportWriter
import de.shopme.tools.knowledge.mapping.catalog.report.ReusableCatalogServerMapping
import java.io.File

class EvaluateCatalogServerMappingsAgainstKnowledgeArtifacts(
    private val mappingFile: File,
    private val serverArtifactDirectory: File,
    private val reportFile: File,
    private val evaluator:
    CatalogServerMappingArtifactCoverageEvaluator =
        CatalogServerMappingArtifactCoverageEvaluator(),
    private val writer:
    CatalogServerMappingCoverageReportWriter =
        CatalogServerMappingCoverageReportWriter(),
    private val printLine: (String) -> Unit =
        ::println
) {

    fun run():
            CatalogServerMappingCoverageReport {

        require(mappingFile.isFile) {
            "Catalog-server mapping file does not exist: " +
                    mappingFile.absolutePath
        }

        require(serverArtifactDirectory.isDirectory) {
            "Server artifact directory does not exist: " +
                    serverArtifactDirectory.absolutePath
        }

        val mappings =
            readMappings(
                file = mappingFile
            )

        val artifactFiles =
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

        require(artifactFiles.isNotEmpty()) {
            "No JSON server artifacts found in: " +
                    serverArtifactDirectory.absolutePath
        }

        val serverKeysByArtifact =
            artifactFiles
                .associate { artifactFile ->
                    artifactFile.name to
                            readServerKeys(
                                file = artifactFile
                            )
                }

        val report =
            evaluator.evaluate(
                mappings = mappings,
                serverKeysByArtifact =
                    serverKeysByArtifact
            )

        writer.write(
            report = report,
            file = reportFile
        )

        printReport(
            report = report
        )

        return report
    }


    private fun readMappings(
        file: File
    ): List<ReusableCatalogServerMapping> {

        val root =
            parseObject(
                file = file
            )

        val mappings =
            root["mappings"]
                ?.takeIf {
                    it.isJsonArray
                }
                ?.asJsonArray
                ?: error(
                    "Missing mappings array in " +
                            file.absolutePath
                )

        return mappings
            .map { element ->

                val mapping =
                    element.asJsonObject

                ReusableCatalogServerMapping(
                    catalogKey =
                        mapping.requiredString(
                            key = "catalogKey"
                        ),
                    serverKey =
                        mapping.requiredString(
                            key = "serverKey"
                        )
                )
            }
            .distinct()
            .sortedWith(
                ReusableCatalogServerMapping.ORDER
            )
    }


    private fun readServerKeys(
        file: File
    ): Set<String> {

        val root =
            JsonParser.parseString(
                file.readText()
            )

        val keys =
            when {
                root.isJsonObject ->
                    readKeysFromObject(
                        root = root.asJsonObject
                    )

                root.isJsonArray ->
                    root.asJsonArray
                        .mapNotNull {
                            readEntryKey(
                                element = it
                            )
                        }
                        .toSet()

                else ->
                    error(
                        "Unsupported server artifact structure: " +
                                file.absolutePath
                    )
            }

        return keys
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSet()
    }


    private fun readKeysFromObject(
        root: JsonObject
    ): Set<String> {

        val entries =
            root["entries"]

        return when {
            entries == null -> {
                root.keySet()
            }

            entries.isJsonObject -> {
                entries.asJsonObject
                    .keySet()
            }

            entries.isJsonArray -> {
                entries.asJsonArray
                    .mapNotNull {
                        readEntryKey(
                            element = it
                        )
                    }
                    .toSet()
            }

            else -> {
                error(
                    "Unsupported entries structure"
                )
            }
        }
    }


    private fun readEntryKey(
        element: JsonElement
    ): String? {

        if (!element.isJsonObject) {
            return null
        }

        val entry =
            element.asJsonObject

        return entry.optionalString("key")
            ?: entry.optionalString("id")
            ?: entry.optionalString("name")
            ?: entry.optionalString("canonicalKey")
            ?: entry.optionalString("serverKey")
    }


    private fun parseObject(
        file: File
    ): JsonObject {

        val root =
            JsonParser.parseString(
                file.readText()
            )

        require(root.isJsonObject) {
            "Expected JSON object in " +
                    file.absolutePath
        }

        return root.asJsonObject
    }


    private fun JsonObject.requiredString(
        key: String
    ): String =
        optionalString(key)
            ?: error(
                "Missing or blank string '$key'"
            )


    private fun JsonObject.optionalString(
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


    private fun printReport(
        report: CatalogServerMappingCoverageReport
    ) {

        printLine("")
        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        printLine("CATALOG-SERVER MAPPING ARTIFACT COVERAGE")
        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        printLine("Mappings  : ${report.mappingCount}")
        printLine("Artifacts : ${report.artifactCount}")
        printLine("")

        report.artifacts
            .forEach { artifact ->

                printLine(
                    artifact.artifact
                        .padEnd(30) +
                            " server=" +
                            artifact.serverKeyCount
                                .toString()
                                .padStart(7) +
                            " reusable=" +
                            artifact.reusableMappingCount
                                .toString()
                                .padStart(5) +
                            " missing=" +
                            artifact.missingMappingCount
                                .toString()
                                .padStart(5) +
                            " coverage=" +
                            "%.2f".format(
                                artifact.coveragePercent
                            ) +
                            "%"
                )
            }

        printLine("")
        printLine("Written   : ${reportFile.path}")
        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }


    companion object {

        @JvmStatic
        fun main(
            args: Array<String>
        ) {

            val paths =
                KnowledgeBuildPaths.default()

            paths.ensureBuildDirectories()

            EvaluateCatalogServerMappingsAgainstKnowledgeArtifacts(
                mappingFile =
                    paths.catalogServerMappings,
                serverArtifactDirectory =
                    paths.serverRoot,
                reportFile =
                    paths.catalogServerMappingCoverageReport
            ).run()
        }
    }
}