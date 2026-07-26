package de.shopme.testing.system.tools.knowledge.rebuild.nutrition.coverage

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RunRemainingLiverSausageNoRequestDiagnosticTest {

    private companion object {

        const val TARGET_CATALOG_KEY =
            "liver sausage"
    }

    @Test
    fun diagnoseRemainingLiverSausageNoRequestGap() {

        val projectRoot =
            File("..")
                .canonicalFile

        val files =
            DiagnosticFiles(
                catalogFile =
                    projectRoot.resolve(
                        "app/src/main/assets/catalog/catalog.json"
                    ),
                serverNutritionFile =
                    projectRoot.resolve(
                        "data/generated/knowledge/server/nutrition.json"
                    ),
                matchReportFile =
                    projectRoot.resolve(
                        "data/generated/reports/catalog-server-matches/nutrition.matches.json"
                    ),
                matchRequestFile =
                    projectRoot.resolve(
                        "data/generated/knowledge/match-requests/nutrition.match-requests.json"
                    ),
                matchDecisionFile =
                    projectRoot.resolve(
                        "data/generated/knowledge/reports/nutrition.match-diagnostics.json"
                    ),
                centralMappingFile =
                    projectRoot.resolve(
                        "data/generated/knowledge/mappings/catalog-server.mappings.json"
                    ),
                runtimeNutritionFile =
                    projectRoot.resolve(
                        "data/generated/knowledge/runtime/nutrition.json"
                    ),
                coverageGapFile =
                    projectRoot.resolve(
                        "data/generated/knowledge/reports/nutrition.coverage-gaps.json"
                    ),
                diagnosticOutputFile =
                    projectRoot.resolve(
                        "data/generated/knowledge/reports/" +
                                "nutrition.liver-sausage-no-request-diagnostic.json"
                    )
            )

        requireInputFiles(
            files = files
        )

        val catalogRoot =
            parseJson(
                file = files.catalogFile
            )

        val serverNutritionRoot =
            parseJson(
                file = files.serverNutritionFile
            )

        val matchReportRoot =
            parseJson(
                file = files.matchReportFile
            )

        val matchRequestRoot =
            parseJson(
                file = files.matchRequestFile
            )

        val matchDecisionRoot =
            parseOptionalJson(
                file = files.matchDecisionFile
            )

        val centralMappingRoot =
            parseOptionalJson(
                file = files.centralMappingFile
            )

        val runtimeNutritionRoot =
            parseOptionalJson(
                file = files.runtimeNutritionFile
            )

        val coverageGapRoot =
            parseJson(
                file = files.coverageGapFile
            )

        val catalogKeyPresent =
            containsStringValue(
                root = catalogRoot,
                expected = TARGET_CATALOG_KEY
            )

        val exactServerKeyPresent =
            containsObjectKey(
                root = serverNutritionRoot,
                expectedKey = TARGET_CATALOG_KEY
            )

        val relatedServerKeys =
            readServerEntryKeys(
                root = serverNutritionRoot
            )
                .filter { serverKey ->
                    serverKey.contains(
                        other = "liver",
                        ignoreCase = true
                    ) ||
                            serverKey.contains(
                                other = "sausage",
                                ignoreCase = true
                            )
                }
                .sorted()

        val exactMatchPresent =
            readStringArray(
                root = matchReportRoot,
                key = "exactMatches"
            )
                .any { exactMatch ->
                    exactMatch ==
                            TARGET_CATALOG_KEY
                }

        val unmatchedEntry =
            findObjectByStringProperty(
                root = matchReportRoot,
                propertyName = "catalogKey",
                expectedValue = TARGET_CATALOG_KEY
            )

        val nearestCandidates =
            unmatchedEntry
                ?.get("nearestCandidates")
                ?.takeIf {
                    it.isJsonArray
                }
                ?.asJsonArray
                ?.mapNotNull { element ->
                    element
                        .takeIf {
                            it.isJsonObject
                        }
                        ?.asJsonObject
                        ?.let(::readCandidate)
                }
                .orEmpty()

        val requestEntry =
            findObjectByStringProperty(
                root = matchRequestRoot,
                propertyName = "catalogKey",
                expectedValue = TARGET_CATALOG_KEY
            )

        val requestCandidates =
            requestEntry
                ?.get("candidates")
                ?.takeIf {
                    it.isJsonArray
                }
                ?.asJsonArray
                ?.mapNotNull { element ->
                    element
                        .takeIf {
                            it.isJsonObject
                        }
                        ?.asJsonObject
                        ?.let(::readCandidate)
                }
                .orEmpty()

        val decisionEntry =
            matchDecisionRoot
                ?.let { root ->
                    findObjectByStringProperty(
                        root = root,
                        propertyName = "catalogKey",
                        expectedValue = TARGET_CATALOG_KEY
                    )
                }

        val mappingPresent =
            centralMappingRoot
                ?.let { root ->
                    containsCatalogMapping(
                        root = root,
                        catalogKey = TARGET_CATALOG_KEY
                    )
                }
                ?: false

        val runtimePresent =
            runtimeNutritionRoot
                ?.let { root ->
                    containsObjectKey(
                        root = root,
                        expectedKey = TARGET_CATALOG_KEY
                    )
                }
                ?: false

        val coverageGapEntry =
            findObjectByStringProperty(
                root = coverageGapRoot,
                propertyName = "catalogKey",
                expectedValue = TARGET_CATALOG_KEY
            )

        val coverageGapType =
            coverageGapEntry
                ?.optionalString(
                    key = "type"
                )

        val firstMissingStage =
            determineFirstMissingStage(
                catalogKeyPresent = catalogKeyPresent,
                exactServerKeyPresent = exactServerKeyPresent,
                exactMatchPresent = exactMatchPresent,
                unmatchedEntryPresent = unmatchedEntry != null,
                nearestCandidates = nearestCandidates,
                requestEntryPresent = requestEntry != null,
                decisionEntryPresent = decisionEntry != null,
                mappingPresent = mappingPresent,
                runtimePresent = runtimePresent
            )

        val explanation =
            determineExplanation(
                exactServerKeyPresent = exactServerKeyPresent,
                exactMatchPresent = exactMatchPresent,
                unmatchedEntryPresent = unmatchedEntry != null,
                nearestCandidates = nearestCandidates,
                requestEntryPresent = requestEntry != null,
                decisionEntryPresent = decisionEntry != null,
                mappingPresent = mappingPresent,
                runtimePresent = runtimePresent
            )

        val report =
            LiverSausageNoRequestDiagnosticReport(
                version = 1,
                catalogKey = TARGET_CATALOG_KEY,
                catalogKeyPresent = catalogKeyPresent,
                exactServerKeyPresent = exactServerKeyPresent,
                relatedServerKeys = relatedServerKeys,
                exactMatchPresent = exactMatchPresent,
                unmatchedMatchReportEntryPresent =
                    unmatchedEntry != null,
                nearestCandidates = nearestCandidates,
                requestPresent = requestEntry != null,
                requestCandidates = requestCandidates,
                decisionPresent = decisionEntry != null,
                decisionType =
                    decisionEntry
                        ?.optionalString(
                            key = "decisionType"
                        ),
                selectedServerKey =
                    decisionEntry
                        ?.optionalString(
                            key = "selectedServerKey"
                        ),
                mappingPresent = mappingPresent,
                runtimePresent = runtimePresent,
                coverageGapPresent = coverageGapEntry != null,
                coverageGapType = coverageGapType,
                firstMissingStage = firstMissingStage,
                explanation = explanation
            )

        verifyDiagnosticReport(
            report = report
        )

        writeReport(
            report = report,
            file = files.diagnosticOutputFile
        )

        printReport(
            report = report,
            outputFile = files.diagnosticOutputFile
        )
    }

    private fun requireInputFiles(
        files: DiagnosticFiles
    ) {
        listOf(
            files.catalogFile,
            files.serverNutritionFile,
            files.matchReportFile,
            files.matchRequestFile,
            files.coverageGapFile
        )
            .forEach { file ->
                require(file.isFile) {
                    "Required diagnostic input does not exist: " +
                            file.absolutePath
                }
            }
    }

    private fun verifyDiagnosticReport(
        report: LiverSausageNoRequestDiagnosticReport
    ) {
        assertTrue(
            actual =
                report.catalogKeyPresent,
            message =
                "'$TARGET_CATALOG_KEY' must exist in the catalog."
        )

        assertTrue(
            actual =
                report.coverageGapPresent,
            message =
                "'$TARGET_CATALOG_KEY' must exist in the current " +
                        "nutrition coverage-gap report."
        )

        assertEquals(
            expected =
                "NO_REQUEST",
            actual =
                report.coverageGapType,
            message =
                "This diagnostic targets the remaining NO_REQUEST gap."
        )

        assertFalse(
            actual =
                report.requestPresent,
            message =
                "'$TARGET_CATALOG_KEY' is no longer a NO_REQUEST gap. " +
                        "Regenerate the coverage report before running " +
                        "this diagnostic."
        )

        /*
         * Die eigentliche diagnostische Invariante:
         *
         * Ein NO_REQUEST-Fall muss entweder bereits im Match-Report
         * ohne verwertbare Kandidaten enden oder zwischen Match-Report
         * und Request-Artefakt verloren gehen.
         */
        assertTrue(
            actual =
                report.exactMatchPresent ||
                        report.unmatchedMatchReportEntryPresent,
            message =
                "'$TARGET_CATALOG_KEY' is absent from both exact and " +
                        "unmatched match-report results."
        )

        if (
            report.unmatchedMatchReportEntryPresent &&
            report.nearestCandidates.isNotEmpty()
        ) {
            assertEquals(
                expected =
                    MissingStage.MATCH_REQUEST,
                actual =
                    report.firstMissingStage,
                message =
                    "Candidates exist in the match report but no persisted " +
                            "request exists. The gap is in request generation."
            )
        }

        if (
            report.unmatchedMatchReportEntryPresent &&
            report.nearestCandidates.isEmpty()
        ) {
            assertEquals(
                expected =
                    MissingStage.RETRIEVAL_CANDIDATES,
                actual =
                    report.firstMissingStage,
                message =
                    "No request is expected when the match report contains " +
                            "no candidates. The gap is in retrieval."
            )
        }
    }

    private fun determineFirstMissingStage(
        catalogKeyPresent: Boolean,
        exactServerKeyPresent: Boolean,
        exactMatchPresent: Boolean,
        unmatchedEntryPresent: Boolean,
        nearestCandidates: List<DiagnosticCandidate>,
        requestEntryPresent: Boolean,
        decisionEntryPresent: Boolean,
        mappingPresent: Boolean,
        runtimePresent: Boolean
    ): MissingStage =
        when {
            !catalogKeyPresent ->
                MissingStage.CATALOG

            exactServerKeyPresent &&
                    exactMatchPresent &&
                    !runtimePresent ->
                MissingStage.RUNTIME

            !exactMatchPresent &&
                    !unmatchedEntryPresent ->
                MissingStage.MATCH_REPORT

            unmatchedEntryPresent &&
                    nearestCandidates.isEmpty() ->
                MissingStage.RETRIEVAL_CANDIDATES

            unmatchedEntryPresent &&
                    nearestCandidates.isNotEmpty() &&
                    !requestEntryPresent ->
                MissingStage.MATCH_REQUEST

            requestEntryPresent &&
                    !decisionEntryPresent ->
                MissingStage.MATCH_DECISION

            decisionEntryPresent &&
                    !mappingPresent ->
                MissingStage.MAPPING

            mappingPresent &&
                    !runtimePresent ->
                MissingStage.RUNTIME

            else ->
                MissingStage.NONE
        }

    private fun determineExplanation(
        exactServerKeyPresent: Boolean,
        exactMatchPresent: Boolean,
        unmatchedEntryPresent: Boolean,
        nearestCandidates: List<DiagnosticCandidate>,
        requestEntryPresent: Boolean,
        decisionEntryPresent: Boolean,
        mappingPresent: Boolean,
        runtimePresent: Boolean
    ): String =
        when {
            exactServerKeyPresent &&
                    exactMatchPresent &&
                    !runtimePresent ->
                "An exact server nutrition key exists and is reported as " +
                        "an exact match, but no runtime nutrition entry exists."

            !exactMatchPresent &&
                    !unmatchedEntryPresent ->
                "The catalog key is missing from both exact and unmatched " +
                        "sections of the nutrition match report."

            unmatchedEntryPresent &&
                    nearestCandidates.isEmpty() ->
                "The nutrition match report contains the catalog key but " +
                        "retrieval produced no candidates. The request " +
                        "generator therefore has nothing to persist."

            unmatchedEntryPresent &&
                    nearestCandidates.isNotEmpty() &&
                    !requestEntryPresent ->
                "The nutrition match report contains candidates, but the " +
                        "catalog key is absent from the persisted match-request " +
                        "artifact. The loss occurs during request generation."

            requestEntryPresent &&
                    !decisionEntryPresent ->
                "A persisted nutrition match request exists, but no match " +
                        "decision has been persisted."

            decisionEntryPresent &&
                    !mappingPresent ->
                "A match decision exists, but no central catalog-server " +
                        "mapping has been persisted."

            mappingPresent &&
                    !runtimePresent ->
                "A central catalog-server mapping exists, but no runtime " +
                        "nutrition entry has been generated."

            else ->
                "No missing pipeline stage was detected."
        }

    private fun readServerEntryKeys(
        root: JsonElement
    ): List<String> {
        if (!root.isJsonObject) {
            return emptyList()
        }

        val rootObject =
            root.asJsonObject

        val entries =
            rootObject["entries"]
                ?.takeIf {
                    it.isJsonObject
                }
                ?.asJsonObject
                ?: return emptyList()

        return entries
            .entrySet()
            .map {
                it.key
            }
    }

    private fun readStringArray(
        root: JsonElement,
        key: String
    ): List<String> {
        if (!root.isJsonObject) {
            return emptyList()
        }

        return root
            .asJsonObject[key]
            ?.takeIf {
                it.isJsonArray
            }
            ?.asJsonArray
            ?.mapNotNull { element ->
                element
                    .takeIf {
                        it.isJsonPrimitive &&
                                it.asJsonPrimitive.isString
                    }
                    ?.asString
                    ?.trim()
                    ?.takeIf {
                        it.isNotBlank()
                    }
            }
            .orEmpty()
    }

    private fun readCandidate(
        json: JsonObject
    ): DiagnosticCandidate? {

        val serverKey =
            json.optionalString(
                key = "serverKey"
            )
                ?: return null

        val score =
            json["score"]
                ?.takeIf {
                    it.isJsonPrimitive &&
                            it.asJsonPrimitive.isNumber
                }
                ?.asDouble

        val sharedTokens =
            json["sharedTokens"]
                ?.takeIf {
                    it.isJsonArray
                }
                ?.asJsonArray
                ?.mapNotNull { element ->
                    element
                        .takeIf {
                            it.isJsonPrimitive &&
                                    it.asJsonPrimitive.isString
                        }
                        ?.asString
                }
                .orEmpty()

        return DiagnosticCandidate(
            serverKey = serverKey,
            score = score,
            sharedTokens = sharedTokens
        )
    }

    private fun findObjectByStringProperty(
        root: JsonElement,
        propertyName: String,
        expectedValue: String
    ): JsonObject? {

        if (root.isJsonObject) {
            val json =
                root.asJsonObject

            val actualValue =
                json.optionalString(
                    key = propertyName
                )

            if (actualValue == expectedValue) {
                return json
            }

            json.entrySet()
                .forEach { entry ->
                    val match =
                        findObjectByStringProperty(
                            root = entry.value,
                            propertyName = propertyName,
                            expectedValue = expectedValue
                        )

                    if (match != null) {
                        return match
                    }
                }
        }

        if (root.isJsonArray) {
            root.asJsonArray
                .forEach { element ->
                    val match =
                        findObjectByStringProperty(
                            root = element,
                            propertyName = propertyName,
                            expectedValue = expectedValue
                        )

                    if (match != null) {
                        return match
                    }
                }
        }

        return null
    }

    private fun containsObjectKey(
        root: JsonElement,
        expectedKey: String
    ): Boolean {

        if (root.isJsonObject) {
            val json =
                root.asJsonObject

            if (json.has(expectedKey)) {
                return true
            }

            return json.entrySet()
                .any { entry ->
                    containsObjectKey(
                        root = entry.value,
                        expectedKey = expectedKey
                    )
                }
        }

        if (root.isJsonArray) {
            return root.asJsonArray
                .any { element ->
                    containsObjectKey(
                        root = element,
                        expectedKey = expectedKey
                    )
                }
        }

        return false
    }

    private fun containsStringValue(
        root: JsonElement,
        expected: String
    ): Boolean {

        if (
            root.isJsonPrimitive &&
            root.asJsonPrimitive.isString
        ) {
            return root.asString == expected
        }

        if (root.isJsonObject) {
            return root.asJsonObject
                .entrySet()
                .any { entry ->
                    containsStringValue(
                        root = entry.value,
                        expected = expected
                    )
                }
        }

        if (root.isJsonArray) {
            return root.asJsonArray
                .any { element ->
                    containsStringValue(
                        root = element,
                        expected = expected
                    )
                }
        }

        return false
    }

    private fun containsCatalogMapping(
        root: JsonElement,
        catalogKey: String
    ): Boolean {

        if (containsObjectKey(root, catalogKey)) {
            return true
        }

        return findObjectByStringProperty(
            root = root,
            propertyName = "catalogKey",
            expectedValue = catalogKey
        ) != null
    }

    private fun parseJson(
        file: File
    ): JsonElement =
        JsonParser.parseString(
            file.readText()
        )

    private fun parseOptionalJson(
        file: File
    ): JsonElement? =
        file
            .takeIf {
                it.isFile
            }
            ?.let(::parseJson)

    private fun writeReport(
        report: LiverSausageNoRequestDiagnosticReport,
        file: File
    ) {
        file.parentFile?.let { parent ->
            if (!parent.exists()) {
                check(parent.mkdirs()) {
                    "Could not create diagnostic report directory: " +
                            parent.absolutePath
                }
            }
        }

        val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()

        file.writeText(
            gson.toJson(report) + "\n"
        )
    }

    private fun printReport(
        report: LiverSausageNoRequestDiagnosticReport,
        outputFile: File
    ) {
        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("LIVER SAUSAGE NO-REQUEST DIAGNOSTIC")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("Catalog key present       : ${report.catalogKeyPresent}")
        println("Exact server key present  : ${report.exactServerKeyPresent}")
        println("Related server keys       : ${report.relatedServerKeys.size}")
        println("Exact match present       : ${report.exactMatchPresent}")
        println(
            "Unmatched report entry    : " +
                    report.unmatchedMatchReportEntryPresent
        )
        println(
            "Nearest candidates        : " +
                    report.nearestCandidates.size
        )

        report.nearestCandidates
            .forEachIndexed { index, candidate ->
                println(
                    "  ${index + 1}. ${candidate.serverKey} " +
                            "score=${candidate.score ?: "-"} " +
                            "sharedTokens=${candidate.sharedTokens}"
                )
            }

        println("Match request present     : ${report.requestPresent}")
        println("Match decision present    : ${report.decisionPresent}")
        println("Mapping present           : ${report.mappingPresent}")
        println("Runtime present           : ${report.runtimePresent}")
        println("Coverage gap type         : ${report.coverageGapType}")
        println("First missing stage       : ${report.firstMissingStage}")
        println("Explanation               : ${report.explanation}")
        println("Report                    : ${outputFile.absolutePath}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    private fun JsonObject.optionalString(
        key: String
    ): String? =
        get(key)
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
}

private data class DiagnosticFiles(
    val catalogFile: File,
    val serverNutritionFile: File,
    val matchReportFile: File,
    val matchRequestFile: File,
    val matchDecisionFile: File,
    val centralMappingFile: File,
    val runtimeNutritionFile: File,
    val coverageGapFile: File,
    val diagnosticOutputFile: File
)

private data class LiverSausageNoRequestDiagnosticReport(
    val version: Int,
    val catalogKey: String,
    val catalogKeyPresent: Boolean,
    val exactServerKeyPresent: Boolean,
    val relatedServerKeys: List<String>,
    val exactMatchPresent: Boolean,
    val unmatchedMatchReportEntryPresent: Boolean,
    val nearestCandidates: List<DiagnosticCandidate>,
    val requestPresent: Boolean,
    val requestCandidates: List<DiagnosticCandidate>,
    val decisionPresent: Boolean,
    val decisionType: String?,
    val selectedServerKey: String?,
    val mappingPresent: Boolean,
    val runtimePresent: Boolean,
    val coverageGapPresent: Boolean,
    val coverageGapType: String?,
    val firstMissingStage: MissingStage,
    val explanation: String
)

private data class DiagnosticCandidate(
    val serverKey: String,
    val score: Double?,
    val sharedTokens: List<String>
)

private enum class MissingStage {
    CATALOG,
    MATCH_REPORT,
    RETRIEVAL_CANDIDATES,
    MATCH_REQUEST,
    MATCH_DECISION,
    MAPPING,
    RUNTIME,
    NONE
}