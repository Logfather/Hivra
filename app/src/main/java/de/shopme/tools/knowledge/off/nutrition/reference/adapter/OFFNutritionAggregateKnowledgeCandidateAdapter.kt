package de.shopme.tools.knowledge.off.nutrition.reference.adapter

import de.shopme.tools.knowledge.ki_candidates.CandidateMetadata
import de.shopme.tools.knowledge.ki_candidates.CanonicalKnowledgeCandidate
import de.shopme.tools.knowledge.ki_candidates.KnowledgeDimensionCandidate
import de.shopme.tools.knowledge.ki_candidates.KnowledgeDimensionCandidateType
import de.shopme.tools.knowledge.off.nutrition.reference.aggregation.CanonicalOFFNutritionReferenceAggregate

/**
 * Übersetzt ein validiertes kanonisches OFF-Nutrition-Aggregat
 * in das generische Eingabeformat des Knowledge Builds.
 *
 * Der Adapter führt keine erneute fachliche Validierung oder
 * Normalisierung durch. Er erhält die bereits validierten und
 * deterministisch persistierten Werte des Aggregate-Datasets.
 */
class OFFNutritionAggregateKnowledgeCandidateAdapter {

    fun adapt(
        aggregate: CanonicalOFFNutritionReferenceAggregate
    ): CanonicalKnowledgeCandidate {

        val aliases =
            aggregate.aliases
                .asSequence()
                .map(String::trim)
                .filter(String::isNotBlank)
                .toSortedSet()

        val matchAliases =
            sequenceOf(
                aggregate.matchAliases,
                aggregate.singleIngredientNutritionAliases
            )
                .flatMap { aliases ->
                    aliases.asSequence()
                }
                .map(String::trim)
                .filter(String::isNotBlank)
                .filterNot { alias ->
                    alias == aggregate.canonicalId
                }
                .filterNot { alias ->
                    alias in aliases
                }
                .toSortedSet()

        return CanonicalKnowledgeCandidate(
            canonicalId =
                aggregate.canonicalId,
            aliases =
                aliases,
            matchAliases =
                matchAliases,
            dimensions =
                listOf(
                    KnowledgeDimensionCandidate(
                        dimension =
                            KnowledgeDimensionCandidateType.NUTRITION,
                        payload =
                            aggregate.nutrition.toSortedMap()
                    )
                ),
            metadata =
                CandidateMetadata(
                    source =
                        aggregate.source,
                    sourceId =
                        aggregate.representativeSourceId,
                    confidence =
                        aggregate.sourceConfidence,
                    version =
                        aggregate.sourceVersion,
                    attributes =
                        sortedMapOf(
                            PROFILE_COUNT_ATTRIBUTE to
                                    aggregate.profileCount.toString(),
                            SOURCE_ID_COUNT_ATTRIBUTE to
                                    aggregate.sourceIds.size.toString()
                        )
                )
        )
    }

    fun adapt(
        aggregates:
        Iterable<CanonicalOFFNutritionReferenceAggregate>
    ): List<CanonicalKnowledgeCandidate> =
        aggregates
            .map { aggregate ->
                adapt(
                    aggregate =
                        aggregate
                )
            }

    companion object {

        const val PROFILE_COUNT_ATTRIBUTE =
            "aggregateProfileCount"

        const val SOURCE_ID_COUNT_ATTRIBUTE =
            "aggregateSourceIdCount"
    }
}