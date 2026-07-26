package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.aggregation

import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.CanonicalOFFNutritionReferenceAggregate
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.OFFNutritionReferenceAggregateDatasetWriter
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.OFFNutritionReferenceNutrientStatistics
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OFFNutritionReferenceAggregateDatasetWriterTest {

    @Test
    fun write_persistsDeterministicAggregateDataset() {

        val outputFile =
            Files
                .createTempDirectory(
                    "off-nutrition-aggregates"
                )
                .resolve(
                    "aggregates.json"
                )
                .toFile()

        val aggregate =
            createAggregate()

        val writer =
            OFFNutritionReferenceAggregateDatasetWriter()

        val firstResult =
            writer.write(
                aggregates =
                    listOf(aggregate),
                outputFile =
                    outputFile
            )

        val firstContent =
            outputFile.readText()

        val secondResult =
            writer.write(
                aggregates =
                    listOf(aggregate),
                outputFile =
                    outputFile
            )

        val secondContent =
            outputFile.readText()

        assertTrue(outputFile.isFile)

        assertEquals(
            1,
            firstResult.aggregateCount
        )

        assertEquals(
            firstResult.aggregateCount,
            secondResult.aggregateCount
        )

        assertEquals(
            firstContent,
            secondContent
        )

        assertTrue(
            firstContent.contains(
                "\"canonicalId\": \"apple\""
            )
        )

        assertTrue(
            firstContent.contains(
                "\"profileCount\": 2"
            )
        )
    }

    private fun createAggregate():
            CanonicalOFFNutritionReferenceAggregate =
        CanonicalOFFNutritionReferenceAggregate(
            canonicalId =
                "apple",
            aliases =
                sortedSetOf(
                    "apple",
                    "fresh apple"
                ),
            matchAliases =
                sortedSetOf("fruit"),
            singleIngredientNutritionAliases =
                sortedSetOf("apple"),
            nutrition =
                sortedMapOf(
                    "fatPer100g" to 0.3
                ),
            nutrientStatistics =
                sortedMapOf(
                    "fatPer100g" to
                            OFFNutritionReferenceNutrientStatistics(
                                observationCount =
                                    2,
                                minimum =
                                    0.2,
                                median =
                                    0.3,
                                maximum =
                                    0.4
                            )
                ),
            profileCount =
                2,
            sourceIds =
                listOf(
                    "111",
                    "222"
                ),
            representativeSourceId =
                "111",
            source =
                "open_food_facts_aggregate",
            sourceVersion =
                "1",
            sourceConfidence =
                1.0
        )
}