package de.shopme.testing.system.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.policy

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionConflictAnalysis
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionConflictExample
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionConflictMatchType
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionConflictNutrient
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionNutrientConflict
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.evaluation.ResultingNutritionConflictClassification
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.evaluation.ResultingNutritionConflictEvaluation
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.evaluation.ResultingNutritionConflictSeverity
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.policy.ResultingNutritionConflictPolicy
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.policy.ResultingNutritionConflictPolicyEvaluator
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.policy.ResultingNutritionConflictPolicyViolationReason
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResultingNutritionConflictPolicyEvaluatorTest {

    @Test
    fun evaluate_approvesMeasuredProductionValues() {

        val directory =
            Files.createTempDirectory(
                "nutrition-conflict-policy-approved"
            )
                .toFile()

        try {
            val analysis =
                analysis(
                    directory =
                        directory,
                    entryConflictRate =
                        0.0008021721563685195,
                    nutrientConflictRate =
                        0.00047332509536345566
                )

            val evaluation =
                evaluation(
                    directory =
                        directory,
                    extremeConflictCount =
                        9L
                )

            val decision =
                ResultingNutritionConflictPolicyEvaluator()
                    .evaluate(
                        analysis =
                            analysis,
                        evaluation =
                            evaluation
                    )

            assertTrue(
                decision.approved,
                "Expected measured production values to be approved. " +
                        "Violations=${decision.violations}"
            )

            assertTrue(
                decision.violations.isEmpty()
            )

            assertEquals(
                9L,
                decision.extremeConflictCount
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun evaluate_rejectsRatesAbovePolicyLimits() {

        val directory =
            Files.createTempDirectory(
                "nutrition-conflict-policy-rejected"
            )
                .toFile()

        try {
            val analysis =
                analysis(
                    directory =
                        directory,
                    entryConflictRate =
                        0.002,
                    nutrientConflictRate =
                        0.001
                )

            val evaluation =
                evaluation(
                    directory =
                        directory,
                    extremeConflictCount =
                        10L
                )

            val decision =
                ResultingNutritionConflictPolicyEvaluator(
                    policy =
                        ResultingNutritionConflictPolicy(
                            maximumEntryConflictRate =
                                0.001,
                            maximumNutrientConflictRate =
                                0.0005,
                            maximumExtremeConflictCount =
                                5L
                        )
                )
                    .evaluate(
                        analysis =
                            analysis,
                        evaluation =
                            evaluation
                    )

            assertFalse(
                decision.approved
            )

            assertTrue(
                decision.violations.any { violation ->
                    violation.reason ==
                            ResultingNutritionConflictPolicyViolationReason
                                .ENTRY_CONFLICT_RATE_EXCEEDS_MAXIMUM
                }
            )

            assertTrue(
                decision.violations.any { violation ->
                    violation.reason ==
                            ResultingNutritionConflictPolicyViolationReason
                                .NUTRIENT_CONFLICT_RATE_EXCEEDS_MAXIMUM
                }
            )

            assertTrue(
                decision.violations.any { violation ->
                    violation.reason ==
                            ResultingNutritionConflictPolicyViolationReason
                                .EXTREME_CONFLICT_COUNT_EXCEEDS_MAXIMUM
                }
            )
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun policy_disallowsAutomaticCorrectionAndRejection() {

        val policy =
            ResultingNutritionConflictPolicy()

        assertFalse(
            policy.automaticCorrectionAllowed
        )

        assertFalse(
            policy.automaticEntryRejectionAllowed
        )

        assertTrue(
            policy.classificationPolicies.values.all { rule ->
                !rule.automaticCorrectionAllowed &&
                        !rule.automaticEntryRejectionAllowed &&
                        rule.requiresPersistedEvidence
            }
        )
    }

    private fun analysis(
        directory: File,
        entryConflictRate: Double,
        nutrientConflictRate: Double
    ): ResultingNutritionConflictAnalysis {

        val aggregateFile =
            directory.resolve(
                "aggregates.json"
            )
                .apply {
                    writeText("[]")
                }

        val runtimeFile =
            directory.resolve(
                "nutrition.json"
            )
                .apply {
                    writeText("{}")
                }

        val conflictExamples =
            (1..CONFLICT_ENTRY_COUNT.toInt())
                .map { index ->

                    val canonicalId =
                        "conflict-" +
                                index
                                    .toString()
                                    .padStart(
                                        length =
                                            2,
                                        padChar =
                                            '0'
                                    )

                    ResultingNutritionConflictExample(
                        aggregateCanonicalId =
                            canonicalId,
                        runtimeCanonicalId =
                            canonicalId,
                        matchType =
                            ResultingNutritionConflictMatchType.EXACT,
                        conflictCount =
                            1,
                        maximumAbsoluteDifference =
                            ENERGY_DIFFERENCE,
                        conflicts =
                            listOf(
                                ResultingNutritionNutrientConflict(
                                    nutrient =
                                        ResultingNutritionConflictNutrient
                                            .ENERGY,
                                    aggregateValue =
                                        100.0,
                                    runtimeValue =
                                        110.0,
                                    absoluteDifference =
                                        ENERGY_DIFFERENCE,
                                    tolerance =
                                        ResultingNutritionConflictNutrient
                                            .ENERGY
                                            .absoluteTolerance
                                )
                            )
                    )
                }

        return ResultingNutritionConflictAnalysis(
            aggregateFile =
                aggregateFile,
            runtimeFile =
                runtimeFile,
            aggregateEntryCount =
                MATCHED_ENTRY_COUNT,
            runtimeEntryCount =
                MATCHED_ENTRY_COUNT,
            exactMatchedEntryCount =
                MATCHED_ENTRY_COUNT,
            normalizationEquivalentMatchedEntryCount =
                0L,
            matchedEntryCount =
                MATCHED_ENTRY_COUNT,
            comparableEntryCount =
                MATCHED_ENTRY_COUNT,
            nonComparableEntryCount =
                0L,
            conflictEntryCount =
                CONFLICT_ENTRY_COUNT,
            conflictFreeEntryCount =
                MATCHED_ENTRY_COUNT -
                        CONFLICT_ENTRY_COUNT,
            nutrientComparisonCount =
                NUTRIENT_COMPARISON_COUNT,
            nutrientConflictCount =
                CONFLICT_ENTRY_COUNT,
            entryConflictRate =
                entryConflictRate,
            nutrientConflictRate =
                nutrientConflictRate,
            comparisonCountsByNutrient =
                mapOf(
                    ResultingNutritionConflictNutrient.ENERGY to
                            NUTRIENT_COMPARISON_COUNT
                ),
            conflictCountsByNutrient =
                mapOf(
                    ResultingNutritionConflictNutrient.ENERGY to
                            CONFLICT_ENTRY_COUNT
                ),
            maximumAbsoluteDifferenceByNutrient =
                mapOf(
                    ResultingNutritionConflictNutrient.ENERGY to
                            ENERGY_DIFFERENCE
                ),
            conflictExamples =
                conflictExamples,
            omittedConflictExampleCount =
                0L,
            durationMillis =
                1L
        )
    }

    private fun evaluation(
        directory: File,
        extremeConflictCount: Long
    ): ResultingNutritionConflictEvaluation {

        require(
            extremeConflictCount in
                    0L..CONFLICT_ENTRY_COUNT
        ) {
            "extremeConflictCount must be between 0 and " +
                    "$CONFLICT_ENTRY_COUNT."
        }

        val lowConflictCount =
            CONFLICT_ENTRY_COUNT -
                    extremeConflictCount

        val countsBySeverity =
            buildMap {
                if (extremeConflictCount > 0L) {
                    put(
                        ResultingNutritionConflictSeverity.EXTREME,
                        extremeConflictCount
                    )
                }

                if (lowConflictCount > 0L) {
                    put(
                        ResultingNutritionConflictSeverity.LOW,
                        lowConflictCount
                    )
                }
            }

        check(
            countsBySeverity.values.sum() ==
                    CONFLICT_ENTRY_COUNT
        ) {
            "Test fixture severity counts are inconsistent. " +
                    "counts=$countsBySeverity"
        }

        return ResultingNutritionConflictEvaluation(
            aggregateFile =
                directory.resolve(
                    "aggregates.json"
                ),
            runtimeFile =
                directory.resolve(
                    "nutrition.json"
                ),
            conflictEntryCount =
                CONFLICT_ENTRY_COUNT,
            evaluatedConflictEntryCount =
                CONFLICT_ENTRY_COUNT,
            countsByClassification =
                mapOf(
                    ResultingNutritionConflictClassification
                        .MULTI_NUTRIENT_PROFILE_CONFLICT to
                            CONFLICT_ENTRY_COUNT
                ),
            countsBySeverity =
                countsBySeverity,
            countsByMatchType =
                mapOf(
                    ResultingNutritionConflictMatchType.EXACT to
                            CONFLICT_ENTRY_COUNT
                ),
            countsByConflictingNutrientCount =
                mapOf(
                    2 to
                            CONFLICT_ENTRY_COUNT
                ),
            classificationsByNutrient =
                emptyMap(),
            uniformScaleMismatchCount =
                0L,
            energyUnitConversionMismatchCount =
                0L,
            singleNutrientConflictCount =
                0L,
            multiNutrientProfileConflictCount =
                CONFLICT_ENTRY_COUNT,
            examples =
                emptyList(),
            omittedExampleCount =
                CONFLICT_ENTRY_COUNT,
            durationMillis =
                1L
        )
    }

    private companion object {

        const val MATCHED_ENTRY_COUNT =
            1_000L

        const val CONFLICT_ENTRY_COUNT =
            10L

        const val NUTRIENT_COMPARISON_COUNT =
            8_000L

        const val ENERGY_DIFFERENCE =
            10.0
    }
}