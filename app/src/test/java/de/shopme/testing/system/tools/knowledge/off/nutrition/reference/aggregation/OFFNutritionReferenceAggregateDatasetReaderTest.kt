package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.aggregation

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.CanonicalOFFNutritionReferenceAggregate
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.OFFNutritionReferenceAggregateDatasetReader
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.OFFNutritionReferenceNutrientStatistics
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OFFNutritionReferenceAggregateDatasetReaderTest {

    @Test
    fun forEachAggregate_readsDatasetStreamingInPersistedOrder() {

        withTemporaryDirectory { directory ->

            val inputFile =
                directory.resolve(
                    "aggregates.json"
                )

            writeAggregates(
                outputFile =
                    inputFile,
                aggregates =
                    listOf(
                        createAggregate(
                            canonicalId =
                                "apple",
                            calories =
                                52.0
                        ),
                        createAggregate(
                            canonicalId =
                                "banana",
                            calories =
                                89.0
                        ),
                        createAggregate(
                            canonicalId =
                                "pear",
                            calories =
                                57.0
                        )
                    )
            )

            val consumedCanonicalIds =
                mutableListOf<String>()

            val resultCount =
                OFFNutritionReferenceAggregateDatasetReader()
                    .forEachAggregate(
                        inputFile =
                            inputFile
                    ) { aggregate ->

                        consumedCanonicalIds +=
                            aggregate.canonicalId
                    }

            assertEquals(
                3,
                resultCount
            )

            assertEquals(
                listOf(
                    "apple",
                    "banana",
                    "pear"
                ),
                consumedCanonicalIds
            )
        }
    }

    @Test
    fun read_returnsAllAggregates() {

        withTemporaryDirectory { directory ->

            val inputFile =
                directory.resolve(
                    "aggregates.json"
                )

            writeAggregates(
                outputFile =
                    inputFile,
                aggregates =
                    listOf(
                        createAggregate(
                            canonicalId =
                                "apple",
                            calories =
                                52.0
                        ),
                        createAggregate(
                            canonicalId =
                                "banana",
                            calories =
                                89.0
                        )
                    )
            )

            val aggregates =
                OFFNutritionReferenceAggregateDatasetReader()
                    .read(
                        inputFile =
                            inputFile
                    )

            assertEquals(
                2,
                aggregates.size
            )

            assertEquals(
                listOf(
                    "apple",
                    "banana"
                ),
                aggregates.map { aggregate ->
                    aggregate.canonicalId
                }
            )

            assertEquals(
                52.0,
                aggregates
                    .first()
                    .nutrition
                    .getValue(
                        "energyKcalPer100g"
                    )
            )
        }
    }

    @Test
    fun forEachAggregate_respectsMaximumAggregateCount() {

        withTemporaryDirectory { directory ->

            val inputFile =
                directory.resolve(
                    "aggregates.json"
                )

            writeAggregates(
                outputFile =
                    inputFile,
                aggregates =
                    listOf(
                        createAggregate(
                            canonicalId =
                                "apple",
                            calories =
                                52.0
                        ),
                        createAggregate(
                            canonicalId =
                                "banana",
                            calories =
                                89.0
                        ),
                        createAggregate(
                            canonicalId =
                                "pear",
                            calories =
                                57.0
                        )
                    )
            )

            val consumedCanonicalIds =
                mutableListOf<String>()

            val resultCount =
                OFFNutritionReferenceAggregateDatasetReader()
                    .forEachAggregate(
                        inputFile =
                            inputFile,
                        maxAggregates =
                            2
                    ) { aggregate ->

                        consumedCanonicalIds +=
                            aggregate.canonicalId
                    }

            assertEquals(
                2,
                resultCount
            )

            assertEquals(
                listOf(
                    "apple",
                    "banana"
                ),
                consumedCanonicalIds
            )
        }
    }

    @Test
    fun forEachAggregate_withZeroMaximumConsumesNothing() {

        withTemporaryDirectory { directory ->

            val inputFile =
                directory.resolve(
                    "aggregates.json"
                )

            writeAggregates(
                outputFile =
                    inputFile,
                aggregates =
                    listOf(
                        createAggregate(
                            canonicalId =
                                "apple",
                            calories =
                                52.0
                        )
                    )
            )

            var consumerInvocationCount =
                0

            val resultCount =
                OFFNutritionReferenceAggregateDatasetReader()
                    .forEachAggregate(
                        inputFile =
                            inputFile,
                        maxAggregates =
                            0
                    ) {
                        consumerInvocationCount++
                    }

            assertEquals(
                0,
                resultCount
            )

            assertEquals(
                0,
                consumerInvocationCount
            )
        }
    }

    @Test
    fun forEachAggregate_rejectsDuplicateCanonicalIds() {

        withTemporaryDirectory { directory ->

            val inputFile =
                directory.resolve(
                    "aggregates.json"
                )

            writeAggregates(
                outputFile =
                    inputFile,
                aggregates =
                    listOf(
                        createAggregate(
                            canonicalId =
                                "apple",
                            calories =
                                52.0
                        ),
                        createAggregate(
                            canonicalId =
                                "apple",
                            calories =
                                53.0
                        )
                    )
            )

            val exception =
                assertFailsWith<
                        IllegalArgumentException
                        > {
                    OFFNutritionReferenceAggregateDatasetReader()
                        .forEachAggregate(
                            inputFile =
                                inputFile
                        ) {
                            // Nothing to do.
                        }
                }

            assertTrue(
                exception.message.orEmpty()
                    .contains(
                        "duplicate canonicalId: apple"
                    )
            )
        }
    }

    @Test
    fun forEachAggregate_rejectsNonDeterministicOrder() {

        withTemporaryDirectory { directory ->

            val inputFile =
                directory.resolve(
                    "aggregates.json"
                )

            writeAggregates(
                outputFile =
                    inputFile,
                aggregates =
                    listOf(
                        createAggregate(
                            canonicalId =
                                "banana",
                            calories =
                                89.0
                        ),
                        createAggregate(
                            canonicalId =
                                "apple",
                            calories =
                                52.0
                        )
                    )
            )

            val exception =
                assertFailsWith<
                        IllegalArgumentException
                        > {
                    OFFNutritionReferenceAggregateDatasetReader()
                        .forEachAggregate(
                            inputFile =
                                inputFile
                        ) {
                            // Nothing to do.
                        }
                }

            assertTrue(
                exception.message.orEmpty()
                    .contains(
                        "not deterministically sorted"
                    )
            )

            assertTrue(
                exception.message.orEmpty()
                    .contains(
                        "previous=banana"
                    )
            )

            assertTrue(
                exception.message.orEmpty()
                    .contains(
                        "current=apple"
                    )
            )
        }
    }

    @Test
    fun forEachAggregate_rejectsNonArrayRoot() {

        withTemporaryDirectory { directory ->

            val inputFile =
                directory.resolve(
                    "aggregates.json"
                )

            inputFile.writeText(
                """
                {
                  "canonicalId": "apple"
                }
                """.trimIndent(),
                StandardCharsets.UTF_8
            )

            val exception =
                assertFailsWith<
                        IllegalArgumentException
                        > {
                    OFFNutritionReferenceAggregateDatasetReader()
                        .forEachAggregate(
                            inputFile =
                                inputFile
                        ) {
                            // Nothing to do.
                        }
                }

            assertTrue(
                exception.message.orEmpty()
                    .contains(
                        "must be a JSON array"
                    )
            )
        }
    }

    @Test
    fun forEachAggregate_rejectsUnexpectedTrailingContent() {

        withTemporaryDirectory { directory ->

            val inputFile =
                directory.resolve(
                    "aggregates.json"
                )

            val aggregate =
                createAggregate(
                    canonicalId =
                        "apple",
                    calories =
                        52.0
                )

            val gson =
                GsonBuilder()
                    .disableHtmlEscaping()
                    .create()

            inputFile.writeText(
                gson.toJson(
                    listOf(aggregate)
                ) +
                        "\n{}",
                StandardCharsets.UTF_8
            )

            val exception =
                assertFailsWith<
                        IllegalArgumentException
                        > {
                    OFFNutritionReferenceAggregateDatasetReader()
                        .forEachAggregate(
                            inputFile =
                                inputFile
                        ) {
                            // Nothing to do.
                        }
                }

            assertTrue(
                exception.message.orEmpty()
                    .contains(
                        "Unexpected content after"
                    )
            )
        }
    }

    @Test
    fun forEachAggregate_rejectsMissingInputFile() {

        withTemporaryDirectory { directory ->

            val inputFile =
                directory.resolve(
                    "missing.json"
                )

            val exception =
                assertFailsWith<
                        IllegalArgumentException
                        > {
                    OFFNutritionReferenceAggregateDatasetReader()
                        .forEachAggregate(
                            inputFile =
                                inputFile
                        ) {
                            // Nothing to do.
                        }
                }

            assertTrue(
                exception.message.orEmpty()
                    .contains(
                        "does not exist"
                    )
            )
        }
    }

    @Test
    fun forEachAggregate_rejectsNegativeMaximum() {

        withTemporaryDirectory { directory ->

            val inputFile =
                directory.resolve(
                    "aggregates.json"
                )

            writeAggregates(
                outputFile =
                    inputFile,
                aggregates =
                    listOf(
                        createAggregate(
                            canonicalId =
                                "apple",
                            calories =
                                52.0
                        )
                    )
            )

            val exception =
                assertFailsWith<
                        IllegalArgumentException
                        > {
                    OFFNutritionReferenceAggregateDatasetReader()
                        .forEachAggregate(
                            inputFile =
                                inputFile,
                            maxAggregates =
                                -1
                        ) {
                            // Nothing to do.
                        }
                }

            assertTrue(
                exception.message.orEmpty()
                    .contains(
                        "maxAggregates must not be negative"
                    )
            )
        }
    }

    private fun writeAggregates(
        outputFile: File,
        aggregates:
        List<CanonicalOFFNutritionReferenceAggregate>
    ) {

        outputFile.writeText(
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
                .toJson(
                    aggregates
                ) +
                    "\n",
            StandardCharsets.UTF_8
        )
    }

    private fun createAggregate(
        canonicalId: String,
        calories: Double
    ): CanonicalOFFNutritionReferenceAggregate {

        val nutrition =
            sortedMapOf(
                "energyKcalPer100g" to
                        calories,
                "fatPer100g" to
                        0.2
            )

        val nutrientStatistics =
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
                nutrientStatistics,
            profileCount =
                1,
            sourceIds =
                listOf(
                    "source-$canonicalId"
                ),
            representativeSourceId =
                "source-$canonicalId",
            source =
                "open_food_facts_aggregate",
            sourceVersion =
                "1",
            sourceConfidence =
                1.0
        )
    }

    private fun withTemporaryDirectory(
        block:
            (File) -> Unit
    ) {

        val directory =
            Files.createTempDirectory(
                "off-nutrition-aggregate-reader"
            )
                .toFile()

        try {
            block(
                directory
            )
        } finally {
            directory.deleteRecursively()
        }
    }
}