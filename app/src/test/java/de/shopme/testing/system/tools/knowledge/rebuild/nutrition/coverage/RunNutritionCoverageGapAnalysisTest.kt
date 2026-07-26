package de.shopme.testing.system.tools.knowledge.rebuild.nutrition.coverage

import com.google.gson.Gson
import de.shopme.tools.knowledge.rebuild.nutrition.coverage.NutritionCoverageGapAnalysisWriter
import de.shopme.tools.knowledge.rebuild.nutrition.coverage.NutritionCoverageGapAnalyzer
import de.shopme.tools.knowledge.rebuild.nutrition.coverage.NutritionCoverageGapReport
import de.shopme.tools.knowledge.rebuild.nutrition.coverage.NutritionCoverageGapType
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RunNutritionCoverageGapAnalysisTest {

    @Test
    fun runNutritionCoverageGapAnalysis() {

        val projectRoot =
            File("..")
                .canonicalFile

        val coverageGapReportFile =
            projectRoot.resolve(
                "data/generated/knowledge/reports/" +
                        "nutrition.coverage-gaps.json"
            )

        require(coverageGapReportFile.isFile) {
            "Nutrition coverage-gap report does not exist. Run " +
                    "RunNutritionCoverageGapClassificationTest first: " +
                    coverageGapReportFile.absolutePath
        }

        val report =
            Gson().fromJson(
                coverageGapReportFile.readText(),
                NutritionCoverageGapReport::class.java
            )

        val analysis =
            NutritionCoverageGapAnalyzer()
                .analyze(
                    report =
                        report,
                    investigatedCatalogKeys =
                        INVESTIGATED_CATALOG_KEYS
                )

        val outputFile =
            projectRoot.resolve(
                "data/generated/knowledge/reports/" +
                        "nutrition.coverage-gap-analysis.json"
            )

        NutritionCoverageGapAnalysisWriter(
            outputFile =
                outputFile
        )
            .write(
                analysis =
                    analysis
            )

        assertEquals(
            expected =
                report.missingCatalogItemCount,
            actual =
                analysis.countsByType.values.sum(),
            message =
                "Every missing catalog key must be represented in the " +
                        "analysis."
        )

        assertEquals(
            expected =
                analysis.exactMatchNotInRuntimeCount,
            actual =
                analysis.countsByType[
                    NutritionCoverageGapType
                        .EXACT_MATCH_NOT_IN_RUNTIME
                        .name
                ] ?: 0
        )

        assertEquals(
            expected =
                analysis.noRequestCount,
            actual =
                analysis.countsByType[
                    NutritionCoverageGapType.NO_REQUEST.name
                ] ?: 0
        )

        val liverSausage =
            analysis.investigatedCatalogKeys
                .singleOrNull {
                    it.catalogKey ==
                            "liver sausage"
                }

        assertNotNull(
            actual =
                liverSausage,
            message =
                "The targeted liver sausage analysis is missing."
        )

        assertTrue(
            actual =
                liverSausage.presentInReport,
            message =
                "liver sausage should still be present as a productive " +
                        "coverage gap until the exact match is persisted."
        )

        assertEquals(
            expected =
                NutritionCoverageGapType
                    .EXACT_MATCH_NOT_IN_RUNTIME
                    .name,
            actual =
                liverSausage.type,
            message =
                "liver sausage must be classified as an exact match " +
                        "that is missing from runtime."
        )

        assertNotEquals(
            illegal =
                NutritionCoverageGapType.NO_REQUEST.name,
            actual =
                liverSausage.type,
            message =
                "liver sausage must no longer be classified as " +
                        "NO_REQUEST."
        )

        require(outputFile.isFile) {
            "Nutrition coverage-gap analysis file was not written."
        }

        printAnalysis(
            analysis =
                analysis,
            outputFile =
                outputFile
        )

        assertEquals(
            expected =
                0,
            actual =
                analysis.exactMatchNotInRuntimeCount,
            message =
                "No reported exact nutrition match may remain outside " +
                        "the persisted runtime mappings."
        )

        assertEquals(
            expected =
                0,
            actual =
                analysis.noRequestCount,
            message =
                "Every missing catalog key must have a persisted match request."
        )

        assertEquals(
            expected =
                0,
            actual =
                analysis.noDecisionCount,
            message =
                "Every persisted nutrition match request must have a decision."
        )

        assertEquals(
            expected =
                0,
            actual =
                analysis.matchNotPersistedCount,
            message =
                "Every accepted MATCH decision must be persisted."
        )
    }

    private fun printAnalysis(
        analysis:
        de.shopme.tools.knowledge.rebuild.nutrition.coverage
        .NutritionCoverageGapAnalysis,
        outputFile: File
    ) {
        println()
        println("Nutrition coverage-gap analysis")
        println("--------------------------------")
        println(
            "Catalog items: " +
                    analysis.catalogItemCount
        )
        println(
            "Covered catalog items: " +
                    analysis.coveredCatalogItemCount
        )
        println(
            "Missing catalog items: " +
                    analysis.missingCatalogItemCount
        )
        println(
            "Classified gaps: " +
                    analysis.classifiedGapCount
        )
        println(
            "Unclassified gaps: " +
                    analysis.unclassifiedGapCount
        )

        println()
        println("Counts by type:")

        analysis.countsByType
            .forEach { (type, count) ->

                println(
                    "  $type: $count"
                )
            }

        println()
        println(
            "EXACT_MATCH_NOT_IN_RUNTIME: " +
                    analysis.exactMatchNotInRuntimeCount
        )

        analysis.exactMatchNotInRuntimeCatalogKeys
            .forEach {
                println(
                    "  - $it"
                )
            }

        println()
        println(
            "NO_REQUEST: " +
                    analysis.noRequestCount
        )

        analysis.noRequestCatalogKeys
            .forEach {
                println(
                    "  - $it"
                )
            }

        println()
        println(
            "MATCH_NOT_PERSISTED: " +
                    analysis.matchNotPersistedCount
        )

        analysis.matchNotPersistedCatalogKeys
            .forEach {
                println(
                    "  - $it"
                )
            }

        println()
        println(
            "NO_DECISION: " +
                    analysis.noDecisionCount
        )

        analysis.noDecisionCatalogKeys
            .forEach {
                println(
                    "  - $it"
                )
            }

        println()
        println(
            "NO_CANDIDATES: " +
                    analysis.noCandidatesCount
        )

        analysis.noCandidatesCatalogKeys
            .forEach {
                println(
                    "  - $it"
                )
            }

        println()
        println(
            "NO_MATCH: " +
                    analysis.noMatchCount
        )

        analysis.countsByNoMatchCause
            .forEach { (cause, count) ->

                println(
                    "  $cause: $count"
                )
            }

        println()
        println("Investigated catalog keys:")

        analysis.investigatedCatalogKeys
            .forEach { investigated ->

                println(
                    "  ${investigated.catalogKey}: " +
                            (
                                    investigated.type
                                        ?: "NOT_PRESENT_IN_GAP_REPORT"
                                    )
                )

                if (investigated.presentInReport) {
                    println(
                        "    requestExists=" +
                                investigated.requestExists
                    )
                    println(
                        "    decisionExists=" +
                                investigated.decisionExists
                    )
                    println(
                        "    decisionType=" +
                                investigated.decisionType
                    )
                    println(
                        "    selectedServerKey=" +
                                investigated.selectedServerKey
                    )
                    println(
                        "    candidateCount=" +
                                investigated.candidateCount
                    )
                    println(
                        "    topCandidateKey=" +
                                investigated.topCandidateKey
                    )
                    println(
                        "    topCandidateScore=" +
                                investigated.topCandidateScore
                    )
                    println(
                        "    details=" +
                                investigated.details
                    )
                }
            }

        println()
        println(
            "Analysis written to: " +
                    outputFile.absolutePath
        )
    }

    private companion object {

        val INVESTIGATED_CATALOG_KEYS =
            sortedSetOf(
                "chervil",
                "liver sausage",
                "mace",
                "salsify",
                "teewurst",
                "toffifee"
            )
    }
}