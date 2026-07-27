package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.retrieval.coverage

import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.CatalogOFFNutritionRetrievalRequestReader
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage.OFFNutritionCoverageAliasDatasetReader
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage.OFFNutritionRawSourceCoverageScanner
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage.OFFNutritionSourceCoverageDiagnoser
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.coverage.OFFNutritionSourceCoverageReportWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiagnoseMissingOFFNutritionSourceCoverageTest {

    @Test
    fun diagnoseMissingOFFNutritionSourceCoverage() {

        val projectRoot =
            resolveProjectRoot()

        val retrievalRequestFile =
            projectRoot.resolve(
                "data/generated/knowledge/off/nutrition/" +
                        "catalog-off-nutrition-retrieval-requests.json"
            )

        val rawOFFFile =
            projectRoot.resolve(
                "data/generated/openfoodfacts/" +
                        "openfoodfacts-products.slim.jsonl.gz"
            )
                .canonicalFile

        val referenceCandidateFile =
            resolveExistingFile(
                projectRoot = projectRoot,
                paths =
                    listOf(
                        "data/generated/knowledge/off/nutrition/" +
                                "nutrition-reference-candidates.json",
                        "data/generated/knowledge/off/nutrition/" +
                                "nutrition-reference-candidates.filtered.json",
                        "data/generated/knowledge/off/nutrition/" +
                                "nutrition-reference-candidates.deduplicated.json"
                    ),
                description =
                    "OFF nutrition reference candidate dataset"
            )

        val referenceAggregateFile =
            projectRoot.resolve(
                "data/generated/knowledge/off/nutrition/" +
                        "nutrition-reference-aggregates.validated.json"
            )

        val matcherCandidateFile =
            projectRoot.resolve(
                "data/generated/knowledge/off/nutrition/" +
                        "nutrition-reference-matcher-candidates.json"
            )

        val reportFile =
            projectRoot.resolve(
                "data/generated/knowledge/off/nutrition/" +
                        "catalog-off-nutrition-source-coverage-diagnostic.json"
            )

        val requests =
            CatalogOFFNutritionRetrievalRequestReader()
                .read(
                    inputFile =
                        retrievalRequestFile
                )

        val missingRequests =
            requests.filter { request ->
                request.candidates.isEmpty()
            }

        val rawSourceCoverage =
            OFFNutritionRawSourceCoverageScanner()
                .scan(
                    inputFile =
                        rawOFFFile,
                    missingRequests =
                        missingRequests
                )

        val aliasReader =
            OFFNutritionCoverageAliasDatasetReader()

        val referenceCandidates =
            aliasReader.read(
                inputFile =
                    referenceCandidateFile
            )

        val referenceAggregates =
            aliasReader.read(
                inputFile =
                    referenceAggregateFile
            )

        val matcherCandidates =
            aliasReader.read(
                inputFile =
                    matcherCandidateFile
            )

        val report =
            OFFNutritionSourceCoverageDiagnoser()
                .diagnose(
                    allRequests =
                        requests,
                    rawSourceCoverage =
                        rawSourceCoverage,
                    referenceCandidates =
                        referenceCandidates,
                    referenceAggregates =
                        referenceAggregates,
                    matcherCandidates =
                        matcherCandidates
                )

        OFFNutritionSourceCoverageReportWriter()
            .write(
                report =
                    report,
                outputFile =
                    reportFile
            )

        assertEquals(
            3_649,
            report.requestCount
        )

        assertEquals(
            missingRequests.size,
            report.missingRequestCount
        )

        assertEquals(
            report.missingRequestCount,
            report.countsByFirstMissingStage.values.sum()
        )

        assertTrue(
            report.rawOFFScannedProductCount > 0
        )

        assertTrue(
            reportFile.isFile
        )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("OFF NUTRITION SOURCE COVERAGE DIAGNOSTIC")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("Requests: ${report.requestCount}")
        println("Missing requests: ${report.missingRequestCount}")
        println(
            "Raw OFF products scanned: " +
                    report.rawOFFScannedProductCount
        )
        println(
            "Counts by first missing stage: " +
                    report.countsByFirstMissingStage
        )
        println("Report: ${reportFile.absolutePath}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }

    private fun resolveExistingFile(
        projectRoot: File,
        paths: List<String>,
        description: String
    ): File {

        return paths
            .asSequence()
            .map(projectRoot::resolve)
            .firstOrNull(File::isFile)
            ?: error(
                "$description not found. Checked:\n" +
                        paths.joinToString("\n") { path ->
                            "  - ${projectRoot.resolve(path).absolutePath}"
                        }
            )
    }

    private fun resolveProjectRoot(): File {

        val workingDirectory =
            File(
                System.getProperty("user.dir")
            )
                .absoluteFile

        return generateSequence(
            workingDirectory
        ) { directory ->
            directory.parentFile
        }
            .take(8)
            .firstOrNull { directory ->
                directory
                    .resolve("settings.gradle.kts")
                    .isFile ||
                        directory
                            .resolve("settings.gradle")
                            .isFile
            }
            ?: error(
                "Could not resolve project root."
            )
    }
}