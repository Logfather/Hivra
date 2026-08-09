package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.batch.closure

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family
.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy
.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy
.CanonicalFamilyAxisSemanticPolicyType

class CanonicalBoundedSemanticPolicyOverrideRegistry {

    fun findOverride(
        familyKey: String,
        axis: CanonicalProductFamilyVariantAxis
    ): CanonicalFamilyAxisSemanticPolicyEntry? =
        overridesByIdentity[
            identityKey(
                familyKey = familyKey,
                axis = axis
            )
        ]

    fun allOverrides():
            List<CanonicalFamilyAxisSemanticPolicyEntry> =
        overridesByIdentity
            .values
            .sortedWith(
                compareBy<
                        CanonicalFamilyAxisSemanticPolicyEntry
                        > {
                    it.familyKey
                }.thenBy {
                    it.axis.name
                }
            )

    private fun identityKey(
        familyKey: String,
        axis: CanonicalProductFamilyVariantAxis
    ): String =
        "${familyKey.trim()}::${axis.name}"

    private fun notApplicablePreparationState(
        familyKey: String,
        rationale: String
    ): CanonicalFamilyAxisSemanticPolicyEntry =
        CanonicalFamilyAxisSemanticPolicyEntry(
            familyKey =
                familyKey,

            axis =
                CanonicalProductFamilyVariantAxis
                    .PREPARATION_STATE,

            policyType =
                CanonicalFamilyAxisSemanticPolicyType
                    .NOT_APPLICABLE,

            allowedValues =
                emptyList(),

            rationale =
                rationale.trim(),

            source =
                OVERRIDE_SOURCE,

            active =
                true
        )

    private val overridesByIdentity:
            Map<String, CanonicalFamilyAxisSemanticPolicyEntry> =
        listOf(
            notApplicablePreparationState(
                familyKey =
                    "dark-chocolate",

                rationale =
                    "Preparation states such as cooked or boiled do not " +
                            "represent canonical retail variants of dark chocolate."
            ),

            notApplicablePreparationState(
                familyKey =
                    "filled-chocolate",

                rationale =
                    "Preparation states such as cooked or boiled do not " +
                            "represent canonical retail variants of filled chocolate."
            ),

            notApplicablePreparationState(
                familyKey =
                    "milk-chocolate",

                rationale =
                    "Preparation states such as cooked or boiled do not " +
                            "represent canonical retail variants of milk chocolate."
            ),

            notApplicablePreparationState(
                familyKey =
                    "pralines",

                rationale =
                    "Preparation states such as cooked or boiled do not " +
                            "represent canonical retail variants of pralines."
            ),

            notApplicablePreparationState(
                familyKey =
                    "chocolate-bars",

                rationale =
                    "Preparation states such as cooked or boiled do not " +
                            "represent canonical retail variants of chocolate bars."
            )
        )
            .associateBy { entry ->
                identityKey(
                    familyKey =
                        entry.familyKey,

                    axis =
                        entry.axis
                )
            }

    companion object {

        const val OVERRIDE_SOURCE =
            "ShopMe curated bounded semantic policy overrides"
    }
}