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
                candidate.canonicalId.trim() ==
                        canonicalId
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
                                candidate.nutrition[
                                    nutrientKey
                                ]
                            }
                            .sorted()

                    createStatistics(
                        observations =
                            observations
                    )
                }
                .toSortedMap()

        val medianNutrition =
            nutrientStatistics
                .mapValues { (_, statistics) ->
                    statistics.median
                }
                .toSortedMap()

        val relationConsistentNutrition =
            makeRelationConsistent(
                medianNutrition =
                    medianNutrition,
                nutrientStatistics =
                    nutrientStatistics,
                candidates =
                    sortedCandidates
            )

        require(
            relationConsistentNutrition.keys ==
                    nutrientStatistics.keys
        ) {
            "Relation-consistent nutrition and nutrient statistics " +
                    "must have identical keys."
        }

        val representative =
            selectRepresentative(
                candidates =
                    sortedCandidates,
                aggregateNutrition =
                    relationConsistentNutrition
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
                relationConsistentNutrition,
            nutrientStatistics =
                nutrientStatistics.toSortedMap(),
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

    /**
     * Einzelne Nährstoffmediane können aus unterschiedlichen
     * Produktprofilen stammen. Dadurch können fachlich unmögliche
     * Kombinationen entstehen:
     *
     * saturatedFat > fat
     * sugars > carbohydrates
     *
     * Bei einer inkonsistenten Beziehung wird deterministisch das
     * nächstgelegene reale und konsistente Kandidatenpaar verwendet.
     *
     * Existiert kein Kandidat mit einem vollständigen konsistenten Paar,
     * wird der nicht belastbare Child-Nährstoff mitsamt seiner Statistik
     * entfernt. Der unabhängig nutzbare Parent-Wert bleibt erhalten.
     */
    private fun makeRelationConsistent(
        medianNutrition: Map<String, Double>,
        nutrientStatistics:
        MutableMap<
                String,
                OFFNutritionReferenceNutrientStatistics
                >,
        candidates: List<CanonicalOFFNutritionReferenceCandidate>
    ): Map<String, Double> {

        val result =
            medianNutrition.toSortedMap()

        repairRelationship(
            nutrition =
                result,
            nutrientStatistics =
                nutrientStatistics,
            candidates =
                candidates,
            parentKey =
                FAT_KEY,
            childKey =
                SATURATED_FAT_KEY
        )

        repairRelationship(
            nutrition =
                result,
            nutrientStatistics =
                nutrientStatistics,
            candidates =
                candidates,
            parentKey =
                CARBOHYDRATES_KEY,
            childKey =
                SUGARS_KEY
        )

        return result.toSortedMap()
    }

    private fun repairRelationship(
        nutrition: MutableMap<String, Double>,
        nutrientStatistics:
        MutableMap<
                String,
                OFFNutritionReferenceNutrientStatistics
                >,
        candidates: List<CanonicalOFFNutritionReferenceCandidate>,
        parentKey: String,
        childKey: String
    ) {

        val aggregateParentValue =
            nutrition[parentKey]
                ?: return

        val aggregateChildValue =
            nutrition[childKey]
                ?: return

        if (
            isRelationshipValid(
                parentValue =
                    aggregateParentValue,
                childValue =
                    aggregateChildValue
            )
        ) {
            return
        }

        val fallbackCandidate =
            selectRelationshipRepresentative(
                candidates =
                    candidates,
                aggregateParentValue =
                    aggregateParentValue,
                aggregateChildValue =
                    aggregateChildValue,
                parentKey =
                    parentKey,
                childKey =
                    childKey
            )

        if (fallbackCandidate == null) {

            /*
             * Kein reales Profil enthält ein vollständiges und
             * konsistentes Parent-/Child-Paar.
             *
             * Der Parent-Wert ist eigenständig interpretierbar und bleibt
             * erhalten. Der Child-Wert ist ohne belastbare Relation nicht
             * als finaler Aggregatewert geeignet.
             *
             * Nutrition und Statistik werden synchron geändert, damit
             * ihre Schlüssel identisch bleiben.
             */
            nutrition.remove(
                childKey
            )

            nutrientStatistics.remove(
                childKey
            )

            return
        }

        nutrition[parentKey] =
            fallbackCandidate.nutrition
                .getValue(
                    parentKey
                )

        nutrition[childKey] =
            fallbackCandidate.nutrition
                .getValue(
                    childKey
                )
    }

    private fun selectRelationshipRepresentative(
        candidates: List<CanonicalOFFNutritionReferenceCandidate>,
        aggregateParentValue: Double,
        aggregateChildValue: Double,
        parentKey: String,
        childKey: String
    ): CanonicalOFFNutritionReferenceCandidate? =
        candidates
            .asSequence()
            .filter { candidate ->

                val parentValue =
                    candidate.nutrition[
                        parentKey
                    ]
                        ?: return@filter false

                val childValue =
                    candidate.nutrition[
                        childKey
                    ]
                        ?: return@filter false

                parentValue.isFinite() &&
                        childValue.isFinite() &&
                        isRelationshipValid(
                            parentValue =
                                parentValue,
                            childValue =
                                childValue
                        )
            }
            .minWithOrNull(
                compareBy<
                        CanonicalOFFNutritionReferenceCandidate
                        > { candidate ->

                    relationshipDistance(
                        candidate =
                            candidate,
                        aggregateParentValue =
                            aggregateParentValue,
                        aggregateChildValue =
                            aggregateChildValue,
                        parentKey =
                            parentKey,
                        childKey =
                            childKey
                    )
                }
                    .thenByDescending { candidate ->
                        candidate.nutrition.size
                    }
                    .thenByDescending { candidate ->
                        candidate.sourceConfidence
                    }
                    .thenByDescending { candidate ->
                        !candidate.productName.isNullOrBlank()
                    }
                    .thenByDescending { candidate ->
                        !candidate.categories.isNullOrBlank()
                    }
                    .thenByDescending { candidate ->
                        candidate.aliases.size
                    }
                    .thenBy { candidate ->
                        candidate.sourceId
                    }
            )

    private fun relationshipDistance(
        candidate: CanonicalOFFNutritionReferenceCandidate,
        aggregateParentValue: Double,
        aggregateChildValue: Double,
        parentKey: String,
        childKey: String
    ): Double {

        val candidateParentValue =
            candidate.nutrition
                .getValue(
                    parentKey
                )

        val candidateChildValue =
            candidate.nutrition
                .getValue(
                    childKey
                )

        return abs(
            candidateParentValue -
                    aggregateParentValue
        ) +
                abs(
                    candidateChildValue -
                            aggregateChildValue
                )
    }

    private fun isRelationshipValid(
        parentValue: Double,
        childValue: Double
    ): Boolean =
        childValue <=
                parentValue +
                RELATIONSHIP_TOLERANCE_GRAMS

    private fun createStatistics(
        observations: List<Double>
    ): OFFNutritionReferenceNutrientStatistics {

        require(observations.isNotEmpty()) {
            "Cannot create statistics without observations."
        }

        require(
            observations.all(
                Double::isFinite
            )
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
                median(
                    sortedValues =
                        sorted
                ),
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
            sortedValues.size /
                    2

        return if (
            sortedValues.size %
            2 ==
            1
        ) {
            sortedValues[
                middleIndex
            ]
        } else {
            val lower =
                sortedValues[
                    middleIndex - 1
                ]

            val upper =
                sortedValues[
                    middleIndex
                ]

            lower +
                    (
                            (
                                    upper -
                                            lower
                                    ) /
                                    2.0
                            )
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
                        > { candidate ->

                    distanceFromAggregate(
                        candidate =
                            candidate,
                        aggregateNutrition =
                            aggregateNutrition
                    )
                }
                    .thenByDescending { candidate ->
                        candidate.nutrition.size
                    }
                    .thenByDescending { candidate ->
                        candidate.sourceConfidence
                    }
                    .thenByDescending { candidate ->
                        !candidate.productName.isNullOrBlank()
                    }
                    .thenByDescending { candidate ->
                        !candidate.categories.isNullOrBlank()
                    }
                    .thenByDescending { candidate ->
                        candidate.aliases.size
                    }
                    .thenBy { candidate ->
                        candidate.sourceId
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
                    candidate.nutrition
                        .getValue(
                            key
                        ) -
                            aggregateNutrition
                                .getValue(
                                    key
                                )
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

        const val FAT_KEY =
            "fatPer100g"

        const val SATURATED_FAT_KEY =
            "saturatedFatPer100g"

        const val CARBOHYDRATES_KEY =
            "carbohydratesPer100g"

        const val SUGARS_KEY =
            "sugarsPer100g"

        const val RELATIONSHIP_TOLERANCE_GRAMS =
            0.5
    }
}