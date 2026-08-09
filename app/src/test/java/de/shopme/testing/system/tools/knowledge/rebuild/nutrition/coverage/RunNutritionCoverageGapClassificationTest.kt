package de.shopme.testing.system.tools.knowledge.rebuild.nutrition.coverage

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.knowledge.rebuild.nutrition.adapter.DefaultNutritionKnowledgeSnapshotReader
import de.shopme.tools.knowledge.rebuild.nutrition.coverage.NutritionCoverageGapClassifier
import de.shopme.tools.knowledge.rebuild.nutrition.coverage.NutritionCoverageGapReportWriter
import de.shopme.tools.knowledge.rebuild.nutrition.runner.NutritionKnowledgeRebuildProjectFiles
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunNutritionCoverageGapClassificationTest {

    @Test
    fun runNutritionCoverageGapClassification() {

        val projectRoot =
            findProjectRoot()

        val files =
            NutritionKnowledgeRebuildProjectFiles
                .fromProjectRoot(
                    projectRoot =
                        projectRoot
                )

        val snapshotReader =
            DefaultNutritionKnowledgeSnapshotReader(
                catalogFile =
                    files.catalogFile,
                exactMappingFile =
                    files.exactMappingFile,
                runtimeNutritionFile =
                    files.runtimeNutritionFile,
                mappingFile =
                    files.outputMappingFile
            )

        val report =
            NutritionCoverageGapClassifier(
                catalogFile =
                    KnowledgeBuildPaths
                        .default()
                        .canonicalFoodCatalog,
                exactMappingFile =
                    projectRoot.resolve(
                        "data/generated/knowledge/mappings/" +
                                "nutrition.mappings.json"
                    ),
                catalogServerMappingFile =
                    projectRoot.resolve(
                        "data/generated/knowledge/mappings/" +
                                "catalog-server.mappings.json"
                    ),
                exactMatchReportFile =
                    projectRoot.resolve(
                        "data/generated/reports/catalog-server-matches/" +
                                "nutrition.matches.json"
                    ),
                requestFile =
                    projectRoot.resolve(
                        "data/generated/knowledge/match-requests/" +
                                "nutrition.match-requests.json"
                    ),
                decisionFile =
                    projectRoot.resolve(
                        "data/generated/knowledge/reports/" +
                                "nutrition.match-diagnostics.json"
                    ),
                snapshotReader =
                    snapshotReader
            )
                .classify()

        val reportFile =
            File(
                projectRoot,
                "data/generated/knowledge/reports/" +
                        "nutrition.coverage-gaps.json"
            )

        NutritionCoverageGapReportWriter(
            outputFile =
                reportFile
        )
            .write(
                report =
                    report
            )

        printReport(
            report =
                report,
            reportFile =
                reportFile
        )

        assertEquals(
            expected =
                report.catalogItemCount,
            actual =
                report.coveredCatalogItemCount +
                        report.missingCatalogItemCount
        )

        assertEquals(
            expected =
                report.missingCatalogItemCount,
            actual =
                report.gaps.size
        )

        assertEquals(
            expected =
                0,
            actual =
                report.unclassifiedGapCount,
            message =
                "Every missing nutrition catalog key must receive a " +
                        "deterministic classification."
        )

        assertTrue(
            actual =
                reportFile.isFile,
            message =
                "Nutrition coverage gap report was not written: " +
                        reportFile.absolutePath
        )
    }

    private fun printReport(
        report:
        de.shopme.tools.knowledge.rebuild.nutrition.coverage
        .NutritionCoverageGapReport,
        reportFile: File
    ) {
        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("NUTRITION COVERAGE GAP CLASSIFICATION")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Catalog items       : " +
                    report.catalogItemCount
        )
        println(
            "Covered             : " +
                    report.coveredCatalogItemCount
        )
        println(
            "Missing             : " +
                    report.missingCatalogItemCount
        )
        println(
            "Classified          : " +
                    report.classifiedGapCount
        )
        println(
            "Unclassified        : " +
                    report.unclassifiedGapCount
        )
        println()

        report.countsByType
            .forEach { (type, count) ->

                println(
                    type.padEnd(
                        length =
                            24
                    ) +
                            ": " +
                            count
                )
            }

        println()
        println(
            "Report              : " +
                    reportFile.absolutePath
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    private fun findProjectRoot():
            File {

        val userDirectory =
            requireNotNull(
                System.getProperty(
                    "user.dir"
                )
            ) {
                "System property 'user.dir' is not available."
            }

        val workingDirectory =
            File(
                userDirectory
            )
                .absoluteFile

        return generateSequence(
            seed =
                workingDirectory
        ) {
            it.parentFile
        }
            .firstOrNull { candidate ->

                File(
                    candidate,
                    "app"
                )
                    .isDirectory &&
                        File(
                            candidate,
                            "data"
                        )
                            .isDirectory
            }
            ?: error(
                "Could not locate ShopMe project root from: " +
                        workingDirectory.absolutePath
            )
    }

    private fun verifyUpdatedNutritionCoverageGapClassification(
        report:
        de.shopme.tools.knowledge.rebuild.nutrition.coverage.NutritionCoverageGapReport
    ) {
        val gapsByCatalogKey =
            report.gaps
                .associateBy {
                    it.catalogKey
                }

        verifyResolvedNoRequestGap(
            catalogKey = "chervil",
            gapsByCatalogKey = gapsByCatalogKey
        )

        verifyResolvedNoRequestGap(
            catalogKey = "salsify",
            gapsByCatalogKey = gapsByCatalogKey
        )

        val noRequestGaps =
            report.gaps
                .filter {
                    it.type ==
                            de.shopme.tools.knowledge.rebuild.nutrition.coverage
                                .NutritionCoverageGapType.NO_REQUEST
                }
                .sortedBy {
                    it.catalogKey
                }

        check(
            noRequestGaps.none {
                it.catalogKey == "chervil" ||
                        it.catalogKey == "salsify"
            }
        ) {
            "CIQUAL-backed catalog keys must no longer be classified as " +
                    "NO_REQUEST."
        }

        check(
            report.gaps.size ==
                    report.missingCatalogItemCount
        ) {
            "Every missing nutrition catalog key must be classified " +
                    "exactly once: gaps=${report.gaps.size}, " +
                    "missing=${report.missingCatalogItemCount}."
        }

        check(
            report.classifiedGapCount +
                    report.unclassifiedGapCount ==
                    report.missingCatalogItemCount
        ) {
            "Classified and unclassified nutrition gaps do not cover " +
                    "all missing catalog keys."
        }

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("NUTRITION COVERAGE GAPS RECLASSIFIED")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Catalog items       : " +
                    report.catalogItemCount
        )
        println(
            "Covered             : " +
                    report.coveredCatalogItemCount
        )
        println(
            "Missing             : " +
                    report.missingCatalogItemCount
        )
        println(
            "Classified          : " +
                    report.classifiedGapCount
        )
        println(
            "Unclassified        : " +
                    report.unclassifiedGapCount
        )
        println()
        println("Counts by type:")

        report.countsByType
            .toSortedMap()
            .forEach { (type, count) ->
                println(
                    "$type=$count"
                )
            }

        println()
        println(
            "Remaining NO_REQUEST keys: " +
                    if (noRequestGaps.isEmpty()) {
                        "-"
                    } else {
                        noRequestGaps.joinToString {
                            it.catalogKey
                        }
                    }
        )

        listOf(
            "chervil",
            "salsify",
            "leberkaese",
            "mace",
            "teewurst",
            "toffifee"
        )
            .forEach { catalogKey ->

                val gap =
                    gapsByCatalogKey[
                        catalogKey
                    ]

                println(
                    "$catalogKey: " +
                            when {
                                gap == null ->
                                    "COVERED_OR_NOT_MISSING"

                                else ->
                                    buildString {
                                        append(
                                            gap.type.name
                                        )
                                        append(
                                            ", requestExists="
                                        )
                                        append(
                                            gap.requestExists
                                        )
                                        append(
                                            ", decisionExists="
                                        )
                                        append(
                                            gap.decisionExists
                                        )
                                        append(
                                            ", candidates="
                                        )
                                        append(
                                            gap.candidateCount
                                        )
                                        append(
                                            ", topCandidate="
                                        )
                                        append(
                                            gap.topCandidateKey
                                                ?: "-"
                                        )
                                    }
                            }
                )
            }

        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    private fun verifyResolvedNoRequestGap(
        catalogKey: String,
        gapsByCatalogKey:
        Map<
                String,
                de.shopme.tools.knowledge.rebuild.nutrition.coverage.NutritionCoverageGap
                >
    ) {
        val gap =
            gapsByCatalogKey[
                catalogKey
            ]

        /*
         * Der Key kann bereits durch ein persistiertes Mapping abgedeckt
         * sein. Dann erscheint er korrekterweise überhaupt nicht mehr in
         * der Liste der Coverage-Gaps.
         */
        if (gap == null) {
            return
        }

        check(
            gap.type !=
                    de.shopme.tools.knowledge.rebuild.nutrition.coverage
                        .NutritionCoverageGapType.NO_REQUEST
        ) {
            "$catalogKey must no longer be classified as NO_REQUEST."
        }

        check(gap.requestExists) {
            "$catalogKey must have a persisted nutrition match request."
        }

        check(gap.candidateCount > 0) {
            "$catalogKey must have at least one persisted nutrition " +
                    "candidate."
        }
    }
}