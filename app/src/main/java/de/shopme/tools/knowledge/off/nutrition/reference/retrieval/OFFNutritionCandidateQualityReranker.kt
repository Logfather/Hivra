package de.shopme.tools.knowledge.off.nutrition.reference.retrieval

import kotlin.math.max
import kotlin.math.min

class OFFNutritionCandidateQualityReranker {

    fun rerank(
        request: CatalogOFFNutritionRetrievalRequest,
        candidates: List<CatalogOFFNutritionRetrievedCandidate>,
        maximumCandidateCount: Int
    ): List<CatalogOFFNutritionRetrievedCandidate> {

        require(maximumCandidateCount > 0)

        val catalogTokens =
            request.catalogTerms
                .asSequence()
                .flatMap { term ->
                    normalizedTokens(term).asSequence()
                }
                .toSet()

        return candidates
            .map { candidate ->
                rerankCandidate(
                    catalogTokens = catalogTokens,
                    candidate = candidate
                )
            }
            .sortedWith(
                CatalogOFFNutritionRetrievedCandidate.DETERMINISTIC_COMPARATOR
            )
            .take(maximumCandidateCount)
            .mapIndexed { index, candidate ->
                candidate.copy(
                    rank = index + 1
                )
            }
    }

    private fun rerankCandidate(
        catalogTokens: Set<String>,
        candidate: CatalogOFFNutritionRetrievedCandidate
    ): CatalogOFFNutritionRetrievedCandidate {

        if (candidate.exactMatch) {
            return candidate.copy(
                score = 1.0
            )
        }

        val candidateTokens =
            normalizedTokens(
                candidate.matchedCandidateAlias
            )

        var adjustedScore =
            candidate.score

        adjustedScore +=
            calculateHeadTokenBonus(
                catalogTokens = catalogTokens,
                candidateTokens = candidateTokens
            )

        adjustedScore -=
            calculateGenericOnlyPenalty(
                catalogTokens = catalogTokens,
                candidateTokens = candidateTokens
            )

        adjustedScore -=
            calculateIntroducedProductFormPenalty(
                catalogTokens = catalogTokens,
                candidateTokens = candidateTokens
            )

        adjustedScore -=
            calculateAnimalSpeciesPenalty(
                catalogTokens = catalogTokens,
                candidateTokens = candidateTokens
            )

        adjustedScore -=
            calculateDietaryMismatchPenalty(
                catalogTokens = catalogTokens,
                candidateTokens = candidateTokens
            )

        adjustedScore -=
            calculateFrozenOnlyPenalty(
                catalogTokens = catalogTokens,
                candidateTokens = candidateTokens
            )

        return candidate.copy(
            score =
                min(
                    1.0,
                    max(
                        0.0,
                        adjustedScore
                    )
                )
        )
    }

    private fun calculateHeadTokenBonus(
        catalogTokens: Set<String>,
        candidateTokens: Set<String>
    ): Double {

        val catalogCoreTokens =
            catalogTokens -
                    GENERIC_MODIFIER_TOKENS -
                    PROCESSING_TOKENS

        val candidateCoreTokens =
            candidateTokens -
                    GENERIC_MODIFIER_TOKENS -
                    PROCESSING_TOKENS

        if (
            catalogCoreTokens.isNotEmpty() &&
            catalogCoreTokens.any(candidateCoreTokens::contains)
        ) {
            return CORE_TOKEN_BONUS
        }

        return 0.0
    }

    private fun calculateGenericOnlyPenalty(
        catalogTokens: Set<String>,
        candidateTokens: Set<String>
    ): Double {

        val sharedTokens =
            catalogTokens intersect candidateTokens

        if (
            sharedTokens.isNotEmpty() &&
            sharedTokens.all(GENERIC_MODIFIER_TOKENS::contains)
        ) {
            return GENERIC_ONLY_PENALTY
        }

        return 0.0
    }

