package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType

class CanonicalFirstGeneralizedSemanticPolicyWaveFactory {

    fun createBatches():
            List<CanonicalCuratedSemanticPolicyBatch> =
        listOf(
            dairyFoodTypeBatch(),
            bakeryGrainTypeBatch(),
            readyMealsAllergenVariantBatch()
        )
            .sortedBy {
                it.sourceImplementationBatchKey
            }

    private fun dairyFoodTypeBatch():
            CanonicalCuratedSemanticPolicyBatch =
        batch(
            sourceImplementationBatchKey =
                "semantic-policy-batch-dairy-food-type-001",

            category =
                "dairy",

            axis =
                CanonicalProductFamilyVariantAxis
                    .FOOD_TYPE,

            policies =
                listOf(
                    curated(
                        familyKey = "blue-cheese",
                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,
                        allowedValues =
                            listOf("cultured-food"),
                        rationale =
                            "Blue cheese is a cultured dairy food."
                    ),

                    notApplicable(
                        familyKey = "butter",
                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,
                        rationale =
                            "The current FOOD_TYPE vocabulary does not " +
                                    "provide a canonical butter-specific value."
                    ),

                    curated(
                        familyKey = "cultured-dairy",
                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,
                        allowedValues =
                            listOf("cultured-food"),
                        rationale =
                            "Cultured dairy is identified by the " +
                                    "cultured-food value."
                    ),

                    notApplicable(
                        familyKey = "dairy-desserts",
                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,
                        rationale =
                            "The current FOOD_TYPE vocabulary does not " +
                                    "distinguish dairy desserts canonically."
                    ),

                    curated(
                        familyKey = "fresh-cheese",
                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,
                        allowedValues =
                            listOf("cultured-food"),
                        rationale =
                            "Fresh cheese is a cultured dairy food."
                    ),

                    curated(
                        familyKey = "hard-cheese",
                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,
                        allowedValues =
                            listOf("cultured-food"),
                        rationale =
                            "Hard cheese is a cultured dairy food."
                    ),

                    notApplicable(
                        familyKey = "cream",
                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,
                        rationale =
                            "Cream is not inherently a concentrate or " +
                                    "cultured food."
                    ),

                    notApplicable(
                        familyKey = "milk",
                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,
                        rationale =
                            "Milk is not inherently coated, concentrated " +
                                    "or cultured."
                    ),

                    curated(
                        familyKey = "quark",
                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,
                        allowedValues =
                            listOf("cultured-food"),
                        rationale =
                            "Quark is a cultured dairy food."
                    ),

                    curated(
                        familyKey = "semi-hard-cheese",
                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,
                        allowedValues =
                            listOf("cultured-food"),
                        rationale =
                            "Semi-hard cheese is a cultured dairy food."
                    ),

                    curated(
                        familyKey = "soft-cheese",
                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,
                        allowedValues =
                            listOf("cultured-food"),
                        rationale =
                            "Soft cheese is a cultured dairy food."
                    ),

                    curated(
                        familyKey = "yogurt",
                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,
                        allowedValues =
                            listOf("cultured-food"),
                        rationale =
                            "Yogurt is a cultured dairy food."
                    )
                )
        )

    private fun bakeryGrainTypeBatch():
            CanonicalCuratedSemanticPolicyBatch {
        val allBakeryGrains =
            listOf(
                "amaranth",
                "barley",
                "buckwheat",
                "corn",
                "durum-wheat",
                "einkorn"
            )

        return batch(
            sourceImplementationBatchKey =
                "semantic-policy-batch-bakery-grain-type-003",

            category =
                "bakery",

            axis =
                CanonicalProductFamilyVariantAxis
                    .GRAIN_TYPE,

            policies =
                listOf(
                    curated(
                        "baguette",
                        CanonicalProductFamilyVariantAxis.GRAIN_TYPE,
                        allBakeryGrains,
                        "Canonical grains suitable for baguette variants."
                    ),

                    curated(
                        "bread-rolls",
                        CanonicalProductFamilyVariantAxis.GRAIN_TYPE,
                        allBakeryGrains,
                        "Canonical grains suitable for bread-roll variants."
                    ),

                    curated(
                        "cakes",
                        CanonicalProductFamilyVariantAxis.GRAIN_TYPE,
                        allBakeryGrains,
                        "Canonical grains and pseudocereals suitable for cakes."
                    ),

                    curated(
                        "crispbread",
                        CanonicalProductFamilyVariantAxis.GRAIN_TYPE,
                        listOf(
                            "amaranth",
                            "barley",
                            "buckwheat",
                            "corn",
                            "einkorn"
                        ),
                        "Canonical grains suitable for crispbread."
                    ),

                    curated(
                        "flatbread",
                        CanonicalProductFamilyVariantAxis.GRAIN_TYPE,
                        allBakeryGrains,
                        "Canonical grains suitable for flatbread."
                    ),

                    curated(
                        "pastries",
                        CanonicalProductFamilyVariantAxis.GRAIN_TYPE,
                        allBakeryGrains,
                        "Canonical grains suitable for pastries."
                    ),

                    curated(
                        "pies-and-tarts",
                        CanonicalProductFamilyVariantAxis.GRAIN_TYPE,
                        allBakeryGrains,
                        "Canonical grains suitable for pies and tarts."
                    ),

                    curated(
                        "sweet-rolls",
                        CanonicalProductFamilyVariantAxis.GRAIN_TYPE,
                        allBakeryGrains,
                        "Canonical grains suitable for sweet rolls."
                    ),

                    curated(
                        "toast-bread",
                        CanonicalProductFamilyVariantAxis.GRAIN_TYPE,
                        allBakeryGrains,
                        "Canonical grains suitable for toast bread."
                    )
                )
        )
    }

