package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyIndex
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySet

class CanonicalFamilySemanticValuePolicy(
    policySet:
    CanonicalFamilyAxisSemanticPolicySet
) {

    private val index =
        CanonicalFamilyAxisSemanticPolicyIndex(
            policySet = policySet
        )

    fun find(
        familyKey: String,
        axis: CanonicalProductFamilyVariantAxis
    ): CanonicalFamilyAxisSemanticPolicyEntry? =
        index.find(
            familyKey = familyKey,
            axis = axis
        )

    fun allowedValues(
        familyKey: String,
        axis: CanonicalProductFamilyVariantAxis
    ): Set<String>? =
        index.allowedValues(
            familyKey = familyKey,
            axis = axis
        )

    fun hasExplicitPolicy(
        familyKey: String,
        axis: CanonicalProductFamilyVariantAxis
    ): Boolean =
        index.hasCompletePolicy(
            familyKey = familyKey,
            axis = axis
        )
}