    private fun calculateIntroducedProductFormPenalty(
        catalogTokens: Set<String>,
        candidateTokens: Set<String>
    ): Double {

        val introducedProductForms =
            (
                    candidateTokens intersect
                            DERIVED_PRODUCT_FORM_TOKENS
                    ) -
                    catalogTokens

        return when {
            introducedProductForms.isEmpty() -> {
                0.0
            }

            introducedProductForms.size == 1 -> {
                INTRODUCED_PRODUCT_FORM_PENALTY
            }

            else -> {
                MULTIPLE_PRODUCT_FORM_PENALTY
            }
        }
    }

    private fun calculateAnimalSpeciesPenalty(
        catalogTokens: Set<String>,
        candidateTokens: Set<String>
    ): Double {

        val catalogSpecies =
            catalogTokens intersect
                    ANIMAL_SPECIES_TOKENS

        val candidateSpecies =
            candidateTokens intersect
                    ANIMAL_SPECIES_TOKENS

        if (
            catalogSpecies.isNotEmpty() &&
            candidateSpecies.isNotEmpty() &&
            catalogSpecies.intersect(candidateSpecies).isEmpty()
        ) {
            return ANIMAL_SPECIES_MISMATCH_PENALTY
        }

        return 0.0
    }

    private fun calculateDietaryMismatchPenalty(
        catalogTokens: Set<String>,
        candidateTokens: Set<String>
    ): Double {

        val catalogDietaryTokens =
            catalogTokens intersect
                    DIETARY_TOKENS

        if (
            catalogDietaryTokens.isNotEmpty() &&
            catalogDietaryTokens.none(candidateTokens::contains)
        ) {
            return DIETARY_MISMATCH_PENALTY
        }

        return 0.0
    }

    private fun calculateFrozenOnlyPenalty(
        catalogTokens: Set<String>,
        candidateTokens: Set<String>
    ): Double {

        if (
            "frozen" in candidateTokens &&
            "frozen" !in catalogTokens
        ) {
            return INTRODUCED_FROZEN_PENALTY
        }

        return 0.0
    }

    private fun normalizedTokens(
        value: String
    ): Set<String> {

        return OFFNutritionRetrievalTextNormalizer
            .normalize(value)
            .split(' ')
            .asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSet()
    }

    companion object {

        private const val CORE_TOKEN_BONUS =
            0.08

        private const val GENERIC_ONLY_PENALTY =
            0.30

        private const val INTRODUCED_PRODUCT_FORM_PENALTY =
            0.22

        private const val MULTIPLE_PRODUCT_FORM_PENALTY =
            0.30

        private const val ANIMAL_SPECIES_MISMATCH_PENALTY =
            0.45

        private const val DIETARY_MISMATCH_PENALTY =
            0.35

        private const val INTRODUCED_FROZEN_PENALTY =
            0.08

        private val GENERIC_MODIFIER_TOKENS =
            setOf(
                "bio",
                "cold",
                "dried",
                "fresh",
                "frozen",
                "ground",
                "organic",
                "plain",
                "prepared",
                "raw",
                "ready",
                "smoked",
                "sweet",
                "wild"
            )

        private val PROCESSING_TOKENS =
            setOf(
                "baked",
                "boiled",
                "breaded",
                "cooked",
                "fried",
                "grilled",
                "pickled",
                "roasted",
                "steamed"
            )

        private val DERIVED_PRODUCT_FORM_TOKENS =
            setOf(
                "butter",
                "cake",
                "candy",
                "cream",
                "drink",
                "flakes",
                "flour",
                "jam",
                "juice",
                "milk",
                "oil",
                "paste",
                "pie",
                "powder",
                "pulp",
                "sauce",
                "shake",
                "spread",
                "syrup",
                "tea",
                "yogurt"
            )

        private val ANIMAL_SPECIES_TOKENS =
            setOf(
                "beef",
                "boar",
                "chicken",
                "cod",
                "duck",
                "eel",
                "goat",
                "goose",
                "haddock",
                "herring",
                "lamb",
                "pork",
                "rabbit",
                "salmon",
                "trout",
                "turkey",
                "veal",
                "venison"
            )

        private val DIETARY_TOKENS =
            setOf(
                "plant",
                "plantbased",
                "substitute",
                "vegan",
                "vegetarian"
            )
    }
}