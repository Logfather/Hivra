package de.shopme.tools.knowledge.him.canonical.family.groundtruth

import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamily
import de.shopme.tools.knowledge.him.canonical.family.HimCanonicalFamilyAuthority
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintEntry
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintGenerator
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintIndex
import de.shopme.tools.knowledge.him.canonical.family.HimEntityFingerprintIndexSources
import de.shopme.tools.knowledge.him.canonical.family.HimEntityId
import de.shopme.tools.knowledge.him.canonical.family.HimEntityIdRegistry
import de.shopme.tools.knowledge.him.canonical.family.HimEntityType
import de.shopme.tools.knowledge.him.canonical.family.HimLifecycleStatus

object HimGroundTruthEntityFingerprintIndexContractV1 {
    const val SCHEMA_VERSION = "1"
}

class HimGroundTruthEntityFingerprintIndexBuilderV1(
    private val generator: HimEntityFingerprintGenerator =
        HimEntityFingerprintGenerator(),
) {

    fun build(
        registry: HimEntityIdRegistry,
        authority: HimCanonicalFamilyAuthority,
        registrySha256: String,
        authoritySha256: String,
    ): HimEntityFingerprintIndex {
        require(registrySha256.isNotBlank()) {
            "Entity-ID Registry SHA-256 must not be blank."
        }

        require(authoritySha256.isNotBlank()) {
            "Canonical Family Authority SHA-256 must not be blank."
        }

        validateRegistryAndAuthority(
            registry = registry,
            authority = authority,
        )

        val entries =
            authority.families
                .sortedBy { it.canonicalId.value }
                .flatMap { family ->
                    buildFamilyEntries(family)
                }

        require(
            entries
                .map { structuralKey(it) }
                .distinct()
                .size == entries.size
        ) {
            "Duplicate structural fingerprint scopes generated."
        }

        require(
            entries
                .map { it.fingerprint }
                .distinct()
                .size == entries.size
        ) {
            "Duplicate entity fingerprints generated."
        }

        return HimEntityFingerprintIndex(
            schemaVersion =
                HimGroundTruthEntityFingerprintIndexContractV1.SCHEMA_VERSION,
            sourceAuthorities =
                HimEntityFingerprintIndexSources(
                    entityIdRegistrySha256 = registrySha256,
                    canonicalFamilyAuthoritySha256 = authoritySha256,
                ),
            entryCount = entries.size,
            entries = entries,
        )
    }

    private fun buildFamilyEntries(
        family: HimCanonicalFamily,
    ): List<HimEntityFingerprintEntry> {
        val canonicalScope =
            entry(
                canonicalId = family.canonicalId,
                identityId = null,
                variantIds =
                    family.variants
                        .filter {
                            it.lifecycleStatus == HimLifecycleStatus.ACTIVE
                        }
                        .map { it.variantId },
            )

        val identityScopes =
            family.identities
                .filter {
                    it.lifecycleStatus == HimLifecycleStatus.ACTIVE
                }
                .sortedBy {
                    it.identityId.value
                }
                .map { identity ->
                    entry(
                        canonicalId = family.canonicalId,
                        identityId = identity.identityId,
                        variantIds =
                            identity.variants
                                .filter {
                                    it.lifecycleStatus ==
                                            HimLifecycleStatus.ACTIVE
                                }
                                .map { it.variantId },
                    )
                }

        return listOf(canonicalScope) + identityScopes
    }

    private fun entry(
        canonicalId: HimEntityId,
        identityId: HimEntityId?,
        variantIds: List<HimEntityId>,
    ): HimEntityFingerprintEntry {
        val orderedVariantIds =
            variantIds
                .distinct()
                .sortedBy { it.value }

        require(orderedVariantIds.size == variantIds.size) {
            "Duplicate variant IDs in fingerprint scope."
        }

        return HimEntityFingerprintEntry(
            fingerprint =
                generator.generate(
                    canonicalId = canonicalId,
                    identityId = identityId,
                    variantIds = orderedVariantIds,
                ),
            canonicalId = canonicalId,
            identityId = identityId,
            variantIds = orderedVariantIds,
        )
    }

    private fun validateRegistryAndAuthority(
        registry: HimEntityIdRegistry,
        authority: HimCanonicalFamilyAuthority,
    ) {
        val registryById =
            registry.entries.associateBy { it.entityId }

        require(registryById.size == registry.entries.size) {
            "Duplicate Entity IDs in active Entity-ID Registry."
        }

        val authorityIds =
            buildList {
                authority.families.forEach { family ->
                    add(
                        family.canonicalId to
                                HimEntityType.CANONICAL
                    )

                    family.identities.forEach { identity ->
                        add(
                            identity.identityId to
                                    HimEntityType.IDENTITY
                        )

                        identity.variants.forEach { variant ->
                            add(
                                variant.variantId to
                                        HimEntityType.VARIANT
                            )
                        }

                        identity.aliases.forEach { alias ->
                            add(
                                alias.aliasId to
                                        HimEntityType.ALIAS
                            )
                        }
                    }

                    family.variants.forEach { variant ->
                        add(
                            variant.variantId to
                                    HimEntityType.VARIANT
                        )
                    }

                    family.aliases.forEach { alias ->
                        add(
                            alias.aliasId to
                                    HimEntityType.ALIAS
                        )
                    }
                }
            }

        require(
            authorityIds
                .map { it.first }
                .distinct()
                .size == authorityIds.size
        ) {
            "Duplicate Entity IDs in Canonical Family Authority."
        }

        authorityIds.forEach { (entityId, entityType) ->
            val registryEntry =
                requireNotNull(
                    registryById[entityId]
                ) {
                    "Authority Entity ID is missing from active Entity-ID Registry: " +
                            entityId.value
                }

            require(
                registryEntry.entityType == entityType
            ) {
                "Entity type mismatch between Authority and Entity-ID Registry " +
                        "for ${entityId.value}."
            }
        }

        require(
            registry.entries.all {
                    registryEntry ->
                authorityIds.any {
                        (entityId, entityType) ->
                    entityId == registryEntry.entityId &&
                            entityType == registryEntry.entityType
                }
            }
        ) {
            "Active Entity-ID Registry contains Entity IDs absent from Authority."
        }
    }

    private fun structuralKey(
        entry: HimEntityFingerprintEntry,
    ): String =
        buildString {
            append(entry.canonicalId.value)
            append('|')
            append(entry.identityId?.value.orEmpty())
            append('|')
            append(
                entry.variantIds
                    .map { it.value }
                    .sorted()
                    .joinToString(",")
            )
        }
}