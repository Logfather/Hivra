package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.retrieval.validation

import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.CatalogOFFNutritionRetrievalRequestReader
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.validation.OFFNutritionRetrievalQualityReportWriter
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.validation.OFFNutritionRetrievalQualityType
import de.shopme.tools.knowledge.off.nutrition.reference.retrieval.validation.OFFNutritionRetrievalQualityValidator
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidateRetrievedOFFNutritionCandidateQualityTest {

    @Test
    fun validateRetrievedOFFNutritionCandidateQuality() {

        val projectRoot =
            resolveProjectRoot()

        val requestFile =
            projectRoot.resolve(
                "data/generated/knowledge/off/nutrition/" +
                        "catalog-off-nutrition-retrieval-requests.json"
            )

        val reportFile =
            projectRoot.resolve(
                "data/generated/knowledge/off/nutrition/" +
                        "catalog-off-nutrition-retrieval-quality-report.json"
            )

        val requests =
            CatalogOFFNutritionRetrievalRequestReader()
                .read(
                    inputFile = requestFile
                )

        val report =
            OFFNutritionRetrievalQualityValidator()
                .validate(
                    requests = requests
                )

        OFFNutritionRetrievalQualityReportWriter()
            .write(
                report = report,
                outputFile = reportFile
            )

        assertEquals(
            3649,
            report.requestCount
        )

        assertEquals(
            77,
            report.requestWithoutCandidatesCount
        )

        assertEquals(
            983,
            report.exactAliasMatchCount
        )

        assertTrue(
            report.riskyFindingCount > 0
        )

        assertTrue(
            report.countsByPrimaryType.containsKey(
                OFFNutritionRetrievalQualityType.NO_CANDIDATES
            )
        )

        assertTrue(
            report.countsByQualityType.containsKey(
                OFFNutritionRetrievalQualityType.PROCESSING_FORM_MISMATCH
            )
        )

        println(
            """
            OFF nutrition retrieval quality validation completed.

            Requests: ${report.requestCount}
            With candidates: ${report.requestWithCandidatesCount}
            Without candidates: ${report.requestWithoutCandidatesCount}
            Exact alias matches: ${report.exactAliasMatchCount}
            Strong lexical matches: ${report.strongLexicalMatchCount}
            Risky findings: ${report.riskyFindingCount}
            Counts by primary type: ${report.countsByPrimaryType}
            Counts by quality type: ${report.countsByQualityType}
            Report: ${reportFile.absolutePath}
            """.trimIndent()
        )
    }

    private fun resolveProjectRoot(): File {

        val workingDirectory =
            File(System.getProperty("user.dir"))
                .absoluteFile

        val candidates =
            generateSequence(workingDirectory) { directory ->
                directory.parentFile
            }
                .take(8)
                .toList()

        return candidates
            .firstOrNull { directory ->
                directory.resolve("settings.gradle.kts").isFile ||
                        directory.resolve("settings.gradle").isFile
            }
            ?: error(
                "Could not resolve project root from " +
                        workingDirectory.absolutePath
            )
    }
}