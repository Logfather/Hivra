package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingDataset
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingDatasetSummary
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingExample
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingExampleRole
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingLabel
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingProvenance
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureContract
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionFeatureSubsetOptimizer
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NutritionFeatureSubsetOptimizerTest {

    @Test
    fun optimizeDomainFeaturesDeterministically() {

        val directory =
            createTempDirectory(
                prefix =
                    "nutrition-feature-optimization-",
            )
                .toFile()

        try {
            val datasetFile =
                File(
                    directory,
                    "dataset.json",
                )

            writeDataset(
                file =
                    datasetFile,
            )

            val optimizer =
                NutritionFeatureSubsetOptimizer()

            val first =
                optimizer.optimize(
                    datasetFile =
                        datasetFile,
                    workingDirectory =
                        File(
                            directory,
                            "first",
                        ),
                )

            val second =
                optimizer.optimize(
                    datasetFile =
                        datasetFile,
                    workingDirectory =
                        File(
                            directory,
                            "second",
                        ),
                )

            val expectedBaselineDomainFeatureNames =
                LocalNutritionMatcherFeatureContract
                    .OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES

            val expectedBaselineFeatureNames =
                LocalNutritionMatcherFeatureContract
                    .BASE_FEATURE_NAMES +
                        expectedBaselineDomainFeatureNames

            assertEquals(
                expected =
                    first,
                actual =
                    second,
            )

            assertEquals(
                expected =
                    REPORT_VERSION,
                actual =
                    first.version,
            )

            assertEquals(
                expected =
                    expectedBaselineFeatureNames,
                actual =
                    first.baselineFeatureNames,
            )

            assertEquals(
                expected =
                    expectedBaselineFeatureNames.size,
                actual =
                    first.baselineFeatureCount,
            )

            assertEquals(
                expected =
                    expectedBaselineDomainFeatureNames.size,
                actual =
                    first.entries.size,
            )

            assertEquals(
                expected =
                    expectedBaselineDomainFeatureNames.sorted(),
                actual =
                    first.entries
                        .map { entry ->
                            entry.featureName
                        }
                        .sorted(),
            )

            assertTrue(
                actual =
                    first.entries.all { entry ->
                        entry.baselineFeatureCount ==
                                expectedBaselineFeatureNames.size
                    },
            )

            assertTrue(
                actual =
                    first.entries.all { entry ->
                        entry.ablatedFeatureCount ==
                                expectedBaselineFeatureNames.size - 1
                    },
            )

            assertTrue(
                actual =
                    first.entries.none { entry ->
                        entry.featureName in
                                LocalNutritionMatcherFeatureContract
                                    .BASE_FEATURE_NAMES
                    },
            )

            assertTrue(
                actual =
                    first.entries.all { entry ->
                        entry.featureName in
                                expectedBaselineDomainFeatureNames
                    },
            )

            assertEquals(
                expected =
                    expectedBaselineDomainFeatureNames.toSet(),
                actual =
                    first.entries
                        .map { entry ->
                            entry.featureName
                        }
                        .toSet(),
            )

            assertEquals(
                expected =
                    first.entries.size,
                actual =
                    first.entries
                        .map { entry ->
                            entry.featureName
                        }
                        .distinct()
                        .size,
            )

            assertTrue(
                actual =
                    first.recommendedFeatureNames.isNotEmpty(),
            )

            assertEquals(
                expected =
                    first.baselineFeatureNames.filterNot { featureName ->
                        featureName in
                                first.harmfulFeatureNames
                    },
                actual =
                    first.recommendedFeatureNames,
            )

            assertEquals(
                expected =
                    LocalNutritionMatcherFeatureContract
                        .BASE_FEATURE_NAMES,
                actual =
                    first.recommendedFeatureNames.take(
                        LocalNutritionMatcherFeatureContract
                            .BASE_FEATURE_COUNT,
                    ),
            )

            assertEquals(
                expected =
                    expectedBaselineDomainFeatureNames.filterNot { featureName ->
                        featureName in
                                first.harmfulFeatureNames
                    },
                actual =
                    first.recommendedFeatureNames.drop(
                        LocalNutritionMatcherFeatureContract
                            .BASE_FEATURE_COUNT,
                    ),
            )

            assertTrue(
                actual =
                    first.harmfulFeatureNames.all { featureName ->
                        featureName in
                                expectedBaselineDomainFeatureNames
                    },
            )

            assertTrue(
                actual =
                    first.requiredFeatureNames.all { featureName ->
                        featureName in
                                expectedBaselineDomainFeatureNames
                    },
            )

            assertTrue(
                actual =
                    first.neutralFeatureNames.all { featureName ->
                        featureName in
                                expectedBaselineDomainFeatureNames
                    },
            )

            val classifiedFeatureNames =
                first.requiredFeatureNames +
                        first.neutralFeatureNames +
                        first.harmfulFeatureNames

            assertEquals(
                expected =
                    expectedBaselineDomainFeatureNames.sorted(),
                actual =
                    classifiedFeatureNames.sorted(),
            )

            assertEquals(
                expected =
                    expectedBaselineDomainFeatureNames.toSet(),
                actual =
                    classifiedFeatureNames.toSet(),
            )

            assertEquals(
                expected =
                    classifiedFeatureNames.size,
                actual =
                    classifiedFeatureNames
                        .distinct()
                        .size,
            )

            assertEquals(
                expected =
                    expectedBaselineDomainFeatureNames.size,
                actual =
                    classifiedFeatureNames.size,
            )

            assertTrue(
                actual =
                    first.entries.all { entry ->
                        entry.precisionDelta.isFinite() &&
                                entry.recallDelta.isFinite() &&
                                entry.f1Delta.isFinite() &&
                                entry.balancedAccuracyDelta.isFinite()
                    },
            )

            assertTrue(
                actual =
                    first.entries.all { entry ->
                        entry.baselineTestPrecision.isFinite() &&
                                entry.ablatedTestPrecision.isFinite() &&
                                entry.baselineTestRecall.isFinite() &&
                                entry.ablatedTestRecall.isFinite() &&
                                entry.baselineTestF1.isFinite() &&
                                entry.ablatedTestF1.isFinite() &&
                                entry.baselineTestBalancedAccuracy.isFinite() &&
                                entry.ablatedTestBalancedAccuracy.isFinite()
                    },
            )

            assertTrue(
                actual =
                    first.entries.all { entry ->
                        entry.precisionDelta ==
                                entry.ablatedTestPrecision -
                                entry.baselineTestPrecision
                    },
            )

            assertTrue(
                actual =
                    first.entries.all { entry ->
                        entry.recallDelta ==
                                entry.ablatedTestRecall -
                                entry.baselineTestRecall
                    },
            )

            assertTrue(
                actual =
                    first.entries.all { entry ->
                        entry.f1Delta ==
                                entry.ablatedTestF1 -
                                entry.baselineTestF1
                    },
            )

            assertTrue(
                actual =
                    first.entries.all { entry ->
                        entry.balancedAccuracyDelta ==
                                entry.ablatedTestBalancedAccuracy -
                                entry.baselineTestBalancedAccuracy
                    },
            )

            assertTrue(
                actual =
                    first.baselineTestPrecision.isFinite(),
            )

            assertTrue(
                actual =
                    first.baselineTestRecall.isFinite(),
            )

            assertTrue(
                actual =
                    first.baselineTestF1.isFinite(),
            )

            assertTrue(
                actual =
                    first.baselineTestBalancedAccuracy.isFinite(),
            )

            assertTrue(
                actual =
                    File(
                        directory,
                        "first/baseline-model.json",
                    ).isFile,
            )

            assertTrue(
                actual =
                    File(
                        directory,
                        "second/baseline-model.json",
                    ).isFile,
            )

            expectedBaselineDomainFeatureNames.forEach { featureName ->

                val expectedFileName =
                    "without-" +
                            sanitizeFeatureName(
                                featureName =
                                    featureName,
                            ) +
                            ".json"

                assertTrue(
                    actual =
                        File(
                            directory,
                            "first/$expectedFileName",
                        ).isFile,
                    message =
                        "Missing first ablation model for feature: " +
                                featureName,
                )

                assertTrue(
                    actual =
                        File(
                            directory,
                            "second/$expectedFileName",
                        ).isFile,
                    message =
                        "Missing second ablation model for feature: " +
                                featureName,
                )
            }
        } finally {
            directory.deleteRecursively()
        }
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

        val sorted =
            examples.sortedWith(
                compareBy<NutritionMatcherTrainingExample>(
                    { it.catalogKey },
                    { it.candidateRank },
                    { it.serverKey },
                    { it.id },
                ),
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
                    sorted,
            )

        val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()

        file.writeText(
            gson.toJson(dataset) + "\n",
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

    private fun sanitizeFeatureName(
        featureName: String,
    ): String {

        return featureName
            .lowercase()
            .map { character ->
                when {
                    character.isLetterOrDigit() ->
                        character

                    else ->
                        '-'
                }
            }
            .joinToString(
                separator =
                    "",
            )
            .trim('-')
    }

    private companion object {

        const val REPORT_VERSION =
            2
    }
}