    private fun readyMealsAllergenVariantBatch():
            CanonicalCuratedSemanticPolicyBatch {
        val allowedAllergenVariants =
            listOf(
                "celery-free",
                "egg-free",
                "gluten-free"
            )

        val familyKeys =
            listOf(
                "fish-meals",
                "international-meals",
                "meat-meals",
                "pasta-meals",
                "pizza",
                "potato-meals",
                "rice-meals",
                "soups",
                "stews",
                "vegan-meals",
                "vegetable-meals",
                "vegetarian-meals"
            )

        return batch(
            sourceImplementationBatchKey =
                "semantic-policy-batch-ready-meals-" +
                        "allergen-relevant-variant-007",

            category =
                "ready-meals",

            axis =
                CanonicalProductFamilyVariantAxis
                    .ALLERGEN_RELEVANT_VARIANT,

            policies =
                familyKeys.map { familyKey ->
                    curated(
                        familyKey =
                            familyKey,

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .ALLERGEN_RELEVANT_VARIANT,

                        allowedValues =
                            allowedAllergenVariants,

                        rationale =
                            "Canonical allergen-relevant absence variants " +
                                    "applicable to ready-meal family " +
                                    "'$familyKey'."
                    )
                }
        )
    }

    private fun batch(
        sourceImplementationBatchKey: String,
        category: String,
        axis: CanonicalProductFamilyVariantAxis,
        policies:
        List<CanonicalFamilyAxisSemanticPolicyEntry>
    ): CanonicalCuratedSemanticPolicyBatch {
        val sortedPolicies =
            policies.sortedWith(
                compareBy<
                        CanonicalFamilyAxisSemanticPolicyEntry
                        > {
                    it.familyKey
                }.thenBy {
                    it.axis.name
                }
            )

        return CanonicalCuratedSemanticPolicyBatch(
            version =
                CanonicalCuratedSemanticPolicyBatch
                    .CURRENT_VERSION,

            curationId =
                "curated-$sourceImplementationBatchKey",

            sourceImplementationBatchKey =
                sourceImplementationBatchKey,

            category =
                category,

            axis =
                axis.name,

            policyCount =
                sortedPolicies.size,

            policies =
                sortedPolicies,

            complete =
                sortedPolicies.all {
                    it.active &&
                            it.complete
                },

            valid =
                sortedPolicies.isNotEmpty() &&
                        sortedPolicies.all {
                            it.active &&
                                    it.complete
                        }
        )
    }

    private fun curated(
        familyKey: String,
        axis: CanonicalProductFamilyVariantAxis,
        allowedValues: List<String>,
        rationale: String
    ): CanonicalFamilyAxisSemanticPolicyEntry =
        CanonicalFamilyAxisSemanticPolicyEntry(
            familyKey =
                familyKey,

            axis =
                axis,

            policyType =
                CanonicalFamilyAxisSemanticPolicyType
                    .CURATED_ALLOWED_VALUES,

            allowedValues =
                allowedValues
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .distinct()
                    .sorted(),

            rationale =
                rationale.trim(),

            source =
                WAVE_SOURCE,

            active =
                true
        )

    private fun notApplicable(
        familyKey: String,
        axis: CanonicalProductFamilyVariantAxis,
        rationale: String
    ): CanonicalFamilyAxisSemanticPolicyEntry =
        CanonicalFamilyAxisSemanticPolicyEntry(
            familyKey =
                familyKey,

            axis =
                axis,

            policyType =
                CanonicalFamilyAxisSemanticPolicyType
                    .NOT_APPLICABLE,

            allowedValues =
                emptyList(),

            rationale =
                rationale.trim(),

            source =
                WAVE_SOURCE,

            active =
                true
        )

    private companion object {
        const val WAVE_SOURCE =
            "ShopMe generalized semantic policy curation wave 1"
    }
}