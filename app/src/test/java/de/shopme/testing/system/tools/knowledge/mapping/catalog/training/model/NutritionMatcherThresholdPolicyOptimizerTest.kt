package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureContract
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionMatcherThresholdPolicyOptimizer
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NutritionMatcherThresholdPolicyOptimizerTest {

    @Test
    fun optimizeThresholdPoliciesDeterministically() {

        val datasetFile =
            File(
                "..",
                "data/generated/knowledge/training/" +
                        "nutrition.matcher-training-dataset.json",
            )

        require(
            datasetFile.isFile,
        )

        val directory =
            createTempDirectory(
                prefix =
                    "nutrition-threshold-policy-",
            )
                .toFile()

        try {
            val optimizer =
                NutritionMatcherThresholdPolicyOptimizer()

            val first =
                optimizer.optimize(
                    datasetFile =
                        datasetFile,
                    workingDirectory =
                        File(
                            directory,
                            "first",
                        ),
                )

            val second =
                optimizer.optimize(
                    datasetFile =
                        datasetFile,
                    workingDirectory =
                        File(
                            directory,
                            "second",
                        ),
                )

            assertEquals(
                expected =
                    first,
                actual =
                    second,
            )

            assertEquals(
                expected =
                    4,
                actual =
                    first.candidateCount,
            )

            assertEquals(
                expected =
                    LocalNutritionMatcherFeatureContract
                        .ACTIVE_FEATURE_NAMES,
                actual =
                    first.featureNames,
            )

            first.entries.forEach { entry ->
                assertTrue(
                    entry.decisionThreshold in
                            0.0..1.0,
                )

                assertEquals(
                    expected =
                        entry.testExampleCount,
                    actual =
                        entry.testTruePositiveCount +
                                entry.testFalsePositiveCount +
                                entry.testTrueNegativeCount +
                                entry.testFalseNegativeCount,
                )
            }

            val recommended =
                first.entries
                    .singleOrNull { entry ->
                        entry.name ==
                                first.recommendedCandidateName
                    }

            assertNotNull(
                actual =
                    recommended,
            )

            assertEquals(
                expected =
                    recommended.decisionThreshold,
                actual =
                    first.recommendedDecisionThreshold,
            )
        } finally {
            directory.deleteRecursively()
        }
    }
}