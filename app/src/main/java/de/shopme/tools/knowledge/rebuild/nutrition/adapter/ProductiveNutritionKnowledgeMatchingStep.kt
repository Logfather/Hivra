package de.shopme.tools.knowledge.rebuild.nutrition.adapter

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchDecision
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchDecisionSource
import de.shopme.tools.knowledge.mapping.catalog.runner.RunOpenAINutritionKnowledgeMatcher
import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeMatchingStep
import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeRebuildMatchingResult
import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeRebuildMode
import java.io.File

class ProductiveNutritionKnowledgeMatchingStep(
    private val runner:
    RunOpenAINutritionKnowledgeMatcher,
    private val decisionFile: File,
    private val requestFile: File
) : NutritionKnowledgeMatchingStep {

    override fun run(
        mode: NutritionKnowledgeRebuildMode
    ): NutritionKnowledgeRebuildMatchingResult {

        require(
            mode ==
                    NutritionKnowledgeRebuildMode.PRODUCTIVE
        ) {
            "Productive matching step only supports PRODUCTIVE mode."
        }

        /*
         * Der produktive Lauf setzt auf dem vorhandenen
         * Decision-Checkpoint auf.
         *
         * Die Decision-Datei kann noch Einträge aus einem früheren
         * Request-Batch enthalten. Deshalb wird der aktuelle Batch
         * anhand der Request-Identitäten validiert und nicht anhand
         * der Gesamtzahl aller persistierten Decisions.
         */
        val requestIdentities =
            readRequestIdentities(
                file =
                    requestFile
            )

        val duplicateRequestIdentities =
            findDuplicateIdentities(
                identities =
                    requestIdentities
            )

        require(
            duplicateRequestIdentities.isEmpty()
        ) {
            "Productive nutrition requests contain duplicate " +
                    "identities: " +
                    formatIdentities(
                        identities =
                            duplicateRequestIdentities
                    )
        }

        val requestIdentitySet =
            requestIdentities
                .toSet()

        /*
         * Auch die Vorher-Zählung wird auf den aktuellen Request-
         * Batch begrenzt. Alte Decisions dürfen die während dieses
         * Laufs erzeugten Source-Deltas nicht verfälschen.
         */
        val beforeSourceCounts =
            readDecisionSourceCounts(
                file =
                    decisionFile,
                includedIdentities =
                    requestIdentitySet
            )

        val result =
            runner.run()

        val persistedDecisionIdentities =
            readDecisionIdentities(
                file =
                    decisionFile
            )

        val duplicateDecisionIdentities =
            findDuplicateIdentities(
                identities =
                    persistedDecisionIdentities
            )

        require(
            duplicateDecisionIdentities.isEmpty()
        ) {
            "Productive nutrition decisions contain duplicate " +
                    "identities: " +
                    formatIdentities(
                        identities =
                            duplicateDecisionIdentities
                    )
        }

        val decisionIdentitySet =
            persistedDecisionIdentities
                .toSet()

        val missingDecisionIdentities =
            requestIdentitySet
                .minus(
                    decisionIdentitySet
                )
                .sortedWith(
                    DECISION_IDENTITY_COMPARATOR
                )

        require(
            missingDecisionIdentities.isEmpty()
        ) {
            "Productive nutrition decision batch is incomplete: " +
                    "missing=${missingDecisionIdentities.size}, " +
                    "requests=${requestIdentities.size}. " +
                    "Missing identities: " +
                    formatIdentities(
                        identities =
                            missingDecisionIdentities
                    )
        }

        /*
         * Stale Decisions sind zulässig:
         *
         * Sie stammen aus älteren Request-Batches und bleiben für
         * Resume- und Diagnosezwecke persistiert. Für den aktuellen
         * Batch werden sie jedoch vollständig ignoriert.
         */
        val staleDecisionIdentities =
            decisionIdentitySet
                .minus(
                    requestIdentitySet
                )
                .sortedWith(
                    DECISION_IDENTITY_COMPARATOR
                )

        val currentDecisionIdentities =
            requestIdentities
                .map { requestIdentity ->

                    check(
                        requestIdentity in
                                decisionIdentitySet
                    ) {
                        "No productive nutrition decision exists " +
                                "for request identity " +
                                "'${requestIdentity.catalogKey} -> " +
                                "${requestIdentity.serverArtifact}'."
                    }

                    requestIdentity
                }

        require(
            currentDecisionIdentities.size ==
                    requestIdentities.size
        ) {
            "Current productive nutrition decision count differs " +
                    "from request count: decisions=" +
                    "${currentDecisionIdentities.size}, " +
                    "requests=${requestIdentities.size}."
        }

        require(
            result.totalRequests ==
                    requestIdentities.size
        ) {
            "Productive runner request count differs from " +
                    "persisted request batch: runner=" +
                    "${result.totalRequests}, " +
                    "persisted=${requestIdentities.size}."
        }

        val afterSourceCounts =
            readDecisionSourceCounts(
                file =
                    decisionFile,
                includedIdentities =
                    requestIdentitySet
            )

        require(
            afterSourceCounts.totalCount ==
                    requestIdentities.size
        ) {
            "Current productive nutrition decision batch is " +
                    "incomplete: decisions=" +
                    "${afterSourceCounts.totalCount}, " +
                    "requests=${requestIdentities.size}, " +
                    "stale=${staleDecisionIdentities.size}."
        }

        val newLocalModelDecisionCount =
            afterSourceCounts.localModelCount -
                    beforeSourceCounts.localModelCount

        val newChatGptDecisionCount =
            afterSourceCounts.chatGptCount -
                    beforeSourceCounts.chatGptCount

        require(
            newLocalModelDecisionCount >= 0
        ) {
            "LOCAL_MODEL decision count decreased during " +
                    "productive matching."
        }

        require(
            newChatGptDecisionCount >= 0
        ) {
            "CHAT_GPT decision count decreased during " +
                    "productive matching."
        }

        val successfulThisRun =
            result.processedThisRun -
                    result.failedThisRun

        require(
            successfulThisRun >= 0
        ) {
            "Successful decision count for this run is negative: " +
                    "processed=${result.processedThisRun}, " +
                    "failed=${result.failedThisRun}."
        }

        require(
            newLocalModelDecisionCount +
                    newChatGptDecisionCount ==
                    successfulThisRun
        ) {
            "New decision source counts differ from successful " +
                    "decisions of this run: " +
                    "local=$newLocalModelDecisionCount, " +
                    "chatGpt=$newChatGptDecisionCount, " +
                    "successfulThisRun=$successfulThisRun, " +
                    "processed=${result.processedThisRun}, " +
                    "failed=${result.failedThisRun}."
        }

        require(
            successfulThisRun +
                    result.failedThisRun ==
                    result.processedThisRun
        ) {
            "Productive matching outcomes do not cover all newly " +
                    "processed requests: " +
                    "successfulThisRun=$successfulThisRun, " +
                    "failed=${result.failedThisRun}, " +
                    "processed=${result.processedThisRun}."
        }

        return NutritionKnowledgeRebuildMatchingResult(
            requestCount =
                result.totalRequests,
            previouslyCompletedCount =
                beforeSourceCounts.totalCount,
            processedCount =
                result.processedThisRun,
            localModelDecisionCount =
                newLocalModelDecisionCount,
            chatGptDecisionCount =
                newChatGptDecisionCount,
            gptFallbackRequiredCount =
                0,
            matchCount =
                result.matchCount,
            noMatchCount =
                result.noMatchCount,
            errorCount =
                result.failedThisRun
        )
    }

    private fun readRequestIdentities(
        file: File
    ): List<DecisionIdentity> {

        require(file.isFile) {
            "Nutrition request file does not exist: " +
                    file.absolutePath
        }

        val root =
            JsonParser.parseString(
                file.readText()
            )

        require(root.isJsonObject) {
            "Nutrition request file must contain a JSON object: " +
                    file.absolutePath
        }

        val requests =
            root.asJsonObject["requests"]
                ?.takeIf {
                    it.isJsonArray
                }
                ?.asJsonArray
                ?: error(
                    "Nutrition request file contains no " +
                            "'requests' array: " +
                            file.absolutePath
                )

        return requests.map { element ->

            require(element.isJsonObject) {
                "Nutrition request entry must be a JSON object."
            }

            readIdentity(
                objectValue =
                    element.asJsonObject,
                entryDescription =
                    "Nutrition request"
            )
        }
    }

    private fun readDecisionIdentities(
        file: File
    ): List<DecisionIdentity> {

        if (!file.isFile) {
            return emptyList()
        }

        val root =
            JsonParser.parseString(
                file.readText()
            )

        require(root.isJsonObject) {
            "Nutrition decision file must contain a JSON object: " +
                    file.absolutePath
        }

        val decisions =
            root.asJsonObject["decisions"]
                ?.takeIf {
                    it.isJsonArray
                }
                ?.asJsonArray
                ?: error(
                    "Nutrition decision file contains no " +
                            "'decisions' array: " +
                            file.absolutePath
                )

        return decisions.map { element ->

            require(element.isJsonObject) {
                "Nutrition decision entry must be a JSON object."
            }

            readIdentity(
                objectValue =
                    element.asJsonObject,
                entryDescription =
                    "Nutrition decision"
            )
        }
    }

    private fun readDecisionSourceCounts(
        file: File,
        includedIdentities: Set<DecisionIdentity>? =
            null
    ): DecisionSourceCounts {

        if (!file.isFile) {

            return DecisionSourceCounts(
                totalCount =
                    0,
                localModelCount =
                    0,
                chatGptCount =
                    0
            )
        }

        val root =
            JsonParser.parseString(
                file.readText()
            )

        require(root.isJsonObject) {
            "Nutrition decision file must contain a JSON object: " +
                    file.absolutePath
        }

        val decisions =
            root.asJsonObject["decisions"]
                ?.takeIf {
                    it.isJsonArray
                }
                ?.asJsonArray
                ?: error(
                    "Nutrition decision file contains no " +
                            "'decisions' array: " +
                            file.absolutePath
                )

        val parsedDecisions =
            decisions.map { element ->

                require(element.isJsonObject) {
                    "Nutrition decision entry must be a JSON object."
                }

                readDecision(
                    objectValue =
                        element.asJsonObject
                )
            }

        val duplicateIdentities =
            findDuplicateIdentities(
                identities =
                    parsedDecisions.map { decision ->

                        DecisionIdentity(
                            catalogKey =
                                decision.catalogKey,
                            serverArtifact =
                                decision.serverArtifact
                        )
                    }
            )

        require(
            duplicateIdentities.isEmpty()
        ) {
            "Nutrition decision file contains duplicate " +
                    "identities: " +
                    formatIdentities(
                        identities =
                            duplicateIdentities
                    )
        }

        val includedDecisions =
            if (includedIdentities == null) {
                parsedDecisions
            } else {
                parsedDecisions.filter { decision ->

                    DecisionIdentity(
                        catalogKey =
                            decision.catalogKey,
                        serverArtifact =
                            decision.serverArtifact
                    ) in includedIdentities
                }
            }

        return DecisionSourceCounts(
            totalCount =
                includedDecisions.size,
            localModelCount =
                includedDecisions.count { decision ->

                    decision.decisionSource ==
                            CatalogKnowledgeMatchDecisionSource
                                .LOCAL_MODEL
                },
            chatGptCount =
                includedDecisions.count { decision ->

                    decision.decisionSource ==
                            CatalogKnowledgeMatchDecisionSource
                                .CHAT_GPT
                }
        )
    }

    private fun readDecision(
        objectValue: JsonObject
    ): CatalogKnowledgeMatchDecision {

        /*
         * Gson setzt Kotlin-Defaultwerte beim Deserialisieren nicht
         * zuverlässig ein. Deshalb wird decisionSource bei älteren
         * Decisions explizit auf CHAT_GPT gesetzt.
         */
        val source =
            objectValue["decisionSource"]
                ?.takeIf {
                    !it.isJsonNull &&
                            it.isJsonPrimitive &&
                            it.asJsonPrimitive.isString
                }
                ?.asString
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let(
                    CatalogKnowledgeMatchDecisionSource::valueOf
                )
                ?: CatalogKnowledgeMatchDecisionSource.CHAT_GPT

        val decision =
            GSON.fromJson(
                objectValue,
                CatalogKnowledgeMatchDecision::class.java
            )

        return decision.copy(
            decisionSource =
                source
        )
    }

    private fun readIdentity(
        objectValue: JsonObject,
        entryDescription: String
    ): DecisionIdentity {

        val catalogKey =
            readRequiredString(
                objectValue =
                    objectValue,
                fieldName =
                    "catalogKey",
                entryDescription =
                    entryDescription
            )

        val serverArtifact =
            readRequiredString(
                objectValue =
                    objectValue,
                fieldName =
                    "serverArtifact",
                entryDescription =
                    entryDescription
            )

        return DecisionIdentity(
            catalogKey =
                catalogKey,
            serverArtifact =
                serverArtifact
        )
    }

    private fun readRequiredString(
        objectValue: JsonObject,
        fieldName: String,
        entryDescription: String
    ): String {

        val value =
            objectValue[fieldName]
                ?.takeIf {
                    !it.isJsonNull &&
                            it.isJsonPrimitive &&
                            it.asJsonPrimitive.isString
                }
                ?.asString
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }

        requireNotNull(value) {
            "$entryDescription contains no non-blank " +
                    "'$fieldName'."
        }

        return value
    }

    private fun findDuplicateIdentities(
        identities: List<DecisionIdentity>
    ): List<DecisionIdentity> =
        identities
            .groupingBy {
                it
            }
            .eachCount()
            .asSequence()
            .filter { (_, count) ->
                count > 1
            }
            .map { (identity, _) ->
                identity
            }
            .sortedWith(
                DECISION_IDENTITY_COMPARATOR
            )
            .toList()

    private fun formatIdentities(
        identities: Collection<DecisionIdentity>
    ): String =
        identities
            .sortedWith(
                DECISION_IDENTITY_COMPARATOR
            )
            .take(
                MAX_DIAGNOSTIC_IDENTITIES
            )
            .joinToString { identity ->

                "${identity.catalogKey} -> " +
                        identity.serverArtifact
            }

    private data class DecisionSourceCounts(
        val totalCount: Int,
        val localModelCount: Int,
        val chatGptCount: Int
    )

    private data class DecisionIdentity(
        val catalogKey: String,
        val serverArtifact: String
    )

    private companion object {

        const val MAX_DIAGNOSTIC_IDENTITIES =
            10

        val DECISION_IDENTITY_COMPARATOR:
                Comparator<DecisionIdentity> =
            compareBy(
                DecisionIdentity::serverArtifact,
                DecisionIdentity::catalogKey
            )

        val GSON: Gson =
            GsonBuilder()
                .disableHtmlEscaping()
                .create()
    }
}