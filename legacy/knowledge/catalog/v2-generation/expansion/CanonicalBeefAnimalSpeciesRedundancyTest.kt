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

class CanonicalBeefAnimalSpeciesRedundancyTest {

    @Test
    fun rejectPureCattleSpeciesVariantAsRedundantForBeef() {
        val variantValue =
            CanonicalExpansionCandidateVariantValue(
                axis =
                    CanonicalProductFamilyVariantAxis
                        .ANIMAL_SPECIES,
                valueKey = "cattle",
                displayName = "Rind"
            )

        val candidate =
            CanonicalCatalogExpansionCandidate(
                candidateIndex = 1,
                candidateKey = "meat-beef-cattle",
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
                    "Rindfleisch – Rind",
                proposedNormalizedKey =
                    "beef-cattle",
                status =
                    CanonicalExpansionCandidateStatus
                        .REQUIRES_SEMANTIC_VALIDATION,
                requiresSemanticValidation = true
            )

        val context =
            CanonicalExpansionSemanticContext(
                candidate = candidate,
                valuesByAxis =
                    mapOf(
                        CanonicalProductFamilyVariantAxis
                            .ANIMAL_SPECIES to
                                "cattle"
                    )
            )

        val findings =
            CanonicalRedundantFamilyValueRule()
                .evaluate(context)

        assertEquals(
            1,
            findings.size
        )

        assertEquals(
            CanonicalExpansionSemanticDecision
                .REJECT_REDUNDANT_VARIANT,
            findings.single().decision
        )

        assertEquals(
            CanonicalExpansionSemanticRuleType
                .REDUNDANT_FAMILY_VALUE,
            findings.single().ruleType
        )
    }
}