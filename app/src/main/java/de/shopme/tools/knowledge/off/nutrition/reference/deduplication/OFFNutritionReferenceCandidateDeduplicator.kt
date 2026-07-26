package de.shopme.tools.knowledge.off.nutrition.reference.deduplication

import de.shopme.tools.knowledge.off.nutrition.reference.CanonicalOFFNutritionReferenceCandidate
import java.math.BigDecimal

class OFFNutritionReferenceCandidateDeduplicator {

    fun deduplicate(
        candidates: List<CanonicalOFFNutritionReferenceCandidate>
    ): OFFNutritionReferenceDeduplicationResult {

        val groups =
            candidates
                .groupBy { candidate ->
                    deduplicationKey(candidate)
                }
                .toSortedMap(KEY_COMPARATOR)

        val deduplicatedCandidates =
            mutableListOf<CanonicalOFFNutritionReferenceCandidate>()

        val duplicateGroups =
            mutableListOf<OFFNutritionReferenceDuplicateGroup>()

        groups.forEach { (key, groupedCandidates) ->

            val sortedGroup =
                groupedCandidates.sortedWith(
                    REPRESENTATIVE_COMPARATOR
                )

            val representative =
                sortedGroup.first()

            val mergedCandidate =
                mergeGroup(
                    representative =
                        representative,
                    groupedCandidates =
                        sortedGroup
                )

            deduplicatedCandidates +=
                mergedCandidate

            if (sortedGroup.size > 1) {
                duplicateGroups +=
                    OFFNutritionReferenceDuplicateGroup(
                        canonicalId =
                            key.canonicalId,
                        nutritionFingerprint =
                            key.nutritionFingerprint,
                        representativeSourceId =
                            representative.sourceId,
                        mergedSourceIds =
                            sortedGroup
                                .map { candidate ->
                                    candidate.sourceId
                                }
                                .distinct()
                                .sorted()
                    )
            }
        }

        val sortedCandidates =
            deduplicatedCandidates.sortedWith(
                OUTPUT_COMPARATOR
            )

        val sortedDuplicateGroups =
            duplicateGroups.sortedWith(
                DUPLICATE_GROUP_COMPARATOR
            )

        return OFFNutritionReferenceDeduplicationResult(
            inputCandidateCount =
                candidates.size,
            outputCandidateCount =
                sortedCandidates.size,
            removedDuplicateCount =
                candidates.size - sortedCandidates.size,
            duplicateGroupCount =
                sortedDuplicateGroups.size,
            candidates =
                sortedCandidates,
            duplicateGroups =
                sortedDuplicateGroups
        )
    }

    fun deduplicationKey(
        candidate: CanonicalOFFNutritionReferenceCandidate
    ): OFFNutritionReferenceDeduplicationKey =
        OFFNutritionReferenceDeduplicationKey(
            canonicalId =
                candidate.canonicalId.trim(),
            nutritionFingerprint =
                nutritionFingerprint(
                    nutrition =
                        candidate.nutrition
                )
        )

    fun nutritionFingerprint(
        nutrition: Map<String, Double>
    ): String {

        require(nutrition.isNotEmpty()) {
            "Cannot create fingerprint for empty nutrition payload."
        }

        return nutrition
            .toSortedMap()
            .entries
            .joinToString(
                separator =
                    "|"
            ) { (key, value) ->
                "$key=${normalizeDouble(value)}"
            }
    }

    private fun mergeGroup(
        representative: CanonicalOFFNutritionReferenceCandidate,
        groupedCandidates:
        List<CanonicalOFFNutritionReferenceCandidate>
    ): CanonicalOFFNutritionReferenceCandidate {

        val mergedAliases =
            groupedCandidates
                .asSequence()
                .flatMap { candidate ->
                    candidate.aliases.asSequence()
                }
                .plus(
                    groupedCandidates
                        .asSequence()
                        .mapNotNull { candidate ->
                            candidate.productName
                        }
                )
                .map(String::trim)
                .filter(String::isNotBlank)
                .toSortedSet()

        val mergedMatchAliases =
            groupedCandidates
                .asSequence()
                .flatMap { candidate ->
                    candidate.matchAliases.asSequence()
                }
                .map(String::trim)
                .filter(String::isNotBlank)
                .toSortedSet()

        val mergedSingleIngredientAliases =
            groupedCandidates
                .asSequence()
                .flatMap { candidate ->
                    candidate
                        .singleIngredientNutritionAliases
                        .asSequence()
                }
                .map(String::trim)
                .filter(String::isNotBlank)
                .toSortedSet()

        return representative.copy(
            aliases =
                mergedAliases,
            matchAliases =
                mergedMatchAliases,
            nutrition =
                representative.nutrition.toSortedMap(),
            singleIngredientNutritionAliases =
                mergedSingleIngredientAliases
        )
    }

    /**
     * Repräsentantenauswahl:
     *
     * 1. höheres sourceConfidence
     * 2. mehr Nutrition-Felder
     * 3. vorhandener Produktname
     * 4. vorhandene Kategorien
     * 5. vorhandene Marke
     * 6. mehr Single-Ingredient-Aliase
     * 7. mehr reguläre Aliase
     * 8. lexikografisch kleinste sourceId
     *
     * Die letzten Kriterien sorgen für eine vollständig stabile Auswahl.
     */
    private val REPRESENTATIVE_COMPARATOR =
        compareByDescending<CanonicalOFFNutritionReferenceCandidate> {
            it.sourceConfidence
        }
            .thenByDescending {
                it.nutrition.size
            }
            .thenByDescending {
                !it.productName.isNullOrBlank()
            }
            .thenByDescending {
                !it.categories.isNullOrBlank()
            }
            .thenByDescending {
                !it.brand.isNullOrBlank()
            }
            .thenByDescending {
                it.singleIngredientNutritionAliases.size
            }
            .thenByDescending {
                it.aliases.size
            }
            .thenBy {
                it.sourceId
            }
            .thenBy {
                it.canonicalId
            }

    private fun normalizeDouble(
        value: Double
    ): String {

        require(value.isFinite()) {
            "Nutrition fingerprint value must be finite: $value"
        }

        return BigDecimal
            .valueOf(value)
            .stripTrailingZeros()
            .toPlainString()
    }

    companion object {

        val OUTPUT_COMPARATOR =
            compareBy<CanonicalOFFNutritionReferenceCandidate>(
                { it.canonicalId },
                { it.sourceId }
            )

        private val KEY_COMPARATOR =
            compareBy<OFFNutritionReferenceDeduplicationKey>(
                { it.canonicalId },
                { it.nutritionFingerprint }
            )

        private val DUPLICATE_GROUP_COMPARATOR =
            compareBy<OFFNutritionReferenceDuplicateGroup>(
                { it.canonicalId },
                { it.nutritionFingerprint },
                { it.representativeSourceId }
            )
    }
}