package de.shopme.testing.system.tools.knowledge.catalog.runner

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.resolution.CanonicalProductFamilyIdentityRepairer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RepairCanonicalProductFamilyIdentitiesTest {

    @Test
    fun repairCanonicalProductFamilyIdentities() {

        val projectRoot =
            resolveProjectRoot()

        val inputFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/regeneration/" +
                        "canonical-food-catalog-identity-resolved.json"
            )

        require(inputFile.isFile) {
            "Identity-resolved catalog not found: " +
                    inputFile.absolutePath
        }

        val source =
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
            actual = source.size
        )

        val repairer =
            CanonicalProductFamilyIdentityRepairer()

        val first =
            repairer.repair(source)

        val second =
            repairer.repair(source)

        assertEquals(
            expected = first,
            actual = second,
            message =
                "Canonical family identity repair must be deterministic."
        )

        assertEquals(
            expected = 4596,
            actual = first.outputEntryCount
        )

        assertTrue(
            first.changedEntryCount > 0,
            "Expected legacy Colagetränke identities to be repaired."
        )

        assertTrue(
            first.entries.none { entry ->
                entry
                    .get("itemname")
                    .asString
                    .startsWith(
                        "Colagetränke"
                    )
            }
        )

        assertTrue(
            first.entries.any { entry ->
                entry
                    .get("itemname")
                    .asString
                    .startsWith(
                        "Cola"
                    )
            }
        )

        val outputFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/regeneration/" +
                        "canonical-food-catalog-family-repaired.json"
            )

        val json =
            JsonArray().apply {
                first
                    .entries
                    .forEach(::add)
            }

        val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()

        outputFile.writeText(
            gson.toJson(json) + "\n"
        )

        println()
        println("Canonical Product Family Identity Repair")
        println("----------------------------------------")
        println(
            "Input entries    : " +
                    first.inputEntryCount
        )
        println(
            "Output entries   : " +
                    first.outputEntryCount
        )
        println(
            "Changed entries  : " +
                    first.changedEntryCount
        )

        println()
        println(
            "Catalog: " +
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