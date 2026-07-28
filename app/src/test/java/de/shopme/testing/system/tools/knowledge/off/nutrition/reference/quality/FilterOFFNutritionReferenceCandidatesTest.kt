package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.quality

import de.shopme.tools.knowledge.off.extractor.OFFCandidateExtractor
import de.shopme.tools.knowledge.off.nutrition.reference.OFFNutritionReferenceCandidateGenerator
import de.shopme.tools.knowledge.off.nutrition.reference.OFFNutritionReferenceDatasetWriter
import de.shopme.tools.knowledge.off.nutrition.reference.quality.OFFNutritionReferenceQualityFilter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilterOFFNutritionReferenceCandidatesTest {

    @Test
    fun filterCanonicalOFFNutritionReferenceCandidates() {

        val inputFile =
            File(
                "../data/generated/openfoodfacts/" +
                        "openfoodfacts-products.slim.jsonl.gz"
            )
                .canonicalFile

        require(inputFile.isFile) {
            "OFF slim dump not found: ${inputFile.absolutePath}"
        }

        val extracted =
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
                        extracted
                )

        val qualityFilterResult =
            OFFNutritionReferenceQualityFilter()
                .filter(
                    candidates =
                        generationResult.candidates
                )

        val outputFile =
            File(
                "../data/generated/knowledge/off/nutrition/" +
                        "nutrition-reference-candidates." +
                        "quality-filtered.json"
            )
                .canonicalFile

        val writeResult =
            OFFNutritionReferenceDatasetWriter()
                .write(
                    candidates =
                        qualityFilterResult.acceptedCandidates,
                    outputFile =
                        outputFile
                )

        val acceptanceRate =
            acceptanceRate(
                acceptedCandidateCount =
                    qualityFilterResult.acceptedCandidateCount,
                inputCandidateCount =
                    qualityFilterResult.inputCandidateCount
            )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("OFF NUTRITION QUALITY FILTER")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "generatedCandidates=" +
                    generationResult.generatedCandidateCount
        )
        println(
            "acceptedCandidates=" +
                    qualityFilterResult.acceptedCandidateCount
        )
        println(
            "rejectedCandidates=" +
                    qualityFilterResult.rejectedCandidateCount
        )
        println(
            "acceptanceRate=" +
                    acceptanceRate
        )
        println(
            "countsByReason=" +
                    qualityFilterResult.countsByReason
        )
        println(
            "rejectionSample=" +
                    qualityFilterResult.rejections.take(20)
        )
        println(
            "persistedCandidates=" +
                    writeResult.candidateCount
        )
        println(
            "fileSizeBytes=" +
                    writeResult.fileSizeBytes
        )
        println(
            "outputFile=" +
                    writeResult.outputFile.absolutePath
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        assertEquals(
            generationResult.generatedCandidateCount,
            qualityFilterResult.inputCandidateCount
        )

        assertEquals(
            qualityFilterResult.inputCandidateCount,
            qualityFilterResult.acceptedCandidateCount +
                    qualityFilterResult.rejectedCandidateCount
        )

        assertEquals(
            qualityFilterResult.acceptedCandidateCount,
            qualityFilterResult.acceptedCandidates.size
        )

        assertEquals(
            qualityFilterResult.rejectedCandidateCount,
            qualityFilterResult.rejections.size
        )

        assertEquals(
            qualityFilterResult.acceptedCandidateCount,
            writeResult.candidateCount
        )

        assertEquals(
            outputFile,
            writeResult.outputFile.canonicalFile
        )

        assertTrue(
            qualityFilterResult.acceptedCandidateCount > 0,
            "The quality filter must retain at least one candidate."
        )

        assertTrue(
            qualityFilterResult.rejectedCandidateCount > 0,
            "The quality filter must reject at least one candidate."
        )

        assertTrue(
            qualityFilterResult.countsByReason.isNotEmpty(),
            "Quality rejection counts must not be empty when " +
                    "candidates were rejected."
        )

        assertTrue(
            acceptanceRate in 0.0..1.0,
            "Acceptance rate must be between zero and one: " +
                    acceptanceRate
        )

        assertTrue(
            writeResult.outputFile.isFile,
            "Quality-filtered OFF nutrition reference dataset " +
                    "was not written."
        )

        assertTrue(
            writeResult.fileSizeBytes > 0L,
            "Quality-filtered OFF nutrition reference dataset " +
                    "must not be empty."
        )

        assertEquals(
            qualityFilterResult.acceptedCandidates
                .sortedWith(
                    OFFNutritionReferenceQualityFilter
                        .CANDIDATE_COMPARATOR
                ),
            qualityFilterResult.acceptedCandidates
        )
    }

    private fun acceptanceRate(
        acceptedCandidateCount: Int,
        inputCandidateCount: Int
    ): Double {

        if (inputCandidateCount == 0) {
            return 0.0
        }

        return acceptedCandidateCount.toDouble() /
                inputCandidateCount.toDouble()
    }

    private companion object {

        const val MAX_CANDIDATES =
            50_000
    }
}