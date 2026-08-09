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

class CanonicalBerriesPlantSpeciesSemanticPolicyTest {

    private val rule =
        CanonicalFamilyValueCompatibilityRule(
            familySemanticValuePolicy =
                CanonicalFamilySemanticValuePolicy(
                    policySet =
                        CanonicalFamilyAxisSemanticPolicyTestFactory
                            .create(
                                CanonicalFamilyAxisSemanticPolicyTestFactory
                                    .curated(
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
                                            )
                                    )
                            )
                )
        )

    @Test
    fun acceptStrawberryForBerries() {
        val findings =
            rule.evaluate(
                context(
                    valueKey = "strawberry",
                    displayName = "Erdbeere"
                )
            )

        assertTrue(
            findings.isEmpty(),
            "Strawberry must be accepted for the berries family."
        )
    }

    @Test
    fun acceptRaspberryForBerries() {
        val findings =
            rule.evaluate(
                context(
                    valueKey = "raspberry",
                    displayName = "Himbeere"
                )
            )

        assertTrue(
            findings.isEmpty(),
            "Raspberry must be accepted for the berries family."
        )
    }

    @Test
    fun acceptBlueberryForBerries() {
        val findings =
            rule.evaluate(
                context(
                    valueKey = "blueberry",
                    displayName = "Blaubeere"
                )
            )

        assertTrue(
            findings.isEmpty(),
            "Blueberry must be accepted for the berries family."
        )
    }

    @Test
    fun acceptBlackberryForBerries() {
        val findings =
            rule.evaluate(
                context(
                    valueKey = "blackberry",
                    displayName = "Brombeere"
                )
            )

        assertTrue(
            findings.isEmpty(),
            "Blackberry must be accepted for the berries family."
        )
    }

    @Test
    fun acceptCurrantForBerries() {
        val findings =
            rule.evaluate(
                context(
                    valueKey = "currant",
                    displayName = "Johannisbeere"
                )
            )

        assertTrue(
            findings.isEmpty(),
            "Currant must be accepted for the berries family."
        )
    }

    @Test
    fun rejectCherryForBerries() {
        val findings =
            rule.evaluate(
                context(
                    valueKey = "cherry",
                    displayName = "Kirsche"
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
    fun rejectGrapeForBerries() {
        val findings =
            rule.evaluate(
                context(
                    valueKey = "grape",
                    displayName = "Traube"
                )
            )

        assertEquals(
            CanonicalExpansionSemanticDecision
                .REJECT_WRONG_FAMILY_VALUE,
            findings.single().decision
        )
    }

    @Test
    fun noMissingPolicyFindingForBerriesPlantSpecies() {
        val findings =
            rule.evaluate(
                context(
                    valueKey = "strawberry",
                    displayName = "Erdbeere"
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
                        .PLANT_SPECIES,
                valueKey = valueKey,
                displayName = displayName
            )

        val candidate =
            CanonicalCatalogExpansionCandidate(
                candidateIndex = 1,
                candidateKey =
                    "fruit-berries-$valueKey",
                category = "fruit",
                familyKey = "berries",
                familyDisplayName = "Beeren",
                familyCandidateIndex = 1,
                templateKey =
                    "single-axis-plant-species",
                combinationMode =
                    CanonicalVariantCombinationMode
                        .SINGLE_AXIS,
                variantValues =
                    listOf(variantValue),
                proposedCanonicalName =
                    "Beeren – $displayName",
                proposedNormalizedKey =
                    "berries-$valueKey",
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
                        .PLANT_SPECIES to
                            valueKey
                )
        )
    }
}