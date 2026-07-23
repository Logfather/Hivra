package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingDataset
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingDatasetSummary
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingExample
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingExampleRole
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingLabel
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingProvenance
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureExtractor
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherModelTrainer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalNutritionMatcherModelTrainerTest {

    @Test
    fun trainModelDeterministically() {

        val directory =
            createTempDirectory(
                prefix =
                    "local-nutrition-matcher-model-"
            )
                .toFile()

        try {
            val datasetFile =
                File(
                    directory,
                    "dataset.json"
                )

            val outputFile =
                File(
                    directory,
                    "model.json"
                )

            writeDataset(
                file =
                    datasetFile
            )

            val trainer =
                LocalNutritionMatcherModelTrainer()

            val first =
                trainer.train(
                    datasetFile =
                        datasetFile,
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
                trainer.train(
                    datasetFile =
                        datasetFile,
                    outputFile =
                        outputFile,
                    output =
                        PrintStream(
                            ByteArrayOutputStream()
                        )
                )

            assertEquals(
                expected =
                    first.model,
                actual =
                    second.model
            )

            assertEquals(
                expected =
                    firstContent,
                actual =
                    outputFile.readText()
            )

            assertTrue(
                first.model.coefficients.all {
                    it.isFinite()
                }
            )

            assertTrue(
                first.model.intercept.isFinite()
            )

            assertEquals(
                expected =
                        LocalNutritionMatcherFeatureExtractor.BASE_FEATURE_COUNT,
                actual =
                    first.model.featureNames.size,
            )

            assertEquals(
                expected =
                    LocalNutritionMatcherFeatureExtractor.BASE_FEATURE_NAMES,
                actual =
                    first.model.featureNames,
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

            assertEquals(
                expected = 2,
                actual = first.model.version,
            )

            assertEquals(
                expected =
                    LocalNutritionMatcherFeatureExtractor
                        .BASE_FEATURE_NAMES,
                actual =
                    first.model.featureNames.take(
                        LocalNutritionMatcherFeatureExtractor
                            .BASE_FEATURE_COUNT,
                    ),
            )

            assertTrue(
                first.model.featureNames.none { featureName ->
                    featureName in
                            LocalNutritionMatcherFeatureExtractor
                                .DOMAIN_MISMATCH_FEATURE_NAMES
                },
            )

            assertTrue(
                "diagnostic_score_available" !in
                        first.model.featureNames,
            )

            assertTrue(
                "domain_feature_version" !in
                        first.model.featureNames,
            )

            assertTrue(
                "domain_report_relationship_present" !in
                        first.model.featureNames,
            )

            assertTrue(
                first.model.diagnosticScoreImputationValue
                    .isFinite()
            )

        } finally {
            directory.deleteRecursively()
        }
    }

    private fun writeDataset(
        file: File
    ) {
        val examples =
            mutableListOf<
                    NutritionMatcherTrainingExample
                    >()

        repeat(50) { index ->

            val positiveCatalogKey =
                "positive food $index"

            examples +=
                example(
                    catalogKey =
                        positiveCatalogKey,
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
                            "positive"
                        )
                )

            examples +=
                example(
                    catalogKey =
                        positiveCatalogKey,
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
                        emptyList()
                )
        }

        val sorted =
            examples.sortedWith(
                compareBy<NutritionMatcherTrainingExample>(
                    { it.catalogKey },
                    { it.candidateRank },
                    { it.serverKey },
                    { it.id }
                )
            )

        val dataset =
            NutritionMatcherTrainingDataset(
                summary =
                    NutritionMatcherTrainingDatasetSummary(
                        sourceCatalogKeyCount =
                            50,
                        exampleCount =
                            sorted.size,
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
                    sorted
            )

        val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()

        file.writeText(
            gson.toJson(dataset) + "\n"
        )
    }

    private fun example(
        catalogKey: String,
        serverKey: String,
        label: NutritionMatcherTrainingLabel,
        role: NutritionMatcherTrainingExampleRole,
        rank: Int,
        score: Double,
        sharedTokens: List<String>
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
                        "SAME_PRODUCT_CLASS"
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
                        "test validator"
                )
        )
    }
}