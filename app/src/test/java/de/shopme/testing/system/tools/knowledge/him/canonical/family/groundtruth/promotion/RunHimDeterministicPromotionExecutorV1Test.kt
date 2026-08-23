package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth.promotion

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintIndexPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistryEntry
import de.shopme.tools.knowledge.him.canonical.family.HimEntitySourceReferenceType
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCanonicalFamilyMutationLedgerPersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthEntityFingerprintIndexBuilderV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseArtifactReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseBuildInputV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseBuilderV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimRetiredEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimTransactionalGroundTruthPublicationInputV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimTransactionalGroundTruthPublicationV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimActiveGroundTruthResolutionV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCandidateReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionIdentityV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionProposalV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCandidatePromotionTargetV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimDeterministicPromotionExecutorV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidatePromotionEligibilityResultV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidatePromotionEligibilityStatus
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.validation.HimCandidateValidationDecisionReference
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

class RunHimDeterministicPromotionExecutorV1Test {

    @Test
    fun `ADD_VARIANT publishes deterministic Ground-Truth child release`() {
        val root = fixtureRoot()
        try {
            val before = createRelease(root)
            val promotion = promotion(root, "Hering eingelegt")

            val result = executor().execute(
                projectRoot = root,
                promotion = promotion,
                newEntityId = VARIANT_ID,
                eligibility = eligible(promotion),
            )

            assertEquals(before, result.previousReleaseReference)
            assertEquals(HimEntityId("Var001"), result.newEntityId)
            assertEquals(HimEntityType.VARIANT, result.targetType)
            assertTrue(result.createdNewRelease)
            assertTrue(result.activeReleaseVerified)

            val active = HimActiveGroundTruthResolutionV1().resolve(root)
            assertEquals(result.newReleaseReference, active.releaseReference)
            val authority = HimCanonicalFamilyPersistence().readAuthority(active.authorityFile)
            val herring = authority.families.single()
            assertEquals(HERRING_ID, herring.canonicalId)
            assertEquals("Hering", herring.canonicalName)
            assertEquals(1, herring.variants.size)
            assertEquals(VARIANT_ID, herring.variants.single().variantId)
            assertEquals("Hering eingelegt", herring.variants.single().variantName)

            val registry = HimCanonicalFamilyPersistence().readRegistry(active.activeRegistryFile)
            assertEquals(2, registry.entries.size)
            assertEquals(VARIANT_ID, registry.entries.single { it.entityId == VARIANT_ID }.entityId)

            val ledger = HimCanonicalFamilyMutationLedgerPersistenceV1().read(active.mutationLedgerFile)
            assertEquals(1, ledger.entries.size)
            assertEquals(result.mutationReference, ledger.entries.single().mutationReference)
            assertTrue(active.fingerprintIndexFile.readBytes().isNotEmpty())
            assertFalse(root.resolve("data/knowledge/catalog").exists())
            assertFalse(root.resolve("data/knowledge/runtime").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `failure before publication preserves active release`() {
        val root = fixtureRoot()
        try {
            val before = createRelease(root)
            val promotion = promotion(root, "Hering eingelegt")
            val currentBefore = currentPointer(root)

            assertFails {
                executor().execute(
                    projectRoot = root,
                    promotion = promotion,
                    newEntityId = VARIANT_ID,
                    eligibility = HimCandidatePromotionEligibilityResultV1(
                        candidateReference = promotion.candidateReference,
                        status = HimCandidatePromotionEligibilityStatus.BLOCKED_REJECTED,
                        validationReference = promotion.validationReference,
                    ),
                )
            }

            assertArrayEquals(currentBefore, currentPointer(root))
            assertEquals(before, HimActiveGroundTruthResolutionV1().resolve(root).releaseReference)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `CREATE_CANONICAL fails without publication`() {
        val root = fixtureRoot()
        try {
            createRelease(root)
            val promotion = promotion(root, "Neue Familie", createCanonical = true)
            val currentBefore = currentPointer(root)

            assertFails {
                executor().execute(root, promotion, HimEntityId("Can001"), eligible(promotion))
            }

            assertArrayEquals(currentBefore, currentPointer(root))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `duplicate semantic child and duplicate Entity ID fail without publication`() {
        val root = fixtureRoot()
        try {
            createRelease(root)
            val promotion = promotion(root, "Hering eingelegt")
            executor().execute(root, promotion, VARIANT_ID, eligible(promotion))
            val activeAfterFirst = currentPointer(root)

            assertFails {
                executor().execute(root, promotion, HimEntityId("Var002"), eligible(promotion))
            }
            assertArrayEquals(activeAfterFirst, currentPointer(root))

            val secondRoot = fixtureRoot()
            try {
                createRelease(secondRoot)
                val duplicateIdPromotion = promotion(secondRoot, "Andere Variante")
                val secondPointer = currentPointer(secondRoot)
                assertFails {
                    executor().execute(secondRoot, duplicateIdPromotion, HERRING_ID, eligible(duplicateIdPromotion))
                }
                assertArrayEquals(secondPointer, currentPointer(secondRoot))
            } finally {
                secondRoot.deleteRecursively()
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `identical isolated inputs produce identical mutation and release artifacts`() {
        val firstRoot = fixtureRoot()
        val secondRoot = fixtureRoot()
        try {
            createRelease(firstRoot)
            createRelease(secondRoot)
            val firstPromotion = promotion(firstRoot, "Hering eingelegt")
            val secondPromotion = promotion(secondRoot, "Hering eingelegt")

            val first = executor().execute(firstRoot, firstPromotion, VARIANT_ID, eligible(firstPromotion))
            val second = executor().execute(secondRoot, secondPromotion, VARIANT_ID, eligible(secondPromotion))

            assertEquals(first.mutationReference, second.mutationReference)
            assertEquals(first.newReleaseReference, second.newReleaseReference)

            val firstActive = HimActiveGroundTruthResolutionV1().resolve(firstRoot)
            val secondActive = HimActiveGroundTruthResolutionV1().resolve(secondRoot)
            assertArrayEquals(firstActive.authorityBytes, secondActive.authorityBytes)
            assertArrayEquals(firstActive.activeRegistryBytes, secondActive.activeRegistryBytes)
            assertArrayEquals(firstActive.retiredRegistryBytes, secondActive.retiredRegistryBytes)
            assertArrayEquals(firstActive.mutationLedgerBytes, secondActive.mutationLedgerBytes)
            assertArrayEquals(firstActive.fingerprintIndexBytes, secondActive.fingerprintIndexBytes)
            assertArrayEquals(firstActive.releaseBytes, secondActive.releaseBytes)
        } finally {
            firstRoot.deleteRecursively()
            secondRoot.deleteRecursively()
        }
    }

    private fun executor() = HimDeterministicPromotionExecutorV1()

    private fun promotion(
        root: File,
        term: String,
        createCanonical: Boolean = false,
    ): HimCandidatePromotionProposalV1 {
        val resolver = HimActiveGroundTruthResolutionV1()
        val active = resolver.resolve(root)
        val authorityDigest = digest(active.authorityBytes)
        val registryDigest = digest(active.activeRegistryBytes)
        val validation = HimCandidateValidationDecisionReference("validation:v1:${"b".repeat(64)}")
        val candidate = HimCandidateReference("candidate:executor-test")
        val target = if (createCanonical) {
            HimCandidatePromotionTargetV1.CreateCanonical(term)
        } else {
            HimCandidatePromotionTargetV1.AddVariant(
                scope = HimFamilyEntityReference.Canonical(HERRING_ID),
                variantName = term,
            )
        }
        return HimCandidatePromotionProposalV1(
            promotionReference = HimCandidatePromotionIdentityV1.promotion(
                candidateReference = candidate,
                validationReference = validation,
                candidateDatasetDigest = digest("dataset".toByteArray()),
                authorityDigestBefore = authorityDigest,
                entityIdRegistryDigestBefore = registryDigest,
                target = target,
            ),
            candidateReference = candidate,
            validationReference = validation,
            candidateDatasetDigest = digest("dataset".toByteArray()),
            authorityDigestBefore = authorityDigest,
            entityIdRegistryDigestBefore = registryDigest,
            target = target,
        )
    }

    private fun eligible(promotion: HimCandidatePromotionProposalV1) =
        HimCandidatePromotionEligibilityResultV1(
            candidateReference = promotion.candidateReference,
            status = HimCandidatePromotionEligibilityStatus.PROMOTION_ELIGIBLE,
            validationReference = promotion.validationReference,
        )

    private fun createRelease(root: File): HimGroundTruthReleaseIdentityV1 {
        val authority = HimCanonicalFamilyAuthority(
            schemaVersion = "1",
            sourceCatalog = HimCanonicalFamilySourceCatalog("catalog.json", "a".repeat(64), 1),
            families = listOf(
                HimCanonicalFamily(
                    canonicalId = HERRING_ID,
                    canonicalName = "Hering",
                    normalizedName = "hering",
                    taxonomyPaths = emptyList(),
                    lifecycleStatus = HimLifecycleStatus.ACTIVE,
                    identities = emptyList(),
                    variants = emptyList(),
                    aliases = emptyList(),
                ),
            ),
        )
        val registry = HimEntityIdRegistry(
            listOf(
                de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistryEntry(
                    HERRING_ID,
                    HimEntityType.CANONICAL,
                    HimEntitySourceReferenceType.PRODUCT_ONLY_CANONICAL_NORMALIZED,
                    "hering",
                ),
            ),
        )
        val retired = HimRetiredEntityIdRegistry("HIM_RETIRED_ENTITY_ID_REGISTRY_V1", emptyList())
        val ledgerPersistence = HimCanonicalFamilyMutationLedgerPersistenceV1()
        val familyPersistence = HimCanonicalFamilyPersistence()
        val indexPersistence = HimEntityFingerprintIndexPersistence()
        val authorityBytes = familyPersistence.serialize(authority)
        val registryBytes = familyPersistence.serialize(registry)
        val retiredBytes = "{\n  \"schemaVersion\": \"HIM_RETIRED_ENTITY_ID_REGISTRY_V1\",\n  \"entries\": []\n}\n".toByteArray()
        val ledger = ledgerPersistence.emptyLedger()
        val ledgerBytes = ledgerPersistence.serialize(ledger)
        val index = HimGroundTruthEntityFingerprintIndexBuilderV1().build(
            registry = registry,
            authority = authority,
            registrySha256 = digest(registryBytes).value,
            authoritySha256 = digest(authorityBytes).value,
        )
        val indexBytes = indexPersistence.serialize(index)
        val buildInput = HimGroundTruthReleaseBuildInputV1(
            authority = authority,
            activeEntityIdRegistry = registry,
            retiredEntityIdRegistry = retired,
            mutationLedger = ledger,
            entityFingerprintIndex = index,
            authorityArtifact = artifact("canonical-family-authority.v1.json", authorityBytes, 1),
            activeEntityIdRegistryArtifact = artifact("him-entity-id-registry.v1.json", registryBytes, 1),
            retiredEntityIdRegistryArtifact = artifact("retired-entity-id-registry.v1.json", retiredBytes, 0),
            mutationLedgerArtifact = artifact("canonical-family-mutation-ledger.v1.json", ledgerBytes, 0),
            entityFingerprintIndexArtifact = artifact("him-entity-fingerprint-index.v1.json", indexBytes, index.entryCount),
        )
        val release = HimGroundTruthReleaseBuilderV1().build(buildInput)
        val releaseBytes = de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleasePersistenceV1().serialize(release)
        val input = HimTransactionalGroundTruthPublicationInputV1(
            authorityBytes,
            registryBytes,
            retiredBytes,
            ledgerBytes,
            indexBytes,
            releaseBytes,
        )
        val publication = HimTransactionalGroundTruthPublicationV1().publish(root, input) {}
        return publication.releaseReference
    }

    private fun artifact(path: String, bytes: ByteArray, records: Int) =
        HimGroundTruthReleaseArtifactReference(path, digest(bytes), records)

    private fun currentPointer(root: File): ByteArray = root.resolve(
        "data/knowledge/him/canonical-family/groundtruth/current.v1.json",
    ).readBytes()

    private fun digest(bytes: ByteArray) = HimSha256(
        MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) },
    )

    private fun fixtureRoot() = Files.createTempDirectory("him-promotion-executor-v1-").toFile()

    private fun assertFails(block: () -> Unit) {
        assertTrue(runCatching(block).isFailure)
    }

    companion object {
        private val HERRING_ID = HimEntityId("OzlByp")
        private val VARIANT_ID = HimEntityId("Var001")
    }
}
