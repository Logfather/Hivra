package de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic

import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate.CanonicalCatalogExpansionCandidate
import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate.CanonicalCatalogExpansionCandidateGenerationResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate.CanonicalExpansionCandidateCategoryResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate.CanonicalExpansionCandidateFamilyResult
import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate.CanonicalExpansionCandidateStatus
import de.shopme.testing.system.tools.knowledge.catalog.expansion.candidate.CanonicalExpansionCandidateVariantValue
import de.shopme.testing.system.tools.knowledge.catalog.expansion.combination.CanonicalVariantCombinationMode
import de.shopme.testing.system.tools.knowledge.catalog.expansion.family.CanonicalProductFamilyVariantAxis
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyEntry
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicySet
import de.shopme.testing.system.tools.knowledge.catalog.expansion.semantic.policy.CanonicalFamilyAxisSemanticPolicyType
import kotlin.test.Test
import kotlin.test.assertEquals

class CanonicalBerriesPlantSpeciesValidatorTest {

    private val policySet =
        createPolicySet()

    private val validator =
        CanonicalCatalogExpansionSemanticValidator(
            policySet = policySet
        )

    @Test
    fun acceptSupportedBerrySpecies() {
        val candidate =
            candidate(
                candidateIndex = 1,
                valueKey = "strawberry",
                displayName = "Erdbeere"
            )

        val result =
            validator.validate(
                generationResult(candidate)
            )

        assertEquals(
            CanonicalExpansionSemanticDecision.ACCEPT,
            result.entries.single().decision
        )
    }

    @Test
    fun rejectUnsupportedBerrySpecies() {
        val candidate =
            candidate(
                candidateIndex = 1,
                valueKey = "cherry",
                displayName = "Kirsche"
            )

        val result =
            validator.validate(
                generationResult(candidate)
            )

        assertEquals(
            CanonicalExpansionSemanticDecision
                .REJECT_WRONG_FAMILY_VALUE,
            result.entries.single().decision
        )
    }

    private fun createPolicySet():
            CanonicalFamilyAxisSemanticPolicySet {
        val entry =
            CanonicalFamilyAxisSemanticPolicyEntry(
                familyKey =
                    FAMILY_KEY,

                axis =
                    CanonicalProductFamilyVariantAxis
                        .PLANT_SPECIES,

                policyType =
                    CanonicalFamilyAxisSemanticPolicyType
                        .CURATED_ALLOWED_VALUES,

                allowedValues =
                    listOf(
                        "blackberry",
                        "blueberry",
                        "currant",
                        "raspberry",
                        "strawberry"
                    ),

                rationale =
                    "Canonical berry species represented as distinct foods.",

                source =
                    "CanonicalBerriesPlantSpeciesValidatorTest",

                active =
                    true
            )

        return CanonicalFamilyAxisSemanticPolicySet(
            version =
                CanonicalFamilyAxisSemanticPolicySet
                    .CURRENT_VERSION,

            policySetId =
                "canonical-family-axis-semantic-policy-test",

            sourceBaselineId =
                TEST_BASELINE_ID,

            sourceBaselineCatalogSha256 =
                TEST_BASELINE_SHA_256,

            policyCount =
                1,

            activePolicyCount =
                1,

            completePolicyCount =
                1,

            incompletePolicyCount =
                0,

            closedIdentityPolicyCount =
                0,

            curatedAllowedValuesPolicyCount =
                1,

            notApplicablePolicyCount =
                0,

            reviewRequiredPolicyCount =
                0,

            coveredFamilyCount =
                1,

            coveredAxisCount =
                1,

            entries =
                listOf(entry),

            valid =
                true
        )
    }

    private fun candidate(
        candidateIndex: Int,
        valueKey: String,
        displayName: String
    ): CanonicalCatalogExpansionCandidate =
        CanonicalCatalogExpansionCandidate(
            candidateIndex =
                candidateIndex,

            candidateKey =
                "$CATEGORY-$FAMILY_KEY-$valueKey",

            category =
                CATEGORY,

            familyKey =
                FAMILY_KEY,

            familyDisplayName =
                FAMILY_DISPLAY_NAME,

            familyCandidateIndex =
                candidateIndex,

            templateKey =
                "single-axis-plant-species",

            combinationMode =
                CanonicalVariantCombinationMode
                    .SINGLE_AXIS,

            variantValues =
                listOf(
                    CanonicalExpansionCandidateVariantValue(
                        axis =
                            CanonicalProductFamilyVariantAxis
                                .PLANT_SPECIES,

                        valueKey =
                            valueKey,

                        displayName =
                            displayName
                    )
                ),

            proposedCanonicalName =
                "$FAMILY_DISPLAY_NAME – $displayName",

            proposedNormalizedKey =
                "$FAMILY_KEY-$valueKey",

            status =
                CanonicalExpansionCandidateStatus
                    .REQUIRES_SEMANTIC_VALIDATION,

            requiresSemanticValidation =
                true
        )

    private fun generationResult(
        candidate:
        CanonicalCatalogExpansionCandidate
    ): CanonicalCatalogExpansionCandidateGenerationResult {
        val familyResult =
            CanonicalExpansionCandidateFamilyResult(
                familyKey =
                    FAMILY_KEY,

                category =
                    CATEGORY,

                familyDisplayName =
                    FAMILY_DISPLAY_NAME,

                sourceTargetEntryCount =
                    1,

                derivedTargetEntryCount =
                    1,

                estimatedBaselineFamilyEntryCount =
                    0,

                requiredExpansionEntryCount =
                    1,

                generatedCandidateCount =
                    1,

                candidates =
                    listOf(candidate),

                complete =
                    true
            )

        val categoryResult =
            CanonicalExpansionCandidateCategoryResult(
                category =
                    CATEGORY,

                baselineEntryCount =
                    0,

                derivedTargetEntryCount =
                    1,

                requiredExpansionEntryCount =
                    1,

                productFamilyCount =
                    1,

                families =
                    listOf(familyResult),

                generatedCandidateCount =
                    1,

                complete =
                    true
            )

        return CanonicalCatalogExpansionCandidateGenerationResult(
            version =
                CanonicalCatalogExpansionCandidateGenerationResult
                    .CURRENT_VERSION,

            sourceBaselineId =
                TEST_BASELINE_ID,

            sourceBaselineCatalogSha256 =
                TEST_BASELINE_SHA_256,

            baselineEntryCount =
                1,

            derivedTargetEntryCount =
                2,

            requiredExpansionEntryCount =
                1,

            categoryCount =
                1,

            productFamilyCount =
                1,

            generatedCandidateCount =
                1,

            categories =
                listOf(categoryResult),

            candidates =
                listOf(candidate),

            uniqueCandidateKeyCount =
                1,

            uniqueProposedNormalizedKeyCount =
                1,

            candidatesRequiringSemanticValidationCount =
                1,

            exactExpansionCountReached =
                true,

            deterministicOrderValid =
                true,

            blockers =
                emptyList(),

            valid =
                true
        )
    }

    private companion object {

        const val CATEGORY =
            "fruit"

        const val FAMILY_KEY =
            "berries"

        const val FAMILY_DISPLAY_NAME =
            "Beeren"

        const val TEST_BASELINE_ID =
            "canonical-food-catalog-test"

        val TEST_BASELINE_SHA_256 =
            "0".repeat(64)
    }
}