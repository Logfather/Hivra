package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis

class CanonicalFamilyAxisSemanticPolicyIndex(
    policySet:
    CanonicalFamilyAxisSemanticPolicySet
) {

    private val entriesByIdentity:
            Map<String, CanonicalFamilyAxisSemanticPolicyEntry>

    init {
        require(policySet.valid) {
            "Semantic policy set must be valid."
        }

        entriesByIdentity =
            policySet.entries
                .associateBy {
                    it.identityKey
                }
                .toSortedMap()

        require(
            entriesByIdentity.size ==
                    policySet.policyCount
        )
    }

    fun find(
        familyKey: String,
        axis: CanonicalProductFamilyVariantAxis
    ): CanonicalFamilyAxisSemanticPolicyEntry? =
        entriesByIdentity[
            identityKey(
                familyKey = familyKey,
                axis = axis
            )
        ]

    fun allowedValues(
        familyKey: String,
        axis: CanonicalProductFamilyVariantAxis
    ): Set<String>? =
        find(
            familyKey = familyKey,
            axis = axis
        )
            ?.takeIf { entry ->
                entry.active &&
                        entry.complete &&
                        entry.policyType !=
                        CanonicalFamilyAxisSemanticPolicyType
                            .NOT_APPLICABLE
            }
            ?.allowedValues
            ?.toSet()

    fun hasCompletePolicy(
        familyKey: String,
        axis: CanonicalProductFamilyVariantAxis
    ): Boolean =
        find(
            familyKey = familyKey,
            axis = axis
        )
            ?.complete ==
                true

    private fun identityKey(
        familyKey: String,
        axis: CanonicalProductFamilyVariantAxis
    ): String =
        "$familyKey::${axis.name}"
}