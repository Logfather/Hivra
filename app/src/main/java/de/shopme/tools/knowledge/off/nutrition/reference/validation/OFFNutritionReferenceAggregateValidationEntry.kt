package de.shopme.tools.knowledge.off.nutrition.reference.validation

data class OFFNutritionReferenceAggregateValidationEntry(
    val canonicalId: String,
    val status: OFFNutritionReferenceAggregateValidationStatus,
    val profileCount: Int,
    val nutrientCount: Int,
    val warningCount: Int,
    val errorCount: Int,
    val issues: List<OFFNutritionReferenceAggregateValidationIssue>
) {

    init {
        require(canonicalId.isNotBlank()) {
            "Validation entry canonicalId must not be blank."
        }

        require(profileCount > 0) {
            "Validation entry profileCount must be positive."
        }

        require(nutrientCount >= 0) {
            "Validation entry nutrientCount must not be negative."
        }

        require(warningCount >= 0) {
            "Validation entry warningCount must not be negative."
        }

        require(errorCount >= 0) {
            "Validation entry errorCount must not be negative."
        }

        require(
            warningCount ==
                    issues.count { issue ->
                        issue.severity ==
                                OFFNutritionReferenceAggregateIssueSeverity.WARNING
                    }
        ) {
            "warningCount does not match validation issues."
        }

        require(
            errorCount ==
                    issues.count { issue ->
                        issue.severity ==
                                OFFNutritionReferenceAggregateIssueSeverity.ERROR
                    }
        ) {
            "errorCount does not match validation issues."
        }

        val expectedStatus =
            when {
                errorCount > 0 ->
                    OFFNutritionReferenceAggregateValidationStatus.REJECTED

                warningCount > 0 ->
                    OFFNutritionReferenceAggregateValidationStatus.WARNING

                else ->
                    OFFNutritionReferenceAggregateValidationStatus.ACCEPTED
            }

        require(status == expectedStatus) {
            "Validation status does not match issue severities."
        }

        require(
            issues ==
                    issues.sortedWith(
                        compareBy<
                                OFFNutritionReferenceAggregateValidationIssue
                                >(
                            { issue ->
                                issue.severity.name
                            },
                            { issue ->
                                issue.type.name
                            },
                            { issue ->
                                issue.nutrientKey.orEmpty()
                            },
                            { issue ->
                                issue.message
                            }
                        )
                    )
        ) {
            "Validation issues must be deterministically sorted."
        }
    }
}