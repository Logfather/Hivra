package de.shopme.tools.knowledge.off.nutrition.reference

import de.shopme.tools.knowledge.ki_candidates.CanonicalKnowledgeCandidate
import de.shopme.tools.knowledge.ki_candidates.KnowledgeDimensionCandidate
import de.shopme.tools.knowledge.ki_candidates.KnowledgeDimensionCandidateType

class OFFNutritionReferenceCandidateGenerator {

    fun generate(
        candidates: List<CanonicalKnowledgeCandidate>
    ): OFFNutritionReferenceCandidateGenerationResult {

        val generated =
            mutableListOf<CanonicalOFFNutritionReferenceCandidate>()

        var skippedWithoutNutritionCount =
            0

        var skippedInvalidIdentityCount =
            0

        var skippedInvalidNutritionPayloadCount =
            0

        candidates.forEach { candidate ->

            val sourceId =
                candidate.metadata.sourceId
                    ?.trim()
                    ?.takeIf(String::isNotBlank)

            val canonicalId =
                candidate.canonicalId
                    .trim()
                    .takeIf(String::isNotBlank)

            if (sourceId == null || canonicalId == null) {
                skippedInvalidIdentityCount++
                return@forEach
            }

            val nutritionDimensions =
                candidate.dimensions
                    .filter { dimension ->
                        dimension.dimension ==
                                KnowledgeDimensionCandidateType.NUTRITION
                    }

            if (nutritionDimensions.isEmpty()) {
                skippedWithoutNutritionCount++
                return@forEach
            }

            if (nutritionDimensions.size > 1) {
                skippedInvalidNutritionPayloadCount++
                return@forEach
            }

            val nutritionDimension =
                nutritionDimensions.single()

            val nutrition =
                parseNutritionPayload(
                    dimension =
                        nutritionDimension
                )

            if (nutrition == null) {
                skippedInvalidNutritionPayloadCount++
                return@forEach
            }

            generated +=
                CanonicalOFFNutritionReferenceCandidate(
                    sourceId =
                        sourceId,
                    canonicalId =
                        canonicalId,
                    aliases =
                        candidate.aliases
                            .normalizeStrings(),
                    matchAliases =
                        candidate.matchAliases
                            .normalizeStrings(),
                    nutrition =
                        nutrition,
                    productName =
                        candidate.metadata.attributes["productName"]
                            ?.normalizeOptionalString(),
                    brand =
                        candidate.metadata.attributes["brand"]
                            ?.normalizeOptionalString(),
                    categories =
                        candidate.metadata.attributes["categories"]
                            ?.normalizeOptionalString(),
                    singleIngredientNutritionAliases =
                        candidate.metadata
                            .attributes["singleIngredientNutritionAliases"]
                            .toStringSet(),
                    source =
                        requireNotNull(candidate.metadata.source) {
                            "Missing source for ${candidate.canonicalId}"
                        },
                    sourceVersion =
                        candidate.metadata.version
                            ?: "1",
                    sourceConfidence =
                        candidate.metadata.confidence
                )
        }

        val sorted =
            generated.sortedWith(
                compareBy<CanonicalOFFNutritionReferenceCandidate>(
                    { it.sourceId },
                    { it.canonicalId }
                )
            )

        return OFFNutritionReferenceCandidateGenerationResult(
            inputCandidateCount =
                candidates.size,
            generatedCandidateCount =
                sorted.size,
            skippedWithoutNutritionCount =
                skippedWithoutNutritionCount,
            skippedInvalidIdentityCount =
                skippedInvalidIdentityCount,
            skippedInvalidNutritionPayloadCount =
                skippedInvalidNutritionPayloadCount,
            candidates =
                sorted
        )
    }

    private fun parseNutritionPayload(
        dimension: KnowledgeDimensionCandidate
    ): Map<String, Double>? {

        val rawPayload =
            dimension.payload as? Map<*, *>
                ?: return null

        val values =
            rawPayload
                .mapNotNull { (rawKey, rawValue) ->

                    val key =
                        rawKey as? String
                            ?: return@mapNotNull null

                    val value =
                        rawValue as? Number
                            ?: return@mapNotNull null

                    key to value.toDouble()
                }
                .toMap()

        if (values.isEmpty()) {
            return null
        }

        if (values.keys.any { key ->
                key !in SUPPORTED_NUTRITION_KEYS
            }
        ) {
            return null
        }

        if (values.values.any { value ->
                !value.isFinite()
            }
        ) {
            return null
        }

        return values
            .toSortedMap()
    }

    private fun Set<String>.normalizeStrings(): Set<String> {
        return asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSortedSet()
    }

    private fun String.normalizeOptionalString(): String? {
        return trim()
            .takeIf(String::isNotBlank)
    }

    private fun String?.toStringSet(): Set<String> {

        if (isNullOrBlank()) {
            return emptySet()
        }

        return split("|")
            .asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSortedSet()
    }

    private companion object {

        val SUPPORTED_NUTRITION_KEYS =
            setOf(
                "energyKcalPer100g",
                "fatPer100g",
                "saturatedFatPer100g",
                "carbohydratesPer100g",
                "sugarsPer100g",
                "fiberPer100g",
                "proteinsPer100g",
                "saltPer100g"
            )
    }
}