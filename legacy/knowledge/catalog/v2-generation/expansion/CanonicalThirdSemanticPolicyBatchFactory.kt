package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType

class CanonicalThirdSemanticPolicyBatchFactory {

    fun createEntries():
            List<CanonicalFamilyAxisSemanticPolicyEntry> =
        listOf(
            curated(
                familyKey = "pome-fruit",
                allowedValues = listOf(
                    "apple",
                    "pear"
                ),
                rationale =
                    "Canonical pome fruit species represented in the " +
                            "current plant-species vocabulary."
            ),

            curated(
                familyKey = "stone-fruit",
                allowedValues = listOf(
                    "apricot",
                    "cherry",
                    "peach",
                    "plum"
                ),
                rationale =
                    "Canonical stone fruit species with an edible fleshy " +
                            "fruit surrounding a hardened stone."
            ),

            curated(
                familyKey = "tropical-fruit",
                allowedValues = listOf(
                    "banana",
                    "mango",
                    "papaya",
                    "pineapple"
                ),
                rationale =
                    "Canonical tropical fruit species represented in the " +
                            "current market-oriented catalog vocabulary."
            ),

            curated(
                familyKey = "citrus-fruit",
                allowedValues = listOf(
                    "grapefruit",
                    "lemon",
                    "lime",
                    "orange"
                ),
                rationale =
                    "Canonical citrus fruit species."
            ),

            curated(
                familyKey = "dried-fruit",
                allowedValues = listOf(
                    "apple",
                    "apricot",
                    "banana",
                    "cherry",
                    "currant",
                    "grape",
                    "mango",
                    "papaya",
                    "peach",
                    "pear",
                    "pineapple",
                    "plum"
                ),
                rationale =
                    "Canonical fruit species commonly represented as dried " +
                            "single-fruit products."
            ),

            curated(
                familyKey = "prepared-fruit",
                allowedValues = listOf(
                    "apple",
                    "apricot",
                    "banana",
                    "blackberry",
                    "blueberry",
                    "cherry",
                    "currant",
                    "grape",
                    "grapefruit",
                    "lemon",
                    "lime",
                    "mango",
                    "orange",
                    "papaya",
                    "peach",
                    "pear",
                    "pineapple",
                    "plum",
                    "raspberry",
                    "strawberry",
                    "watermelon"
                ),
                rationale =
                    "Canonical fruit species that may occur as prepared, " +
                            "cut, cooked or otherwise processed single-fruit foods."
            ),

            curated(
                familyKey = "melons",
                allowedValues = listOf(
                    "watermelon"
                ),
                rationale =
                    "Canonical melon species currently represented in the " +
                            "global plant-species vocabulary."
            ),

            curated(
                familyKey = "grapes",
                allowedValues = listOf(
                    "grape"
                ),
                rationale =
                    "The grapes family is canonically identified by the " +
                            "grape plant-species value."
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

    private companion object {
        const val BATCH_SOURCE =
            "ShopMe canonical semantic policy batch 3: " +
                    "fruit × PLANT_SPECIES"
    }
}