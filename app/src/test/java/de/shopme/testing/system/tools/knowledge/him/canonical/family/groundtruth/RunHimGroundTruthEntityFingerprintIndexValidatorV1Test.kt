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
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthEntityFingerprintIndexBuilderV1
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthEntityFingerprintIndexValidatorV1
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunHimGroundTruthEntityFingerprintIndexValidatorV1Test {

    @Test
    fun `dynamic ground truth fingerprint index validates canonical and identity scopes`() {
        val authority =
            authority()

        val registry =
            registry()

        val builder =
            HimGroundTruthEntityFingerprintIndexBuilderV1()

        val index =
            builder.build(
                registry =
                    registry,
                authority =
                    authority,
                registrySha256 =
                    REGISTRY_SHA,
                authoritySha256 =
                    AUTHORITY_SHA,
            )

        val result =
            HimGroundTruthEntityFingerprintIndexValidatorV1()
                .validate(
                    registry =
                        registry,
                    authority =
                        authority,
                    index =
                        index,
                    registrySha256 =
                        REGISTRY_SHA,
                    authoritySha256 =
                        AUTHORITY_SHA,
                )

        assertEquals(
            3,
            result.entries,
        )

        assertEquals(
            1,
            result.canonicalScopes,
        )

        assertEquals(
            2,
            result.identityScopes,
        )

        assertEquals(
            3,
            result.variantReferences,
        )

        assertEquals(
            3,
            result.uniqueFingerprints,
        )

        assertEquals(
            0,
            result.duplicateFingerprintGroups,
        )

        assertEquals(
            0,
            result.invalidFingerprints,
        )

        assertEquals(
            0,
            result.registryAuthorityMismatches,
        )

        assertEquals(
            0,
            result.missingExpectedScopes,
        )

        assertEquals(
            0,
            result.unexpectedScopes,
        )

        assertEquals(
            0,
            result.fingerprintRecomputationMismatches,
        )
    }

    @Test
    fun `aliases do not alter structural fingerprint scopes`() {
        val authority =
            authority()

        val registry =
            registry()

        val builder =
            HimGroundTruthEntityFingerprintIndexBuilderV1()

        val first =
            builder.build(
                registry =
                    registry,
                authority =
                    authority,
                registrySha256 =
                    REGISTRY_SHA,
                authoritySha256 =
                    AUTHORITY_SHA,
            )

        val withoutAliases =
            authority.copy(
                families =
                    authority.families.map { family ->
                        family.copy(
                            aliases =
                                emptyList(),
                            identities =
                                family.identities.map {
                                    it.copy(
                                        aliases =
                                            emptyList()
                                    )
                                },
                        )
                    },
            )

        val registryWithoutAliases =
            registry.copy(
                entries =
                    registry.entries.filter {
                        it.entityType !=
                                HimEntityType.ALIAS
                    },
            )

        val second =
            builder.build(
                registry =
                    registryWithoutAliases,
                authority =
                    withoutAliases,
                registrySha256 =
                    "c".repeat(64),
                authoritySha256 =
                    "d".repeat(64),
            )

        assertEquals(
            first.entries,
            second.entries,
        )
    }

    @Test
    fun `fingerprint tampering is rejected`() {
        val authority =
            authority()

        val registry =
            registry()

        val builder =
            HimGroundTruthEntityFingerprintIndexBuilderV1()

        val index =
            builder.build(
                registry =
                    registry,
                authority =
                    authority,
                registrySha256 =
                    REGISTRY_SHA,
                authoritySha256 =
                    AUTHORITY_SHA,
            )

        val corrupted =
            index.copy(
                entries =
                    index.entries.mapIndexed { indexValue, entry ->
                        if (indexValue == 0) {
                            entry.copy(
                                fingerprint =
                                    "0".repeat(64)
                            )
                        } else {
                            entry
                        }
                    },
            )

        val failure =
            runCatching {
                HimGroundTruthEntityFingerprintIndexValidatorV1()
                    .validate(
                        registry =
                            registry,
                        authority =
                            authority,
                        index =
                            corrupted,
                        registrySha256 =
                            REGISTRY_SHA,
                        authoritySha256 =
                            AUTHORITY_SHA,
                    )
            }.exceptionOrNull()

        assertTrue(
            failure is IllegalArgumentException
        )
    }

    @Test
    fun `missing structural scope is rejected`() {
        val authority =
            authority()

        val registry =
            registry()

        val builder =
            HimGroundTruthEntityFingerprintIndexBuilderV1()

        val index =
            builder.build(
                registry =
                    registry,
                authority =
                    authority,
                registrySha256 =
                    REGISTRY_SHA,
                authoritySha256 =
                    AUTHORITY_SHA,
            )

        val corruptedEntries =
            index.entries.dropLast(1)

        val corrupted =
            index.copy(
                entryCount =
                    corruptedEntries.size,
                entries =
                    corruptedEntries,
            )

        val failure =
            runCatching {
                HimGroundTruthEntityFingerprintIndexValidatorV1()
                    .validate(
                        registry =
                            registry,
                        authority =
                            authority,
                        index =
                            corrupted,
                        registrySha256 =
                            REGISTRY_SHA,
                        authoritySha256 =
                            AUTHORITY_SHA,
                    )
            }.exceptionOrNull()

        assertTrue(
            failure is IllegalArgumentException
        )
    }

    @Test
    fun `registry authority mismatch is rejected`() {
        val authority =
            authority()

        val registry =
            registry()

        val index =
            HimGroundTruthEntityFingerprintIndexBuilderV1()
                .build(
                    registry =
                        registry,
                    authority =
                        authority,
                    registrySha256 =
                        REGISTRY_SHA,
                    authoritySha256 =
                        AUTHORITY_SHA,
                )

        val invalidRegistry =
            registry.copy(
                entries =
                    registry.entries.filterNot {
                        it.entityId ==
                                HimEntityId("Var001")
                    },
            )

        val failure =
            runCatching {
                HimGroundTruthEntityFingerprintIndexValidatorV1()
                    .validate(
                        registry =
                            invalidRegistry,
                        authority =
                            authority,
                        index =
                            index,
                        registrySha256 =
                            REGISTRY_SHA,
                        authoritySha256 =
                            AUTHORITY_SHA,
                    )
            }.exceptionOrNull()

        assertTrue(
            failure is IllegalArgumentException
        )
    }

    private fun authority() =
        HimCanonicalFamilyAuthority(
            schemaVersion =
                "1",
            sourceCatalog =
                HimCanonicalFamilySourceCatalog(
                    path =
                        "diagnostic",
                    contentSha256 =
                        "a".repeat(64),
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
                                        "Identity One",
                                    normalizedName =
                                        "identity one",
                                    lifecycleStatus =
                                        HimLifecycleStatus.ACTIVE,
                                    variants =
                                        listOf(
                                            variant(
                                                id =
                                                    "Var002",
                                                name =
                                                    "Identity Variant Two",
                                            )
                                        ),
                                    aliases =
                                        listOf(
                                            alias(
                                                id =
                                                    "Als002",
                                                name =
                                                    "Identity Alias",
                                            )
                                        ),
                                ),
                                HimCanonicalIdentity(
                                    identityId =
                                        HimEntityId("Id0002"),
                                    identityName =
                                        "Identity Two",
                                    normalizedName =
                                        "identity two",
                                    lifecycleStatus =
                                        HimLifecycleStatus.ACTIVE,
                                    variants =
                                        listOf(
                                            variant(
                                                id =
                                                    "Var003",
                                                name =
                                                    "Identity Variant Three",
                                            )
                                        ),
                                    aliases =
                                        emptyList(),
                                ),
                            ),
                        variants =
                            listOf(
                                variant(
                                    id =
                                        "Var001",
                                    name =
                                        "Hering eingelegt",
                                )
                            ),
                        aliases =
                            listOf(
                                alias(
                                    id =
                                        "Als001",
                                    name =
                                        "Canonical Alias",
                                )
                            ),
                    )
                ),
        )

    private fun registry() =
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
                        "Id0002",
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
                        "Var003",
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

    companion object {
        private val REGISTRY_SHA =
            "a".repeat(64)

        private val AUTHORITY_SHA =
            "b".repeat(64)
    }
}