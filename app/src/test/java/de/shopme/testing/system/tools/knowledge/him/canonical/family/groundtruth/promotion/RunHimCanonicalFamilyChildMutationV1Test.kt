package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.promotion

import de.shopme.tools.knowledge.him.canonical.family.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.*
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecisionReference
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest

class RunHimCanonicalFamilyChildMutationV1Test {
    private val persistence = HimCanonicalFamilyPersistence()
    private val engine = HimCanonicalFamilyChildMutationV1(persistence)

    @Test fun addVariantAndReplayDeterministically() {
        val before = fixture()
        val target = HimCandidatePromotionTargetV1.AddVariant(HimFamilyEntityReference.Canonical(HERRING_ID), "Hering eingelegt")
        val result1 = apply(before, target, HimEntityId("Var001"))
        val result2 = apply(before, target, HimEntityId("Var001"))
        assertEquals(result1, result2)
        assertArrayEquals(persistence.serialize(result1.authorityAfter), persistence.serialize(result2.authorityAfter))
        assertArrayEquals(persistence.serialize(result1.registryAfter), persistence.serialize(result2.registryAfter))
        assertEquals(result1.mutationLedgerEntry.mutationReference, result2.mutationLedgerEntry.mutationReference)
        val herring = result1.authorityAfter.families.single { it.canonicalId == HERRING_ID }
        assertEquals("Hering", herring.canonicalName)
        assertEquals(listOf(HimCanonicalVariant(HimEntityId("Var001"), "Hering eingelegt", "hering eingelegt", HimLifecycleStatus.ACTIVE)), herring.variants)
        assertEquals(before.authority.families[1], result1.authorityAfter.families[1])
        assertEquals(before.authority.sourceCatalog, result1.authorityAfter.sourceCatalog)
        assertRegistryAndLedger(before, result1, HimEntityType.VARIANT, HimGroundTruthMutationType.ADD_VARIANT, HimFamilyEntityReference.Canonical(HERRING_ID))
    }

    @Test fun addIdentityAliasAndIdentityScopedVariant() {
        val before = fixture()
        val identity = apply(before, HimCandidatePromotionTargetV1.AddIdentity(HERRING_ID, "Matjes"), HimEntityId("Ide001"))
        assertEquals("Matjes", identity.authorityAfter.families[0].identities.single().identityName)
        assertRegistryAndLedger(before, identity, HimEntityType.IDENTITY, HimGroundTruthMutationType.ADD_IDENTITY, HimFamilyEntityReference.Canonical(HERRING_ID))

        val identityBefore = Fixture(identity.authorityAfter, identity.registryAfter)
        val identityScope = HimFamilyEntityReference.Identity(HERRING_ID, HimEntityId("Ide001"))
        val alias = apply(identityBefore, HimCandidatePromotionTargetV1.AddAlias(identityScope, "Matjeshering"), HimEntityId("Ali001"))
        assertEquals("Matjeshering", alias.authorityAfter.families[0].identities.single().aliases.single().aliasName)
        assertRegistryAndLedger(identityBefore, alias, HimEntityType.ALIAS, HimGroundTruthMutationType.ADD_ALIAS, identityScope)

        val variant = apply(identityBefore, HimCandidatePromotionTargetV1.AddVariant(identityScope, "Matjes in Öl"), HimEntityId("Var002"))
        assertEquals("Matjes in Öl", variant.authorityAfter.families[0].identities.single().variants.single().variantName)
        assertRegistryAndLedger(identityBefore, variant, HimEntityType.VARIANT, HimGroundTruthMutationType.ADD_VARIANT, identityScope)

        val canonicalAlias = apply(before, HimCandidatePromotionTargetV1.AddAlias(HimFamilyEntityReference.Canonical(HERRING_ID), "Clupea harengus"), HimEntityId("Ali002"))
        assertEquals("Clupea harengus", canonicalAlias.authorityAfter.families[0].aliases.single().aliasName)
    }

    @Test fun failClosedGuards() {
        val before = fixture()
        val existingVariant = apply(before, HimCandidatePromotionTargetV1.AddVariant(HimFamilyEntityReference.Canonical(HERRING_ID), "Hering eingelegt"), HimEntityId("Var001"))
        assertFails { apply(Fixture(existingVariant.authorityAfter, existingVariant.registryAfter), HimCandidatePromotionTargetV1.AddVariant(HimFamilyEntityReference.Canonical(HERRING_ID), "  HERING   EINGELEGT "), HimEntityId("Var002")) }
        assertFails { apply(before, HimCandidatePromotionTargetV1.AddVariant(HimFamilyEntityReference.Canonical(HERRING_ID), "Neu"), HERRING_ID) }
        val registryCollision = before.registry.copy(entries = before.registry.entries + HimEntityIdRegistryEntry(HimEntityId("Dup001"), HimEntityType.ALIAS, HimEntitySourceReferenceType.GROUND_TRUTH_PROMOTION, "promotion:v1:${"1".repeat(64)}"))
        assertFails { apply(Fixture(before.authority, registryCollision), HimCandidatePromotionTargetV1.AddAlias(HimFamilyEntityReference.Canonical(HERRING_ID), "Neu"), HimEntityId("Dup001")) }
        assertFails { apply(before, HimCandidatePromotionTargetV1.AddIdentity(HimEntityId("NoSuch"), "Neu"), HimEntityId("Ide001")) }
        assertFails { apply(before, HimCandidatePromotionTargetV1.AddVariant(HimFamilyEntityReference.Identity(HERRING_ID, HimEntityId("NoSuch")), "Neu"), HimEntityId("Var001")) }
        assertFails { apply(before, HimCandidatePromotionTargetV1.CreateCanonical("Neue Familie"), HimEntityId("Can001")) }
    }

