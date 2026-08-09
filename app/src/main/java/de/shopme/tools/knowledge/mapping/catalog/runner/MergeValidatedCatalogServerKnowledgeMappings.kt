package de.shopme.tools.knowledge.mapping.catalog.runner

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.mapping.catalog.CatalogServerKnowledgeMapping
import de.shopme.tools.knowledge.mapping.catalog.CatalogServerKnowledgeMappingConflictReport
import de.shopme.tools.knowledge.mapping.catalog.CatalogServerKnowledgeMappingMergeReport
import de.shopme.tools.knowledge.mapping.catalog.CatalogServerKnowledgeMappingMergeReportWriter
import de.shopme.tools.knowledge.mapping.catalog.CatalogServerKnowledgeMappingMerger
import de.shopme.tools.knowledge.mapping.catalog.CatalogServerKnowledgeMappingMethod
import de.shopme.tools.knowledge.mapping.catalog.CatalogServerKnowledgeMappingWriter
import de.shopme.tools.knowledge.mapping.catalog.CatalogServerKnowledgeMappings
import java.io.File

class MergeValidatedCatalogServerKnowledgeMappings(
    private val existingMappingFile: File,
    private val validatedMappingDirectory: File,
    private val outputMappingFile: File,
    private val conflictReportFile: File,
    private val mergeReportFile: File,
    private val merger:
    CatalogServerKnowledgeMappingMerger =
        CatalogServerKnowledgeMappingMerger(),
    private val mappingWriter:
    CatalogServerKnowledgeMappingWriter =
        CatalogServerKnowledgeMappingWriter(),
    private val reportWriter:
    CatalogServerKnowledgeMappingMergeReportWriter =
        CatalogServerKnowledgeMappingMergeReportWriter(),
    private val printLine: (String) -> Unit =
        ::println
) {

    fun run():
            CatalogServerKnowledgeMappingMergeReport {

        require(
            validatedMappingDirectory.isDirectory
        ) {
            "Validated mapping directory does not exist: " +
                    validatedMappingDirectory.absolutePath
        }

        val existing =
            readMappingsOrEmpty(
                file = existingMappingFile
            )

        val incomingFiles =
            validatedMappingDirectory
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

        require(incomingFiles.isNotEmpty()) {
            "No validated mapping files found in: " +
                    validatedMappingDirectory.absolutePath
        }

        val incoming =
            incomingFiles.map { file ->
                readMappings(
                    file = file
                )
            }

        val result =
            merger.merge(
                existing = existing,
                incoming = incoming
            )

        mappingWriter.write(
            mappings = result.mappings,
            file = outputMappingFile
        )

        reportWriter.writeConflictReport(
            report =
                CatalogServerKnowledgeMappingConflictReport(
                    version =
                        CatalogServerKnowledgeMappingConflictReport
                            .CURRENT_VERSION,
                    conflicts =
                        result.report.conflicts
                ),
            file =
                conflictReportFile
        )

        reportWriter.writeMergeReport(
            report = result.report,
            file = mergeReportFile
        )

        printReport(
            inputFiles = incomingFiles,
            report = result.report
        )

        return result.report
    }


    private fun readMappingsOrEmpty(
        file: File
    ): CatalogServerKnowledgeMappings {

        if (!file.isFile) {
            return CatalogServerKnowledgeMappings(
                version =
                    CatalogServerKnowledgeMappings.CURRENT_VERSION,
                mappings =
                    emptyList()
            )
        }

        return readMappings(
            file = file
        )
    }


    private fun readMappings(
        file: File
    ): CatalogServerKnowledgeMappings {

        val root =
            JsonParser
                .parseString(
                    file.readText()
                )

        require(root.isJsonObject) {
            "Mapping file must contain JSON object: " +
                    file.absolutePath
        }

        val rootObject =
            root.asJsonObject

        val version =
            rootObject["version"]
                ?.takeIf {
                    it.isJsonPrimitive &&
                            it.asJsonPrimitive.isNumber
                }
                ?.asInt
                ?: error(
                    "Missing version in ${file.absolutePath}"
                )

        require(
            version ==
                    CatalogServerKnowledgeMappings.CURRENT_VERSION
        ) {
            "Unsupported mapping version $version in " +
                    file.absolutePath
        }

        val mappings =
            rootObject["mappings"]
                ?.takeIf {
                    it.isJsonArray
                }
                ?.asJsonArray
                ?: error(
                    "Missing mappings array in " +
                            file.absolutePath
                )

        val parsedMappings =
            mappings
                .map { element ->

                    val mapping =
                        element.asJsonObject

                    CatalogServerKnowledgeMapping(
                        catalogKey =
                            mapping.requiredString(
                                key = "catalogKey"
                            ),
                        serverKey =
                            mapping.requiredString(
                                key = "serverKey"
                            ),
                        sourceArtifact =
                            mapping.requiredString(
                                key = "sourceArtifact"
                            ),
                        method =
                            CatalogServerKnowledgeMappingMethod.valueOf(
                                mapping.requiredString(
                                    key = "method"
                                )
                            ),
                        confidence =
                            mapping.requiredDouble(
                                key = "confidence"
                            ),
                        reason =
                            mapping.requiredString(
                                key = "reason"
                            )
                    )
                }
                .sortedWith(
                    CatalogServerKnowledgeMappings.MAPPING_ORDER
                )

        return CatalogServerKnowledgeMappings(
            version = version,
            mappings = parsedMappings
        )
    }


    private fun JsonObject.requiredString(
        key: String
    ): String =
        get(key)
            ?.takeIf {
                !it.isJsonNull &&
                        it.isJsonPrimitive
            }
            ?.asString
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: error(
                "Missing or blank string '$key'"
            )


    private fun JsonObject.requiredDouble(
        key: String
    ): Double {

        val value =
            get(key)

        require(
            value != null &&
                    !value.isJsonNull &&
                    value.isJsonPrimitive &&
                    value.asJsonPrimitive.isNumber
        ) {
            "Missing numeric '$key'"
        }

        return value.asDouble
    }


    private fun printReport(
        inputFiles: List<File>,
        report: CatalogServerKnowledgeMappingMergeReport
    ) {

        printLine("")
        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        printLine("CATALOG-SERVER MAPPING MERGE")
        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        printLine("Input files         : ${inputFiles.size}")
        printLine("Existing mappings   : ${report.existingMappingCount}")
        printLine("Incoming mappings   : ${report.incomingMappingCount}")
        printLine("Added mappings      : ${report.addedMappingCount}")
        printLine("Unchanged mappings  : ${report.unchangedMappingCount}")
        printLine("Conflicts           : ${report.conflictCount}")
        printLine("Total mappings      : ${report.totalMappingCount}")
        printLine("Mappings written    : ${outputMappingFile.path}")
        printLine("Conflicts written   : ${conflictReportFile.path}")
        printLine("Report written      : ${mergeReportFile.path}")
        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }


    companion object {

        @JvmStatic
        fun main(
            args: Array<String>
        ) {

            require(args.isEmpty()) {
                "MergeValidatedCatalogServerKnowledgeMappings " +
                        "does not accept arguments."
            }

            val paths =
                KnowledgeBuildPaths.default()

            paths.ensureBuildDirectories()

            MergeValidatedCatalogServerKnowledgeMappings(
                existingMappingFile =
                    paths.catalogServerMappings,
                validatedMappingDirectory =
                    paths.validatedMappingsDirectory,
                outputMappingFile =
                    paths.catalogServerMappings,
                conflictReportFile =
                    paths.catalogServerMappingConflictReport,
                mergeReportFile =
                    paths.catalogServerMappingMergeReport
            ).run()
        }
    }
}