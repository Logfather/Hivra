package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureContract
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionFeatureOptimizationEvaluationWriter
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionFeatureOptimizationEvaluator
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionFeatureOptimizationReport
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunNutritionFeatureOptimizationEvaluationTest {

    @Test
    fun evaluateNutritionFeatureOptimizationResults() {

        require(
            OPTIMIZATION_REPORT_FILE.isFile,
        ) {
            "Nutrition feature optimization report does not exist: " +
                    OPTIMIZATION_REPORT_FILE.path
        }

        val report =
            GSON.fromJson(
                OPTIMIZATION_REPORT_FILE.readText(),
                NutritionFeatureOptimizationReport::class.java,
            )

        val evaluation =
            NutritionFeatureOptimizationEvaluator()
                .evaluate(
                    report =
                        report,
                )

        NutritionFeatureOptimizationEvaluationWriter()
            .apply {
                writeJson(
                    evaluation =
                        evaluation,
                    outputFile =
                        JSON_OUTPUT_FILE,
                )

                writeMarkdown(
                    evaluation =
                        evaluation,
                    outputFile =
                        MARKDOWN_OUTPUT_FILE,
                )
            }

        assertTrue(
            actual =
                JSON_OUTPUT_FILE.isFile,
        )

        assertTrue(
            actual =
                MARKDOWN_OUTPUT_FILE.isFile,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_FEATURE_COUNT,
            actual =
                evaluation.baseline.featureCount,
        )

        assertEquals(
            expected =
                evaluation.baseline.featureCount -
                        evaluation.removedFeatureCount,
            actual =
                evaluation.recommendedFeatureCount,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .BASE_FEATURE_NAMES,
            actual =
                evaluation.recommendedFeatureNames.take(
                    LocalNutritionMatcherFeatureContract
                        .BASE_FEATURE_COUNT,
                ),
        )

        println()
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println("NUTRITION FEATURE OPTIMIZATION EVALUATION")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println(
            "Baseline features           : " +
                    evaluation.baseline.featureCount,
        )
        println(
            "Baseline test precision     : " +
                    evaluation.baseline.testPrecision,
        )
        println(
            "Baseline test recall        : " +
                    evaluation.baseline.testRecall,
        )
        println(
            "Baseline test F1            : " +
                    evaluation.baseline.testF1,
        )
        println(
            "Baseline balanced accuracy  : " +
                    evaluation.baseline.testBalancedAccuracy,
        )
        println()
        println(
            "Required features           : " +
                    evaluation.classificationCounts.required,
        )
        println(
            "Neutral features            : " +
                    evaluation.classificationCounts.neutral,
        )
        println(
            "Harmful features            : " +
                    evaluation.classificationCounts.harmful,
        )
        println(
            "Recommended feature count   : " +
                    evaluation.recommendedFeatureCount,
        )
        println(
            "Removed feature count       : " +
                    evaluation.removedFeatureCount,
        )
        println(
            "Removed feature names       : " +
                    evaluation.removedFeatureNames,
        )
        println()
        println(
            "JSON evaluation             : " +
                    JSON_OUTPUT_FILE.path,
        )
        println(
            "Markdown evaluation         : " +
                    MARKDOWN_OUTPUT_FILE.path,
        )
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        println()
    }

    private companion object {

        val OPTIMIZATION_REPORT_FILE =
            File(
                "../data/generated/knowledge/reports/" +
                        "nutrition.local-matcher-feature-optimization.json",
            )

        val JSON_OUTPUT_FILE =
            File(
                "../data/generated/knowledge/reports/" +
                        "nutrition.local-matcher-feature-" +
                        "optimization-evaluation.json",
            )

        val MARKDOWN_OUTPUT_FILE =
            File(
                "../data/generated/knowledge/reports/" +
                        "nutrition.local-matcher-feature-" +
                        "optimization-evaluation.md",
            )

        val GSON: Gson =
            GsonBuilder()
                .disableHtmlEscaping()
                .create()
    }
}