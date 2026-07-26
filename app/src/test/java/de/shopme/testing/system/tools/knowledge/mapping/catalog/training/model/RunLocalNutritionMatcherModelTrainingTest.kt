package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import com.google.gson.JsonParser
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureContract
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureExtractor
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureSubsetExtractor
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherModelContract
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherModelTrainer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RunLocalNutritionMatcherModelTrainingTest {

    @Test
    fun trainFirstLocalNutritionMatcherModel() {
        val projectRoot =
            File("..")

        val datasetFile =
            File(
                projectRoot,
                "data/generated/knowledge/" +
                        "training/" +
                        "nutrition.matcher-training-dataset.json",
            )

        val outputFile =
            File(
                projectRoot,
                "data/generated/knowledge/" +
                        "models/" +
                        "nutrition.local-matcher-model.json",
            )

        require(
            datasetFile.isFile,
        ) {
            "Nutrition matcher training dataset does not exist: " +
                    datasetFile.absolutePath
        }

        val selectedFeatureNames =
            LocalNutritionMatcherFeatureContract
                .ACTIVE_FEATURE_NAMES

        val featureExtractor =
            LocalNutritionMatcherFeatureSubsetExtractor(
                delegate =
                    LocalNutritionMatcherFeatureExtractor(),
                selectedFeatureNames =
                    selectedFeatureNames,
            )

        val trainer =
            LocalNutritionMatcherModelTrainer(
                featureExtractor =
                    featureExtractor,
                supportedFeatureNames =
                    selectedFeatureNames,
            )

        val result =
            trainer.train(
                datasetFile =
                    datasetFile,
                outputFile =
                    outputFile,
            )

        val model =
            result.model

        assertTrue(
            actual =
                outputFile.isFile,
            message =
                "Local nutrition matcher model was not written: " +
                        outputFile.absolutePath,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherModelContract
                    .CURRENT_VERSION,
            actual =
                model.version,
        )

        assertEquals(
            expected =
                "WEIGHTED_LOGISTIC_REGRESSION",
            actual =
                model.modelType,
        )

        assertEquals(
            expected =
                "NUTRITION_CATALOG_SERVER_MATCHER",
            actual =
                model.datasetType,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_FEATURE_COUNT,
            actual =
                model.featureNames.size,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_FEATURE_NAMES,
            actual =
                model.featureNames,
        )

        assertEquals(
            expected =
                featureExtractor.featureNames,
            actual =
                model.featureNames,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .BASE_FEATURE_NAMES,
            actual =
                model.featureNames.take(
                    LocalNutritionMatcherFeatureContract
                        .BASE_FEATURE_COUNT,
                ),
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_DOMAIN_FEATURE_NAMES,
            actual =
                model.featureNames.drop(
                    LocalNutritionMatcherFeatureContract
                        .BASE_FEATURE_COUNT,
                ),
        )

        assertTrue(
            actual =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_DOMAIN_FEATURE_NAMES
                    .all { featureName ->
                        featureName in model.featureNames
                    },
            message =
                "The trained model does not contain every active " +
                        "domain-mismatch feature.",
        )

        assertTrue(
            actual =
                model.featureNames.none { featureName ->
                    featureName in
                            LocalNutritionMatcherFeatureContract
                                .HARMFUL_DOMAIN_FEATURE_NAMES
                },
            message =
                "The trained model still contains a harmful " +
                        "domain-mismatch feature.",
        )

        assertEquals(
            expected =
                model.featureNames.size,
            actual =
                model.coefficients.size,
        )

        assertEquals(
            expected =
                model.featureNames.size,
            actual =
                model.featureMeans.size,
        )

        assertEquals(
            expected =
                model.featureNames.size,
            actual =
                model.featureStandardDeviations.size,
        )

        assertTrue(
            actual =
                model.training.exampleCount > 0,
            message =
                "The training dataset must contain examples.",
        )

        assertTrue(
            actual =
                model.training.trainingExampleCount > 0,
            message =
                "The training split must contain examples.",
        )

        assertTrue(
            actual =
                model.training.testExampleCount > 0,
            message =
                "The test split must contain examples.",
        )

        assertEquals(
            expected =
                model.training.exampleCount,
            actual =
                model.training.trainingExampleCount +
                        model.training.testExampleCount,
        )

        assertTrue(
            actual =
                model.metrics.training.averageLogLoss.isFinite(),
            message =
                "Training log loss must be finite.",
        )

        assertTrue(
            actual =
                model.metrics.test.averageLogLoss.isFinite(),
            message =
                "Test log loss must be finite.",
        )

        assertTrue(
            actual =
                model.metrics.training.f1 in 0.0..1.0,
            message =
                "Training F1 must be between zero and one.",
        )

        assertTrue(
            actual =
                model.metrics.test.f1 in 0.0..1.0,
            message =
                "Test F1 must be between zero and one.",
        )

        assertTrue(
            actual =
                model.metrics.training.balancedAccuracy in 0.0..1.0,
            message =
                "Training balanced accuracy must be between zero and one.",
        )

        assertTrue(
            actual =
                model.metrics.test.balancedAccuracy in 0.0..1.0,
            message =
                "Test balanced accuracy must be between zero and one.",
        )

        assertTrue(
            actual =
                "diagnostic_score_available" !in
                        model.featureNames,
            message =
                "Leakage feature diagnostic_score_available must not " +
                        "be part of the model.",
        )

        assertTrue(
            actual =
                "domain_feature_version" !in
                        model.featureNames,
            message =
                "Metadata field domain_feature_version must not be " +
                        "part of the model.",
        )

        assertTrue(
            actual =
                "domain_report_relationship_present" !in
                        model.featureNames,
            message =
                "Metadata field domain_report_relationship_present " +
                        "must not be part of the model.",
        )

        assertTrue(
            actual =
                model.diagnosticScoreImputationValue.isFinite(),
            message =
                "Diagnostic-score imputation value must be finite.",
        )

        assertTrue(
            actual =
                model.metrics.testByRole.isNotEmpty(),
            message =
                "Test metrics by training-example role must not be empty.",
        )

        val persisted =
            JsonParser
                .parseString(
                    outputFile.readText(),
                )
                .asJsonObject

        assertEquals(
            expected =
                LocalNutritionMatcherModelContract
                    .CURRENT_VERSION,
            actual =
                persisted["version"]
                    .asInt,
        )

        assertEquals(
            expected =
                "WEIGHTED_LOGISTIC_REGRESSION",
            actual =
                persisted["modelType"]
                    .asString,
        )

        assertEquals(
            expected =
                "NUTRITION_CATALOG_SERVER_MATCHER",
            actual =
                persisted["datasetType"]
                    .asString,
        )

        val persistedFeatureNames =
            persisted["featureNames"]
                .asJsonArray
                .map { element ->
                    element.asString
                }

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_FEATURE_COUNT,
            actual =
                persistedFeatureNames.size,
        )

        assertEquals(
            expected =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_FEATURE_NAMES,
            actual =
                persistedFeatureNames,
        )

        assertEquals(
            expected =
                model.featureNames,
            actual =
                persistedFeatureNames,
        )

        assertTrue(
            actual =
                persistedFeatureNames.none { featureName ->
                    featureName in
                            LocalNutritionMatcherFeatureContract
                                .HARMFUL_DOMAIN_FEATURE_NAMES
                },
            message =
                "The persisted model still contains a harmful " +
                        "domain-mismatch feature.",
        )

        assertEquals(
            expected =
                model.featureNames.size,
            actual =
                persisted["coefficients"]
                    .asJsonArray
                    .size(),
        )

        assertEquals(
            expected =
                model.featureNames.size,
            actual =
                persisted["featureMeans"]
                    .asJsonArray
                    .size(),
        )

        assertEquals(
            expected =
                model.featureNames.size,
            actual =
                persisted["featureStandardDeviations"]
                    .asJsonArray
                    .size(),
        )

        printSummary(
            modelFile =
                outputFile,
            featureCount =
                model.featureNames.size,
            activeDomainFeatureCount =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_DOMAIN_FEATURE_NAMES
                    .size,
            removedHarmfulFeatureCount =
                LocalNutritionMatcherFeatureContract
                    .HARMFUL_DOMAIN_FEATURE_NAMES
                    .size,
            trainingExampleCount =
                model.training.trainingExampleCount,
            testExampleCount =
                model.training.testExampleCount,
            trainingPrecision =
                model.metrics.training.precision,
            trainingRecall =
                model.metrics.training.recall,
            trainingF1 =
                model.metrics.training.f1,
            trainingBalancedAccuracy =
                model.metrics.training.balancedAccuracy,
            testPrecision =
                model.metrics.test.precision,
            testRecall =
                model.metrics.test.recall,
            testF1 =
                model.metrics.test.f1,
            testBalancedAccuracy =
                model.metrics.test.balancedAccuracy,
            diagnosticScoreImputationValue =
                model.diagnosticScoreImputationValue,
        )

        model.metrics.testByRole.forEach { roleMetrics ->
            println(
                "Test role=${roleMetrics.role}, " +
                        "count=${roleMetrics.exampleCount}, " +
                        "precision=${roleMetrics.precision}, " +
                        "recall=${roleMetrics.recall}, " +
                        "falsePositiveRate=${roleMetrics.falsePositiveRate}",
            )
        }
    }

    private fun printSummary(
        modelFile: File,
        featureCount: Int,
        activeDomainFeatureCount: Int,
        removedHarmfulFeatureCount: Int,
        trainingExampleCount: Int,
        testExampleCount: Int,
        trainingPrecision: Double,
        trainingRecall: Double,
        trainingF1: Double,
        trainingBalancedAccuracy: Double,
        testPrecision: Double,
        testRecall: Double,
        testF1: Double,
        testBalancedAccuracy: Double,
        diagnosticScoreImputationValue: Double,
    ) {
        println()
        println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        )
        println(
            "LOCAL NUTRITION MATCHER MODEL TRAINING",
        )
        println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        )
        println()
        println(
            "Feature count            : $featureCount",
        )
        println(
            "Active domain features   : $activeDomainFeatureCount",
        )
        println(
            "Removed harmful features : $removedHarmfulFeatureCount",
        )
        println(
            "Training examples        : $trainingExampleCount",
        )
        println(
            "Test examples            : $testExampleCount",
        )
        println()
        println(
            "Training precision       : $trainingPrecision",
        )
        println(
            "Training recall          : $trainingRecall",
        )
        println(
            "Training F1              : $trainingF1",
        )
        println(
            "Training balanced acc.   : $trainingBalancedAccuracy",
        )
        println()
        println(
            "Test precision           : $testPrecision",
        )
        println(
            "Test recall              : $testRecall",
        )
        println(
            "Test F1                  : $testF1",
        )
        println(
            "Test balanced accuracy   : $testBalancedAccuracy",
        )
        println()
        println(
            "Diagnostic imputation    : $diagnosticScoreImputationValue",
        )
        println(
            "Model file               : ${modelFile.path}",
        )
        println()
    }
}