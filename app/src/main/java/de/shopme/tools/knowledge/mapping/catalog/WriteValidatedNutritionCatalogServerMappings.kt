package de.shopme.tools.knowledge.mapping.catalog.runner

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMappingIdentity
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMappingValidationReportWriter
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchCandidate
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchDecision
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchDecisionContract
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchDecisionSource
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchDecisionType
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchDecisionValidationResult
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchDecisionValidator
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchDecisions
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchDiagnosticsWriter
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchRequest
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchRequestContract
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchRequests
import de.shopme.tools.knowledge.mapping.catalog.CatalogServerKnowledgeMappingWriter
import java.io.File

class WriteValidatedNutritionCatalogServerMappings(
    private val requestFile: File,
    private val decisionFile: File,
    private val serverArtifactFile: File,
    private val exactMappingFile: File,
    private val outputMappingFile: File,
    private val validationReportFile: File,
    private val diagnosticsFile: File,
    private val minimumConfidence: Double = 0.80,
    private val mappingWriter:
    CatalogServerKnowledgeMappingWriter =
        CatalogServerKnowledgeMappingWriter(),
    private val validationReportWriter:
    CatalogKnowledgeMappingValidationReportWriter =
        CatalogKnowledgeMappingValidationReportWriter(),
    private val diagnosticsWriter:
    CatalogKnowledgeMatchDiagnosticsWriter =
        CatalogKnowledgeMatchDiagnosticsWriter(),
    private val printLine: (String) -> Unit = ::println
) {

    fun run():
            WriteValidatedNutritionCatalogServerMappingsResult {

        require(requestFile.isFile) {
            "Nutrition request file does not exist: " +
                    requestFile.absolutePath
        }

        require(decisionFile.isFile) {
            "Nutrition decision file does not exist: " +
                    decisionFile.absolutePath
        }

        require(serverArtifactFile.isFile) {
            "Nutrition server artifact does not exist: " +
                    serverArtifactFile.absolutePath
        }

        require(minimumConfidence in 0.0..1.0) {
            "minimumConfidence must be between 0.0 and 1.0"
        }

        val requests =
            readRequests(
                file = requestFile
            )

        val decisions =
            readDecisions(
                file = decisionFile
            )

        val serverKeys =
            readServerKeys(
                file = serverArtifactFile
            )

        val exactMappingIdentities =
            readExactMappingIdentities(
                file = exactMappingFile
            )

        require(
            requests.requests.all {
                it.serverArtifact == NUTRITION_ARTIFACT
            }
        ) {
            "Request file contains non-nutrition requests"
        }

        require(
            decisions.decisions.all {
                it.serverArtifact == NUTRITION_ARTIFACT
            }
        ) {
            "Decision file contains non-nutrition decisions"
        }

        val validationResult =
            CatalogKnowledgeMatchDecisionValidator(
                minimumConfidence =
                    minimumConfidence
            ).validate(
                requests =
                    requests,
                decisions =
                    decisions,
                serverKeysByArtifact =
                    mapOf(
                        NUTRITION_ARTIFACT to
                                serverKeys
                    ),
                existingExactMappings =
                    exactMappingIdentities
            )

        mappingWriter.write(
            mappings =
                validationResult.mappings,
            file =
                outputMappingFile
        )

        validationReportWriter.write(
            report =
                validationResult.report,
            file =
                validationReportFile
        )

        val diagnostics =
            diagnosticsWriter.write(
                requestFile =
                    requestFile,
                decisionFile =
                    decisionFile,
                validationReportFile =
                    validationReportFile,
                mappingFile =
                    outputMappingFile,
                outputFile =
                    diagnosticsFile
            )

        val result =
            createResult(
                requests = requests,
                decisions = decisions,
                serverKeys = serverKeys,
                exactMappingIdentities =
                    exactMappingIdentities,
                validationResult =
                    validationResult,
                diagnosticCount =
                    diagnostics.diagnostics.size
            )

        printResult(
            result = result
        )

        return result
    }


    private fun createResult(
        requests: CatalogKnowledgeMatchRequests,
        decisions: CatalogKnowledgeMatchDecisions,
        serverKeys: Set<String>,
        exactMappingIdentities:
        Set<CatalogKnowledgeMappingIdentity>,
        validationResult:
        CatalogKnowledgeMatchDecisionValidationResult,
        diagnosticCount: Int
    ): WriteValidatedNutritionCatalogServerMappingsResult {

        val statusCounts =
            validationResult.report.validations
                .groupingBy {
                    it.status
                }
                .eachCount()
                .mapKeys {
                    it.key.name
                }
                .toSortedMap()

        return WriteValidatedNutritionCatalogServerMappingsResult(
            requestCount =
                requests.requests.size,
            decisionCount =
                decisions.decisions.size,
            serverKeyCount =
                serverKeys.size,
            exactMappingCount =
                exactMappingIdentities.size,
            acceptedMappingCount =
                validationResult.mappings.mappings.size,
            rejectedDecisionCount =
                validationResult.report.rejectedCount,
            validationStatusCounts =
                statusCounts,
            diagnosticCount =
                diagnosticCount,
            outputMappingFile =
                outputMappingFile.path,
            validationReportFile =
                validationReportFile.path,
            diagnosticsFile =
                diagnosticsFile.path
        )
    }


    private fun printResult(
        result:
        WriteValidatedNutritionCatalogServerMappingsResult
    ) {

        printLine("")
        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        printLine("VALIDATED NUTRITION CATALOG MAPPINGS")
        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        printLine("Requests             : ${result.requestCount}")
        printLine("Decisions            : ${result.decisionCount}")
        printLine("Server keys          : ${result.serverKeyCount}")
        printLine("Exact mappings       : ${result.exactMappingCount}")
        printLine(
            "Accepted AI mappings : " +
                    result.acceptedMappingCount
        )
        printLine(
            "Rejected decisions   : " +
                    result.rejectedDecisionCount
        )

        result.validationStatusCounts
            .forEach { (status, count) ->
                printLine(
                    "$status : $count"
                )
            }

        printLine(
            "Mappings written     : " +
                    result.outputMappingFile
        )

        printLine(
            "Report written       : " +
                    result.validationReportFile
        )

        printLine(
            "Diagnostics          : " +
                    result.diagnosticCount
        )

        printLine(
            "Diagnostics written  : " +
                    result.diagnosticsFile
        )

        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }


    private fun readRequests(
        file: File
    ): CatalogKnowledgeMatchRequests {

        val root =
            parseObject(
                file = file
            )

        val version =
            root.requiredInt(
                key = "version"
            )

        require(
            version ==
                    CatalogKnowledgeMatchRequestContract.CURRENT_VERSION
        ) {
            "Unsupported request version: $version"
        }

        val requests =
            root.requiredArray("requests")
                .map { element ->

                    val requestObject =
                        element.asJsonObject

                    val candidates =
                        requestObject
                            .requiredArray("candidates")
                            .map { candidateElement ->

                                val candidate =
                                    candidateElement.asJsonObject

                                CatalogKnowledgeMatchCandidate(
                                    serverKey =
                                        candidate.requiredString(
                                            "serverKey"
                                        ),
                                    diagnosticScore =
                                        candidate.requiredDouble(
                                            "diagnosticScore"
                                        ),
                                    sharedTokens =
                                        candidate
                                            .requiredArray(
                                                "sharedTokens"
                                            )
                                            .map {
                                                it.asString.trim()
                                            }
                                            .filter(String::isNotBlank)
                                            .distinct()
                                            .sorted()
                                )
                            }
                            .sortedWith(
                                CatalogKnowledgeMatchRequest
                                    .CANDIDATE_ORDER
                            )

                    CatalogKnowledgeMatchRequest(
                        catalogKey =
                            requestObject.requiredString(
                                "catalogKey"
                            ),
                        serverArtifact =
                            requestObject.requiredString(
                                "serverArtifact"
                            ),
                        candidates =
                            candidates
                    )
                }
                .sortedWith(
                    CatalogKnowledgeMatchRequests.REQUEST_ORDER
                )

        return CatalogKnowledgeMatchRequests(
            version = version,
            requests = requests
        )
    }


    private fun readDecisions(
        file: File
    ): CatalogKnowledgeMatchDecisions {

        val root =
            parseObject(
                file = file
            )

        val version =
            root.requiredInt(
                key = "version"
            )

        require(
            version ==
                    CatalogKnowledgeMatchDecisionContract.CURRENT_VERSION
        ) {
            "Unsupported decision version: $version"
        }

        val decisions =
            root.requiredArray("decisions")
                .map { element ->

                    val decisionObject =
                        element.asJsonObject

                    CatalogKnowledgeMatchDecision(
                        catalogKey =
                            decisionObject.requiredString(
                                "catalogKey"
                            ),
                        serverArtifact =
                            decisionObject.requiredString(
                                "serverArtifact"
                            ),
                        type =
                            CatalogKnowledgeMatchDecisionType.valueOf(
                                decisionObject.requiredString(
                                    "type"
                                )
                            ),
                        selectedServerKey =
                            decisionObject.optionalString(
                                "selectedServerKey"
                            ),
                        confidence =
                            decisionObject.requiredDouble(
                                "confidence"
                            ),
                        reason =
                            decisionObject.requiredString(
                                "reason"
                            ),
                        decisionSource =
                            decisionObject.optionalString(
                                "decisionSource"
                            )
                                ?.let(
                                    CatalogKnowledgeMatchDecisionSource::valueOf
                                )
                                ?: CatalogKnowledgeMatchDecisionSource.CHAT_GPT
                    )
                }
                .sortedWith(
                    CatalogKnowledgeMatchDecisions.DECISION_ORDER
                )

        return CatalogKnowledgeMatchDecisions(
            version = version,
            decisions = decisions
        )
    }


    private fun readServerKeys(
        file: File
    ): Set<String> {

        val root =
            JsonParser.parseString(
                file.readText()
            )

        val entries =
            when {
                root.isJsonObject -> {
                    val rootObject =
                        root.asJsonObject

                    when {
                        rootObject["entries"]?.isJsonObject == true ->
                            rootObject["entries"]
                                .asJsonObject
                                .keySet()

                        rootObject["entries"]?.isJsonArray == true ->
                            rootObject["entries"]
                                .asJsonArray
                                .mapNotNull {
                                    readEntryKey(
                                        element =
                                            it.asJsonObject
                                    )
                                }
                                .toSet()

                        else ->
                            rootObject.keySet()
                    }
                }

                root.isJsonArray ->
                    root.asJsonArray
                        .mapNotNull {
                            readEntryKey(
                                element =
                                    it.asJsonObject
                            )
                        }
                        .toSet()

                else ->
                    error(
                        "Unsupported server artifact structure: " +
                                file.absolutePath
                    )
            }

        return entries
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSet()
    }


    private fun readEntryKey(
        element: JsonObject
    ): String? =
        element.optionalString("key")
            ?: element.optionalString("id")
            ?: element.optionalString("name")
            ?: element.optionalString("canonicalKey")
            ?: element.optionalString("serverKey")


    private fun readExactMappingIdentities(
        file: File
    ): Set<CatalogKnowledgeMappingIdentity> {

        if (!file.isFile) {
            return emptySet()
        }

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
                ?: return emptySet()

        return mappings
            .mapNotNull { element ->

                val mapping =
                    element.asJsonObject

                val catalogKey =
                    mapping.optionalString("catalogKey")
                        ?: return@mapNotNull null

                val serverArtifact =
                    mapping.optionalString(
                        "serverArtifact"
                    )
                        ?: mapping.optionalString(
                            "sourceArtifact"
                        )
                        ?: NUTRITION_ARTIFACT

                CatalogKnowledgeMappingIdentity(
                    catalogKey =
                        catalogKey,
                    serverArtifact =
                        serverArtifact
                )
            }
            .toSet()
    }


    private fun parseObject(
        file: File
    ): JsonObject {

        val element =
            JsonParser.parseString(
                file.readText()
            )

        require(element.isJsonObject) {
            "Expected JSON object in ${file.absolutePath}"
        }

        return element.asJsonObject
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


    private fun JsonObject.requiredInt(
        key: String
    ): Int {

        val value =
            get(key)

        require(
            value != null &&
                    !value.isJsonNull &&
                    value.isJsonPrimitive &&
                    value.asJsonPrimitive.isNumber
        ) {
            "Missing integer '$key'"
        }

        return value.asInt
    }


    private fun JsonObject.requiredArray(
        key: String
    ): JsonArray =
        get(key)
            ?.takeIf {
                it.isJsonArray
            }
            ?.asJsonArray
            ?: error(
                "Missing array '$key'"
            )


    companion object {

        private const val NUTRITION_ARTIFACT =
            "nutrition.json"

        @JvmStatic
        fun main(
            args: Array<String>
        ) {
            require(args.isEmpty()) {
                "WriteValidatedNutritionCatalogServerMappings " +
                        "does not accept arguments."
            }

            val paths =
                KnowledgeBuildPaths.default()

            paths.ensureBuildDirectories()

            WriteValidatedNutritionCatalogServerMappings(
                requestFile =
                    paths.nutritionMatchRequests,
                decisionFile =
                    paths.nutritionMatchDecisions,
                serverArtifactFile =
                    paths.serverArtifact(
                        "nutrition.json"
                    ),
                exactMappingFile =
                    paths.nutritionExactMappings,
                outputMappingFile =
                    paths.catalogServerMappings,
                validationReportFile =
                    paths.nutritionValidationReport,
                diagnosticsFile =
                    paths.nutritionMatchDiagnostics,
                minimumConfidence =
                    0.80
            ).run()
        }
    }
}


data class WriteValidatedNutritionCatalogServerMappingsResult(
    val requestCount: Int,
    val decisionCount: Int,
    val serverKeyCount: Int,
    val exactMappingCount: Int,
    val acceptedMappingCount: Int,
    val rejectedDecisionCount: Int,
    val validationStatusCounts: Map<String, Int>,
    val diagnosticCount: Int,
    val outputMappingFile: String,
    val validationReportFile: String,
    val diagnosticsFile: String
)