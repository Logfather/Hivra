package de.shopme.tools.knowledge.him.canonical.family

class HimEntityFingerprintIndexBuilder(
    private val generator: HimEntityFingerprintGenerator =
        HimEntityFingerprintGenerator(),
) {

    fun build(
        registry: HimEntityIdRegistry,
        authority: HimCanonicalFamilyAuthority,
        registrySha256: String,
        authoritySha256: String,
    ): HimEntityFingerprintIndex {
        require(registrySha256 == EXPECTED_REGISTRY_SHA256)
        require(authoritySha256 == EXPECTED_AUTHORITY_SHA256)
        require(registry.entries.size == EXPECTED_ENTRY_COUNT)
        require(registry.entries.all { it.entityType == HimEntityType.CANONICAL })
        require(authority.families.size == EXPECTED_ENTRY_COUNT)
        require(authority.families.all {
            it.lifecycleStatus == HimLifecycleStatus.ACTIVE
        })
        require(authority.families.all {
            it.identities.isEmpty() && it.variants.isEmpty() && it.aliases.isEmpty()
        })

        val registryIds = registry.entries.map { it.entityId }.toSet()
        require(registryIds.size == EXPECTED_ENTRY_COUNT)
        require(authority.families.all { it.canonicalId in registryIds })

        val entries =
            authority.families.map { family ->
                HimEntityFingerprintEntry(
                    fingerprint =
                        generator.generate(
                            canonicalId = family.canonicalId,
                            identityId = null,
                            variantIds = emptyList(),
                        ),
                    canonicalId = family.canonicalId,
                    identityId = null,
                    variantIds = emptyList(),
                )
            }

        return HimEntityFingerprintIndex(
            schemaVersion = INDEX_SCHEMA_VERSION,
            sourceAuthorities =
                HimEntityFingerprintIndexSources(
                    entityIdRegistrySha256 = registrySha256,
                    canonicalFamilyAuthoritySha256 = authoritySha256,
                ),
            entryCount = entries.size,
            entries = entries,
        )
    }

    companion object {
        const val INDEX_SCHEMA_VERSION = "1"
        const val EXPECTED_ENTRY_COUNT = 1384
        const val EXPECTED_REGISTRY_SHA256 =
            "46145e663b483777228894a099f958f8150773af98b3772882823745b365cf9a"
        const val EXPECTED_AUTHORITY_SHA256 =
            "86b29621ecd21c91d53231d4a76f633cd172fced7c6f73fe47a7577bb600e184"
    }
}
