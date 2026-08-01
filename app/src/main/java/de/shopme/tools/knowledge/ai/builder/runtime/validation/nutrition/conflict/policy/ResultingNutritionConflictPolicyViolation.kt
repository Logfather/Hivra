package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.policy

data class ResultingNutritionConflictPolicyViolation(
    val reason:
    ResultingNutritionConflictPolicyViolationReason,
    val actualValue: Double,
    val maximumAllowedValue: Double,
    val message: String
) {

    init {
        require(actualValue.isFinite())
        require(maximumAllowedValue.isFinite())
        require(actualValue > maximumAllowedValue) {
            "Policy violation actualValue must exceed its maximum."
        }

        require(message.isNotBlank())
    }
}

enum class ResultingNutritionConflictPolicyViolationReason {
    ENTRY_CONFLICT_RATE_EXCEEDS_MAXIMUM,
    NUTRIENT_CONFLICT_RATE_EXCEEDS_MAXIMUM,
    EXTREME_CONFLICT_COUNT_EXCEEDS_MAXIMUM,
    INCOMPLETE_CONFLICT_EVALUATION,
    INCOMPLETE_CONFLICT_EVIDENCE
}