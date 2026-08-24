package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.retrieval

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalAlias
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalIdentity
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalVariant
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.inference.HimSemanticEvidenceRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentClassificationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimEvidenceAlignmentInputV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.retrieval.HimPrimaryIdentityStateV1
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RunHimEvidenceAlignmentContractV1Test {

    @Test
    fun canonicalPrimaryIdentitySupportsDirectEvidence() {
        val result = evaluate(primary = "Vanille")

        assertEquals(HimEvidenceAlignmentClassificationV1.PRIMARY_CANONICAL_MATCH, result.classification)
        assertTrue(result.directEvidenceSupported)
        assertEquals(HimSemanticEvidenceRelation.DIRECT, result.effectiveEvidenceRelation)
        assertEquals(HimEntityType.CANONICAL, result.matchedAuthorityEntity?.entityType)
    }

    @Test
    fun modifierDoesNotEraseMatchingPrimaryIdentityAndCoverageRemainsSeparate() {
        val result = evaluate(primary = "Vanille", modifiers = listOf("Bourbon"))

        assertEquals(HimEvidenceAlignmentClassificationV1.PRIMARY_CANONICAL_MATCH, result.classification)
        assertTrue(result.directEvidenceSupported)
        assertEquals(listOf("Bourbon"), result.uncoveredModifiers)
        assertEquals(listOf("bourbon"), result.modifierCoverage.map { it.normalizedModifier })
    }

    @Test
    fun canonicalOnlyAsModifierNeverSupportsDirectEvidence() {
        listOf("Pudding", "Zucker", "Keks").forEach { primary ->
            val result = evaluate(primary = primary, modifiers = listOf("Vanille"), claimed = HimSemanticEvidenceRelation.DIRECT)

            assertEquals(HimEvidenceAlignmentClassificationV1.CANONICAL_ONLY_AS_MODIFIER, result.classification)
            assertFalse(result.directEvidenceSupported)
            assertNull(result.effectiveEvidenceRelation)
            assertEquals(HimSemanticEvidenceRelation.DIRECT, result.claimedEvidenceRelation)
        }
    }

    @Test
    fun missingAndUnresolvedPrimaryIdentityFailClosed() {
        val missing = evaluate(primary = null, state = HimPrimaryIdentityStateV1.MISSING)
        val unresolved = evaluate(primary = null, state = HimPrimaryIdentityStateV1.UNRESOLVED)

        assertEquals(HimEvidenceAlignmentClassificationV1.MISSING_PRIMARY_IDENTITY, missing.classification)
        assertEquals(HimEvidenceAlignmentClassificationV1.UNRESOLVED_PRIMARY_IDENTITY, unresolved.classification)
        assertFalse(missing.directEvidenceSupported)
        assertFalse(unresolved.directEvidenceSupported)
        assertNull(missing.effectiveEvidenceRelation)
        assertNull(unresolved.effectiveEvidenceRelation)
    }

    @Test
    fun explicitlyFamilyBoundIdentityVariantAndAliasSupportAlignment() {
        val cases = listOf(
            "Tahiti" to HimEntityType.IDENTITY,
            "gousse" to HimEntityType.VARIANT,
            "Vanilleschote" to HimEntityType.ALIAS,
        )

        cases.forEach { (primary, expectedType) ->
            val result = evaluate(primary = primary)

            assertEquals(HimEvidenceAlignmentClassificationV1.PRIMARY_AUTHORITY_BOUND_MATCH, result.classification)
            assertTrue(result.directEvidenceSupported)
            assertEquals(expectedType, result.matchedAuthorityEntity?.entityType)
        }
    }

    @Test
    fun sameTermWithoutAuthorityBindingDoesNotSupportAlignment() {
        val result = HimEvidenceAlignmentContractV1.evaluate(
            HimEvidenceAlignmentInputV1(
                sourceRecordIdentity = "fixture:unbound",
                primaryIdentity = "Vanilleschote",
                primaryIdentityState = HimPrimaryIdentityStateV1.RESOLVED,
            ),
            family(withChildren = false),
        )

        assertEquals(HimEvidenceAlignmentClassificationV1.OTHER_PRIMARY_IDENTITY, result.classification)
        assertFalse(result.directEvidenceSupported)
        assertNull(result.effectiveEvidenceRelation)
    }

    @Test
    fun repeatedEvaluationPreservesSemanticAndModifierOrder() {
        val input = HimEvidenceAlignmentInputV1(
            sourceRecordIdentity = "fixture:repeat",
            primaryIdentity = "Vanille",
            primaryIdentityState = HimPrimaryIdentityStateV1.RESOLVED,
            modifiers = listOf("Bourbon", "gousse"),
        )

        val first = HimEvidenceAlignmentContractV1.evaluate(input, family(withChildren = true))
        val second = HimEvidenceAlignmentContractV1.evaluate(input, family(withChildren = true))

        assertEquals(first, second)
        assertEquals(listOf("Bourbon", "gousse"), first.modifierCoverage.map { it.modifier })
        assertEquals(listOf("Bourbon"), first.uncoveredModifiers)
    }

    private fun evaluate(
        primary: String?,
        modifiers: List<String> = emptyList(),
        state: HimPrimaryIdentityStateV1 = if (primary == null) HimPrimaryIdentityStateV1.MISSING else HimPrimaryIdentityStateV1.RESOLVED,
        claimed: HimSemanticEvidenceRelation? = null,
    ) = HimEvidenceAlignmentContractV1.evaluate(
        HimEvidenceAlignmentInputV1(
            sourceRecordIdentity = "fixture:${primary ?: state.name.lowercase()}",
            primaryIdentity = primary,
            primaryIdentityState = state,
            modifiers = modifiers,
            claimedEvidenceRelation = claimed,
        ),
        family(withChildren = true),
    )

    private fun family(withChildren: Boolean): HimCanonicalFamily = HimCanonicalFamily(
        canonicalId = HimEntityId("uEV2jY"),
        canonicalName = "Vanille",
        normalizedName = "vanille",
        taxonomyPaths = emptyList(),
        lifecycleStatus = HimLifecycleStatus.ACTIVE,
        identities = if (withChildren) listOf(
            HimCanonicalIdentity(
                identityId = HimEntityId("a00001"),
                identityName = "Tahiti",
                normalizedName = "tahiti",
                lifecycleStatus = HimLifecycleStatus.ACTIVE,
                variants = listOf(
                    HimCanonicalVariant(
                        variantId = HimEntityId("a00002"),
                        variantName = "gousse",
                        normalizedName = "gousse",
                        lifecycleStatus = HimLifecycleStatus.ACTIVE,
                    ),
                ),
                aliases = listOf(
                    HimCanonicalAlias(
                        aliasId = HimEntityId("a00003"),
                        aliasName = "Vanilleschote",
                        normalizedName = "vanilleschote",
                        lifecycleStatus = HimLifecycleStatus.ACTIVE,
                    ),
                ),
            ),
        ) else emptyList(),
        variants = emptyList(),
        aliases = emptyList(),
    )
}
