package de.shopme.testing.system.tools.knowledge.off

import com.google.gson.JsonParser
import de.shopme.tools.knowledge.ki_candidates.KnowledgeDimensionCandidateType
import de.shopme.tools.knowledge.off.extractor.OFFCandidateExtractor
import java.io.File
import java.nio.file.Files
import java.util.zip.GZIPOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OFFSingleIngredientNutritionCandidateExtractionTest {

    @Test
    fun addSingleIngredientAliasToValidNutritionCandidate() {

        val directory =
            Files.createTempDirectory(
                "off-single-ingredient-nutrition-",
            ).toFile()

        try {
            val inputFile =
                File(
                    directory,
                    "off.jsonl.gz",
                )

            writeGzipJsonLines(
                file =
                    inputFile,
                lines =
                    listOf(
                        """
                        {
                          "code": "single-chervil",
                          "product_name_en": "Organic herb refill",
                          "brands": "Example",
                          "categories": "Herbs and spices",
                          "ingredients_tags": [
                            "en:chervil"
                          ],
                          "ingredients_text_en": "Chervil",
                          "nutriments": {
                            "energy-kcal_100g": 237.0,
                            "fat_100g": 3.9,
                            "saturated-fat_100g": 1.0,
                            "carbohydrates_100g": 49.1,
                            "sugars_100g": 0.0,
                            "fiber_100g": 11.3,
                            "proteins_100g": 23.2,
                            "salt_100g": 0.2
                          }
                        }
                        """.trimIndent(),
                    ),
            )

            val candidate =
                OFFCandidateExtractor()
                    .extract(
                        file =
                            inputFile,
                    )
                    .single()

            assertTrue(
                actual =
                    "chervil" in
                            candidate.matchAliases,
            )

            assertTrue(
                actual =
                    candidate.dimensions.any { dimension ->
                        dimension.dimension ==
                                KnowledgeDimensionCandidateType.NUTRITION
                    },
            )

            assertEquals(
                expected =
                    "chervil",
                actual =
                    candidate
                        .metadata
                        .attributes[
                        "singleIngredientNutritionAliases"
                    ],
            )

        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun doNotAddIngredientAliasToCompositeProduct() {

        val directory =
            Files.createTempDirectory(
                "off-composite-ingredient-nutrition-",
            ).toFile()

        try {
            val inputFile =
                File(
                    directory,
                    "off.jsonl.gz",
                )

            writeGzipJsonLines(
                file =
                    inputFile,
                lines =
                    listOf(
                        """
                        {
                          "code": "mixed-seasoning",
                          "product_name_en": "Vegetable seasoning",
                          "brands": "Example",
                          "categories": "Seasonings",
                          "ingredients_tags": [
                            "en:chervil",
                            "en:salt",
                            "en:onion"
                          ],
                          "ingredients_text_en":
                            "Chervil, salt, onion",
                          "nutriments": {
                            "energy-kcal_100g": 120.0,
                            "fat_100g": 1.0,
                            "carbohydrates_100g": 22.0,
                            "proteins_100g": 4.0,
                            "salt_100g": 18.0
                          }
                        }
                        """.trimIndent(),
                    ),
            )

            val candidate =
                OFFCandidateExtractor()
                    .extract(
                        file =
                            inputFile,
                    )
                    .single()

            assertFalse(
                actual =
                    "chervil" in
                            candidate.matchAliases,
            )

            assertTrue(
                actual =
                    candidate.dimensions.any { dimension ->
                        dimension.dimension ==
                                KnowledgeDimensionCandidateType.NUTRITION
                    },
            )

            assertFalse(
                actual =
                    candidate
                        .metadata
                        .attributes
                        .containsKey(
                            "singleIngredientNutritionAliases",
                        ),
            )

        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun doNotAddAliasWithoutValidNutrition() {

        val directory =
            Files.createTempDirectory(
                "off-single-ingredient-without-nutrition-",
            ).toFile()

        try {
            val inputFile =
                File(
                    directory,
                    "off.jsonl.gz",
                )

            writeGzipJsonLines(
                file =
                    inputFile,
                lines =
                    listOf(
                        """
                        {
                          "code": "single-mace",
                          "product_name_en": "Spice refill",
                          "brands": "Example",
                          "categories": "Spices",
                          "ingredients_tags": [
                            "en:mace"
                          ],
                          "ingredients_text_en": "Mace"
                        }
                        """.trimIndent(),
                    ),
            )

            val candidate =
                OFFCandidateExtractor()
                    .extract(
                        file =
                            inputFile,
                    )
                    .single()

            assertFalse(
                actual =
                    "mace" in
                            candidate.matchAliases,
            )

            assertFalse(
                actual =
                    candidate.dimensions.any { dimension ->
                        dimension.dimension ==
                                KnowledgeDimensionCandidateType.NUTRITION
                    },
            )

        } finally {
            directory.deleteRecursively()
        }
    }

    private fun writeGzipJsonLines(
        file: File,
        lines: List<String>,
    ) {
        file.parentFile?.mkdirs()

        GZIPOutputStream(
            file.outputStream(),
        )
            .bufferedWriter(
                Charsets.UTF_8,
            )
            .use { writer ->

                lines.forEach { rawJson ->

                    val compactJson =
                        JsonParser
                            .parseString(
                                rawJson,
                            )
                            .toString()

                    writer.write(
                        compactJson,
                    )

                    writer.newLine()
                }
            }
    }
}