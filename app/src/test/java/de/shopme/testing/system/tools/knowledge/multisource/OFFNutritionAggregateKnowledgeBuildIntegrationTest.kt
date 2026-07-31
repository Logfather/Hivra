package de.shopme.testing.system.tools.knowledge.multisource

import de.shopme.tools.knowledge.ai.builder.runtime.MultiSourceRuntimeKnowledgeBuild
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OFFNutritionAggregateKnowledgeBuildIntegrationTest {

    @Test
    fun build_integratesOFFNutritionAggregateDatasetIntoKnowledgeBuild() {

        val outputDirectory =
            Files.createTempDirectory(
                "off-nutrition-aggregate-build-integration"
            )
                .toFile()

        try {
            val result =
                MultiSourceRuntimeKnowledgeBuild()
                    .build(
                        offFile =
                            File(
                                "../data/preview/openfoodfacts/" +
                                        "off-products-preview-50k.jsonl.gz"
                            ),
                        offNutritionAggregateFile =
                            File(
                                "../data/generated/knowledge/references/off/" +
                                        "off-nutrition-reference-aggregates.json"
                            ),
                        agribalyseFile =
                            File(
                                "../data/generated/agribalyse/" +
                                        "agribalyse-foods.slim.tsv"
                            ),
                        ciqualDirectory =
                            File(
                                "../data/raw/ciqual/Ciqual"
                            ),
                        outputDir =
                            outputDirectory,
                        maxOffCandidates =
                            MAX_OFF_CANDIDATES,
                        maxOffNutritionAggregates =
                            MAX_OFF_NUTRITION_AGGREGATES
                    )

            assertTrue(
                result.offCandidateCount > 0,
                "The Knowledge Build must process raw OFF candidates."
            )

            assertTrue(
                result.offCandidateCount <=
                        MAX_OFF_CANDIDATES,
                "The raw OFF candidate count must respect the configured limit."
            )

            assertTrue(
                result.offNutritionAggregateCount > 0,
                "The Knowledge Build must process OFF nutrition aggregates."
            )

            assertTrue(
                result.offNutritionAggregateCount <=
                        MAX_OFF_NUTRITION_AGGREGATES,
                "The OFF nutrition aggregate count must respect the configured limit."
            )

            val expectedInputCandidateCount =
                result.offCandidateCount +
                        result.offNutritionAggregateCount +
                        result.agribalyseCandidateCount +
                        result.ciqualCandidateCount

            assertEquals(
                expected =
                    expectedInputCandidateCount,
                actual =
                    result.inputCandidateCount,
                message =
                    "Input candidate count must equal the sum of raw OFF, " +
                            "OFF nutrition aggregate, Agribalyse and CIQUAL candidates."
            )

            assertTrue(
                result.normalizedCandidateCount > 0,
                "The Knowledge Build must normalize candidates."
            )

            assertTrue(
                result.mergedCandidateCount > 0,
                "The Knowledge Build must produce merged candidates."
            )

            assertTrue(
                result.nutritionArtifactEntryCount > 0,
                "The generated nutrition artifact must contain entries."
            )

            assertTrue(
                result.nutritionArtifactFile.isFile,
                "The generated nutrition artifact file must exist."
            )

            assertTrue(
                result.nutritionArtifactFile.length() > 0L,
                "The generated nutrition artifact file must not be empty."
            )
        } finally {
            outputDirectory.deleteRecursively()
        }
    }

    private companion object {

        const val MAX_OFF_CANDIDATES =
            50_000

        const val MAX_OFF_NUTRITION_AGGREGATES =
            50_000
    }
}