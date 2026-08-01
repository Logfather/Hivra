package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.policy

data class ResultingNutritionConflictPolicyDecision(
    val policyVersion: Int,
    val policyStatus:
    ResultingNutritionConflictPolicyStatus,
    val approved: Boolean,
    val entryConflictRate: Double,
    val maximumEntryConflictRate: Double,
    val nutrientConflictRate: Double,
    val maximumNutrientConflictRate: Double,
    val extremeConflictCount: Long,
    val maximumExtremeConflictCount: Long,
    val conflictEntryCount: Long,
    val evaluatedConflictEntryCount: Long,
    val omittedConflictEvidenceCount: Long,
    val omittedEvaluationExampleCount: Long,
    val violations:
    List<ResultingNutritionConflictPolicyViolation>
) {

    init {
        require(policyVersion > 0)
        require(entryConflictRate in 0.0..1.0)
        require(maximumEntryConflictRate in 0.0..1.0)
        require(nutrientConflictRate in 0.0..1.0)
        require(maximumNutrientConflictRate in 0.0..1.0)
        require(extremeConflictCount >= 0L)
        require(maximumExtremeConflictCount >= 0L)
        require(conflictEntryCount >= 0L)
        require(evaluatedConflictEntryCount >= 0L)
        require(omittedConflictEvidenceCount >= 0L)
        require(omittedEvaluationExampleCount >= 0L)

        require(
            approved ==
                    violations.isEmpty()
        ) {
            "Policy approval must equal absence of violations."
        }

        require(
            violations ==
                    violations.sortedBy { violation ->
                        violation.reason.name
                    }
        ) {
            "Policy violations must be sorted by reason."
        }
    }
}