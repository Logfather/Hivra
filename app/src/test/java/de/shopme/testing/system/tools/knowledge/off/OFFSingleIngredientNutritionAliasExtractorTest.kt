package de.shopme.testing.system.tools.knowledge.off

import com.google.gson.JsonParser
import de.shopme.tools.knowledge.off.extractor.OFFSingleIngredientNutritionAliasExtractor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OFFSingleIngredientNutritionAliasExtractorTest {

    private val extractor =
        OFFSingleIngredientNutritionAliasExtractor()

    @Test
    fun extractSingleIngredientFromTags() {

        val product =
            JsonParser
                .parseString(
                    """
                    {
                      "ingredients_tags": [
                        "en:black-salsify"
                      ]
                    }
                    """.trimIndent(),
                )
                .asJsonObject

        assertEquals(
            expected =
                setOf(
                    "black salsify",
                ),
            actual =
                extractor.extract(
                    product =
                        product,
                ),
        )
    }

    @Test
    fun extractSingleIngredientFromText() {

        val product =
            JsonParser
                .parseString(
                    """
                    {
                      "ingredients_text_en": "Chervil"
                    }
                    """.trimIndent(),
                )
                .asJsonObject

        assertEquals(
            expected =
                setOf(
                    "chervil",
                ),
            actual =
                extractor.extract(
                    product =
                        product,
                ),
        )
    }

    @Test
    fun normalizeGermanDiacritics() {

        val product =
            JsonParser
                .parseString(
                    """
                    {
                      "ingredients_text_de": "Muskatblüte"
                    }
                    """.trimIndent(),
                )
                .asJsonObject

        assertEquals(
            expected =
                setOf(
                    "muskatblute",
                ),
            actual =
                extractor.extract(
                    product =
                        product,
                ),
        )
    }

    @Test
    fun rejectMultipleIngredientTags() {

        val product =
            JsonParser
                .parseString(
                    """
                    {
                      "ingredients_tags": [
                        "en:mace",
                        "en:salt"
                      ]
                    }
                    """.trimIndent(),
                )
                .asJsonObject

        assertTrue(
            actual =
                extractor
                    .extract(
                        product =
                            product,
                    )
                    .isEmpty(),
        )
    }

    @Test
    fun rejectCompositeIngredientText() {

        val product =
            JsonParser
                .parseString(
                    """
                    {
                      "ingredients_text_en":
                        "Black salsify, water, salt"
                    }
                    """.trimIndent(),
                )
                .asJsonObject

        assertTrue(
            actual =
                extractor
                    .extract(
                        product =
                            product,
                    )
                    .isEmpty(),
        )
    }

    @Test
    fun rejectPercentageIngredientText() {

        val product =
            JsonParser
                .parseString(
                    """
                    {
                      "ingredients_text_en":
                        "Chervil 98%"
                    }
                    """.trimIndent(),
                )
                .asJsonObject

        assertTrue(
            actual =
                extractor
                    .extract(
                        product =
                            product,
                    )
                    .isEmpty(),
        )
    }

    @Test
    fun rejectMixtureDescription() {

        val product =
            JsonParser
                .parseString(
                    """
                    {
                      "ingredients_text_en":
                        "Mace spice blend"
                    }
                    """.trimIndent(),
                )
                .asJsonObject

        assertTrue(
            actual =
                extractor
                    .extract(
                        product =
                            product,
                    )
                    .isEmpty(),
        )
    }
}