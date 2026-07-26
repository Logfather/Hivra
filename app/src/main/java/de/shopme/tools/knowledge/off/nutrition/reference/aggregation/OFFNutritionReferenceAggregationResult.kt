package de.shopme.tools.knowledge.off.nutrition.reference.aggregation

data class OFFNutritionReferenceAggregationResult(
    val inputCandidateCount: Int,
    val aggregateCount: Int,
    val multiProfileAggregateCount: Int,
    val singleProfileAggregateCount: Int,
    val maximumProfileCount: Int,
    val aggregates: List<CanonicalOFFNutritionReferenceAggregate>
) {

    init {
        require(inputCandidateCount >= 0) {
            "inputCandidateCount must not be negative."
        }

        require(aggregateCount >= 0) {
            "aggregateCount must not be negative."
        }

        require(multiProfileAggregateCount >= 0) {
            "multiProfileAggregateCount must not be negative."
        }

        require(singleProfileAggregateCount >= 0) {
            "singleProfileAggregateCount must not be negative."
        }

        require(maximumProfileCount >= 0) {
            "maximumProfileCount must not be negative."
        }

        require(aggregateCount == aggregates.size) {
            "aggregateCount must equal aggregates.size."
        }

        require(
            aggregateCount ==
                    multiProfileAggregateCount +
                    singleProfileAggregateCount
        ) {
            "Single- and multi-profile counts must cover all aggregates."
        }

        require(
            inputCandidateCount ==
                    aggregates.sumOf { aggregate ->
                        aggregate.profileCount
                    }
        ) {
            "Aggregate profile counts must cover all input candidates."
        }

        val expectedMaximum =
            aggregates.maxOfOrNull { aggregate ->
                aggregate.profileCount
            } ?: 0

        require(maximumProfileCount == expectedMaximum) {
            "maximumProfileCount does not match aggregates."
        }

        require(
            aggregates ==
                    aggregates.sortedBy { aggregate ->
                        aggregate.canonicalId
                    }
        ) {
            "Canonical OFF nutrition aggregates must be sorted."
        }

        require(
            aggregates
                .map { aggregate ->
                    aggregate.canonicalId
                }
                .distinct()
                .size ==
                    aggregates.size
        ) {
            "Canonical OFF nutrition aggregates must have unique IDs."
        }
    }
}