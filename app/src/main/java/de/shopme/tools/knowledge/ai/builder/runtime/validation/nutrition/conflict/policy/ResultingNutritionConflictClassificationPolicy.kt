package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.policy

import de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition.conflict.evaluation.ResultingNutritionConflictClassification

data class ResultingNutritionConflictClassificationPolicy(
    val classification:
    ResultingNutritionConflictClassification,
    val action:
    ResultingNutritionConflictPolicyAction,
    val automaticCorrectionAllowed: Boolean,
    val automaticEntryRejectionAllowed: Boolean,
    val requiresPersistedEvidence: Boolean,
    val rationale: String
) {

    init {
        require(rationale.isNotBlank()) {
            "Nutrition conflict classification policy rationale " +
                    "must not be blank."
        }

        require(
            !automaticCorrectionAllowed
        ) {
            "Approved Nutrition conflict policy must not permit " +
                    "automatic numerical correction."
        }

        require(
            !automaticEntryRejectionAllowed
        ) {
            "Approved Nutrition conflict policy must not permit " +
                    "automatic entry rejection."
        }

        require(requiresPersistedEvidence) {
            "Every Nutrition conflict must retain persisted evidence."
        }

        require(
            action !=
                    ResultingNutritionConflictPolicyAction.REJECT_BUILD
        ) {
            "Individual Nutrition conflict classes must not reject " +
                    "the complete build."
        }
    }
}