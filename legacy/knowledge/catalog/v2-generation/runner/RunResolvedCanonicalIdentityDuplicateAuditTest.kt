package de.shopme.testing.system.tools.knowledge.catalog.runner

import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.audit.CanonicalIdentityDuplicateAnalyzer
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.audit.CanonicalIdentityDuplicateAuditReportWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class RunResolvedCanonicalIdentityDuplicateAuditTest {

    @Test
    fun auditResolvedCanonicalIdentities() {

        val projectRoot =
            resolveProjectRoot()

        val resolvedCatalogFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/regeneration/" +
                        "canonical-food-catalog-identity-resolved.json"
            )

        require(resolvedCatalogFile.isFile) {
            "Resolved canonical catalog not found: " +
                    resolvedCatalogFile.absolutePath
        }

        val resolvedEntries =
            JsonParser
                .parseString(
                    resolvedCatalogFile.readText()
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
            actual = resolvedEntries.size,
            message =
                "Resolved canonical catalog arithmetic changed unexpectedly."
        )

        /*
         * Für diesen zweiten Audit ist die Source-Layer-
         * Unterscheidung nicht mehr entscheidend.
         *
         * Wir behandeln den bereits resolvierten Bestand als
         * kanonische Ausgangsmenge.
         */
        val report =
            CanonicalIdentityDuplicateAnalyzer()
                .analyze(
                    baseline = resolvedEntries,
                    regeneratedExpansion = emptyList()
                )

        assertEquals(
            expected = 0,
            actual = report.errorGroupCount,
            message =
                "Resolved canonical catalog must contain no hard " +
                        "canonical identity conflicts."
        )

        assertEquals(
            expected = 0,
            actual = report.reviewGroupCount,
            message =
                "Resolved canonical catalog must contain no unresolved " +
                        "duplicate identity candidates."
        )

        assertEquals(
            expected = 0,
            actual = report.duplicateGroupCount,
            message =
                "Resolved canonical catalog must be duplicate-free."
        )

        val outputFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/semantic-audit/" +
                        "canonical-food-catalog-" +
                        "duplicate-identity-after-resolution.json"
            )

        CanonicalIdentityDuplicateAuditReportWriter()
            .write(
                report = report,
                outputFile = outputFile
            )

        println()
        println("Resolved Canonical Identity Duplicate Audit")
        println("-------------------------------------------")
        println(
            "Resolved catalog entries : " +
                    report.projectedCatalogEntryCount
        )
        println(
            "ERROR groups             : " +
                    report.errorGroupCount
        )
        println(
            "REVIEW groups            : " +
                    report.reviewGroupCount
        )
        println(
            "Total groups             : " +
                    report.duplicateGroupCount
        )

        println()
        println("Remaining groups:")

        report
            .groups
            .forEach { group ->

                println(
                    "  ${group.reason.name} " +
                            "[${group.fingerprint}]"
                )

                group.entries.forEach { entry ->
                    println(
                        "      ${entry.normalized.padEnd(35)} " +
                                entry.itemName
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
                    "data/generated/knowledge/catalog"
                ).isDirectory
            }
            ?: error(
                "Could not resolve ShopMe project root."
            )
    }
}