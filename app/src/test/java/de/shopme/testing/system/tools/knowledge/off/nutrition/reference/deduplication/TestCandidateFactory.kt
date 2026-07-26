package de.shopme.testing.system.tools.knowledge.off.nutrition.reference.deduplication

import de.shopme.tools.knowledge.off.nutrition.reference.CanonicalOFFNutritionReferenceCandidate

object TestCandidateFactory {

    fun create(
        sourceId: String,
        canonicalId: String
    ): CanonicalOFFNutritionReferenceCandidate =
        CanonicalOFFNutritionReferenceCandidate(
            sourceId =
                sourceId,
            canonicalId =
                canonicalId,
            aliases =
                sortedSetOf(canonicalId),
            matchAliases =
                emptySet(),
            nutrition =
                sortedMapOf(
                    "energyKcalPer100g" to 61.0,
                    "fatPer100g" to 3.3,
                    "carbohydratesPer100g" to 4.7,
                    "proteinsPer100g" to 3.5,
                    "saltPer100g" to 0.1
                ),
            productName =
                canonicalId,
            brand =
                null,
            categories =
                null,
            singleIngredientNutritionAliases =
                emptySet(),
            source =
                "open_food_facts",
            sourceVersion =
                "1",
            sourceConfidence =
                1.0
        )
}