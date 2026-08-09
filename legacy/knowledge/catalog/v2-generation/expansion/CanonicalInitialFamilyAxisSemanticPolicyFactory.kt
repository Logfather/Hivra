package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy

import de.shopme.testing.system.tools.knowledge.catalog.baseline.CanonicalFoodCatalogBaseline
import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import java.security.MessageDigest

class CanonicalInitialFamilyAxisSemanticPolicyFactory {

    fun create(
        baseline:
        CanonicalFoodCatalogBaseline
    ): CanonicalFamilyAxisSemanticPolicySet {
        require(baseline.valid)

        val entries =
            listOf(
                closedIdentity(
                    familyKey = "beef",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .ANIMAL_SPECIES,
                    allowedValue = "cattle",
                    rationale =
                        "Beef is canonically derived from cattle."
                ),

                curated(
                    familyKey = "berries",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .PLANT_SPECIES,
                    allowedValues =
                        listOf(
                            "blackberry",
                            "blueberry",
                            "currant",
                            "raspberry",
                            "strawberry"
                        ),
                    rationale =
                        "Canonical berry species represented as distinct foods."
                ),

                curated(
                    familyKey = "bread",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .GRAIN_TYPE,
                    allowedValues =
                        listOf(
                            "barley",
                            "buckwheat",
                            "corn",
                            "millet",
                            "mixed-grain",
                            "oats",
                            "rye",
                            "spelt",
                            "wheat",
                            "wholegrain"
                        ),
                    rationale =
                        "Common canonical grain identities for bread."
                ),

                curated(
                    familyKey = "butter",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .DIETARY_FORM,
                    allowedValues =
                        listOf(
                            "lactose-free",
                            "organic-recipe",
                            "standard"
                        ),
                    rationale =
                        "Supported dietary forms for canonical butter."
                ),

                curated(
                    familyKey = "butter",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .FAT_LEVEL,
                    allowedValues =
                        listOf(
                            "full-fat",
                            "high-fat"
                        ),
                    rationale =
                        "Canonical butter requires a high milk-fat level."
                ),

                curated(
                    familyKey = "butter",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .FLAVOR_PROFILE,
                    allowedValues =
                        listOf(
                            "neutral",
                            "salty"
                        ),
                    rationale =
                        "Supported canonical butter flavor profiles."
                ),

                curated(
                    familyKey = "canned-fish",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .PRESERVATION_METHOD,
                    allowedValues =
                        listOf(
                            "canned",
                            "shelf-stable"
                        ),
                    rationale =
                        "Canned fish is shelf-stable or explicitly canned."
                ),

                curated(
                    familyKey = "canned-fruit",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .PRESERVATION_METHOD,
                    allowedValues =
                        listOf(
                            "canned",
                            "shelf-stable"
                        ),
                    rationale =
                        "Canned fruit is shelf-stable or explicitly canned."
                ),

                curated(
                    familyKey = "canned-vegetables",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .PRESERVATION_METHOD,
                    allowedValues =
                        listOf(
                            "canned",
                            "shelf-stable"
                        ),
                    rationale =
                        "Canned vegetables are shelf-stable or explicitly canned."
                ),

                curated(
                    familyKey = "dried-fruit",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .PREPARATION_STATE,
                    allowedValues =
                        listOf(
                            "prepared",
                            "raw",
                            "ready-to-eat"
                        ),
                    rationale =
                        "Supported preparation states for dried fruit."
                ),

                closedIdentity(
                    familyKey = "dried-fruit",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .PRESERVATION_METHOD,
                    allowedValue = "dried",
                    rationale =
                        "Dried fruit canonically uses drying as preservation."
                ),

                curated(
                    familyKey = "fresh-fish",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .PREPARATION_STATE,
                    allowedValues =
                        listOf(
                            "raw",
                            "ready-to-cook"
                        ),
                    rationale =
                        "Supported preparation states for fresh fish."
                ),

                curated(
                    familyKey = "fresh-fish",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .PRESERVATION_METHOD,
                    allowedValues =
                        listOf(
                            "chilled",
                            "fresh"
                        ),
                    rationale =
                        "Fresh fish must be fresh or chilled."
                ),

                curated(
                    familyKey = "milk",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .DIETARY_FORM,
                    allowedValues =
                        listOf(
                            "lactose-free",
                            "organic-recipe",
                            "standard"
                        ),
                    rationale =
                        "Supported dietary forms for canonical milk."
                ),

                curated(
                    familyKey = "milk",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .FAT_LEVEL,
                    allowedValues =
                        listOf(
                            "fat-free",
                            "full-fat",
                            "low-fat",
                            "medium-fat",
                            "very-low-fat"
                        ),
                    rationale =
                        "Supported canonical milk-fat levels."
                ),

                curated(
                    familyKey = "milk",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .PRESERVATION_METHOD,
                    allowedValues =
                        listOf(
                            "chilled",
                            "fresh",
                            "shelf-stable"
                        ),
                    rationale =
                        "Supported preservation forms for milk."
                ),

                curated(
                    familyKey = "milk",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .PROCESSING_METHOD,
                    allowedValues =
                        listOf(
                            "fermented",
                            "unprocessed"
                        ),
                    rationale =
                        "Currently supported processing states for milk."
                ),

                closedIdentity(
                    familyKey = "olive-oil",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .PHYSICAL_FORM,
                    allowedValue = "liquid",
                    rationale =
                        "Olive oil is canonically liquid."
                ),

                closedIdentity(
                    familyKey = "olive-oil",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .PRIMARY_INGREDIENT,
                    allowedValue = "olive",
                    rationale =
                        "Olive oil is canonically derived from olives."
                ),

                closedIdentity(
                    familyKey = "pork",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .ANIMAL_SPECIES,
                    allowedValue = "pig",
                    rationale =
                        "Pork is canonically derived from pigs."
                ),

                closedIdentity(
                    familyKey = "rapeseed-oil",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .PHYSICAL_FORM,
                    allowedValue = "liquid",
                    rationale =
                        "Rapeseed oil is canonically liquid."
                ),

                closedIdentity(
                    familyKey = "rapeseed-oil",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .PRIMARY_INGREDIENT,
                    allowedValue = "rapeseed",
                    rationale =
                        "Rapeseed oil is canonically derived from rapeseed."
                ),

                curated(
                    familyKey = "smoked-fish",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .PRESERVATION_METHOD,
                    allowedValues =
                        listOf(
                            "chilled",
                            "smoked",
                            "vacuum-packed"
                        ),
                    rationale =
                        "Supported preservation forms for smoked fish."
                ),

                closedIdentity(
                    familyKey = "smoked-fish",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .PROCESSING_METHOD,
                    allowedValue = "smoked",
                    rationale =
                        "Smoked fish canonically uses smoking."
                ),

                closedIdentity(
                    familyKey = "sunflower-oil",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .PHYSICAL_FORM,
                    allowedValue = "liquid",
                    rationale =
                        "Sunflower oil is canonically liquid."
                ),

                closedIdentity(
                    familyKey = "sunflower-oil",
                    axis =
                        CanonicalProductFamilyVariantAxis
                            .PRIMARY_INGREDIENT,
                    allowedValue = "sunflower",
                    rationale =
                        "Sunflower oil is canonically derived from sunflower."
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

        val completeCount =
            entries.count {
                it.complete
            }

        val policySetId =
            createPolicySetId(
                entries = entries,
                baselineId = baseline.baselineId
            )

        return CanonicalFamilyAxisSemanticPolicySet(
            version =
                CanonicalFamilyAxisSemanticPolicySet
                    .CURRENT_VERSION,

            policySetId =
                policySetId,

            sourceBaselineId =
                baseline.baselineId,

            sourceBaselineCatalogSha256 =
                baseline.catalogArtifact.sha256,

            policyCount =
                entries.size,

            activePolicyCount =
                entries.count {
                    it.active
                },

            completePolicyCount =
                completeCount,

            incompletePolicyCount =
                entries.size -
                        completeCount,

            closedIdentityPolicyCount =
                entries.count {
                    it.policyType ==
                            CanonicalFamilyAxisSemanticPolicyType
                                .CLOSED_IDENTITY
                },

            curatedAllowedValuesPolicyCount =
                entries.count {
                    it.policyType ==
                            CanonicalFamilyAxisSemanticPolicyType
                                .CURATED_ALLOWED_VALUES
                },

            notApplicablePolicyCount =
                0,

            reviewRequiredPolicyCount =
                entries.count {
                    it.policyType ==
                            CanonicalFamilyAxisSemanticPolicyType
                                .REVIEW_REQUIRED
                },

            coveredFamilyCount =
                entries
                    .map {
                        it.familyKey
                    }
                    .distinct()
                    .size,

            coveredAxisCount =
                entries
                    .map {
                        it.axis
                    }
                    .distinct()
                    .size,

            entries =
                entries,

            valid =
                entries.isNotEmpty() &&
                        entries.all {
                            it.complete &&
                                    it.active
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
            familyKey = familyKey,
            axis = axis,
            policyType =
                CanonicalFamilyAxisSemanticPolicyType
                    .CURATED_ALLOWED_VALUES,
            allowedValues =
                allowedValues
                    .map(String::trim)
                    .filter(String::isNotBlank)
                    .distinct()
                    .sorted(),
            rationale = rationale.trim(),
            source =
                "ShopMe canonical catalog semantic policy migration",
            active = true
        )

    private fun closedIdentity(
        familyKey: String,
        axis: CanonicalProductFamilyVariantAxis,
        allowedValue: String,
        rationale: String
    ): CanonicalFamilyAxisSemanticPolicyEntry =
        CanonicalFamilyAxisSemanticPolicyEntry(
            familyKey = familyKey,
            axis = axis,
            policyType =
                CanonicalFamilyAxisSemanticPolicyType
                    .CLOSED_IDENTITY,
            allowedValues =
                listOf(allowedValue.trim()),
            rationale = rationale.trim(),
            source =
                "ShopMe canonical catalog semantic policy migration",
            active = true
        )

    private fun createPolicySetId(
        entries:
        List<CanonicalFamilyAxisSemanticPolicyEntry>,

        baselineId: String
    ): String {
        val canonicalPayload =
            buildString {
                append(baselineId)
                append('\n')

                entries.forEach { entry ->
                    append(entry.familyKey)
                    append('|')
                    append(entry.axis.name)
                    append('|')
                    append(entry.policyType.name)
                    append('|')
                    append(
                        entry.allowedValues
                            .joinToString(",")
                    )
                    append('\n')
                }
            }

        val hash =
            MessageDigest
                .getInstance("SHA-256")
                .digest(
                    canonicalPayload
                        .toByteArray(Charsets.UTF_8)
                )
                .joinToString("") {
                    "%02x".format(it)
                }
                .take(16)

        return "canonical-family-axis-semantic-policy-v1-$hash"
    }
}