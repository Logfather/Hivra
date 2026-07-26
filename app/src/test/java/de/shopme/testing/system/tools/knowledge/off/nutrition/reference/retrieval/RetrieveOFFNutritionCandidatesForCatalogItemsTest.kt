package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.retrieval

import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.CatalogOFFNutritionCandidateRetriever
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.CatalogOFFNutritionRetrievalDatasetWriter
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.CatalogOFFNutritionRetrievalItemReader
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.CatalogOFFNutritionRetrievalReportWriter
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.OFFNutritionMatcherTrainingCandidateReader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RetrieveOFFNutritionCandidatesForCatalogItemsTest {

    @Test
    fun retrieveOFFNutritionCandidatesForCatalogItems() {

        val projectRoot =
            File("..")
                .canonicalFile

        val catalogFile =
            resolveCatalogFile(
                projectRoot
            )

        val sourceCandidateFile =
            projectRoot.resolve(
                "data/generated/knowledge/off/nutrition/" +
                        "nutrition-reference-matcher-candidates.json"
            )

        val catalogItems =
            CatalogOFFNutritionRetrievalItemReader()
                .read(
                    catalogFile
                )

        val sourceCandidates =
            OFFNutritionMatcherTrainingCandidateReader()
                .read(
                    sourceCandidateFile
                )

        val result =
            CatalogOFFNutritionCandidateRetriever(
                maximumCandidatesPerRequest =
                    10,
                minimumScore =
                    0.18
            )
                .retrieve(
                    catalogItems =
                        catalogItems,
                    sourceCandidates =
                        sourceCandidates
                )

        val outputDirectory =
            projectRoot.resolve(
                "data/generated/knowledge/off/nutrition"
            )

        val datasetFile =
            outputDirectory.resolve(
                "catalog-off-nutrition-retrieval-requests.json"
            )

        val reportFile =
            outputDirectory.resolve(
                "catalog-off-nutrition-retrieval-report.json"
            )

        CatalogOFFNutritionRetrievalDatasetWriter()
            .write(
                requests =
                    result.requests,
                outputFile =
                    datasetFile
            )

        CatalogOFFNutritionRetrievalReportWriter()
            .write(
                result =
                    result,
                outputFile =
                    reportFile
            )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("CATALOG → OFF NUTRITION CANDIDATE RETRIEVAL")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("catalogItems=${result.catalogItemCount}")
        println("sourceCandidates=${result.sourceCandidateCount}")
        println("requests=${result.requestCount}")
        println(
            "requestsWithCandidates=" +
                    result.requestWithCandidatesCount
        )
        println(
            "requestsWithoutCandidates=" +
                    result.requestWithoutCandidatesCount
        )
        println(
            "exactTopCandidates=" +
                    result.exactTopCandidateCount
        )
        println(
            "totalRetrievedCandidates=" +
                    result.totalRetrievedCandidateCount
        )
        println(
            "maximumCandidateCount=" +
                    result.maximumCandidateCount
        )
        println("datasetFile=${datasetFile.absolutePath}")
        println("reportFile=${reportFile.absolutePath}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        assertEquals(
            catalogItems.size,
            result.requestCount
        )

        assertEquals(
            sourceCandidates.size,
            result.sourceCandidateCount
        )

        assertTrue(
            result.requestWithCandidatesCount > 0
        )

        assertTrue(
            result.maximumCandidateCount <= 10
        )

        assertTrue(datasetFile.isFile)
        assertTrue(reportFile.isFile)
    }

    private fun resolveCatalogFile(
        projectRoot: File
    ): File {

        val catalogFile =
            projectRoot.resolve(
                "app/src/main/assets/catalog/catalog.json"
            )

        require(catalogFile.isFile) {
            "Catalog file not found: ${catalogFile.absolutePath}"
        }

        return catalogFile
    }
}