package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import com.google.gson.JsonParser
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureExtractor
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

        require(datasetFile.isFile) {
            "Nutrition matcher training dataset does not exist: " +
                    datasetFile.absolutePath
        }

        val featureExtractor =
            LocalNutritionMatcherFeatureExtractor()

        val result =
            LocalNutritionMatcherModelTrainer(
                featureExtractor =
                    featureExtractor,
            )
                .train(
                    datasetFile =
                        datasetFile,
                    outputFile =
                        outputFile,
                )

        val model =
            result.model

        assertTrue(
            outputFile.isFile,
        )

        assertEquals(
            expected = 2,
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
                featureExtractor.featureNames.size,
            actual =
                model.featureNames.size,
        )

        assertEquals(
            expected =
                featureExtractor.featureNames,
            actual =
                model.featureNames,
        )

        assertTrue(
            LocalNutritionMatcherFeatureExtractor
                .DOMAIN_MISMATCH_FEATURE_NAMES
                .all {
                    it in model.featureNames
                },
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

        assertEquals(
            expected = 4256,
            actual =
                model.training.exampleCount,
        )

        assertEquals(
            expected = 3441,
            actual =
                model.training.trainingExampleCount,
        )

        assertEquals(
            expected = 815,
            actual =
                model.training.testExampleCount,
        )

        assertEquals(
            expected =
                model.training.exampleCount,
            actual =
                model.training.trainingExampleCount +
                        model.training.testExampleCount,
        )

        assertTrue(
            model.training.trainingExampleCount > 0,
        )

        assertTrue(
            model.training.testExampleCount > 0,
        )

        assertTrue(
            model.metrics.training.averageLogLoss
                .isFinite(),
        )

        assertTrue(
            model.metrics.test.averageLogLoss
                .isFinite(),
        )

        assertTrue(
            model.metrics.training.f1 in
                    0.0..1.0,
        )

        assertEquals(
            expected =
                featureExtractor.featureNames,
            actual =
                model.featureNames,
        )

        assertEquals(
            expected =
                featureExtractor.featureNames.size,
            actual =
                model.coefficients.size,
        )

        assertTrue(
            model.metrics.test.f1 in
                    0.0..1.0,
        )

        assertTrue(
            "diagnostic_score_available" !in
                    model.featureNames,
        )

        assertTrue(
            "domain_feature_version" !in
                    model.featureNames,
        )

        assertTrue(
            "domain_report_relationship_present" !in
                    model.featureNames,
        )

        assertTrue(
            model.diagnosticScoreImputationValue
                .isFinite(),
        )

        assertTrue(
            model.metrics.testByRole.isNotEmpty(),
        )

        val persisted =
            JsonParser.parseString(
                outputFile.readText(),
            )
                .asJsonObject

        assertEquals(
            expected = 2,
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
                featureExtractor.featureNames.size,
            actual =
                persisted["featureNames"]
                    .asJsonArray
                    .size(),
        )

        println()
        println(
            "Local matcher training examples=" +
                    model.training.trainingExampleCount,
        )
        println(
            "Local matcher test examples=" +
                    model.training.testExampleCount,
        )
        println(
            "Local matcher train precision=" +
                    model.metrics.training.precision,
        )
        println(
            "Local matcher train recall=" +
                    model.metrics.training.recall,
        )
        println(
            "Local matcher train F1=" +
                    model.metrics.training.f1,
        )
        println(
            "Local matcher test precision=" +
                    model.metrics.test.precision,
        )
        println(
            "Local matcher test recall=" +
                    model.metrics.test.recall,
        )
        println(
            "Local matcher test F1=" +
                    model.metrics.test.f1,
        )
        println(
            "Local matcher model=" +
                    outputFile.path,
        )
        println(
            "Diagnostic score imputation=" +
                    model.diagnosticScoreImputationValue,
        )

        model.metrics.testByRole.forEach {
            println(
                "Test role=${it.role}, " +
                        "count=${it.exampleCount}, " +
                        "precision=${it.precision}, " +
                        "recall=${it.recall}, " +
                        "falsePositiveRate=${it.falsePositiveRate}",
            )
        }
    }
}