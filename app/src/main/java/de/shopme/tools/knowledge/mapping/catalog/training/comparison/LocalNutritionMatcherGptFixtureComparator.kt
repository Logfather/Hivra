package de.shopme.tools.knowledge.mapping.catalog.training.comparison

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingDataset
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingExample
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingExampleRole
import de.shopme.tools.knowledge.mapping.catalog.training.NutritionMatcherTrainingLabel
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherFeatureContract
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherModel
import de.shopme.tools.knowledge.mapping.catalog.training.model.LocalNutritionMatcherPredictor
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionMatcherConservativeThresholdPolicyContract
import java.io.File
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

class LocalNutritionMatcherGptFixtureComparator {

    fun run(
        datasetFile: File,
        modelFile: File,
        outputFile: File,
        output: PrintStream = System.out
    ): CompareLocalNutritionMatcherAgainstGptFixturesResult {

        require(datasetFile.isFile) {
            "Nutrition matcher training dataset does not exist: " +
                    datasetFile.absolutePath
        }

        require(modelFile.isFile) {
            "Local nutrition matcher model does not exist: " +
                    modelFile.absolutePath
        }

        val dataset =
            readDataset(
                file = datasetFile
            )

        val model =
            readModel(
                file = modelFile
            )

        validateContracts(
            dataset = dataset,
            model = model
        )

        val predictor =
            LocalNutritionMatcherPredictor(
                model = model
            )

        val testExamples =
            dataset.examples
                .asSequence()
                .filter { example ->
                    isTestCatalogKey(
                        catalogKey = example.catalogKey,
                        model = model
                    )
                }
                .map { example ->

                    ScoredFixture(
                        example = example,
                        probability =
                            predictor.predictProbability(
                                example = example
                            )
                    )
                }
                .sortedWith(
                    compareBy<ScoredFixture>(
                        { it.example.catalogKey },
                        { it.example.candidateRank },
                        { it.example.serverKey },
                        { it.example.id }
                    )
                )
                .toList()

        require(testExamples.isNotEmpty()) {
            "GPT-5.5 comparison test fixture set is empty."
        }

        require(
            testExamples.any { fixture ->
                fixture.example.label ==
                        NutritionMatcherTrainingLabel.POSITIVE
            }
        ) {
            "GPT-5.5 comparison fixtures contain no positives."
        }

        require(
            testExamples.any { fixture ->
                fixture.example.label ==
                        NutritionMatcherTrainingLabel.NEGATIVE
            }
        ) {
            "GPT-5.5 comparison fixtures contain no negatives."
        }

        val evaluatedThresholds =
            (
                    FIXED_THRESHOLDS +
                            model.decisionThreshold
                    )
                .distinct()
                .sorted()

        val thresholdComparisons =
            evaluatedThresholds.map { threshold ->

                calculateThresholdComparison(
                    fixtures = testExamples,
                    threshold = threshold
                )
            }

        val legacyGridRecommendation =
            selectRecommendedThreshold(
                comparisons = thresholdComparisons
            )

        val productionThresholdComparison =
            thresholdComparisons.single { comparison ->
                comparison.threshold ==
                        model.decisionThreshold
            }

        val topOneComparison =
            calculateTopOneComparison(
                fixtures = testExamples
            )

        val comparison =
            LocalNutritionMatcherGptFixtureComparison(
                modelVersion =
                    model.version,
                modelType =
                    model.modelType,
                featureNames =
                    model.featureNames,
                featureCount =
                    model.featureNames.size,
                productionDecisionThreshold =
                    model.decisionThreshold,
                thresholdMinimumPrecision =
                    model.decisionThresholdOptimization
                        .minimumPrecision,
                thresholdMaximumFalsePositiveRate =
                    model.decisionThresholdOptimization
                        .maximumFalsePositiveRate,
                thresholdMinimumPredictedPositiveCount =
                    model.decisionThresholdOptimization
                        .minimumPredictedPositiveCount,
                thresholdPolicySatisfied =
                    model.decisionThresholdOptimization
                        .policySatisfied,
                productionThreshold =
                    productionThresholdComparison,
                datasetFile =
                    datasetFile.name,
                modelFile =
                    modelFile.name,
                testCatalogKeyCount =
                    testExamples
                        .map { fixture ->
                            fixture.example.catalogKey
                        }
                        .distinct()
                        .size,
                testExampleCount =
                    testExamples.size,
                positiveFixtureCount =
                    testExamples.count { fixture ->
                        fixture.example.label ==
                                NutritionMatcherTrainingLabel.POSITIVE
                    },
                negativeFixtureCount =
                    testExamples.count { fixture ->
                        fixture.example.label ==
                                NutritionMatcherTrainingLabel.NEGATIVE
                    },
                thresholds =
                    thresholdComparisons,
                recommendedThreshold =
                    legacyGridRecommendation,
                topOne =
                    topOneComparison,
                historicalTopOneAccuracy =
                    HISTORICAL_TOP_ONE_ACCURACY,
                historicalMeanPositiveRank =
                    HISTORICAL_MEAN_POSITIVE_RANK,
                topOneAccuracyDelta =
                    topOneComparison.accuracy -
                            HISTORICAL_TOP_ONE_ACCURACY,
                meanPositiveRankDelta =
                    topOneComparison.meanPositiveRank -
                            HISTORICAL_MEAN_POSITIVE_RANK
            )

        validateComparison(
            comparison = comparison
        )

        writeComparison(
            comparison = comparison,
            outputFile = outputFile
        )

        printComparison(
            comparison = comparison,
            outputFile = outputFile,
            output = output
        )

        return CompareLocalNutritionMatcherAgainstGptFixturesResult(
            comparison = comparison,
            outputFile = outputFile.absolutePath
        )
    }

