package de.shopme.tools.knowledge.ai.builder.runtime.validation.nutrition

data class ResultingNutritionKnowledgeValidationIssue(
    val canonicalId: String,
    val severity: ResultingNutritionKnowledgeValidationSeverity,
    val reason: ResultingNutritionKnowledgeValidationReason,
    val nutrientKey: String? = null,
    val actualValue: Double? = null,
    val relatedValue: Double? = null,
    val message: String
) {

    init {
        require(canonicalId.isNotBlank()) {
            "canonicalId must not be blank."
        }

        require(message.isNotBlank()) {
            "message must not be blank."
        }

        require(
            actualValue == null ||
                    actualValue.isFinite()
        ) {
            "actualValue must be finite when present."
        }

        require(
            relatedValue == null ||
                    relatedValue.isFinite()
        ) {
            "relatedValue must be finite when present."
        }
    }
}

enum class ResultingNutritionKnowledgeValidationSeverity {
    WARNING,
    ERROR
}

enum class ResultingNutritionKnowledgeValidationReason {
    BLANK_CANONICAL_ID,
    NON_DETERMINISTIC_CANONICAL_ID_ORDER,
    DUPLICATE_CANONICAL_ID,
    MISSING_NUTRITION_PAYLOAD,
    NO_SUPPORTED_NUTRIENTS,
    NON_NUMERIC_NUTRIENT_VALUE,
    NON_FINITE_NUTRIENT_VALUE,
    NEGATIVE_NUTRIENT_VALUE,
    ENERGY_ABOVE_MAXIMUM,
    COMPONENT_ABOVE_MAXIMUM,
    SATURATED_FAT_EXCEEDS_TOTAL_FAT,
    SUGARS_EXCEED_CARBOHYDRATES,
    MACRONUTRIENT_SUM_EXCEEDS_MAXIMUM
}