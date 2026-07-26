package de.shopme.tools.knowledge.off.nutrition.reference.training

import de.shopme.tools.knowledge.off.nutrition.reference.aggregation
.CanonicalOFFNutritionReferenceAggregate
import de.shopme.tools.knowledge.off.nutrition.reference.validation
.OFFNutritionReferenceAggregateValidationEntry
import de.shopme.tools.knowledge.off.nutrition.reference.validation
.OFFNutritionReferenceAggregateValidationResult
import de.shopme.tools.knowledge.off.nutrition.reference.validation
.OFFNutritionReferenceAggregateValidationStatus

class OFFNutritionMatcherTrainingCandidateExporter {

    fun export(
        validationResult:
        OFFNutritionReferenceAggregateValidationResult
    ): OFFNutritionMatcherTrainingCandidateExportResult {

        require(validationResult.rejectedAggregateCount == 0) {
            "Rejected OFF nutrition aggregates must not be exported as " +
                    "matcher training candidates."
        }

        val validationEntryByCanonicalId =
            validationResult.entries
                .associateBy { entry ->
                    entry.canonicalId
                }

        require(
            validationEntryByCanonicalId.size ==
                    validationResult.entries.size
        ) {
            "Validation entries contain duplicate canonical IDs."
        }

        val candidates =
            validationResult.validatedAggregates
                .map { aggregate ->
                    val validationEntry =
                        requireNotNull(
                            validationEntryByCanonicalId[
                                aggregate.canonicalId
                            ]
                        ) {
                            "Missing validation entry for aggregate: " +
                                    aggregate.canonicalId
                        }

                    createCandidate(
                        aggregate =
                            aggregate,
                        validationEntry =
                            validationEntry
                    )
                }
                .sortedBy { candidate ->
                    candidate.serverKey
                }

        val acceptedCandidateCount =
            candidates.count { candidate ->
                candidate.validationStatus ==
                        OFFNutritionReferenceAggregateValidationStatus
                            .ACCEPTED
            }

        val warningCandidateCount =
            candidates.count { candidate ->
                candidate.validationStatus ==
                        OFFNutritionReferenceAggregateValidationStatus
                            .WARNING
            }

        val uniqueRetrievalAliasCount =
            candidates
                .asSequence()
                .flatMap { candidate ->
                    candidate.retrievalAliases.asSequence()
                }
                .distinct()
                .count()

        return OFFNutritionMatcherTrainingCandidateExportResult(
            inputAggregateCount =
                validationResult.inputAggregateCount,
            exportedCandidateCount =
                candidates.size,
            acceptedCandidateCount =
                acceptedCandidateCount,
            warningCandidateCount =
                warningCandidateCount,
            uniqueRetrievalAliasCount =
                uniqueRetrievalAliasCount,
            candidates =
                candidates
        )
    }

    private fun createCandidate(
        aggregate: CanonicalOFFNutritionReferenceAggregate,
        validationEntry:
        OFFNutritionReferenceAggregateValidationEntry
    ): OFFNutritionMatcherTrainingCandidate {

        require(
            validationEntry.status !=
                    OFFNutritionReferenceAggregateValidationStatus
                        .REJECTED
        ) {
            "Rejected aggregate must not be exported: " +
                    aggregate.canonicalId
        }

        require(
            aggregate.canonicalId ==
                    validationEntry.canonicalId
        ) {
            "Aggregate and validation entry canonical IDs differ."
        }

        val canonicalAliases =
            aggregate.aliases
                .normalizeStrings()

        val matchAliases =
            aggregate.matchAliases
                .normalizeStrings()

        val singleIngredientAliases =
            aggregate.singleIngredientNutritionAliases
                .normalizeStrings()

        val retrievalAliases =
            buildSet {
                add(
                    aggregate.canonicalId
                        .normalizeRequiredString()
                )

                addAll(canonicalAliases)
                addAll(matchAliases)
                addAll(singleIngredientAliases)
            }
                .toSortedSet()
                .toList()

        val validationIssueTypes =
            validationEntry.issues
                .map { issue ->
                    issue.type.name
                }
                .distinct()
                .sorted()

        return OFFNutritionMatcherTrainingCandidate(
            serverArtifact =
                OFFNutritionMatcherTrainingCandidate.SERVER_ARTIFACT,
            serverKey =
                aggregate.canonicalId
                    .normalizeRequiredString(),
            canonicalId =
                aggregate.canonicalId
                    .normalizeRequiredString(),
            retrievalAliases =
                retrievalAliases,
            canonicalAliases =
                canonicalAliases,
            matchAliases =
                matchAliases,
            singleIngredientNutritionAliases =
                singleIngredientAliases,
            nutrition =
                aggregate.nutrition.toSortedMap(),
            profileCount =
                aggregate.profileCount,
            nutrientCount =
                aggregate.nutrition.size,
            validationStatus =
                validationEntry.status,
            warningCount =
                validationEntry.warningCount,
            validationIssueTypes =
                validationIssueTypes,
            representativeSourceId =
                aggregate.representativeSourceId
                    .normalizeRequiredString(),
            sourceIds =
                aggregate.sourceIds
                    .asSequence()
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .distinct()
                    .sorted()
                    .toList(),
            source =
                aggregate.source
                    .normalizeRequiredString(),
            sourceVersion =
                aggregate.sourceVersion
                    .normalizeRequiredString(),
            sourceConfidence =
                aggregate.sourceConfidence
        )
    }

    private fun Set<String>.normalizeStrings(): List<String> =
        asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()
            .toList()

    private fun String.normalizeRequiredString(): String =
        trim()
            .also { normalized ->
                require(normalized.isNotBlank()) {
                    "Required matcher training candidate string is blank."
                }
            }
}