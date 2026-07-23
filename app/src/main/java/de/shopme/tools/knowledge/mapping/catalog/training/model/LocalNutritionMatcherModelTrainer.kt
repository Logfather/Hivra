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
                LocalNutritionMatcherFeatureExtractor.BASE_FEATURE_NAMES,
        ),
    private val supportedFeatureNames: List<String> =
        featureExtractor.featureNames,
) {



    init {
        require(
            supportedFeatureNames.isNotEmpty(),
        ) {
            "Supported local nutrition matcher feature names must not be empty."
        }

        require(
            supportedFeatureNames.distinct().size ==
                    supportedFeatureNames.size,
        ) {
            "Supported local nutrition matcher feature names must not contain duplicates."
        }

        val knownFeatureNames =
            (
                    LocalNutritionMatcherFeatureExtractor.BASE_FEATURE_NAMES +
                            LocalNutritionMatcherFeatureExtractor.DOMAIN_MISMATCH_FEATURE_NAMES
                    )
                .toSet()

        val unknownSupportedFeatureNames =
            supportedFeatureNames
                .filterNot {
                    it in knownFeatureNames
                }

        require(
            unknownSupportedFeatureNames.isEmpty(),
        ) {
            "Supported local nutrition matcher feature names contain unknown features: " +
                    unknownSupportedFeatureNames
                        .sorted()
                        .joinToString()
        }

        require(
            featureExtractor.featureNames ==
                    supportedFeatureNames,
        ) {
            "Local nutrition matcher feature extractor and supported feature contract differ. " +
                    "Extractor features: " +
                    featureExtractor.featureNames.joinToString() +
                    "; supported features: " +
                    supportedFeatureNames.joinToString()
        }
    }

    fun train(
        datasetFile: File,
        outputFile: File,
        output: PrintStream = System.out
    ): TrainLocalNutritionMatcherModelResult {

        require(datasetFile.isFile) {
            "Nutrition matcher training dataset does not exist: " +
                    datasetFile.absolutePath
        }

        val dataset =
            readDataset(
                datasetFile = datasetFile
            )

        require(dataset.version == DATASET_VERSION) {
            "Unsupported nutrition matcher dataset version: " +
                    dataset.version
        }

        require(
            dataset.datasetType ==
                    EXPECTED_DATASET_TYPE
        ) {
            "Unsupported nutrition matcher dataset type: " +
                    dataset.datasetType
        }

        require(dataset.examples.isNotEmpty()) {
            "Nutrition matcher training dataset is empty."
        }

        require(
            dataset.examples.any {
                it.label ==
                        NutritionMatcherTrainingLabel.POSITIVE
            }
        ) {
            "Nutrition matcher training dataset has no " +
                    "positive examples."
        }

        require(
            dataset.examples.any {
                it.label ==
                        NutritionMatcherTrainingLabel.NEGATIVE
            }
        ) {
            "Nutrition matcher training dataset has no " +
                    "negative examples."
        }

        val split =
            splitByCatalogKey(
                examples =
                    dataset.examples
            )

        require(
            split.trainingExamples.isNotEmpty()
        ) {
            "Local matcher training split is empty."
        }

        require(
            split.testExamples.isNotEmpty()
        ) {
            "Local matcher test split is empty."
        }

        require(
            split.trainingExamples.any {
                it.label ==
                        NutritionMatcherTrainingLabel.POSITIVE
            }
        ) {
            "Training split has no positive examples."
        }

        require(
            split.trainingExamples.any {
                it.label ==
                        NutritionMatcherTrainingLabel.NEGATIVE
            }
        ) {
            "Training split has no negative examples."
        }

        val diagnosticScoreImputationValue =
            calculateDiagnosticScoreImputationValue(
                trainingExamples =
                    split.trainingExamples
            )

        val trainingRaw =
            split.trainingExamples.map {
                RawTrainingExample(
                    source =
                        it,
                    features =
                        featureExtractor.extract(
                            example =
                                it,
                            diagnosticScoreImputationValue =
                                diagnosticScoreImputationValue
                        ),
                    target =
                        targetOf(it)
                )
            }

        val testRaw =
            split.testExamples.map {
                RawTrainingExample(
                    source =
                        it,
                    features =
                        featureExtractor.extract(
                            example =
                                it,
                            diagnosticScoreImputationValue =
                                diagnosticScoreImputationValue
                        ),
                    target =
                        targetOf(it)
                )
            }

        val scaling =
            calculateScaling(
                trainingExamples =
                    trainingRaw
            )

        val trainingExamples =
            trainingRaw.map {
                it.toScaled(
                    scaling = scaling
                )
            }

        val testExamples =
            testRaw.map {
                it.toScaled(
                    scaling = scaling
                )
            }

        val classWeights =
            calculateClassWeights(
                examples =
                    trainingExamples
            )

        val trained =
            fitLogisticRegression(
                examples =
                    trainingExamples,
                positiveClassWeight =
                    classWeights.positive,
                negativeClassWeight =
                    classWeights.negative
            )

        val trainingMetrics =
            calculateMetrics(
                examples =
                    trainingExamples,
                coefficients =
                    trained.coefficients,
                intercept =
                    trained.intercept
            )

        val testMetrics =
            calculateMetrics(
                examples =
                    testExamples,
                coefficients =
                    trained.coefficients,
                intercept =
                    trained.intercept
            )

        val trainingByRole =
            calculateRoleMetrics(
                examples =
                    trainingExamples,
                coefficients =
                    trained.coefficients,
                intercept =
                    trained.intercept
            )

        val testByRole =
            calculateRoleMetrics(
                examples =
                    testExamples,
                coefficients =
                    trained.coefficients,
                intercept =
                    trained.intercept
            )

        val model =
            LocalNutritionMatcherModel(
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
                    DECISION_THRESHOLD,
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
                            TEST_BUCKETS.sorted()
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
                            testByRole
                    )
            )

        validateModel(
            model = model
        )

        writeModel(
            model = model,
            outputFile = outputFile
        )

        printResult(
            model = model,
            outputFile = outputFile,
            output = output
        )

        return TrainLocalNutritionMatcherModelResult(
            model = model,
            outputFile = outputFile.absolutePath
        )
    }

    private fun calculateDiagnosticScoreImputationValue(
        trainingExamples:
        List<NutritionMatcherTrainingExample>
    ): Double {

        val availableScores =
            trainingExamples
                .asSequence()
                .filter {
                    it.diagnosticScoreAvailable
                }
                .map {
                    it.diagnosticScore
                }
                .toList()

        require(availableScores.isNotEmpty()) {
            "Training split contains no available diagnostic scores."
        }

        require(
            availableScores.all {
                it.isFinite()
            }
        ) {
            "Training split contains a non-finite diagnostic score."
        }

        return availableScores.average()
    }

    private fun calculateRoleMetrics(
        examples: List<ScaledTrainingExample>,
        coefficients: DoubleArray,
        intercept: Double
    ): List<LocalNutritionMatcherRoleMetrics> {

        return examples
            .groupBy {
                it.source.role
            }
            .toSortedMap(
                compareBy {
                    it.name
                }
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
                        intercept
                )
            }
    }

    private fun calculateRoleMetrics(
        role: NutritionMatcherTrainingExampleRole,
        examples: List<ScaledTrainingExample>,
        coefficients: DoubleArray,
        intercept: Double
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
                    intercept +
                            dot(
                                coefficients,
                                example.features
                            )
                )

            val predictedPositive =
                probability >=
                        DECISION_THRESHOLD

            val actualPositive =
                example.target == 1.0

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
            truePositive + falseNegative

        val negativeCount =
            trueNegative + falsePositive

        val precision =
            divide(
                numerator =
                    truePositive,
                denominator =
                    truePositive + falsePositive
            )

        val recall =
            divide(
                numerator =
                    truePositive,
                denominator =
                    truePositive + falseNegative
            )

        val falsePositiveRate =
            divide(
                numerator =
                    falsePositive,
                denominator =
                    falsePositive + trueNegative
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
                truePositive + falsePositive,
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
                falsePositiveRate
        )
    }

    private fun readDataset(
        datasetFile: File
    ): NutritionMatcherTrainingDataset {

        return runCatching {

            GsonBuilder()
                .create()
                .fromJson(
                    datasetFile.readText(),
                    NutritionMatcherTrainingDataset::class.java
                )

        }.getOrElse { throwable ->

            throw IllegalArgumentException(
                "Could not read nutrition matcher training " +
                        "dataset: " +
                        datasetFile.absolutePath,
                throwable
            )
        }
    }

    private fun splitByCatalogKey(
        examples: List<NutritionMatcherTrainingExample>
    ): DatasetSplit {

        val grouped =
            examples.groupBy {
                it.catalogKey
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
                        value = catalogKey,
                        modulo = SPLIT_MODULO
                    )

                if (bucket in TEST_BUCKETS) {
                    testCatalogKeys += catalogKey
                } else {
                    trainingCatalogKeys += catalogKey
                }
            }

        val trainingExamples =
            examples.filter {
                it.catalogKey in trainingCatalogKeys
            }

        val testExamples =
            examples.filter {
                it.catalogKey in testCatalogKeys
            }

        require(
            trainingCatalogKeys
                .intersect(testCatalogKeys)
                .isEmpty()
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
                testCatalogKeys
        )
    }

    private fun calculateScaling(
        trainingExamples: List<RawTrainingExample>
    ): FeatureScaling {

        val featureCount =
            featureExtractor.featureNames.size

        require(
            trainingExamples.all {
                it.features.size == featureCount
            }
        ) {
            "Unexpected local matcher feature count."
        }

        val means =
            DoubleArray(featureCount)

        trainingExamples.forEach { example ->

            example.features.forEachIndexed {
                    index,
                    value ->

                means[index] += value
            }
        }

        means.indices.forEach { index ->

            means[index] /=
                trainingExamples.size.toDouble()
        }

        val variances =
            DoubleArray(featureCount)

        trainingExamples.forEach { example ->

            example.features.forEachIndexed {
                    index,
                    value ->

                val difference =
                    value - means[index]

                variances[index] +=
                    difference * difference
            }
        }

        val standardDeviations =
            DoubleArray(featureCount)

        standardDeviations.indices.forEach { index ->

            val variance =
                variances[index] /
                        trainingExamples.size.toDouble()

            val standardDeviation =
                sqrt(
                    max(
                        variance,
                        0.0
                    )
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
                standardDeviations
        )
    }

    private fun calculateClassWeights(
        examples: List<ScaledTrainingExample>
    ): ClassWeights {

        val positiveCount =
            examples.count {
                it.target == 1.0
            }

        val negativeCount =
            examples.size - positiveCount

        require(positiveCount > 0)
        require(negativeCount > 0)

        val total =
            examples.size.toDouble()

        return ClassWeights(
            positive =
                total /
                        (2.0 * positiveCount.toDouble()),
            negative =
                total /
                        (2.0 * negativeCount.toDouble())
        )
    }

    private fun fitLogisticRegression(
        examples: List<ScaledTrainingExample>,
        positiveClassWeight: Double,
        negativeClassWeight: Double
    ): TrainedParameters {

        val featureCount =
            featureExtractor.featureNames.size

        val coefficients =
            DoubleArray(featureCount)

        var intercept =
            0.0

        repeat(ITERATION_COUNT) {

            val coefficientGradients =
                DoubleArray(featureCount)

            var interceptGradient =
                0.0

            var totalWeight =
                0.0

            examples.forEach { example ->

                val probability =
                    sigmoid(
                        intercept +
                                dot(
                                    coefficients,
                                    example.features
                                )
                    )

                val classWeight =
                    if (example.target == 1.0) {
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
                    effectiveWeight * error

                totalWeight +=
                    effectiveWeight
            }

            require(totalWeight > 0.0)

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
                intercept
        )
    }

    private fun calculateMetrics(
        examples: List<ScaledTrainingExample>,
        coefficients: DoubleArray,
        intercept: Double
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
                    intercept +
                            dot(
                                coefficients,
                                example.features
                            )
                )
                    .coerceIn(
                        MIN_PROBABILITY,
                        MAX_PROBABILITY
                    )

            val predictedPositive =
                probability >=
                        DECISION_THRESHOLD

            val actualPositive =
                example.target == 1.0

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
                                ln(probability) +
                                (1.0 - example.target) *
                                ln(1.0 - probability)
                        )
        }

        val positiveCount =
            truePositive + falseNegative

        val negativeCount =
            trueNegative + falsePositive

        val accuracy =
            divide(
                numerator =
                    truePositive + trueNegative,
                denominator =
                    examples.size
            )

        val precision =
            divide(
                numerator =
                    truePositive,
                denominator =
                    truePositive + falsePositive
            )

        val recall =
            divide(
                numerator =
                    truePositive,
                denominator =
                    truePositive + falseNegative
            )

        val specificity =
            divide(
                numerator =
                    trueNegative,
                denominator =
                    trueNegative + falsePositive
            )

        val f1 =
            if (
                precision + recall == 0.0
            ) {
                0.0
            } else {
                2.0 *
                        precision *
                        recall /
                        (precision + recall)
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
                (recall + specificity) / 2.0,
            averageLogLoss =
                if (examples.isEmpty()) {
                    0.0
                } else {
                    totalLogLoss /
                            examples.size.toDouble()
                }
        )
    }

    private fun validateModel(
        model: LocalNutritionMatcherModel,
    ) {
        val featureCount =
            model.featureNames.size

        require(model.version == MODEL_VERSION) {
            "Unsupported local nutrition matcher model version: " +
                    model.version
        }

        require(
            model.diagnosticScoreImputationValue.isFinite(),
        ) {
            "Diagnostic score imputation value must be finite."
        }

        require(
            model.featureNames.isNotEmpty(),
        ) {
            "Local nutrition matcher model must contain at least one feature."
        }

        require(
            model.featureNames.size ==
                    model.featureNames.distinct().size,
        ) {
            "Local nutrition matcher model must not contain duplicate features."
        }

        require(
            "diagnostic_score_available" !in
                    model.featureNames,
        ) {
            "diagnostic_score_available must not be a model feature."
        }

        require(
            "domain_feature_version" !in
                    model.featureNames,
        ) {
            "Domain-Mismatch schema version must not be a model feature."
        }

        require(
            "domain_report_relationship_present" !in
                    model.featureNames,
        ) {
            "Domain-Mismatch report availability must not be a model feature."
        }

        require(
            model.featureNames ==
                    featureExtractor.featureNames,
        ) {
            "Persisted local nutrition matcher feature contract " +
                    "differs from the active feature extractor."
        }

        val supportedFeatureNames =
            featureExtractor.featureNames

        val supportedFeatureNameSet =
            supportedFeatureNames.toSet()

        val unsupportedFeatureNames =
            model.featureNames
                .filterNot {
                    it in supportedFeatureNameSet
                }

        require(
            unsupportedFeatureNames.isEmpty(),
        ) {
            "Local nutrition matcher model contains unsupported features: " +
                    unsupportedFeatureNames
                        .sorted()
                        .joinToString()
        }

        require(
            model.featureNames ==
                    featureExtractor.featureNames,
        ) {
            "Local nutrition matcher model feature contract differs from the feature extractor. " +
                    "Expected: " +
                    featureExtractor.featureNames.joinToString() +
                    "; actual: " +
                    model.featureNames.joinToString()
        }

        val expectedBaseFeatureNames =
            LocalNutritionMatcherFeatureExtractor
                .BASE_FEATURE_NAMES

        require(
            featureCount >=
                    expectedBaseFeatureNames.size,
        ) {
            "Local nutrition matcher model contains fewer features " +
                    "than the required base-feature contract."
        }

        val actualBaseFeatureNames =
            model.featureNames.take(
                LocalNutritionMatcherFeatureExtractor
                    .BASE_FEATURE_COUNT,
            )

        require(
            actualBaseFeatureNames ==
                    expectedBaseFeatureNames,
        ) {
            "The Nutrition base-feature prefix differs " +
                    "from the expected contract."
        }

        val actualDomainFeatureNames =
            model.featureNames.drop(
                LocalNutritionMatcherFeatureExtractor
                    .BASE_FEATURE_COUNT,
            )

        val supportedDomainFeatureNames =
            LocalNutritionMatcherFeatureExtractor
                .DOMAIN_MISMATCH_FEATURE_NAMES

        val unsupportedDomainFeatureNames =
            actualDomainFeatureNames
                .filterNot {
                    it in supportedDomainFeatureNames
                }

        require(
            unsupportedDomainFeatureNames.isEmpty(),
        ) {
            "The Nutrition model contains unsupported " +
                    "Domain-Mismatch features: " +
                    unsupportedDomainFeatureNames
                        .sorted()
                        .joinToString()
        }

        require(
            actualDomainFeatureNames.size ==
                    actualDomainFeatureNames.distinct().size,
        ) {
            "The Nutrition model contains duplicate " +
                    "Domain-Mismatch features."
        }

        val expectedDomainFeatureOrder =
            supportedDomainFeatureNames
                .filter {
                    it in actualDomainFeatureNames
                }

        require(
            actualDomainFeatureNames ==
                    expectedDomainFeatureOrder,
        ) {
            "The Nutrition Domain-Mismatch features differ " +
                    "from the expected deterministic order."
        }

        require(
            model.featureMeans.size ==
                    featureCount,
        ) {
            "Feature-mean count differs from feature count."
        }

        require(
            model.featureStandardDeviations.size ==
                    featureCount,
        ) {
            "Feature-standard-deviation count differs from feature count."
        }

        require(
            model.coefficients.size ==
                    featureCount,
        ) {
            "Coefficient count differs from feature count."
        }

        require(
            model.featureMeans.all {
                it.isFinite()
            },
        ) {
            "All feature means must be finite."
        }

        require(
            model.featureStandardDeviations.all {
                it.isFinite() &&
                        it > 0.0
            },
        ) {
            "All feature standard deviations must be finite and positive."
        }

        require(
            model.coefficients.all {
                it.isFinite()
            },
        ) {
            "All coefficients must be finite."
        }

        require(
            model.intercept.isFinite(),
        ) {
            "Model intercept must be finite."
        }

        require(
            model.decisionThreshold in 0.0..1.0,
        ) {
            "Decision threshold must be between 0.0 and 1.0."
        }
    }

    private fun writeModel(
        model: LocalNutritionMatcherModel,
        outputFile: File
    ) {
        outputFile.parentFile
            ?.let { directory ->

                if (!directory.exists()) {
                    check(directory.mkdirs()) {
                        "Could not create local matcher model " +
                                "directory: " +
                                directory.absolutePath
                    }
                }

                require(directory.isDirectory) {
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
            gson.toJson(model) + "\n"
        )
    }

    private fun printResult(
        model: LocalNutritionMatcherModel,
        outputFile: File,
        output: PrintStream
    ) {
        val training =
            model.metrics.training

        val test =
            model.metrics.test

        output.println()
        output.println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        )
        output.println(
            "LOCAL NUTRITION MATCHER MODEL"
        )
        output.println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        )
        output.println(
            "Model                    : " +
                    model.modelType
        )
        output.println(
            "Features                 : " +
                    model.featureNames.size
        )
        output.println(
            "Training examples        : " +
                    training.exampleCount
        )
        output.println(
            "Test examples            : " +
                    test.exampleCount
        )
        output.println()
        output.println(
            "TRAIN precision          : " +
                    format(training.precision)
        )
        output.println(
            "TRAIN recall             : " +
                    format(training.recall)
        )
        output.println(
            "TRAIN F1                 : " +
                    format(training.f1)
        )
        output.println(
            "TRAIN balanced accuracy  : " +
                    format(
                        training.balancedAccuracy
                    )
        )
        output.println()
        output.println(
            "TEST precision           : " +
                    format(test.precision)
        )
        output.println(
            "TEST recall              : " +
                    format(test.recall)
        )
        output.println(
            "TEST F1                  : " +
                    format(test.f1)
        )
        output.println(
            "TEST balanced accuracy   : " +
                    format(
                        test.balancedAccuracy
                    )
        )
        output.println()
        output.println(
            "Output                   : " +
                    outputFile.absolutePath
        )
        output.println()
        output.println(
            "TEST METRICS BY ROLE"
        )

        model.metrics.testByRole
            .forEach { role ->

                output.println(
                    "${role.role.padEnd(29)} " +
                            "count=${role.exampleCount} " +
                            "precision=${format(role.precision)} " +
                            "recall=${format(role.recall)} " +
                            "fpr=${format(role.falsePositiveRate)}"
                )
            }
        output.println()
        output.println(
            "Diagnostic score mean    : " +
                    format(
                        model.diagnosticScoreImputationValue
                    )
        )
        output.println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        )
    }

    private fun RawTrainingExample.toScaled(
        scaling: FeatureScaling
    ): ScaledTrainingExample {

        val scaledFeatures =
            DoubleArray(features.size)

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
                target
        )
    }

    private fun targetOf(
        example: NutritionMatcherTrainingExample
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
        modulo: Int
    ): Int {

        require(modulo > 0)

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

        return (unsigned and Int.MAX_VALUE) % modulo
    }

    private fun sigmoid(
        value: Double
    ): Double {

        return when {

            value >= 0.0 -> {
                1.0 /
                        (
                                1.0 +
                                        exp(-value)
                                )
            }

            else -> {
                val exponential =
                    exp(value)

                exponential /
                        (1.0 + exponential)
            }
        }
    }

    private fun dot(
        left: DoubleArray,
        right: DoubleArray
    ): Double {

        require(left.size == right.size)

        var result =
            0.0

        left.indices.forEach { index ->

            result +=
                left[index] * right[index]
        }

        return result
    }

    private fun divide(
        numerator: Int,
        denominator: Int
    ): Double {

        if (denominator == 0) {
            return 0.0
        }

        return numerator.toDouble() /
                denominator.toDouble()
    }

    private fun format(
        value: Double
    ): String {

        return "%.4f".format(
            java.util.Locale.ROOT,
            value
        )
    }

    private data class DatasetSplit(
        val trainingExamples:
        List<NutritionMatcherTrainingExample>,
        val testExamples:
        List<NutritionMatcherTrainingExample>,
        val trainingCatalogKeys: Set<String>,
        val testCatalogKeys: Set<String>
    )

    private data class RawTrainingExample(
        val source: NutritionMatcherTrainingExample,
        val features: DoubleArray,
        val target: Double
    )

    private data class ScaledTrainingExample(
        val source: NutritionMatcherTrainingExample,
        val features: DoubleArray,
        val target: Double
    )

    private data class FeatureScaling(
        val means: DoubleArray,
        val standardDeviations: DoubleArray
    )

    private data class ClassWeights(
        val positive: Double,
        val negative: Double
    )

    private data class TrainedParameters(
        val coefficients: DoubleArray,
        val intercept: Double
    )

    private companion object {

        private const val MODEL_VERSION =
            2

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

        const val DECISION_THRESHOLD =
            0.5

        const val SPLIT_MODULO =
            10

        val TEST_BUCKETS =
            setOf(
                0,
                1
            )

        const val MIN_STANDARD_DEVIATION =
            1e-12

        const val MIN_PROBABILITY =
            1e-12

        const val MAX_PROBABILITY =
            1.0 - 1e-12
    }
}