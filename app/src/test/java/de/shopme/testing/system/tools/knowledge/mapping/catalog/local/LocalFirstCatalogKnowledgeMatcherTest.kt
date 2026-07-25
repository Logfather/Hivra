package de.shopme.testing.system.tools.knowledge.mapping.catalog.local

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchCandidate
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchDecision
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchDecisionType
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatchRequest
import de.shopme.tools.knowledge.mapping.catalog.CatalogKnowledgeMatcher
import de.shopme.tools.knowledge.mapping.catalog.local.ConservativeLocalNutritionMatcher
import de.shopme.tools.knowledge.mapping.catalog.local.LocalFirstCatalogKnowledgeMatcher
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

class LocalFirstCatalogKnowledgeMatcherTest {

    @Test
    fun useLocalDecisionAboveConservativeThreshold() {

        val directory =
            createTempDirectory(
                prefix =
                    "local-first-catalog-matcher-"
            )
                .toFile()

        try {
            var fallbackCalled =
                false

            val matcher =
                LocalFirstCatalogKnowledgeMatcher(
                    localMatcher =
                        createLocalMatcher(
                            directory =
                                directory
                        ),
                    fallbackMatcher =
                        object : CatalogKnowledgeMatcher {

                            override fun match(
                                request: CatalogKnowledgeMatchRequest
                            ): CatalogKnowledgeMatchDecision =
                                CatalogKnowledgeMatchDecision(
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
                                    .also {
                                        fallbackCalled = true
                                    }
                        }
                )

            val decision =
                matcher.match(
                    request =
                        request(
                            firstScore =
                                1.0,
                            secondScore =
                                0.0
                        )
                )

            assertFalse(
                fallbackCalled
            )

            assertEquals(
                expected =
                    CatalogKnowledgeMatchDecisionType.MATCH,
                actual =
                    decision.type
            )

            assertEquals(
                expected =
                    "apple raw",
                actual =
                    decision.selectedServerKey
            )

            assertTrue(
                decision.confidence >=
                        0.98
            )

            assertTrue(
                decision.reason.contains(
                    "Accepted by local nutrition matcher"
                )
            )

        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun callFallbackMatcherBelowConservativeThreshold() {

        val directory =
            createTempDirectory(
                prefix =
                    "local-first-gpt-fallback-"
            )
                .toFile()

        try {
            var fallbackCallCount =
                0

            val expectedDecision =
                CatalogKnowledgeMatchDecision(
                    catalogKey =
                        "apple",
                    serverArtifact =
                        "nutrition.json",
                    type =
                        CatalogKnowledgeMatchDecisionType.MATCH,
                    selectedServerKey =
                        "apple pie",
                    confidence =
                        0.91,
                    reason =
                        "GPT-5.5 selected the candidate."
                )

            val matcher =
                LocalFirstCatalogKnowledgeMatcher(
                    localMatcher =
                        createLocalMatcher(
                            directory =
                                directory
                        ),
                    fallbackMatcher =
                        object : CatalogKnowledgeMatcher {

                            override fun match(
                                request: CatalogKnowledgeMatchRequest
                            ): CatalogKnowledgeMatchDecision {

                                fallbackCallCount++

                                return expectedDecision
                            }
                        }
                )

            val actualDecision =
                matcher.match(
                    request =
                        request(
                            firstScore =
                                0.5,
                            secondScore =
                                0.4
                        )
                )

            assertEquals(
                expected = 1,
                actual =
                    fallbackCallCount
            )

            assertEquals(
                expected =
                    expectedDecision,
                actual =
                    actualDecision
            )

        } finally {
            directory.deleteRecursively()
        }
    }

    private fun request(
        firstScore: Double,
        secondScore: Double
    ): CatalogKnowledgeMatchRequest {

        return CatalogKnowledgeMatchRequest(
            catalogKey =
                "apple",
            serverArtifact =
                "nutrition.json",
            candidates =
                listOf(
                    CatalogKnowledgeMatchCandidate(
                        serverKey =
                            "apple raw",
                        diagnosticScore =
                            firstScore,
                        sharedTokens =
                            listOf(
                                "apple"
                            )
                    ),
                    CatalogKnowledgeMatchCandidate(
                        serverKey =
                            "apple pie",
                        diagnosticScore =
                            secondScore,
                        sharedTokens =
                            listOf(
                                "apple"
                            )
                    )
                )
        )
    }

    private fun createLocalMatcher(
        directory: File
    ): ConservativeLocalNutritionMatcher {

        val modelFile =
            File(
                directory,
                "nutrition.local-matcher-model.json"
            )

        val featureNames =
            LocalNutritionMatcherFeatureContract
                .ACTIVE_FEATURE_NAMES

        val emptyMetrics =
            LocalNutritionMatcherClassificationMetrics(
                exampleCount =
                    0,
                positiveCount =
                    0,
                negativeCount =
                    0,
                truePositive =
                    0,
                falsePositive =
                    0,
                trueNegative =
                    0,
                falseNegative =
                    0,
                accuracy =
                    0.0,
                precision =
                    0.0,
                recall =
                    0.0,
                f1 =
                    0.0,
                balancedAccuracy =
                    0.0,
                averageLogLoss =
                    0.0
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
                    0.5,
                training =
                    LocalNutritionMatcherTrainingMetadata(
                        datasetFile =
                            "fixture.json",
                        datasetVersion =
                            1,
                        exampleCount =
                            0,
                        trainingExampleCount =
                            0,
                        testExampleCount =
                            0,
                        trainingCatalogKeyCount =
                            0,
                        testCatalogKeyCount =
                            0,
                        positiveClassWeight =
                            1.0,
                        negativeClassWeight =
                            1.0,
                        learningRate =
                            0.05,
                        iterationCount =
                            1,
                        l2Regularization =
                            0.0,
                        splitModulo =
                            10,
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