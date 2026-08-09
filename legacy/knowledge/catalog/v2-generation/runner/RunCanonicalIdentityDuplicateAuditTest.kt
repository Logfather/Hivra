package de.shopme.testing.system.tools.knowledge.catalog.runner

import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.audit.CanonicalIdentityDuplicateAnalyzer
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.audit.CanonicalIdentityDuplicateAuditReportWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class RunCanonicalIdentityDuplicateAuditTest {

    @Test
    fun auditProjectedCanonicalIdentities() {

        val projectRoot =
            resolveProjectRoot()

        val frozenV1File =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/final/" +
                        "canonical-food-catalog-final-v1-" +
                        "f6bbdd7fbd37f3cb.json"
            )

        val regeneratedExpansionFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/regeneration/" +
                        "canonical-food-catalog-semantic-" +
                        "expansion-regenerated.json"
            )

        require(frozenV1File.isFile) {
            "Frozen v1 catalog not found: " +
                    frozenV1File.absolutePath
        }

        require(regeneratedExpansionFile.isFile) {
            "Regenerated semantic expansion not found: " +
                    regeneratedExpansionFile.absolutePath
        }

        val frozenCatalog =
            JsonParser
                .parseString(
                    frozenV1File.readText()
                )
                .asJsonArray

        val baseline =
            frozenCatalog
                .mapNotNull { element ->
                    element
                        .takeIf {
                            it.isJsonObject
                        }
                        ?.asJsonObject
                }
                .filter { entry ->
                    !entry
                        .get("itemname")
                        .asString
                        .contains(
                            VARIANT_SEPARATOR
                        )
                }

        val regeneratedExpansion =
            JsonParser
                .parseString(
                    regeneratedExpansionFile.readText()
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
            expected = 3419,
            actual = baseline.size,
            message =
                "Frozen v1 baseline arithmetic changed unexpectedly."
        )

        assertEquals(
            expected = 1295,
            actual = regeneratedExpansion.size,
            message =
                "Regenerated semantic expansion arithmetic changed " +
                        "unexpectedly."
        )

        val report =
            CanonicalIdentityDuplicateAnalyzer()
                .analyze(
                    baseline = baseline,
                    regeneratedExpansion =
                        regeneratedExpansion
                )

        assertEquals(
            expected = 4714,
            actual =
                report.projectedCatalogEntryCount,
            message =
                "Canonical identity audit must cover the complete " +
                        "projected v2 catalog."
        )

        val outputFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/" +
                        "semantic-audit/" +
                        "canonical-food-catalog-" +
                        "duplicate-identity-audit.json"
            )

        CanonicalIdentityDuplicateAuditReportWriter()
            .write(
                report = report,
                outputFile = outputFile
            )

        println()
        println("Canonical Identity Duplicate Audit")
        println("----------------------------------")
        println(
            "Baseline entries                : " +
                    report.baselineEntryCount
        )
        println(
            "Regenerated expansion entries   : " +
                    report.regeneratedExpansionEntryCount
        )
        println(
            "Projected catalog entries       : " +
                    report.projectedCatalogEntryCount
        )

        println()
        println("Duplicate groups:")
        println(
            "  ERROR groups                  : " +
                    report.errorGroupCount
        )
        println(
            "  REVIEW groups                 : " +
                    report.reviewGroupCount
        )
        println(
            "  Total groups                  : " +
                    report.duplicateGroupCount
        )

        println()
        println("Affected entries:")
        println(
            "  ERROR affected               : " +
                    report.errorAffectedEntryCount
        )
        println(
            "  REVIEW affected              : " +
                    report.reviewAffectedEntryCount
        )
        println(
            "  Total affected               : " +
                    report.affectedEntryCount
        )

        println()
        println("By reason:")

        report
            .countsByReason
            .forEach { (reason, count) ->
                println(
                    "  ${reason.padEnd(34)} $count"
                )
            }

        println()
        println("ERROR groups:")

        report
            .groups
            .filter {
                it.severity.name ==
                        "ERROR"
            }
            .take(30)
            .forEach { group ->

                println(
                    "  ${group.reason.name} " +
                            "[${group.fingerprint}]"
                )

                group.entries.forEach { entry ->
                    println(
                        "      ${entry.sourceLayer.name.padEnd(23)} " +
                                "${entry.normalized.padEnd(35)} " +
                                entry.itemName
                    )
                }
            }

        println()
        println("Top REVIEW groups:")

        report
            .groups
            .filter {
                it.severity.name ==
                        "REVIEW"
            }
            .take(30)
            .forEach { group ->

                println(
                    "  ${group.reason.name} " +
                            "[${group.fingerprint}]"
                )

                group.entries.forEach { entry ->
                    println(
                        "      ${entry.sourceLayer.name.padEnd(23)} " +
                                "${entry.normalized.padEnd(35)} " +
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
                    "data/generated/knowledge/catalog/final"
                ).isDirectory
            }
            ?: error(
                "Could not resolve ShopMe project root from: " +
                        current.absolutePath
            )
    }

    private companion object {

        const val VARIANT_SEPARATOR =
            " – "
    }
}