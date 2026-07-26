package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionFeatureSetOptimizationReportWriter
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionFeatureSetOptimizer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunNutritionFeatureSetOptimizationTest {

    @Test
    fun runNutritionFeatureSetOptimization() {

        val projectDirectory =
            locateProjectDirectory()

        val datasetFile =
            File(
                projectDirectory,
                "data/generated/knowledge/training/" +
                        "nutrition.matcher-training-dataset.json",
            )

        val modelDirectory =
            File(
                projectDirectory,
                "data/generated/knowledge/models/comparison/" +
                        "feature-set-optimization",
            )

        val reportFile =
            File(
                projectDirectory,
                "data/generated/knowledge/reports/" +
                        "nutrition.local-matcher-feature-set-" +
                        "optimization.json",
            )

        require(
            datasetFile.isFile,
        ) {
            "Nutrition matcher training dataset does not exist: " +
                    datasetFile.absolutePath
        }

        val report =
            NutritionFeatureSetOptimizer()
                .optimize(
                    datasetFile =
                        datasetFile,
                    workingDirectory =
                        modelDirectory,
                )

        NutritionFeatureSetOptimizationReportWriter()
            .write(
                report =
                    report,
                outputFile =
                    reportFile,
            )

        assertEquals(
            expected =
                3,
            actual =
                report.candidateCount,
        )

        assertEquals(
            expected =
                listOf(
                    11,
                    15,
                    18,
                ),
            actual =
                report.entries
                    .sortedBy { entry ->
                        entry.featureCount
                    }
                    .map { entry ->
                        entry.featureCount
                    },
        )

        assertTrue(
            actual =
                reportFile.isFile,
        )

        assertTrue(
            actual =
                File(
                    modelDirectory,
                    "base-11-model.json",
                )
                    .isFile,
        )

        assertTrue(
            actual =
                File(
                    modelDirectory,
                    "optimized-15-model.json",
                )
                    .isFile,
        )

        assertTrue(
            actual =
                File(
                    modelDirectory,
                    "optimization-baseline-18-model.json",
                )
                    .isFile,
        )

        println()
        println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        )
        println(
            "NUTRITION FEATURE-SET OPTIMIZATION",
        )
        println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        )

        report.entries
            .sortedBy { entry ->
                entry.featureCount
            }
            .forEach { entry ->
                println(
                    entry.name.padEnd(
                        length =
                            26,
                    ) +
                            "features=" +
                            entry.featureCount
                                .toString()
                                .padEnd(
                                    length =
                                        3,
                                ) +
                            " precision=" +
                            format(
                                value =
                                    entry.testPrecision,
                            ) +
                            " recall=" +
                            format(
                                value =
                                    entry.testRecall,
                            ) +
                            " f1=" +
                            format(
                                value =
                                    entry.testF1,
                            ) +
                            " balanced=" +
                            format(
                                value =
                                    entry.testBalancedAccuracy,
                            ) +
                            " fp=" +
                            entry.testFalsePositiveCount +
                            " policy=" +
                            entry.thresholdPolicySatisfied,
                )
            }

        println()
        println(
            "Recommended              : " +
                    report.recommendedCandidateName,
        )
        println(
            "Recommended features     : " +
                    report.recommendedFeatureNames.size,
        )
        println(
            "Report                   : " +
                    reportFile.absolutePath,
        )
        println(
            "Models                   : " +
                    modelDirectory.absolutePath,
        )
        println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        )
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

    private fun format(
        value: Double,
    ): String {

        return "%.4f".format(
            locale =
                java.util.Locale.ROOT,
            value,
        )
    }
}