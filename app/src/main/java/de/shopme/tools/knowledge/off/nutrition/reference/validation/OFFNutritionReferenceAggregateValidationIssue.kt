package de.shopme.tools.knowledge.off.nutrition.reference.validation

data class OFFNutritionReferenceAggregateValidationIssue(
    val type: OFFNutritionReferenceAggregateIssueType,
    val severity: OFFNutritionReferenceAggregateIssueSeverity,
    val nutrientKey: String?,
    val message: String,
    val observedValue: Double?,
    val threshold: Double?
) {

    init {
        require(message.isNotBlank()) {
            "Validation issue message must not be blank."
        }

        require(
            observedValue == null ||
                    observedValue.isFinite()
        ) {
            "Validation issue observedValue must be finite."
        }

        require(
            threshold == null ||
                    threshold.isFinite()
        ) {
            "Validation issue threshold must be finite."
        }
    }
}