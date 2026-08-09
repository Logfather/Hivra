package de.shopme.testing.system.tools.knowledge.catalog.runner

import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantTaxonomyAnalyzer
import de.shopme.testing.system.tools.knowledge.catalog.semantic.variant.SemanticVariantTaxonomyAuditReportWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunSemanticVariantTaxonomyAuditTest {

    @Test
    fun auditSemanticVariantTaxonomyCoverage() {

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
            SemanticVariantTaxonomyAnalyzer()
                .analyze(
                    catalog = catalog,
                    inputFile =
                        inputFile
                            .relativeTo(projectRoot)
                            .path
                )

        val outputFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/" +
                        "semantic-audit/" +
                        "canonical-food-catalog-" +
                        "semantic-variant-taxonomy.json"
            )

        SemanticVariantTaxonomyAuditReportWriter()
            .write(
                report = report,
                outputFile = outputFile
            )

        assertTrue(
            report.variantBearingEntryCount > 0
        )

        assertTrue(
            report.distinctVariantCount > 0
        )

        assertTrue(
            report.classifiedDistinctVariantCount > 0
        )

        assertEquals(
            expected = 0,
            actual = report.unknownDistinctVariantCount,
            message =
                "Every materialized semantic variant in the frozen " +
                        "v1 catalog must have an explicit taxonomy definition."
        )

        assertEquals(
            expected = 1.0,
            actual = report.classificationCoverage,
            message =
                "Semantic variant taxonomy must provide complete " +
                        "coverage for the frozen v1 catalog."
        )

        println()
        println("Semantic Variant Taxonomy Audit")
        println("--------------------------------")
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
        println(
            "Distinct variants            : " +
                    report.distinctVariantCount
        )
        println(
            "Classified distinct variants : " +
                    report.classifiedDistinctVariantCount
        )
        println(
            "Unknown distinct variants    : " +
                    report.unknownDistinctVariantCount
        )
        println(
            "Classification coverage      : " +
                    "%.2f%%".format(
                        report.classificationCoverage * 100.0
                    )
        )

        println()
        println("Occurrences by semantic type:")

        report
            .countsByType
            .forEach { (type, count) ->
                println(
                    "  ${type.padEnd(24)} $count"
                )
            }

        println()
        println("Top unknown variants:")

        report
            .unknownVariants
            .take(30)
            .forEach { unknown ->
                println(
                    "  ${unknown.rawValue.padEnd(30)} " +
                            unknown.occurrenceCount
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