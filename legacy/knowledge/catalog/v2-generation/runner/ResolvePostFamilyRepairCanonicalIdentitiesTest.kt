package de.shopme.testing.system.tools.knowledge.catalog.runner

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.resolution.PostFamilyRepairIdentityResolver
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResolvePostFamilyRepairCanonicalIdentitiesTest {

    @Test
    fun resolvePostFamilyRepairCanonicalIdentities() {

        val projectRoot =
            resolveProjectRoot()

        val inputFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/regeneration/" +
                        "canonical-food-catalog-family-repaired.json"
            )

        require(inputFile.isFile) {
            "Family-repaired catalog not found: " +
                    inputFile.absolutePath
        }

        val entries =
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
            expected = 4597,
            actual = entries.size,
            message =
                "Family-repaired catalog arithmetic changed unexpectedly."
        )

        val resolver =
            PostFamilyRepairIdentityResolver()

        val first =
            resolver.resolve(entries)

        val second =
            resolver.resolve(entries)

        assertEquals(
            expected = first,
            actual = second,
            message =
                "Post-family identity resolution must be deterministic."
        )

        /*
         * Aktuell exakt eine bekannte neue Kollision:
         *
         * Apfelsaft / apfelsaft
         * Apfelsaft / fruit-juice-apple
         */
        assertEquals(
            expected = 1,
            actual = first.collisionGroupCount
        )

        assertEquals(
            expected = 1,
            actual = first.removedEntryCount
        )

        assertEquals(
            expected = 4596,
            actual = first.outputEntryCount
        )

        assertTrue(
            "fruit-juice-apple" in
                    first.removedNormalizedKeys
        )

        val outputFile =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/regeneration/" +
                        "canonical-food-catalog-family-identity-resolved.json"
            )

        val array =
            JsonArray().apply {
                first.entries.forEach(::add)
            }

        val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()

        outputFile
            .parentFile
            ?.mkdirs()

        outputFile.writeText(
            gson.toJson(array) + "\n"
        )

        println()
        println("Post-Family Canonical Identity Resolution")
        println("-----------------------------------------")
        println(
            "Input entries       : " +
                    first.inputEntryCount
        )
        println(
            "Collision groups    : " +
                    first.collisionGroupCount
        )
        println(
            "Removed identities  : " +
                    first.removedEntryCount
        )
        println(
            "Output entries      : " +
                    first.outputEntryCount
        )

        println()
        println("Removed normalized keys:")

        first
            .removedNormalizedKeys
            .forEach {
                println(
                    "  $it"
                )
            }

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
            File(".").canonicalFile

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