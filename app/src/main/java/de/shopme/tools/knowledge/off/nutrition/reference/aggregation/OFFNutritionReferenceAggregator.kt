package de.shopme.tools.knowledge.off.nutrition.reference.aggregation

import de.shopme.tools.knowledge.off.nutrition.reference.CanonicalOFFNutritionReferenceCandidate
import kotlin.math.abs

class OFFNutritionReferenceAggregator {

    fun aggregate(
        candidates: List<CanonicalOFFNutritionReferenceCandidate>
    ): OFFNutritionReferenceAggregationResult {

        val groupedCandidates =
            candidates
                .groupBy { candidate ->
                    candidate.canonicalId.trim()
                }
                .toSortedMap()

        val aggregates =
            groupedCandidates
                .map { (canonicalId, group) ->
                    aggregateGroup(
                        canonicalId =
                            canonicalId,
                        candidates =
                            group
                    )
                }
                .sortedBy { aggregate ->
                    aggregate.canonicalId
                }

        return OFFNutritionReferenceAggregationResult(
            inputCandidateCount =
                candidates.size,
            aggregateCount =
                aggregates.size,
            multiProfileAggregateCount =
                aggregates.count { aggregate ->
                    aggregate.profileCount > 1
                },
            singleProfileAggregateCount =
                aggregates.count { aggregate ->
                    aggregate.profileCount == 1
                },
            maximumProfileCount =
                aggregates.maxOfOrNull { aggregate ->
                    aggregate.profileCount
                } ?: 0,
            aggregates =
                aggregates
        )
    }

    private fun aggregateGroup(
        canonicalId: String,
        candidates: List<CanonicalOFFNutritionReferenceCandidate>
    ): CanonicalOFFNutritionReferenceAggregate {

        require(candidates.isNotEmpty()) {
            "Cannot aggregate empty OFF nutrition candidate group."
        }

        require(
            candidates.all { candidate ->
                candidate.canonicalId.trim() == canonicalId
            }
        ) {
            "Aggregate group contains multiple canonical IDs."
        }

        val sortedCandidates =
            candidates.sortedBy { candidate ->
                candidate.sourceId
            }

        val nutrientKeys =
            sortedCandidates
                .asSequence()
                .flatMap { candidate ->
                    candidate.nutrition.keys.asSequence()
                }
                .distinct()
                .sorted()
                .toList()

        val nutrientStatistics =
            nutrientKeys
                .associateWith { nutrientKey ->
                    val observations =
                        sortedCandidates
                            .mapNotNull { candidate ->
                                candidate.nutrition[nutrientKey]
                            }
                            .sorted()

                    createStatistics(
                        observations =
                            observations
                    )
                }
                .toSortedMap()

        val aggregatedNutrition =
            nutrientStatistics
                .mapValues { (_, statistics) ->
                    statistics.median
                }
                .toSortedMap()

        val representative =
            selectRepresentative(
                candidates =
                    sortedCandidates,
                aggregateNutrition =
                    aggregatedNutrition
            )

        val aliases =
            sortedCandidates
                .asSequence()
                .flatMap { candidate ->
                    candidate.aliases.asSequence()
                }
                .plus(
                    sequenceOf(
                        canonicalId
                    )
                )
                .plus(
                    sortedCandidates
                        .asSequence()
                        .mapNotNull { candidate ->
                            candidate.productName
                        }
                )
                .map(String::trim)
                .filter(String::isNotBlank)
                .toSortedSet()

        val matchAliases =
            sortedCandidates
                .asSequence()
                .flatMap { candidate ->
                    candidate.matchAliases.asSequence()
                }
                .map(String::trim)
                .filter(String::isNotBlank)
                .toSortedSet()

        val singleIngredientAliases =
            sortedCandidates
                .asSequence()
                .flatMap { candidate ->
                    candidate
                        .singleIngredientNutritionAliases
                        .asSequence()
                }
                .map(String::trim)
                .filter(String::isNotBlank)
                .toSortedSet()

        val sourceIds =
            sortedCandidates
                .map { candidate ->
                    candidate.sourceId
                }
                .distinct()
                .sorted()

        return CanonicalOFFNutritionReferenceAggregate(
            canonicalId =
                canonicalId,
            aliases =
                aliases,
            matchAliases =
                matchAliases,
            singleIngredientNutritionAliases =
                singleIngredientAliases,
            nutrition =
                aggregatedNutrition,
            nutrientStatistics =
                nutrientStatistics,
            profileCount =
                sortedCandidates.size,
            sourceIds =
                sourceIds,
            representativeSourceId =
                representative.sourceId,
            source =
                AGGREGATE_SOURCE,
            sourceVersion =
                AGGREGATE_SOURCE_VERSION,
            sourceConfidence =
                aggregateConfidence(
                    candidates =
                        sortedCandidates
                )
        )
    }

