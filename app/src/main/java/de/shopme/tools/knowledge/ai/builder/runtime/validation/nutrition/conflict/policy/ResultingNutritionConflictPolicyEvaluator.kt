package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.policy

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.ResultingNutritionConflictAnalysis
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.evaluation.ResultingNutritionConflictEvaluation
import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.evaluation.ResultingNutritionConflictSeverity

class ResultingNutritionConflictPolicyEvaluator(
    private val policy:
    ResultingNutritionConflictPolicy =
        ResultingNutritionConflictPolicy()
) {

    fun evaluate(
        analysis: ResultingNutritionConflictAnalysis,
        evaluation: ResultingNutritionConflictEvaluation
    ): ResultingNutritionConflictPolicyDecision {

        require(
            analysis.conflictEntryCount ==
                    evaluation.conflictEntryCount
        ) {
            "Conflict analysis and evaluation must describe the same " +
                    "number of conflict entries."
        }

        val violations =
            mutableListOf<
                    ResultingNutritionConflictPolicyViolation
                    >()

        if (
            analysis.entryConflictRate >
            policy.maximumEntryConflictRate
        ) {
            violations +=
                ResultingNutritionConflictPolicyViolation(
                    reason =
                        ResultingNutritionConflictPolicyViolationReason
                            .ENTRY_CONFLICT_RATE_EXCEEDS_MAXIMUM,
                    actualValue =
                        analysis.entryConflictRate,
                    maximumAllowedValue =
                        policy.maximumEntryConflictRate,
                    message =
                        "Nutrition entry conflict rate exceeds the " +
                                "approved policy maximum."
                )
        }

        if (
            analysis.nutrientConflictRate >
            policy.maximumNutrientConflictRate
        ) {
            violations +=
                ResultingNutritionConflictPolicyViolation(
                    reason =
                        ResultingNutritionConflictPolicyViolationReason
                            .NUTRIENT_CONFLICT_RATE_EXCEEDS_MAXIMUM,
                    actualValue =
                        analysis.nutrientConflictRate,
                    maximumAllowedValue =
                        policy.maximumNutrientConflictRate,
                    message =
                        "Nutrition nutrient conflict rate exceeds the " +
                                "approved policy maximum."
                )
        }

        val extremeConflictCount =
            evaluation.countsBySeverity[
                ResultingNutritionConflictSeverity.EXTREME
            ]
                ?: 0L

        if (
            extremeConflictCount >
            policy.maximumExtremeConflictCount
        ) {
            violations +=
                ResultingNutritionConflictPolicyViolation(
                    reason =
                        ResultingNutritionConflictPolicyViolationReason
                            .EXTREME_CONFLICT_COUNT_EXCEEDS_MAXIMUM,
                    actualValue =
                        extremeConflictCount.toDouble(),
                    maximumAllowedValue =
                        policy.maximumExtremeConflictCount.toDouble(),
                    message =
                        "Extreme Nutrition conflict count exceeds the " +
                                "approved policy maximum."
                )
        }

        if (
            policy.requireCompleteEvaluation &&
            evaluation.evaluatedConflictEntryCount !=
            analysis.conflictEntryCount
        ) {
            val missingEvaluationCount =
                analysis.conflictEntryCount -
                        evaluation.evaluatedConflictEntryCount

            violations +=
                ResultingNutritionConflictPolicyViolation(
                    reason =
                        ResultingNutritionConflictPolicyViolationReason
                            .INCOMPLETE_CONFLICT_EVALUATION,
                    actualValue =
                        missingEvaluationCount.toDouble(),
                    maximumAllowedValue =
                        0.0,
                    message =
                        "Not every Nutrition conflict entry was " +
                                "classified by the evaluation."
                )
        }

        if (
            policy.requireCompleteConflictEvidence &&
            analysis.omittedConflictExampleCount > 0L
        ) {
            violations +=
                ResultingNutritionConflictPolicyViolation(
                    reason =
                        ResultingNutritionConflictPolicyViolationReason
                            .INCOMPLETE_CONFLICT_EVIDENCE,
                    actualValue =
                        analysis.omittedConflictExampleCount.toDouble(),
                    maximumAllowedValue =
                        0.0,
                    message =
                        "Not every Nutrition conflict retained detailed " +
                                "evidence."
                )
        }

        val sortedViolations =
            violations.sortedBy { violation ->
                violation.reason.name
            }

        return ResultingNutritionConflictPolicyDecision(
            policyVersion =
                policy.version,
            policyStatus =
                policy.status,
            approved =
                sortedViolations.isEmpty(),
            entryConflictRate =
                analysis.entryConflictRate,
            maximumEntryConflictRate =
                policy.maximumEntryConflictRate,
            nutrientConflictRate =
                analysis.nutrientConflictRate,
            maximumNutrientConflictRate =
                policy.maximumNutrientConflictRate,
            extremeConflictCount =
                extremeConflictCount,
            maximumExtremeConflictCount =
                policy.maximumExtremeConflictCount,
            conflictEntryCount =
                analysis.conflictEntryCount,
            evaluatedConflictEntryCount =
                evaluation.evaluatedConflictEntryCount,
            omittedConflictEvidenceCount =
                analysis.omittedConflictExampleCount,
            omittedEvaluationExampleCount =
                evaluation.omittedExampleCount,
            violations =
                sortedViolations
        )
    }
}