package de.shopme.testing.system.tools.knowledge.mapping.catalog.runner

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchDecision
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchDecisionType
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchRequest
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatcher
import de.shopme.tools.knowledge.mapping.catalog.local.ConservativeLocalNutritionMatcher
import de.shopme.tools.knowledge.mapping.catalog.local.LocalFirstCatalogKnowledgeMatcher
import de.shopme.tools.knowledge.mapping.catalog.runner.RunOpenAINutritionKnowledgeMatcher
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherClassificationMetrics
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureContract
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherModel
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherModelContract
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherModelMetrics
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherRoleMetrics
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherThresholdOptimizationMetadata
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherTrainingMetadata
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RunLocalFirstNutritionKnowledgeMatcherTest {

    @Test
    fun persistLocalDecisionWithoutCallingFallback() {

        val directory =
            createTempDirectory(
                prefix =
                    "run-local-first-nutrition-"
            )
                .toFile()

        try {
            val requestFile =
                File(
                    directory,
                    "nutrition.match-requests.json"
                )
                    .apply {
                        writeText(
                            """
                            {
                              "version": 1,
                              "requests": [
                                {
                                  "catalogKey": "apple",
                                  "serverArtifact": "nutrition.json",
                                  "candidates": [
                                    {
                                      "serverKey": "apple raw",
                                      "diagnosticScore": 1.0,
                                      "sharedTokens": [
                                        "apple"
                                      ]
                                    },
                                    {
                                      "serverKey": "apple pie",
                                      "diagnosticScore": 0.0,
                                      "sharedTokens": [
                                        "apple"
                                      ]
                                    }
                                  ]
                                }
                              ]
                            }
                            """.trimIndent()
                        )
                    }

            val decisionFile =
                File(
                    directory,
                    "nutrition.match-decisions.json"
                )

            val errorFile =
                File(
                    directory,
                    "nutrition.match-errors.json"
                )

            var fallbackCalled =
                false

            val fallbackMatcher =
                object : CatalogKnowledgeMatcher {

                    override fun match(
                        request:
                        CatalogKnowledgeMatchRequest
                    ): CatalogKnowledgeMatchDecision {

                        fallbackCalled =
                            true

                        return CatalogKnowledgeMatchDecision(
                            catalogKey =
                                request.catalogKey,
                            serverArtifact =
                                request.serverArtifact,
                            type =
                                CatalogKnowledgeMatchDecisionType
                                    .NO_MATCH,
                            selectedServerKey =
                                null,
                            confidence =
                                1.0,
                            reason =
                                "Fallback."
                        )
                    }
                }

            val matcher =
                LocalFirstCatalogKnowledgeMatcher(
                    localMatcher =
                        createLocalMatcher(
                            directory =
                                directory
                        ),
                    fallbackMatcher =
                        fallbackMatcher
                )

            val result =
                RunOpenAINutritionKnowledgeMatcher(
                    matcher =
                        matcher,
                    requestFile =
                        requestFile,
                    decisionFile =
                        decisionFile,
                    errorFile =
                        errorFile,
                    printLine =
                        {}
                )
                    .run()

            assertFalse(
                fallbackCalled
            )

            assertEquals(
                expected = 1,
                actual =
                    result.successfulDecisions
            )

            assertEquals(
                expected = 1,
                actual =
                    result.matchCount
            )

            assertEquals(
                expected = 0,
                actual =
                    result.noMatchCount
            )

            assertTrue(
                decisionFile.isFile
            )

            val decision =
                JsonParser.parseString(
                    decisionFile.readText()
                )
                    .asJsonObject["decisions"]
                    .asJsonArray
                    .single()
                    .asJsonObject

            assertEquals(
                expected =
                    "MATCH",
                actual =
                    decision["type"]
                        .asString
            )

            assertEquals(
                expected =
                    "apple raw",
                actual =
                    decision["selectedServerKey"]
                        .asString
            )

            assertTrue(
                decision["confidence"]
                    .asDouble >=
                        0.98
            )

            assertTrue(
                decision["reason"]
                    .asString
                    .contains(
                        "local nutrition matcher"
                    )
            )

        } finally {
            directory.deleteRecursively()
        }
    }

    private fun createLocalMatcher(
        directory: File
    ): ConservativeLocalNutritionMatcher {

        val modelFile =
            File(
                directory,
                "model.json"
            )

        val featureNames =
            LocalNutritionMatcherFeatureContract
                .ACTIVE_FEATURE_NAMES

        val emptyMetrics =
            LocalNutritionMatcherClassificationMetrics(
                exampleCount = 0,
                positiveCount = 0,
                negativeCount = 0,
                truePositive = 0,
                falsePositive = 0,
                trueNegative = 0,
                falseNegative = 0,
                accuracy = 0.0,
                precision = 0.0,
                recall = 0.0,
                f1 = 0.0,
                balancedAccuracy = 0.0,
                averageLogLoss = 0.0
            )

        val model =
            LocalNutritionMatcherModel(
                version =
                    LocalNutritionMatcherModelContract.CURRENT_VERSION,
                featureNames =
                    featureNames,
                featureMeans =
                    List(
                        featureNames.size
                    ) {
                        0.0
                    },
                featureStandardDeviations =
                    List(
                        featureNames.size
                    ) {
                        1.0
                    },
                coefficients =
                    listOf(
                        10.0
                    ) +
                            List(
                                featureNames.size - 1
                            ) {
                                0.0
                            },
                intercept =
                    -5.0,
                decisionThreshold =
                    0.5,
                decisionThresholdOptimization =
                    LocalNutritionMatcherThresholdOptimizationMetadata(
                        calibrationExampleCount =
                            1,
                        minimumPrecision =
                            0.0,
                        maximumFalsePositiveRate =
                            1.0,
                        minimumPredictedPositiveCount =
                            1,
                        policySatisfied =
                            false,
                        evaluatedThresholdCount =
                            1,
                        selectedPrecision =
                            0.0,
                        selectedRecall =
                            0.0,
                        selectedFalsePositiveRate =
                            0.0,
                        selectedF1 =
                            0.0,
                        selectedTruePositiveCount =
                            0,
                        selectedFalsePositiveCount =
                            0,
                        selectedTrueNegativeCount =
                            1,
                        selectedFalseNegativeCount =
                            0,
                    ),
                diagnosticScoreImputationValue =
                    0.0,
                training =
                    LocalNutritionMatcherTrainingMetadata(
                        datasetFile = "fixture.json",
                        datasetVersion = 1,
                        exampleCount = 0,
                        trainingExampleCount = 0,
                        testExampleCount = 0,
                        trainingCatalogKeyCount = 0,
                        testCatalogKeyCount = 0,
                        positiveClassWeight = 1.0,
                        negativeClassWeight = 1.0,
                        learningRate = 0.05,
                        iterationCount = 1,
                        l2Regularization = 0.0,
                        splitModulo = 10,
                        testBuckets =
                            listOf(
                                0,
                                1
                            )
                    ),
                metrics =
                    LocalNutritionMatcherModelMetrics(
                        training =
                            emptyMetrics,
                        test =
                            emptyMetrics,
                        trainingByRole =
                            emptyList<
                                    LocalNutritionMatcherRoleMetrics
                                    >(),
                        testByRole =
                            emptyList<
                                    LocalNutritionMatcherRoleMetrics
                                    >()
                    )
            )

        val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()

        modelFile.writeText(
            gson.toJson(model) + "\n"
        )

        return ConservativeLocalNutritionMatcher
            .fromModelFile(
                modelFile =
                    modelFile,
                autoAcceptThreshold =
                    0.98
            )
    }
}