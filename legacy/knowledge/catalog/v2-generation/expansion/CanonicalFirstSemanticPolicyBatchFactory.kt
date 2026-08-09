package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType

class CanonicalFirstSemanticPolicyBatchFactory {

    fun createEntries():
            List<CanonicalFamilyAxisSemanticPolicyEntry> =
        listOf(
            curated(
                familyKey = "fruit-vegetables",
                allowedValues =
                    listOf(
                        "cucumber",
                        "eggplant",
                        "pepper",
                        "pumpkin",
                        "tomato",
                        "zucchini"
                    ),
                rationale =
                    "Canonical culinary fruit vegetables represented as " +
                            "distinct vegetable foods."
            ),

            curated(
                familyKey = "leafy-vegetables",
                allowedValues =
                    listOf(
                        "chard",
                        "kale",
                        "lettuce",
                        "rocket",
                        "spinach"
                    ),
                rationale =
                    "Canonical leafy vegetable species."
            ),

            curated(
                familyKey = "root-vegetables",
                allowedValues =
                    listOf(
                        "beetroot",
                        "carrot",
                        "celeriac",
                        "parsnip",
                        "radish",
                        "turnip"
                    ),
                rationale =
                    "Canonical root vegetable species."
            ),

            curated(
                familyKey = "cabbage",
                allowedValues =
                    listOf(
                        "broccoli",
                        "brussels-sprouts",
                        "cauliflower",
                        "kale",
                        "kohlrabi",
                        "red-cabbage",
                        "savoy-cabbage",
                        "white-cabbage"
                    ),
                rationale =
                    "Canonical cabbage and brassica vegetable forms."
            ),

            curated(
                familyKey = "tuber-vegetables",
                allowedValues =
                    listOf(
                        "cassava",
                        "jerusalem-artichoke",
                        "potato",
                        "sweet-potato",
                        "yam"
                    ),
                rationale =
                    "Canonical edible tuber species."
            ),

            curated(
                familyKey = "onion-vegetables",
                allowedValues =
                    listOf(
                        "garlic",
                        "leek",
                        "onion",
                        "shallot",
                        "spring-onion"
                    ),
                rationale =
                    "Canonical allium vegetable species."
            ),

            curated(
                familyKey = "flower-vegetables",
                allowedValues =
                    listOf(
                        "artichoke",
                        "broccoli",
                        "cauliflower"
                    ),
                rationale =
                    "Canonical vegetables consumed primarily as flower " +
                            "structures."
            ),

            notApplicable(
                familyKey = "mushrooms",
                rationale =
                    "Mushrooms are fungi, not plant species. The " +
                            "PLANT_SPECIES axis is not applicable to this family."
            ),

            curated(
                familyKey = "stem-vegetables",
                allowedValues =
                    listOf(
                        "asparagus",
                        "bamboo-shoot",
                        "celery",
                        "rhubarb"
                    ),
                rationale =
                    "Canonical vegetables consumed primarily as stems or " +
                            "shoots."
            ),

            curated(
                familyKey = "sprouts",
                allowedValues =
                    listOf(
                        "alfalfa",
                        "bean-sprouts",
                        "broccoli-sprouts",
                        "lentil-sprouts",
                        "mung-bean-sprouts"
                    ),
                rationale =
                    "Canonical edible sprout identities."
            ),

            notApplicable(
                familyKey = "vegetable-mixtures",
                rationale =
                    "A vegetable mixture is not identified by one plant " +
                            "species. Its canonical differentiation requires a " +
                            "composition-oriented axis."
            ),

            curated(
                familyKey = "sea-vegetables",
                allowedValues =
                    listOf(
                        "dulse",
                        "kombu",
                        "nori",
                        "sea-lettuce",
                        "wakame"
                    ),
                rationale =
                    "Canonical edible seaweed identities represented through " +
                            "the current species-oriented axis."
            )
        )
            .sortedWith(
                compareBy<
                        CanonicalFamilyAxisSemanticPolicyEntry
                        > {
                    it.familyKey
                }.thenBy {
                    it.axis.name
                }
            )

    private fun curated(
        familyKey: String,
        allowedValues: List<String>,
        rationale: String
    ): CanonicalFamilyAxisSemanticPolicyEntry =
        CanonicalFamilyAxisSemanticPolicyEntry(
            familyKey = familyKey,
            axis =
                CanonicalProductFamilyVariantAxis
                    .PLANT_SPECIES,
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
                BATCH_SOURCE,
            active =
                true
        )

    private fun notApplicable(
        familyKey: String,
        rationale: String
    ): CanonicalFamilyAxisSemanticPolicyEntry =
        CanonicalFamilyAxisSemanticPolicyEntry(
            familyKey = familyKey,
            axis =
                CanonicalProductFamilyVariantAxis
                    .PLANT_SPECIES,
            policyType =
                CanonicalFamilyAxisSemanticPolicyType
                    .NOT_APPLICABLE,
            allowedValues =
                emptyList(),
            rationale =
                rationale.trim(),
            source =
                BATCH_SOURCE,
            active =
                true
        )

    private companion object {
        const val BATCH_SOURCE =
            "ShopMe canonical semantic policy batch 1: " +
                    "vegetables × PLANT_SPECIES"
    }
}