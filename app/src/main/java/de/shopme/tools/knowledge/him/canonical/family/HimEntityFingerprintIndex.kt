package de.shopme.tools.knowledge.him.canonical.family

data class HimEntityFingerprintIndex(
    val schemaVersion: String,
    val sourceAuthorities: HimEntityFingerprintIndexSources,
    val entryCount: Int,
    val entries: List<HimEntityFingerprintEntry>,
)

data class HimEntityFingerprintIndexSources(
    val entityIdRegistrySha256: String,
    val canonicalFamilyAuthoritySha256: String,
)

data class HimEntityFingerprintEntry(
    val fingerprint: String,
    val canonicalId: HimEntityId,
    val identityId: HimEntityId?,
    val variantIds: List<HimEntityId>,
)
