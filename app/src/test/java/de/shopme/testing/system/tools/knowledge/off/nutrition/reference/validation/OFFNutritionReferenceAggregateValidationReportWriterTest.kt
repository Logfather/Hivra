package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.validation

import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.CanonicalOFFNutritionReferenceAggregate
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.OFFNutritionReferenceNutrientStatistics
import de.shopme.tools.knowledge.off.nutrition.reference.validation.OFFNutritionReferenceAggregateValidationReportWriter
import de.shopme.tools.knowledge.off.nutrition.reference.validation.OFFNutritionReferenceAggregateValidator
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OFFNutritionReferenceAggregateValidationReportWriterTest {

    @Test
    fun write_persistsDeterministicValidationReport() {

        val aggregate =
            createAggregate()

        val result =
            OFFNutritionReferenceAggregateValidator()
                .validate(
                    aggregates =
                        listOf(aggregate)
                )

        val outputFile =
            Files
                .createTempDirectory(
                    "off-nutrition-validation"
                )
                .resolve(
                    "validation-report.json"
                )
                .toFile()

        val writer =
            OFFNutritionReferenceAggregateValidationReportWriter()

        writer.write(
            result =
                result,
            outputFile =
                outputFile
        )

        val firstContent =
            outputFile.readText()

        writer.write(
            result =
                result,
            outputFile =
                outputFile
        )

        val secondContent =
            outputFile.readText()

        assertTrue(outputFile.isFile)

        assertEquals(
            firstContent,
            secondContent
        )

        assertTrue(
            firstContent.contains(
                "\"inputAggregateCount\": 1"
            )
        )

        assertTrue(
            firstContent.contains(
                "\"canonicalId\": \"sample\""
            )
        )
    }

    private fun createAggregate():
            CanonicalOFFNutritionReferenceAggregate {

        val nutrition =
            sortedMapOf(
                "energyKcalPer100g" to 100.0,
                "fatPer100g" to 2.0,
                "carbohydratesPer100g" to 10.0,
                "proteinsPer100g" to 4.0,
                "saltPer100g" to 0.1
            )

        val statistics =
            nutrition
                .mapValues { (_, value) ->
                    OFFNutritionReferenceNutrientStatistics(
                        observationCount =
                            1,
                        minimum =
                            value,
                        median =
                            value,
                        maximum =
                            value
                    )
                }
                .toSortedMap()

        return CanonicalOFFNutritionReferenceAggregate(
            canonicalId =
                "sample",
            aliases =
                sortedSetOf("sample"),
            matchAliases =
                emptySet(),
            singleIngredientNutritionAliases =
                emptySet(),
            nutrition =
                nutrition,
            nutrientStatistics =
                statistics,
            profileCount =
                1,
            sourceIds =
                listOf("001"),
            representativeSourceId =
                "001",
            source =
                "open_food_facts_aggregate",
            sourceVersion =
                "1",
            sourceConfidence =
                1.0
        )
    }
}