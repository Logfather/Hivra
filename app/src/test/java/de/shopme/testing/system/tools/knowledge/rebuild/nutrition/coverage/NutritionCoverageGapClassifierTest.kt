package de.shopme.testing.system.tools.knowledge.rebuild.nutrition.coverage

import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeRebuildSnapshot
import de.shopme.tools.knowledge.rebuild.nutrition.NutritionKnowledgeSnapshotReader
import de.shopme.tools.knowledge.rebuild.nutrition.coverage.NutritionCoverageGapClassifier
import de.shopme.tools.knowledge.rebuild.nutrition.coverage.NutritionCoverageGapType
import de.shopme.tools.knowledge.rebuild.nutrition.coverage.NutritionNoMatchCause
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class NutritionCoverageGapClassifierTest {

    @Test
    fun classifyEveryMissingNutritionCatalogKeyExactlyOnce() {

        val directory =
            Files.createTempDirectory(
                "nutrition-coverage-gap-classifier"
            )
                .toFile()

        try {
            val catalogFile =
                File(
                    directory,
                    "catalog.json"
                )

            val exactMappingFile =
                File(
                    directory,
                    "nutrition.mappings.json"
                )

            val catalogServerMappingFile =
                File(
                    directory,
                    "catalog-server.mappings.json"
                )

            val exactMatchReportFile =
                File(
                    directory,
                    "nutrition.matches.json"
                )

            val requestFile =
                File(
                    directory,
                    "nutrition.match-requests.json"
                )

            val decisionFile =
                File(
                    directory,
                    "nutrition.match-decisions.json"
                )



            catalogFile.writeText(
                """
                {
                  "foods": [
                    {
                      "normalizedEnglish": "apple"
                    },
                    {
                      "normalizedEnglish": "banana"
                    },
                    {
                      "normalizedEnglish": "chervil"
                    },
                    {
                      "normalizedEnglish": "frozen berry mix"
                    },
                    {
                      "normalizedEnglish": "liver sausage"
                    },
                    {
                      "normalizedEnglish": "plain yogurt"
                    }
                  ]
                }
                """.trimIndent()
            )

            exactMappingFile.writeText(
                """
                {
                  "version": 1,
                  "mappings": [
                    {
                      "catalogKey": "apple",
                      "serverKey": "apple",
                      "serverArtifact": "nutrition.json"
                    }
                  ]
                }
                """.trimIndent()
            )

            catalogServerMappingFile.writeText(
                """
                {
                  "version": 1,
                  "mappings": [
                    {
                      "catalogKey": "banana",
                      "serverKey": "banana raw",
                      "sourceArtifact": "nutrition.json",
                      "method": "AI_VALIDATED",
                      "confidence": 0.95,
                      "reason": "Validated representative mapping."
                    }
                  ]
                }
                """.trimIndent()
            )

            exactMatchReportFile.writeText(
                """
                {
                  "version": 1,
                  "serverArtifact": "nutrition.json",
                  "exactMatches": [
                    "apple",
                    "liver sausage"
                  ],
                  "unmatched": []
                }
                """.trimIndent()
            )

            requestFile.writeText(
                """
                {
                  "version": 1,
                  "requests": [
                    {
                      "catalogKey": "frozen berry mix",
                      "serverArtifact": "nutrition.json",
                      "candidates": [
                        {
                          "serverKey": "berry",
                          "diagnosticScore": 0.42,
                          "sharedTokens": [
                            "berry"
                          ]
                        }
                      ]
                    },
                    {
                      "catalogKey": "plain yogurt",
                      "serverArtifact": "nutrition.json",
                      "candidates": [
                        {
                          "serverKey": "plain lowfat yogurt",
                          "diagnosticScore": 0.91,
                          "sharedTokens": [
                            "plain",
                            "yogurt"
                          ]
                        }
                      ]
                    }
                  ]
                }
                """.trimIndent()
            )

            decisionFile.writeText(
                """
                {
                  "version": 1,
                  "decisions": [
                    {
                      "catalogKey": "frozen berry mix",
                      "serverArtifact": "nutrition.json",
                      "type": "NO_MATCH",
                      "selectedServerKey": null,
                      "confidence": 0.91,
                      "reason": "Candidate is not sufficiently equivalent.",
                      "decisionSource": "CHAT_GPT"
                    },
                    {
                      "catalogKey": "plain yogurt",
                      "serverArtifact": "nutrition.json",
                      "type": "MATCH",
                      "selectedServerKey": "plain lowfat yogurt",
                      "confidence": 0.91,
                      "reason": "Representative nutrition match.",
                      "decisionSource": "CHAT_GPT"
                    }
                  ]
                }
                """.trimIndent()
            )

            val snapshotReader =
                object :
                    NutritionKnowledgeSnapshotReader {

                    override fun read(
                    ): NutritionKnowledgeRebuildSnapshot {

                        return NutritionKnowledgeRebuildSnapshot(
                            mappingCount =
                                1,
                            catalogItemCount =
                                6,
                            exactMatchCount =
                                1,
                            mappedMatchCount =
                                1,
                            runtimeEntryCount =
                                2,
                            coveredCatalogItemCount =
                                2,
                            missingCatalogItemCount =
                                4,
                            coverage =
                                2.0 / 6.0
                        )
                    }
                }

            val report =
                NutritionCoverageGapClassifier(
                    catalogFile =
                        catalogFile,
                    exactMappingFile =
                        exactMappingFile,
                    catalogServerMappingFile =
                        catalogServerMappingFile,
                    exactMatchReportFile =
                        exactMatchReportFile,
                    requestFile =
                        requestFile,
                    decisionFile =
                        decisionFile,
                    snapshotReader =
                        snapshotReader
                )
                    .classify()

            assertEquals(
                expected =
                    6,
                actual =
                    report.catalogItemCount
            )

            assertEquals(
                expected =
                    2,
                actual =
                    report.coveredCatalogItemCount
            )

            assertEquals(
                expected =
                    4,
                actual =
                    report.missingCatalogItemCount
            )

            assertEquals(
                expected =
                    4,
                actual =
                    report.classifiedGapCount
            )

            assertEquals(
                expected =
                    0,
                actual =
                    report.unclassifiedGapCount
            )

            assertEquals(
                expected =
                    listOf(
                        "chervil",
                        "frozen berry mix",
                        "liver sausage",
                        "plain yogurt"
                    ),
                actual =
                    report.gaps.map {
                        it.catalogKey
                    }
            )

            assertEquals(
                expected =
                    NutritionCoverageGapType.VERY_LOW_SCORE,
                actual =
                    report.gaps
                        .first {
                            it.catalogKey ==
                                    "frozen berry mix"
                        }
                        .type
            )

            val liverSausageGap =
                report.gaps
                    .first {
                        it.catalogKey ==
                                "liver sausage"
                    }

            assertEquals(
                expected =
                    NutritionCoverageGapType
                        .EXACT_MATCH_NOT_IN_RUNTIME,
                actual =
                    liverSausageGap.type
            )

            assertFalse(
                actual =
                    liverSausageGap.requestExists
            )

            assertEquals(
                expected =
                    0,
                actual =
                    liverSausageGap.candidateCount
            )

            assertNull(
                actual =
                    liverSausageGap.topCandidateKey
            )

            assertFalse(
                actual =
                    liverSausageGap.mappingExists
            )

            assertEquals(
                expected =
                    NutritionCoverageGapType.MATCH_NOT_PERSISTED,
                actual =
                    report.gaps
                        .first {
                            it.catalogKey ==
                                    "plain yogurt"
                        }
                        .type
            )

            assertEquals(
                expected =
                    1,
                actual =
                    report.countsByType[
                        NutritionCoverageGapType
                            .EXACT_MATCH_NOT_IN_RUNTIME
                            .name
                    ]
            )

            assertEquals(
                expected =
                    1,
                actual =
                    report.countsByType[
                        NutritionCoverageGapType
                            .NO_REQUEST
                            .name
                    ] ?: 0
            )

        } finally {

            directory.deleteRecursively()
        }
    }

    @Test
    fun preferSpecificSemanticCauseOverScoreCluster() {

        val directory =
            Files.createTempDirectory(
                "nutrition-coverage-gap-semantic-priority"
            )
                .toFile()

        try {
            val catalogFile =
                File(
                    directory,
                    "catalog.json"
                )

            val exactMappingFile =
                File(
                    directory,
                    "nutrition.mappings.json"
                )

            val catalogServerMappingFile =
                File(
                    directory,
                    "catalog-server.mappings.json"
                )

            val exactMatchReportFile =
                File(
                    directory,
                    "nutrition.matches.json"
                )

            val requestFile =
                File(
                    directory,
                    "nutrition.match-requests.json"
                )

            val decisionFile =
                File(
                    directory,
                    "nutrition.match-decisions.json"
                )

            catalogFile.writeText(
                """
                {
                  "foods": [
                    {
                      "normalizedEnglish": "acerola juice"
                    }
                  ]
                }
                """.trimIndent()
            )

            exactMappingFile.writeText(
                """
                {
                  "version": 1,
                  "mappings": []
                }
                """.trimIndent()
            )

            catalogServerMappingFile.writeText(
                """
                {
                  "version": 1,
                  "mappings": []
                }
                """.trimIndent()
            )

            exactMatchReportFile.writeText(
                """
                {
                  "version": 1,
                  "serverArtifact": "nutrition.json",
                  "exactMatches": [],
                  "unmatched": [
                    "acerola juice"
                  ]
                }
                """.trimIndent()
            )

            requestFile.writeText(
                """
                {
                  "version": 1,
                  "requests": [
                    {
                      "catalogKey": "acerola juice",
                      "serverArtifact": "nutrition.json",
                      "candidates": [
                        {
                          "serverKey": "orange mango acerola juice drink",
                          "diagnosticScore": 0.80,
                          "sharedTokens": [
                            "acerola",
                            "juice"
                          ]
                        },
                        {
                          "serverKey": "mango acerola juice beverage",
                          "diagnosticScore": 0.78,
                          "sharedTokens": [
                            "acerola",
                            "juice"
                          ]
                        }
                      ]
                    }
                  ]
                }
                """.trimIndent()
            )

            decisionFile.writeText(
                """
                {
                  "version": 1,
                  "decisions": [
                    {
                      "catalogKey": "acerola juice",
                      "serverArtifact": "nutrition.json",
                      "type": "NO_MATCH",
                      "selectedServerKey": null,
                      "confidence": 0.93,
                      "reason": "Candidates are more specific mixed drinks.",
                      "decisionSource": "CHAT_GPT"
                    }
                  ]
                }
                """.trimIndent()
            )

            val snapshotReader =
                object :
                    NutritionKnowledgeSnapshotReader {

                    override fun read(
                    ): NutritionKnowledgeRebuildSnapshot {

                        return NutritionKnowledgeRebuildSnapshot(
                            mappingCount =
                                0,
                            catalogItemCount =
                                1,
                            exactMatchCount =
                                0,
                            mappedMatchCount =
                                0,
                            runtimeEntryCount =
                                0,
                            coveredCatalogItemCount =
                                0,
                            missingCatalogItemCount =
                                1,
                            coverage =
                                0.0
                        )
                    }
                }

            val report =
                NutritionCoverageGapClassifier(
                    catalogFile =
                        catalogFile,
                    exactMappingFile =
                        exactMappingFile,
                    catalogServerMappingFile =
                        catalogServerMappingFile,
                    exactMatchReportFile =
                        exactMatchReportFile,
                    requestFile =
                        requestFile,
                    decisionFile =
                        decisionFile,
                    snapshotReader =
                        snapshotReader
                )
                    .classify()

            val gap =
                report.gaps.single()

            assertEquals(
                expected =
                    NutritionCoverageGapType.TOO_SPECIFIC,
                actual =
                    gap.type
            )

        } finally {

            directory.deleteRecursively()
        }
    }

    @Test
    fun classifyResidualNoMatchCauseDeterministically() {

        val directory =
            Files.createTempDirectory(
                "nutrition-no-match-cause"
            )
                .toFile()

        try {
            val catalogFile =
                File(
                    directory,
                    "catalog.json"
                )

            val exactMappingFile =
                File(
                    directory,
                    "nutrition.mappings.json"
                )

            val catalogServerMappingFile =
                File(
                    directory,
                    "catalog-server.mappings.json"
                )

            val exactMatchReportFile =
                File(
                    directory,
                    "nutrition.matches.json"
                )

            val requestFile =
                File(
                    directory,
                    "nutrition.match-requests.json"
                )

            val decisionFile =
                File(
                    directory,
                    "nutrition.match-decisions.json"
                )

            catalogFile.writeText(
                """
                {
                  "foods": [
                    {
                      "normalizedEnglish": "apple juice"
                    }
                  ]
                }
                """.trimIndent()
            )

            exactMappingFile.writeText(
                """
                {
                  "version": 1,
                  "mappings": []
                }
                """.trimIndent()
            )

            catalogServerMappingFile.writeText(
                """
                {
                  "version": 1,
                  "mappings": []
                }
                """.trimIndent()
            )

            exactMatchReportFile.writeText(
                """
                {
                  "version": 1,
                  "serverArtifact": "nutrition.json",
                  "exactMatches": [],
                  "unmatched": [
                    "apple juice"
                  ]
                }
                """.trimIndent()
            )

            requestFile.writeText(
                """
                {
                  "version": 1,
                  "requests": [
                    {
                      "catalogKey": "apple juice",
                      "serverArtifact": "nutrition.json",
                      "candidates": [
                        {
                          "serverKey": "apple drink",
                          "diagnosticScore": 0.74,
                          "sharedTokens": [
                            "apple"
                          ]
                        },
                        {
                          "serverKey": "apple beverage",
                          "diagnosticScore": 0.68,
                          "sharedTokens": [
                            "apple"
                          ]
                        }
                      ]
                    }
                  ]
                }
                """.trimIndent()
            )

            decisionFile.writeText(
                """
                {
                  "version": 1,
                  "decisions": [
                    {
                      "catalogKey": "apple juice",
                      "serverArtifact": "nutrition.json",
                      "type": "NO_MATCH",
                      "selectedServerKey": null,
                      "confidence": 0.90,
                      "reason": "The candidate is broader than apple juice.",
                      "decisionSource": "CHAT_GPT"
                    }
                  ]
                }
                """.trimIndent()
            )

            val snapshotReader =
                object :
                    NutritionKnowledgeSnapshotReader {

                    override fun read(
                    ): NutritionKnowledgeRebuildSnapshot {

                        return NutritionKnowledgeRebuildSnapshot(
                            mappingCount =
                                0,
                            catalogItemCount =
                                1,
                            exactMatchCount =
                                0,
                            mappedMatchCount =
                                0,
                            runtimeEntryCount =
                                0,
                            coveredCatalogItemCount =
                                0,
                            missingCatalogItemCount =
                                1,
                            coverage =
                                0.0
                        )
                    }
                }

            val report =
                NutritionCoverageGapClassifier(
                    catalogFile =
                        catalogFile,
                    exactMappingFile =
                        exactMappingFile,
                    catalogServerMappingFile =
                        catalogServerMappingFile,
                    exactMatchReportFile =
                        exactMatchReportFile,
                    requestFile =
                        requestFile,
                    decisionFile =
                        decisionFile,
                    snapshotReader =
                        snapshotReader
                )
                    .classify()

            val gap =
                report.gaps.single()

            assertEquals(
                expected =
                    NutritionCoverageGapType.NO_MATCH,
                actual =
                    gap.type
            )

            assertEquals(
                expected =
                    NutritionNoMatchCause
                        .MODERATE_TOP_CANDIDATE_REJECTED,
                actual =
                    gap.noMatchCause
            )

        } finally {

            directory.deleteRecursively()
        }
    }
}