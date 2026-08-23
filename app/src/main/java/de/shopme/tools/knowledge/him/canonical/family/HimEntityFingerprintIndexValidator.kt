package de.shopme.tools.knowledge.him.canonical.family

data class HimEntityFingerprintIndexValidationResult(
    val entries: Int,
    val uniqueFingerprints: Int,
    val duplicateFingerprintGroups: Int,
    val invalidFingerprints: Int,
    val familiesWithoutFingerprint: Int,
    val fingerprintsWithoutFamily: Int,
    val unknownCanonicalIds: Int,
    val duplicateCanonicalIdEntries: Int,
    val identityReferences: Int,
    val variantReferences: Int,
    val fingerprintRecomputationMismatches: Int,
)

class HimEntityFingerprintIndexValidator(
    private val generator: HimEntityFingerprintGenerator =
        HimEntityFingerprintGenerator(),
) {

    fun validate(
        registry: HimEntityIdRegistry,
        authority: HimCanonicalFamilyAuthority,
        index: HimEntityFingerprintIndex,
        registrySha256: String,
        authoritySha256: String,
    ): HimEntityFingerprintIndexValidationResult {
        require(registrySha256 == HimEntityFingerprintIndexBuilder.EXPECTED_REGISTRY_SHA256)
        require(authoritySha256 == HimEntityFingerprintIndexBuilder.EXPECTED_AUTHORITY_SHA256)
        require(index.schemaVersion == HimEntityFingerprintIndexBuilder.INDEX_SCHEMA_VERSION)
        require(index.sourceAuthorities.entityIdRegistrySha256 == registrySha256)
        require(index.sourceAuthorities.canonicalFamilyAuthoritySha256 == authoritySha256)
        require(index.entryCount == index.entries.size)
        require(index.entryCount == HimEntityFingerprintIndexBuilder.EXPECTED_ENTRY_COUNT)

        val canonicalRegistryIds =
            registry.entries
                .filter { it.entityType == HimEntityType.CANONICAL }
                .map { it.entityId }
                .toSet()
        val familyIds = authority.families.map { it.canonicalId }.toSet()
        val entryIds = index.entries.map { it.canonicalId }
        val entryIdSet = entryIds.toSet()
        val fingerprintCounts = index.entries.groupingBy { it.fingerprint }.eachCount()

        val result =
            HimEntityFingerprintIndexValidationResult(
                entries = index.entries.size,
                uniqueFingerprints = fingerprintCounts.size,
                duplicateFingerprintGroups = fingerprintCounts.count { it.value > 1 },
                invalidFingerprints = index.entries.count {
                    !it.fingerprint.matches(FINGERPRINT_PATTERN)
                },
                familiesWithoutFingerprint = (familyIds - entryIdSet).size,
                fingerprintsWithoutFamily = (entryIdSet - familyIds).size,
                unknownCanonicalIds = (entryIdSet - canonicalRegistryIds).size,
                duplicateCanonicalIdEntries = entryIds.size - entryIdSet.size,
                identityReferences = index.entries.count { it.identityId != null },
                variantReferences = index.entries.sumOf { it.variantIds.size },
                fingerprintRecomputationMismatches = index.entries.count { entry ->
                    entry.fingerprint !=
                            generator.generate(
                                canonicalId = entry.canonicalId,
                                identityId = entry.identityId,
                                variantIds = entry.variantIds,
                            )
                },
            )

        require(result.entries == authority.families.size)
        require(result.uniqueFingerprints == result.entries)
        require(result.duplicateFingerprintGroups == 0)
        require(result.invalidFingerprints == 0)
        require(result.familiesWithoutFingerprint == 0)
        require(result.fingerprintsWithoutFamily == 0)
        require(result.unknownCanonicalIds == 0)
        require(result.duplicateCanonicalIdEntries == 0)
        require(result.identityReferences == 0)
        require(result.variantReferences == 0)
        require(result.fingerprintRecomputationMismatches == 0)
        require(index.entries.map { it.canonicalId } == authority.families.map { it.canonicalId })

        return result
    }

    private companion object {
        val FINGERPRINT_PATTERN = Regex("[0-9a-f]{64}")
    }
}
