package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.rule

import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate.CanonicalCatalogExpansionCandidate
import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate.CanonicalExpansionCandidateStatus
import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate.CanonicalExpansionCandidateVariantValue
import de.shopme.testing.system.tools.knowledge.catalog.expansion.combination.CanonicalVariantCombinationMode
import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalExpansionSemanticContext
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalExpansionSemanticDecision
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.CanonicalExpansionSemanticRuleType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CanonicalBeefAnimalSpeciesSemanticPolicyTest {

    private val rule =
        CanonicalFamilyValueCompatibilityRuleTestFixtures
            .closedIdentityRule(
                familyKey = "beef",
                axis =
                    CanonicalProductFamilyVariantAxis
                        .ANIMAL_SPECIES,
                allowedValue = "cattle"
            )

    @Test
    fun acceptCattleSpeciesForBeefFamilyCompatibility() {
        val findings =
            rule.evaluate(
                context(
                    valueKey = "cattle",
                    displayName = "Rind"
                )
            )

        assertTrue(
            findings.isEmpty(),
            "Beef with animal species cattle must be covered by the explicit policy."
        )
    }

    @Test
    fun rejectCalfSpeciesForBeef() {
        val findings =
            rule.evaluate(
                context(
                    valueKey = "calf",
                    displayName = "Kalb"
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
    fun rejectPigSpeciesForBeef() {
        val findings =
            rule.evaluate(
                context(
                    valueKey = "pig",
                    displayName = "Schwein"
                )
            )

        assertEquals(
            CanonicalExpansionSemanticDecision
                .REJECT_WRONG_FAMILY_VALUE,
            findings.single().decision
        )
    }

    @Test
    fun rejectMixedMeatSpeciesForBeef() {
        val findings =
            rule.evaluate(
                context(
                    valueKey = "mixed-meat",
                    displayName = "Gemischte Fleischarten"
                )
            )

        assertEquals(
            CanonicalExpansionSemanticDecision
                .REJECT_WRONG_FAMILY_VALUE,
            findings.single().decision
        )
    }

    @Test
    fun noMissingPolicyFindingForBeefAnimalSpecies() {
        val findings =
            rule.evaluate(
                context(
                    valueKey = "cattle",
                    displayName = "Rind"
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
                    "meat-beef-$valueKey",
                category = "meat",
                familyKey = "beef",
                familyDisplayName = "Rindfleisch",
                familyCandidateIndex = 1,
                templateKey =
                    "single-axis-animal-species",
                combinationMode =
                    CanonicalVariantCombinationMode
                        .SINGLE_AXIS,
                variantValues =
                    listOf(variantValue),
                proposedCanonicalName =
                    "Rindfleisch – $displayName",
                proposedNormalizedKey =
                    "beef-$valueKey",
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