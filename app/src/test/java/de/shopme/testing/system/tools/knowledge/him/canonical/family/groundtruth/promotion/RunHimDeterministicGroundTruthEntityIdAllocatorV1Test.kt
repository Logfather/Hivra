package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.promotion

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalVariant
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistryEntry
import de.shopme.tools.knowledge.him.canonical.family.HimEntitySourceReferenceType
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateConfidence
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateRelation
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateDatasetPersistenceV2
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.candidate.dataset.HimCandidateIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionTargetV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimDeterministicGroundTruthEntityIdAllocatorContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimDeterministicGroundTruthEntityIdAllocatorV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecision
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RunHimDeterministicGroundTruthEntityIdAllocatorV1Test {

    @Test
    fun `same input is stable across repeated calls and equivalent roots`() {
        val allocator = HimDeterministicGroundTruthEntityIdAllocatorV1()
        val promotion = promotion("a")
        val first = allocator.allocate(promotion, HimEntityType.VARIANT, registry(), authority())
        val replay = allocator.allocate(promotion, HimEntityType.VARIANT, registry(), authority())
        val equivalentRoot = allocator.allocate(promotion, HimEntityType.VARIANT, registry(), authority())

        assertEquals(first, replay)
        assertEquals(first, equivalentRoot)
        assertEquals(6, first.value.length)
        assertTrue(first.value.matches(Regex("[0-9A-Za-z]{6}")))
    }

    @Test
    fun `entity type is allocation-bound`() {
        val allocator = HimDeterministicGroundTruthEntityIdAllocatorV1()
        val promotion = promotion("b")

        val identity = allocator.allocate(promotion, HimEntityType.IDENTITY, registry(), authority())
        val variant = allocator.allocate(promotion, HimEntityType.VARIANT, registry(), authority())
        val alias = allocator.allocate(promotion, HimEntityType.ALIAS, registry(), authority())

        assertNotEquals(identity, variant)
        assertNotEquals(identity, alias)
        assertNotEquals(variant, alias)
    }

    @Test
    fun `primary and multiple collisions probe deterministically`() {
        val allocator = HimDeterministicGroundTruthEntityIdAllocatorV1()
        val promotion = promotion("c")
        val primary = allocator.allocate(promotion, HimEntityType.VARIANT, registry(), authority())
        val firstCollisionRegistry = registryWith(
            occupied(primary, "other-primary"),
        )
        val second = allocator.allocate(promotion, HimEntityType.VARIANT, firstCollisionRegistry, authority())
        val secondCollisionRegistry = registryWith(
            occupied(primary, "other-primary"),
            occupied(second, "other-secondary"),
        )
        val third = allocator.allocate(promotion, HimEntityType.VARIANT, secondCollisionRegistry, authority())
        val replay = allocator.allocate(promotion, HimEntityType.VARIANT, secondCollisionRegistry, authority())

        assertNotEquals(primary, second)
        assertNotEquals(second, third)
        assertEquals(third, replay)
        assertEquals("other-primary", firstCollisionRegistry.entries.single { it.entityId == primary }.sourceReference)
    }

    @Test
    fun `same promotion reuses compatible Ground-Truth provenance`() {
        val allocator = HimDeterministicGroundTruthEntityIdAllocatorV1()
        val promotion = promotion("d")
        val primary = allocator.allocate(promotion, HimEntityType.VARIANT, registry(), authority())
        val compatibleRegistry = registryWith(
            HimEntityIdRegistryEntry(
                entityId = primary,
                entityType = HimEntityType.VARIANT,
                sourceReferenceType = HimEntitySourceReferenceType.GROUND_TRUTH_PROMOTION,
                sourceReference = promotion.value,
            ),
        )

        assertEquals(
            primary,
            allocator.allocate(promotion, HimEntityType.VARIANT, compatibleRegistry, authorityWithVariant(primary)),
        )
    }

    @Test
    fun `occupied Registry and Authority IDs are never returned`() {
        val allocator = HimDeterministicGroundTruthEntityIdAllocatorV1()
        val promotion = promotion("e")
        val primary = allocator.allocate(promotion, HimEntityType.VARIANT, registry(), authority())

        val registryOccupied = allocator.allocate(
            promotion,
            HimEntityType.VARIANT,
            registryWith(occupied(primary, "registry-occupant")),
            authority(),
        )
        val authorityOccupied = allocator.allocate(
            promotion,
            HimEntityType.VARIANT,
            registry(),
            authorityWithVariant(primary),
        )

        assertNotEquals(primary, registryOccupied)
        assertNotEquals(primary, authorityOccupied)
    }

    @Test
    fun `insertion order does not affect allocation`() {
        val allocator = HimDeterministicGroundTruthEntityIdAllocatorV1()
        val promotion = promotion("f")
        val primary = allocator.allocate(promotion, HimEntityType.VARIANT, registry(), authority())
        val second = allocator.allocate(promotion, HimEntityType.VARIANT, registryWith(occupied(primary, "one")), authority())
        val ordered = registryWith(occupied(primary, "one"), occupied(second, "two"))
        val reversed = HimEntityIdRegistry(ordered.entries.reversed())

        assertEquals(
            allocator.allocate(promotion, HimEntityType.VARIANT, ordered, authority()),
            allocator.allocate(promotion, HimEntityType.VARIANT, reversed, authority()),
        )
    }

    @Test
    fun `unsupported canonical type fails and probe exhaustion is finite`() {
        val allocator = HimDeterministicGroundTruthEntityIdAllocatorV1()
        assertFails {
            allocator.allocate(promotion("g"), HimEntityType.CANONICAL, registry(), authority())
        }

        val exhaustionPromotion = promotion("8")
        var occupiedRegistry = registry()
        repeat(HimDeterministicGroundTruthEntityIdAllocatorContractV1.MAX_PROBE_ATTEMPTS) { index ->
            val allocated = allocator.allocate(
                exhaustionPromotion,
                HimEntityType.VARIANT,
                occupiedRegistry,
                authority(),
            )
            occupiedRegistry = HimEntityIdRegistry(
                occupiedRegistry.entries + occupied(allocated, "exhaustion-$index"),
            )
        }

        assertFails {
            allocator.allocate(
                exhaustionPromotion,
                HimEntityType.VARIANT,
                occupiedRegistry,
                authority(),
            )
        }
    }

    @Test
    fun `real Hering promotion reference produces a deterministic non-test ID`() {
        val root = projectRoot()
        val dataset = HimCandidateDatasetPersistenceV2.readDataset(
            root.resolve("data/knowledge/him/candidates/master/candidate-dataset.v2.json"),
        )
        val candidate = dataset.candidates.single { it.candidate.candidateTerm == "Hering eingelegt" }.candidate
        val run = dataset.runs.single { it.runReference.value == REAL_RUN_REFERENCE }
        assertTrue(run.occurrences.any { it.candidate.candidateReference == candidate.candidateReference })
        assertEquals(HimCandidateConfidence.HIGH, candidate.confidence)
        assertEquals(HimCandidateRelation.Variant(HimFamilyEntityReference.Canonical(HERRING_ID)), candidate.relation)

        val persistence = HimCanonicalFamilyPersistence()
        val authorityFile = root.resolve("data/knowledge/him/canonical-family/master/canonical-family-authority.v1.json")
        val registryFile = root.resolve("data/knowledge/him/canonical-family/master/him-entity-id-registry.v1.json")
        val authority = persistence.readAuthority(authorityFile)
        val registry = persistence.readRegistry(registryFile)
        val datasetDigest = HimCandidateIdentityV1.datasetDigest(dataset)
        val validationReference = HimCandidateValidationIdentityV1.validation(
            candidateReference = candidate.candidateReference,
            candidateDatasetDigest = datasetDigest,
            decision = HimCandidateValidationDecision.APPROVE,
            reason = HimCandidateValidationReason.SEMANTICALLY_CORRECT,
            supersededByCandidateReference = null,
        )
        val promotionReference = HimCandidatePromotionIdentityV1.promotion(
            candidateReference = candidate.candidateReference,
            validationReference = validationReference,
            candidateDatasetDigest = datasetDigest,
            authorityDigestBefore = sha256(authorityFile.readBytes()),
            entityIdRegistryDigestBefore = sha256(registryFile.readBytes()),
            target = HimCandidatePromotionTargetV1.AddVariant(
                scope = HimFamilyEntityReference.Canonical(HERRING_ID),
                variantName = candidate.candidateTerm,
            ),
        )
        val allocated = HimDeterministicGroundTruthEntityIdAllocatorV1().allocate(
            promotionReference = promotionReference,
            entityType = HimEntityType.VARIANT,
            registry = registry,
            authority = authority,
        )
        assertNotEquals(HimEntityId("Tst001"), allocated)
        assertTrue(registry.entries.none { it.entityId == allocated })
        assertTrue(authority.families.flatMap { family -> family.variants.map { it.variantId } }.none { it == allocated })
        assertEquals(
            allocated,
            HimDeterministicGroundTruthEntityIdAllocatorV1().allocate(
                promotionReference,
                HimEntityType.VARIANT,
                registry,
                authority,
            ),
        )
    }

    private fun promotion(suffix: String) =
        HimCandidatePromotionReference("promotion:v1:${suffix.repeat(64)}")

    private fun registryWith(vararg entries: HimEntityIdRegistryEntry) =
        HimEntityIdRegistry(registry().entries + entries)

    private fun occupied(entityId: HimEntityId, source: String) =
        HimEntityIdRegistryEntry(
            entityId = entityId,
            entityType = HimEntityType.VARIANT,
            sourceReferenceType = HimEntitySourceReferenceType.GROUND_TRUTH_PROMOTION,
            sourceReference = source,
        )

    private fun registry() =
        HimEntityIdRegistry(
            listOf(
                HimEntityIdRegistryEntry(
                    entityId = HERRING_ID,
                    entityType = HimEntityType.CANONICAL,
                    sourceReferenceType = HimEntitySourceReferenceType.PRODUCT_ONLY_CANONICAL_NORMALIZED,
                    sourceReference = "hering",
                ),
            ),
        )

    private fun authority() =
        HimCanonicalFamilyAuthority(
            schemaVersion = "1",
            sourceCatalog = HimCanonicalFamilySourceCatalog("fixture.json", "a".repeat(64), 1),
            families = listOf(family(HERRING_ID)),
        )

    private fun authorityWithVariant(variantId: HimEntityId) =
        HimCanonicalFamilyAuthority(
            schemaVersion = "1",
            sourceCatalog = HimCanonicalFamilySourceCatalog("fixture.json", "a".repeat(64), 1),
            families = listOf(family(HERRING_ID, variantId)),
        )

    private fun family(canonicalId: HimEntityId, variantId: HimEntityId? = null) =
        HimCanonicalFamily(
            canonicalId = canonicalId,
            canonicalName = if (canonicalId == HERRING_ID) "Hering" else "Existing",
            normalizedName = if (canonicalId == HERRING_ID) "hering" else canonicalId.value.lowercase(),
            taxonomyPaths = emptyList(),
            lifecycleStatus = HimLifecycleStatus.ACTIVE,
            identities = emptyList(),
            variants = listOfNotNull(
                variantId?.let { HimCanonicalVariant(it, "Existing variant", "existing variant", HimLifecycleStatus.ACTIVE) },
            ),
            aliases = emptyList(),
        )

    private fun projectRoot(): File = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).canonicalFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").isFile }

    private fun sha256(bytes: ByteArray) = HimSha256(
        java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) },
    )

    private fun assertFails(block: () -> Unit) {
        assertTrue(runCatching(block).isFailure)
    }

    companion object {
        private val HERRING_ID = HimEntityId("OzlByp")
        private const val REAL_RUN_REFERENCE = "run:v2:c1ab93c23dde76444a3585afd80b592038ecf9c14e39442846ddbaf7671a0823"
    }
}
