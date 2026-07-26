package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureContract
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionFeatureSetOptimizer
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NutritionFeatureSetOptimizerTest {

    @Test
    fun compareNutritionFeatureSetsDeterministically() {

        val projectDirectory =
            locateProjectDirectory()

        val datasetFile =
            File(
                projectDirectory,
                "data/generated/knowledge/training/" +
                        "nutrition.matcher-training-dataset.json",
            )

        require(
            datasetFile.isFile,
        ) {
            "Nutrition matcher training dataset does not exist: " +
                    datasetFile.absolutePath
        }

        val temporaryDirectory =
            createTempDirectory(
                prefix =
                    "nutrition-feature-set-optimizer-",
            )
                .toFile()

        try {
            val firstWorkingDirectory =
                File(
                    temporaryDirectory,
                    "first",
                )

            val secondWorkingDirectory =
                File(
                    temporaryDirectory,
                    "second",
                )

            val optimizer =
                NutritionFeatureSetOptimizer()

            val firstReport =
                optimizer.optimize(
                    datasetFile =
                        datasetFile,
                    workingDirectory =
                        firstWorkingDirectory,
                )

            val secondReport =
                optimizer.optimize(
                    datasetFile =
                        datasetFile,
                    workingDirectory =
                        secondWorkingDirectory,
                )

            assertEquals(
                expected =
                    firstReport,
                actual =
                    secondReport,
            )

            assertEquals(
                expected =
                    NutritionFeatureSetOptimizer
                        .REPORT_VERSION,
                actual =
                    firstReport.version,
            )

            assertEquals(
                expected =
                    3,
                actual =
                    firstReport.candidateCount,
            )

            assertEquals(
                expected =
                    3,
                actual =
                    firstReport.entries.size,
            )

            assertEquals(
                expected =
                    listOf(
                        NutritionFeatureSetOptimizer
                            .BASE_CANDIDATE_NAME,
                        NutritionFeatureSetOptimizer
                            .OPTIMIZED_CANDIDATE_NAME,
                        NutritionFeatureSetOptimizer
                            .OPTIMIZATION_BASELINE_CANDIDATE_NAME,
                    ),
                actual =
                    firstReport.entries
                        .sortedBy { entry ->
                            entry.featureCount
                        }
                        .map { entry ->
                            entry.name
                        },
            )

            assertEquals(
                expected =
                    listOf(
                        11,
                        15,
                        18,
                    ),
                actual =
                    firstReport.entries
                        .sortedBy { entry ->
                            entry.featureCount
                        }
                        .map { entry ->
                            entry.featureCount
                        },
            )

            val baseEntry =
                firstReport.entries
                    .single { entry ->
                        entry.name ==
                                NutritionFeatureSetOptimizer
                                    .BASE_CANDIDATE_NAME
                    }

            val optimizedEntry =
                firstReport.entries
                    .single { entry ->
                        entry.name ==
                                NutritionFeatureSetOptimizer
                                    .OPTIMIZED_CANDIDATE_NAME
                    }

            val baselineEntry =
                firstReport.entries
                    .single { entry ->
                        entry.name ==
                                NutritionFeatureSetOptimizer
                                    .OPTIMIZATION_BASELINE_CANDIDATE_NAME
                    }

            assertEquals(
                expected =
                    LocalNutritionMatcherFeatureContract
                        .BASE_FEATURE_NAMES,
                actual =
                    baseEntry.featureNames,
            )

            assertEquals(
                expected =
                    LocalNutritionMatcherFeatureContract
                        .OPTIMIZED_FEATURE_NAMES,
                actual =
                    optimizedEntry.featureNames,
            )

            assertEquals(
                expected =
                    LocalNutritionMatcherFeatureContract
                        .BASE_FEATURE_NAMES +
                            LocalNutritionMatcherFeatureContract
                                .OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES,
                actual =
                    baselineEntry.featureNames,
            )

            firstReport.entries
                .forEach { entry ->
                    assertTrue(
                        actual =
                            entry.decisionThreshold.isFinite(),
                    )

                    assertTrue(
                        actual =
                            entry.decisionThreshold in
                                    0.0..1.0,
                    )

                    assertTrue(
                        actual =
                            entry.testPrecision in
                                    0.0..1.0,
                    )

                    assertTrue(
                        actual =
                            entry.testRecall in
                                    0.0..1.0,
                    )

                    assertTrue(
                        actual =
                            entry.testF1 in
                                    0.0..1.0,
                    )

                    assertTrue(
                        actual =
                            entry.testBalancedAccuracy in
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

            val recommendedEntry =
                firstReport.entries
                    .singleOrNull { entry ->
                        entry.name ==
                                firstReport
                                    .recommendedCandidateName
                    }

            assertNotNull(
                actual =
                    recommendedEntry,
            )

            assertEquals(
                expected =
                    recommendedEntry.featureNames,
                actual =
                    firstReport
                        .recommendedFeatureNames,
            )

            assertTrue(
                actual =
                    File(
                        firstWorkingDirectory,
                        "base-11-model.json",
                    )
                        .isFile,
            )

            assertTrue(
                actual =
                    File(
                        firstWorkingDirectory,
                        "optimized-15-model.json",
                    )
                        .isFile,
            )

            assertTrue(
                actual =
                    File(
                        firstWorkingDirectory,
                        "optimization-baseline-18-model.json",
                    )
                        .isFile,
            )
        } finally {
            temporaryDirectory.deleteRecursively()
        }
    }

    private fun locateProjectDirectory(): File {

        val userDirectory =
            requireNotNull(
                System.getProperty(
                    "user.dir",
                ),
            ) {
                "System property user.dir is missing."
            }

        var currentDirectory =
            File(
                userDirectory,
            )
                .absoluteFile

        while (true) {
            if (
                File(
                    currentDirectory,
                    "data/generated/knowledge/training/" +
                            "nutrition.matcher-training-dataset.json",
                )
                    .isFile
            ) {
                return currentDirectory
            }

            currentDirectory =
                currentDirectory.parentFile
                    ?: error(
                        "Could not locate ShopMe project directory from: " +
                                userDirectory,
                    )
        }
    }
}