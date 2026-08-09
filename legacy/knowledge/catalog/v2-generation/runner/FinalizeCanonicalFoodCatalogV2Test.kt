package de.shopme.testing.system.tools.knowledge.catalog.runner

import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.catalog.finalization.v2.CanonicalFoodCatalogV2FinalizationWriter
import de.shopme.testing.system.tools.knowledge.catalog.finalization.v2.CanonicalFoodCatalogV2Finalizer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FinalizeCanonicalFoodCatalogV2Test {

    @Test
    fun finalizeCanonicalFoodCatalogV2() {

        val projectRoot =
            resolveProjectRoot()

        val catalogInputFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/regeneration/" +
                        "canonical-food-catalog-linguistic-repaired.json"
            )

        val linguisticReportFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/regeneration/" +
                        "canonical-food-catalog-linguistic-repair-report.json"
            )

        require(catalogInputFile.isFile) {
            "Linguistically repaired catalog not found: " +
                    catalogInputFile.absolutePath
        }

        require(linguisticReportFile.isFile) {
            "Linguistic repair report not found: " +
                    linguisticReportFile.absolutePath
        }

        val catalog =
            JsonParser
                .parseString(
                    catalogInputFile.readText()
                )
                .asJsonArray
                .mapNotNull { element ->
                    element
                        .takeIf {
                            it.isJsonObject
                        }
                        ?.asJsonObject
                }

        val linguisticReport =
            JsonParser
                .parseString(
                    linguisticReportFile.readText()
                )
                .asJsonObject

        assertEquals(
            expected = 4596,
            actual = catalog.size,
            message =
                "Canonical food catalog v2 input arithmetic changed."
        )

        assertTrue(
            linguisticReport
                .get("valid")
                .asBoolean,
            "Linguistic repair must be valid before v2 finalization."
        )

        assertEquals(
            expected = 0,
            actual =
                linguisticReport
                    .getAsJsonObject("stats")
                    .get("familyPluralFallbackCount")
                    .asInt,
            message =
                "Canonical food catalog v2 must have complete " +
                        "family plural coverage."
        )

        val finalizer =
            CanonicalFoodCatalogV2Finalizer()

        val first =
            finalizer.finalize(
                catalog = catalog,
                linguisticRepairReport =
                    linguisticReport
            )

        val second =
            finalizer.finalize(
                catalog = catalog,
                linguisticRepairReport =
                    linguisticReport
            )

        /*
         * Full deterministic finalization.
         */
        assertEquals(
            expected = first,
            actual = second,
            message =
                "Canonical food catalog v2 finalization must " +
                        "be deterministic."
        )

        val result =
            first.first

        val payload =
            first.second

        assertEquals(
            expected = 4596,
            actual = result.finalEntryCount
        )

        assertEquals(
            expected = 4596,
            actual = result.uniqueNormalizedKeyCount
        )

        assertEquals(
            expected = 4596,
            actual = result.uniqueCanonicalNameCount
        )

        assertEquals(
            expected = 0,
            actual = result.familyPluralFallbackCount
        )

        assertTrue(
            result.deterministicOrder
        )

        assertTrue(
            result.jsonRoundtripValid
        )

        assertTrue(
            result.linguisticRepairValid
        )

        assertEquals(
            expected = 64,
            actual = result.sha256.length
        )

        assertTrue(
            result.finalizationId.startsWith(
                "canonical-food-catalog-final-v2-"
            )
        )

        assertTrue(
            result.valid
        )

        val paths =
            CanonicalFoodCatalogV2FinalizationWriter()
                .write(
                    projectRoot =
                        projectRoot,
                    result =
                        result,
                    catalogPayload =
                        payload
                )

        println()
        println("Canonical Food Catalog V2 Finalization")
        println("--------------------------------------")
        println(
            "Input entries             : " +
                    result.inputEntryCount
        )
        println(
            "Final catalog entries     : " +
                    result.finalEntryCount
        )
        println(
            "Categories                : " +
                    result.categoryCount
        )

        println()
        println("Identity:")
        println(
            "  Unique canonical names  : " +
                    result.uniqueCanonicalNameCount
        )
        println(
            "  Unique normalized keys  : " +
                    result.uniqueNormalizedKeyCount
        )

        println()
        println("Linguistic:")
        println(
            "  Family plural fallback  : " +
                    result.familyPluralFallbackCount
        )
        println(
            "  Linguistic repair valid : " +
                    result.linguisticRepairValid
        )

        println()
        println("Determinism:")
        println(
            "  Deterministic order     : " +
                    result.deterministicOrder
        )
        println(
            "  JSON roundtrip          : " +
                    result.jsonRoundtripValid
        )

        println()
        println(
            "SHA-256                   : " +
                    result.sha256
        )
        println(
            "Finalization ID           : " +
                    result.finalizationId
        )
        println(
            "Finalization valid        : " +
                    result.valid
        )

        println()
        println("Release:")
        println(
            "  Final catalog           : " +
                    paths.finalCatalogPath
        )
        println(
            "  Immutable snapshot      : " +
                    paths.immutableSnapshotPath
        )
        println(
            "  Finalization report     : " +
                    paths.finalizationReportPath
        )
        println(
            "  Productive catalog      : " +
                    paths.productiveCatalogPath
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