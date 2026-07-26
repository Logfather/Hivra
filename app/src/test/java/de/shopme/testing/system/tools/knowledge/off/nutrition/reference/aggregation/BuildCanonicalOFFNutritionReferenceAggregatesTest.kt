package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.aggregation

import de.shopme.tools.knowledge.off.extractor.OFFCandidateExtractor
import de.shopme.tools.knowledge.off.nutrition.reference.OFFNutritionReferenceCandidateGenerator
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.OFFNutritionReferenceAggregateDatasetWriter
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.OFFNutritionReferenceAggregationReportWriter
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.OFFNutritionReferenceAggregator
import de.shopme.tools.knowledge.off.nutrition.reference.deduplication.OFFNutritionReferenceCandidateDeduplicator
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityFilter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BuildCanonicalOFFNutritionReferenceAggregatesTest {

    @Test
    fun buildCanonicalOFFNutritionReferenceAggregates() {

        val inputFile =
            File(
                "../data/generated/openfoodfacts/" +
                        "openfoodfacts-products.slim.jsonl.gz"
            )
                .canonicalFile

        require(inputFile.isFile) {
            "OFF slim dump not found: ${inputFile.absolutePath}"
        }

        val extractedCandidates =
            OFFCandidateExtractor()
                .extract(
                    file =
                        inputFile,
                    maxCandidates =
                        MAX_CANDIDATES
                )

        val generationResult =
            OFFNutritionReferenceCandidateGenerator()
                .generate(
                    candidates =
                        extractedCandidates
                )

        val qualityFilterResult =
            OFFNutritionReferenceQualityFilter()
                .filter(
                    candidates =
                        generationResult.candidates
                )

        val deduplicationResult =
            OFFNutritionReferenceCandidateDeduplicator()
                .deduplicate(
                    candidates =
                        qualityFilterResult.acceptedCandidates
                )

        val aggregationResult =
            OFFNutritionReferenceAggregator()
                .aggregate(
                    candidates =
                        deduplicationResult.candidates
                )

        val outputDirectory =
            File(
                "../data/generated/knowledge/off/nutrition"
            )
                .canonicalFile

        val aggregateDatasetFile =
            outputDirectory.resolve(
                "nutrition-reference-aggregates.json"
            )

        val aggregationReportFile =
            outputDirectory.resolve(
                "nutrition-reference-aggregation-report.json"
            )

        val datasetWriteResult =
            OFFNutritionReferenceAggregateDatasetWriter()
                .write(
                    aggregates =
                        aggregationResult.aggregates,
                    outputFile =
                        aggregateDatasetFile
                )

        OFFNutritionReferenceAggregationReportWriter()
            .write(
                result =
                    aggregationResult,
                outputFile =
                    aggregationReportFile
            )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("CANONICAL OFF NUTRITION REFERENCE AGGREGATES")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "generatedCandidates=" +
                    generationResult.generatedCandidateCount
        )
        println(
            "qualityAcceptedCandidates=" +
                    qualityFilterResult.acceptedCandidateCount
        )
        println(
            "deduplicatedCandidates=" +
                    deduplicationResult.outputCandidateCount
        )
        println(
            "aggregateInputCandidates=" +
                    aggregationResult.inputCandidateCount
        )
        println(
            "aggregateCount=" +
                    aggregationResult.aggregateCount
        )
        println(
            "singleProfileAggregates=" +
                    aggregationResult
                        .singleProfileAggregateCount
        )
        println(
            "multiProfileAggregates=" +
                    aggregationResult
                        .multiProfileAggregateCount
        )
        println(
            "maximumProfileCount=" +
                    aggregationResult.maximumProfileCount
        )
        println(
            "largestAggregates=" +
                    aggregationResult
                        .aggregates
                        .sortedWith(
                            compareByDescending<
                                    de.shopme.tools.knowledge.off
                                    .nutrition.reference
                                    .aggregation
                                    .CanonicalOFFNutritionReferenceAggregate
                                    > {
                                it.profileCount
                            }
                                .thenBy {
                                    it.canonicalId
                                }
                        )
                        .take(20)
                        .map { aggregate ->
                            "${aggregate.canonicalId}=" +
                                    aggregate.profileCount
                        }
        )
        println(
            "aggregateDatasetFile=" +
                    aggregateDatasetFile.absolutePath
        )
        println(
            "aggregationReportFile=" +
                    aggregationReportFile.absolutePath
        )
        println(
            "aggregateDatasetSizeBytes=" +
                    datasetWriteResult.fileSizeBytes
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        assertEquals(
            deduplicationResult.outputCandidateCount,
            aggregationResult.inputCandidateCount
        )

        assertEquals(
            aggregationResult.aggregateCount,
            datasetWriteResult.aggregateCount
        )

        assertEquals(
            aggregationResult.inputCandidateCount,
            aggregationResult.aggregates
                .sumOf { aggregate ->
                    aggregate.profileCount
                }
        )

        assertTrue(
            aggregationResult.aggregateCount > 0
        )

        assertTrue(
            aggregationResult.aggregateCount <=
                    aggregationResult.inputCandidateCount
        )

        assertTrue(aggregateDatasetFile.isFile)
        assertTrue(aggregationReportFile.isFile)

        assertEquals(
            aggregationResult.aggregates
                .map { aggregate ->
                    aggregate.canonicalId
                }
                .distinct()
                .size,
            aggregationResult.aggregateCount
        )
    }

    private companion object {

        const val MAX_CANDIDATES =
            50_000
    }
}