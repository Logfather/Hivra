package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.curation

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyImplementationBatch
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.CanonicalSemanticPolicyImplementationBatchResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType

class CanonicalThirdGeneralizedSemanticPolicyWaveFactory(
    private val batchResolver:
    CanonicalOpenSemanticPolicyBatchResolver =
        CanonicalOpenSemanticPolicyBatchResolver()
) {

    fun createBatches(
        openBatches:
        CanonicalSemanticPolicyImplementationBatchResult,

        curatedManifest:
        CanonicalCuratedSemanticPolicyBatchManifest
    ): List<CanonicalCuratedSemanticPolicyBatch> {
        require(openBatches.valid)
        require(curatedManifest.valid)

        return listOf(
            resolveOrCreateBatch(
                openBatches =
                    openBatches,

                curatedManifest =
                    curatedManifest,

                category =
                    "beverages",

                axis =
                    CanonicalProductFamilyVariantAxis
                        .FOOD_TYPE,

                create =
                    ::beveragesFoodTypeBatch
            ),

            resolveOrCreateBatch(
                openBatches =
                    openBatches,

                curatedManifest =
                    curatedManifest,

                category =
                    "baking-ingredients",

                axis =
                    CanonicalProductFamilyVariantAxis
                        .PHYSICAL_FORM,

                create =
                    ::bakingIngredientsPhysicalFormBatch
            ),

            resolveOrCreateBatch(
                openBatches =
                    openBatches,

                curatedManifest =
                    curatedManifest,

                category =
                    "meat",

                axis =
                    CanonicalProductFamilyVariantAxis
                        .CUT_FORM,

                create =
                    ::meatCutFormBatch
            )
        )
            .sortedBy {
                it.sourceImplementationBatchKey
            }
    }

    private fun resolveOrCreateBatch(
        openBatches:
        CanonicalSemanticPolicyImplementationBatchResult,

        curatedManifest:
        CanonicalCuratedSemanticPolicyBatchManifest,

        category: String,

        axis:
        CanonicalProductFamilyVariantAxis,

        create:
            (
            CanonicalSemanticPolicyImplementationBatch
        ) -> CanonicalCuratedSemanticPolicyBatch
    ): CanonicalCuratedSemanticPolicyBatch {
        val persistedMatches =
            curatedManifest.batches
                .filter { batch ->
                    batch.category ==
                            category &&
                            batch.axis ==
                            axis.name &&
                            batch.policies.all { policy ->
                                policy.source ==
                                        WAVE_SOURCE
                            }
                }

        require(
            persistedMatches.size <= 1
        ) {
            "Expected at most one persisted Wave-3 batch for " +
                    "category '$category' and axis '${axis.name}', " +
                    "but found ${persistedMatches.size}."
        }

        val persisted =
            persistedMatches.singleOrNull()

        if (persisted != null) {
            require(
                persisted.valid &&
                        persisted.complete
            ) {
                "Persisted Wave-3 batch " +
                        "'${persisted.sourceImplementationBatchKey}' " +
                        "is invalid or incomplete."
            }

            return persisted
        }

        val openBatch =
            batchResolver.resolve(
                openBatches =
                    openBatches,

                category =
                    category,

                axis =
                    axis
            )

        return create(openBatch)
    }

    private fun beveragesFoodTypeBatch(
        openBatch:
        CanonicalSemanticPolicyImplementationBatch
    ): CanonicalCuratedSemanticPolicyBatch =
        batch(
            openBatch =
                openBatch,

            policies =
                listOf(
                    curated(
                        familyKey =
                            "coffee",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,

                        allowedValues =
                            listOf(
                                "concentrate"
                            ),

                        rationale =
                            "Coffee may canonically occur as a beverage " +
                                    "concentrate. Coated-food and cultured-food " +
                                    "are not canonical coffee variants."
                    ),

                    notApplicable(
                        familyKey =
                            "cocoa-drinks",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,

                        rationale =
                            "The current FOOD_TYPE values do not provide a " +
                                    "canonical distinction for cocoa drinks."
                    ),

                    notApplicable(
                        familyKey =
                            "cola",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,

                        rationale =
                            "Cola is not canonically classified as coated, " +
                                    "concentrated or cultured food."
                    ),

                    notApplicable(
                        familyKey =
                            "energy-drinks",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,

                        rationale =
                            "The current FOOD_TYPE values do not provide a " +
                                    "canonical distinction for energy drinks."
                    ),

                    notApplicable(
                        familyKey =
                            "fruit-drinks",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,

                        rationale =
                            "Fruit drinks are not inherently concentrates, " +
                                    "coated foods or cultured foods."
                    ),

                    curated(
                        familyKey =
                            "fruit-juice",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,

                        allowedValues =
                            listOf(
                                "concentrate"
                            ),

                        rationale =
                            "Fruit juice concentrate is a canonical food " +
                                    "form represented by the current vocabulary."
                    ),

                    curated(
                        familyKey =
                            "fruit-nectar",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,

                        allowedValues =
                            listOf(
                                "concentrate"
                            ),

                        rationale =
                            "Fruit nectar may canonically occur as a " +
                                    "concentrated preparation."
                    ),

                    notApplicable(
                        familyKey =
                            "iced-tea",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,

                        rationale =
                            "Ready-to-drink iced tea is not canonically " +
                                    "distinguished by the current FOOD_TYPE values."
                    ),

                    curated(
                        familyKey =
                            "lemonade",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,

                        allowedValues =
                            listOf(
                                "concentrate"
                            ),

                        rationale =
                            "Lemonade concentrate is a canonical preparation " +
                                    "represented by the current vocabulary."
                    ),

                    notApplicable(
                        familyKey =
                            "malt-drinks",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,

                        rationale =
                            "Malt drinks are not canonically differentiated " +
                                    "by coated-food, concentrate or cultured-food."
                    ),

                    notApplicable(
                        familyKey =
                            "mineral-water",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,

                        rationale =
                            "Mineral water cannot canonically be coated, " +
                                    "concentrated or cultured."
                    ),

                    notApplicable(
                        familyKey =
                            "sports-drinks",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,

                        rationale =
                            "The current FOOD_TYPE values do not provide a " +
                                    "canonical sports-drink distinction."
                    ),

                    notApplicable(
                        familyKey =
                            "table-water",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,

                        rationale =
                            "Table water cannot canonically be coated, " +
                                    "concentrated or cultured."
                    ),

                    curated(
                        familyKey =
                            "tea",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,

                        allowedValues =
                            listOf(
                                "concentrate"
                            ),

                        rationale =
                            "Tea concentrate is a canonical preparation " +
                                    "represented by the current vocabulary."
                    ),

                    curated(
                        familyKey =
                            "vegetable-juice",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .FOOD_TYPE,

                        allowedValues =
                            listOf(
                                "concentrate"
                            ),

                        rationale =
                            "Vegetable-juice concentrate is a canonical food " +
                                    "form represented by the current vocabulary."
                    )
                )
        )

    private fun bakingIngredientsPhysicalFormBatch(
        openBatch:
        CanonicalSemanticPolicyImplementationBatch
    ): CanonicalCuratedSemanticPolicyBatch =
        batch(
            openBatch =
                openBatch,

            policies =
                listOf(
                    curated(
                        familyKey =
                            "baking-chocolate",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .PHYSICAL_FORM,

                        allowedValues =
                            listOf(
                                "diced",
                                "flakes",
                                "granules",
                                "grated"
                            ),

                        rationale =
                            "Baking chocolate may canonically occur as " +
                                    "pieces, flakes, granules or grated chocolate."
                    ),

                    notApplicable(
                        familyKey =
                            "baking-flavors",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .PHYSICAL_FORM,

                        rationale =
                            "The current PHYSICAL_FORM values do not " +
                                    "canonically describe baking flavors."
                    ),

                    curated(
                        familyKey =
                            "baking-fruit",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .PHYSICAL_FORM,

                        allowedValues =
                            listOf(
                                "diced",
                                "flakes"
                            ),

                        rationale =
                            "Baking fruit may canonically occur diced or as " +
                                    "fruit flakes."
                    ),

                    curated(
                        familyKey =
                            "baking-nuts",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .PHYSICAL_FORM,

                        allowedValues =
                            listOf(
                                "flakes",
                                "grated"
                            ),

                        rationale =
                            "Baking nuts may canonically occur as flakes or " +
                                    "finely grated nut material."
                    ),

                    curated(
                        familyKey =
                            "cake-decorations",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .PHYSICAL_FORM,

                        allowedValues =
                            listOf(
                                "flakes",
                                "granules"
                            ),

                        rationale =
                            "Cake decorations commonly occur as decorative " +
                                    "flakes or granules."
                    ),

                    notApplicable(
                        familyKey =
                            "cocoa-products",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .PHYSICAL_FORM,

                        rationale =
                            "The available values omit the canonical cocoa " +
                                    "forms powder, nibs and mass."
                    ),

                    notApplicable(
                        familyKey =
                            "dessert-mixes",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .PHYSICAL_FORM,

                        rationale =
                            "Dessert mixes require powder or liquid forms, " +
                                    "which are not represented by this value set."
                    ),

                    curated(
                        familyKey =
                            "gelling-agents",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .PHYSICAL_FORM,

                        allowedValues =
                            listOf(
                                "flakes",
                                "granules"
                            ),

                        rationale =
                            "Gelling agents may canonically occur as flakes " +
                                    "or granules."
                    ),

                    notApplicable(
                        familyKey =
                            "raising-agents",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .PHYSICAL_FORM,

                        rationale =
                            "Raising agents are canonically powders, liquids " +
                                    "or compressed preparations; those forms are " +
                                    "not represented by this value set."
                    )
                )
        )

    private fun meatCutFormBatch(
        openBatch:
        CanonicalSemanticPolicyImplementationBatch
    ): CanonicalCuratedSemanticPolicyBatch =
        batch(
            openBatch =
                openBatch,

            policies =
                listOf(
                    curated(
                        familyKey =
                            "beef",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .CUT_FORM,

                        allowedValues =
                            listOf(
                                "chop",
                                "cubes",
                                "cutlet",
                                "fillet"
                            ),

                        rationale =
                            "Beef may canonically occur as chops, cubes, " +
                                    "cutlets or fillets."
                    ),

                    curated(
                        familyKey =
                            "chicken",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .CUT_FORM,

                        allowedValues =
                            listOf(
                                "cubes",
                                "cutlet",
                                "fillet"
                            ),

                        rationale =
                            "Chicken may canonically occur as cubes, cutlets " +
                                    "or fillets; chop is not a standard chicken cut."
                    ),

                    curated(
                        familyKey =
                            "duck-and-goose",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .CUT_FORM,

                        allowedValues =
                            listOf(
                                "fillet"
                            ),

                        rationale =
                            "Duck and goose breast fillet is the applicable " +
                                    "canonical value from the current vocabulary."
                    ),

                    curated(
                        familyKey =
                            "game",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .CUT_FORM,

                        allowedValues =
                            listOf(
                                "chop",
                                "cubes",
                                "cutlet",
                                "fillet"
                            ),

                        rationale =
                            "Game meat may canonically occur as chops, cubes, " +
                                    "cutlets or fillets."
                    ),

                    curated(
                        familyKey =
                            "lamb",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .CUT_FORM,

                        allowedValues =
                            listOf(
                                "chop",
                                "cubes",
                                "cutlet",
                                "fillet"
                            ),

                        rationale =
                            "Lamb may canonically occur as chops, cubes, " +
                                    "cutlets or fillets."
                    ),

                    notApplicable(
                        familyKey =
                            "offal",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .CUT_FORM,

                        rationale =
                            "Chop, cubes, cutlet and fillet do not provide a " +
                                    "consistent canonical classification for offal."
                    ),

                    curated(
                        familyKey =
                            "pork",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .CUT_FORM,

                        allowedValues =
                            listOf(
                                "chop",
                                "cubes",
                                "cutlet",
                                "fillet"
                            ),

                        rationale =
                            "Pork may canonically occur as chops, cubes, " +
                                    "cutlets or fillets."
                    ),

                    curated(
                        familyKey =
                            "turkey",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .CUT_FORM,

                        allowedValues =
                            listOf(
                                "cubes",
                                "cutlet",
                                "fillet"
                            ),

                        rationale =
                            "Turkey may canonically occur as cubes, cutlets " +
                                    "or fillets."
                    ),

                    curated(
                        familyKey =
                            "veal",

                        axis =
                            CanonicalProductFamilyVariantAxis
                                .CUT_FORM,

                        allowedValues =
                            listOf(
                                "chop",
                                "cubes",
                                "cutlet",
                                "fillet"
                            ),

                        rationale =
                            "Veal may canonically occur as chops, cubes, " +
                                    "cutlets or fillets."
                    )
                )
        )

    private fun batch(
        openBatch:
        CanonicalSemanticPolicyImplementationBatch,

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

        val expectedGapKeys =
            openBatch.gaps
                .map {
                    it.gapKey
                }
                .sorted()

        val actualPolicyKeys =
            sortedPolicies
                .map {
                    it.identityKey
                }
                .sorted()

        require(
            actualPolicyKeys ==
                    expectedGapKeys
        ) {
            "Wave-3 policies do not exactly cover open batch " +
                    "'${openBatch.batchKey}'. " +
                    "expected=$expectedGapKeys, actual=$actualPolicyKeys"
        }

        return CanonicalCuratedSemanticPolicyBatch(
            version =
                CanonicalCuratedSemanticPolicyBatch
                    .CURRENT_VERSION,

            curationId =
                "curated-${openBatch.batchKey}",

            sourceImplementationBatchKey =
                openBatch.batchKey,

            category =
                openBatch.category,

            axis =
                openBatch.axis.name,

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
            "ShopMe generalized semantic policy curation wave 3"
    }
}