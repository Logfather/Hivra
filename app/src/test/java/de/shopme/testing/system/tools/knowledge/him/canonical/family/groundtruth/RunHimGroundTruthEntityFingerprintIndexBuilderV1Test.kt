package de.shopme.testing.system.tools.knowledge.him.canonical.family.groundtruth

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalAlias
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyPersistence
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilySourceCatalog
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalIdentity
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalVariant
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintGenerator
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistryEntry
import de.shopme.tools.knowledge.him.canonical.family.HimEntitySourceReferenceType
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus
import de.shopme.tools.knowledge.him.canonical.family.groundtruth.HimGroundTruthEntityFingerprintIndexBuilderV1
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RunHimGroundTruthEntityFingerprintIndexBuilderV1Test {

    @Test
    fun `canonical only authority produces one deterministic canonical scope`() {
        val authority =
            authority(
                family(
                    canonicalId = "OzlByp",
                    canonicalName = "Hering",
                )
            )

        val registry =
            registry(
                registryEntry(
                    id = "OzlByp",
                    type = HimEntityType.CANONICAL,
                )
            )

        val index =
            builder().build(
                registry = registry,
                authority = authority,
                registrySha256 = REGISTRY_SHA,
                authoritySha256 = AUTHORITY_SHA,
            )

        assertEquals(1, index.entryCount)
        assertEquals(1, index.entries.size)

        val entry = index.entries.single()

        assertEquals(
            HimEntityId("OzlByp"),
            entry.canonicalId,
        )
        assertEquals(
            null,
            entry.identityId,
        )
        assertTrue(
            entry.variantIds.isEmpty()
        )

        assertEquals(
            generator.generate(
                canonicalId = HimEntityId("OzlByp"),
                identityId = null,
                variantIds = emptyList(),
            ),
            entry.fingerprint,
        )
    }

    @Test
    fun `canonical scoped variants are included in canonical fingerprint scope`() {
        val authority =
            authority(
                family(
                    canonicalId = "OzlByp",
                    canonicalName = "Hering",
                    variants =
                        listOf(
                            variant(
                                id = "Var002",
                                name = "Hering geräuchert",
                            ),
                            variant(
                                id = "Var001",
                                name = "Hering eingelegt",
                            ),
                        ),
                )
            )

        val registry =
            registry(
                registryEntry(
                    "OzlByp",
                    HimEntityType.CANONICAL,
                ),
                registryEntry(
                    "Var001",
                    HimEntityType.VARIANT,
                ),
                registryEntry(
                    "Var002",
                    HimEntityType.VARIANT,
                ),
            )

        val index =
            builder().build(
                registry = registry,
                authority = authority,
                registrySha256 = REGISTRY_SHA,
                authoritySha256 = AUTHORITY_SHA,
            )

        val entry =
            index.entries.single()

        assertEquals(
            listOf(
                HimEntityId("Var001"),
                HimEntityId("Var002"),
            ),
            entry.variantIds,
        )

        assertEquals(
            generator.generate(
                canonicalId =
                    HimEntityId("OzlByp"),
                identityId =
                    null,
                variantIds =
                    listOf(
                        HimEntityId("Var001"),
                        HimEntityId("Var002"),
                    ),
            ),
            entry.fingerprint,
        )
    }

    @Test
    fun `identity produces separate fingerprint scope`() {
        val authority =
            authority(
                family(
                    canonicalId = "OzlByp",
                    canonicalName = "Hering",
                    identities =
                        listOf(
                            identity(
                                id = "Id0001",
                                name = "Bismarckhering",
                            )
                        ),
                )
            )

        val registry =
            registry(
                registryEntry(
                    "OzlByp",
                    HimEntityType.CANONICAL,
                ),
                registryEntry(
                    "Id0001",
                    HimEntityType.IDENTITY,
                ),
            )

        val index =
            builder().build(
                registry = registry,
                authority = authority,
                registrySha256 = REGISTRY_SHA,
                authoritySha256 = AUTHORITY_SHA,
            )

        assertEquals(
            2,
            index.entryCount,
        )

        val canonicalEntry =
            index.entries.single {
                it.identityId == null
            }

        val identityEntry =
            index.entries.single {
                it.identityId ==
                        HimEntityId("Id0001")
            }

        assertEquals(
            HimEntityId("OzlByp"),
            canonicalEntry.canonicalId,
        )

        assertEquals(
            HimEntityId("OzlByp"),
            identityEntry.canonicalId,
        )

        assertTrue(
            canonicalEntry.variantIds.isEmpty()
        )

        assertTrue(
            identityEntry.variantIds.isEmpty()
        )
    }

    @Test
    fun `identity scoped variants belong only to identity fingerprint scope`() {
        val authority =
            authority(
                family(
                    canonicalId = "OzlByp",
                    canonicalName = "Hering",
                    variants =
                        listOf(
                            variant(
                                id = "Var001",
                                name = "Hering eingelegt",
                            )
                        ),
                    identities =
                        listOf(
                            identity(
                                id = "Id0001",
                                name = "Bismarckhering",
                                variants =
                                    listOf(
                                        variant(
                                            id = "Var002",
                                            name = "Bismarckhering mild",
                                        ),
                                        variant(
                                            id = "Var003",
                                            name = "Bismarckhering scharf",
                                        ),
                                    ),
                            )
                        ),
                )
            )

        val registry =
            registry(
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
                    "Var003",
                    HimEntityType.VARIANT,
                ),
            )

        val index =
            builder().build(
                registry = registry,
                authority = authority,
                registrySha256 = REGISTRY_SHA,
                authoritySha256 = AUTHORITY_SHA,
            )

        val canonicalEntry =
            index.entries.single {
                it.identityId == null
            }

        val identityEntry =
            index.entries.single {
                it.identityId ==
                        HimEntityId("Id0001")
            }

        assertEquals(
            listOf(
                HimEntityId("Var001")
            ),
            canonicalEntry.variantIds,
        )

        assertEquals(
            listOf(
                HimEntityId("Var002"),
                HimEntityId("Var003"),
            ),
            identityEntry.variantIds,
        )
    }

    @Test
    fun `aliases do not change structural fingerprints`() {
        val withoutAliases =
            authority(
                family(
                    canonicalId = "OzlByp",
                    canonicalName = "Hering",
                    identities =
                        listOf(
                            identity(
                                id = "Id0001",
                                name = "Bismarckhering",
                            )
                        ),
                )
            )

        val withAliases =
            authority(
                family(
                    canonicalId = "OzlByp",
                    canonicalName = "Hering",
                    identities =
                        listOf(
                            identity(
                                id = "Id0001",
                                name = "Bismarckhering",
                                aliases =
                                    listOf(
                                        alias(
                                            id = "Als002",
                                            name = "Bismarck-Hering",
                                        )
                                    ),
                            )
                        ),
                    aliases =
                        listOf(
                            alias(
                                id = "Als001",
                                name = "Atlantischer Hering",
                            )
                        ),
                )
            )

        val registryWithoutAliases =
            registry(
                registryEntry(
                    "OzlByp",
                    HimEntityType.CANONICAL,
                ),
                registryEntry(
                    "Id0001",
                    HimEntityType.IDENTITY,
                ),
            )

        val registryWithAliases =
            registry(
                registryEntry(
                    "OzlByp",
                    HimEntityType.CANONICAL,
                ),
                registryEntry(
                    "Id0001",
                    HimEntityType.IDENTITY,
                ),
                registryEntry(
                    "Als001",
                    HimEntityType.ALIAS,
                ),
                registryEntry(
                    "Als002",
                    HimEntityType.ALIAS,
                ),
            )

        val first =
            builder().build(
                registry = registryWithoutAliases,
                authority = withoutAliases,
                registrySha256 = "1".repeat(64),
                authoritySha256 = "2".repeat(64),
            )

        val second =
            builder().build(
                registry = registryWithAliases,
                authority = withAliases,
                registrySha256 = "3".repeat(64),
                authoritySha256 = "4".repeat(64),
            )

        assertEquals(
            first.entries,
            second.entries,
        )

        assertNotEquals(
            first.sourceAuthorities,
            second.sourceAuthorities,
        )
    }

    @Test
    fun `builder orders canonical identity and variant scopes deterministically`() {
        val authority =
            authority(
                family(
                    canonicalId = "Zzz999",
                    canonicalName = "Second",
                    identities =
                        listOf(
                            identity(
                                id = "Id0003",
                                name = "Identity Three",
                            )
                        ),
                ),
                family(
                    canonicalId = "Aaa111",
                    canonicalName = "First",
                    variants =
                        listOf(
                            variant(
                                id = "Var009",
                                name = "Variant Nine",
                            ),
                            variant(
                                id = "Var001",
                                name = "Variant One",
                            ),
                        ),
                    identities =
                        listOf(
                            identity(
                                id = "Id0002",
                                name = "Identity Two",
                            ),
                            identity(
                                id = "Id0001",
                                name = "Identity One",
                            ),
                        ),
                ),
            )

        val registry =
            registryFromAuthority(
                authority
            )

        val index =
            builder().build(
                registry = registry,
                authority = authority,
                registrySha256 = REGISTRY_SHA,
                authoritySha256 = AUTHORITY_SHA,
            )

        assertEquals(
            listOf(
                "Aaa111|null|Var001,Var009",
                "Aaa111|Id0001|",
                "Aaa111|Id0002|",
                "Zzz999|null|",
                "Zzz999|Id0003|",
            ),
            index.entries.map {
                "${it.canonicalId.value}|" +
                        "${it.identityId?.value ?: "null"}|" +
                        it.variantIds.joinToString(",") { id ->
                            id.value
                        }
            },
        )
    }

    @Test
    fun `identical input produces equal and byte stable index`() {
        val authority =
            authority(
                family(
                    canonicalId = "OzlByp",
                    canonicalName = "Hering",
                    variants =
                        listOf(
                            variant(
                                id = "Var001",
                                name = "Hering eingelegt",
                            )
                        ),
                    identities =
                        listOf(
                            identity(
                                id = "Id0001",
                                name = "Bismarckhering",
                                variants =
                                    listOf(
                                        variant(
                                            id = "Var002",
                                            name = "Bismarckhering mild",
                                        )
                                    ),
                            )
                        ),
                )
            )

        val registry =
            registryFromAuthority(
                authority
            )

        val first =
            builder().build(
                registry = registry,
                authority = authority,
                registrySha256 = REGISTRY_SHA,
                authoritySha256 = AUTHORITY_SHA,
            )

        val second =
            builder().build(
                registry = registry,
                authority = authority,
                registrySha256 = REGISTRY_SHA,
                authoritySha256 = AUTHORITY_SHA,
            )

        assertEquals(
            first,
            second,
        )

        val persistence =
            HimCanonicalFamilyPersistence()

        assertArrayEquals(
            persistence.serialize(first),
            persistence.serialize(second),
        )
    }

    @Test
    fun `registry authority type mismatch fails closed`() {
        val authority =
            authority(
                family(
                    canonicalId = "OzlByp",
                    canonicalName = "Hering",
                    variants =
                        listOf(
                            variant(
                                id = "Var001",
                                name = "Hering eingelegt",
                            )
                        ),
                )
            )

        val registry =
            registry(
                registryEntry(
                    "OzlByp",
                    HimEntityType.CANONICAL,
                ),
                registryEntry(
                    "Var001",
                    HimEntityType.IDENTITY,
                ),
            )

        val failure =
            runCatching {
                builder().build(
                    registry = registry,
                    authority = authority,
                    registrySha256 = REGISTRY_SHA,
                    authoritySha256 = AUTHORITY_SHA,
                )
            }.exceptionOrNull()

        assertTrue(
            failure is IllegalArgumentException
        )
    }

    @Test
    fun `registry entity missing from authority fails closed`() {
        val authority =
            authority(
                family(
                    canonicalId = "OzlByp",
                    canonicalName = "Hering",
                )
            )

        val registry =
            registry(
                registryEntry(
                    "OzlByp",
                    HimEntityType.CANONICAL,
                ),
                registryEntry(
                    "Var001",
                    HimEntityType.VARIANT,
                ),
            )

        val failure =
            runCatching {
                builder().build(
                    registry = registry,
                    authority = authority,
                    registrySha256 = REGISTRY_SHA,
                    authoritySha256 = AUTHORITY_SHA,
                )
            }.exceptionOrNull()

        assertTrue(
            failure is IllegalArgumentException
        )
    }

    @Test
    fun `authority entity missing from registry fails closed`() {
        val authority =
            authority(
                family(
                    canonicalId = "OzlByp",
                    canonicalName = "Hering",
                    variants =
                        listOf(
                            variant(
                                id = "Var001",
                                name = "Hering eingelegt",
                            )
                        ),
                )
            )

        val registry =
            registry(
                registryEntry(
                    "OzlByp",
                    HimEntityType.CANONICAL,
                )
            )

        val failure =
            runCatching {
                builder().build(
                    registry = registry,
                    authority = authority,
                    registrySha256 = REGISTRY_SHA,
                    authoritySha256 = AUTHORITY_SHA,
                )
            }.exceptionOrNull()

        assertTrue(
            failure is IllegalArgumentException
        )
    }

    @Test
    fun `duplicate entity id inside authority fails closed`() {
        val duplicate =
            HimEntityId("Dup001")

        val authority =
            authority(
                family(
                    canonicalId = "OzlByp",
                    canonicalName = "Hering",
                    variants =
                        listOf(
                            variant(
                                id = duplicate.value,
                                name = "Variant",
                            )
                        ),
                    aliases =
                        listOf(
                            alias(
                                id = duplicate.value,
                                name = "Alias",
                            )
                        ),
                )
            )

        val registry =
            registry(
                registryEntry(
                    "OzlByp",
                    HimEntityType.CANONICAL,
                ),
                registryEntry(
                    duplicate.value,
                    HimEntityType.VARIANT,
                ),
            )

        val failure =
            runCatching {
                builder().build(
                    registry = registry,
                    authority = authority,
                    registrySha256 = REGISTRY_SHA,
                    authoritySha256 = AUTHORITY_SHA,
                )
            }.exceptionOrNull()

        assertTrue(
            failure is IllegalArgumentException
        )
    }

    private val generator =
        HimEntityFingerprintGenerator()

    private fun builder() =
        HimGroundTruthEntityFingerprintIndexBuilderV1(
            generator =
                generator
        )

    private fun authority(
        vararg families: HimCanonicalFamily,
    ) =
        HimCanonicalFamilyAuthority(
            schemaVersion =
                "1",
            sourceCatalog =
                HimCanonicalFamilySourceCatalog(
                    path =
                        "diagnostic-canonical-catalog.json",
                    contentSha256 =
                        "f".repeat(64),
                    recordCount =
                        families.size,
                ),
            families =
                families.toList(),
        )

    private fun family(
        canonicalId: String,
        canonicalName: String,
        identities: List<HimCanonicalIdentity> =
            emptyList(),
        variants: List<HimCanonicalVariant> =
            emptyList(),
        aliases: List<HimCanonicalAlias> =
            emptyList(),
    ) =
        HimCanonicalFamily(
            canonicalId =
                HimEntityId(canonicalId),
            canonicalName =
                canonicalName,
            normalizedName =
                canonicalName.lowercase(),
            taxonomyPaths =
                emptyList(),
            lifecycleStatus =
                HimLifecycleStatus.ACTIVE,
            identities =
                identities,
            variants =
                variants,
            aliases =
                aliases,
        )

    private fun identity(
        id: String,
        name: String,
        variants: List<HimCanonicalVariant> =
            emptyList(),
        aliases: List<HimCanonicalAlias> =
            emptyList(),
    ) =
        HimCanonicalIdentity(
            identityId =
                HimEntityId(id),
            identityName =
                name,
            normalizedName =
                name.lowercase(),
            lifecycleStatus =
                HimLifecycleStatus.ACTIVE,
            variants =
                variants,
            aliases =
                aliases,
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

    private fun registry(
        vararg entries: HimEntityIdRegistryEntry,
    ) =
        HimEntityIdRegistry(
            entries =
                entries.toList(),
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

    private fun registryFromAuthority(
        authority: HimCanonicalFamilyAuthority,
    ): HimEntityIdRegistry {
        val entries =
            buildList {
                authority.families.forEach { family ->
                    add(
                        registryEntry(
                            family.canonicalId.value,
                            HimEntityType.CANONICAL,
                        )
                    )

                    family.identities.forEach { identity ->
                        add(
                            registryEntry(
                                identity.identityId.value,
                                HimEntityType.IDENTITY,
                            )
                        )

                        identity.variants.forEach { variant ->
                            add(
                                registryEntry(
                                    variant.variantId.value,
                                    HimEntityType.VARIANT,
                                )
                            )
                        }

                        identity.aliases.forEach { alias ->
                            add(
                                registryEntry(
                                    alias.aliasId.value,
                                    HimEntityType.ALIAS,
                                )
                            )
                        }
                    }

                    family.variants.forEach { variant ->
                        add(
                            registryEntry(
                                variant.variantId.value,
                                HimEntityType.VARIANT,
                            )
                        )
                    }

                    family.aliases.forEach { alias ->
                        add(
                            registryEntry(
                                alias.aliasId.value,
                                HimEntityType.ALIAS,
                            )
                        )
                    }
                }
            }

        return HimEntityIdRegistry(
            entries =
                entries,
        )
    }

    companion object {
        private const val REGISTRY_SHA =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"

        private const val AUTHORITY_SHA =
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
    }
}