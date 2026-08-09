package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.rule

import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalExpansionSemanticContext
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalExpansionSemanticDecision
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalExpansionSemanticFinding
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalExpansionSemanticRule
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalExpansionSemanticRuleType
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalFamilySemanticValuePolicy
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType

class CanonicalFamilyValueCompatibilityRule(
    private val familySemanticValuePolicy:
    CanonicalFamilySemanticValuePolicy
) : CanonicalExpansionSemanticRule {

    override fun evaluate(
        context: CanonicalExpansionSemanticContext
    ): List<CanonicalExpansionSemanticFinding> {
        val findings =
            mutableListOf<
                    CanonicalExpansionSemanticFinding
                    >()

        context.candidate.variantValues
            .forEach { value ->
                val policyEntry =
                    familySemanticValuePolicy.find(
                        familyKey =
                            context.candidate.familyKey,
                        axis =
                            value.axis
                    )

                when {
                    policyEntry == null ||
                            !policyEntry.complete ->
                        findings +=
                            CanonicalExpansionSemanticFinding(
                                ruleType =
                                    CanonicalExpansionSemanticRuleType
                                        .FAMILY_SPECIFIC_REVIEW,

                                decision =
                                    CanonicalExpansionSemanticDecision
                                        .REVIEW_REQUIRED,

                                ruleKey =
                                    "missing-family-axis-policy",

                                message =
                                    "No complete semantic value policy " +
                                            "exists for family " +
                                            "'${context.candidate.familyKey}' " +
                                            "and axis '${value.axis}'."
                            )

                    policyEntry.policyType ==
                            CanonicalFamilyAxisSemanticPolicyType
                                .NOT_APPLICABLE ->
                        findings +=
                            CanonicalExpansionSemanticFinding(
                                ruleType =
                                    CanonicalExpansionSemanticRuleType
                                        .FAMILY_VALUE_COMPATIBILITY,

                                decision =
                                    CanonicalExpansionSemanticDecision
                                        .REJECT_WRONG_FAMILY_VALUE,

                                ruleKey =
                                    "family-axis-not-applicable",

                                message =
                                    "Axis '${value.axis}' is not applicable " +
                                            "to family " +
                                            "'${context.candidate.familyKey}'."
                            )

                    value.valueKey !in
                            policyEntry.allowedValues ->
                        findings +=
                            CanonicalExpansionSemanticFinding(
                                ruleType =
                                    CanonicalExpansionSemanticRuleType
                                        .FAMILY_VALUE_COMPATIBILITY,

                                decision =
                                    CanonicalExpansionSemanticDecision
                                        .REJECT_WRONG_FAMILY_VALUE,

                                ruleKey =
                                    "wrong-family-axis-value",

                                message =
                                    "Value '${value.valueKey}' is not " +
                                            "allowed for family " +
                                            "'${context.candidate.familyKey}' " +
                                            "on axis '${value.axis}'."
                            )
                }
            }

        return findings
    }
}