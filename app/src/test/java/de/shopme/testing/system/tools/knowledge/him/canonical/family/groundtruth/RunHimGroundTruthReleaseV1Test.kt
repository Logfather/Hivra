package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalAlias
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalIdentity
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalVariant
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistryEntry
import de.shopme.tools.knowledge.him.canonical.family.HimEntitySourceReferenceType
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCanonicalFamilyMutationLedger
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCanonicalFamilyMutationLedgerContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimCanonicalFamilyMutationLedgerEntry
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimFamilyEntityReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthEntityFingerprintIndexBuilderV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthMutationType
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthRelease
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseArtifactReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseBuildInputV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseBuilderV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseContractV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseCounts
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleasePersistenceV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseSources
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseState
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthReleaseValidatorV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimMutationReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimRetiredEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimRetiredEntityIdRegistryEntry
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimSha256
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimValidationReference
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.promotion.HimCanonicalFamilyChildMutationContractV1
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class RunHimGroundTruthReleaseV1Test {

    @Test
    fun `ground truth release binds all artifacts and counts deterministically`() {
        val input =
            input()

        val first =
            HimGroundTruthReleaseBuilderV1()
                .build(input)

        val second =
            HimGroundTruthReleaseBuilderV1()
                .build(input)

        assertEquals(
            first,
            second,
        )

        assertEquals(
            HimGroundTruthReleaseContractV1.RELEASE_VERSION,
            first.releaseVersion,
        )

        assertEquals(
            HimGroundTruthReleaseState.RELEASED,
            first.state,
        )

        assertEquals(
            1,
            first.counts.canonicals,
        )

        assertEquals(
            1,
            first.counts.identities,
        )

        assertEquals(
            2,
            first.counts.variants,
        )

        assertEquals(
            2,
            first.counts.aliases,
        )

        assertEquals(
            6,
            first.counts.activeEntityIds,
        )

        assertEquals(
            1,
            first.counts.retiredEntityIds,
        )

        assertEquals(
            1,
            first.counts.mutations,
        )
    }

    @Test
    fun `release validates fingerprint authority bindings`() {
        val input =
            input()

        val release =
            HimGroundTruthReleaseBuilderV1()
                .build(input)

        val invalidIndex =
            input.entityFingerprintIndex.copy(
                sourceAuthorities =
                    input.entityFingerprintIndex
                        .sourceAuthorities
                        .copy(
                            canonicalFamilyAuthoritySha256 =
                                "f".repeat(64)
                        )
            )

        val invalidInput =
            input.copy(
                entityFingerprintIndex =
                    invalidIndex
            )

        val failure =
            runCatching {
                HimGroundTruthReleaseValidatorV1()
                    .validate(
                        release =
                            release,
                        input =
                            invalidInput,
                    )
            }.exceptionOrNull()

        assertTrue(
            failure is IllegalArgumentException
        )
    }

    @Test
    fun `active and retired entity ids may not overlap`() {
        val input =
            input()

        val duplicateRetired =
            input.retiredEntityIdRegistry.copy(
                entries =
                    listOf(
                        HimRetiredEntityIdRegistryEntry(
                            entityId =
                                HimEntityId("Var001"),
                            entityType =
                                HimEntityType.VARIANT,
                            retirementMutationReference =
                                mutationReference("9"),
                        )
                    )
            )

        val artifact =
            artifact(
                path =
                    "retired.json",
                sha =
                    "e",
                records =
                    1,
            )

        val invalidInput =
            input.copy(
                retiredEntityIdRegistry =
                    duplicateRetired,
                retiredEntityIdRegistryArtifact =
                    artifact,
            )

        val release =
            HimGroundTruthRelease(
                releaseVersion =
                    HimGroundTruthReleaseContractV1.RELEASE_VERSION,
                sources =
                    HimGroundTruthReleaseSources(
                        canonicalFamilyAuthority =
                            invalidInput.authorityArtifact,
                        activeEntityIdRegistry =
                            invalidInput.activeEntityIdRegistryArtifact,
                        retiredEntityIdRegistry =
                            invalidInput.retiredEntityIdRegistryArtifact,
                        mutationLedger =
                            invalidInput.mutationLedgerArtifact,
                        entityFingerprintIndex =
                            invalidInput.entityFingerprintIndexArtifact,
                    ),
                counts =
                    HimGroundTruthReleaseCounts(
                        canonicals = 1,
                        identities = 1,
                        variants = 2,
                        aliases = 2,
                        activeEntityIds = 6,
                        retiredEntityIds = 1,
                        mutations = 1,
                    ),
                state =
                    HimGroundTruthReleaseState.RELEASED,
            )

        val failure =
            runCatching {
                HimGroundTruthReleaseValidatorV1()
                    .validate(
                        release =
                            release,
                        input =
                            invalidInput,
                    )
            }.exceptionOrNull()

        assertTrue(
            failure is IllegalArgumentException
        )
    }

    @Test
    fun `release persistence reloads byte stably`() {
        val input =
            input()

        val release =
            HimGroundTruthReleaseBuilderV1()
                .build(input)

        val persistence =
            HimGroundTruthReleasePersistenceV1()

        val bytes =
            persistence.serialize(release)

        val temporary =
            Files.createTempDirectory(
                "him-ground-truth-release-v1-"
            ).toFile()

        try {
            val file =
                temporary.resolve(
                    "ground-truth-release.v1.json"
                )

            persistence.write(
                file =
                    file,
                release =
                    release,
            )

            assertArrayEquals(
                bytes,
                file.readBytes(),
            )

            val reloaded =
                persistence.read(file)

            assertEquals(
                release,
                reloaded,
            )

            assertArrayEquals(
                bytes,
                persistence.serialize(reloaded),
            )
        } finally {
            temporary.deleteRecursively()
        }
    }

    private fun input():
            HimGroundTruthReleaseBuildInputV1 {
        val authority =
            authority()

        val activeRegistry =
            activeRegistry()

        val retiredRegistry =
            HimRetiredEntityIdRegistry(
                schemaVersion =
                    "HIM_RETIRED_ENTITY_ID_REGISTRY_V1",
                entries =
                    listOf(
                        HimRetiredEntityIdRegistryEntry(
                            entityId =
                                HimEntityId("Ret001"),
                            entityType =
                                HimEntityType.VARIANT,
                            retirementMutationReference =
                                mutationReference("7"),
                        )
                    ),
            )

        val ledger =
            HimCanonicalFamilyMutationLedger(
                schemaVersion =
                    HimCanonicalFamilyMutationLedgerContractV1.SCHEMA_VERSION,
                entries =
                    listOf(
                        mutationEntry()
                    ),
            )

        val authorityArtifact =
            artifact(
                path =
                    "authority.json",
                sha =
                    "a",
                records =
                    authority.families.size,
            )

        val registryArtifact =
            artifact(
                path =
                    "registry.json",
                sha =
                    "b",
                records =
                    activeRegistry.entries.size,
            )

        val retiredArtifact =
            artifact(
                path =
                    "retired.json",
                sha =
                    "c",
                records =
                    retiredRegistry.entries.size,
            )

        val ledgerArtifact =
            artifact(
                path =
                    "mutation-ledger.json",
                sha =
                    "d",
                records =
                    ledger.entries.size,
            )

        val index =
            HimGroundTruthEntityFingerprintIndexBuilderV1()
                .build(
                    registry =
                        activeRegistry,
                    authority =
                        authority,
                    registrySha256 =
                        registryArtifact.sha256.value,
                    authoritySha256 =
                        authorityArtifact.sha256.value,
                )

        val indexArtifact =
            artifact(
                path =
                    "fingerprint-index.json",
                sha =
                    "e",
                records =
                    index.entryCount,
            )

        return HimGroundTruthReleaseBuildInputV1(
            authority =
                authority,
            activeEntityIdRegistry =
                activeRegistry,
            retiredEntityIdRegistry =
                retiredRegistry,
            mutationLedger =
                ledger,
            entityFingerprintIndex =
                index,
            authorityArtifact =
                authorityArtifact,
            activeEntityIdRegistryArtifact =
                registryArtifact,
            retiredEntityIdRegistryArtifact =
                retiredArtifact,
            mutationLedgerArtifact =
                ledgerArtifact,
            entityFingerprintIndexArtifact =
                indexArtifact,
        )
    }

    private fun authority() =
        HimCanonicalFamilyAuthority(
            schemaVersion =
                "1",
            sourceCatalog =
                HimCanonicalFamilySourceCatalog(
                    path =
                        "canonical-catalog.json",
                    contentSha256 =
                        "f".repeat(64),
                    recordCount =
                        1,
                ),
            families =
                listOf(
                    HimCanonicalFamily(
                        canonicalId =
                            HimEntityId("OzlByp"),
                        canonicalName =
                            "Hering",
                        normalizedName =
                            "hering",
                        taxonomyPaths =
                            emptyList(),
                        lifecycleStatus =
                            HimLifecycleStatus.ACTIVE,
                        identities =
                            listOf(
                                HimCanonicalIdentity(
                                    identityId =
                                        HimEntityId("Id0001"),
                                    identityName =
                                        "Bismarckhering",
                                    normalizedName =
                                        "bismarckhering",
                                    lifecycleStatus =
                                        HimLifecycleStatus.ACTIVE,
                                    variants =
                                        listOf(
                                            variant(
                                                "Var002",
                                                "Bismarckhering mild",
                                            )
                                        ),
                                    aliases =
                                        listOf(
                                            alias(
                                                "Als002",
                                                "Bismarck-Hering",
                                            )
                                        ),
                                )
                            ),
                        variants =
                            listOf(
                                variant(
                                    "Var001",
                                    "Hering eingelegt",
                                )
                            ),
                        aliases =
                            listOf(
                                alias(
                                    "Als001",
                                    "Atlantischer Hering",
                                )
                            ),
                    )
                ),
        )

    private fun activeRegistry() =
        HimEntityIdRegistry(
            entries =
                listOf(
                    registryEntry(
                        "OzlByp",
                        HimEntityType.CANONICAL,
                    ),
                    registryEntry(
                        "Id0001",
                        HimEntityType.IDENTITY,
                    ),
                    registryEntry(
                        "Var001",
                        HimEntityType.VARIANT,
                    ),
                    registryEntry(
                        "Var002",
                        HimEntityType.VARIANT,
                    ),
                    registryEntry(
                        "Als001",
                        HimEntityType.ALIAS,
                    ),
                    registryEntry(
                        "Als002",
                        HimEntityType.ALIAS,
                    ),
                ),
        )

    private fun mutationEntry() =
        HimCanonicalFamilyMutationLedgerEntry(
            mutationReference =
                mutationReference("1"),
            validationReference =
                HimValidationReference(
                    "validation:v1:" +
                            "2".repeat(64)
                ),
            mutationType =
                HimGroundTruthMutationType.ADD_VARIANT,
            entityType =
                HimEntityType.VARIANT,
            newEntityId =
                HimEntityId("Var001"),
            parent =
                HimFamilyEntityReference.Canonical(
                    canonicalId =
                        HimEntityId("OzlByp")
                ),
            canonicalFamilyAuthoritySha256Before =
                HimSha256("1".repeat(64)),
            canonicalFamilyAuthoritySha256After =
                HimSha256("a".repeat(64)),
            entityIdRegistrySha256Before =
                HimSha256("2".repeat(64)),
            entityIdRegistrySha256After =
                HimSha256("b".repeat(64)),
            contractVersion =
                HimCanonicalFamilyChildMutationContractV1.VERSION,
        )

    private fun mutationReference(
        digit: String,
    ) =
        HimMutationReference(
            "mutation:v1:" +
                    digit.repeat(64)
        )

    private fun artifact(
        path: String,
        sha: String,
        records: Int,
    ) =
        HimGroundTruthReleaseArtifactReference(
            path =
                path,
            sha256 =
                HimSha256(
                    sha.repeat(64)
                ),
            recordCount =
                records,
        )

    private fun registryEntry(
        id: String,
        type: HimEntityType,
    ) =
        HimEntityIdRegistryEntry(
            entityId =
                HimEntityId(id),
            entityType =
                type,
            sourceReferenceType =
                if (
                    type ==
                    HimEntityType.CANONICAL
                ) {
                    HimEntitySourceReferenceType
                        .PRODUCT_ONLY_CANONICAL_NORMALIZED
                } else {
                    HimEntitySourceReferenceType
                        .GROUND_TRUTH_PROMOTION
                },
            sourceReference =
                "diagnostic:$id",
        )

    private fun variant(
        id: String,
        name: String,
    ) =
        HimCanonicalVariant(
            variantId =
                HimEntityId(id),
            variantName =
                name,
            normalizedName =
                name.lowercase(),
            lifecycleStatus =
                HimLifecycleStatus.ACTIVE,
        )

    private fun alias(
        id: String,
        name: String,
    ) =
        HimCanonicalAlias(
            aliasId =
                HimEntityId(id),
            aliasName =
                name,
            normalizedName =
                name.lowercase(),
            lifecycleStatus =
                HimLifecycleStatus.ACTIVE,
        )
}