package de.shopme.testing.system.tools.knowledge.catalog.runner

import com.google.gson.JsonParser
import de.shopme.testing.system.tools.knowledge.catalog.semantic.regeneration.SemanticExpansionRegenerationWriter
import de.shopme.testing.system.tools.knowledge.catalog.semantic.regeneration.SemanticExpansionRegenerator
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegenerateSemanticExpansionTest {

    @Test
    fun regenerateSemanticExpansion() {

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
            "Frozen canonical catalog v1 not found: " +
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

        val result =
            SemanticExpansionRegenerator()
                .regenerate(catalog)

        assertEquals(
            expected = 3419,
            actual = result.baselineEntryCount,
            message =
                "Frozen v1 baseline arithmetic changed unexpectedly."
        )

        assertEquals(
            expected = 6935,
            actual = result.expansionInputEntryCount,
            message =
                "Frozen v1 expansion arithmetic changed unexpectedly."
        )

        assertEquals(
            expected = 6935,
            actual =
                result.acceptedInputEntryCount +
                        result.rejectedInputEntryCount +
                        result.reviewInputEntryCount,
            message =
                "Every v1 expansion entry must receive exactly one " +
                        "regeneration disposition."
        )

        assertTrue(
            result.regeneratedExpansionEntryCount > 0
        )

        assertTrue(
            result.projectedCatalogEntryCount <
                    result.inputCatalogEntryCount,
            message =
                "Semantic regeneration should reduce the synthetic " +
                        "v1 catalog."
        )

        /*
         * REJECT und REVIEW dürfen niemals materialisiert werden.
         */
        val outputKeys =
            result
                .regeneratedExpansion
                .map {
                    it
                        .get("normalized")
                        .asString
                }
                .toSet()

        assertEquals(
            expected =
                result.regeneratedExpansionEntryCount,
            actual =
                outputKeys.size,
            message =
                "Regenerated semantic expansion must have unique " +
                        "normalized keys."
        )

        val outputDirectory =
            File(
                projectRoot,
                "data/generated/knowledge/catalog/regeneration"
            )

        SemanticExpansionRegenerationWriter()
            .write(
                result = result,
                outputDirectory = outputDirectory
            )

        println()
        println("Semantic Expansion Regeneration")
        println("-------------------------------")
        println(
            "Input catalog entries              : " +
                    result.inputCatalogEntryCount
        )
        println(
            "Baseline entries                   : " +
                    result.baselineEntryCount
        )
        println(
            "Expansion input entries            : " +
                    result.expansionInputEntryCount
        )

        println()
        println("Market disposition:")
        println(
            "  ACCEPT                           : " +
                    result.acceptedInputEntryCount
        )
        println(
            "  REJECT                           : " +
                    result.rejectedInputEntryCount
        )
        println(
            "  REVIEW                           : " +
                    result.reviewInputEntryCount
        )

        println()
        println("Projection:")
        println(
            "  Accepted projected candidates    : " +
                    result.acceptedProjectedEntryCount
        )
        println(
            "  Knowledge projections            : " +
                    result.knowledgeAttributeProjectionCount
        )
        println(
            "  Collapsed duplicate identities   : " +
                    result.collapsedDuplicateCount
        )

        println()
        println(
            "Regenerated expansion entries      : " +
                    result.regeneratedExpansionEntryCount
        )
        println(
            "Projected catalog entries          : " +
                    result.projectedCatalogEntryCount
        )

        println()
        println(
            "Expansion: data/generated/knowledge/catalog/" +
                    "regeneration/" +
                    "canonical-food-catalog-semantic-" +
                    "expansion-regenerated.json"
        )

        println(
            "Report   : data/generated/knowledge/catalog/" +
                    "regeneration/" +
                    "canonical-food-catalog-semantic-" +
                    "expansion-regeneration-report.json"
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