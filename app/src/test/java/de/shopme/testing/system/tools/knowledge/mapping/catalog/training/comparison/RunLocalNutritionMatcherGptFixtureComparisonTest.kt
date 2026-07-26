package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.comparison

import com.google.gson.JsonParser
import de.shopme.tools.knowledge.mapping.catalog.training.comparison.LocalNutritionMatcherGptFixtureComparator
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunLocalNutritionMatcherGptFixtureComparisonTest {

    @Test
    fun compareLocalNutritionMatcherAgainstGpt55Fixtures() {

        val projectRoot =
            File("..")

        val datasetFile =
            File(
                projectRoot,
                "data/generated/knowledge/" +
                        "training/" +
                        "nutrition.matcher-training-dataset.json"
            )

        val modelFile =
            File(
                projectRoot,
                "data/generated/knowledge/" +
                        "models/" +
                        "nutrition.local-matcher-model.json"
            )

        val outputFile =
            File(
                projectRoot,
                "data/generated/knowledge/" +
                        "reports/" +
                        "nutrition.local-vs-gpt55-comparison.json"
            )

        require(datasetFile.isFile) {
            "Nutrition matcher training dataset does not exist: " +
                    datasetFile.absolutePath
        }

        require(modelFile.isFile) {
            "Local nutrition matcher model does not exist: " +
                    modelFile.absolutePath
        }

        val result =
            LocalNutritionMatcherGptFixtureComparator()
                .run(
                    datasetFile =
                        datasetFile,
                    modelFile =
                        modelFile,
                    outputFile =
                        outputFile
                )

        val comparison =
            result.comparison

        assertTrue(
            outputFile.isFile
        )

        assertEquals(
            expected =
                "LOCAL_NUTRITION_MATCHER_VS_GPT_5_5",
            actual =
                comparison.comparisonType
        )

        assertTrue(
            comparison.testCatalogKeyCount > 0
        )

        assertTrue(
            comparison.testExampleCount > 0
        )

        assertTrue(
            comparison.positiveFixtureCount > 0
        )

        assertTrue(
            comparison.negativeFixtureCount > 0
        )

        assertEquals(
            expected =
                comparison.testExampleCount,
            actual =
                comparison.positiveFixtureCount +
                        comparison.negativeFixtureCount
        )

        val thresholdValues =
            comparison.thresholds
                .map {
                    it.threshold
                }

        assertEquals(
            expected =
                thresholdValues
                    .distinct()
                    .sorted(),
            actual =
                thresholdValues
        )

        assertTrue(
            thresholdValues.containsAll(
                listOf(
                    0.50,
                    0.60,
                    0.70,
                    0.80,
                    0.90,
                    0.95,
                    0.98,
                    0.99
                )
            )
        )

        assertTrue(
            thresholdValues.contains(
                comparison.productionDecisionThreshold
            )
        )

        assertEquals(
            expected =
                comparison.productionDecisionThreshold,
            actual =
                comparison.productionThreshold.threshold
        )

        assertTrue(
            comparison.thresholds.all {
                it.exampleCount ==
                        comparison.testExampleCount
            }
        )

        assertTrue(
            comparison.thresholds.all {
                it.precision in 0.0..1.0 &&
                        it.recall in 0.0..1.0 &&
                        it.f1 in 0.0..1.0
            }
        )

        assertTrue(
            comparison.topOne.accuracy in
                    0.0..1.0
        )

        comparison.recommendedThreshold
            ?.let { recommended ->

                assertTrue(
                    recommended.precision >=
                            recommended.minimumPrecision
                )
            }

        val persisted =
            JsonParser.parseString(
                outputFile.readText()
            )
                .asJsonObject

        assertEquals(
            expected = 1,
            actual =
                persisted["version"]
                    .asInt
        )

        assertEquals(
            expected =
                "LOCAL_NUTRITION_MATCHER_VS_GPT_5_5",
            actual =
                persisted["comparisonType"]
                    .asString
        )

        println()
        println(
            "GPT-5.5 comparison test catalog keys=" +
                    comparison.testCatalogKeyCount
        )
        println(
            "GPT-5.5 comparison examples=" +
                    comparison.testExampleCount
        )
        println(
            "GPT-5.5 positive fixtures=" +
                    comparison.positiveFixtureCount
        )
        println(
            "GPT-5.5 negative fixtures=" +
                    comparison.negativeFixtureCount
        )
        println(
            "Local matcher top-1 accuracy=" +
                    comparison.topOne.accuracy
        )
        println(
            "Local matcher mean positive rank=" +
                    comparison.topOne.meanPositiveRank
        )

        comparison.recommendedThreshold
            ?.let {
                println(
                    "Recommended auto-accept threshold=" +
                            it.threshold
                )
                println(
                    "Recommended auto-accept precision=" +
                            it.precision
                )
                println(
                    "Recommended auto-accept recall=" +
                            it.recall
                )
            }
            ?: println(
                "No auto-accept threshold reached " +
                        "minimum precision."
            )

        println(
            "Comparison report=" +
                    outputFile.path
        )
    }
}