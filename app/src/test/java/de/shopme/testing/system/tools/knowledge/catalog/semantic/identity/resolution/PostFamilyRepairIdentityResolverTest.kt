package de.shopme.testing.system.tools.knowledge.catalog.semantic.identity.resolution

import com.google.gson.JsonParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PostFamilyRepairIdentityResolverTest {

    @Test
    fun collapsesSyntheticAppleJuiceIntoExistingCanonicalIdentity() {

        val catalog =
            listOf(
                item(
                    """
                    {
                      "itemname": "Apfelsaft",
                      "category": "beverages",
                      "production": "Standard",
                      "normalized": "apfelsaft"
                    }
                    """
                ),
                item(
                    """
                    {
                      "itemname": "Apfelsaft",
                      "category": "beverages",
                      "production": "Standard",
                      "normalized": "fruit-juice-apple"
                    }
                    """
                )
            )

        val result =
            PostFamilyRepairIdentityResolver()
                .resolve(catalog)

        assertEquals(
            expected = 1,
            actual = result.collisionGroupCount
        )

        assertEquals(
            expected = 1,
            actual = result.removedEntryCount
        )

        assertEquals(
            expected = 1,
            actual = result.outputEntryCount
        )

        assertEquals(
            expected = "apfelsaft",
            actual =
                result.entries
                    .single()
                    .get("normalized")
                    .asString
        )

        assertTrue(
            "fruit-juice-apple" in
                    result.removedNormalizedKeys
        )
    }

    @Test
    fun leavesUniqueIdentitiesUntouched() {

        val catalog =
            listOf(
                item(
                    """
                    {
                      "itemname": "Orangensaft",
                      "category": "beverages",
                      "production": "Standard",
                      "normalized": "fruit-juice-orange"
                    }
                    """
                )
            )

        val result =
            PostFamilyRepairIdentityResolver()
                .resolve(catalog)

        assertEquals(
            expected = 0,
            actual = result.collisionGroupCount
        )

        assertEquals(
            expected = 1,
            actual = result.outputEntryCount
        )
    }

    private fun item(
        json: String
    ) =
        JsonParser
            .parseString(
                json.trimIndent()
            )
            .asJsonObject
}