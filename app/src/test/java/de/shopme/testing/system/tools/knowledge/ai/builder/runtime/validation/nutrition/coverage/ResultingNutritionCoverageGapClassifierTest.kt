package de.shopme.testing.system.tools.knowledge.ai.builder.runtime.validation.nutrition.coverage

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.coverage.ResultingNutritionCoverageGapClassifier
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResultingNutritionCoverageGapClassifierTest {

    @Test
    fun classify_separatesExactNormalizedAndTrueGaps() {

        val directory =
            Files.createTempDirectory(
                "nutrition-coverage-gap-classification"
            )
                .toFile()

        try {
            val aggregateFile =
                directory.resolve(
                    "aggregates.json"
                )

            val runtimeFile =
                directory.resolve(
                    "nutrition.json"
                )

            aggregateFile.writeText(
                """
                [
                  {"canonicalId": "apple"},
                  {"canonicalId": "fat-free milk"},
                  {"canonicalId": "missing product"},
                  {"canonicalId": "stir-fried noodles"}
                ]
                """.trimIndent()
            )

            runtimeFile.writeText(
                """
                {
                  "entries": {
                    "additional product": {},
                    "apple": {},
                    "fat free milk": {},
                    "stir fried noodles": {}
                  }
                }
                """.trimIndent()
            )

            val result =
                ResultingNutritionCoverageGapClassifier()
                    .classify(
                        aggregateFile =
                            aggregateFile,
                        runtimeFile =
                            runtimeFile
                    )

            assertEquals(
                4L,
                result.aggregateEntryCount
            )

            assertEquals(
                4L,
                result.runtimeEntryCount
            )

            assertEquals(
                1L,
                result.exactMatchCount
            )

            assertEquals(
                2L,
                result.normalizationEquivalentMatchCount
            )

            assertEquals(
                1L,
                result.trueMissingRuntimeEntryCount
            )

            assertEquals(
                1L,
                result.trueAdditionalRuntimeEntryCount
            )

            assertEquals(
                0.25,
                result.exactCoverageRate
            )

            assertEquals(
                0.75,
                result.effectiveCoverageRate
            )

            assertEquals(
                listOf(
                    "missing product"
                ),
                result.trueMissingRuntimeCanonicalIds
            )

            assertEquals(
                listOf(
                    "additional product"
                ),
                result.trueAdditionalRuntimeCanonicalIds
            )

            assertEquals(
                listOf(
                    "fat-free milk",
                    "stir-fried noodles"
                ),
                result.normalizationEquivalentExamples
                    .map { pair ->
                        pair.aggregateCanonicalId
                    }
            )

            assertFalse(
                result.complete
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun classify_prefersExactMatchesBeforeNormalization() {

        val directory =
            Files.createTempDirectory(
                "nutrition-coverage-exact-precedence"
            )
                .toFile()

        try {
            val aggregateFile =
                directory.resolve(
                    "aggregates.json"
                )

            val runtimeFile =
                directory.resolve(
                    "nutrition.json"
                )

            aggregateFile.writeText(
                """
                [
                  {"canonicalId": "a b"},
                  {"canonicalId": "a-b"}
                ]
                """.trimIndent()
            )

            runtimeFile.writeText(
                """
                {
                  "entries": {
                    "a b": {},
                    "a-b": {}
                  }
                }
                """.trimIndent()
            )

            val result =
                ResultingNutritionCoverageGapClassifier()
                    .classify(
                        aggregateFile =
                            aggregateFile,
                        runtimeFile =
                            runtimeFile
                    )

            assertEquals(
                2L,
                result.exactMatchCount
            )

            assertEquals(
                0L,
                result.normalizationEquivalentMatchCount
            )

            assertEquals(
                0L,
                result.trueMissingRuntimeEntryCount
            )

            assertEquals(
                0L,
                result.trueAdditionalRuntimeEntryCount
            )

            assertEquals(
                1.0,
                result.effectiveCoverageRate
            )

            assertTrue(
                result.complete
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun classify_pairsNormalizationCollisionsDeterministically() {

        val directory =
            Files.createTempDirectory(
                "nutrition-coverage-normalization-collision"
            )
                .toFile()

        try {
            val aggregateFile =
                directory.resolve(
                    "aggregates.json"
                )

            val runtimeFile =
                directory.resolve(
                    "nutrition.json"
                )

            aggregateFile.writeText(
                """
                [
                  {"canonicalId": "a--b"},
                  {"canonicalId": "a-b"}
                ]
                """.trimIndent()
            )

            runtimeFile.writeText(
                """
                {
                  "entries": {
                    "a b": {}
                  }
                }
                """.trimIndent()
            )

            val result =
                ResultingNutritionCoverageGapClassifier()
                    .classify(
                        aggregateFile =
                            aggregateFile,
                        runtimeFile =
                            runtimeFile
                    )

            assertEquals(
                1L,
                result.normalizationEquivalentMatchCount
            )

            assertEquals(
                1L,
                result.trueMissingRuntimeEntryCount
            )

            assertEquals(
                1L,
                result.normalizationCollisionGroupCount
            )

            assertEquals(
                "a--b",
                result.normalizationEquivalentExamples
                    .single()
                    .aggregateCanonicalId
            )

            assertEquals(
                listOf(
                    "a-b"
                ),
                result.trueMissingRuntimeCanonicalIds
            )
        } finally {
            directory.deleteRecursively()
        }
    }
}