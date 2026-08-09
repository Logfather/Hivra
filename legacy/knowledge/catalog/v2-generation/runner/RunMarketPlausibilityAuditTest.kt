package de.shopme.testing.system.tools.knowledge.catalog.runner

import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.catalog.semantic.plausibility.MarketPlausibilityAnalyzer
import de.shopme.testing.system.tools.knowledge.catalog.semantic.plausibility.MarketPlausibilityAuditReportWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunMarketPlausibilityAuditTest {

    @Test
    fun auditMarketPlausibility() {

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
                .parseString(
                    inputFile.readText()
                )
                .asJsonArray

        assertEquals(
            expected = 10354,
            actual = catalog.size()
        )

        val report =
            MarketPlausibilityAnalyzer()
                .analyze(
                    catalog = catalog,
                    inputFile =
                        inputFile
                            .relativeTo(projectRoot)
                            .path
                )

        assertEquals(
            expected = 10354,
            actual =
                report.acceptedEntryCount +
                        report.rejectedEntryCount +
                        report.reviewEntryCount,
            message =
                "Every catalog entry must receive exactly one " +
                        "market plausibility decision."
        )

        assertTrue(
            report.rejectedEntryCount > 0
        )

        assertTrue(
            report.acceptedEntryCount > 0
        )

        val outputFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/" +
                        "semantic-audit/" +
                        "canonical-food-catalog-" +
                        "market-plausibility.json"
            )

        MarketPlausibilityAuditReportWriter()
            .write(
                report = report,
                outputFile = outputFile
            )

        println()
        println("Market Plausibility Audit")
        println("-------------------------")
        println(
            "Catalog entries : " +
                    report.catalogEntryCount
        )

        println()
        println("Decisions:")
        println(
            "  ACCEPT        : " +
                    report.acceptedEntryCount
        )
        println(
            "  REJECT        : " +
                    report.rejectedEntryCount
        )
        println(
            "  REVIEW        : " +
                    report.reviewEntryCount
        )

        println()
        println("By reason:")

        report
            .countsByReason
            .forEach { (reason, count) ->
                println(
                    "  ${reason.padEnd(38)} $count"
                )
            }

        println()
        println("Rejected by category:")

        report
            .rejectedCountsByCategory
            .entries
            .sortedByDescending {
                it.value
            }
            .take(25)
            .forEach { (category, count) ->
                println(
                    "  ${category.padEnd(30)} $count"
                )
            }

        println()
        println("Top market review gaps:")

        report
            .reviewGaps
            .take(30)
            .forEach { gap ->

                println(
                    "  ${gap.family.padEnd(30)} " +
                            gap.occurrenceCount +
                            "  [" +
                            gap.variantKeys.joinToString(", ") +
                            "]"
                )

                gap.exampleItems
                    .take(3)
                    .forEach { example ->
                        println(
                            "      $example"
                        )
                    }
            }

        println()
        println(
            "Report: " +
                    outputFile
                        .relativeTo(projectRoot)
                        .path
        )
    }

    private fun resolveProjectRoot(): File {

        val current =
            File(".")
                .canonicalFile

        return listOfNotNull(
            current,
            current.parentFile
        )
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