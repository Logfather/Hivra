package de.shopme.tools.knowledge.mapping.catalog.training.model

import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingDataset
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingExample
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingExampleRole
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingLabel
import java.io.File
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt

class LocalNutritionMatcherModelTrainer(
    private val featureExtractor:
    LocalNutritionMatcherFeatureProvider =
        LocalNutritionMatcherFeatureSubsetExtractor(
            delegate =
                LocalNutritionMatcherFeatureExtractor(),
            selectedFeatureNames =
                LocalNutritionMatcherFeatureContract
                    .ACTIVE_FEATURE_NAMES,
        ),
    private val supportedFeatureNames: List<String> =
        featureExtractor.featureNames,
    private val thresholdOptimizationPolicy:
    NutritionMatcherThresholdOptimizationPolicy =
        NutritionMatcherConservativeThresholdPolicyContract
            .ACTIVE_POLICY,
) {

    init {
        require(
            featureExtractor.featureNames ==
                    supportedFeatureNames,
        ) {
            "Feature extractor contract differs from trainer contract. " +
                    "Expected: " +
                    supportedFeatureNames.joinToString() +
                    "; actual: " +
                    featureExtractor.featureNames.joinToString()
        }

        validateSupportedFeatureContract(
            featureNames =
                supportedFeatureNames,
        )

        require(
            thresholdOptimizationPolicy.minimumPrecision in
                    0.0..1.0,
        ) {
            "Threshold optimization minimum precision must be between " +
                    "zero and one."
        }

        require(
            thresholdOptimizationPolicy.maximumFalsePositiveRate in
                    0.0..1.0,
        ) {
            "Threshold optimization maximum false-positive rate must be " +
                    "between zero and one."
        }

        require(
            thresholdOptimizationPolicy.minimumPredictedPositiveCount >
                    0,
        ) {
            "Threshold optimization minimum predicted-positive count must " +
                    "be positive."
        }

        require(
            thresholdOptimizationPolicy.minimumPrecision in
                    0.0..1.0,
        ) {
            "Threshold minimum precision must be between zero and one."
        }

        require(
            thresholdOptimizationPolicy.maximumFalsePositiveRate in
                    0.0..1.0,
        ) {
            "Threshold maximum false-positive rate must be between zero and one."
        }

        require(
            thresholdOptimizationPolicy.minimumPredictedPositiveCount >
                    0,
        ) {
            "Threshold minimum predicted-positive count must be positive."
        }
    }

    fun train(
        datasetFile: File,
        outputFile: File,
        output: PrintStream = System.out,
    ): TrainLocalNutritionMatcherModelResult {

        require(
            datasetFile.isFile,
        ) {
            "Nutrition matcher training dataset does not exist: " +
                    datasetFile.absolutePath
        }

        val dataset =
            readDataset(
                datasetFile =
                    datasetFile,
            )

        require(
            dataset.version ==
                    DATASET_VERSION,
        ) {
            "Unsupported nutrition matcher dataset version: " +
                    dataset.version
        }

        require(
            dataset.datasetType ==
                    EXPECTED_DATASET_TYPE,
        ) {
            "Unsupported nutrition matcher dataset type: " +
                    dataset.datasetType
        }

        require(
            dataset.examples.isNotEmpty(),
        ) {
            "Nutrition matcher training dataset is empty."
        }

        require(
            dataset.examples.any { example ->
                example.label ==
                        NutritionMatcherTrainingLabel.POSITIVE
            },
        ) {
            "Nutrition matcher training dataset has no " +
                    "positive examples."
        }

        require(
            dataset.examples.any { example ->
                example.label ==
                        NutritionMatcherTrainingLabel.NEGATIVE
            },
        ) {
            "Nutrition matcher training dataset has no " +
                    "negative examples."
        }

        val split =
            splitByCatalogKey(
                examples =
                    dataset.examples,
            )

        require(
            split.trainingExamples.isNotEmpty(),
        ) {
            "Local matcher training split is empty."
        }

        require(
            split.testExamples.isNotEmpty(),
        ) {
            "Local matcher test split is empty."
        }

        require(
            split.trainingExamples.any { example ->
                example.label ==
                        NutritionMatcherTrainingLabel.POSITIVE
            },
        ) {
            "Training split has no positive examples."
        }

        require(
            split.trainingExamples.any { example ->
                example.label ==
                        NutritionMatcherTrainingLabel.NEGATIVE
            },
        ) {
            "Training split has no negative examples."
        }

        val diagnosticScoreImputationValue =
            calculateDiagnosticScoreImputationValue(
                trainingExamples =
                    split.trainingExamples,
            )

        val trainingRaw =
            split.trainingExamples.map { example ->
                RawTrainingExample(
                    source =
                        example,
                    features =
                        featureExtractor.extract(
                            example =
                                example,
                            diagnosticScoreImputationValue =
                                diagnosticScoreImputationValue,
                        ),
                    target =
                        targetOf(
                            example =
                                example,
                        ),
                )
            }

        val testRaw =
            split.testExamples.map { example ->
                RawTrainingExample(
                    source =
                        example,
                    features =
                        featureExtractor.extract(
                            example =
                                example,
                            diagnosticScoreImputationValue =
                                diagnosticScoreImputationValue,
                        ),
                    target =
                        targetOf(
                            example =
                                example,
                        ),
                )
            }

        val scaling =
            calculateScaling(
                trainingExamples =
                    trainingRaw,
            )

        val trainingExamples =
            trainingRaw.map { example ->
                example.toScaled(
                    scaling =
                        scaling,
                )
            }

        val testExamples =
            testRaw.map { example ->
                example.toScaled(
                    scaling =
                        scaling,
                )
            }

        val classWeights =
            calculateClassWeights(
                examples =
                    trainingExamples,
            )

        val trained =
            fitLogisticRegression(
                examples =
                    trainingExamples,
                positiveClassWeight =
                    classWeights.positive,
                negativeClassWeight =
                    classWeights.negative,
            )

        val trainingScoredLabels =
            trainingExamples.map { example ->
                NutritionMatcherScoredLabel(
                    score =
                        predictProbability(
                            example =
                                example,
                            coefficients =
                                trained.coefficients,
                            intercept =
                                trained.intercept,
                        ),
                    positive =
                        example.target ==
                                1.0,
                )
            }

        val thresholdOptimization =
            NutritionMatcherDecisionThresholdOptimizer(
                policy =
                    thresholdOptimizationPolicy,
            )
                .optimize(
                    scoredLabels =
                        trainingScoredLabels,
                )

        val decisionThreshold =
            thresholdOptimization.selectedThreshold

        val trainingMetrics =
            calculateMetrics(
                examples =
                    trainingExamples,
                coefficients =
                    trained.coefficients,
                intercept =
                    trained.intercept,
                decisionThreshold =
                    decisionThreshold,
            )

        val testMetrics =
            calculateMetrics(
                examples =
                    testExamples,
                coefficients =
                    trained.coefficients,
                intercept =
                    trained.intercept,
                decisionThreshold =
                    decisionThreshold,
            )

        val trainingByRole =
            calculateRoleMetrics(
                examples =
                    trainingExamples,
                coefficients =
                    trained.coefficients,
                intercept =
                    trained.intercept,
                decisionThreshold =
                    decisionThreshold,
            )

        val testByRole =
            calculateRoleMetrics(
                examples =
                    testExamples,
                coefficients =
                    trained.coefficients,
                intercept =
                    trained.intercept,
                decisionThreshold =
                    decisionThreshold,
            )

        val model =
            LocalNutritionMatcherModel(
                version =
                    LocalNutritionMatcherModelContract
                        .CURRENT_VERSION,
                featureNames =
                    featureExtractor.featureNames,
                featureMeans =
                    scaling.means.toList(),
                featureStandardDeviations =
                    scaling.standardDeviations.toList(),
                coefficients =
                    trained.coefficients.toList(),
                intercept =
                    trained.intercept,
                decisionThreshold =
                    decisionThreshold,
                decisionThresholdOptimization =
                    LocalNutritionMatcherThresholdOptimizationMetadata(
                        calibrationExampleCount =
                            trainingScoredLabels.size,
                        minimumPrecision =
                            thresholdOptimizationPolicy
                                .minimumPrecision,
                        maximumFalsePositiveRate =
                            thresholdOptimizationPolicy
                                .maximumFalsePositiveRate,
                        minimumPredictedPositiveCount =
                            thresholdOptimizationPolicy
                                .minimumPredictedPositiveCount,
                        policySatisfied =
                            thresholdOptimization
                                .policySatisfied,
                        evaluatedThresholdCount =
                            thresholdOptimization
                                .evaluatedThresholdCount,
                        selectedPrecision =
                            thresholdOptimization
                                .selectedMetrics
                                .precision,
                        selectedRecall =
                            thresholdOptimization
                                .selectedMetrics
                                .recall,
                        selectedFalsePositiveRate =
                            thresholdOptimization
                                .selectedMetrics
                                .falsePositiveRate,
                        selectedF1 =
                            thresholdOptimization
                                .selectedMetrics
                                .f1,
                        selectedTruePositiveCount =
                            thresholdOptimization
                                .selectedMetrics
                                .truePositiveCount,
                        selectedFalsePositiveCount =
                            thresholdOptimization
                                .selectedMetrics
                                .falsePositiveCount,
                        selectedTrueNegativeCount =
                            thresholdOptimization
                                .selectedMetrics
                                .trueNegativeCount,
                        selectedFalseNegativeCount =
                            thresholdOptimization
                                .selectedMetrics
                                .falseNegativeCount,
                    ),
                diagnosticScoreImputationValue =
                    diagnosticScoreImputationValue,
                training =
                    LocalNutritionMatcherTrainingMetadata(
                        datasetFile =
                            datasetFile.name,
                        datasetVersion =
                            dataset.version,
                        exampleCount =
                            dataset.examples.size,
                        trainingExampleCount =
                            trainingExamples.size,
                        testExampleCount =
                            testExamples.size,
                        trainingCatalogKeyCount =
                            split.trainingCatalogKeys.size,
                        testCatalogKeyCount =
                            split.testCatalogKeys.size,
                        positiveClassWeight =
                            classWeights.positive,
                        negativeClassWeight =
                            classWeights.negative,
                        learningRate =
                            LEARNING_RATE,
                        iterationCount =
                            ITERATION_COUNT,
                        l2Regularization =
                            L2_REGULARIZATION,
                        splitModulo =
                            SPLIT_MODULO,
                        testBuckets =
                            TEST_BUCKETS.sorted(),
                    ),
                metrics =
                    LocalNutritionMatcherModelMetrics(
                        training =
                            trainingMetrics,
                        test =
                            testMetrics,
                        trainingByRole =
                            trainingByRole,
                        testByRole =
                            testByRole,
                    ),
            )



        validateModel(
            model =
                model,
        )

        writeModel(
            model =
                model,
            outputFile =
                outputFile,
        )

        printResult(
            model =
                model,
            outputFile =
                outputFile,
            output =
                output,
        )

        return TrainLocalNutritionMatcherModelResult(
            model =
                model,
            outputFile =
                outputFile.absolutePath,
        )
    }

    private fun calculateDiagnosticScoreImputationValue(
        trainingExamples:
        List<NutritionMatcherTrainingExample>,
    ): Double {

        val availableScores =
            trainingExamples
                .asSequence()
                .filter { example ->
                    example.diagnosticScoreAvailable
                }
                .map { example ->
                    example.diagnosticScore
                }
                .toList()

        require(
            availableScores.isNotEmpty(),
        ) {
            "Training split contains no available diagnostic scores."
        }

        require(
            availableScores.all { score ->
                score.isFinite()
            },
        ) {
            "Training split contains a non-finite diagnostic score."
        }

        return availableScores.average()
    }

    private fun calculateRoleMetrics(
        examples: List<ScaledTrainingExample>,
        coefficients: DoubleArray,
        intercept: Double,
        decisionThreshold: Double,
    ): List<LocalNutritionMatcherRoleMetrics> {

        return examples
            .groupBy { example ->
                example.source.role
            }
            .toSortedMap(
                compareBy { role ->
                    role.name
                },
            )
            .map { (role, roleExamples) ->
                calculateRoleMetrics(
                    role =
                        role,
                    examples =
                        roleExamples,
                    coefficients =
                        coefficients,
                    intercept =
                        intercept,
                    decisionThreshold =
                        decisionThreshold,
                )
            }
    }

    private fun calculateRoleMetrics(
        role: NutritionMatcherTrainingExampleRole,
        examples: List<ScaledTrainingExample>,
        coefficients: DoubleArray,
        intercept: Double,
        decisionThreshold: Double,
    ): LocalNutritionMatcherRoleMetrics {

        var truePositive =
            0

        var falsePositive =
            0

        var trueNegative =
            0

        var falseNegative =
            0

        examples.forEach { example ->

            val probability =
                sigmoid(
                    value =
                        intercept +
                                dot(
                                    left =
                                        coefficients,
                                    right =
                                        example.features,
                                ),
                )

            val predictedPositive =
                probability >=
                        decisionThreshold

            val actualPositive =
                example.target ==
                        1.0

            when {

                predictedPositive &&
                        actualPositive -> {
                    truePositive++
                }

                predictedPositive &&
                        !actualPositive -> {
                    falsePositive++
                }

                !predictedPositive &&
                        actualPositive -> {
                    falseNegative++
                }

                else -> {
                    trueNegative++
                }
            }
        }

        val positiveCount =
            truePositive +
                    falseNegative

        val negativeCount =
            trueNegative +
                    falsePositive

        val precision =
            divide(
                numerator =
                    truePositive,
                denominator =
                    truePositive +
                            falsePositive,
            )

        val recall =
            divide(
                numerator =
                    truePositive,
                denominator =
                    truePositive +
                            falseNegative,
            )

        val falsePositiveRate =
            divide(
                numerator =
                    falsePositive,
                denominator =
                    falsePositive +
                            trueNegative,
            )

        return LocalNutritionMatcherRoleMetrics(
            role =
                role.name,
            exampleCount =
                examples.size,
            positiveCount =
                positiveCount,
            negativeCount =
                negativeCount,
            predictedPositiveCount =
                truePositive +
                        falsePositive,
            truePositive =
                truePositive,
            falsePositive =
                falsePositive,
            trueNegative =
                trueNegative,
            falseNegative =
                falseNegative,
            precision =
                precision,
            recall =
                recall,
            falsePositiveRate =
                falsePositiveRate,
        )
    }

    private fun readDataset(
        datasetFile: File,
    ): NutritionMatcherTrainingDataset {

        return runCatching {
            GsonBuilder()
                .create()
                .fromJson(
                    datasetFile.readText(),
                    NutritionMatcherTrainingDataset::class.java,
                )
        }.getOrElse { throwable ->
            throw IllegalArgumentException(
                "Could not read nutrition matcher training " +
                        "dataset: " +
                        datasetFile.absolutePath,
                throwable,
            )
        }
    }

    private fun splitByCatalogKey(
        examples: List<NutritionMatcherTrainingExample>,
    ): DatasetSplit {

        val grouped =
            examples.groupBy { example ->
                example.catalogKey
            }

        val trainingCatalogKeys =
            sortedSetOf<String>()

        val testCatalogKeys =
            sortedSetOf<String>()

        grouped.keys
            .sorted()
            .forEach { catalogKey ->

                val bucket =
                    stableBucket(
                        value =
                            catalogKey,
                        modulo =
                            SPLIT_MODULO,
                    )

                if (
                    bucket in
                    TEST_BUCKETS
                ) {
                    testCatalogKeys +=
                        catalogKey
                } else {
                    trainingCatalogKeys +=
                        catalogKey
                }
            }

        val trainingExamples =
            examples.filter { example ->
                example.catalogKey in
                        trainingCatalogKeys
            }

        val testExamples =
            examples.filter { example ->
                example.catalogKey in
                        testCatalogKeys
            }

        require(
            trainingCatalogKeys
                .intersect(
                    other =
                        testCatalogKeys,
                )
                .isEmpty(),
        ) {
            "Catalog keys overlap between training and test split."
        }

        return DatasetSplit(
            trainingExamples =
                trainingExamples,
            testExamples =
                testExamples,
            trainingCatalogKeys =
                trainingCatalogKeys,
            testCatalogKeys =
                testCatalogKeys,
        )
    }

    private fun calculateScaling(
        trainingExamples: List<RawTrainingExample>,
    ): FeatureScaling {

        val featureCount =
            featureExtractor
                .featureNames
                .size

        require(
            trainingExamples.all { example ->
                example.features.size ==
                        featureCount
            },
        ) {
            "Unexpected local matcher feature count."
        }

        val means =
            DoubleArray(
                size =
                    featureCount,
            )

        trainingExamples.forEach { example ->
            example.features.forEachIndexed {
                    index,
                    value ->

                means[index] +=
                    value
            }
        }

        means.indices.forEach { index ->
            means[index] /=
                trainingExamples.size.toDouble()
        }

        val variances =
            DoubleArray(
                size =
                    featureCount,
            )

        trainingExamples.forEach { example ->
            example.features.forEachIndexed {
                    index,
                    value ->

                val difference =
                    value -
                            means[index]

                variances[index] +=
                    difference *
                            difference
            }
        }

        val standardDeviations =
            DoubleArray(
                size =
                    featureCount,
            )

        standardDeviations.indices.forEach { index ->

            val variance =
                variances[index] /
                        trainingExamples.size.toDouble()

            val standardDeviation =
                sqrt(
                    x =
                        max(
                            a =
                                variance,
                            b =
                                0.0,
                        ),
                )

            standardDeviations[index] =
                if (
                    standardDeviation <
                    MIN_STANDARD_DEVIATION
                ) {
                    1.0
                } else {
                    standardDeviation
                }
        }

        return FeatureScaling(
            means =
                means,
            standardDeviations =
                standardDeviations,
        )
    }

    private fun calculateClassWeights(
        examples: List<ScaledTrainingExample>,
    ): ClassWeights {

        val positiveCount =
            examples.count { example ->
                example.target ==
                        1.0
            }

        val negativeCount =
            examples.size -
                    positiveCount

        require(
            positiveCount >
                    0,
        )

        require(
            negativeCount >
                    0,
        )

        val total =
            examples.size.toDouble()

        return ClassWeights(
            positive =
                total /
                        (
                                2.0 *
                                        positiveCount.toDouble()
                                ),
            negative =
                total /
                        (
                                2.0 *
                                        negativeCount.toDouble()
                                ),
        )
    }

    private fun fitLogisticRegression(
        examples: List<ScaledTrainingExample>,
        positiveClassWeight: Double,
        negativeClassWeight: Double,
    ): TrainedParameters {

        val featureCount =
            featureExtractor
                .featureNames
                .size

        val coefficients =
            DoubleArray(
                size =
                    featureCount,
            )

        var intercept =
            0.0

        repeat(
            times =
                ITERATION_COUNT,
        ) {

            val coefficientGradients =
                DoubleArray(
                    size =
                        featureCount,
                )

            var interceptGradient =
                0.0

            var totalWeight =
                0.0

            examples.forEach { example ->

                val probability =
                    sigmoid(
                        value =
                            intercept +
                                    dot(
                                        left =
                                            coefficients,
                                        right =
                                            example.features,
                                    ),
                    )

                val classWeight =
                    if (
                        example.target ==
                        1.0
                    ) {
                        positiveClassWeight
                    } else {
                        negativeClassWeight
                    }

                val effectiveWeight =
                    classWeight *
                            example.source.trainingWeight

                val error =
                    probability -
                            example.target

                coefficientGradients.indices
                    .forEach { index ->

                        coefficientGradients[index] +=
                            effectiveWeight *
                                    error *
                                    example.features[index]
                    }

                interceptGradient +=
                    effectiveWeight *
                            error

                totalWeight +=
                    effectiveWeight
            }

            require(
                totalWeight >
                        0.0,
            )

            coefficients.indices.forEach { index ->

                val averageGradient =
                    coefficientGradients[index] /
                            totalWeight

                val regularizationGradient =
                    L2_REGULARIZATION *
                            coefficients[index]

                coefficients[index] -=
                    LEARNING_RATE *
                            (
                                    averageGradient +
                                            regularizationGradient
                                    )
            }

            intercept -=
                LEARNING_RATE *
                        (
                                interceptGradient /
                                        totalWeight
                                )
        }

        return TrainedParameters(
            coefficients =
                coefficients,
            intercept =
                intercept,
        )
    }

    private fun predictProbability(
        example: ScaledTrainingExample,
        coefficients: DoubleArray,
        intercept: Double,
    ): Double {

        require(
            example.features.size ==
                    coefficients.size,
        ) {
            "Nutrition matcher prediction feature count differs " +
                    "from coefficient count."
        }

        return sigmoid(
            value =
                intercept +
                        dot(
                            left =
                                coefficients,
                            right =
                                example.features,
                        ),
        )
    }

    private fun calculateMetrics(
        examples: List<ScaledTrainingExample>,
        coefficients: DoubleArray,
        intercept: Double,
        decisionThreshold: Double,
    ): LocalNutritionMatcherClassificationMetrics {

        var truePositive =
            0

        var falsePositive =
            0

        var trueNegative =
            0

        var falseNegative =
            0

        var totalLogLoss =
            0.0

        examples.forEach { example ->

            val probability =
                sigmoid(
                    value =
                        intercept +
                                dot(
                                    left =
                                        coefficients,
                                    right =
                                        example.features,
                                ),
                )
                    .coerceIn(
                        minimumValue =
                            MIN_PROBABILITY,
                        maximumValue =
                            MAX_PROBABILITY,
                    )

            val predictedPositive =
                probability >=
                        decisionThreshold

            val actualPositive =
                example.target ==
                        1.0

            when {

                predictedPositive &&
                        actualPositive -> {
                    truePositive++
                }

                predictedPositive &&
                        !actualPositive -> {
                    falsePositive++
                }

                !predictedPositive &&
                        actualPositive -> {
                    falseNegative++
                }

                else -> {
                    trueNegative++
                }
            }

            totalLogLoss +=
                -(
                        example.target *
                                ln(
                                    x =
                                        probability,
                                ) +
                                (
                                        1.0 -
                                                example.target
                                        ) *
                                ln(
                                    x =
                                        1.0 -
                                                probability,
                                )
                        )
        }

        val positiveCount =
            truePositive +
                    falseNegative

        val negativeCount =
            trueNegative +
                    falsePositive

        val accuracy =
            divide(
                numerator =
                    truePositive +
                            trueNegative,
                denominator =
                    examples.size,
            )

        val precision =
            divide(
                numerator =
                    truePositive,
                denominator =
                    truePositive +
                            falsePositive,
            )

        val recall =
            divide(
                numerator =
                    truePositive,
                denominator =
                    truePositive +
                            falseNegative,
            )

        val specificity =
            divide(
                numerator =
                    trueNegative,
                denominator =
                    trueNegative +
                            falsePositive,
            )

        val f1 =
            if (
                precision +
                recall ==
                0.0
            ) {
                0.0
            } else {
                2.0 *
                        precision *
                        recall /
                        (
                                precision +
                                        recall
                                )
            }

        return LocalNutritionMatcherClassificationMetrics(
            exampleCount =
                examples.size,
            positiveCount =
                positiveCount,
            negativeCount =
                negativeCount,
            truePositive =
                truePositive,
            falsePositive =
                falsePositive,
            trueNegative =
                trueNegative,
            falseNegative =
                falseNegative,
            accuracy =
                accuracy,
            precision =
                precision,
            recall =
                recall,
            f1 =
                f1,
            balancedAccuracy =
                (
                        recall +
                                specificity
                        ) /
                        2.0,
            averageLogLoss =
                if (
                    examples.isEmpty()
                ) {
                    0.0
                } else {
                    totalLogLoss /
                            examples.size.toDouble()
                },
        )
    }

    private fun validateSupportedFeatureContract(
        featureNames: List<String>,
    ) {

        require(
            featureNames.isNotEmpty(),
        ) {
            "Local nutrition matcher trainer feature contract must not " +
                    "be empty."
        }

        require(
            featureNames.distinct().size ==
                    featureNames.size,
        ) {
            "Local nutrition matcher trainer feature contract contains " +
                    "duplicate feature names: " +
                    featureNames.joinToString()
        }

        val isProductionFeatureContract =
            featureNames ==
                    LocalNutritionMatcherFeatureContract
                        .ACTIVE_FEATURE_NAMES

        if (isProductionFeatureContract) {
            LocalNutritionMatcherFeatureContract
                .validateTrainingFeatureSubset(
                    featureNames =
                        featureNames,
                )

            return
        }

        LocalNutritionMatcherFeatureContract
            .validateOptimizationFeatureSubset(
                featureNames =
                    featureNames,
            )
    }

    private fun validateModel(
        model: LocalNutritionMatcherModel,
    ) {

        require(
            model.featureNames ==
                    supportedFeatureNames,
        ) {
            "Persisted model differs from trainer feature contract. " +
                    "Expected: " +
                    supportedFeatureNames.joinToString() +
                    "; actual: " +
                    model.featureNames.joinToString()
        }

        validateSupportedFeatureContract( featureNames = model.featureNames, )

        require(
            model.version ==
                    LocalNutritionMatcherModelContract
                        .CURRENT_VERSION,
        ) {
            "Unexpected local nutrition matcher model version: " +
                    model.version
        }

        require(
            model.decisionThreshold.isFinite() &&
                    model.decisionThreshold in
                    0.0..1.0,
        ) {
            "Local nutrition matcher decision threshold must be finite " +
                    "and between zero and one."
        }

        val thresholdMetadata =
            model.decisionThresholdOptimization

        require(
            thresholdMetadata.calibrationExampleCount ==
                    model.training.trainingExampleCount,
        ) {
            "Threshold calibration example count differs from the " +
                    "training example count."
        }

        require(
            thresholdMetadata.minimumPrecision in
                    0.0..1.0,
        ) {
            "Threshold minimum precision must be between zero and one."
        }

        require(
            thresholdMetadata.maximumFalsePositiveRate in
                    0.0..1.0,
        ) {
            "Threshold maximum false-positive rate must be between " +
                    "zero and one."
        }

        require(
            thresholdMetadata.minimumPredictedPositiveCount >
                    0,
        ) {
            "Threshold minimum predicted-positive count must be positive."
        }

        require(
            thresholdMetadata.evaluatedThresholdCount >
                    0,
        ) {
            "Threshold optimizer must evaluate at least one threshold."
        }

        require(
            thresholdMetadata.selectedPrecision in
                    0.0..1.0,
        ) {
            "Selected threshold precision must be between zero and one."
        }

        require(
            thresholdMetadata.selectedRecall in
                    0.0..1.0,
        ) {
            "Selected threshold recall must be between zero and one."
        }

        require(
            thresholdMetadata.selectedFalsePositiveRate in
                    0.0..1.0,
        ) {
            "Selected threshold false-positive rate must be between " +
                    "zero and one."
        }

        require(
            thresholdMetadata.selectedF1 in
                    0.0..1.0,
        ) {
            "Selected threshold F1 must be between zero and one."
        }

        require(
            thresholdMetadata.selectedTruePositiveCount >=
                    0 &&
                    thresholdMetadata.selectedFalsePositiveCount >=
                    0 &&
                    thresholdMetadata.selectedTrueNegativeCount >=
                    0 &&
                    thresholdMetadata.selectedFalseNegativeCount >=
                    0,
        ) {
            "Selected threshold confusion-matrix counts must not be " +
                    "negative."
        }

        require(
            thresholdMetadata.selectedTruePositiveCount +
                    thresholdMetadata.selectedFalsePositiveCount +
                    thresholdMetadata.selectedTrueNegativeCount +
                    thresholdMetadata.selectedFalseNegativeCount ==
                    thresholdMetadata.calibrationExampleCount,
        ) {
            "Selected threshold confusion matrix does not cover every " +
                    "calibration example."
        }
    }

    private fun writeModel(
        model: LocalNutritionMatcherModel,
        outputFile: File,
    ) {

        outputFile.parentFile
            ?.let { directory ->

                if (
                    !directory.exists()
                ) {
                    check(
                        directory.mkdirs(),
                    ) {
                        "Could not create local matcher model " +
                                "directory: " +
                                directory.absolutePath
                    }
                }

                require(
                    directory.isDirectory,
                ) {
                    "Local matcher model parent path is not " +
                            "a directory: " +
                            directory.absolutePath
                }
            }

        val gson =
            GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()

        outputFile.writeText(
            text =
                gson.toJson(
                    model,
                ) +
                        "\n",
        )
    }

    private fun printResult(
        model: LocalNutritionMatcherModel,
        outputFile: File,
        output: PrintStream,
    ) {

        val training =
            model.metrics.training

        val test =
            model.metrics.test

        output.println()
        output.println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        )
        output.println(
            "LOCAL NUTRITION MATCHER MODEL",
        )
        output.println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        )
        output.println(
            "Model                    : " +
                    model.modelType,
        )
        output.println(
            "Features                 : " +
                    model.featureNames.size,
        )
        output.println(
            "Training examples        : " +
                    training.exampleCount,
        )
        output.println(
            "Test examples            : " +
                    test.exampleCount,
        )
        output.println()
        output.println(
            "Decision threshold       : " +
                    format(
                        value =
                            model.decisionThreshold,
                    ),
        )
        output.println(
            "Threshold policy met     : " +
                    model
                        .decisionThresholdOptimization
                        .policySatisfied,
        )
        output.println()
        output.println(
            "TRAIN precision          : " +
                    format(
                        value =
                            training.precision,
                    ),
        )
        output.println(
            "TRAIN recall             : " +
                    format(
                        value =
                            training.recall,
                    ),
        )
        output.println(
            "TRAIN F1                 : " +
                    format(
                        value =
                            training.f1,
                    ),
        )
        output.println(
            "TRAIN balanced accuracy  : " +
                    format(
                        value =
                            training.balancedAccuracy,
                    ),
        )
        output.println()
        output.println(
            "TEST precision           : " +
                    format(
                        value =
                            test.precision,
                    ),
        )
        output.println(
            "TEST recall              : " +
                    format(
                        value =
                            test.recall,
                    ),
        )
        output.println(
            "TEST F1                  : " +
                    format(
                        value =
                            test.f1,
                    ),
        )
        output.println(
            "TEST balanced accuracy   : " +
                    format(
                        value =
                            test.balancedAccuracy,
                    ),
        )
        output.println()
        output.println(
            "Output                   : " +
                    outputFile.absolutePath,
        )
        output.println()
        output.println(
            "TEST METRICS BY ROLE",
        )

        model.metrics.testByRole
            .forEach { role ->
                output.println(
                    "${role.role.padEnd(29)} " +
                            "count=${role.exampleCount} " +
                            "precision=${format(role.precision)} " +
                            "recall=${format(role.recall)} " +
                            "fpr=${format(role.falsePositiveRate)}",
                )
            }

        output.println()
        output.println(
            "Diagnostic score mean    : " +
                    format(
                        value =
                            model.diagnosticScoreImputationValue,
                    ),
        )
        output.println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
        )
    }

    private fun RawTrainingExample.toScaled(
        scaling: FeatureScaling,
    ): ScaledTrainingExample {

        val scaledFeatures =
            DoubleArray(
                size =
                    features.size,
            )

        features.indices.forEach { index ->
            scaledFeatures[index] =
                (
                        features[index] -
                                scaling.means[index]
                        ) /
                        scaling.standardDeviations[index]
        }

        return ScaledTrainingExample(
            source =
                source,
            features =
                scaledFeatures,
            target =
                target,
        )
    }

    private fun targetOf(
        example: NutritionMatcherTrainingExample,
    ): Double {

        return if (
            example.label ==
            NutritionMatcherTrainingLabel.POSITIVE
        ) {
            1.0
        } else {
            0.0
        }
    }

    private fun stableBucket(
        value: String,
        modulo: Int,
    ): Int {

        require(
            modulo >
                    0,
        )

        val digest =
            MessageDigest
                .getInstance(
                    "SHA-256",
                )
                .digest(
                    value.toByteArray(
                        charset =
                            StandardCharsets.UTF_8,
                    ),
                )

        val unsigned =
            (
                    (
                            digest[0].toInt() and
                                    0xff
                            ) shl
                            24
                    ) or
                    (
                            (
                                    digest[1].toInt() and
                                            0xff
                                    ) shl
                                    16
                            ) or
                    (
                            (
                                    digest[2].toInt() and
                                            0xff
                                    ) shl
                                    8
                            ) or
                    (
                            digest[3].toInt() and
                                    0xff
                            )

        return (
                unsigned and
                        Int.MAX_VALUE
                ) %
                modulo
    }

    private fun sigmoid(
        value: Double,
    ): Double {

        return when {

            value >=
                    0.0 -> {
                1.0 /
                        (
                                1.0 +
                                        exp(
                                            x =
                                                -value,
                                        )
                                )
            }

            else -> {
                val exponential =
                    exp(
                        x =
                            value,
                    )

                exponential /
                        (
                                1.0 +
                                        exponential
                                )
            }
        }
    }

    private fun dot(
        left: DoubleArray,
        right: DoubleArray,
    ): Double {

        require(
            left.size ==
                    right.size,
        )

        var result =
            0.0

        left.indices.forEach { index ->
            result +=
                left[index] *
                        right[index]
        }

        return result
    }

    private fun divide(
        numerator: Int,
        denominator: Int,
    ): Double {

        if (
            denominator ==
            0
        ) {
            return 0.0
        }

        return numerator.toDouble() /
                denominator.toDouble()
    }

    private fun format(
        value: Double,
    ): String {

        return "%.4f".format(
            locale =
                java.util.Locale.ROOT,
            value,
        )
    }

    private data class DatasetSplit(
        val trainingExamples:
        List<NutritionMatcherTrainingExample>,
        val testExamples:
        List<NutritionMatcherTrainingExample>,
        val trainingCatalogKeys:
        Set<String>,
        val testCatalogKeys:
        Set<String>,
    )

    private data class RawTrainingExample(
        val source:
        NutritionMatcherTrainingExample,
        val features:
        DoubleArray,
        val target:
        Double,
    )

    private data class ScaledTrainingExample(
        val source:
        NutritionMatcherTrainingExample,
        val features:
        DoubleArray,
        val target:
        Double,
    )

    private data class FeatureScaling(
        val means:
        DoubleArray,
        val standardDeviations:
        DoubleArray,
    )

    private data class ClassWeights(
        val positive:
        Double,
        val negative:
        Double,
    )

    private data class TrainedParameters(
        val coefficients:
        DoubleArray,
        val intercept:
        Double,
    )

    companion object {

        const val DATASET_VERSION =
            1

        const val EXPECTED_DATASET_TYPE =
            "NUTRITION_CATALOG_SERVER_MATCHER"

        const val LEARNING_RATE =
            0.05

        const val ITERATION_COUNT =
            3000

        const val L2_REGULARIZATION =
            0.001

        const val SPLIT_MODULO =
            10

        val TEST_BUCKETS =
            setOf(
                0,
                1,
            )

        const val MIN_STANDARD_DEVIATION =
            1e-12

        const val MIN_PROBABILITY =
            1e-12

        const val MAX_PROBABILITY =
            1.0 -
                    1e-12

        const val MINIMUM_THRESHOLD_PRECISION =
            0.95

        const val MAXIMUM_THRESHOLD_FALSE_POSITIVE_RATE =
            0.02

        const val MINIMUM_THRESHOLD_PREDICTED_POSITIVE_COUNT =
            5
    }
}