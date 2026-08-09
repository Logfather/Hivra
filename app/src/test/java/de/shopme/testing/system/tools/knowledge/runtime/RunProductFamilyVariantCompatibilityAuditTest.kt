package de.shopme.testing.system.tools.knowledge.catalog.runner

import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility.ProductFamilyVariantCompatibilityAnalyzer
import de.shopme.testing.system.tools.knowledge.catalog.semantic.compatibility.ProductFamilyVariantCompatibilityAuditReportWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunProductFamilyVariantCompatibilityAuditTest {

    @Test
    fun auditProductFamilyVariantCompatibility() {

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
            ProductFamilyVariantCompatibilityAnalyzer()
                .analyze(
                    catalog = catalog,
                    inputFile =
                        inputFile
                            .relativeTo(projectRoot)
                            .path
                )

        /*
         * Die Variant Taxonomy ist bereits vollständig.
         * Daher müssen exakt dieselben 7.768 Variant-
         * Occurrences durch den Compatibility Audit laufen.
         */
        assertEquals(
            expected = 7768,
            actual = report.variantOccurrenceCount,
            message =
                "Compatibility audit must evaluate every semantic " +
                        "variant occurrence from the frozen v1 catalog."
        )

        assertEquals(
            expected = 6935,
            actual = report.variantBearingEntryCount
        )

        assertTrue(
            report.rejectOccurrenceCount > 0
        )

        assertTrue(
            report.reviewOccurrenceCount > 0
        )

        val outputFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/" +
                        "semantic-audit/" +
                        "canonical-food-catalog-" +
                        "family-variant-compatibility.json"
            )

        ProductFamilyVariantCompatibilityAuditReportWriter()
            .write(
                report = report,
                outputFile = outputFile
            )

        println()
        println("Product Family Variant Compatibility Audit")
        println("------------------------------------------")
        println(
            "Catalog entries              : " +
                    report.catalogEntryCount
        )
        println(
            "Variant-bearing entries      : " +
                    report.variantBearingEntryCount
        )
        println(
            "Variant occurrences          : " +
                    report.variantOccurrenceCount
        )

        println()
        println("Occurrences:")
        println(
            "  ALLOW                      : " +
                    report.allowOccurrenceCount
        )
        println(
            "  REJECT                     : " +
                    report.rejectOccurrenceCount
        )
        println(
            "  REVIEW                     : " +
                    report.reviewOccurrenceCount
        )

        println()
        println("Affected catalog entries:")
        println(
            "  Entries with ALLOW         : " +
                    report.allowedEntryCount
        )
        println(
            "  Entries with REJECT        : " +
                    report.rejectedEntryCount
        )
        println(
            "  Entries with REVIEW        : " +
                    report.reviewEntryCount
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
        println("Top rejected families:")

        report
            .rejectedCountsByFamily
            .entries
            .take(25)
            .forEach { (family, count) ->
                println(
                    "  ${family.padEnd(30)} $count"
                )
            }

        println()
        println("Top rejected variants:")

        report
            .rejectedCountsByVariant
            .entries
            .take(25)
            .forEach { (variant, count) ->
                println(
                    "  ${variant.padEnd(30)} $count"
                )
            }

        println()
        println("Top compatibility gaps:")

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