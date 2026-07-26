package de.shopme.testing.system.tools.knowledge.off.nutrition.reference

import com.google.gson.JsonParser
import de.shopme.tools.knowledge.off.nutrition.reference.CanonicalOFFNutritionReferenceCandidate
import de.shopme.tools.knowledge.off.nutrition.reference.OFFNutritionReferenceDatasetWriter
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OFFNutritionReferenceDatasetWriterTest {

    @Test
    fun write_persistsCandidatesDeterministically() {

        val outputDirectory =
            Files
                .createTempDirectory(
                    "off-nutrition-reference-dataset-writer"
                )
                .toFile()

        try {
            val outputFile =
                outputDirectory.resolve(
                    "nutrition-reference-candidates.json"
                )

            val candidates =
                listOf(
                    createCandidate(
                        sourceId =
                            "2222222222222",
                        canonicalId =
                            "plain yogurt"
                    ),
                    createCandidate(
                        sourceId =
                            "1111111111111",
                        canonicalId =
                            "apple"
                    )
                )

            val writer =
                OFFNutritionReferenceDatasetWriter()

            val firstResult =
                writer.write(
                    candidates =
                        candidates,
                    outputFile =
                        outputFile
                )

            val firstContent =
                outputFile.readText()

            val secondResult =
                writer.write(
                    candidates =
                        candidates.reversed(),
                    outputFile =
                        outputFile
                )

            val secondContent =
                outputFile.readText()

            assertTrue(
                outputFile.isFile
            )

            assertEquals(
                2,
                firstResult.candidateCount
            )

            assertEquals(
                2,
                secondResult.candidateCount
            )

            assertEquals(
                outputFile.length(),
                secondResult.fileSizeBytes
            )

            assertEquals(
                firstContent,
                secondContent
            )

            val persistedCandidates =
                JsonParser
                    .parseString(secondContent)
                    .asJsonArray

            assertEquals(
                2,
                persistedCandidates.size()
            )

            assertEquals(
                "1111111111111",
                persistedCandidates[0]
                    .asJsonObject["sourceId"]
                    .asString
            )

            assertEquals(
                "2222222222222",
                persistedCandidates[1]
                    .asJsonObject["sourceId"]
                    .asString
            )

            assertEquals(
                "apple",
                persistedCandidates[0]
                    .asJsonObject["canonicalId"]
                    .asString
            )

            assertEquals(
                "plain yogurt",
                persistedCandidates[1]
                    .asJsonObject["canonicalId"]
                    .asString
            )

            assertFalse(
                outputDirectory
                    .resolve(
                        "nutrition-reference-candidates.json.tmp"
                    )
                    .exists()
            )
        } finally {
            outputDirectory.deleteRecursively()
        }
    }

    @Test
    fun write_rejectsDuplicateSourceIds() {

        val outputDirectory =
            Files
                .createTempDirectory(
                    "off-nutrition-reference-duplicate-source-id"
                )
                .toFile()

        try {
            val outputFile =
                outputDirectory.resolve(
                    "nutrition-reference-candidates.json"
                )

            val candidates =
                listOf(
                    createCandidate(
                        sourceId =
                            "1111111111111",
                        canonicalId =
                            "apple"
                    ),
                    createCandidate(
                        sourceId =
                            "1111111111111",
                        canonicalId =
                            "apple juice"
                    )
                )

            val exception =
                assertFailsWith<IllegalArgumentException> {
                    OFFNutritionReferenceDatasetWriter()
                        .write(
                            candidates =
                                candidates,
                            outputFile =
                                outputFile
                        )
                }

            assertTrue(
                exception.message
                    .orEmpty()
                    .contains(
                        "duplicate sourceIds"
                    )
            )

            assertFalse(
                outputFile.exists()
            )
        } finally {
            outputDirectory.deleteRecursively()
        }
    }

    @Test
    fun write_rejectsEmptyNutritionPayload() {

        val outputDirectory =
            Files
                .createTempDirectory(
                    "off-nutrition-reference-empty-nutrition"
                )
                .toFile()

        try {
            val outputFile =
                outputDirectory.resolve(
                    "nutrition-reference-candidates.json"
                )

            val invalidCandidate =
                createCandidate(
                    sourceId =
                        "1111111111111",
                    canonicalId =
                        "apple"
                )
                    .copy(
                        nutrition =
                            emptyMap()
                    )

            val exception =
                assertFailsWith<IllegalArgumentException> {
                    OFFNutritionReferenceDatasetWriter()
                        .write(
                            candidates =
                                listOf(invalidCandidate),
                            outputFile =
                                outputFile
                        )
                }

            assertTrue(
                exception.message
                    .orEmpty()
                    .contains(
                        "empty nutrition payload"
                    )
            )

            assertFalse(
                outputFile.exists()
            )
        } finally {
            outputDirectory.deleteRecursively()
        }
    }

    @Test
    fun write_overwritesExistingDataset() {

        val outputDirectory =
            Files
                .createTempDirectory(
                    "off-nutrition-reference-overwrite"
                )
                .toFile()

        try {
            val outputFile =
                outputDirectory.resolve(
                    "nutrition-reference-candidates.json"
                )

            outputFile.writeText(
                "obsolete"
            )

            val result =
                OFFNutritionReferenceDatasetWriter()
                    .write(
                        candidates =
                            listOf(
                                createCandidate(
                                    sourceId =
                                        "1111111111111",
                                    canonicalId =
                                        "apple"
                                )
                            ),
                        outputFile =
                            outputFile
                    )

            assertEquals(
                1,
                result.candidateCount
            )

            assertTrue(
                outputFile.readText()
                    .trim()
                    .startsWith("[")
            )

            assertFalse(
                outputFile.readText()
                    .contains(
                        "obsolete"
                    )
            )
        } finally {
            outputDirectory.deleteRecursively()
        }
    }

    private fun createCandidate(
        sourceId: String,
        canonicalId: String
    ): CanonicalOFFNutritionReferenceCandidate =
        CanonicalOFFNutritionReferenceCandidate(
            sourceId =
                sourceId,
            canonicalId =
                canonicalId,
            aliases =
                sortedSetOf(
                    canonicalId
                ),
            matchAliases =
                sortedSetOf(
                    canonicalId
                ),
            nutrition =
                sortedMapOf(
                    "energyKcalPer100g" to 61.0,
                    "fatPer100g" to 3.3,
                    "saturatedFatPer100g" to 2.1,
                    "carbohydratesPer100g" to 4.7,
                    "sugarsPer100g" to 4.7,
                    "proteinsPer100g" to 3.5,
                    "saltPer100g" to 0.1
                ),
            productName =
                canonicalId,
            brand =
                "example brand",
            categories =
                "example category",
            singleIngredientNutritionAliases =
                sortedSetOf(
                    canonicalId
                ),
            source =
                "open_food_facts",
            sourceVersion =
                "1",
            sourceConfidence =
                1.0
        )
}