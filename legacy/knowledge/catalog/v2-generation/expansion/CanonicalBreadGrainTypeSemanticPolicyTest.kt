package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.rule

import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate
.CanonicalCatalogExpansionCandidate
import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate
.CanonicalExpansionCandidateStatus
import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate
.CanonicalExpansionCandidateVariantValue
import de.shopme.testing.system.tools.knowledge.catalog.expansion.combination
.CanonicalVariantCombinationMode
import de.shopme.testing.system.tools.knowledge.catalog.expansion.family
.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalExpansionSemanticContext
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalExpansionSemanticDecision
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic
.CanonicalExpansionSemanticRuleType
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalFamilySemanticValuePolicy
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyTestFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalBreadGrainTypeSemanticPolicyTest {

    private val rule =
        CanonicalFamilyValueCompatibilityRuleTestFixtures
            .curatedRule(
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
                    )
            )

    @Test
    fun acceptBreadWheatGrainType() {
        val findings =
            rule.evaluate(
                context(
                    valueKey = "wheat",
                    displayName = "Weizen"
                )
            )

        assertTrue(
            findings.isEmpty(),
            "Bread with wheat must be covered by the explicit policy."
        )
    }

    @Test
    fun acceptBreadRyeGrainType() {
        val findings =
            rule.evaluate(
                context(
                    valueKey = "rye",
                    displayName = "Roggen"
                )
            )

        assertTrue(
            findings.isEmpty(),
            "Bread with rye must be covered by the explicit policy."
        )
    }

    @Test
    fun acceptBreadMixedGrainType() {
        val findings =
            rule.evaluate(
                context(
                    valueKey = "mixed-grain",
                    displayName = "Getreidemischung"
                )
            )

        assertTrue(
            findings.isEmpty(),
            "Mixed-grain bread must be covered by the explicit policy."
        )
    }

    @Test
    fun rejectBreadRiceGrainType() {
        val findings =
            rule.evaluate(
                context(
                    valueKey = "rice",
                    displayName = "Reis"
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
            "wrong-family-axis-value",
            finding.ruleKey
        )
    }

    @Test
    fun noMissingPolicyFindingForBreadGrainType() {
        val findings =
            rule.evaluate(
                context(
                    valueKey = "wheat",
                    displayName = "Weizen"
                )
            )

        assertTrue(
            findings.none {
                it.ruleKey ==
                        "missing-family-axis-policy"
            }
        )
    }

    private fun context(
        valueKey: String,
        displayName: String
    ): CanonicalExpansionSemanticContext {
        val variantValue =
            CanonicalExpansionCandidateVariantValue(
                axis =
                    CanonicalProductFamilyVariantAxis
                        .GRAIN_TYPE,
                valueKey = valueKey,
                displayName = displayName
            )

        val candidate =
            CanonicalCatalogExpansionCandidate(
                candidateIndex = 1,
                candidateKey =
                    "bakery-bread-$valueKey",
                category = "bakery",
                familyKey = "bread",
                familyDisplayName = "Brot",
                familyCandidateIndex = 1,
                templateKey =
                    "single-axis-grain-type",
                combinationMode =
                    CanonicalVariantCombinationMode
                        .SINGLE_AXIS,
                variantValues =
                    listOf(variantValue),
                proposedCanonicalName =
                    "Brot – $displayName",
                proposedNormalizedKey =
                    "bread-$valueKey",
                status =
                    CanonicalExpansionCandidateStatus
                        .REQUIRES_SEMANTIC_VALIDATION,
                requiresSemanticValidation = true
            )

        return CanonicalExpansionSemanticContext(
            candidate = candidate,
            valuesByAxis =
                mapOf(
                    CanonicalProductFamilyVariantAxis
                        .GRAIN_TYPE to
                            valueKey
                )
        )
    }
}