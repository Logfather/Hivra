package de.shopme.testing.system.tools.knowledge.off.nutrition.reference

import de.shopme.tools.knowledge.off.extractor.OFFCandidateExtractor
import de.shopme.tools.knowledge.off.nutrition.reference.OFFNutritionReferenceCandidateGenerator
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
                        50_000
                )

        val result =
            OFFNutritionReferenceCandidateGenerator()
                .generate(
                    candidates =
                        extracted
                )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("OFF NUTRITION REFERENCE CANDIDATES")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("input=${result.inputCandidateCount}")
        println("generated=${result.generatedCandidateCount}")
        println(
            "skippedWithoutNutrition=" +
                    result.skippedWithoutNutritionCount
        )
        println(
            "skippedInvalidIdentity=" +
                    result.skippedInvalidIdentityCount
        )
        println(
            "skippedInvalidNutritionPayload=" +
                    result.skippedInvalidNutritionPayloadCount
        )
        println("sample=${result.candidates.take(10)}")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        assertEquals(
            extracted.size,
            result.inputCandidateCount
        )

        assertTrue(
            result.generatedCandidateCount > 0
        )

        assertTrue(
            result.candidates.all { candidate ->
                candidate.sourceId.isNotBlank()
            }
        )

        assertTrue(
            result.candidates.all { candidate ->
                candidate.canonicalId.isNotBlank()
            }
        )

        assertTrue(
            result.candidates.all { candidate ->
                candidate.nutrition.isNotEmpty()
            }
        )

        assertEquals(
            result.candidates
                .sortedWith(
                    compareBy(
                        { it.sourceId },
                        { it.canonicalId }
                    )
                ),
            result.candidates
        )
    }
}