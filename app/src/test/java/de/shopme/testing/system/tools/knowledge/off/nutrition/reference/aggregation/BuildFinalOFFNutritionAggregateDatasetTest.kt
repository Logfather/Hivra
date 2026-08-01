package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.aggregation

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import de.shopme.tools.knowledge.off.nutrition.reference.CanonicalOFFNutritionReferenceCandidate
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.BuildFinalOFFNutritionAggregateDataset
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.CanonicalOFFNutritionReferenceAggregate
import java.io.BufferedOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.GZIPOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuildFinalOFFNutritionAggregateDatasetTest {

    private val gson =
        GsonBuilder()
            .disableHtmlEscaping()
            .create()

    @Test
    fun buildFinalAggregateDataset() {

        val temporaryDirectory =
            Files.createTempDirectory(
                "off-nutrition-aggregate-build-test"
            ).toFile()

        try {
            val acceptedReferenceFile =
                temporaryDirectory.resolve(
                    "accepted.jsonl.gz"
                )

            val aggregateDatasetFile =
                temporaryDirectory.resolve(
                    "aggregate-dataset.json"
                )

            val aggregationReportFile =
                temporaryDirectory.resolve(
                    "aggregation-report.json"
                )

            writeAcceptedReferences(
                outputFile =
                    acceptedReferenceFile,
                candidates =
                    listOf(
                        candidate(
                            sourceId =
                                "source-1",
                            canonicalId =
                                "apple",
                            nutrition =
                                mapOf(
                                    "energyKcalPer100g" to 50.0,
                                    "sugarsPer100g" to 10.0
                                )
                        ),
                        candidate(
                            sourceId =
                                "source-2",
                            canonicalId =
                                "apple",
                            nutrition =
                                mapOf(
                                    "energyKcalPer100g" to 50.0,
                                    "sugarsPer100g" to 10.0
                                )
                        ),
                        candidate(
                            sourceId =
                                "source-3",
                            canonicalId =
                                "apple",
                            nutrition =
                                mapOf(
                                    "energyKcalPer100g" to 60.0,
                                    "sugarsPer100g" to 12.0
                                )
                        ),
                        candidate(
                            sourceId =
                                "source-4",
                            canonicalId =
                                "banana",
                            nutrition =
                                mapOf(
                                    "energyKcalPer100g" to 89.0,
                                    "sugarsPer100g" to 12.0
                                )
                        )
                    )
            )

            val result =
                BuildFinalOFFNutritionAggregateDataset()
                    .run(
                        acceptedReferenceFile =
                            acceptedReferenceFile,
                        aggregateDatasetOutputFile =
                            aggregateDatasetFile,
                        aggregationReportOutputFile =
                            aggregationReportFile,
                        bucketCount =
                            4
                    )

            assertEquals(
                4L,
                result.inputReferenceCount
            )

            assertEquals(
                3L,
                result.deduplicatedReferenceCount
            )

            assertEquals(
                1L,
                result.removedDuplicateCount
            )

            assertEquals(
                1L,
                result.duplicateGroupCount
            )

            assertEquals(
                2,
                result.aggregateCount
            )

            assertEquals(
                1,
                result.singleProfileAggregateCount
            )

            assertEquals(
                1,
                result.multiProfileAggregateCount
            )

            assertEquals(
                2,
                result.maximumProfileCount
            )

            assertTrue(
                result.aggregateDatasetFile.isFile
            )

            assertTrue(
                result.aggregationReportFile.isFile
            )

            val aggregates =
                readAggregates(
                    inputFile =
                        aggregateDatasetFile
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

            val apple =
                aggregates.single { aggregate ->
                    aggregate.canonicalId == "apple"
                }

            assertEquals(
                2,
                apple.profileCount
            )

            assertEquals(
                55.0,
                apple.nutrition.getValue(
                    "energyKcalPer100g"
                )
            )

            assertEquals(
                11.0,
                apple.nutrition.getValue(
                    "sugarsPer100g"
                )
            )

            val banana =
                aggregates.single { aggregate ->
                    aggregate.canonicalId == "banana"
                }

            assertEquals(
                1,
                banana.profileCount
            )

            assertEquals(
                89.0,
                banana.nutrition.getValue(
                    "energyKcalPer100g"
                )
            )
        } finally {
            temporaryDirectory.deleteRecursively()
        }
    }

    private fun writeAcceptedReferences(
        outputFile: File,
        candidates:
        List<CanonicalOFFNutritionReferenceCandidate>
    ) {
        GZIPOutputStream(
            BufferedOutputStream(
                outputFile.outputStream()
            )
        ).bufferedWriter(
            StandardCharsets.UTF_8
        ).use { writer ->

            candidates.forEach { candidate ->
                gson.toJson(
                    candidate,
                    writer
                )

                writer.newLine()
            }
        }
    }

    private fun readAggregates(
        inputFile: File
    ): List<CanonicalOFFNutritionReferenceAggregate> {

        val listType =
            object :
                TypeToken<
                        List<CanonicalOFFNutritionReferenceAggregate>
                        >() {}
                .type

        return inputFile
            .bufferedReader(
                StandardCharsets.UTF_8
            )
            .use { reader ->
                gson.fromJson(
                    reader,
                    listType
                )
            }
    }

    private fun candidate(
        sourceId: String,
        canonicalId: String,
        nutrition: Map<String, Double>
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
                nutrition.toSortedMap(),
            productName =
                canonicalId,
            brand =
                null,
            categories =
                null,
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