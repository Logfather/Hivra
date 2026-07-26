package de.shopme.testing.system.tools.knowledge.off.nutrition.reference

import de.shopme.tools.knowledge.off.extractor.OFFCandidateExtractor
import de.shopme.tools.knowledge.off.nutrition.reference.OFFNutritionReferenceCandidateGenerator
import de.shopme.tools.knowledge.off.nutrition.reference.OFFNutritionReferenceDatasetWriter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenerateOFFNutritionReferenceCandidatesTest {

    @Test
    fun generateCanonicalOFFNutritionReferenceCandidates() {

        val inputFile =
            File(
                "../data/generated/openfoodfacts/" +
                        "openfoodfacts-products.slim.jsonl.gz"
            )

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

        val outputFile =
            File(
                "../data/generated/knowledge/off/nutrition/" +
                        "nutrition-reference-candidates.json"
            )

        val writeResult =
            OFFNutritionReferenceDatasetWriter()
                .write(
                    candidates =
                        generationResult.candidates,
                    outputFile =
                        outputFile
                )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("OFF NUTRITION REFERENCE DATASET")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "inputCandidates=" +
                    generationResult.inputCandidateCount
        )
        println(
            "generatedCandidates=" +
                    generationResult.generatedCandidateCount
        )
        println(
            "skippedWithoutNutrition=" +
                    generationResult.skippedWithoutNutritionCount
        )
        println(
            "skippedInvalidIdentity=" +
                    generationResult.skippedInvalidIdentityCount
        )
        println(
            "skippedInvalidNutritionPayload=" +
                    generationResult
                        .skippedInvalidNutritionPayloadCount
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
        println(
            "sample=" +
                    generationResult.candidates.take(10)
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        assertEquals(
            extracted.size,
            generationResult.inputCandidateCount
        )

        assertTrue(
            generationResult.generatedCandidateCount > 0
        )

        assertEquals(
            generationResult.generatedCandidateCount,
            writeResult.candidateCount
        )

        assertTrue(
            generationResult.candidates.all { candidate ->
                candidate.sourceId.isNotBlank()
            }
        )

        assertTrue(
            generationResult.candidates.all { candidate ->
                candidate.canonicalId.isNotBlank()
            }
        )

        assertTrue(
            generationResult.candidates.all { candidate ->
                candidate.nutrition.isNotEmpty()
            }
        )

        assertEquals(
            generationResult.candidates
                .sortedWith(
                    OFFNutritionReferenceDatasetWriter
                        .CANDIDATE_COMPARATOR
                ),
            generationResult.candidates
        )

        assertTrue(
            outputFile.isFile
        )

        assertTrue(
            outputFile.length() > 0L
        )

        assertEquals(
            outputFile.length(),
            writeResult.fileSizeBytes
        )
    }

    private companion object {

        const val MAX_CANDIDATES =
            50_000
    }
}