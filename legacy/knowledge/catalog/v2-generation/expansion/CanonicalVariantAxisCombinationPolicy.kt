package de.shopme.testing.system.tools.knowledge.catalog.expansion.combination

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis

object CanonicalVariantAxisCombinationPolicy {

    private val ANCHOR_AXES =
        listOf(
            CanonicalProductFamilyVariantAxis.FOOD_TYPE,
            CanonicalProductFamilyVariantAxis.PRIMARY_INGREDIENT,
            CanonicalProductFamilyVariantAxis.PROTEIN_SOURCE,
            CanonicalProductFamilyVariantAxis.GRAIN_TYPE,
            CanonicalProductFamilyVariantAxis.ANIMAL_SPECIES,
            CanonicalProductFamilyVariantAxis.PLANT_SPECIES,
            CanonicalProductFamilyVariantAxis.RECIPE_TYPE
        )

    private val QUALIFIER_AXES =
        listOf(
            CanonicalProductFamilyVariantAxis.PROCESSING_METHOD,
            CanonicalProductFamilyVariantAxis.PRESERVATION_METHOD,
            CanonicalProductFamilyVariantAxis.PREPARATION_STATE,
            CanonicalProductFamilyVariantAxis.PHYSICAL_FORM,
            CanonicalProductFamilyVariantAxis.CUT_FORM,
            CanonicalProductFamilyVariantAxis.FAT_LEVEL,
            CanonicalProductFamilyVariantAxis.FLAVOR_PROFILE,
            CanonicalProductFamilyVariantAxis.SWEETENING_TYPE,
            CanonicalProductFamilyVariantAxis.RIPENING_OR_AGING,
            CanonicalProductFamilyVariantAxis.DIETARY_FORM,
            CanonicalProductFamilyVariantAxis.ALLERGEN_RELEVANT_VARIANT,
            CanonicalProductFamilyVariantAxis
                .NUTRITIONALLY_RELEVANT_VARIANT,
            CanonicalProductFamilyVariantAxis.SECONDARY_INGREDIENT
        )

    private val FORBIDDEN_PAIRS =
        setOf(
            pair(
                CanonicalProductFamilyVariantAxis.ANIMAL_SPECIES,
                CanonicalProductFamilyVariantAxis.PLANT_SPECIES
            ),

            pair(
                CanonicalProductFamilyVariantAxis.FAT_LEVEL,
                CanonicalProductFamilyVariantAxis.CUT_FORM
            ),

            pair(
                CanonicalProductFamilyVariantAxis.RIPENING_OR_AGING,
                CanonicalProductFamilyVariantAxis.CUT_FORM
            ),

            pair(
                CanonicalProductFamilyVariantAxis.SWEETENING_TYPE,
                CanonicalProductFamilyVariantAxis.CUT_FORM
            ),

            pair(
                CanonicalProductFamilyVariantAxis.FLAVOR_PROFILE,
                CanonicalProductFamilyVariantAxis.ANIMAL_SPECIES
            )
        )

    fun anchorAxesFrom(
        axes: Collection<CanonicalProductFamilyVariantAxis>
    ): List<CanonicalProductFamilyVariantAxis> =
        ANCHOR_AXES
            .filter { it in axes }

    fun qualifierAxesFrom(
        axes: Collection<CanonicalProductFamilyVariantAxis>
    ): List<CanonicalProductFamilyVariantAxis> =
        QUALIFIER_AXES
            .filter { it in axes }

    fun canCombine(
        first: CanonicalProductFamilyVariantAxis,
        second: CanonicalProductFamilyVariantAxis
    ): Boolean {
        if (first == second) {
            return false
        }

        return pair(first, second) !in FORBIDDEN_PAIRS
    }

    fun canCombine(
        axes: Collection<CanonicalProductFamilyVariantAxis>
    ): Boolean {
        val distinctAxes =
            axes.distinct()

        if (distinctAxes.size !in 2..3) {
            return false
        }

        return distinctAxes
            .flatMapIndexed { index, first ->
                distinctAxes
                    .drop(index + 1)
                    .map { second ->
                        first to second
                    }
            }
            .all { (first, second) ->
                canCombine(first, second)
            }
    }

    private fun pair(
        first: CanonicalProductFamilyVariantAxis,
        second: CanonicalProductFamilyVariantAxis
    ): Set<CanonicalProductFamilyVariantAxis> =
        setOf(first, second)
}