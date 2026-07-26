package de.shopme.testing.system.tools.knowledge.mapping.catalog

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchRequestWriter
import de.shopme.tools.knowledge.mapping.catalog.DefaultCatalogKnowledgeMatchRequestGenerator
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegenerateNutritionKnowledgeMatchRequestsTest {

    @Test
    fun regenerateNutritionMatchRequestsFromUpdatedServerKnowledge() {

        val projectRoot =
            File("..")
                .canonicalFile

        val serverNutritionFile =
            projectRoot.resolve(
                "data/generated/knowledge/server/nutrition.json"
            )

        val nutritionMatchReportFile =
            projectRoot.resolve(
                "data/generated/reports/catalog-server-matches/nutrition.matches.json"
            )

        val nutritionMatchRequestFile =
            projectRoot.resolve(
                "data/generated/knowledge/match-requests/nutrition.match-requests.json"
            )

        require(serverNutritionFile.isFile) {
            "Server nutrition artifact does not exist: " +
                    serverNutritionFile.absolutePath
        }

        require(nutritionMatchReportFile.isFile) {
            "Nutrition match report does not exist: " +
                    nutritionMatchReportFile.absolutePath
        }

        requireMatchReportIsCurrent(
            serverNutritionFile = serverNutritionFile,
            nutritionMatchReportFile = nutritionMatchReportFile
        )

        val requests =
            DefaultCatalogKnowledgeMatchRequestGenerator()
                .generate(
                    matchReportFile =
                        nutritionMatchReportFile
                )

        assertTrue(
            actual =
                requests.requests.isNotEmpty(),
            message =
                "Updated nutrition match report produced no match requests."
        )

        verifyCiqualBackedRequests(
            requestsByCatalogKey =
                requests.requests
                    .associateBy {
                        it.catalogKey
                    }
        )

        CatalogKnowledgeMatchRequestWriter()
            .write(
                requests = requests,
                file = nutritionMatchRequestFile
            )

        verifyWrittenRequestArtifact(
            file = nutritionMatchRequestFile,
            expectedVersion = requests.version,
            expectedRequestCount = requests.requests.size
        )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("NUTRITION MATCH REQUESTS REGENERATED")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Server nutrition artifact : " +
                    serverNutritionFile.path
        )
        println(
            "Nutrition match report    : " +
                    nutritionMatchReportFile.path
        )
        println(
            "Nutrition match requests  : " +
                    nutritionMatchRequestFile.path
        )
        println(
            "Request count             : " +
                    requests.requests.size
        )
        println(
            "chervil candidates        : " +
                    requests.requests
                        .single {
                            it.catalogKey == "chervil"
                        }
                        .candidates
                        .joinToString {
                            it.serverKey
                        }
        )
        println(
            "salsify candidates        : " +
                    requests.requests
                        .single {
                            it.catalogKey == "salsify"
                        }
                        .candidates
                        .joinToString {
                            it.serverKey
                        }
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    private fun requireMatchReportIsCurrent(
        serverNutritionFile: File,
        nutritionMatchReportFile: File
    ) {
        require(
            nutritionMatchReportFile.lastModified() >=
                    serverNutritionFile.lastModified()
        ) {
            buildString {
                appendLine(
                    "Nutrition match report is older than the server " +
                            "nutrition artifact."
                )
                appendLine(
                    "Server artifact: " +
                            serverNutritionFile.absolutePath
                )
                appendLine(
                    "Match report: " +
                            nutritionMatchReportFile.absolutePath
                )
                append(
                    "Run ReportUnmatchedCatalogKnowledgeKeysTest before " +
                            "regenerating nutrition match requests."
                )
            }
        }
    }

    private fun verifyCiqualBackedRequests(
        requestsByCatalogKey:
        Map<String, de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchRequest>
    ) {
        val chervil =
            requestsByCatalogKey["chervil"]

        assertTrue(
            actual =
                chervil != null,
            message =
                "Updated nutrition match requests must contain chervil."
        )

        assertTrue(
            actual =
                chervil.candidates.any { candidate ->
                    candidate.serverKey ==
                            "chervil raw" ||
                            candidate.serverKey ==
                            "chervil dried"
                },
            message =
                "CIQUAL-backed chervil candidates are missing. " +
                        "Actual candidates: " +
                        chervil.candidates.joinToString {
                            it.serverKey
                        }
        )

        val salsify =
            requestsByCatalogKey["salsify"]

        assertTrue(
            actual =
                salsify != null,
            message =
                "Updated nutrition match requests must contain salsify."
        )

        assertTrue(
            actual =
                salsify.candidates.any { candidate ->
                    candidate.serverKey.startsWith(
                        prefix = "salsify "
                    )
                },
            message =
                "CIQUAL-backed salsify candidates are missing. " +
                        "Actual candidates: " +
                        salsify.candidates.joinToString {
                            it.serverKey
                        }
        )
    }

    private fun verifyWrittenRequestArtifact(
        file: File,
        expectedVersion: Int,
        expectedRequestCount: Int
    ) {
        assertTrue(
            actual =
                file.isFile,
            message =
                "Nutrition match request file was not written: " +
                        file.absolutePath
        )

        assertTrue(
            actual =
                file.length() > 0L,
            message =
                "Written nutrition match request file is empty: " +
                        file.absolutePath
        )

        val root =
            JsonParser
                .parseString(
                    file.readText()
                )
                .asJsonObject

        assertEquals(
            expected =
                expectedVersion,
            actual =
                root.requiredInt(
                    key = "version"
                )
        )

        val writtenRequests =
            root.requiredArraySize(
                key = "requests"
            )

        assertEquals(
            expected =
                expectedRequestCount,
            actual =
                writtenRequests
        )

        val writtenRequestsByCatalogKey =
            root["requests"]
                .asJsonArray
                .asSequence()
                .map {
                    it.asJsonObject
                }
                .associateBy {
                    it.requiredString(
                        key = "catalogKey"
                    )
                }

        assertTrue(
            actual =
                "chervil" in
                        writtenRequestsByCatalogKey,
            message =
                "Written nutrition match request artifact does not " +
                        "contain chervil."
        )

        assertTrue(
            actual =
                "salsify" in
                        writtenRequestsByCatalogKey,
            message =
                "Written nutrition match request artifact does not " +
                        "contain salsify."
        )
    }

    private fun JsonObject.requiredInt(
        key: String
    ): Int =
        get(key)
            ?.takeIf {
                !it.isJsonNull &&
                        it.isJsonPrimitive
            }
            ?.asInt
            ?: error(
                "Missing integer '$key'."
            )

    private fun JsonObject.requiredArraySize(
        key: String
    ): Int =
        get(key)
            ?.takeIf {
                it.isJsonArray
            }
            ?.asJsonArray
            ?.size()
            ?: error(
                "Missing JSON array '$key'."
            )

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
            ?.takeIf {
                it.isNotBlank()
            }
            ?: error(
                "Missing or blank string '$key'."
            )
}