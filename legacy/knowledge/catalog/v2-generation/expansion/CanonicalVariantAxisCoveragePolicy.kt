package de.shopme.testing.system.tools.knowledge.catalog.expansion.variant

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis

object CanonicalVariantAxisCoveragePolicy {

    fun requirementFor(
        axis: CanonicalProductFamilyVariantAxis
    ): CanonicalVariantAxisCoverageRequirement =
        when (axis) {
            CanonicalProductFamilyVariantAxis.FOOD_TYPE ->
                required(
                    axis = axis,
                    minimum = 1,
                    recommended = 3,
                    maximum = 12,
                    allowCrossAxisCombination = false,
                    rationale =
                        "Food type defines the primary canonical identity. " +
                                "Only materially distinct food types are retained."
                )

            CanonicalProductFamilyVariantAxis.PRIMARY_INGREDIENT ->
                required(
                    axis = axis,
                    minimum = 1,
                    recommended = 6,
                    maximum = 30,
                    allowCrossAxisCombination = true,
                    rationale =
                        "Different primary ingredients can represent " +
                                "materially different canonical foods."
                )

            CanonicalProductFamilyVariantAxis.SECONDARY_INGREDIENT ->
                recommended(
                    axis = axis,
                    minimum = 1,
                    recommended = 4,
                    maximum = 20,
                    allowCrossAxisCombination = true,
                    rationale =
                        "Secondary ingredients are retained only when they " +
                                "materially affect recipe, nutrition or allergens."
                )

            CanonicalProductFamilyVariantAxis.RECIPE_TYPE ->
                required(
                    axis = axis,
                    minimum = 1,
                    recommended = 5,
                    maximum = 20,
                    allowCrossAxisCombination = true,
                    rationale =
                        "Distinct recipes can justify separate canonical " +
                                "foods when their food knowledge differs."
                )

            CanonicalProductFamilyVariantAxis.FLAVOR_PROFILE ->
                optional(
                    axis = axis,
                    minimum = 1,
                    recommended = 4,
                    maximum = 16,
                    allowCrossAxisCombination = false,
                    rationale =
                        "Flavor variants are curated and never expanded into " +
                                "brand- or edition-specific retail variants."
                )

            CanonicalProductFamilyVariantAxis.SWEETENING_TYPE ->
                recommended(
                    axis = axis,
                    minimum = 1,
                    recommended = 3,
                    maximum = 6,
                    allowCrossAxisCombination = true,
                    rationale =
                        "Sugar, no-added-sugar and alternative sweetening may " +
                                "materially affect nutrition."
                )

            CanonicalProductFamilyVariantAxis.FAT_LEVEL ->
                recommended(
                    axis = axis,
                    minimum = 1,
                    recommended = 4,
                    maximum = 8,
                    allowCrossAxisCombination = true,
                    rationale =
                        "Fat levels are retained where they materially change " +
                                "nutrition and common food identity."
                )

            CanonicalProductFamilyVariantAxis.PROTEIN_SOURCE ->
                required(
                    axis = axis,
                    minimum = 1,
                    recommended = 5,
                    maximum = 16,
                    allowCrossAxisCombination = true,
                    rationale =
                        "Protein source distinguishes materially different " +
                                "animal- and plant-based foods."
                )

            CanonicalProductFamilyVariantAxis.GRAIN_TYPE ->
                required(
                    axis = axis,
                    minimum = 1,
                    recommended = 6,
                    maximum = 20,
                    allowCrossAxisCombination = true,
                    rationale =
                        "Grain species and grain composition are canonical " +
                                "identity dimensions."
                )

            CanonicalProductFamilyVariantAxis.ANIMAL_SPECIES ->
                required(
                    axis = axis,
                    minimum = 1,
                    recommended = 4,
                    maximum = 16,
                    allowCrossAxisCombination = true,
                    rationale =
                        "Animal species changes food identity, nutrition and " +
                                "environmental knowledge."
                )

            CanonicalProductFamilyVariantAxis.PLANT_SPECIES ->
                required(
                    axis = axis,
                    minimum = 1,
                    recommended = 8,
                    maximum = 40,
                    allowCrossAxisCombination = true,
                    rationale =
                        "Relevant plant species define distinct canonical " +
                                "fruit, vegetable and ingredient foods."
                )

            CanonicalProductFamilyVariantAxis.PROCESSING_METHOD ->
                required(
                    axis = axis,
                    minimum = 1,
                    recommended = 5,
                    maximum = 14,
                    allowCrossAxisCombination = true,
                    rationale =
                        "Processing is retained when it changes food identity, " +
                                "nutrition or processing classification."
                )

            CanonicalProductFamilyVariantAxis.PRESERVATION_METHOD ->
                recommended(
                    axis = axis,
                    minimum = 1,
                    recommended = 4,
                    maximum = 10,
                    allowCrossAxisCombination = true,
                    rationale =
                        "Fresh, frozen, dried, canned and pickled forms may " +
                                "represent materially distinct foods."
                )

            CanonicalProductFamilyVariantAxis.PREPARATION_STATE ->
                recommended(
                    axis = axis,
                    minimum = 1,
                    recommended = 4,
                    maximum = 10,
                    allowCrossAxisCombination = true,
                    rationale =
                        "Raw, cooked and ready-to-eat states are retained " +
                                "where food knowledge changes materially."
                )

            CanonicalProductFamilyVariantAxis.PHYSICAL_FORM ->
                recommended(
                    axis = axis,
                    minimum = 1,
                    recommended = 4,
                    maximum = 12,
                    allowCrossAxisCombination = false,
                    rationale =
                        "Physical form is curated only where whole, ground, " +
                                "powdered, liquid or sliced forms are meaningful."
                )

            CanonicalProductFamilyVariantAxis.CUT_FORM ->
                optional(
                    axis = axis,
                    minimum = 1,
                    recommended = 4,
                    maximum = 12,
                    allowCrossAxisCombination = false,
                    rationale =
                        "Cut form is retained only when it changes culinary " +
                                "identity or preparation behavior."
                )

            CanonicalProductFamilyVariantAxis.RIPENING_OR_AGING ->
                recommended(
                    axis = axis,
                    minimum = 1,
                    recommended = 4,
                    maximum = 10,
                    allowCrossAxisCombination = true,
                    rationale =
                        "Ripening and aging stages are retained where they " +
                                "define materially different foods."
                )

            CanonicalProductFamilyVariantAxis.DIETARY_FORM ->
                recommended(
                    axis = axis,
                    minimum = 1,
                    recommended = 4,
                    maximum = 10,
                    allowCrossAxisCombination = false,
                    rationale =
                        "Vegan, vegetarian, gluten-free or lactose-free forms " +
                                "are retained only when recipe identity changes."
                )

            CanonicalProductFamilyVariantAxis.ALLERGEN_RELEVANT_VARIANT ->
                required(
                    axis = axis,
                    minimum = 1,
                    recommended = 3,
                    maximum = 10,
                    allowCrossAxisCombination = true,
                    rationale =
                        "Allergen-relevant recipe changes can justify distinct " +
                                "canonical foods."
                )

            CanonicalProductFamilyVariantAxis
                .NUTRITIONALLY_RELEVANT_VARIANT ->
                required(
                    axis = axis,
                    minimum = 1,
                    recommended = 4,
                    maximum = 12,
                    allowCrossAxisCombination = true,
                    rationale =
                        "Nutrition variants are retained only when they " +
                                "materially affect canonical knowledge."
                )
        }