    private fun createStatistics(
        observations: List<Double>
    ): OFFNutritionReferenceNutrientStatistics {

        require(observations.isNotEmpty()) {
            "Cannot create statistics without observations."
        }

        require(
            observations.all(Double::isFinite)
        ) {
            "Nutrition aggregate observations must be finite."
        }

        val sorted =
            observations.sorted()

        return OFFNutritionReferenceNutrientStatistics(
            observationCount =
                sorted.size,
            minimum =
                sorted.first(),
            median =
                median(sorted),
            maximum =
                sorted.last()
        )
    }

    fun median(
        sortedValues: List<Double>
    ): Double {

        require(sortedValues.isNotEmpty()) {
            "Cannot calculate median of empty values."
        }

        require(
            sortedValues ==
                    sortedValues.sorted()
        ) {
            "Median input values must be sorted."
        }

        val middleIndex =
            sortedValues.size / 2

        return if (sortedValues.size % 2 == 1) {
            sortedValues[middleIndex]
        } else {
            val lower =
                sortedValues[middleIndex - 1]

            val upper =
                sortedValues[middleIndex]

            lower + ((upper - lower) / 2.0)
        }
    }

    private fun selectRepresentative(
        candidates: List<CanonicalOFFNutritionReferenceCandidate>,
        aggregateNutrition: Map<String, Double>
    ): CanonicalOFFNutritionReferenceCandidate =
        candidates
            .minWithOrNull(
                compareBy<
                        CanonicalOFFNutritionReferenceCandidate
                        > {
                    distanceFromAggregate(
                        candidate =
                            it,
                        aggregateNutrition =
                            aggregateNutrition
                    )
                }
                    .thenByDescending {
                        it.nutrition.size
                    }
                    .thenByDescending {
                        it.sourceConfidence
                    }
                    .thenByDescending {
                        !it.productName.isNullOrBlank()
                    }
                    .thenByDescending {
                        !it.categories.isNullOrBlank()
                    }
                    .thenByDescending {
                        it.aliases.size
                    }
                    .thenBy {
                        it.sourceId
                    }
            )
            ?: error(
                "Cannot select representative from empty candidate group."
            )

    private fun distanceFromAggregate(
        candidate: CanonicalOFFNutritionReferenceCandidate,
        aggregateNutrition: Map<String, Double>
    ): Double {

        val commonKeys =
            candidate.nutrition.keys
                .intersect(
                    aggregateNutrition.keys
                )

        if (commonKeys.isEmpty()) {
            return Double.POSITIVE_INFINITY
        }

        val absoluteDifferenceSum =
            commonKeys.sumOf { key ->
                abs(
                    candidate.nutrition.getValue(key) -
                            aggregateNutrition.getValue(key)
                )
            }

        return absoluteDifferenceSum /
                commonKeys.size.toDouble()
    }

    private fun aggregateConfidence(
        candidates: List<CanonicalOFFNutritionReferenceCandidate>
    ): Double {

        val averageConfidence =
            candidates
                .map { candidate ->
                    candidate.sourceConfidence
                }
                .average()

        return averageConfidence
            .coerceIn(
                minimumValue =
                    0.0,
                maximumValue =
                    1.0
            )
    }

    private companion object {

        const val AGGREGATE_SOURCE =
            "open_food_facts_aggregate"

        const val AGGREGATE_SOURCE_VERSION =
            "1"
    }
}