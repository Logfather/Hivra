package de.shopme.testing.system.tools.knowledge.off.nutrition.reference

import de.shopme.tools.knowledge.off.extractor.OFFCandidateExtractor
import de.shopme.tools.knowledge.off.nutrition.reference.OFFNutritionReferenceCandidateGenerator
import de.shopme.tools.knowledge.off.nutrition.reference.OFFNutritionReferenceDatasetWriter
import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.CollectingOFFNutritionReferenceCandidateTraceSink
import de.shopme.tools.knowledge.off.nutrition.reference.candidate.diagnostic.OFFNutritionReferenceCandidateTraceWriter
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
            ).canonicalFile

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

        val traceSink =
            CollectingOFFNutritionReferenceCandidateTraceSink()

        val generationResult =
            OFFNutritionReferenceCandidateGenerator(
                traceSink =
                    traceSink
            )
                .generate(
                    candidates =
                        extracted
                )

        val outputDirectory =
            File(
                "../data/generated/knowledge/off/nutrition"
            ).canonicalFile

        val outputFile =
            outputDirectory.resolve(
                "nutrition-reference-candidates.json"
            )

        val traceFile =
            outputDirectory.resolve(
                "nutrition-reference-candidate-traces.json"
            )

        val writeResult =
            OFFNutritionReferenceDatasetWriter()
                .write(
                    candidates =
                        generationResult.candidates,
                    outputFile =
                        outputFile
                )

        val traces =
            traceSink.traces()

        OFFNutritionReferenceCandidateTraceWriter()
            .write(
                traces =
                    traces,
                outputFile =
                    traceFile
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
                    generationResult.skippedInvalidNutritionPayloadCount
        )
        println(
            "persistedCandidates=" +
                    writeResult.candidateCount
        )
        println(
            "traceCount=" +
                    traces.size
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
            "traceFile=" +
                    traceFile.absolutePath
        )
        println(
            "sample=" +
                    generationResult.candidates.take(10)
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        assertEquals(
            expected =
                extracted.size,
            actual =
                generationResult.inputCandidateCount
        )

        assertTrue(
            actual =
                generationResult.generatedCandidateCount > 0
        )

        assertEquals(
            expected =
                generationResult.generatedCandidateCount,
            actual =
                writeResult.candidateCount
        )

        /*
         * Der Generator muss für jeden verarbeiteten Eingangskandidaten
         * genau einen finalen Trace erzeugen:
         *
         * - Kandidat erzeugt
         * - ungültige Identität
         * - keine Nutrition-Dimension
         * - ungültiger Nutrition-Payload
         */
        assertEquals(
            expected =
                generationResult.inputCandidateCount,
            actual =
                traces.size,
            message =
                "Every input candidate must produce exactly one diagnostic trace."
        )

        assertEquals(
            expected =
                generationResult.generatedCandidateCount,
            actual =
                traces.count { trace ->
                    trace.candidateCreated
                },
            message =
                "Created-candidate traces must match generatedCandidateCount."
        )

        assertTrue(
            actual =
                generationResult.candidates.all { candidate ->
                    candidate.sourceId.isNotBlank()
                }
        )

        assertTrue(
            actual =
                generationResult.candidates.all { candidate ->
                    candidate.canonicalId.isNotBlank()
                }
        )

        assertTrue(
            actual =
                generationResult.candidates.all { candidate ->
                    candidate.nutrition.isNotEmpty()
                }
        )

        assertEquals(
            expected =
                generationResult.candidates
                    .sortedWith(
                        OFFNutritionReferenceDatasetWriter
                            .CANDIDATE_COMPARATOR
                    ),
            actual =
                generationResult.candidates
        )

        assertTrue(
            actual =
                outputFile.isFile,
            message =
                "Reference candidate dataset was not written: " +
                        outputFile.absolutePath
        )

        assertTrue(
            actual =
                outputFile.length() > 0L
        )

        assertEquals(
            expected =
                outputFile.length(),
            actual =
                writeResult.fileSizeBytes
        )

        assertTrue(
            actual =
                traceFile.isFile,
            message =
                "Reference candidate trace file was not written: " +
                        traceFile.absolutePath
        )

        assertTrue(
            actual =
                traceFile.length() > 0L
        )
    }

    private companion object {

        const val MAX_CANDIDATES =
            50_000
    }
}