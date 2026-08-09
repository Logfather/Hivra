package de.shopme.testing.system.tools.knowledge.report

import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import de.shopme.tools.report.CatalogKnowledgeKeyExtractor
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ReportUnmatchedCatalogKnowledgeKeysTest {

    @Test
    fun reportUnmatchedCatalogKnowledgeKeys() {

        val paths =
            KnowledgeBuildPaths.default()

        paths.ensureBuildDirectories()

        val catalogFile =
            paths.canonicalFoodCatalog

        val catalogKeys =
            CatalogKnowledgeKeyExtractor()
                .extract(
                    CatalogJsonFileReader()
                        .read(catalogFile)
                )

        assertTrue(
            catalogKeys.isNotEmpty(),
            "No runtime catalog knowledge keys found"
        )

        println(
            "Catalog knowledge keys = ${catalogKeys.size}"
        )

        val serverDirectory =
            paths.serverRoot

        require(serverDirectory.isDirectory) {
            "Server knowledge directory does not exist: " +
                    serverDirectory.absolutePath
        }

        val outputDirectory =
            paths.reportsRoot.resolve(
                "catalog-server-matches"
            )

        if (!outputDirectory.exists()) {
            check(
                outputDirectory.mkdirs()
            ) {
                "Could not create report directory: " +
                        outputDirectory.absolutePath
            }
        }

        require(outputDirectory.isDirectory) {
            "Report output path is not a directory: " +
                    outputDirectory.absolutePath
        }

        val nutritionQueryExpander =
            NutritionRetrievalQueryExpander()

        val writer =
            CatalogServerKnowledgeMatchReportWriter()

        serverDirectory
            .listFiles()
            .orEmpty()
            .asSequence()
            .filter(File::isFile)
            .filter {
                it.extension.equals(
                    other = "json",
                    ignoreCase = true
                )
            }
            .sortedBy {
                it.name
            }
            .forEach { artifactFile ->

                val reporter =
                    CatalogServerKnowledgeMatchReporter(
                        nearestCandidateLimit =
                            5,
                        queryExpander =
                            if (
                                artifactFile.name.equals(
                                    other =
                                        "nutrition.json",
                                    ignoreCase =
                                        true
                                )
                            ) {
                                nutritionQueryExpander
                            } else {
                                null
                            }
                    )

                val report =
                    reporter.report(
                        artifactFile =
                            artifactFile,
                        catalogKeys =
                            catalogKeys
                    )

                if (
                    artifactFile.name.equals(
                        "nutrition.json",
                        ignoreCase = true
                    )
                ) {
                    verifyCiqualCoverage(
                        report
                    )
                }

                val outputFile =
                    File(
                        outputDirectory,
                        "${artifactFile.nameWithoutExtension}.matches.json"
                    )

                writer.write(
                    report =
                        report,
                    outputFile =
                        outputFile
                )

                val unmatchedWithCandidates =
                    report.unmatched.count {
                        it.nearestCandidates.isNotEmpty()
                    }

                val unmatchedWithoutCandidates =
                    report.unmatched.count {
                        it.nearestCandidates.isEmpty()
                    }

                println()
                println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                println("CATALOG → SERVER MATCH REPORT")
                println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                println("Artifact                  : ${report.artifactName}")
                println("Catalog keys              : ${report.catalogKeyCount}")
                println("Server keys               : ${report.serverKeyCount}")
                println("Exact matches             : ${report.exactMatches.size}")
                println("Unmatched                 : ${report.unmatched.size}")
                println("Unmatched with candidates : $unmatchedWithCandidates")
                println("No candidates             : $unmatchedWithoutCandidates")
                println("Report                    : ${outputFile.path}")
                println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            }
    }

    private fun verifyCiqualCoverage(
        report: CatalogServerKnowledgeMatchReport
    ) {

        fun find(
            key: String
        ) =
            report.unmatched.firstOrNull {
                it.catalogKey == key
            }

        val chervil =
            find("chervil")

        if (chervil != null) {
            assertTrue(
                chervil.nearestCandidates.isNotEmpty(),
                "CIQUAL should provide nutrition candidates for chervil."
            )
        } else {
            assertTrue(
                report.exactMatches.contains("chervil"),
                "Chervil must either match exactly or provide retrieval candidates."
            )
        }

        val salsify =
            find("salsify")

        if (salsify != null) {
            assertTrue(
                salsify.nearestCandidates.isNotEmpty(),
                "CIQUAL should provide nutrition candidates for salsify."
            )
        } else {
            assertTrue(
                report.exactMatches.contains("salsify"),
                "Salsify must either match exactly or provide retrieval candidates."
            )
        }

        val mace =
            find("mace")

        if (mace != null) {
            assertTrue(
                mace.nearestCandidates.isEmpty(),
                "CIQUAL currently contains no nutrition mapping for mace."
            )
        }
    }
}