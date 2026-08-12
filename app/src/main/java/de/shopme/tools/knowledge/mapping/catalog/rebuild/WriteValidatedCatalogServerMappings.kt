package de.shopme.tools.knowledge.mapping.catalog.rebuild

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
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
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchRequest
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchRequestContract
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchRequests
import de.shopme.tools.knowledge.mapping.catalog.CatalogServerKnowledgeMappingWriter
import java.io.File

data class WriteValidatedCatalogServerMappingsResult(
    val serverArtifact: String,
    val requestCount: Int,
    val decisionCount: Int,
    val serverKeyCount: Int,
    val exactMappingCount: Int,
    val acceptedMappingCount: Int,
    val rejectedDecisionCount: Int,
    val validationStatusCounts: Map<String, Int>,
    val outputMappingFile: String,
    val validationReportFile: String
)

class WriteValidatedCatalogServerMappings(
    private val serverArtifact: String,
    private val requestFile: File,
    private val decisionFile: File,
    private val serverArtifactFile: File,
    private val exactMappingFile: File,
    private val outputMappingFile: File,
    private val validationReportFile: File,
    private val minimumConfidence: Double = DEFAULT_MINIMUM_CONFIDENCE,
    private val mappingWriter:
    CatalogServerKnowledgeMappingWriter =
        CatalogServerKnowledgeMappingWriter(),
    private val validationReportWriter:
    CatalogKnowledgeMappingValidationReportWriter =
        CatalogKnowledgeMappingValidationReportWriter(),
    private val printLine: (String) -> Unit =
        ::println
) {

    fun run():
            WriteValidatedCatalogServerMappingsResult {

        require(serverArtifact.isNotBlank()) {
            "serverArtifact must not be blank."
        }

        require(requestFile.isFile) {
            "Match request file does not exist: " +
                    requestFile.absolutePath
        }

        require(decisionFile.isFile) {
            "Match decision file does not exist: " +
                    decisionFile.absolutePath
        }

        require(serverArtifactFile.isFile) {
            "Server artifact does not exist: " +
                    serverArtifactFile.absolutePath
        }

        require(
            minimumConfidence in 0.0..1.0
        ) {
            "minimumConfidence must be between 0.0 and 1.0."
        }

        val requests =
            readRequests(
                file =
                    requestFile
            )

        val decisions =
            readDecisions(
                file =
                    decisionFile
            )

        require(
            requests.requests.all {
                it.serverArtifact ==
                        serverArtifact
            }
        ) {
            "Request file contains requests for another artifact."
        }

        require(
            decisions.decisions.all {
                it.serverArtifact ==
                        serverArtifact
            }
        ) {
            "Decision file contains decisions for another artifact."
        }

        val serverKeys =
            readServerKeys(
                file =
                    serverArtifactFile
            )

        val exactMappingIdentities =
            readExactMappingIdentities(
                file =
                    exactMappingFile,
                serverArtifact =
                    serverArtifact
            )

        val validationResult =
            CatalogKnowledgeMatchDecisionValidator(
                minimumConfidence =
                    minimumConfidence
            )
                .validate(
                    requests =
                        requests,
                    decisions =
                        decisions,
                    serverKeysByArtifact =
                        mapOf(
                            serverArtifact to
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

        val result =
            createResult(
                requests =
                    requests,
                decisions =
                    decisions,
                serverKeys =
                    serverKeys,
                exactMappingIdentities =
                    exactMappingIdentities,
                validationResult =
                    validationResult
            )

        printResult(
            result =
                result
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
        CatalogKnowledgeMatchDecisionValidationResult
    ): WriteValidatedCatalogServerMappingsResult {

        val statusCounts =
            validationResult
                .report
                .validations
                .groupingBy {
                    it.status
                }
                .eachCount()
                .mapKeys {
                    it.key.name
                }
                .toSortedMap()

        return WriteValidatedCatalogServerMappingsResult(
            serverArtifact =
                serverArtifact,
            requestCount =
                requests.requests.size,
            decisionCount =
                decisions.decisions.size,
            serverKeyCount =
                serverKeys.size,
            exactMappingCount =
                exactMappingIdentities.size,
            acceptedMappingCount =
                validationResult
                    .mappings
                    .mappings
                    .size,
            rejectedDecisionCount =
                validationResult
                    .report
                    .rejectedCount,
            validationStatusCounts =
                statusCounts,
            outputMappingFile =
                outputMappingFile.path,
            validationReportFile =
                validationReportFile.path
        )
    }

    private fun readRequests(
        file: File
    ): CatalogKnowledgeMatchRequests {

        val root =
            parseObject(
                file =
                    file
            )

        val version =
            root.requiredInt(
                key =
                    "version"
            )

        require(
            version ==
                    CatalogKnowledgeMatchRequestContract
                        .CURRENT_VERSION
        ) {
            "Unsupported request version: $version"
        }

        val requests =
            root
                .requiredArray(
                    key =
                        "requests"
                )
                .map { element ->

                    val requestObject =
                        element.asJsonObject

                    val candidates =
                        requestObject
                            .requiredArray(
                                key =
                                    "candidates"
                            )
                            .map { candidateElement ->

                                val candidate =
                                    candidateElement
                                        .asJsonObject

                                CatalogKnowledgeMatchCandidate(
                                    serverKey =
                                        candidate.requiredString(
                                            key =
                                                "serverKey"
                                        ),
                                    diagnosticScore =
                                        candidate.requiredDouble(
                                            key =
                                                "diagnosticScore"
                                        ),
                                    sharedTokens =
                                        candidate
                                            .requiredArray(
                                                key =
                                                    "sharedTokens"
                                            )
                                            .map {
                                                it.asString
                                                    .trim()
                                            }
                                            .filter(
                                                String::isNotBlank
                                            )
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
                                key =
                                    "catalogKey"
                            ),
                        serverArtifact =
                            requestObject.requiredString(
                                key =
                                    "serverArtifact"
                            ),
                        candidates =
                            candidates
                    )
                }
                .sortedWith(
                    CatalogKnowledgeMatchRequests
                        .REQUEST_ORDER
                )

        return CatalogKnowledgeMatchRequests(
            version =
                version,
            requests =
                requests
        )
    }

    private fun readDecisions(
        file: File
    ): CatalogKnowledgeMatchDecisions {

        val root =
            parseObject(
                file =
                    file
            )

        val version =
            root.requiredInt(
                key =
                    "version"
            )

        require(
            version ==
                    CatalogKnowledgeMatchDecisionContract
                        .CURRENT_VERSION
        ) {
            "Unsupported decision version: $version"
        }

        val decisions =
            root
                .requiredArray(
                    key =
                        "decisions"
                )
                .map { element ->

                    val decisionObject =
                        element.asJsonObject

                    CatalogKnowledgeMatchDecision(
                        catalogKey =
                            decisionObject.requiredString(
                                key =
                                    "catalogKey"
                            ),
                        serverArtifact =
                            decisionObject.requiredString(
                                key =
                                    "serverArtifact"
                            ),
                        type =
                            CatalogKnowledgeMatchDecisionType
                                .valueOf(
                                    decisionObject.requiredString(
                                        key =
                                            "type"
                                    )
                                ),
                        selectedServerKey =
                            decisionObject.optionalString(
                                key =
                                    "selectedServerKey"
                            ),
                        confidence =
                            decisionObject.requiredDouble(
                                key =
                                    "confidence"
                            ),
                        reason =
                            decisionObject.requiredString(
                                key =
                                    "reason"
                            ),
                        decisionSource =
                            decisionObject
                                .optionalString(
                                    key =
                                        "decisionSource"
                                )
                                ?.let(
                                    CatalogKnowledgeMatchDecisionSource::valueOf
                                )
                                ?: CatalogKnowledgeMatchDecisionSource
                                    .CHAT_GPT
                    )
                }
                .sortedWith(
                    CatalogKnowledgeMatchDecisions
                        .DECISION_ORDER
                )

        return CatalogKnowledgeMatchDecisions(
            version =
                version,
            decisions =
                decisions
        )
    }

    /**
     * Streaming ist hier Pflicht:
     * nutrition.json, ingredients.json usw. sind mehrere
     * hundert MB groß.
     */
    private fun readServerKeys(
        file: File
    ): Set<String> {

        val keys =
            linkedSetOf<String>()

        file.bufferedReader()
            .use { bufferedReader ->

                JsonReader(
                    bufferedReader
                ).use { reader ->

                    reader.beginObject()

                    var entriesFound =
                        false

                    while (reader.hasNext()) {

                        when (
                            reader.nextName()
                        ) {

                            "entries" -> {

                                entriesFound =
                                    true

                                reader.beginObject()

                                while (reader.hasNext()) {

                                    keys +=
                                        reader.nextName()

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

                    require(entriesFound) {
                        "Server artifact does not contain 'entries': " +
                                file.absolutePath
                    }
                }
            }

        return keys
    }

    private fun readExactMappingIdentities(
        file: File,
        serverArtifact: String
    ): Set<CatalogKnowledgeMappingIdentity> {

        if (!file.isFile) {
            return emptySet()
        }

        val root =
            parseObject(
                file =
                    file
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
                    mapping.optionalString(
                        key =
                            "catalogKey"
                    )
                        ?: return@mapNotNull null

                val artifact =
                    mapping.optionalString(
                        key =
                            "serverArtifact"
                    )
                        ?: mapping.optionalString(
                            key =
                                "sourceArtifact"
                        )
                        ?: return@mapNotNull null

                if (
                    artifact !=
                    serverArtifact
                ) {
                    return@mapNotNull null
                }

                CatalogKnowledgeMappingIdentity(
                    catalogKey =
                        catalogKey,
                    serverArtifact =
                        artifact
                )
            }
            .toSet()
    }

    private fun parseObject(
        file: File
    ): JsonObject {

        val element =
            JsonParser
                .parseString(
                    file.readText()
                )

        require(
            element.isJsonObject
        ) {
            "Expected JSON object in: " +
                    file.absolutePath
        }

        return element.asJsonObject
    }

    private fun JsonObject.requiredString(
        key: String
    ): String =
        optionalString(
            key =
                key
        )
            ?: error(
                "Missing or blank string '$key'."
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
            ?.takeIf(
                String::isNotBlank
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
                    value
                        .asJsonPrimitive
                        .isNumber
        ) {
            "Missing numeric '$key'."
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
                    value
                        .asJsonPrimitive
                        .isNumber
        ) {
            "Missing integer '$key'."
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
                "Missing JSON array '$key'."
            )

    private fun printResult(
        result:
        WriteValidatedCatalogServerMappingsResult
    ) {

        printLine("")
        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        printLine("VALIDATED CATALOG → SERVER MAPPINGS")
        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        printLine(
            "Artifact          : " +
                    result.serverArtifact
        )
        printLine(
            "Requests          : " +
                    result.requestCount
        )
        printLine(
            "Decisions         : " +
                    result.decisionCount
        )
        printLine(
            "Server keys       : " +
                    result.serverKeyCount
        )
        printLine(
            "Exact mappings    : " +
                    result.exactMappingCount
        )
        printLine(
            "Accepted mappings : " +
                    result.acceptedMappingCount
        )
        printLine(
            "Rejected          : " +
                    result.rejectedDecisionCount
        )

        result.validationStatusCounts
            .forEach { (status, count) ->

                printLine(
                    "$status : $count"
                )
            }

        printLine(
            "Mappings written  : " +
                    result.outputMappingFile
        )
        printLine(
            "Report written    : " +
                    result.validationReportFile
        )
        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    companion object {

        const val DEFAULT_MINIMUM_CONFIDENCE =
            0.80
    }
}