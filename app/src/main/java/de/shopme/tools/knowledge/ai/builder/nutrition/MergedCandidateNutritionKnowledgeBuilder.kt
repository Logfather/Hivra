package de.shopme.tools.knowledge.ai.builder.nutrition

import de.shopme.tools.knowledge.ki_candidates.CanonicalKnowledgeCandidate
import de.shopme.tools.knowledge.ki_candidates.KnowledgeDimensionCandidateType
import de.shopme.tools.knowledge.nutrition.NutritionFacts
import de.shopme.tools.knowledge.nutrition.NutritionFactsKnowledge

class MergedCandidateNutritionKnowledgeBuilder {

    fun build(
        candidates: List<CanonicalKnowledgeCandidate>
    ) =
        build(
            candidates.asSequence()
        )

    fun build(
        candidates: Sequence<CanonicalKnowledgeCandidate>
    ): NutritionFactsKnowledge {

        val entries =
            candidates
                .mapNotNull { candidate ->

                    val payload =
                        candidate.dimensions
                            .firstOrNull { dimension ->
                                dimension.dimension ==
                                        KnowledgeDimensionCandidateType
                                            .NUTRITION
                            }
                            ?.payload
                            ?: return@mapNotNull null

                    val nutrition =
                        payload.toNutritionFacts()
                            ?: return@mapNotNull null

                    val key =
                        candidate.canonicalId
                            .trim()

                    if (key.isBlank()) {
                        return@mapNotNull null
                    }

                    key to
                            nutrition
                }
                .toMap()
                .toSortedMap()

        return NutritionFactsKnowledge(
            entries
        )
    }

    private fun Any.toNutritionFacts():
            NutritionFacts? {

        if (this is NutritionFacts) {
            return this
        }

        if (this !is Map<*, *>) {
            return null
        }

        val presentNutrients =
            sortedSetOf<String>()

        return NutritionFacts(
            calories =
                doubleOrZero(
                    sourceKey =
                        "energyKcalPer100g",
                    runtimeKey =
                        NutritionFacts.CALORIES,
                    presentNutrients =
                        presentNutrients
                ),

            protein =
                doubleOrZero(
                    sourceKey =
                        "proteinsPer100g",
                    runtimeKey =
                        NutritionFacts.PROTEIN,
                    presentNutrients =
                        presentNutrients
                ),

            fat =
                doubleOrZero(
                    sourceKey =
                        "fatPer100g",
                    runtimeKey =
                        NutritionFacts.FAT,
                    presentNutrients =
                        presentNutrients
                ),

            saturatedFat =
                doubleOrZero(
                    sourceKey =
                        "saturatedFatPer100g",
                    runtimeKey =
                        NutritionFacts.SATURATED_FAT,
                    presentNutrients =
                        presentNutrients
                ),

            carbohydrates =
                doubleOrZero(
                    sourceKey =
                        "carbohydratesPer100g",
                    runtimeKey =
                        NutritionFacts.CARBOHYDRATES,
                    presentNutrients =
                        presentNutrients
                ),

            sugar =
                doubleOrZero(
                    sourceKey =
                        "sugarsPer100g",
                    runtimeKey =
                        NutritionFacts.SUGAR,
                    presentNutrients =
                        presentNutrients
                ),

            fiber =
                doubleOrZero(
                    sourceKey =
                        "fiberPer100g",
                    runtimeKey =
                        NutritionFacts.FIBER,
                    presentNutrients =
                        presentNutrients
                ),

            salt =
                doubleOrZero(
                    sourceKey =
                        "saltPer100g",
                    runtimeKey =
                        NutritionFacts.SALT,
                    presentNutrients =
                        presentNutrients
                ),

            presentNutrients =
                presentNutrients
        )
    }

    private fun Map<*, *>.doubleOrZero(
        sourceKey: String,
        runtimeKey: String,
        presentNutrients:
        MutableSet<String>
    ): Double {

        val value =
            (this[sourceKey] as? Number)
                ?.toDouble()
                ?.takeIf(
                    Double::isFinite
                )
                ?: return 0.0

        presentNutrients +=
            runtimeKey

        return value
    }
}