package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.deduplication

import de.shopme.tools.knowledge.off.extractor.OFFCandidateExtractor
import de.shopme.tools.knowledge.off.nutrition.reference.OFFNutritionReferenceCandidateGenerator
import de.shopme.tools.knowledge.off.nutrition.reference.OFFNutritionReferenceDatasetWriter
import de.shopme.tools.knowledge.off.nutrition.reference.deduplication.OFFNutritionReferenceCandidateDeduplicator
import de.shopme.tools.knowledge.off.nutrition.reference.deduplication.OFFNutritionReferenceDuplicateReportWriter
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityFilter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeduplicateOFFNutritionReferenceCandidatesTest {

    @Test
    fun deduplicateOFFNutritionReferenceCandidates() {

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

        val outputDirectory =
            File(
                "../data/generated/knowledge/off/nutrition"
            )
                .canonicalFile

        val datasetFile =
            outputDirectory.resolve(
                "nutrition-reference-candidates.deduplicated.json"
            )

        val duplicateReportFile =
            outputDirectory.resolve(
                "nutrition-reference-duplicates.json"
            )

        val writeResult =
            OFFNutritionReferenceDatasetWriter()
                .write(
                    candidates =
                        deduplicationResult.candidates,
                    outputFile =
                        datasetFile
                )

        OFFNutritionReferenceDuplicateReportWriter()
            .write(
                result =
                    deduplicationResult,
                outputFile =
                    duplicateReportFile
            )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("OFF NUTRITION REFERENCE DEDUPLICATION")
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
            "inputCandidates=" +
                    deduplicationResult.inputCandidateCount
        )
        println(
            "outputCandidates=" +
                    deduplicationResult.outputCandidateCount
        )
        println(
            "removedDuplicates=" +
                    deduplicationResult.removedDuplicateCount
        )
        println(
            "duplicateGroups=" +
                    deduplicationResult.duplicateGroupCount
        )
        println(
            "deduplicationRate=" +
                    deduplicationRate(
                        removedDuplicateCount =
                            deduplicationResult
                                .removedDuplicateCount,
                        inputCandidateCount =
                            deduplicationResult
                                .inputCandidateCount
                    )
        )
        println(
            "largestDuplicateGroups=" +
                    deduplicationResult
                        .duplicateGroups
                        .sortedWith(
                            compareByDescending<
                                    de.shopme.tools.knowledge.off
                                    .nutrition.reference
                                    .deduplication
                                    .OFFNutritionReferenceDuplicateGroup
                                    > {
                                it.mergedSourceIds.size
                            }
                                .thenBy {
                                    it.canonicalId
                                }
                        )
                        .take(20)
        )
        println(
            "datasetFile=" +
                    datasetFile.absolutePath
        )
        println(
            "duplicateReportFile=" +
                    duplicateReportFile.absolutePath
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        assertEquals(
            qualityFilterResult.acceptedCandidateCount,
            deduplicationResult.inputCandidateCount
        )

        assertEquals(
            deduplicationResult.inputCandidateCount,
            deduplicationResult.outputCandidateCount +
                    deduplicationResult.removedDuplicateCount
        )

        assertEquals(
            deduplicationResult.outputCandidateCount,
            writeResult.candidateCount
        )

        assertTrue(
            deduplicationResult.outputCandidateCount > 0
        )

        assertTrue(
            deduplicationResult.outputCandidateCount <=
                    deduplicationResult.inputCandidateCount
        )

        assertTrue(datasetFile.isFile)
        assertTrue(duplicateReportFile.isFile)

        assertEquals(
            deduplicationResult.candidates
                .sortedWith(
                    OFFNutritionReferenceCandidateDeduplicator
                        .OUTPUT_COMPARATOR
                ),
            deduplicationResult.candidates
        )
    }

    private fun deduplicationRate(
        removedDuplicateCount: Int,
        inputCandidateCount: Int
    ): Double {

        if (inputCandidateCount == 0) {
            return 0.0
        }

        return removedDuplicateCount.toDouble() /
                inputCandidateCount.toDouble()
    }

    private companion object {

        const val MAX_CANDIDATES =
            50_000
    }
}