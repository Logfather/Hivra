package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.comparison

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingDataset
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingDatasetSummary
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingExample
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingExampleRole
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingLabel
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingProvenance
import de.shopme.tools.knowledge.mapping.catalog.training.comparison.LocalNutritionMatcherGptFixtureComparator
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherClassificationMetrics
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureContract
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherModel
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherModelContract
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherModelMetrics
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherRoleMetrics
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherThresholdOptimizationMetadata
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherTrainingMetadata
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalNutritionMatcherGptFixtureComparatorTest {

    @Test
    fun compareFixturesDeterministically() {

        val directory =
            createTempDirectory(
                prefix =
                    "local-vs-gpt55-comparison-"
            )
                .toFile()

        try {
            val datasetFile =
                File(
                    directory,
                    "dataset.json"
                )

            val modelFile =
                File(
                    directory,
                    "model.json"
                )

            val outputFile =
                File(
                    directory,
                    "comparison.json"
                )

            val examples =
                createFixtureExamples()

            writeJson(
                file =
                    datasetFile,
                value =
                    NutritionMatcherTrainingDataset(
                        summary =
                            NutritionMatcherTrainingDatasetSummary(
                                sourceCatalogKeyCount =
                                    examples
                                        .map {
                                            it.catalogKey
                                        }
                                        .distinct()
                                        .size,
                                exampleCount =
                                    examples.size,
                                positiveCount =
                                    examples.count {
                                        it.label ==
                                                NutritionMatcherTrainingLabel
                                                    .POSITIVE
                                    },
                                negativeCount =
                                    examples.count {
                                        it.label ==
                                                NutritionMatcherTrainingLabel
                                                    .NEGATIVE
                                    },
                                acceptedOriginalMatchCount =
                                    examples.count {
                                        it.role ==
                                                NutritionMatcherTrainingExampleRole
                                                    .ACCEPTED_ORIGINAL_MATCH
                                    },
                                acceptedSelectedCount =
                                    examples.count {
                                        it.role ==
                                                NutritionMatcherTrainingExampleRole
                                                    .ACCEPTED_SELECTED
                                    },
                                rejectedSelectedCount =
                                    0,
                                rejectedNoMatchCandidateCount =
                                    examples.count {
                                        it.role ==
                                                NutritionMatcherTrainingExampleRole
                                                    .REJECTED_NO_MATCH_CANDIDATE
                                    },
                                nonSelectedAlternativeCount =
                                    examples.count {
                                        it.role ==
                                                NutritionMatcherTrainingExampleRole
                                                    .NON_SELECTED_ALTERNATIVE
                                    }
                            ),
                        examples =
                            examples
                    )
            )

            writeJson(
                file =
                    modelFile,
                value =
                    createFixtureModel(
                        exampleCount =
                            examples.size
                    )
            )

            val comparator =
                LocalNutritionMatcherGptFixtureComparator()

            val first =
                comparator.run(
                    datasetFile =
                        datasetFile,
                    modelFile =
                        modelFile,
                    outputFile =
                        outputFile,
                    output =
                        PrintStream(
                            ByteArrayOutputStream()
                        )
                )

            val firstContent =
                outputFile.readText()

            val second =
                comparator.run(
                    datasetFile =
                        datasetFile,
                    modelFile =
                        modelFile,
                    outputFile =
                        outputFile,
                    output =
                        PrintStream(
                            ByteArrayOutputStream()
                        )
                )

            assertEquals(
                expected =
                    first.comparison,
                actual =
                    second.comparison
            )

            assertEquals(
                expected =
                    firstContent,
                actual =
                    outputFile.readText()
            )

            assertTrue(
                first.comparison.thresholds.isNotEmpty()
            )

            assertTrue(
                first.comparison.topOne.accuracy in
                        0.0..1.0
            )

        } finally {
            directory.deleteRecursively()
        }
    }

    private fun createFixtureExamples():
            List<NutritionMatcherTrainingExample> {

        val catalogKeys =
            generateSequence(
                seed = 0
            ) {
                it + 1
            }
                .map {
                    "fixture food $it"
                }
                .filter {
                    stableBucket(
                        value = it,
                        modulo = 10
                    ) in setOf(
                        0,
                        1
                    )
                }
                .take(4)
                .toList()

        val examples =
            catalogKeys.flatMapIndexed {
                    index,
                    catalogKey ->

                val positiveServerKey =
                    "$catalogKey exact"

                val negativeServerKey =
                    "unrelated product $index"

                listOf(
                    example(
                        catalogKey =
                            catalogKey,
                        serverKey =
                            positiveServerKey,
                        label =
                            NutritionMatcherTrainingLabel.POSITIVE,
                        role =
                            NutritionMatcherTrainingExampleRole
                                .ACCEPTED_ORIGINAL_MATCH,
                        candidateRank =
                            1,
                        diagnosticScore =
                            0.9
                    ),
                    example(
                        catalogKey =
                            catalogKey,
                        serverKey =
                            negativeServerKey,
                        label =
                            NutritionMatcherTrainingLabel.NEGATIVE,
                        role =
                            NutritionMatcherTrainingExampleRole
                                .NON_SELECTED_ALTERNATIVE,
                        candidateRank =
                            2,
                        diagnosticScore =
                            0.2
                    )
                )
            }

        return examples.sortedWith(
            compareBy<NutritionMatcherTrainingExample>(
                { it.catalogKey },
                { it.candidateRank },
                { it.serverKey },
                { it.id }
            )
        )
    }

    private fun example(
        catalogKey: String,
        serverKey: String,
        label: NutritionMatcherTrainingLabel,
        role: NutritionMatcherTrainingExampleRole,
        candidateRank: Int,
        diagnosticScore: Double
    ): NutritionMatcherTrainingExample {

        return NutritionMatcherTrainingExample(
            id =
                "$catalogKey|$serverKey|$label|$role",
            catalogKey =
                catalogKey,
            serverArtifact =
                "nutrition.json",
            serverKey =
                serverKey,
            label =
                label,
            role =
                role,
            selected =
                label ==
                        NutritionMatcherTrainingLabel.POSITIVE,
            candidateRank =
                candidateRank,
            candidateCount =
                2,
            diagnosticScore =
                diagnosticScore,
            diagnosticScoreAvailable =
                true,
            sharedTokens =
                emptyList(),
            matcherConfidence =
                0.8,
            originalDecisionType =
                if (
                    label ==
                    NutritionMatcherTrainingLabel.POSITIVE
                ) {
                    "MATCH"
                } else {
                    "NO_MATCH"
                },
            originalDecisionReason =
                null,
            originalValidationStatus =
                if (
                    label ==
                    NutritionMatcherTrainingLabel.POSITIVE
                ) {
                    "ACCEPTED"
                } else {
                    "REJECTED_NO_MATCH"
                },
            originalValidationReason =
                null,
            representativeDecisionType =
                null,
            representativeReasons =
                emptyList(),
            trainingWeight =
                1.0,
            provenance =
                NutritionMatcherTrainingProvenance(
                    sourceType =
                        "TEST",
                    candidateQualityFile =
                        "candidate-quality.json",
                    diagnosticsFile =
                        "diagnostics.json",
                    representativeValidationFile =
                        "representative.json",
                    sourceVersion =
                        1,
                    matcher =
                        "test matcher",
                    validator =
                        "test validator"
                )
        )
    }

    private fun createFixtureModel(
        exampleCount: Int
    ): LocalNutritionMatcherModel {

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

        return LocalNutritionMatcherModel(
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
                    8.0
                ) +
                        List(
                            featureNames.size - 1
                        ) {
                            0.0
                        },
            intercept =
                -4.0,
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
                    datasetFile =
                        "dataset.json",
                    datasetVersion =
                        1,
                    exampleCount =
                        exampleCount,
                    trainingExampleCount =
                        0,
                    testExampleCount =
                        exampleCount,
                    trainingCatalogKeyCount =
                        0,
                    testCatalogKeyCount =
                        exampleCount / 2,
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
                        emptyList<LocalNutritionMatcherRoleMetrics>(),
                    testByRole =
                        emptyList<LocalNutritionMatcherRoleMetrics>()
                )
        )
    }

    private fun writeJson(
        file: File,
        value: Any
    ) {
        val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()

        file.writeText(
            gson.toJson(value) + "\n"
        )
    }

    private fun stableBucket(
        value: String,
        modulo: Int
    ): Int {

        val digest =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    value.toByteArray(
                        StandardCharsets.UTF_8
                    )
                )

        val unsigned =
            (
                    (digest[0].toInt() and 0xff) shl 24
                    ) or
                    (
                            (digest[1].toInt() and 0xff) shl 16
                            ) or
                    (
                            (digest[2].toInt() and 0xff) shl 8
                            ) or
                    (digest[3].toInt() and 0xff)

        return (unsigned and Int.MAX_VALUE) %
                modulo
    }
}