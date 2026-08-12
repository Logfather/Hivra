package de.shopme.testing.system.tools.knowledge.catalog.canonical.rebuild

import com.google.gson.JsonParser
import de.shopme.tools.knowledge.build.KnowledgeBuildPaths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RebuiltCanonicalFoodCatalogSchemaTest {

    @Test
    fun rebuiltCatalogContainsOnlyCanonicalIdentityFields() {

        val paths =
            KnowledgeBuildPaths.default()

        val file =
            paths.projectRoot.resolve(
                "build/knowledge/catalog/rebuild/" +
                        "canonical-food-catalog.vnext.json"
            )

        require(
            file.isFile
        )

        val entries =
            JsonParser
                .parseString(
                    file.readText()
                )
                .asJsonArray

        assertTrue(
            entries.size() >
                    0
        )

        entries.forEach { element ->

            val json =
                element.asJsonObject

            assertEquals(
                setOf(
                    "itemname",
                    "normalized",
                    "category",
                    "variants",
                    "sourceVariants"
                ),
                json.keySet()
            )

            assertFalse(
                json[
                    "itemname"
                ].asString.isBlank()
            )

            assertFalse(
                json[
                    "normalized"
                ].asString.isBlank()
            )

            assertFalse(
                json[
                    "category"
                ].asString.isBlank()
            )

            assertTrue(
                json[
                    "variants"
                ].isJsonArray
            )

            assertTrue(
                json[
                    "sourceVariants"
                ].isJsonArray
            )
        }
    }
}