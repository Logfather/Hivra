package de.shopme.tools.knowledge.mapping.catalog.runner

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.ai.AIProviderConfig
import de.shopme.tools.knowledge.ai.openai.OpenAIProvider
import de.shopme.tools.knowledge.ai.openai.OpenAIProviderConfig
import de.shopme.tools.knowledge.ai.openai.RealOpenAIHttpClient
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchCandidate
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchDecision
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchDecisionContract
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchDecisionSource
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchDecisionType
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchDecisions
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchRequest
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchRequestContract
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchRequests
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatcher
import de.shopme.tools.knowledge.mapping.catalog.OpenAICatalogKnowledgeMatcherFactory
import de.shopme.tools.knowledge.mapping.catalog.local.ConservativeLocalNutritionMatcher
import de.shopme.tools.knowledge.mapping.catalog.local.LocalFirstCatalogKnowledgeMatcher
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.system.exitProcess

class RunOpenAINutritionKnowledgeMatcher(
    private val matcher: CatalogKnowledgeMatcher,
    private val requestFile: File,
    private val decisionFile: File,
    private val errorFile: File,
    private val printLine: (String) -> Unit = ::println
) {

    fun run(): OpenAINutritionKnowledgeMatcherRunResult {

        require(requestFile.isFile) {
            "Nutrition match request file does not exist: " +
                    requestFile.absolutePath
        }

        val requestBatch =
            readRequests(
                file = requestFile
            )

        require(
            requestBatch.requests.all {
                it.serverArtifact ==
                        NUTRITION_ARTIFACT
            }
        ) {
            "Nutrition runner received requests for another artifact"
        }

        val existingDecisions =
            readExistingDecisions(
                file = decisionFile
            )

        val existingByIdentity =
            existingDecisions
                .associateBy {
                    it.identity()
                }
                .toMutableMap()

        val requestsToProcess =
            requestBatch.requests
                .filterNot { request ->
                    existingByIdentity.containsKey(
                        request.identity()
                    )
                }

        val errors =
            mutableListOf<OpenAINutritionKnowledgeMatcherError>()

        printHeader(
            totalRequests =
                requestBatch.requests.size,
            existingDecisions =
                existingDecisions.size,
            remainingRequests =
                requestsToProcess.size
        )

        requestsToProcess
            .forEachIndexed { index, request ->

                val absoluteIndex =
                    existingDecisions.size +
                            index +
                            1

                printLine("")
                printLine(
                    "[$absoluteIndex/${requestBatch.requests.size}] " +
                            request.catalogKey
                )

                try {
                    val decision =
                        matcher.match(
                            request = request
                        )

                    validateDecisionAgainstRequest(
                        request = request,
                        decision = decision
                    )

                    existingByIdentity[
                        request.identity()
                    ] = decision

                    persistDecisions(
                        decisions =
                            existingByIdentity
                                .values
                                .sortedWith(
                                    CatalogKnowledgeMatchDecisions
                                        .DECISION_ORDER
                                )
                    )

                    printLine(
                        "Decision   : ${decision.type}"
                    )

                    printLine(
                        "Selected   : " +
                                (
                                        decision.selectedServerKey
                                            ?: "-"
                                        )
                    )

                    printLine(
                        "Confidence : ${decision.confidence}"
                    )

                } catch (exception: Exception) {

                    val error =
                        OpenAINutritionKnowledgeMatcherError(
                            catalogKey =
                                request.catalogKey,
                            serverArtifact =
                                request.serverArtifact,
                            message =
                                exception.message
                                    ?: exception::class.java.name
                        )

                    errors += error

                    persistErrors(
                        errors = errors
                    )

                    printLine(
                        "ERROR      : ${error.message}"
                    )
                }
            }

        val finalDecisions =
            existingByIdentity
                .values
                .sortedWith(
                    CatalogKnowledgeMatchDecisions.DECISION_ORDER
                )

        persistDecisions(
            decisions = finalDecisions
        )

        persistErrors(
            errors = errors
        )

        val result =
            OpenAINutritionKnowledgeMatcherRunResult(
                totalRequests =
                    requestBatch.requests.size,
                previouslyCompleted =
                    existingDecisions.size,
                processedThisRun =
                    requestsToProcess.size,
                successfulDecisions =
                    finalDecisions.size,
                failedThisRun =
                    errors.size,
                matchCount =
                    finalDecisions.count {
                        it.type ==
                                CatalogKnowledgeMatchDecisionType.MATCH
                    },
                noMatchCount =
                    finalDecisions.count {
                        it.type ==
                                CatalogKnowledgeMatchDecisionType.NO_MATCH
                    },
                decisionFile =
                    decisionFile.path,
                errorFile =
                    errorFile.path,
                localModelDecisionCount =
                    finalDecisions.count {
                        it.decisionSource ==
                                CatalogKnowledgeMatchDecisionSource.LOCAL_MODEL
                    },
                chatGptDecisionCount =
                    finalDecisions.count {
                        it.decisionSource ==
                                CatalogKnowledgeMatchDecisionSource.CHAT_GPT
                    },
            )

        printSummary(
            result = result
        )

        return result
    }


    private fun validateDecisionAgainstRequest(
        request: CatalogKnowledgeMatchRequest,
        decision: CatalogKnowledgeMatchDecision
    ) {

        require(
            decision.catalogKey ==
                    request.catalogKey
        ) {
            "Decision catalogKey does not match request: " +
                    "${decision.catalogKey} != ${request.catalogKey}"
        }

        require(
            decision.serverArtifact ==
                    request.serverArtifact
        ) {
            "Decision serverArtifact does not match request: " +
                    "${decision.serverArtifact} != " +
                    request.serverArtifact
        }

        when (decision.type) {

            CatalogKnowledgeMatchDecisionType.MATCH -> {

                val selectedServerKey =
                    requireNotNull(
                        decision.selectedServerKey
                    ) {
                        "MATCH decision has no selectedServerKey"
                    }

                require(
                    request.candidates.any { candidate ->
                        candidate.serverKey ==
                                selectedServerKey
                    }
                ) {
                    "Selected server key was not supplied as candidate: " +
                            selectedServerKey
                }
            }

            CatalogKnowledgeMatchDecisionType.NO_MATCH -> {

                require(
                    decision.selectedServerKey == null
                ) {
                    "NO_MATCH decision must not select a server key"
                }
            }
        }
    }


    private fun readRequests(
        file: File
    ): CatalogKnowledgeMatchRequests {

        val root =
            parseJsonObject(
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
            "Unsupported match request version: $version"
        }

        val requests =
            root
                .requiredArray(
                    key = "requests"
                )
                .map { requestElement ->

                    val requestObject =
                        requestElement.asJsonObject

                    val candidates =
                        requestObject
                            .requiredArray(
                                key = "candidates"
                            )
                            .map { candidateElement ->

                                val candidateObject =
                                    candidateElement.asJsonObject

                                CatalogKnowledgeMatchCandidate(
                                    serverKey =
                                        candidateObject.requiredString(
                                            key = "serverKey"
                                        ),
                                    diagnosticScore =
                                        candidateObject.requiredDouble(
                                            key = "diagnosticScore"
                                        ),
                                    sharedTokens =
                                        candidateObject
                                            .requiredArray(
                                                key = "sharedTokens"
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
                                key = "catalogKey"
                            ),
                        serverArtifact =
                            requestObject.requiredString(
                                key = "serverArtifact"
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


    private fun readExistingDecisions(
        file: File
    ): List<CatalogKnowledgeMatchDecision> {

        if (!file.isFile) {
            return emptyList()
        }

        val root =
            parseJsonObject(
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
            "Unsupported match decision version: $version"
        }

        return root
            .requiredArray(
                key = "decisions"
            )
            .map { decisionElement ->

                val decisionObject =
                    decisionElement.asJsonObject

                val type =
                    CatalogKnowledgeMatchDecisionType.valueOf(
                        decisionObject.requiredString(
                            key = "type"
                        )
                    )

                CatalogKnowledgeMatchDecision(
                    catalogKey =
                        decisionObject.requiredString(
                            key = "catalogKey"
                        ),
                    serverArtifact =
                        decisionObject.requiredString(
                            key = "serverArtifact"
                        ),
                    type =
                        type,
                    selectedServerKey =
                        decisionObject.optionalString(
                            key = "selectedServerKey"
                        ),
                    confidence =
                        decisionObject.requiredDouble(
                            key = "confidence"
                        ),
                    reason =
                        decisionObject.requiredString(
                            key = "reason"
                        ),
                    decisionSource =
                        decisionObject
                            .optionalString(
                                key = "decisionSource"
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
            .also { decisions ->

                CatalogKnowledgeMatchDecisions(
                    version =
                        CatalogKnowledgeMatchDecisionContract
                            .CURRENT_VERSION,
                    decisions =
                        decisions
                )
            }
    }


    private fun persistDecisions(
        decisions: List<CatalogKnowledgeMatchDecision>
    ) {

        val batch =
            CatalogKnowledgeMatchDecisions(
                version =
                    CatalogKnowledgeMatchDecisionContract.CURRENT_VERSION,
                decisions =
                    decisions.sortedWith(
                        CatalogKnowledgeMatchDecisions.DECISION_ORDER
                    )
            )

        writeJsonAtomically(
            value = batch,
            file = decisionFile
        )
    }


    private fun persistErrors(
        errors: List<OpenAINutritionKnowledgeMatcherError>
    ) {

        val report =
            OpenAINutritionKnowledgeMatcherErrorReport(
                errors =
                    errors.sortedWith(
                        compareBy<OpenAINutritionKnowledgeMatcherError> {
                            it.serverArtifact
                        }.thenBy {
                            it.catalogKey
                        }
                    )
            )

        writeJsonAtomically(
            value = report,
            file = errorFile
        )
    }


    private fun writeJsonAtomically(
        value: Any,
        file: File
    ) {

        val parentDirectory =
            file.parentFile

        if (
            parentDirectory != null &&
            !parentDirectory.exists()
        ) {
            check(
                parentDirectory.mkdirs()
            ) {
                "Could not create directory: " +
                        parentDirectory.absolutePath
            }
        }

        val temporaryFile =
            File(
                parentDirectory,
                "${file.name}.tmp"
            )

        temporaryFile.writeText(
            GSON.toJson(value)
        )

        try {
            Files.move(
                temporaryFile.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (
            exception: AtomicMoveNotSupportedException
        ) {
            Files.move(
                temporaryFile.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }


    private fun printHeader(
        totalRequests: Int,
        existingDecisions: Int,
        remainingRequests: Int
    ) {

        printLine("")
        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        printLine("OPENAI NUTRITION KNOWLEDGE MATCH")
        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        printLine("Requests total      : $totalRequests")
        printLine("Already completed   : $existingDecisions")
        printLine("Remaining           : $remainingRequests")
        printLine("Request file        : ${requestFile.path}")
        printLine("Decision file       : ${decisionFile.path}")
        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }


    private fun printSummary(
        result: OpenAINutritionKnowledgeMatcherRunResult
    ) {

        printLine("")
        printLine(
            "LOCAL_MODEL         : " +
                    result.localModelDecisionCount
        )

        printLine(
            "CHAT_GPT            : " +
                    result.chatGptDecisionCount
        )

        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        printLine("OPENAI NUTRITION MATCH SUMMARY")
        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        printLine("Total requests      : ${result.totalRequests}")
        printLine("Previously complete : ${result.previouslyCompleted}")
        printLine("Processed this run  : ${result.processedThisRun}")
        printLine("Successful total    : ${result.successfulDecisions}")
        printLine("Failed this run     : ${result.failedThisRun}")
        printLine("MATCH               : ${result.matchCount}")
        printLine("NO_MATCH            : ${result.noMatchCount}")
        printLine("Decisions written   : ${result.decisionFile}")
        printLine("Errors written      : ${result.errorFile}")
        printLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }


    private fun parseJsonObject(
        file: File
    ): JsonObject {

        val root =
            JsonParser.parseString(
                file.readText()
            )

        require(root.isJsonObject) {
            "Expected JSON object in ${file.absolutePath}"
        }

        return root.asJsonObject
    }


    private fun JsonObject.requiredString(
        key: String
    ): String {

        val value =
            optionalString(
                key = key
            )

        require(
            !value.isNullOrBlank()
        ) {
            "Missing or blank string '$key'"
        }

        return value
    }


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


    private fun JsonObject.requiredInt(
        key: String
    ): Int {

        val element =
            get(key)

        require(
            element != null &&
                    !element.isJsonNull &&
                    element.isJsonPrimitive &&
                    element.asJsonPrimitive.isNumber
        ) {
            "Missing integer '$key'"
        }

        return element.asInt
    }


    private fun JsonObject.requiredDouble(
        key: String
    ): Double {

        val element =
            get(key)

        require(
            element != null &&
                    !element.isJsonNull &&
                    element.isJsonPrimitive &&
                    element.asJsonPrimitive.isNumber
        ) {
            "Missing number '$key'"
        }

        return element.asDouble
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
                "Missing JSON array '$key'"
            )


    private fun CatalogKnowledgeMatchRequest.identity():
            MatchIdentity =
        MatchIdentity(
            catalogKey = catalogKey,
            serverArtifact = serverArtifact
        )


    private fun CatalogKnowledgeMatchDecision.identity():
            MatchIdentity =
        MatchIdentity(
            catalogKey = catalogKey,
            serverArtifact = serverArtifact
        )


    private data class MatchIdentity(
        val catalogKey: String,
        val serverArtifact: String
    )


    companion object {

        private const val NUTRITION_ARTIFACT =
            "nutrition.json"

        private val GSON: Gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()


        @JvmStatic
        fun main(
            args: Array<String>
        ) {

            val enabled =
                System.getenv(
                    RUN_ENVIRONMENT_VARIABLE
                )
                    ?.trim()
                    ?.equals(
                        other = "true",
                        ignoreCase = true
                    )
                    ?: false

            if (!enabled) {
                System.err.println(
                    "OpenAI nutrition match runner is disabled. " +
                            "Set $RUN_ENVIRONMENT_VARIABLE=true."
                )

                exitProcess(1)
            }

            val paths =
                KnowledgeBuildPaths.default()

            paths.ensureBuildDirectories()

            val openAIConfig =
                OpenAIProviderConfig
                    .fromEnvironment()

            val providerConfig =
                AIProviderConfig(
                    providerName =
                        "openai",
                    model =
                        openAIConfig.model,
                    apiKey =
                        openAIConfig.apiKey,
                    endpoint =
                        openAIConfig.endpoint,

                    /*
                     * null bedeutet: Modell-Default verwenden.
                     * GPT-5.5 akzeptiert keinen expliziten Wert 0.
                     */
                    temperature =
                        null
                )

            val provider =
                OpenAIProvider(
                    config =
                        providerConfig,
                    httpClient =
                        RealOpenAIHttpClient(
                            config = providerConfig
                        )
                )

            val openAIMatcher =
                OpenAICatalogKnowledgeMatcherFactory(
                    openAIProvider =
                        provider
                )
                    .create()

            val localModelFile =
                paths.nutritionLocalMatcherModel

            require(localModelFile.isFile) {
                "Local nutrition matcher model does not exist: " +
                        localModelFile.absolutePath
            }

            val conservativeLocalMatcher =
                ConservativeLocalNutritionMatcher
                    .fromModelFile(
                        modelFile =
                            localModelFile,
                        autoAcceptThreshold =
                            ConservativeLocalNutritionMatcher
                                .DEFAULT_AUTO_ACCEPT_THRESHOLD
                    )

            val matcher =
                LocalFirstCatalogKnowledgeMatcher(
                    localMatcher =
                        conservativeLocalMatcher,
                    fallbackMatcher =
                        openAIMatcher
                )

            val runner =
                RunOpenAINutritionKnowledgeMatcher(
                    matcher =
                        matcher,
                    requestFile =
                        paths.nutritionMatchRequests,

                    decisionFile =
                        paths.nutritionMatchDecisions,

                    errorFile =
                        paths.nutritionMatchErrors
                )

            val result =
                runner.run()

            if (result.failedThisRun > 0) {
                exitProcess(2)
            }
        }


        private const val RUN_ENVIRONMENT_VARIABLE =
            "RUN_OPENAI_NUTRITION_MATCH"
    }
}


data class OpenAINutritionKnowledgeMatcherRunResult(
    val totalRequests: Int,
    val previouslyCompleted: Int,
    val processedThisRun: Int,
    val successfulDecisions: Int,
    val failedThisRun: Int,
    val matchCount: Int,
    val noMatchCount: Int,
    val localModelDecisionCount: Int,
    val chatGptDecisionCount: Int,
    val decisionFile: String,
    val errorFile: String
)


data class OpenAINutritionKnowledgeMatcherError(
    val catalogKey: String,
    val serverArtifact: String,
    val message: String
)


data class OpenAINutritionKnowledgeMatcherErrorReport(
    val errors: List<OpenAINutritionKnowledgeMatcherError>
)