    private fun calculateThresholdComparison(
        fixtures: List<ScoredFixture>,
        threshold: Double
    ): LocalNutritionMatcherThresholdComparison {

        require(threshold in 0.0..1.0) {
            "Threshold must be between 0.0 and 1.0: $threshold"
        }

        var truePositive =
            0

        var falsePositive =
            0

        var trueNegative =
            0

        var falseNegative =
            0

        fixtures.forEach { fixture ->

            val predictedPositive =
                fixture.probability >=
                        threshold

            val actualPositive =
                fixture.example.label ==
                        NutritionMatcherTrainingLabel.POSITIVE

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

        val precision =
            divide(
                numerator = truePositive,
                denominator =
                    truePositive +
                            falsePositive
            )

        val recall =
            divide(
                numerator = truePositive,
                denominator =
                    truePositive +
                            falseNegative
            )

        val specificity =
            divide(
                numerator = trueNegative,
                denominator =
                    trueNegative +
                            falsePositive
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

        return LocalNutritionMatcherThresholdComparison(
            threshold =
                threshold,
            exampleCount =
                fixtures.size,
            positiveCount =
                truePositive +
                        falseNegative,
            negativeCount =
                trueNegative +
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
            f1 =
                f1,
            specificity =
                specificity,
            balancedAccuracy =
                (
                        recall +
                                specificity
                        ) /
                        2.0,
            predictedPositiveCount =
                truePositive +
                        falsePositive,
            predictedNegativeCount =
                trueNegative +
                        falseNegative,
            acceptedOriginalMatchRecall =
                calculateRoleRecall(
                    fixtures = fixtures,
                    role =
                        NutritionMatcherTrainingExampleRole
                            .ACCEPTED_ORIGINAL_MATCH,
                    threshold = threshold
                ),
            acceptedRepresentativeRecall =
                calculateRoleRecall(
                    fixtures = fixtures,
                    role =
                        NutritionMatcherTrainingExampleRole
                            .ACCEPTED_SELECTED,
                    threshold = threshold
                ),
            rejectedSelectedFalsePositiveRate =
                calculateRoleFalsePositiveRate(
                    fixtures = fixtures,
                    role =
                        NutritionMatcherTrainingExampleRole
                            .REJECTED_SELECTED,
                    threshold = threshold
                ),
            noMatchFalsePositiveRate =
                calculateRoleFalsePositiveRate(
                    fixtures = fixtures,
                    role =
                        NutritionMatcherTrainingExampleRole
                            .REJECTED_NO_MATCH_CANDIDATE,
                    threshold = threshold
                ),
            alternativeFalsePositiveRate =
                calculateRoleFalsePositiveRate(
                    fixtures = fixtures,
                    role =
                        NutritionMatcherTrainingExampleRole
                            .NON_SELECTED_ALTERNATIVE,
                    threshold = threshold
                )
        )
    }

    private fun calculateRoleRecall(
        fixtures: List<ScoredFixture>,
        role: NutritionMatcherTrainingExampleRole,
        threshold: Double
    ): Double {

        val positiveRoleFixtures =
            fixtures.filter { fixture ->
                fixture.example.role ==
                        role &&
                        fixture.example.label ==
                        NutritionMatcherTrainingLabel.POSITIVE
            }

        if (
            positiveRoleFixtures.isEmpty()
        ) {
            return 0.0
        }

        return positiveRoleFixtures
            .count { fixture ->
                fixture.probability >=
                        threshold
            }
            .toDouble() /
                positiveRoleFixtures.size.toDouble()
    }

    private fun calculateRoleFalsePositiveRate(
        fixtures: List<ScoredFixture>,
        role: NutritionMatcherTrainingExampleRole,
        threshold: Double
    ): Double {

        val negativeRoleFixtures =
            fixtures.filter { fixture ->
                fixture.example.role ==
                        role &&
                        fixture.example.label ==
                        NutritionMatcherTrainingLabel.NEGATIVE
            }

        if (
            negativeRoleFixtures.isEmpty()
        ) {
            return 0.0
        }

        return negativeRoleFixtures
            .count { fixture ->
                fixture.probability >=
                        threshold
            }
            .toDouble() /
                negativeRoleFixtures.size.toDouble()
    }

    private fun selectRecommendedThreshold(
        comparisons:
        List<LocalNutritionMatcherThresholdComparison>
    ): LocalNutritionMatcherRecommendedThreshold? {

        val eligible =
            comparisons
                .filter { comparison ->
                    comparison.precision >=
                            MINIMUM_AUTO_ACCEPT_PRECISION
                }
                .filter { comparison ->
                    comparison.truePositive >
                            0
                }

        val selected =
            eligible.maxWithOrNull(
                compareBy<
                        LocalNutritionMatcherThresholdComparison
                        >(
                    { comparison ->
                        comparison.recall
                    },
                    { comparison ->
                        comparison.f1
                    },
                    { comparison ->
                        comparison.precision
                    },
                    { comparison ->
                        -comparison.threshold
                    }
                )
            )
                ?: return null

        return LocalNutritionMatcherRecommendedThreshold(
            threshold =
                selected.threshold,
            minimumPrecision =
                MINIMUM_AUTO_ACCEPT_PRECISION,
            precision =
                selected.precision,
            recall =
                selected.recall,
            f1 =
                selected.f1,
            truePositive =
                selected.truePositive,
            falsePositive =
                selected.falsePositive,
            falseNegative =
                selected.falseNegative
        )
    }

    private fun calculateTopOneComparison(
        fixtures: List<ScoredFixture>
    ): LocalNutritionMatcherTopOneComparison {

        val eligibleGroups =
            fixtures
                .groupBy { fixture ->
                    fixture.example.catalogKey
                }
                .filterValues { group ->
                    group.any { fixture ->
                        fixture.example.label ==
                                NutritionMatcherTrainingLabel.POSITIVE
                    }
                }

        if (
            eligibleGroups.isEmpty()
        ) {
            return LocalNutritionMatcherTopOneComparison(
                eligibleCatalogKeyCount =
                    0,
                correctCatalogKeyCount =
                    0,
                accuracy =
                    0.0,
                meanPositiveRank =
                    0.0,
                positiveAtRankOneCount =
                    0
            )
        }

        var correctCatalogKeyCount =
            0

        var positiveAtRankOneCount =
            0

        var positiveRankSum =
            0.0

        eligibleGroups
            .toSortedMap()
            .forEach { (_, group) ->

                val ranked =
                    group.sortedWith(
                        compareByDescending<ScoredFixture> { fixture ->
                            fixture.probability
                        }
                            .thenBy { fixture ->
                                fixture.example.candidateRank
                            }
                            .thenBy { fixture ->
                                fixture.example.serverKey
                            }
                            .thenBy { fixture ->
                                fixture.example.id
                            }
                    )

                if (
                    ranked.first().example.label ==
                    NutritionMatcherTrainingLabel.POSITIVE
                ) {
                    correctCatalogKeyCount++
                }

                val firstPositiveIndex =
                    ranked.indexOfFirst { fixture ->
                        fixture.example.label ==
                                NutritionMatcherTrainingLabel.POSITIVE
                    }

                require(
                    firstPositiveIndex >=
                            0
                ) {
                    "Eligible GPT-5.5 fixture group contains no positive " +
                            "candidate."
                }

                val positiveRank =
                    firstPositiveIndex +
                            1

                positiveRankSum +=
                    positiveRank.toDouble()

                if (
                    positiveRank ==
                    1
                ) {
                    positiveAtRankOneCount++
                }
            }

        return LocalNutritionMatcherTopOneComparison(
            eligibleCatalogKeyCount =
                eligibleGroups.size,
            correctCatalogKeyCount =
                correctCatalogKeyCount,
            accuracy =
                correctCatalogKeyCount.toDouble() /
                        eligibleGroups.size.toDouble(),
            meanPositiveRank =
                positiveRankSum /
                        eligibleGroups.size.toDouble(),
            positiveAtRankOneCount =
                positiveAtRankOneCount
        )
    }

    private fun isTestCatalogKey(
        catalogKey: String,
        model: LocalNutritionMatcherModel
    ): Boolean {

        val bucket =
            stableBucket(
                value = catalogKey,
                modulo =
                    model.training.splitModulo
            )

        return bucket in
                model.training.testBuckets
    }

    private fun stableBucket(
        value: String,
        modulo: Int
    ): Int {

        require(
            modulo >
                    0
        ) {
            "Split modulo must be greater than zero."
        }

        val digest =
            MessageDigest
                .getInstance(
                    "SHA-256"
                )
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

        return (
                unsigned and
                        Int.MAX_VALUE
                ) %
                modulo
    }

    private fun validateContracts(
        dataset: NutritionMatcherTrainingDataset,
        model: LocalNutritionMatcherModel
    ) {

        require(
            dataset.version ==
                    model.training.datasetVersion
        ) {
            "Dataset version differs from trained model metadata."
        }

        require(
            dataset.datasetType ==
                    model.datasetType
        ) {
            "Dataset type differs from trained model."
        }

        require(
            dataset.examples.size ==
                    model.training.exampleCount
        ) {
            "Dataset example count differs from trained model: " +
                    "dataset=${dataset.examples.size}, " +
                    "model=${model.training.exampleCount}"
        }

        require(
            model.training.testBuckets.isNotEmpty()
        ) {
            "Local matcher model contains no test buckets."
        }

        require(
            model.training.testBuckets.all { bucket ->
                bucket in
                        0 until
                        model.training.splitModulo
            }
        ) {
            "Local matcher model contains an invalid test bucket."
        }

        require(
            model.featureNames ==
                    LocalNutritionMatcherFeatureContract
                        .ACTIVE_FEATURE_NAMES
        ) {
            "Local matcher model does not use the active feature contract: " +
                    "model=${model.featureNames}, " +
                    "active=" +
                    LocalNutritionMatcherFeatureContract
                        .ACTIVE_FEATURE_NAMES
        }

        require(
            model.decisionThresholdOptimization.minimumPrecision ==
                    NutritionMatcherConservativeThresholdPolicyContract
                        .ACTIVE_POLICY
                        .minimumPrecision
        ) {
            "Local matcher model does not use the active minimum " +
                    "precision policy."
        }

        require(
            model.decisionThresholdOptimization.maximumFalsePositiveRate ==
                    NutritionMatcherConservativeThresholdPolicyContract
                        .ACTIVE_POLICY
                        .maximumFalsePositiveRate
        ) {
            "Local matcher model does not use the active maximum " +
                    "false-positive-rate policy."
        }

        require(
            model.decisionThresholdOptimization
                .minimumPredictedPositiveCount ==
                    NutritionMatcherConservativeThresholdPolicyContract
                        .ACTIVE_POLICY
                        .minimumPredictedPositiveCount
        ) {
            "Local matcher model does not use the active minimum " +
                    "predicted-positive-count policy."
        }

        require(
            model.decisionThresholdOptimization.policySatisfied
        ) {
            "Local matcher model threshold policy is not satisfied."
        }

        require(
            model.decisionThreshold in
                    0.0..1.0
        ) {
            "Local matcher model decision threshold is invalid: " +
                    model.decisionThreshold
        }
    }

    private fun validateComparison(
        comparison:
        LocalNutritionMatcherGptFixtureComparison
    ) {

        require(
            comparison.testExampleCount ==
                    comparison.positiveFixtureCount +
                    comparison.negativeFixtureCount
        ) {
            "Fixture counts are inconsistent."
        }

        val thresholdValues =
            comparison.thresholds
                .map { threshold ->
                    threshold.threshold
                }

        require(
            thresholdValues ==
                    thresholdValues
                        .distinct()
                        .sorted()
        ) {
            "Threshold comparisons must be unique and sorted."
        }

        require(
            comparison.thresholds.any { threshold ->
                threshold.threshold ==
                        comparison.productionDecisionThreshold
            }
        ) {
            "Threshold comparisons do not contain the production threshold."
        }

        require(
            comparison.productionThreshold.threshold ==
                    comparison.productionDecisionThreshold
        ) {
            "Production threshold comparison uses a different threshold."
        }

        require(
            comparison.thresholds.all { threshold ->
                threshold.exampleCount ==
                        comparison.testExampleCount
            }
        ) {
            "Threshold comparison example counts are inconsistent."
        }

        require(
            comparison.thresholds.all { threshold ->
                threshold.truePositive +
                        threshold.falsePositive +
                        threshold.trueNegative +
                        threshold.falseNegative ==
                        comparison.testExampleCount
            }
        ) {
            "Threshold comparison confusion matrices are inconsistent."
        }

        require(
            comparison.thresholds.all { threshold ->
                threshold.precision in
                        0.0..1.0 &&
                        threshold.recall in
                        0.0..1.0 &&
                        threshold.f1 in
                        0.0..1.0 &&
                        threshold.specificity in
                        0.0..1.0 &&
                        threshold.balancedAccuracy in
                        0.0..1.0
            }
        ) {
            "Threshold comparison metrics must be between 0.0 and 1.0."
        }

        require(
            comparison.featureCount ==
                    comparison.featureNames.size
        ) {
            "Comparison feature count is inconsistent."
        }

        require(
            comparison.featureNames ==
                    LocalNutritionMatcherFeatureContract
                        .ACTIVE_FEATURE_NAMES
        ) {
            "Comparison does not use the active feature set."
        }

        require(
            comparison.thresholdPolicySatisfied
        ) {
            "Production model threshold policy is not satisfied."
        }

        require(
            comparison.modelVersion ==
                    EXPECTED_PRODUCTION_MODEL_VERSION
        ) {
            "Expected productive nutrition matcher model version " +
                    "$EXPECTED_PRODUCTION_MODEL_VERSION, but was " +
                    comparison.modelVersion
        }

        require(
            comparison.featureCount ==
                    LocalNutritionMatcherFeatureContract
                        .ACTIVE_FEATURE_NAMES
                        .size
        ) {
            "Comparison feature count differs from the active feature " +
                    "contract."
        }

        require(
            comparison.topOne.accuracy in
                    0.0..1.0
        ) {
            "TOP-1 accuracy must be between 0.0 and 1.0."
        }

        require(
            comparison.topOne.meanPositiveRank >=
                    1.0
        ) {
            "Mean positive rank must be at least 1.0."
        }

        comparison.recommendedThreshold
            ?.let { recommended ->

                require(
                    recommended.precision >=
                            recommended.minimumPrecision
                ) {
                    "Legacy grid recommendation does not satisfy its " +
                            "minimum precision."
                }

                require(
                    recommended.threshold in
                            thresholdValues
                ) {
                    "Legacy grid recommendation does not reference an " +
                            "evaluated threshold."
                }
            }
    }

    private fun readDataset(
        file: File
    ): NutritionMatcherTrainingDataset {

        return runCatching {

            Gson().fromJson(
                file.readText(),
                NutritionMatcherTrainingDataset::class.java
            )

        }.getOrElse { throwable ->

            throw IllegalArgumentException(
                "Could not read nutrition matcher training dataset: " +
                        file.absolutePath,
                throwable
            )
        }
    }

    private fun readModel(
        file: File
    ): LocalNutritionMatcherModel {

        return runCatching {

            Gson().fromJson(
                file.readText(),
                LocalNutritionMatcherModel::class.java
            )

        }.getOrElse { throwable ->

            throw IllegalArgumentException(
                "Could not read local nutrition matcher model: " +
                        file.absolutePath,
                throwable
            )
        }
    }

    private fun writeComparison(
        comparison:
        LocalNutritionMatcherGptFixtureComparison,
        outputFile: File
    ) {

        outputFile.parentFile
            ?.let { directory ->

                if (
                    !directory.exists()
                ) {
                    check(
                        directory.mkdirs()
                    ) {
                        "Could not create local matcher comparison " +
                                "directory: " +
                                directory.absolutePath
                    }
                }

                require(
                    directory.isDirectory
                ) {
                    "Local matcher comparison parent path is not " +
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
            gson.toJson(
                comparison
            ) +
                    "\n"
        )
    }

    private fun printComparison(
        comparison:
        LocalNutritionMatcherGptFixtureComparison,
        outputFile: File,
        output: PrintStream
    ) {

        output.println()
        output.println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        )
        output.println(
            "LOCAL NUTRITION MATCHER VS GPT-5.5 FIXTURES"
        )
        output.println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        )
        output.println(
            "Model version             : " +
                    comparison.modelVersion
        )
        output.println(
            "Model type                : " +
                    comparison.modelType
        )
        output.println(
            "Feature count             : " +
                    comparison.featureCount
        )
        output.println(
            "Production threshold      : " +
                    format(
                        comparison.productionDecisionThreshold
                    )
        )
        output.println(
            "Minimum precision policy  : " +
                    format(
                        comparison.thresholdMinimumPrecision
                    )
        )
        output.println(
            "Maximum FPR policy        : " +
                    format(
                        comparison.thresholdMaximumFalsePositiveRate
                    )
        )
        output.println(
            "Minimum predicted positive: " +
                    comparison.thresholdMinimumPredictedPositiveCount
        )
        output.println(
            "Threshold policy satisfied: " +
                    comparison.thresholdPolicySatisfied
        )
        output.println()
        output.println(
            "Test catalog keys         : " +
                    comparison.testCatalogKeyCount
        )
        output.println(
            "Test examples             : " +
                    comparison.testExampleCount
        )
        output.println(
            "Positive fixtures         : " +
                    comparison.positiveFixtureCount
        )
        output.println(
            "Negative fixtures         : " +
                    comparison.negativeFixtureCount
        )
        output.println()
        output.println(
            "Threshold  Precision  Recall     F1       FP     FN     NO_MATCH FPR  ALT FPR"
        )

        comparison.thresholds.forEach { threshold ->

            output.println(
                buildString {

                    append(
                        format(
                            value = threshold.threshold,
                            width = 9
                        )
                    )

                    append(
                        "  "
                    )

                    append(
                        format(
                            value = threshold.precision,
                            width = 9
                        )
                    )

                    append(
                        "  "
                    )

                    append(
                        format(
                            value = threshold.recall,
                            width = 7
                        )
                    )

                    append(
                        "  "
                    )

                    append(
                        format(
                            value = threshold.f1,
                            width = 7
                        )
                    )

                    append(
                        "  "
                    )

                    append(
                        threshold.falsePositive
                            .toString()
                            .padStart(
                                length = 5
                            )
                    )

                    append(
                        "  "
                    )

                    append(
                        threshold.falseNegative
                            .toString()
                            .padStart(
                                length = 5
                            )
                    )

                    append(
                        "  "
                    )

                    append(
                        format(
                            value =
                                threshold.noMatchFalsePositiveRate,
                            width = 12
                        )
                    )

                    append(
                        "  "
                    )

                    append(
                        format(
                            value =
                                threshold.alternativeFalsePositiveRate,
                            width = 7
                        )
                    )
                }
            )
        }

        val production =
            comparison.productionThreshold

        output.println()
        output.println(
            "PRODUCTION THRESHOLD"
        )
        output.println(
            "Precision                 : " +
                    format(
                        production.precision
                    )
        )
        output.println(
            "Recall                    : " +
                    format(
                        production.recall
                    )
        )
        output.println(
            "F1                        : " +
                    format(
                        production.f1
                    )
        )
        output.println(
            "Balanced accuracy         : " +
                    format(
                        production.balancedAccuracy
                    )
        )
        output.println(
            "True positive             : " +
                    production.truePositive
        )
        output.println(
            "False positive            : " +
                    production.falsePositive
        )
        output.println(
            "True negative             : " +
                    production.trueNegative
        )
        output.println(
            "False negative            : " +
                    production.falseNegative
        )
        output.println(
            "Accepted original recall  : " +
                    format(
                        production.acceptedOriginalMatchRecall
                    )
        )
        output.println(
            "Accepted representative recall: " +
                    format(
                        production.acceptedRepresentativeRecall
                    )
        )
        output.println(
            "NO_MATCH false positive rate: " +
                    format(
                        production.noMatchFalsePositiveRate
                    )
        )
        output.println(
            "Alternative false positive rate: " +
                    format(
                        production.alternativeFalsePositiveRate
                    )
        )

        output.println()
        output.println(
            "TOP-1 eligible keys       : " +
                    comparison.topOne.eligibleCatalogKeyCount
        )
        output.println(
            "TOP-1 correct keys        : " +
                    comparison.topOne.correctCatalogKeyCount
        )
        output.println(
            "TOP-1 accuracy            : " +
                    format(
                        comparison.topOne.accuracy
                    )
        )
        output.println(
            "Mean positive rank        : " +
                    format(
                        comparison.topOne.meanPositiveRank
                    )
        )
        output.println(
            "Positive at rank one      : " +
                    comparison.topOne.positiveAtRankOneCount
        )
        output.println(
            "Historical TOP-1 accuracy : " +
                    format(
                        comparison.historicalTopOneAccuracy
                    )
        )
        output.println(
            "TOP-1 accuracy delta      : " +
                    formatSigned(
                        comparison.topOneAccuracyDelta
                    )
        )
        output.println(
            "Historical mean rank      : " +
                    format(
                        comparison.historicalMeanPositiveRank
                    )
        )
        output.println(
            "Mean rank delta           : " +
                    formatSigned(
                        comparison.meanPositiveRankDelta
                    )
        )

        output.println()

        val legacyGridRecommendation =
            comparison.recommendedThreshold

        if (
            legacyGridRecommendation ==
            null
        ) {
            output.println(
                "Legacy grid recommendation: NONE"
            )
            output.println(
                "Reason                    : No evaluated threshold " +
                        "reached minimum precision " +
                        format(
                            MINIMUM_AUTO_ACCEPT_PRECISION
                        )
            )
        } else {
            output.println(
                "Legacy grid recommendation: " +
                        format(
                            legacyGridRecommendation.threshold
                        )
            )
            output.println(
                "Legacy grid precision     : " +
                        format(
                            legacyGridRecommendation.precision
                        )
            )
            output.println(
                "Legacy grid recall        : " +
                        format(
                            legacyGridRecommendation.recall
                        )
            )
            output.println(
                "Legacy grid F1            : " +
                        format(
                            legacyGridRecommendation.f1
                        )
            )
            output.println(
                "Legacy grid true positive : " +
                        legacyGridRecommendation.truePositive
            )
            output.println(
                "Legacy grid false positive: " +
                        legacyGridRecommendation.falsePositive
            )
            output.println(
                "Legacy grid false negative: " +
                        legacyGridRecommendation.falseNegative
            )
        }

        output.println()
        output.println(
            "Output                     : " +
                    outputFile.absolutePath
        )
        output.println(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        )
    }

    private fun divide(
        numerator: Int,
        denominator: Int
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
        width: Int = 0
    ): String {

        val formatted =
            String.format(
                Locale.ROOT,
                "%.4f",
                value
            )

        return if (
            width >
            0
        ) {
            formatted.padStart(
                length = width
            )
        } else {
            formatted
        }
    }

    private fun formatSigned(
        value: Double
    ): String {

        return String.format(
            Locale.ROOT,
            "%+.4f",
            value
        )
    }

    private data class ScoredFixture(
        val example:
        NutritionMatcherTrainingExample,
        val probability: Double
    )

    private companion object {

        val FIXED_THRESHOLDS =
            listOf(
                0.50,
                0.60,
                0.70,
                0.80,
                0.90,
                0.95,
                0.98,
                0.99
            )

        const val MINIMUM_AUTO_ACCEPT_PRECISION =
            0.95

        const val HISTORICAL_TOP_ONE_ACCURACY =
            0.9888

        const val HISTORICAL_MEAN_POSITIVE_RANK =
            1.01

        const val EXPECTED_PRODUCTION_MODEL_VERSION =
            5
    }
}