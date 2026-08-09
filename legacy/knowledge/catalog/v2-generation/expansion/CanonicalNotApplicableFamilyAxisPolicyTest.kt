package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.rule

import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate.CanonicalCatalogExpansionCandidate
import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate.CanonicalExpansionCandidateStatus
import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate.CanonicalExpansionCandidateVariantValue
import de.shopme.testing.system.tools.knowledge.catalog.expansion.combination.CanonicalVariantCombinationMode
import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalExpansionSemanticContext
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalExpansionSemanticDecision
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalExpansionSemanticRuleType
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalFamilySemanticValuePolicy
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyTestFactory
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType
import kotlin.test.Test
import kotlin.test.assertEquals

class CanonicalNotApplicableFamilyAxisPolicyTest {

    private val rule =
        CanonicalFamilyValueCompatibilityRule(
            familySemanticValuePolicy =
                CanonicalFamilySemanticValuePolicy(
                    policySet =
                        CanonicalFamilyAxisSemanticPolicyTestFactory
                            .create(
                                CanonicalFamilyAxisSemanticPolicyEntry(
                                    familyKey = "mushrooms",
                                    axis =
                                        CanonicalProductFamilyVariantAxis
                                            .PLANT_SPECIES,
                                    policyType =
                                        CanonicalFamilyAxisSemanticPolicyType
                                            .NOT_APPLICABLE,
                                    allowedValues =
                                        emptyList(),
                                    rationale =
                                        "Mushrooms are fungi.",
                                    source =
                                        "Test fixture",
                                    active =
                                        true
                                )
                            )
                )
        )

    @Test
    fun rejectPlantSpeciesAxisForMushrooms() {
        val findings =
            rule.evaluate(
                context(
                    familyKey = "mushrooms",
                    valueKey = "apple"
                )
            )

        assertEquals(
            1,
            findings.size
        )

        val finding =
            findings.single()

        assertEquals(
            CanonicalExpansionSemanticDecision
                .REJECT_WRONG_FAMILY_VALUE,
            finding.decision
        )

        assertEquals(
            CanonicalExpansionSemanticRuleType
                .FAMILY_VALUE_COMPATIBILITY,
            finding.ruleType
        )

        assertEquals(
            "family-axis-not-applicable",
            finding.ruleKey
        )
    }

    private fun context(
        familyKey: String,
        valueKey: String
    ): CanonicalExpansionSemanticContext {
        val value =
            CanonicalExpansionCandidateVariantValue(
                axis =
                    CanonicalProductFamilyVariantAxis
                        .PLANT_SPECIES,
                valueKey =
                    valueKey,
                displayName =
                    valueKey
            )

        val candidate =
            CanonicalCatalogExpansionCandidate(
                candidateIndex =
                    1,
                candidateKey =
                    "vegetables-$familyKey-$valueKey",
                category =
                    "vegetables",
                familyKey =
                    familyKey,
                familyDisplayName =
                    familyKey,
                familyCandidateIndex =
                    1,
                templateKey =
                    "single-axis-plant-species",
                combinationMode =
                    CanonicalVariantCombinationMode
                        .SINGLE_AXIS,
                variantValues =
                    listOf(value),
                proposedCanonicalName =
                    "$familyKey – $valueKey",
                proposedNormalizedKey =
                    "$familyKey-$valueKey",
                status =
                    CanonicalExpansionCandidateStatus
                        .REQUIRES_SEMANTIC_VALIDATION,
                requiresSemanticValidation =
                    true
            )

        return CanonicalExpansionSemanticContext(
            candidate =
                candidate,
            valuesByAxis =
                mapOf(
                    CanonicalProductFamilyVariantAxis
                        .PLANT_SPECIES to
                            valueKey
                )
        )
    }
}