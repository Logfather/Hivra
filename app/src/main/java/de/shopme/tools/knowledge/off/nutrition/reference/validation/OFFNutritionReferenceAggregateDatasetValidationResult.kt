package de.shopme.tools.knowledge.off.nutrition.reference.validation

data class OFFNutritionReferenceAggregateDatasetValidationResult(
    val inputAggregateCount: Int,
    val acceptedAggregateCount: Int,
    val warningAggregateCount: Int,
    val rejectedAggregateCount: Int,
    val totalProfileCount: Long,
    val totalWarningCount: Int,
    val totalErrorCount: Int,
    val issueCountsByType:
    Map<OFFNutritionReferenceAggregateIssueType, Int>,
    val warningEntries:
    List<OFFNutritionReferenceAggregateValidationEntry>,
    val rejectedEntries:
    List<OFFNutritionReferenceAggregateValidationEntry>
) {

    init {
        require(inputAggregateCount >= 0) {
            "inputAggregateCount must not be negative."
        }

        require(acceptedAggregateCount >= 0) {
            "acceptedAggregateCount must not be negative."
        }

        require(warningAggregateCount >= 0) {
            "warningAggregateCount must not be negative."
        }

        require(rejectedAggregateCount >= 0) {
            "rejectedAggregateCount must not be negative."
        }

        require(totalProfileCount >= 0L) {
            "totalProfileCount must not be negative."
        }

        require(totalWarningCount >= 0) {
            "totalWarningCount must not be negative."
        }

        require(totalErrorCount >= 0) {
            "totalErrorCount must not be negative."
        }

        require(
            inputAggregateCount ==
                    acceptedAggregateCount +
                    warningAggregateCount +
                    rejectedAggregateCount
        ) {
            "Validation status counts do not cover all aggregates."
        }

        require(
            warningEntries.size ==
                    warningAggregateCount
        ) {
            "warningEntries do not match warningAggregateCount."
        }

        require(
            rejectedEntries.size ==
                    rejectedAggregateCount
        ) {
            "rejectedEntries do not match rejectedAggregateCount."
        }

        require(
            totalWarningCount ==
                    warningEntries.sumOf { entry ->
                        entry.warningCount
                    } +
                    rejectedEntries.sumOf { entry ->
                        entry.warningCount
                    }
        ) {
            "totalWarningCount does not match persisted entries."
        }

        require(
            totalErrorCount ==
                    rejectedEntries.sumOf { entry ->
                        entry.errorCount
                    }
        ) {
            "totalErrorCount does not match rejected entries."
        }

        require(
            warningEntries.all { entry ->
                entry.status ==
                        OFFNutritionReferenceAggregateValidationStatus.WARNING
            }
        ) {
            "warningEntries contain a non-warning entry."
        }

        require(
            rejectedEntries.all { entry ->
                entry.status ==
                        OFFNutritionReferenceAggregateValidationStatus.REJECTED
            }
        ) {
            "rejectedEntries contain a non-rejected entry."
        }

        require(
            warningEntries ==
                    warningEntries.sortedBy { entry ->
                        entry.canonicalId
                    }
        ) {
            "warningEntries must be sorted by canonicalId."
        }

        require(
            rejectedEntries ==
                    rejectedEntries.sortedBy { entry ->
                        entry.canonicalId
                    }
        ) {
            "rejectedEntries must be sorted by canonicalId."
        }

        require(
            issueCountsByType ==
                    issueCountsByType.toSortedMap(
                        compareBy { type ->
                            type.name
                        }
                    )
        ) {
            "issueCountsByType must be deterministically sorted."
        }
    }

    val valid: Boolean
        get() =
            rejectedAggregateCount == 0 &&
                    totalErrorCount == 0
}