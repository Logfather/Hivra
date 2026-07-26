package de.shopme.tools.knowledge.mapping.catalog.training.model

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * Identifiziert schädliche Nutrition-Domain-Features über eine
 * deterministische Leave-one-feature-out-Ablation.
 *
 * Die Optimierungsbaseline ist bewusst unabhängig vom aktuell produktiven
 * Featurevertrag:
 *
 * BASE_FEATURE_NAMES
 * +
 * OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES
 *
 * Dadurch werden auch Features erneut bewertet, die im produktiven Modell
 * bereits als schädlich ausgeschlossen sind. Die Auswertung bestätigt daher
 * nicht lediglich den aktuellen Vertrag, sondern berechnet die Klassifikation
 * reproduzierbar aus Trainingsdataset und Modellmetriken neu.
 */
class NutritionFeatureSubsetOptimizer(
    private val fullFeatureExtractor:
    LocalNutritionMatcherFeatureProvider =
        LocalNutritionMatcherFeatureExtractor(),
) {

    fun optimize(
        datasetFile: File,
        workingDirectory: File,
    ): NutritionFeatureOptimizationReport {

        validateInputs(
            datasetFile =
                datasetFile,
            workingDirectory =
                workingDirectory,
        )

        val baselineDomainFeatureNames =
            LocalNutritionMatcherFeatureContract
                .OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES
                .toList()

        val baselineFeatureNames =
            LocalNutritionMatcherFeatureContract
                .BASE_FEATURE_NAMES +
                    baselineDomainFeatureNames

        validateOptimizationContract(
            baselineFeatureNames =
                baselineFeatureNames,
            baselineDomainFeatureNames =
                baselineDomainFeatureNames,
        )

        val baselineModel =
            train(
                datasetFile =
                    datasetFile,
                outputFile =
                    File(
                        workingDirectory,
                        BASELINE_MODEL_FILE_NAME,
                    ),
                selectedFeatureNames =
                    baselineFeatureNames,
            )

        val entries =
            baselineDomainFeatureNames
                .map { removedFeatureName ->

                    val ablatedFeatureNames =
                        baselineFeatureNames.filterNot { featureName ->
                            featureName ==
                                    removedFeatureName
                        }

                    validateAblatedFeatureContract(
                        baselineFeatureNames =
                            baselineFeatureNames,
                        ablatedFeatureNames =
                            ablatedFeatureNames,
                        removedFeatureName =
                            removedFeatureName,
                    )

                    val ablatedModel =
                        train(
                            datasetFile =
                                datasetFile,
                            outputFile =
                                File(
                                    workingDirectory,
                                    "without-" +
                                            sanitizeFeatureName(
                                                featureName =
                                                    removedFeatureName,
                                            ) +
                                            MODEL_FILE_SUFFIX,
                                ),
                            selectedFeatureNames =
                                ablatedFeatureNames,
                        )

                    createEntry(
                        removedFeatureName =
                            removedFeatureName,
                        baselineModel =
                            baselineModel,
                        ablatedModel =
                            ablatedModel,
                    )
                }
                .sortedBy { entry ->
                    entry.featureName
                }

        require(
            entries.size ==
                    baselineDomainFeatureNames.size,
        ) {
            "Nutrition feature optimization did not evaluate every " +
                    "optimization-baseline domain feature."
        }

        require(
            entries
                .map { entry ->
                    entry.featureName
                }
                .toSet() ==
                    baselineDomainFeatureNames.toSet(),
        ) {
            "Nutrition feature optimization entries differ from the " +
                    "optimization-baseline domain feature contract."
        }

        val requiredFeatureNames =
            entries
                .filter { entry ->
                    entry.classification ==
                            NutritionFeatureOptimizationClassification
                                .REQUIRED
                }
                .map { entry ->
                    entry.featureName
                }
                .sorted()

        val neutralFeatureNames =
            entries
                .filter { entry ->
                    entry.classification ==
                            NutritionFeatureOptimizationClassification
                                .NEUTRAL
                }
                .map { entry ->
                    entry.featureName
                }
                .sorted()

        val harmfulFeatureNames =
            entries
                .filter { entry ->
                    entry.classification ==
                            NutritionFeatureOptimizationClassification
                                .HARMFUL
                }
                .map { entry ->
                    entry.featureName
                }
                .sorted()

        validateClassificationPartition(
            baselineDomainFeatureNames =
                baselineDomainFeatureNames,
            requiredFeatureNames =
                requiredFeatureNames,
            neutralFeatureNames =
                neutralFeatureNames,
            harmfulFeatureNames =
                harmfulFeatureNames,
        )

        val recommendedDomainFeatureNames =
            baselineDomainFeatureNames.filterNot { featureName ->
                featureName in
                        harmfulFeatureNames
            }

        val recommendedFeatureNames =
            LocalNutritionMatcherFeatureContract
                .BASE_FEATURE_NAMES +
                    recommendedDomainFeatureNames

        require(
            recommendedFeatureNames.take(
                LocalNutritionMatcherFeatureContract
                    .BASE_FEATURE_COUNT,
            ) ==
                    LocalNutritionMatcherFeatureContract
                        .BASE_FEATURE_NAMES,
        ) {
            "Recommended nutrition feature subset must preserve every " +
                    "base feature in its original order."
        }

        require(
            recommendedFeatureNames.drop(
                LocalNutritionMatcherFeatureContract
                    .BASE_FEATURE_COUNT,
            ) ==
                    recommendedDomainFeatureNames,
        ) {
            "Recommended nutrition domain features do not preserve the " +
                    "optimization-baseline order."
        }

        return NutritionFeatureOptimizationReport(
            version =
                REPORT_VERSION,
            baselineFeatureNames =
                baselineFeatureNames,
            baselineFeatureCount =
                baselineFeatureNames.size,
            baselineTestPrecision =
                baselineModel.metrics.test.precision,
            baselineTestRecall =
                baselineModel.metrics.test.recall,
            baselineTestF1 =
                baselineModel.metrics.test.f1,
            baselineTestBalancedAccuracy =
                baselineModel.metrics.test.balancedAccuracy,
            requiredFeatureNames =
                requiredFeatureNames,
            neutralFeatureNames =
                neutralFeatureNames,
            harmfulFeatureNames =
                harmfulFeatureNames,
            recommendedFeatureNames =
                recommendedFeatureNames,
            entries =
                entries,
        )
    }

    private fun validateInputs(
        datasetFile: File,
        workingDirectory: File,
    ) {

        require(datasetFile.isFile) {
            "Nutrition matcher training dataset does not exist: " +
                    datasetFile.path
        }

        if (!workingDirectory.exists()) {
            require(workingDirectory.mkdirs()) {
                "Could not create feature optimization directory: " +
                        workingDirectory.path
            }
        }

        require(workingDirectory.isDirectory) {
            "Feature optimization working path is not a directory: " +
                    workingDirectory.path
        }
    }

    private fun validateOptimizationContract(
        baselineFeatureNames: List<String>,
        baselineDomainFeatureNames: List<String>,
    ) {

        require(
            baselineDomainFeatureNames.isNotEmpty(),
        ) {
            "Nutrition optimization-baseline domain features must not " +
                    "be empty."
        }

        require(
            baselineDomainFeatureNames.distinct().size ==
                    baselineDomainFeatureNames.size,
        ) {
            "Nutrition optimization-baseline domain features must be " +
                    "unique."
        }

        require(
            baselineDomainFeatureNames.all { featureName ->
                featureName in
                        LocalNutritionMatcherFeatureContract
                            .ALL_DOMAIN_FEATURE_NAMES
            },
        ) {
            "Every optimization-baseline domain feature must belong " +
                    "to the complete domain feature contract."
        }

        require(
            baselineFeatureNames ==
                    LocalNutritionMatcherFeatureContract
                        .BASE_FEATURE_NAMES +
                    baselineDomainFeatureNames,
        ) {
            "Nutrition optimization baseline must consist of the " +
                    "complete base-feature prefix followed by the " +
                    "optimization-baseline domain features."
        }

        require(
            baselineFeatureNames.distinct().size ==
                    baselineFeatureNames.size,
        ) {
            "Nutrition optimization baseline contains duplicate features."
        }

        require(
            baselineFeatureNames.all { featureName ->
                featureName in
                        fullFeatureExtractor.featureNames
            },
        ) {
            "Nutrition optimization baseline contains a feature that " +
                    "the full extractor cannot produce."
        }
    }

    private fun validateAblatedFeatureContract(
        baselineFeatureNames: List<String>,
        ablatedFeatureNames: List<String>,
        removedFeatureName: String,
    ) {

        require(
            removedFeatureName in
                    LocalNutritionMatcherFeatureContract
                        .OPTIMIZATION_BASELINE_DOMAIN_FEATURE_NAMES,
        ) {
            "Only optimization-baseline domain features may be ablated: " +
                    removedFeatureName
        }

        require(
            removedFeatureName in
                    baselineFeatureNames,
        ) {
            "Removed feature is not part of the optimization baseline: " +
                    removedFeatureName
        }

        require(
            removedFeatureName !in
                    ablatedFeatureNames,
        ) {
            "Removed feature remains part of the ablated feature subset: " +
                    removedFeatureName
        }

        require(
            ablatedFeatureNames.size ==
                    baselineFeatureNames.size - 1,
        ) {
            "Ablated nutrition feature subset must contain exactly one " +
                    "feature less than the optimization baseline."
        }

        require(
            ablatedFeatureNames.take(
                LocalNutritionMatcherFeatureContract
                    .BASE_FEATURE_COUNT,
            ) ==
                    LocalNutritionMatcherFeatureContract
                        .BASE_FEATURE_NAMES,
        ) {
            "Feature ablation must preserve every base feature in its " +
                    "original order: $removedFeatureName"
        }

        require(
            ablatedFeatureNames ==
                    baselineFeatureNames.filterNot { featureName ->
                        featureName ==
                                removedFeatureName
                    },
        ) {
            "Feature ablation may remove only the requested feature: " +
                    removedFeatureName
        }
    }

    private fun train(
        datasetFile: File,
        outputFile: File,
        selectedFeatureNames: List<String>,
    ): LocalNutritionMatcherModel {

        require(selectedFeatureNames.isNotEmpty()) {
            "Selected nutrition matcher feature names must not be empty."
        }

        require(
            selectedFeatureNames.distinct().size ==
                    selectedFeatureNames.size,
        ) {
            "Selected nutrition matcher feature names must be unique."
        }

        require(
            selectedFeatureNames.take(
                LocalNutritionMatcherFeatureContract
                    .BASE_FEATURE_COUNT,
            ) ==
                    LocalNutritionMatcherFeatureContract
                        .BASE_FEATURE_NAMES,
        ) {
            "Selected nutrition matcher features must preserve the " +
                    "base-feature prefix."
        }

        LocalNutritionMatcherFeatureContract
            .validateOptimizationFeatureSubset(
                featureNames =
                    selectedFeatureNames,
            )

        val featureExtractor =
            LocalNutritionMatcherFeatureSubsetExtractor(
                delegate =
                    fullFeatureExtractor,
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

        val model =
            trainer
                .train(
                    datasetFile =
                        datasetFile,
                    outputFile =
                        outputFile,
                    output =
                        PrintStream(
                            ByteArrayOutputStream(),
                        ),
                )
                .model

        require(
            model.featureNames ==
                    selectedFeatureNames,
        ) {
            "Trained nutrition matcher feature contract differs from " +
                    "the requested feature subset."
        }

        return model
    }

    private fun createEntry(
        removedFeatureName: String,
        baselineModel: LocalNutritionMatcherModel,
        ablatedModel: LocalNutritionMatcherModel,
    ): NutritionFeatureOptimizationEntry {

        require(
            removedFeatureName in
                    baselineModel.featureNames,
        ) {
            "Removed feature is not part of the baseline model: " +
                    removedFeatureName
        }

        require(
            removedFeatureName !in
                    ablatedModel.featureNames,
        ) {
            "Removed feature remains part of the ablated model: " +
                    removedFeatureName
        }

        require(
            ablatedModel.featureNames.size ==
                    baselineModel.featureNames.size - 1,
        ) {
            "Ablated model must contain exactly one feature less than " +
                    "the baseline model."
        }

        val precisionDelta =
            ablatedModel.metrics.test.precision -
                    baselineModel.metrics.test.precision

        val recallDelta =
            ablatedModel.metrics.test.recall -
                    baselineModel.metrics.test.recall

        val f1Delta =
            ablatedModel.metrics.test.f1 -
                    baselineModel.metrics.test.f1

        val balancedAccuracyDelta =
            ablatedModel.metrics.test.balancedAccuracy -
                    baselineModel.metrics.test.balancedAccuracy

        require(
            listOf(
                precisionDelta,
                recallDelta,
                f1Delta,
                balancedAccuracyDelta,
            ).all { value ->
                value.isFinite()
            },
        ) {
            "Nutrition feature optimization produced a non-finite " +
                    "metric delta for feature: $removedFeatureName"
        }

        return NutritionFeatureOptimizationEntry(
            featureName =
                removedFeatureName,
            baselineFeatureCount =
                baselineModel.featureNames.size,
            ablatedFeatureCount =
                ablatedModel.featureNames.size,
            baselineTestPrecision =
                baselineModel.metrics.test.precision,
            ablatedTestPrecision =
                ablatedModel.metrics.test.precision,
            precisionDelta =
                precisionDelta,
            baselineTestRecall =
                baselineModel.metrics.test.recall,
            ablatedTestRecall =
                ablatedModel.metrics.test.recall,
            recallDelta =
                recallDelta,
            baselineTestF1 =
                baselineModel.metrics.test.f1,
            ablatedTestF1 =
                ablatedModel.metrics.test.f1,
            f1Delta =
                f1Delta,
            baselineTestBalancedAccuracy =
                baselineModel.metrics.test.balancedAccuracy,
            ablatedTestBalancedAccuracy =
                ablatedModel.metrics.test.balancedAccuracy,
            balancedAccuracyDelta =
                balancedAccuracyDelta,
            classification =
                classify(
                    f1Delta =
                        f1Delta,
                    balancedAccuracyDelta =
                        balancedAccuracyDelta,
                ),
        )
    }

    /**
     * Bedeutung der Deltas:
     *
     * Positiver Delta-Wert:
     *     Das Modell wird besser, wenn das Feature entfernt wird.
     *
     * Negativer Delta-Wert:
     *     Das Modell wird schlechter, wenn das Feature entfernt wird.
     *
     * HARMFUL:
     *     Das Entfernen verbessert F1 materiell und verschlechtert
     *     Balanced Accuracy nicht materiell.
     *
     * REQUIRED:
     *     Das Entfernen verschlechtert F1 oder Balanced Accuracy
     *     materiell.
     *
     * NEUTRAL:
     *     Keine materielle Änderung.
     */
    private fun classify(
        f1Delta: Double,
        balancedAccuracyDelta: Double,
    ): NutritionFeatureOptimizationClassification {

        require(f1Delta.isFinite()) {
            "Nutrition feature optimization F1 delta must be finite."
        }

        require(balancedAccuracyDelta.isFinite()) {
            "Nutrition feature optimization balanced-accuracy delta " +
                    "must be finite."
        }

        return when {
            f1Delta >=
                    MATERIAL_IMPROVEMENT_THRESHOLD &&
                    balancedAccuracyDelta >=
                    -MATERIAL_REGRESSION_THRESHOLD -> {

                NutritionFeatureOptimizationClassification
                    .HARMFUL
            }

            f1Delta <=
                    -MATERIAL_REGRESSION_THRESHOLD ||
                    balancedAccuracyDelta <=
                    -MATERIAL_REGRESSION_THRESHOLD -> {

                NutritionFeatureOptimizationClassification
                    .REQUIRED
            }

            else -> {
                NutritionFeatureOptimizationClassification
                    .NEUTRAL
            }
        }
    }

    private fun validateClassificationPartition(
        baselineDomainFeatureNames: List<String>,
        requiredFeatureNames: List<String>,
        neutralFeatureNames: List<String>,
        harmfulFeatureNames: List<String>,
    ) {

        val classifiedFeatureNames =
            requiredFeatureNames +
                    neutralFeatureNames +
                    harmfulFeatureNames

        require(
            classifiedFeatureNames.size ==
                    baselineDomainFeatureNames.size,
        ) {
            "Nutrition feature classifications do not cover every " +
                    "optimization-baseline domain feature."
        }

        require(
            classifiedFeatureNames.distinct().size ==
                    classifiedFeatureNames.size,
        ) {
            "Nutrition feature classifications overlap."
        }

        require(
            classifiedFeatureNames.toSet() ==
                    baselineDomainFeatureNames.toSet(),
        ) {
            "Nutrition feature classifications differ from the " +
                    "optimization-baseline domain feature contract."
        }
    }

    private fun sanitizeFeatureName(
        featureName: String,
    ): String {

        val sanitized =
            featureName
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

        require(sanitized.isNotBlank()) {
            "Could not create a file-safe nutrition feature name from: " +
                    featureName
        }

        return sanitized
    }

    private companion object {

        const val REPORT_VERSION =
            2

        const val BASELINE_MODEL_FILE_NAME =
            "baseline-model.json"

        const val MODEL_FILE_SUFFIX =
            ".json"

        /*
         * Absolute Änderungen unter 0,25 Prozentpunkten gelten als
         * numerisches beziehungsweise datensatzbedingtes Rauschen.
         */
        const val MATERIAL_IMPROVEMENT_THRESHOLD =
            0.0025

        const val MATERIAL_REGRESSION_THRESHOLD =
            0.0025
    }
}