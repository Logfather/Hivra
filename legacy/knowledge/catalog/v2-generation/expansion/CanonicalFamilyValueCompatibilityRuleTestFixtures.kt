package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.rule

import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalFamilySemanticValuePolicy
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySet
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyTestFactory

object CanonicalFamilyValueCompatibilityRuleTestFixtures {

    fun rule(
        entry:
        CanonicalFamilyAxisSemanticPolicyEntry
    ): CanonicalFamilyValueCompatibilityRule =
        rule(
            policySet =
                CanonicalFamilyAxisSemanticPolicyTestFactory
                    .create(entry)
        )

    fun rule(
        policySet:
        CanonicalFamilyAxisSemanticPolicySet
    ): CanonicalFamilyValueCompatibilityRule =
        CanonicalFamilyValueCompatibilityRule(
            familySemanticValuePolicy =
                CanonicalFamilySemanticValuePolicy(
                    policySet = policySet
                )
        )

    fun curatedRule(
        familyKey: String,
        axis: CanonicalProductFamilyVariantAxis,
        allowedValues: List<String>
    ): CanonicalFamilyValueCompatibilityRule =
        rule(
            CanonicalFamilyAxisSemanticPolicyTestFactory
                .curated(
                    familyKey = familyKey,
                    axis = axis,
                    allowedValues = allowedValues
                )
        )

    fun closedIdentityRule(
        familyKey: String,
        axis: CanonicalProductFamilyVariantAxis,
        allowedValue: String
    ): CanonicalFamilyValueCompatibilityRule =
        rule(
            CanonicalFamilyAxisSemanticPolicyTestFactory
                .closedIdentity(
                    familyKey = familyKey,
                    axis = axis,
                    allowedValue = allowedValue
                )
        )
}