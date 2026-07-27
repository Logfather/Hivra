package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.retrieval.missing

import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.CatalogOFFNutritionRetrievalRequestReader
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.missing.MissingOFFNutritionRetrievalCandidateClassifier
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.missing.MissingOFFNutritionRetrievalCandidateReportWriter
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.missing.OFFNutritionSourceAliasReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClassifyMissingOFFNutritionRetrievalCandidatesTest {

    @Test
    fun classifyMissingOFFNutritionRetrievalCandidates() {

        val projectRoot =
            resolveProjectRoot()

        val retrievalRequestFile =
            projectRoot.resolve(
                "data/generated/knowledge/off/nutrition/" +
                        "catalog-off-nutrition-retrieval-requests.json"
            )

        /*
         * Verwende hier dieselbe OFF-Referenzdatei, die bereits
         * RetrieveOFFNutritionCandidatesForCatalogItemsTest als
         * Kandidatenquelle verwendet.
         */
        val sourceReferenceFile =
            resolveSourceReferenceFile(
                projectRoot = projectRoot
            )

        val reportFile =
            projectRoot.resolve(
                "data/generated/knowledge/off/nutrition/" +
                        "catalog-off-nutrition-missing-candidate-report.json"
            )

        val requests =
            CatalogOFFNutritionRetrievalRequestReader()
                .read(
                    inputFile =
                        retrievalRequestFile
                )

        val sourceAliases =
            OFFNutritionSourceAliasReader()
                .read(
                    inputFile =
                        sourceReferenceFile
                )

        val report =
            MissingOFFNutritionRetrievalCandidateClassifier()
                .classify(
                    requests =
                        requests,
                    sourceAliases =
                        sourceAliases
                )

        MissingOFFNutritionRetrievalCandidateReportWriter()
            .write(
                report =
                    report,
                outputFile =
                    reportFile
            )

        assertEquals(
            requests.count { request ->
                request.candidates.isEmpty()
            },
            report.missingRequestCount
        )

        assertEquals(
            report.missingRequestCount,
            report.countsByType.values.sum()
        )

        assertTrue(
            reportFile.isFile
        )

        println(
            """
            Missing OFF nutrition retrieval candidates classified.

            Requests: ${report.requestCount}
            Missing requests: ${report.missingRequestCount}
            Counts by type: ${report.countsByType}
            Source aliases: ${sourceAliases.size}
            Report: ${reportFile.absolutePath}
            """.trimIndent()
        )
    }

    private fun resolveSourceReferenceFile(
        projectRoot: File
    ): File {

        val candidates =
            listOf(
                projectRoot.resolve(
                    "data/generated/knowledge/off/nutrition/" +
                            "off-nutrition-reference-aggregates.json"
                ),
                projectRoot.resolve(
                    "data/generated/knowledge/off/nutrition/" +
                            "off-nutrition-reference-aggregate.json"
                ),
                projectRoot.resolve(
                    "data/generated/knowledge/off/nutrition/" +
                            "off-nutrition-reference-candidates.json"
                ),
                projectRoot.resolve(
                    "data/generated/knowledge/off/nutrition/" +
                            "nutrition-reference-matcher-candidates.json"
                )
            )

        return candidates
            .firstOrNull(File::isFile)
            ?: error(
                "Could not locate OFF nutrition source reference file. " +
                        "Use the exact source file already configured in " +
                        "RetrieveOFFNutritionCandidatesForCatalogItemsTest."
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