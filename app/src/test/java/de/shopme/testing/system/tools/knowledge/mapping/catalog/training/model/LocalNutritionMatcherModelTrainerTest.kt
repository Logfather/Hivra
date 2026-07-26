package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingDataset
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingDatasetSummary
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingExample
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingExampleRole
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingLabel
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingProvenance
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherCandidate
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureContract
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureExtractor
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureProvider
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureSubsetExtractor
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherModelContract
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherModelTrainer
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionMatcherConservativeThresholdPolicyContract
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LocalNutritionMatcherModelTrainerTest {

    @Test
    fun trainModelDeterministically() {
        val directory =
            createTempDirectory(
                prefix =
                    "local-nutrition-matcher-model-",
            )
                .toFile()

        try {
            val datasetFile =
                File(
                    directory,
                    "dataset.json",
                )

            val outputFile =
                File(
                    directory,
                    "model.json",
                )

            writeDataset(
                file =
                    datasetFile,
            )

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

            val output =
                PrintStream(
                    ByteArrayOutputStream(),
                )

            val trainingResult =
                trainer.train(
                    datasetFile =
                        datasetFile,
                    outputFile =
                        outputFile,
                    output =
                        output,
                )

            val first =
                trainSilently(
                    trainer =
                        trainer,
                    datasetFile =
                        datasetFile,
                    outputFile =
                        outputFile,
                )

            val firstContent =
                outputFile.readText()

            val second =
                trainSilently(
                    trainer =
                        trainer,
                    datasetFile =
                        datasetFile,
                    outputFile =
                        outputFile,
                )

            assertEquals(
                expected =
                    first.model,
                actual =
                    second.model,
            )

            assertEquals(
                expected =
                    firstContent,
                actual =
                    outputFile.readText(),
            )

            assertEquals(
                expected =
                    LocalNutritionMatcherModelContract
                        .CURRENT_VERSION,
                actual =
                    first.model.version,
            )

            assertEquals(
                expected =
                    LocalNutritionMatcherFeatureContract
                        .ACTIVE_FEATURE_COUNT,
                actual =
                    first.model.featureNames.size,
            )

            assertEquals(
                expected =
                    selectedFeatureNames,
                actual =
                    first.model.featureNames,
            )

            assertEquals(
                expected =
                    LocalNutritionMatcherFeatureContract
                        .BASE_FEATURE_NAMES,
                actual =
                    first.model.featureNames.take(
                        LocalNutritionMatcherFeatureContract
                            .BASE_FEATURE_COUNT,
                    ),
            )

            assertEquals(
                expected =
                    LocalNutritionMatcherFeatureContract
                        .ACTIVE_DOMAIN_FEATURE_NAMES,
                actual =
                    first.model.featureNames.drop(
                        LocalNutritionMatcherFeatureContract
                            .BASE_FEATURE_COUNT,
                    ),
            )

            assertTrue(
                actual =
                    first.model.featureNames.none { featureName ->
                        featureName in
                                LocalNutritionMatcherFeatureContract
                                    .HARMFUL_DOMAIN_FEATURE_NAMES
                    },
            )

            assertEquals(
                expected =
                    first.model.featureNames.size,
                actual =
                    first.model.coefficients.size,
            )

            assertEquals(
                expected =
                    first.model.featureNames.size,
                actual =
                    first.model.featureMeans.size,
            )

            assertEquals(
                expected =
                    first.model.featureNames.size,
                actual =
                    first.model.featureStandardDeviations.size,
            )

            assertTrue(
                actual =
                    first.model.coefficients.all { coefficient ->
                        coefficient.isFinite()
                    },
            )

            assertTrue(
                actual =
                    first.model.featureMeans.all { mean ->
                        mean.isFinite()
                    },
            )

            assertTrue(
                actual =
                    first.model.featureStandardDeviations
                        .all { standardDeviation ->
                            standardDeviation.isFinite() &&
                                    standardDeviation > 0.0
                        },
            )

            assertTrue(
                actual =
                    first.model.intercept.isFinite(),
            )

            assertTrue(
                actual =
                    first.model.decisionThreshold.isFinite(),
            )

            assertTrue(
                actual =
                    first.model.diagnosticScoreImputationValue
                        .isFinite(),
            )

            assertTrue(
                actual =
                    "diagnostic_score_available" !in
                            first.model.featureNames,
            )

            assertTrue(
                actual =
                    "domain_feature_version" !in
                            first.model.featureNames,
            )

            assertTrue(
                actual =
                    "domain_report_relationship_present" !in
                            first.model.featureNames,
            )

            assertEquals(
                expected =
                    datasetFile.name,
                actual =
                    first.model.training.datasetFile,
            )

            assertEquals(
                expected =
                    100,
                actual =
                    first.model.training.exampleCount,
            )

            assertEquals(
                expected =
                    first.model.training.exampleCount,
                actual =
                    first.model.training.trainingExampleCount +
                            first.model.training.testExampleCount,
            )

            assertTrue(
                actual =
                    first.model.training.trainingExampleCount > 0,
            )

            assertTrue(
                actual =
                    first.model.training.testExampleCount > 0,
            )

            assertTrue(
                actual =
                    first.model.metrics.training.exampleCount > 0,
            )

            assertTrue(
                actual =
                    first.model.metrics.test.exampleCount > 0,
            )

            assertEquals(
                expected =
                    NutritionMatcherConservativeThresholdPolicyContract
                        .ACTIVE_POLICY
                        .minimumPrecision,
                actual =
                    trainingResult.model
                        .decisionThresholdOptimization
                        .minimumPrecision,
            )

            assertEquals(
                expected =
                    NutritionMatcherConservativeThresholdPolicyContract
                        .ACTIVE_POLICY
                        .maximumFalsePositiveRate,
                actual =
                    trainingResult.model
                        .decisionThresholdOptimization
                        .maximumFalsePositiveRate,
            )

            assertEquals(
                expected =
                    NutritionMatcherConservativeThresholdPolicyContract
                        .ACTIVE_POLICY
                        .minimumPredictedPositiveCount,
                actual =
                    trainingResult.model
                        .decisionThresholdOptimization
                        .minimumPredictedPositiveCount,
            )

            assertTrue(
                actual =
                    trainingResult.model
                        .decisionThresholdOptimization
                        .policySatisfied,
            )

            assertTrue(
                actual =
                    outputFile.isFile,
            )

            assertTrue(
                actual =
                    outputFile.length() > 0L,
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun trainSilently(
        trainer: LocalNutritionMatcherModelTrainer,
        datasetFile: File,
        outputFile: File,
    ) =
        PrintStream(
            ByteArrayOutputStream(),
        )
            .use { output ->
                trainer.train(
                    datasetFile =
                        datasetFile,
                    outputFile =
                        outputFile,
                    output =
                        output,
                )
            }

    private fun writeDataset(
        file: File,
    ) {
        val examples =
            mutableListOf<NutritionMatcherTrainingExample>()

        repeat(50) { index ->
            val catalogKey =
                "positive food $index"

            examples +=
                example(
                    catalogKey =
                        catalogKey,
                    serverKey =
                        "positive food $index",
                    label =
                        NutritionMatcherTrainingLabel.POSITIVE,
                    role =
                        NutritionMatcherTrainingExampleRole
                            .ACCEPTED_SELECTED,
                    rank =
                        1,
                    score =
                        0.95,
                    sharedTokens =
                        listOf(
                            "food",
                            "positive",
                        ),
                )

            examples +=
                example(
                    catalogKey =
                        catalogKey,
                    serverKey =
                        "unrelated product $index",
                    label =
                        NutritionMatcherTrainingLabel.NEGATIVE,
                    role =
                        NutritionMatcherTrainingExampleRole
                            .NON_SELECTED_ALTERNATIVE,
                    rank =
                        2,
                    score =
                        0.25,
                    sharedTokens =
                        emptyList(),
                )
        }

        val sortedExamples =
            examples.sortedWith(
                compareBy<NutritionMatcherTrainingExample>(
                    { example ->
                        example.catalogKey
                    },
                    { example ->
                        example.candidateRank
                    },
                    { example ->
                        example.serverKey
                    },
                    { example ->
                        example.id
                    },
                ),
            )

        val dataset =
            NutritionMatcherTrainingDataset(
                summary =
                    NutritionMatcherTrainingDatasetSummary(
                        sourceCatalogKeyCount =
                            50,
                        exampleCount =
                            sortedExamples.size,
                        positiveCount =
                            50,
                        negativeCount =
                            50,
                        acceptedSelectedCount =
                            50,
                        rejectedSelectedCount =
                            0,
                        rejectedNoMatchCandidateCount =
                            0,
                        nonSelectedAlternativeCount =
                            50,
                        acceptedOriginalMatchCount =
                            0,
                    ),
                examples =
                    sortedExamples,
            )

        val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()

        file.writeText(
            gson.toJson(
                dataset,
            ) + System.lineSeparator(),
        )
    }

    private fun example(
        catalogKey: String,
        serverKey: String,
        label: NutritionMatcherTrainingLabel,
        role: NutritionMatcherTrainingExampleRole,
        rank: Int,
        score: Double,
        sharedTokens: List<String>,
    ): NutritionMatcherTrainingExample {
        return NutritionMatcherTrainingExample(
            id =
                "$catalogKey|$serverKey|${label.name}|${role.name}",
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
                role ==
                        NutritionMatcherTrainingExampleRole
                            .ACCEPTED_SELECTED,
            candidateRank =
                rank,
            candidateCount =
                2,
            diagnosticScore =
                score,
            diagnosticScoreAvailable =
                true,
            sharedTokens =
                sharedTokens.sorted(),
            matcherConfidence =
                0.78,
            originalDecisionType =
                "MATCH",
            originalDecisionReason =
                null,
            originalValidationStatus =
                "REJECTED_LOW_CONFIDENCE",
            originalValidationReason =
                null,
            representativeDecisionType =
                if (
                    label ==
                    NutritionMatcherTrainingLabel.POSITIVE
                ) {
                    "REPRESENTATIVE"
                } else {
                    null
                },
            representativeReasons =
                if (
                    label ==
                    NutritionMatcherTrainingLabel.POSITIVE
                ) {
                    listOf(
                        "SAME_PRODUCT_CLASS",
                    )
                } else {
                    emptyList()
                },
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
                        "validation.json",
                    sourceVersion =
                        1,
                    matcher =
                        "test matcher",
                    validator =
                        "test validator",
                ),
        )
    }

    @Test
    fun acceptsOptimizationBaselineFeatureContract() {

        val selectedFeatureNames =
            LocalNutritionMatcherFeatureContract
                .BASE_FEATURE_NAMES +
                    LocalNutritionMatcherFeatureContract
                        .OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES

        val featureExtractor =
            LocalNutritionMatcherFeatureSubsetExtractor(
                delegate =
                    LocalNutritionMatcherFeatureExtractor(),
                selectedFeatureNames =
                    selectedFeatureNames,
            )

        LocalNutritionMatcherModelTrainer(
            featureExtractor =
                featureExtractor,
            supportedFeatureNames =
                selectedFeatureNames,
        )
    }

    @Test
    fun rejectsUnsupportedOptimizationFeatureContract() {

        val unsupportedFeatureNames =
            LocalNutritionMatcherFeatureContract
                .BASE_FEATURE_NAMES +
                    listOf(
                        "unsupportedFeature",
                    )

        val featureExtractor =
            object : LocalNutritionMatcherFeatureProvider {

                override val featureNames: List<String> =
                    unsupportedFeatureNames

                override fun extract(
                    example:
                    NutritionMatcherTrainingExample,
                    diagnosticScoreImputationValue:
                    Double,
                ): DoubleArray {

                    return DoubleArray(
                        size =
                            featureNames.size,
                    )
                }

                override fun extract(
                    candidate:
                    LocalNutritionMatcherCandidate,
                    diagnosticScoreImputationValue:
                    Double,
                ): DoubleArray {

                    return DoubleArray(
                        size =
                            featureNames.size,
                    )
                }
            }

        assertFailsWith<IllegalArgumentException> {
            LocalNutritionMatcherModelTrainer(
                featureExtractor =
                    featureExtractor,
                supportedFeatureNames =
                    unsupportedFeatureNames,
            )
        }
    }

    @Test
    fun acceptsOptimizationBaselineWithOneDomainFeatureRemoved() {

        val removedFeatureName =
            LocalNutritionMatcherFeatureContract
                .OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES
                .first()

        val selectedFeatureNames =
            LocalNutritionMatcherFeatureContract
                .BASE_FEATURE_NAMES +
                    LocalNutritionMatcherFeatureContract
                        .OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES
                        .filterNot { featureName ->
                            featureName ==
                                    removedFeatureName
                        }

        val featureExtractor =
            LocalNutritionMatcherFeatureSubsetExtractor(
                delegate =
                    LocalNutritionMatcherFeatureExtractor(),
                selectedFeatureNames =
                    selectedFeatureNames,
            )

        LocalNutritionMatcherModelTrainer(
            featureExtractor =
                featureExtractor,
            supportedFeatureNames =
                selectedFeatureNames,
        )
    }
}