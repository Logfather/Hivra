package de.shopme.testing.system.tools.knowledge.catalog.runner

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.audit.CanonicalIdentityDuplicateAnalyzer
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.resolution.CanonicalIdentityResolutionApplier
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.resolution.CanonicalIdentityResolutionDecision
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.resolution.CanonicalIdentityResolutionReport
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.resolution.CanonicalIdentityResolver
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunCanonicalIdentityResolutionTest {

    @Test
    fun resolveCanonicalIdentityConflicts() {

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
            actual = baseline.size
        )

        assertEquals(
            expected = 1279,
            actual = regeneratedExpansion.size,
            message =
                "Regenerated semantic expansion arithmetic changed " +
                        "unexpectedly."
        )

        val duplicateAudit =
            CanonicalIdentityDuplicateAnalyzer()
                .analyze(
                    baseline =
                        baseline,
                    regeneratedExpansion =
                        regeneratedExpansion
                )

        assertEquals(
            expected = 4698,
            actual =
                duplicateAudit.projectedCatalogEntryCount
        )

        val resolutions =
            CanonicalIdentityResolver()
                .resolve(
                    duplicateAudit.groups
                )

        val resolved =
            resolutions.filter {
                it.decision ==
                        CanonicalIdentityResolutionDecision.MERGE
            }

        val review =
            resolutions.filter {
                it.decision ==
                        CanonicalIdentityResolutionDecision.REVIEW
            }

        val removedNormalizedKeys =
            resolved
                .flatMap {
                    it.merged
                }
                .map {
                    it.normalized
                }
                .toSet()

        val removedEntryCount =
            removedNormalizedKeys.size

        val result =
            CanonicalIdentityResolutionReport(
                schemaVersion = 1,

                inputEntryCount =
                    duplicateAudit.projectedCatalogEntryCount,

                conflictGroupCount =
                    resolutions.size,

                resolvedGroupCount =
                    resolved.size,

                reviewGroupCount =
                    review.size,

                removedEntryCount =
                    removedEntryCount,

                resolvedEntryCount =
                    duplicateAudit.projectedCatalogEntryCount -
                            removedEntryCount,

                countsByReason =
                    resolutions
                        .groupingBy {
                            it.reason.name
                        }
                        .eachCount()
                        .toSortedMap(),

                resolutions =
                    resolutions
            )

        val application =
            CanonicalIdentityResolutionApplier()
                .apply(
                    baseline = baseline,
                    regeneratedExpansion =
                        regeneratedExpansion,
                    resolutions =
                        resolutions
                )

        assertEquals(
            expected = result.resolvedEntryCount,
            actual = application.outputEntryCount
        )

        val resolvedCatalogFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/regeneration/" +
                        "canonical-food-catalog-identity-resolved.json"
            )

        val resolvedArray =
            com.google.gson.JsonArray()

        application
            .entries
            .forEach {
                resolvedArray.add(it)
            }

        resolvedCatalogFile
            .parentFile
            ?.mkdirs()

        val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()

        resolvedCatalogFile.writeText(
            gson.toJson(resolvedArray) + "\n"
        )

        /*
         * Die neue deutsche Compound-Policy muss mindestens
         * einen Teil der bisherigen Punctuation-Gruppen
         * deterministisch auflösen.
         */
        assertTrue(
            result.countsByReason[
                "GERMAN_COMPOUND_PREFERRED"
            ]
                ?.let { it > 0 }
                ?: false
        )

        val outputFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/" +
                        "semantic-audit/" +
                        "canonical-food-catalog-" +
                        "identity-resolution.json"
            )

        outputFile
            .parentFile
            ?.mkdirs()

        outputFile.writeText(
            gson.toJson(result) + "\n"
        )

        println()
        println("Canonical Identity Resolution")
        println("-----------------------------")
        println(
            "Input entries        : " +
                    result.inputEntryCount
        )
        println(
            "Conflict groups      : " +
                    result.conflictGroupCount
        )
        println(
            "Resolved groups      : " +
                    result.resolvedGroupCount
        )
        println(
            "Review groups        : " +
                    result.reviewGroupCount
        )
        println(
            "Removed identities   : " +
                    result.removedEntryCount
        )
        println(
            "Projected entries    : " +
                    result.resolvedEntryCount
        )

        println()
        println("By reason:")

        result
            .countsByReason
            .forEach { (reason, count) ->
                println(
                    "  ${reason.padEnd(40)} $count"
                )
            }

        println()
        println("Remaining REVIEW groups:")

        review
            .take(30)
            .forEach { resolution ->

                println(
                    "  ${resolution.duplicateReason.name} " +
                            "[${resolution.fingerprint}]"
                )

                resolution
                    .merged
                    .forEach { candidate ->
                        println(
                            "      " +
                                    "${candidate.sourceLayer.name.padEnd(23)} " +
                                    "${candidate.normalized.padEnd(35)} " +
                                    candidate.itemName
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
                "Could not resolve ShopMe project root."
            )
    }

    private companion object {

        const val VARIANT_SEPARATOR =
            " – "
    }
}