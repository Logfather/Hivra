package de.shopme.tools.knowledge.mapping.catalog.training.model

data class NutritionMatcherThresholdOptimizationPolicy(
    val minimumPrecision: Double,
    val maximumFalsePositiveRate: Double,
    val minimumPredictedPositiveCount: Int,
) {

    init {
        require(
            minimumPrecision in 0.0..1.0,
        ) {
            "Minimum precision must be between zero and one."
        }

        require(
            maximumFalsePositiveRate in 0.0..1.0,
        ) {
            "Maximum false-positive rate must be between zero and one."
        }

        require(
            minimumPredictedPositiveCount >= 1,
        ) {
            "Minimum predicted-positive count must be positive."
        }
    }
}

data class NutritionMatcherScoredLabel(
    val score: Double,
    val positive: Boolean,
) {

    init {
        require(
            score.isFinite(),
        ) {
            "Nutrition matcher score must be finite."
        }

        require(
            score in 0.0..1.0,
        ) {
            "Nutrition matcher score must be between zero and one."
        }
    }
}

data class NutritionMatcherThresholdMetrics(
    val threshold: Double,
    val truePositiveCount: Int,
    val falsePositiveCount: Int,
    val trueNegativeCount: Int,
    val falseNegativeCount: Int,
    val predictedPositiveCount: Int,
    val precision: Double,
    val recall: Double,
    val falsePositiveRate: Double,
    val f1: Double,
)

data class NutritionMatcherThresholdOptimizationResult(
    val selectedThreshold: Double,
    val selectedMetrics: NutritionMatcherThresholdMetrics,
    val policySatisfied: Boolean,
    val evaluatedThresholdCount: Int,
)

class NutritionMatcherDecisionThresholdOptimizer(
    private val policy:
    NutritionMatcherThresholdOptimizationPolicy,
) {

    fun optimize(
        scoredLabels: List<NutritionMatcherScoredLabel>,
    ): NutritionMatcherThresholdOptimizationResult {

        require(
            scoredLabels.isNotEmpty(),
        ) {
            "Threshold optimization requires scored labels."
        }

        require(
            scoredLabels.any { scoredLabel ->
                scoredLabel.positive
            },
        ) {
            "Threshold optimization requires positive labels."
        }

        require(
            scoredLabels.any { scoredLabel ->
                !scoredLabel.positive
            },
        ) {
            "Threshold optimization requires negative labels."
        }

        val thresholds =
            buildThresholds(
                scoredLabels =
                    scoredLabels,
            )

        val evaluated =
            thresholds.map { threshold ->
                evaluate(
                    scoredLabels =
                        scoredLabels,
                    threshold =
                        threshold,
                )
            }

        val policyCandidates =
            evaluated.filter { metrics ->
                metrics.predictedPositiveCount >=
                        policy.minimumPredictedPositiveCount &&
                        metrics.precision + EPSILON >=
                        policy.minimumPrecision &&
                        metrics.falsePositiveRate <=
                        policy.maximumFalsePositiveRate +
                        EPSILON
            }

        val selected =
            if (policyCandidates.isNotEmpty()) {
                policyCandidates.maxWithOrNull(
                    compareBy<NutritionMatcherThresholdMetrics>(
                        { metrics ->
                            metrics.recall
                        },
                        { metrics ->
                            metrics.precision
                        },
                        { metrics ->
                            metrics.threshold
                        },
                    ),
                )!!
            } else {
                selectConservativeFallback(
                    evaluated =
                        evaluated,
                )
            }

        return NutritionMatcherThresholdOptimizationResult(
            selectedThreshold =
                selected.threshold,
            selectedMetrics =
                selected,
            policySatisfied =
                selected in policyCandidates,
            evaluatedThresholdCount =
                evaluated.size,
        )
    }

    private fun buildThresholds(
        scoredLabels: List<NutritionMatcherScoredLabel>,
    ): List<Double> {

        val distinctScores =
            scoredLabels
                .map { scoredLabel ->
                    scoredLabel.score
                }
                .distinct()
                .sorted()

        val thresholds =
            buildList {

                add(0.0)

                distinctScores.forEach { score ->
                    add(score)
                }

                distinctScores
                    .zipWithNext()
                    .forEach { (lower, upper) ->
                        add(
                            lower +
                                    (upper - lower) /
                                    2.0,
                        )
                    }

                add(1.0)
            }

        return thresholds
            .distinctBy { threshold ->
                threshold.toBits()
            }
            .sorted()
    }

    private fun evaluate(
        scoredLabels: List<NutritionMatcherScoredLabel>,
        threshold: Double,
    ): NutritionMatcherThresholdMetrics {

        var truePositiveCount =
            0

        var falsePositiveCount =
            0

        var trueNegativeCount =
            0

        var falseNegativeCount =
            0

        scoredLabels.forEach { scoredLabel ->

            val predictedPositive =
                scoredLabel.score >=
                        threshold

            when {
                predictedPositive &&
                        scoredLabel.positive -> {
                    truePositiveCount +=
                        1
                }

                predictedPositive &&
                        !scoredLabel.positive -> {
                    falsePositiveCount +=
                        1
                }

                !predictedPositive &&
                        !scoredLabel.positive -> {
                    trueNegativeCount +=
                        1
                }

                else -> {
                    falseNegativeCount +=
                        1
                }
            }
        }

        val predictedPositiveCount =
            truePositiveCount +
                    falsePositiveCount

        val precision =
            divideOrZero(
                numerator =
                    truePositiveCount,
                denominator =
                    predictedPositiveCount,
            )

        val recall =
            divideOrZero(
                numerator =
                    truePositiveCount,
                denominator =
                    truePositiveCount +
                            falseNegativeCount,
            )

        val falsePositiveRate =
            divideOrZero(
                numerator =
                    falsePositiveCount,
                denominator =
                    falsePositiveCount +
                            trueNegativeCount,
            )

        val f1 =
            if (
                precision +
                recall <=
                EPSILON
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

        return NutritionMatcherThresholdMetrics(
            threshold =
                threshold,
            truePositiveCount =
                truePositiveCount,
            falsePositiveCount =
                falsePositiveCount,
            trueNegativeCount =
                trueNegativeCount,
            falseNegativeCount =
                falseNegativeCount,
            predictedPositiveCount =
                predictedPositiveCount,
            precision =
                precision,
            recall =
                recall,
            falsePositiveRate =
                falsePositiveRate,
            f1 =
                f1,
        )
    }

    private fun selectConservativeFallback(
        evaluated: List<NutritionMatcherThresholdMetrics>,
    ): NutritionMatcherThresholdMetrics {

        return evaluated.maxWithOrNull(
            compareBy<NutritionMatcherThresholdMetrics>(
                { metrics ->
                    metrics.precision
                },
                { metrics ->
                    -metrics.falsePositiveRate
                },
                { metrics ->
                    metrics.threshold
                },
                { metrics ->
                    metrics.recall
                },
            ),
        )!!
    }

    private fun divideOrZero(
        numerator: Int,
        denominator: Int,
    ): Double {

        return if (denominator == 0) {
            0.0
        } else {
            numerator.toDouble() /
                    denominator.toDouble()
        }
    }

    private companion object {

        const val EPSILON =
            1e-12
    }
}