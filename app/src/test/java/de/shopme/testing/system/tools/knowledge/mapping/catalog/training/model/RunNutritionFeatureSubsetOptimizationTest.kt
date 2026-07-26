package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionFeatureOptimizationReportWriter
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionFeatureSubsetOptimizer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class RunNutritionFeatureSubsetOptimizationTest {

    @Test
    fun optimizeLocalNutritionMatcherFeatureSubset() {

        val datasetFile =
            File(
                "../data/generated/knowledge/training/" +
                        "nutrition.matcher-training-dataset.json",
            )

        val workingDirectory =
            File(
                "../data/generated/knowledge/models/" +
                        "nutrition-feature-optimization",
            )

        val outputFile =
            File(
                "../data/generated/knowledge/reports/" +
                        "nutrition.local-matcher-feature-optimization.json",
            )

        val report =
            NutritionFeatureSubsetOptimizer()
                .optimize(
                    datasetFile =
                        datasetFile,
                    workingDirectory =
                        workingDirectory,
                )

        NutritionFeatureOptimizationReportWriter()
            .write(
                report =
                    report,
                outputFile =
                    outputFile,
            )

        assertTrue(
            actual =
                outputFile.isFile,
        )

        println()
        println("Nutrition feature optimization")
        println(
            "Baseline feature count=" +
                    report.baselineFeatureCount,
        )
        println(
            "Baseline test precision=" +
                    report.baselineTestPrecision,
        )
        println(
            "Baseline test recall=" +
                    report.baselineTestRecall,
        )
        println(
            "Baseline test F1=" +
                    report.baselineTestF1,
        )
        println(
            "Baseline test balanced accuracy=" +
                    report.baselineTestBalancedAccuracy,
        )
        println(
            "Required features=" +
                    report.requiredFeatureNames.size,
        )
        println(
            "Neutral features=" +
                    report.neutralFeatureNames.size,
        )
        println(
            "Harmful features=" +
                    report.harmfulFeatureNames.size,
        )
        println(
            "Recommended feature count=" +
                    report.recommendedFeatureNames.size,
        )
        println(
            "Harmful feature names=" +
                    report.harmfulFeatureNames,
        )
        println(
            "Report=" +
                    outputFile.path,
        )
    }
}