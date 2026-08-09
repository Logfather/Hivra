package de.shopme.testing.system.tools.knowledge.catalog.runner

import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.catalog.semantic.plausibility.value.FamilyVariantValuePlausibilityAnalyzer
import de.shopme.testing.system.tools.knowledge.catalog.semantic.plausibility.value.FamilyVariantValuePlausibilityAuditWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunFamilyVariantValuePlausibilityAuditTest {

    @Test
    fun auditFamilyVariantValuePlausibility() {

        val projectRoot =
            resolveProjectRoot()

        val inputFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/final/" +
                        "canonical-food-catalog-final-v1-" +
                        "f6bbdd7fbd37f3cb.json"
            )

        require(
            inputFile.isFile
        ) {
            "Frozen v1 canonical catalog not found: " +
                    inputFile.absolutePath
        }

        val catalog =
            JsonParser
                .parseString(
                    inputFile.readText()
                )
                .asJsonArray
                .mapNotNull { element ->
                    element
                        .takeIf {
                            it.isJsonObject
                        }
                        ?.asJsonObject
                }

        assertEquals(
            expected = 10354,
            actual = catalog.size
        )

        val report =
            FamilyVariantValuePlausibilityAnalyzer()
                .analyze(
                    catalog
                )

        assertTrue(
            report.variantOccurrenceCount > 0
        )

        assertTrue(
            report.rejectOccurrenceCount > 0,
            "Expected invalid concrete Family×Variant values."
        )

        val outputFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/semantic-audit/" +
                        "canonical-food-catalog-" +
                        "family-variant-value-plausibility.json"
            )

        FamilyVariantValuePlausibilityAuditWriter()
            .write(
                report = report,
                outputFile = outputFile
            )

        println()
        println("Family-specific Variant Value Plausibility Audit")
        println("-----------------------------------------------")
        println(
            "Catalog entries             : " +
                    report.inputEntryCount
        )
        println(
            "Variant-bearing entries     : " +
                    report.variantBearingEntryCount
        )
        println(
            "Variant occurrences         : " +
                    report.variantOccurrenceCount
        )

        println()
        println("Decisions:")
        println(
            "  ALLOW                     : " +
                    report.allowOccurrenceCount
        )
        println(
            "  REJECT                    : " +
                    report.rejectOccurrenceCount
        )
        println(
            "  REVIEW                    : " +
                    report.reviewOccurrenceCount
        )

        println()
        println(
            "Reject-affected entries     : " +
                    report.affectedRejectEntryCount
        )
        println(
            "Review-affected entries     : " +
                    report.affectedReviewEntryCount
        )

        println()
        println("By reason:")

        report
            .countsByReason
            .forEach { (reason, count) ->
                println(
                    "  ${reason.padEnd(36)} $count"
                )
            }

        println()
        println("Top rejected values:")

        report
            .rejectedValues
            .entries
            .take(30)
            .forEach { (value, count) ->
                println(
                    "  ${value.padEnd(32)} $count"
                )
            }

        println()
        println("Top unresolved Family×Value gaps:")

        report
            .reviewGaps
            .take(40)
            .forEach { gap ->

                println(
                    "  ${gap.family.padEnd(30)} " +
                            "${gap.variantCanonicalKey.padEnd(24)} " +
                            gap.occurrenceCount
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
                        .relativeTo(
                            projectRoot
                        )
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
                "Could not resolve ShopMe project root."
            )
    }
}