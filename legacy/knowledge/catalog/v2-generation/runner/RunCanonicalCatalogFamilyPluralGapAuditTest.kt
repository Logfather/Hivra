package de.shopme.testing.system.tools.knowledge.catalog.runner

import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.catalog.linguistic.repair.CanonicalCatalogFamilyPluralGapAnalyzer
import de.shopme.testing.system.tools.knowledge.catalog.linguistic.repair.CanonicalCatalogFamilyPluralGapAuditWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class RunCanonicalCatalogFamilyPluralGapAuditTest {

    @Test
    fun auditCanonicalCatalogFamilyPluralGaps() {

        val projectRoot =
            resolveProjectRoot()

        val inputFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/regeneration/" +
                        "canonical-food-catalog-family-identity-resolved.json"
            )

        require(inputFile.isFile) {
            "Family repaired catalog not found: " +
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
            expected = 4596,
            actual = catalog.size,
            message =
                "Post-family resolved catalog arithmetic changed unexpectedly."
        )

        val report =
            CanonicalCatalogFamilyPluralGapAnalyzer()
                .analyze(
                    catalog
                )

        assertEquals(
            expected =
                report.variantEntryCount,
            actual =
                report.familyPluralResolvedCount +
                        report.familyPluralFallbackCount,
            message =
                "Every variant entry must be classified as " +
                        "resolved or fallback."
        )

        val outputFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/semantic-audit/" +
                        "canonical-food-catalog-family-plural-gaps.json"
            )

        CanonicalCatalogFamilyPluralGapAuditWriter()
            .write(
                report = report,
                outputFile = outputFile
            )

        println()
        println("Canonical Catalog Family Plural Gap Audit")
        println("-----------------------------------------")
        println(
            "Catalog entries              : " +
                    report.inputEntryCount
        )
        println(
            "Variant entries              : " +
                    report.variantEntryCount
        )
        println(
            "Family plural resolved       : " +
                    report.familyPluralResolvedCount
        )
        println(
            "Family plural fallback       : " +
                    report.familyPluralFallbackCount
        )
        println(
            "Distinct fallback families   : " +
                    report.distinctFallbackFamilyCount
        )

        println()
        println("Top family plural gaps:")

        report
            .gaps
            .take(50)
            .forEach { gap ->

                println(
                    "  ${gap.family.padEnd(32)} " +
                            gap.occurrenceCount
                )

                gap
                    .exampleItems
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
                    "data/generated/knowledge/catalog/regeneration"
                ).isDirectory
            }
            ?: error(
                "Could not resolve ShopMe project root."
            )
    }
}