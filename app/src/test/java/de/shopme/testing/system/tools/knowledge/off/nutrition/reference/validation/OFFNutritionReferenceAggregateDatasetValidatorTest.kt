package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.validation

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.CanonicalOFFNutritionReferenceAggregate
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.OFFNutritionReferenceNutrientStatistics
import de.shopme.tools.knowledge.off.nutrition.reference.validation.OFFNutritionReferenceAggregateDatasetValidator
import de.shopme.tools.knowledge.off.nutrition.reference.validation.OFFNutritionReferenceAggregateValidationStatus
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OFFNutritionReferenceAggregateDatasetValidatorTest {

    @Test
    fun validate_readsDatasetStreamingAndPersistsOnlyDiagnostics() {

        val temporaryDirectory =
            Files.createTempDirectory(
                "off-nutrition-dataset-validation"
            ).toFile()

        try {
            val inputFile =
                temporaryDirectory.resolve(
                    "aggregates.json"
                )

            val aggregates =
                listOf(
                    createAggregate(
                        canonicalId =
                            "apple",
                        profileCount =
                            1,
                        energy =
                            52.0,
                        fat =
                            0.2
                    ),
                    createAggregate(
                        canonicalId =
                            "yogurt",
                        profileCount =
                            50,
                        energy =
                            61.0,
                        fat =
                            3.3
                    )
                )

            inputFile.writeText(
                GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create()
                    .toJson(
                        aggregates
                    ),
                StandardCharsets.UTF_8
            )

            val result =
                OFFNutritionReferenceAggregateDatasetValidator()
                    .validate(
                        inputFile =
                            inputFile
                    )

            assertEquals(
                2,
                result.inputAggregateCount
            )

            assertEquals(
                1,
                result.acceptedAggregateCount
            )

            assertEquals(
                1,
                result.warningAggregateCount
            )

            assertEquals(
                0,
                result.rejectedAggregateCount
            )

            assertEquals(
                51L,
                result.totalProfileCount
            )

            assertEquals(
                1,
                result.warningEntries.size
            )

            assertEquals(
                OFFNutritionReferenceAggregateValidationStatus.WARNING,
                result.warningEntries.single().status
            )

            assertEquals(
                "yogurt",
                result.warningEntries.single().canonicalId
            )

            assertTrue(
                result.rejectedEntries.isEmpty()
            )

            assertTrue(result.valid)
        } finally {
            temporaryDirectory.deleteRecursively()
        }
    }

    private fun createAggregate(
        canonicalId: String,
        profileCount: Int,
        energy: Double,
        fat: Double
    ): CanonicalOFFNutritionReferenceAggregate {

        val nutrition =
            sortedMapOf(
                "energyKcalPer100g" to energy,
                "fatPer100g" to fat
            )

        val statistics =
            nutrition
                .mapValues { (_, value) ->
                    OFFNutritionReferenceNutrientStatistics(
                        observationCount =
                            profileCount,
                        minimum =
                            value,
                        median =
                            value,
                        maximum =
                            value
                    )
                }
                .toSortedMap()

        val sourceIds =
            (1..profileCount)
                .map { index ->
                    index
                        .toString()
                        .padStart(
                            length =
                                3,
                            padChar =
                                '0'
                        )
                }

        return CanonicalOFFNutritionReferenceAggregate(
            canonicalId =
                canonicalId,
            aliases =
                sortedSetOf(
                    canonicalId
                ),
            matchAliases =
                emptySet(),
            singleIngredientNutritionAliases =
                emptySet(),
            nutrition =
                nutrition,
            nutrientStatistics =
                statistics,
            profileCount =
                profileCount,
            sourceIds =
                sourceIds,
            representativeSourceId =
                sourceIds.first(),
            source =
                "open_food_facts_aggregate",
            sourceVersion =
                "1",
            sourceConfidence =
                1.0
        )
    }
}