    private fun apply(before: Fixture, target: HimCandidatePromotionTargetV1, newId: HimEntityId): HimCanonicalFamilyChildMutationResultV1 {
        val authorityDigest = digest(persistence.serialize(before.authority))
        val registryDigest = digest(persistence.serialize(before.registry))
        val validation = HimCandidateValidationDecisionReference("validation:v1:${"2".repeat(64)}")
        val promotion = HimCandidatePromotionProposalV1(
            promotionReference = HimCandidatePromotionIdentityV1.promotion(
                HimCandidateReference("candidate:test"), validation, digest("dataset".toByteArray()), authorityDigest, registryDigest, target,
            ),
            candidateReference = HimCandidateReference("candidate:test"),
            validationReference = validation,
            candidateDatasetDigest = digest("dataset".toByteArray()),
            authorityDigestBefore = authorityDigest,
            entityIdRegistryDigestBefore = registryDigest,
            target = target,
        )
        return engine.apply(before.authority, before.registry, promotion, HimValidationReference(validation.value), newId, authorityDigest, registryDigest)
    }

    private fun assertRegistryAndLedger(before: Fixture, result: HimCanonicalFamilyChildMutationResultV1, entityType: HimEntityType, mutationType: HimGroundTruthMutationType, parent: HimFamilyEntityReference) {
        assertEquals(before.registry.entries, result.registryAfter.entries.dropLast(1))
        val entry = result.registryAfter.entries.last()
        assertEquals(entityType, entry.entityType)
        assertEquals(HimEntitySourceReferenceType.GROUND_TRUTH_PROMOTION, entry.sourceReferenceType)
        assertTrue(entry.sourceReference.startsWith("promotion:v1:"))
        assertEquals(mutationType, result.mutationLedgerEntry.mutationType)
        assertEquals(entityType, result.mutationLedgerEntry.entityType)
        assertEquals(parent, result.mutationLedgerEntry.parent)
        assertNotEquals(result.mutationLedgerEntry.canonicalFamilyAuthoritySha256Before, result.mutationLedgerEntry.canonicalFamilyAuthoritySha256After)
        assertNotEquals(result.mutationLedgerEntry.entityIdRegistrySha256Before, result.mutationLedgerEntry.entityIdRegistrySha256After)
        assertEquals(before.authority.families.size, result.authorityAfter.families.size)
        assertEquals(before.authority.schemaVersion, result.authorityAfter.schemaVersion)
        assertEquals(before.authority.sourceCatalog, result.authorityAfter.sourceCatalog)
        assertEquals(before.authority.families.drop(1), result.authorityAfter.families.drop(1))
        assertEquals(before.registry.entries.size + 1, result.registryAfter.entries.size)
        assertEquals(setOf(result.mutationLedgerEntry.newEntityId), authorityIds(result.authorityAfter) - authorityIds(before.authority))
    }

    private fun fixture(): Fixture {
        val source = HimCanonicalFamilySourceCatalog("fixture.json", "a".repeat(64), 2)
        val herring = HimCanonicalFamily(HERRING_ID, "Hering", "hering", emptyList(), HimLifecycleStatus.ACTIVE, emptyList(), emptyList(), emptyList())
        val apple = HimCanonicalFamily(APPLE_ID, "Apfel", "apfel", emptyList(), HimLifecycleStatus.ACTIVE, emptyList(), emptyList(), emptyList())
        val authority = HimCanonicalFamilyAuthority("1", source, listOf(herring, apple))
        val registry = HimEntityIdRegistry(listOf(
            HimEntityIdRegistryEntry(HERRING_ID, HimEntityType.CANONICAL, HimEntitySourceReferenceType.PRODUCT_ONLY_CANONICAL_NORMALIZED, "hering"),
            HimEntityIdRegistryEntry(APPLE_ID, HimEntityType.CANONICAL, HimEntitySourceReferenceType.PRODUCT_ONLY_CANONICAL_NORMALIZED, "apfel"),
        ))
        return Fixture(authority, registry)
    }

    private fun digest(bytes: ByteArray) = HimSha256(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) })
    private fun authorityIds(authority: HimCanonicalFamilyAuthority): Set<HimEntityId> = authority.families.flatMap { family ->
        listOf(family.canonicalId) + family.variants.map { it.variantId } + family.aliases.map { it.aliasId } + family.identities.flatMap { identity ->
            listOf(identity.identityId) + identity.variants.map { it.variantId } + identity.aliases.map { it.aliasId }
        }
    }.toSet()
    private fun assertFails(block: () -> Unit) = assertTrue(runCatching(block).isFailure)
    private data class Fixture(val authority: HimCanonicalFamilyAuthority, val registry: HimEntityIdRegistry)

    companion object {
        private val HERRING_ID = HimEntityId("OzlByp")
        private val APPLE_ID = HimEntityId("Apl001")
    }
}
