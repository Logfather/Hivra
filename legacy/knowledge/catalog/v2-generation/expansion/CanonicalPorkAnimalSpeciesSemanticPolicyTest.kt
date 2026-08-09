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
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyTestFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalPorkAnimalSpeciesSemanticPolicyTest {

    private val rule =
        CanonicalFamilyValueCompatibilityRuleTestFixtures
            .closedIdentityRule(
                familyKey = "pork",
                axis =
                    CanonicalProductFamilyVariantAxis
                        .ANIMAL_SPECIES,
                allowedValue = "pig"
            )

    @Test
    fun acceptPigSpeciesForPorkFamilyCompatibility() {
        val findings =
            rule.evaluate(
                context(
                    valueKey = "pig",
                    displayName = "Schwein"
                )
            )

        assertTrue(
            findings.isEmpty(),
            "Pork with animal species pig must be covered by the explicit policy."
        )
    }

    @Test
    fun rejectCattleSpeciesForPork() {
        val findings =
            rule.evaluate(
                context(
                    valueKey = "cattle",
                    displayName = "Rind"
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
    fun rejectWildBoarSpeciesForPork() {
        val findings =
            rule.evaluate(
                context(
                    valueKey = "wild-boar",
                    displayName = "Wildschwein"
                )
            )

        assertEquals(
            CanonicalExpansionSemanticDecision
                .REJECT_WRONG_FAMILY_VALUE,
            findings.single().decision
        )
    }

    @Test
    fun noMissingPolicyFindingForPorkAnimalSpecies() {
        val findings =
            rule.evaluate(
                context(
                    valueKey = "pig",
                    displayName = "Schwein"
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
                        .ANIMAL_SPECIES,
                valueKey = valueKey,
                displayName = displayName
            )

        val candidate =
            CanonicalCatalogExpansionCandidate(
                candidateIndex = 1,
                candidateKey =
                    "meat-pork-$valueKey",
                category = "meat",
                familyKey = "pork",
                familyDisplayName = "Schweinefleisch",
                familyCandidateIndex = 1,
                templateKey =
                    "single-axis-animal-species",
                combinationMode =
                    CanonicalVariantCombinationMode
                        .SINGLE_AXIS,
                variantValues =
                    listOf(variantValue),
                proposedCanonicalName =
                    "Schweinefleisch – $displayName",
                proposedNormalizedKey =
                    "pork-$valueKey",
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
                        .ANIMAL_SPECIES to
                            valueKey
                )
        )
    }
}