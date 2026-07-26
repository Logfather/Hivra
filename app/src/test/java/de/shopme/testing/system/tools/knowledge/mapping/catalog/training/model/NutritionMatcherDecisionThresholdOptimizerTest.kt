package de.shopme.testing.system.tools.knowledge.mapping.catalog.training.model

import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionMatcherDecisionThresholdOptimizer
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionMatcherScoredLabel
import de.shopme.tools.knowledge.mapping.catalog.training.model.NutritionMatcherThresholdOptimizationPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NutritionMatcherDecisionThresholdOptimizerTest {

    @Test
    fun selectHighestRecallThresholdMeetingConservativePolicy() {

        val optimizer =
            NutritionMatcherDecisionThresholdOptimizer(
                policy =
                    NutritionMatcherThresholdOptimizationPolicy(
                        minimumPrecision =
                            0.8,
                        maximumFalsePositiveRate =
                            0.25,
                        minimumPredictedPositiveCount =
                            1,
                    ),
            )

        val result =
            optimizer.optimize(
                scoredLabels =
                    listOf(
                        positive(
                            score =
                                0.95,
                        ),
                        positive(
                            score =
                                0.85,
                        ),
                        positive(
                            score =
                                0.65,
                        ),
                        negative(
                            score =
                                0.75,
                        ),
                        negative(
                            score =
                                0.40,
                        ),
                        negative(
                            score =
                                0.20,
                        ),
                    ),
            )

        assertTrue(
            actual =
                result.policySatisfied,
        )

        assertEquals(
            expected =
                0.85,
            actual =
                result.selectedThreshold,
        )

        assertEquals(
            expected =
                1.0,
            actual =
                result.selectedMetrics.precision,
        )

        assertEquals(
            expected =
                2.0 / 3.0,
            actual =
                result.selectedMetrics.recall,
        )

        assertEquals(
            expected =
                0,
            actual =
                result.selectedMetrics
                    .falsePositiveCount,
        )
    }

    @Test
    fun preferHigherThresholdWhenMetricsAreEqual() {

        val optimizer =
            NutritionMatcherDecisionThresholdOptimizer(
                policy =
                    NutritionMatcherThresholdOptimizationPolicy(
                        minimumPrecision =
                            1.0,
                        maximumFalsePositiveRate =
                            0.0,
                        minimumPredictedPositiveCount =
                            1,
                    ),
            )

        val result =
            optimizer.optimize(
                scoredLabels =
                    listOf(
                        positive(
                            score =
                                0.9,
                        ),
                        negative(
                            score =
                                0.2,
                        ),
                    ),
            )

        assertTrue(
            actual =
                result.policySatisfied,
        )

        assertEquals(
            expected =
                0.9,
            actual =
                result.selectedThreshold,
        )
    }

    @Test
    fun useConservativeFallbackWhenPolicyCannotBeSatisfied() {

        val optimizer =
            NutritionMatcherDecisionThresholdOptimizer(
                policy =
                    NutritionMatcherThresholdOptimizationPolicy(
                        minimumPrecision =
                            1.0,
                        maximumFalsePositiveRate =
                            0.0,
                        minimumPredictedPositiveCount =
                            2,
                    ),
            )

        val result =
            optimizer.optimize(
                scoredLabels =
                    listOf(
                        positive(
                            score =
                                0.7,
                        ),
                        negative(
                            score =
                                0.8,
                        ),
                        negative(
                            score =
                                0.3,
                        ),
                    ),
            )

        assertFalse(
            actual =
                result.policySatisfied,
        )

        assertEquals(
            expected =
                0.7,
            actual =
                result.selectedThreshold,
        )

        assertEquals(
            expected =
                2,
            actual =
                result.selectedMetrics
                    .predictedPositiveCount,
        )

        assertEquals(
            expected =
                1,
            actual =
                result.selectedMetrics
                    .truePositiveCount,
        )

        assertEquals(
            expected =
                1,
            actual =
                result.selectedMetrics
                    .falsePositiveCount,
        )

        assertEquals(
            expected =
                0.5,
            actual =
                result.selectedMetrics
                    .precision,
        )
    }

    private fun positive(
        score: Double,
    ) =
        NutritionMatcherScoredLabel(
            score =
                score,
            positive =
                true,
        )

    private fun negative(
        score: Double,
    ) =
        NutritionMatcherScoredLabel(
            score =
                score,
            positive =
                false,
        )
}