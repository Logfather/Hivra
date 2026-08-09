package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType

class CanonicalSecondSemanticPolicyBatchFactory {

    fun createEntries():
            List<CanonicalFamilyAxisSemanticPolicyEntry> =
        listOf(
            curated(
                familyKey = "coffee",
                allowedValues =
                    listOf(
                        "bitter",
                        "creamy",
                        "floral",
                        "fruity",
                        "mild",
                        "nutty",
                        "roasted",
                        "smoky"
                    ),
                rationale =
                    "Canonical sensory profiles of coffee beverages, " +
                            "including roast-derived, bitter, creamy and " +
                            "origin-dependent aromatic distinctions."
            ),

            curated(
                familyKey = "fruit-drinks",
                allowedValues =
                    listOf(
                        "citrusy",
                        "fruity",
                        "mild",
                        "sour",
                        "sweet"
                    ),
                rationale =
                    "Canonical fruit-drink flavor profiles derived from " +
                            "fruit character, sweetness and acidity."
            ),

            curated(
                familyKey = "fruit-juice",
                allowedValues =
                    listOf(
                        "bitter",
                        "citrusy",
                        "fruity",
                        "sour",
                        "sweet"
                    ),
                rationale =
                    "Canonical fruit-juice profiles covering fruit character, " +
                            "sweetness, acidity, citrus notes and natural bitterness."
            ),

            curated(
                familyKey = "fruit-nectar",
                allowedValues =
                    listOf(
                        "creamy",
                        "fruity",
                        "mild",
                        "sour",
                        "sweet"
                    ),
                rationale =
                    "Canonical fruit-nectar profiles including fruit character, " +
                            "sweetness, acidity and pulp-associated creaminess."
            ),

            curated(
                familyKey = "iced-tea",
                allowedValues =
                    listOf(
                        "bitter",
                        "citrusy",
                        "floral",
                        "fruity",
                        "herbal",
                        "sweet"
                    ),
                rationale =
                    "Canonical iced-tea profiles derived from tea, herbs, " +
                            "fruit, citrus character, sweetness and bitterness."
            ),

            curated(
                familyKey = "lemonade",
                allowedValues =
                    listOf(
                        "bitter",
                        "citrusy",
                        "fruity",
                        "herbal",
                        "sour",
                        "sweet"
                    ),
                rationale =
                    "Canonical lemonade profiles covering sweet, acidic, " +
                            "fruit, citrus, herbal and bitter distinctions."
            ),

            curated(
                familyKey = "mineral-water",
                allowedValues =
                    listOf(
                        "mild",
                        "mineral",
                        "neutral"
                    ),
                rationale =
                    "Canonical mineral-water profiles distinguish neutral, " +
                            "mild and mineral sensory character."
            ),

            curated(
                familyKey = "tea",
                allowedValues =
                    listOf(
                        "bitter",
                        "citrusy",
                        "earthy",
                        "floral",
                        "fruity",
                        "herbal",
                        "mild",
                        "smoky"
                    ),
                rationale =
                    "Canonical tea profiles including herbal, floral, earthy, " +
                            "smoky, fruity, citrus and bitterness distinctions."
            ),

            curated(
                familyKey = "cola",
                allowedValues =
                    listOf(
                        "bitter",
                        "citrusy",
                        "sour",
                        "spicy",
                        "sweet"
                    ),
                rationale =
                    "Canonical cola profiles covering sweetness, acidity, " +
                            "citrus, spice and bitter character."
            ),

            curated(
                familyKey = "energy-drinks",
                allowedValues =
                    listOf(
                        "bitter",
                        "citrusy",
                        "fruity",
                        "sour",
                        "sweet"
                    ),
                rationale =
                    "Canonical energy-drink profiles based on sweetness, " +
                            "acidity, fruit, citrus and bitter functional notes."
            ),

            curated(
                familyKey = "vegetable-juice",
                allowedValues =
                    listOf(
                        "earthy",
                        "herbal",
                        "mild",
                        "savory",
                        "spicy",
                        "umami"
                    ),
                rationale =
                    "Canonical vegetable-juice profiles include savory, earthy, " +
                            "herbal, spicy, mild and umami distinctions."
            ),

            curated(
                familyKey = "sports-drinks",
                allowedValues =
                    listOf(
                        "citrusy",
                        "fruity",
                        "mild",
                        "sour",
                        "sweet"
                    ),
                rationale =
                    "Canonical sports-drink profiles based on fruit, citrus, " +
                            "sweetness, acidity and mild flavor intensity."
            ),

            curated(
                familyKey = "table-water",
                allowedValues =
                    listOf(
                        "mild",
                        "mineral",
                        "neutral"
                    ),
                rationale =
                    "Canonical table-water profiles distinguish neutral, mild " +
                            "and mineral sensory character."
            ),

            curated(
                familyKey = "cocoa-drinks",
                allowedValues =
                    listOf(
                        "bitter",
                        "creamy",
                        "nutty",
                        "roasted",
                        "sweet"
                    ),
                rationale =
                    "Canonical cocoa-drink profiles include cocoa bitterness, " +
                            "roasted and nutty character, sweetness and creaminess."
            ),

            curated(
                familyKey = "malt-drinks",
                allowedValues =
                    listOf(
                        "creamy",
                        "malty",
                        "mild",
                        "roasted",
                        "sweet"
                    ),
                rationale =
                    "Canonical malt-drink profiles include malt character, " +
                            "roasted notes, sweetness, mildness and creaminess."
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
            familyKey =
                familyKey,

            axis =
                CanonicalProductFamilyVariantAxis
                    .FLAVOR_PROFILE,

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
            "ShopMe canonical semantic policy batch 2: " +
                    "beverages × FLAVOR_PROFILE"
    }
}