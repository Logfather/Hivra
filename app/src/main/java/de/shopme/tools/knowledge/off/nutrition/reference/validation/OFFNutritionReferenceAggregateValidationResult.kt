package de.shopme.tools.knowledge.off.nutrition.reference.validation

import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.CanonicalOFFNutritionReferenceAggregate

data class OFFNutritionReferenceAggregateValidationResult(
    val inputAggregateCount: Int,
    val acceptedAggregateCount: Int,
    val warningAggregateCount: Int,
    val rejectedAggregateCount: Int,
    val totalWarningCount: Int,
    val totalErrorCount: Int,
    val validatedAggregates:
    List<CanonicalOFFNutritionReferenceAggregate>,
    val rejectedAggregates:
    List<CanonicalOFFNutritionReferenceAggregate>,
    val entries:
    List<OFFNutritionReferenceAggregateValidationEntry>
) {

    init {
        require(inputAggregateCount >= 0)
        require(acceptedAggregateCount >= 0)
        require(warningAggregateCount >= 0)
        require(rejectedAggregateCount >= 0)
        require(totalWarningCount >= 0)
        require(totalErrorCount >= 0)

        require(
            inputAggregateCount ==
                    acceptedAggregateCount +
                    warningAggregateCount +
                    rejectedAggregateCount
        ) {
            "Validation status counts do not cover all aggregates."
        }

        require(
            inputAggregateCount ==
                    entries.size
        ) {
            "Validation entries do not cover all aggregates."
        }

        require(
            validatedAggregates.size ==
                    acceptedAggregateCount +
                    warningAggregateCount
        ) {
            "Validated aggregates must contain accepted and warning entries."
        }

        require(
            rejectedAggregates.size ==
                    rejectedAggregateCount
        ) {
            "Rejected aggregate count does not match rejected aggregates."
        }

        require(
            totalWarningCount ==
                    entries.sumOf { entry ->
                        entry.warningCount
                    }
        ) {
            "totalWarningCount does not match validation entries."
        }

        require(
            totalErrorCount ==
                    entries.sumOf { entry ->
                        entry.errorCount
                    }
        ) {
            "totalErrorCount does not match validation entries."
        }

        require(
            entries ==
                    entries.sortedBy { entry ->
                        entry.canonicalId
                    }
        ) {
            "Validation entries must be sorted by canonicalId."
        }

        require(
            validatedAggregates ==
                    validatedAggregates.sortedBy { aggregate ->
                        aggregate.canonicalId
                    }
        ) {
            "Validated aggregates must be sorted by canonicalId."
        }

        require(
            rejectedAggregates ==
                    rejectedAggregates.sortedBy { aggregate ->
                        aggregate.canonicalId
                    }
        ) {
            "Rejected aggregates must be sorted by canonicalId."
        }

        val validatedIds =
            validatedAggregates
                .map { aggregate ->
                    aggregate.canonicalId
                }
                .toSet()

        val rejectedIds =
            rejectedAggregates
                .map { aggregate ->
                    aggregate.canonicalId
                }
                .toSet()

        require(
            validatedIds.intersect(rejectedIds).isEmpty()
        ) {
            "Validated and rejected aggregates must be disjoint."
        }

        require(
            validatedIds.size +
                    rejectedIds.size ==
                    inputAggregateCount
        ) {
            "Validated and rejected aggregates must cover all input aggregates."
        }
    }
}