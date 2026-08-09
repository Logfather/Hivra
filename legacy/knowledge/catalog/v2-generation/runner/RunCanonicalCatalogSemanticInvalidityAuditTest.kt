package de.shopme.testing.system.tools.knowledge.catalog.runner

import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.catalog.semantic.audit.CanonicalCatalogSemanticInvalidityAnalyzer
import de.shopme.testing.system.tools.knowledge.catalog.semantic.audit.CanonicalCatalogSemanticInvalidityReportWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalCatalogSemanticInvalidityAuditTest {

    @Test
    fun auditFinalCatalogForSemanticInvalidity() {

        val projectRoot =
            resolveProjectRoot()

        val inputFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/final/" +
                        "canonical-food-catalog-final-v1-" +
                        "f6bbdd7fbd37f3cb.json"
            )

        require(inputFile.isFile) {
            "Final canonical catalog release not found: " +
                    inputFile.absolutePath
        }

        val catalog =
            JsonParser
                .parseString(inputFile.readText())
                .asJsonArray

        assertEquals(
            expected = 10354,
            actual = catalog.size(),
            message =
                "Semantic invalidity audit must run against the " +
                        "frozen 10,354-entry v1 release."
        )

        val report =
            CanonicalCatalogSemanticInvalidityAnalyzer()
                .analyze(
                    catalog = catalog,
                    inputFile =
                        inputFile.relativeTo(projectRoot).path
                )

        val outputFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/semantic-audit/" +
                        "canonical-food-catalog-semantic-invalidity.json"
            )

        CanonicalCatalogSemanticInvalidityReportWriter()
            .write(
                report = report,
                outputFile = outputFile
            )

        assertTrue(
            actual = report.issueCount > 0,
            message =
                "Expected semantic invalidities in current v1 release."
        )

        println("Catalog entries             : ${report.catalogEntryCount}")
        println()
        println("Product identity:")
        println(
            "  Semantic ERROR entries     : " +
                    report.semanticInvalidEntryCount
        )
        println(
            "  Semantic REVIEW entries    : " +
                    report.semanticReviewEntryCount
        )
        println(
            "  Semantic issues            : " +
                    report.semanticIssueCount
        )
        println()
        println("Metadata:")
        println(
            "  Metadata ERROR entries     : " +
                    report.metadataInvalidEntryCount
        )
        println(
            "  Metadata REVIEW entries    : " +
                    report.metadataReviewEntryCount
        )
        println(
            "  Metadata issues            : " +
                    report.metadataIssueCount
        )
        println()
        println("Total issues                : ${report.issueCount}")

        report
            .countsByReason
            .forEach { (reason, count) ->
                println(
                    "  ${reason.padEnd(38)} $count"
                )
            }

        println()
        println(
            "Report: ${outputFile.relativeTo(projectRoot).path}"
        )
    }

    private fun resolveProjectRoot(): File {

        val current =
            File(".")
                .canonicalFile

        val candidates =
            listOfNotNull(
                current,
                current.parentFile
            )

        return candidates
            .firstOrNull { candidate ->
                File(
                    candidate,
                    "data/generated/knowledge/catalog/final"
                ).isDirectory
            }
            ?: error(
                "Could not resolve ShopMe project root from: " +
                        current.absolutePath
            )
    }
}