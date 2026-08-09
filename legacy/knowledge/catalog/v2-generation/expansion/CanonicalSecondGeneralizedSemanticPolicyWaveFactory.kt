package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType

class CanonicalSecondGeneralizedSemanticPolicyWaveFactory {

    fun createBatches():
            List<CanonicalCuratedSemanticPolicyBatch> =
        listOf(
            bakeryPhysicalFormBatch(),
            bakeryProcessingMethodBatch(),
            readyMealsFoodTypeBatch()
        )
            .sortedBy {
                it.sourceImplementationBatchKey
            }

    private fun bakeryPhysicalFormBatch():
            CanonicalCuratedSemanticPolicyBatch {
        /*
         * Die global beobachteten Werte
         *
         * diced, flakes, granules, grated
         *
         * beschreiben keine fachlich eigenständigen kanonischen Varianten
         * der hier aufgeführten Backwarenfamilien.
         *
         * Beispiele:
         * - gewürfeltes Brot wäre eher Croutons,
         * - geriebene Backware eher Paniermehl,
         * - Flocken und Granulate gehören in andere Produktfamilien.
         *
         * Die Achse ist deshalb für diese Familien nicht anwendbar.
         */
        val familyKeys =
            listOf(
                "baguette",
                "bread",
                "bread-rolls",
                "cakes",
                "crispbread",
                "flatbread",
                "pastries",
                "pies-and-tarts",
                "sweet-rolls",
                "toast-bread"
            )

        return batch(
            sourceImplementationBatchKey =
                "semantic-policy-batch-bakery-physical-form-002",

            category =
                "bakery",

            axis =
                CanonicalProductFamilyVariantAxis
                    .PHYSICAL_FORM,

            policies =
                familyKeys.map { familyKey ->
                    notApplicable(
                        familyKey =
                            familyKey,

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .PHYSICAL_FORM,

                        rationale =
                            "The globally projected PHYSICAL_FORM values " +
                                    "diced, flakes, granules and grated do not " +
                                    "represent canonical variants of bakery " +
                                    "family '$familyKey'. Such forms belong to " +
                                    "separate derived product families."
                    )
                }
        )
    }

    private fun bakeryProcessingMethodBatch():
            CanonicalCuratedSemanticPolicyBatch =
        batch(
            sourceImplementationBatchKey =
                "semantic-policy-batch-bakery-processing-method-008",

            category =
                "bakery",

            axis =
                CanonicalProductFamilyVariantAxis
                    .PROCESSING_METHOD,

            policies =
                listOf(
                    curated(
                        familyKey =
                            "baguette",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .PROCESSING_METHOD,

                        allowedValues =
                            listOf(
                                "baked",
                                "fermented"
                            ),

                        rationale =
                            "Baguette is canonically baked and may be " +
                                    "distinguished by fermented dough production."
                    ),

                    curated(
                        familyKey =
                            "bread",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .PROCESSING_METHOD,

                        allowedValues =
                            listOf(
                                "baked",
                                "fermented"
                            ),

                        rationale =
                            "Bread is canonically baked and may be " +
                                    "distinguished by fermented dough production."
                    ),

                    curated(
                        familyKey =
                            "bread-rolls",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .PROCESSING_METHOD,

                        allowedValues =
                            listOf(
                                "baked",
                                "fermented"
                            ),

                        rationale =
                            "Bread rolls are canonically baked and may use " +
                                    "fermented dough."
                    ),

                    curated(
                        familyKey =
                            "cakes",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .PROCESSING_METHOD,

                        allowedValues =
                            listOf(
                                "baked"
                            ),

                        rationale =
                            "The canonical processing method represented for " +
                                    "cakes in the current vocabulary is baked."
                    ),

                    curated(
                        familyKey =
                            "crispbread",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .PROCESSING_METHOD,

                        allowedValues =
                            listOf(
                                "baked"
                            ),

                        rationale =
                            "Crispbread is canonically represented as baked."
                    ),

                    curated(
                        familyKey =
                            "flatbread",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .PROCESSING_METHOD,

                        allowedValues =
                            listOf(
                                "baked"
                            ),

                        rationale =
                            "Flatbread is canonically represented as baked. " +
                                    "Boiled and deep-fried forms belong to other " +
                                    "product families."
                    ),

                    curated(
                        familyKey =
                            "pastries",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .PROCESSING_METHOD,

                        allowedValues =
                            listOf(
                                "baked",
                                "deep-fried"
                            ),

                        rationale =
                            "Canonical pastry products may be baked or " +
                                    "deep-fried."
                    ),

                    curated(
                        familyKey =
                            "pies-and-tarts",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .PROCESSING_METHOD,

                        allowedValues =
                            listOf(
                                "baked"
                            ),

                        rationale =
                            "Pies and tarts are canonically represented as " +
                                    "baked products."
                    ),

                    curated(
                        familyKey =
                            "sweet-rolls",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .PROCESSING_METHOD,

                        allowedValues =
                            listOf(
                                "baked",
                                "fermented"
                            ),

                        rationale =
                            "Sweet rolls are baked and may use fermented " +
                                    "yeast dough."
                    ),

                    curated(
                        familyKey =
                            "toast-bread",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .PROCESSING_METHOD,

                        allowedValues =
                            listOf(
                                "baked",
                                "fermented"
                            ),

                        rationale =
                            "Toast bread is baked and may use fermented dough."
                    )
                )
        )

    private fun readyMealsFoodTypeBatch():
            CanonicalCuratedSemanticPolicyBatch {
        /*
         * FOOD_TYPE enthält derzeit:
         *
         * coated-food
         * concentrate
         * cultured-food
         *
         * Keine dieser Eigenschaften ist eine allgemein gültige,
         * kanonische Unterteilung der betroffenen Fertiggerichtfamilien.
         *
         * Ein einzelnes Produkt kann beispielsweise paniert sein, doch
         * daraus folgt keine kanonische FOOD_TYPE-Variante der gesamten
         * Familie "meat-meals" oder "fish-meals".
         */
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
                "semantic-policy-batch-ready-meals-food-type-009",

            category =
                "ready-meals",

            axis =
                CanonicalProductFamilyVariantAxis
                    .FOOD_TYPE,

            policies =
                familyKeys.map { familyKey ->
                    notApplicable(
                        familyKey =
                            familyKey,

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,

                        rationale =
                            "The current FOOD_TYPE values coated-food, " +
                                    "concentrate and cultured-food do not provide " +
                                    "a canonical family-wide distinction for " +
                                    "ready-meal family '$familyKey'."
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
            "ShopMe generalized semantic policy curation wave 2"
    }
}