    private fun required(
        axis: CanonicalProductFamilyVariantAxis,
        minimum: Int,
        recommended: Int,
        maximum: Int,
        allowCrossAxisCombination: Boolean,
        rationale: String
    ): CanonicalVariantAxisCoverageRequirement =
        requirement(
            axis = axis,
            criticality =
                CanonicalVariantAxisCriticality.REQUIRED,
            selectionMode =
                CanonicalVariantAxisSelectionMode
                    .EXHAUSTIVE_RELEVANT_VALUES,
            minimum = minimum,
            recommended = recommended,
            maximum = maximum,
            allowCrossAxisCombination =
                allowCrossAxisCombination,
            rationale = rationale
        )

    private fun recommended(
        axis: CanonicalProductFamilyVariantAxis,
        minimum: Int,
        recommended: Int,
        maximum: Int,
        allowCrossAxisCombination: Boolean,
        rationale: String
    ): CanonicalVariantAxisCoverageRequirement =
        requirement(
            axis = axis,
            criticality =
                CanonicalVariantAxisCriticality.RECOMMENDED,
            selectionMode =
                CanonicalVariantAxisSelectionMode
                    .CURATED_RELEVANT_VALUES,
            minimum = minimum,
            recommended = recommended,
            maximum = maximum,
            allowCrossAxisCombination =
                allowCrossAxisCombination,
            rationale = rationale
        )

    private fun optional(
        axis: CanonicalProductFamilyVariantAxis,
        minimum: Int,
        recommended: Int,
        maximum: Int,
        allowCrossAxisCombination: Boolean,
        rationale: String
    ): CanonicalVariantAxisCoverageRequirement =
        requirement(
            axis = axis,
            criticality =
                CanonicalVariantAxisCriticality.OPTIONAL,
            selectionMode =
                CanonicalVariantAxisSelectionMode
                    .CURATED_RELEVANT_VALUES,
            minimum = minimum,
            recommended = recommended,
            maximum = maximum,
            allowCrossAxisCombination =
                allowCrossAxisCombination,
            rationale = rationale
        )

    private fun requirement(
        axis: CanonicalProductFamilyVariantAxis,
        criticality: CanonicalVariantAxisCriticality,
        selectionMode: CanonicalVariantAxisSelectionMode,
        minimum: Int,
        recommended: Int,
        maximum: Int,
        allowCrossAxisCombination: Boolean,
        rationale: String
    ): CanonicalVariantAxisCoverageRequirement =
        CanonicalVariantAxisCoverageRequirement(
            axis = axis,
            criticality = criticality,
            selectionMode = selectionMode,
            minimumRelevantValueCount = minimum,
            recommendedRelevantValueCount = recommended,
            maximumRelevantValueCount = maximum,
            allowCrossAxisCombination =
                allowCrossAxisCombination,
            rationale = rationale
        )
}